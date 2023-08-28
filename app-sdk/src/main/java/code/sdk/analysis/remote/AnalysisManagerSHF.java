package code.sdk.analysis.remote;

import android.content.Context;

import com.androidx.h5.data.model.BaseResponse;
import com.androidx.h5.http.HttpCallback;
import com.androidx.h5.http.HttpRequest;
import com.androidx.h5.utils.AES;

import java.nio.charset.StandardCharsets;
import java.util.List;

import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;
import code.util.LogUtil;

public class AnalysisManagerSHF {

    public static final String TAG = AnalysisManagerSHF.class.getSimpleName();

    private String mHosts[] = new String[]{};
    private static final String DD2_DOT_API = "api/v1/dot";

    private Context mContext;
    private static AnalysisManagerSHF sInstance;
    private AnalysisCallback mAnalysisCallback;
    private AnalysisRequest analysisRequest;

    private AnalysisManagerSHF(Context context) {
        mContext = context;
    }

    public static AnalysisManagerSHF init(Context context) {
        if (null == sInstance) {
            sInstance = new AnalysisManagerSHF(context);
        }
        return sInstance;
    }

    public static AnalysisManagerSHF getInstance() {
        if (null == sInstance) {
            throw new NullPointerException("Please call init first");
        }
        return sInstance;
    }

    public void setAnalysisCallback(AnalysisCallback analysisCallback) {
        mAnalysisCallback = analysisCallback;
    }

    public void setHosts(String[] hosts) {
        mHosts = hosts;
    }

    private int mCurHostIndex = 0;
    private int mRetryCount = 0;
    private static final int RETRY_TOTAL_COUNT = 3;

    public void start() {
        if (null == mHosts || mHosts.length == 0) {
            LogUtil.e(TAG, "～～～ Url is empty, can not start request ～～～");
            if (null != mAnalysisCallback) {
                mAnalysisCallback.onResult(-1);
            }
            return;
        }
        mRetryCount = 0;
        mCurHostIndex = 0;
        doRequest();
    }

    private AnalysisRequest buildRemoteRequest() {
        String deviceId = DeviceUtil.getDeviceID();
        String packageName = PackageUtil.getPackageName();
        String channel = PackageUtil.getChannel();
        String brandCode = PackageUtil.getBrand();
        //int cvc = PackageUtil.getPackageVersionCode();
        //String cvn = PackageUtil.getPackageVersionName();
        //int svc = Build.VERSION.SDK_INT;
        //String svn = Build.VERSION.RELEASE;
        List<String> simCountryIsoList = DeviceUtil.getAllSimCountryIso(mContext);
        String simCountryIso = String.join(",", simCountryIsoList);
        String networkCountryCode = DeviceUtil.getNetworkCountryCode(mContext);
        String language = DeviceUtil.getLanguage(mContext);
        String platform = "android";
        String referrer = PreferenceUtil.readInstallReferrer();
        //String deviceInfo = DeviceUtil.getDeviceInfoForSHF(mContext);

        AnalysisRequest remoteRequest = new AnalysisRequest();
        remoteRequest.setDeviceId(deviceId);
        remoteRequest.setPackageName(packageName);
        remoteRequest.setChannel(channel);
        remoteRequest.setBrandCode(brandCode);
        remoteRequest.setSimCountryCode(simCountryIso);
        remoteRequest.setSysCountryCode(networkCountryCode);
        remoteRequest.setLanguage(language);
        remoteRequest.setPlatform(platform);
        remoteRequest.setReferrer(referrer);
        //remoteRequest.setDeviceInfo(deviceInfo);

        return remoteRequest;
    }

    private void doRequest() {
        if (mCurHostIndex < 0 || mCurHostIndex >= mHosts.length) {
            return;
        }
        String host = mHosts[mCurHostIndex];
        if (null == analysisRequest) {
            analysisRequest = buildRemoteRequest();
        }
        String requestJson = analysisRequest.toJson();
        LogUtil.d(TAG, "Analysis request=" + requestJson);
        byte[] postBody = AES.encryptByGCM(requestJson.getBytes(StandardCharsets.UTF_8), AES.MODE256);
        HttpRequest httpRequest = new HttpRequest.Builder()
                .setLoggable(LogUtil.isDebug())
                .setHost(host)
                .setApi(DD2_DOT_API)
                .appendQuery("enc", AES.enc())
                .appendQuery("nonce", AES.NONCE)
                .setMethod("POST")
                .appendFormData(postBody)
                .build();
        httpRequest.startAsync(new HttpCallback() {
            @Override
            public void onSuccess(String data) {
                LogUtil.d(TAG, "URL[%s] Analysis request success: data=%s", host, data);
                mAnalysisCallback.onResult(getStatus(data));
            }

            @Override
            public void onFailure(int code, String message) {
                LogUtil.e(TAG, "URL[%s] Analysis request failed: code=%d, message=%s", host, code, message);
                if (mCurHostIndex == mHosts.length - 1 && mRetryCount >= RETRY_TOTAL_COUNT) {
                    LogUtil.e(TAG, "Analysis request all fail，please check your url");
                    if (null != mAnalysisCallback) {
                        mAnalysisCallback.onResult(-1);
                    }
                    return;
                }
                retryRequest();
            }
        });
    }

    private int getStatus(String json) {
        BaseResponse response = new BaseResponse().fromJson(json, null);
        return response.getStatus();
    }

    private void retryRequest() {
        if (mRetryCount < RETRY_TOTAL_COUNT) {//retry current url
            mRetryCount++;
            doRequest();
            LogUtil.d(TAG, "URL[%s]：Analysis retry [%d] times", mHosts[mCurHostIndex], mRetryCount);
        } else {//next url
            mRetryCount = 0;
            mCurHostIndex++;
            doRequest();
        }
    }

    public void setRequest(AnalysisRequest analysisRequest) {
        this.analysisRequest = analysisRequest;
    }
}
