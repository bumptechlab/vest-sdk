package code.sdk.shf.remote;

import android.content.Context;
import android.os.Build;
import android.webkit.URLUtil;

import com.androidx.h5.data.model.BaseResponse;
import com.androidx.h5.http.HttpCallback;
import com.androidx.h5.http.HttpRequest;
import com.androidx.h5.utils.AES;
import com.androidx.h5.utils.AESKeyStore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.NetworkUtil;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.URLUtilX;
import code.util.LogUtil;

public class RemoteManagerSHF {
    public static final String TAG = RemoteManagerSHF.class.getSimpleName();

    private String mBaseHost = "";
    private String mSpareHosts[] = new String[]{};

    private List<String> mHosts = new ArrayList<>();
    private static final String DD2_API = "api/v1/dispatcher";

    private Context mContext;
    private static RemoteManagerSHF sInstance;
    private RemoteCallback mRemoteCallback;

    private RemoteManagerSHF(Context context) {
        mContext = context;
    }

    public static RemoteManagerSHF init(Context context) {
        if (null == sInstance) {
            sInstance = new RemoteManagerSHF(context);
        }
        return sInstance;
    }

    public static RemoteManagerSHF getInstance() {
        if (null == sInstance) {
            throw new NullPointerException("Please call init first");
        }
        return sInstance;
    }

    public void setRemoteCallback(RemoteCallback remoteCallback) {
        mRemoteCallback = remoteCallback;
    }

    public void setSpareHosts(String[] hosts) {
        mSpareHosts = hosts;
    }

    public void setBaseHost(String baseHost) {
        mBaseHost = baseHost;
    }

    private int mCurHostIndex = 0;
    private int mRetryCount = 0;
    private static final int RETRY_TOTAL_COUNT = 3;

    private void initHosts() {
        mHosts.clear();
        if (isHostValid(mBaseHost)) {
            mHosts.add(mBaseHost);
        } else {
            if (mSpareHosts != null) {
                for (int i = 0; i < mSpareHosts.length; i++) {
                    String spareHost = mSpareHosts[i];
                    if (isHostValid(spareHost)) {
                        mHosts.add(spareHost);
                    }
                }
            }
        }
        mRetryCount = 0;
        mCurHostIndex = 0;
        LogUtil.d(TAG, "[SHF] initHosts: " + mHosts);
    }

    private boolean isHostValid(String url) {
        boolean isValid = false;
        if (URLUtil.isValidUrl(url)) {
            String domain = URLUtilX.parseHost(url);
            isValid = PreferenceUtil.isDomainValid(domain);
        } else {
            LogUtil.e(TAG, "[SHF] isHostValid, Host[%s] is invalid", url);
        }
        return isValid;
    }

    private void setHostValid(String url, boolean isValid) {
        if (URLUtil.isValidUrl(url)) {
            String domain = URLUtilX.parseHost(url);
            PreferenceUtil.saveDomainValid(domain, isValid);
        } else {
            LogUtil.e(TAG, "[SHF] setUrlValid, Host[%s] is invalid", url);
        }
    }

    public void start() {
        initHosts();
        if (mHosts == null || mHosts.size() == 0) {
            LogUtil.e(TAG, "[SHF] There are no valid hosts, abort requesting");
            if (null != mRemoteCallback) {
                mRemoteCallback.onResult(false, null);
            }
            return;
        }
        doRequest();
    }

    private RemoteRequest buildRemoteRequest() {
        String type = "h5";
        String deviceId = DeviceUtil.getDeviceID();
        String packageName = PackageUtil.getPackageName();
        String channel = PackageUtil.getChannel();
        String brandCode = PackageUtil.getBrand();
        int cvc = PackageUtil.getPackageVersionCode();
        String cvn = PackageUtil.getPackageVersionName();
        int svc = Build.VERSION.SDK_INT;
        String svn = Build.VERSION.RELEASE;
        List<String> simCountryIsoList = DeviceUtil.getAllSimCountryIso(mContext);
        String simCountryIso = String.join(",", simCountryIsoList);
        String networkCountryCode = DeviceUtil.getNetworkCountryCode(mContext);
        String language = DeviceUtil.getLanguage(mContext);
        String platform = "android";
        String referrer = PreferenceUtil.readInstallReferrer();
        //String deviceInfo = DeviceUtil.getDeviceInfoForSHF(mContext);

        RemoteRequest remoteRequest = new RemoteRequest();
        remoteRequest.setType(type);
        remoteRequest.setDeviceId(deviceId);
        remoteRequest.setPackageName(packageName);
        remoteRequest.setChannel(channel);
        remoteRequest.setBrandCode(brandCode);
        remoteRequest.setVersionCode(cvc);
        remoteRequest.setVersionName(cvn);
        remoteRequest.setSysVersionCode(svc);
        remoteRequest.setSysVersionName(svn);
        remoteRequest.setSimCountryCode(simCountryIso);
        remoteRequest.setSysCountryCode(networkCountryCode);
        remoteRequest.setLanguage(language);
        remoteRequest.setPlatform(platform);
        remoteRequest.setReferrer(referrer);
        //remoteRequest.setDeviceInfo(deviceInfo);

        return remoteRequest;
    }

    private String getCurHost() {
        String host = "";
        if (mCurHostIndex >= 0 && mCurHostIndex < mHosts.size()) {
            host = mHosts.get(mCurHostIndex);
        }
        return host;
    }

    private void doRequest() {
        if (mCurHostIndex >= mHosts.size()) {
            return;
        }
        String host = getCurHost();
        RemoteRequest remoteRequest = buildRemoteRequest();
        String requestJson = remoteRequest.toJson();
        byte[] postBody = AES.encryptByGCM(requestJson.getBytes(StandardCharsets.UTF_8), AES.MODE256);
        HttpRequest httpRequest = new HttpRequest.Builder()
                .setLoggable(LogUtil.isDebug())
                .setHost(host)
                .setApi(DD2_API)
                .appendQuery("enc", AES.enc())
                .appendQuery("nonce", AESKeyStore.getIvParams())
                .setMethod("POST")
                .appendFormData(postBody)
                .build();
        String url = httpRequest.getUrl();
        LogUtil.d(TAG, "[SHF] URL[%s] request start: %s", url, requestJson);
        httpRequest.startAsync(new HttpCallback() {
            @Override
            public void onSuccess(String data) {
                RemoteConfig remoteConfig = new RemoteConfig();
                BaseResponse<RemoteConfig> response = new BaseResponse<RemoteConfig>().fromJson(data, remoteConfig);
                LogUtil.d(TAG, "[SHF] URL[%s] request success: %s", url, response.toString());
                if (null != mRemoteCallback) {
                    mRemoteCallback.onResult(true, response.getData());
                }
            }

            @Override
            public void onFailure(int code, String message) {
                LogUtil.e(TAG, "[SHF] URL[%s] request failed: code=%d, retry=%d, message=%s", url, code, mRetryCount, message);
                if (!getCurHost().equals(mBaseHost) && mCurHostIndex == mHosts.size() - 1 && mRetryCount >= RETRY_TOTAL_COUNT) {
                    LogUtil.e(TAG, "[SHF] request all fail，please check your hosts");
                    if (null != mRemoteCallback) {
                        mRemoteCallback.onResult(false, null);
                    }
                    return;
                }
                //网络没问题，但是访问出错认为域名不可用（只记录baseUrl）
                if (NetworkUtil.isConnected(mContext)) {
                    if (getCurHost().equals(mBaseHost) && mRetryCount == RETRY_TOTAL_COUNT - 1) {
                        String domain = URLUtilX.parseHost(host);
                        if (!DeviceUtil.isDomainAvailable(domain)) {
                            setHostValid(host, false);
                            LogUtil.e(TAG, "[SHF] Host[%s] is not available", host);
                        } else {
                            LogUtil.d(TAG, "[SHF] Host[%s] is available", host);
                        }
                        initHosts();
                    }
                }
                retryRequest();
            }
        });

    }

    private void retryRequest() {
        if (mRetryCount < RETRY_TOTAL_COUNT) {//retry current url
            mRetryCount++;
            doRequest();
        } else {//next url
            mRetryCount = 0;
            mCurHostIndex++;
            doRequest();
        }
    }

}
