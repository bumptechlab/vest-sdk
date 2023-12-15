package code.sdk.shf.remote;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.URLUtil;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.NetworkUtil;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.URLUtilX;
import code.sdk.shf.http.BaseResponse;
import code.sdk.shf.http.HttpClient;
import code.util.AES;
import code.util.AESKeyStore;
import code.util.LogUtil;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class RemoteManagerSHF {

    public static final String TAG = RemoteManagerSHF.class.getSimpleName();

    private static final int RETRY_TOTAL_COUNT = 3;

    private Context mContext;

    private RemoteCallback mRemoteCallback;

    private boolean isRequesting;


    private static class InstanceHolder {
        private static final RemoteManagerSHF INSTANCE = new RemoteManagerSHF();
    }

    public static synchronized RemoteManagerSHF init(Context context) {
        RemoteManagerSHF instance = InstanceHolder.INSTANCE;
        instance.mContext = context;
        return instance;
    }

    public static synchronized RemoteManagerSHF getInstance() {
        if (null == InstanceHolder.INSTANCE) {
            throw new NullPointerException("Please call init first");
        }
        return InstanceHolder.INSTANCE;
    }

    public void setRemoteCallback(RemoteCallback remoteCallback) {
        mRemoteCallback = remoteCallback;
    }

    public void start(String baseHost, String[] spareHosts) {
        if (isRequesting) {
            LogUtil.d(TAG, "[SHF] is requesting, could not proceed another request");
            return;
        }
        List<String> hosts = initializeHosts(baseHost, spareHosts);
        if (hosts.isEmpty()) {
            handleError("[SHF] there are no valid hosts, abort requesting");
            return;
        }
        doRequest(hosts, 0, 0);
    }

    private List<String> initializeHosts(String baseHost, String[] spareHosts) {
        List<String> hosts = new ArrayList<>();
        if (isHostValid(baseHost)) {
            hosts.add(baseHost);
        }
        if (spareHosts != null) {
            for (String spareHost : spareHosts) {
                if (isHostValid(spareHost)) {
                    hosts.add(spareHost);
                }
            }
        }
        return hosts;
    }

    private void handleError(String errorMessage) {
        LogUtil.e(TAG, "[SHF] encounter an error: " + errorMessage);
        isRequesting = false;
        if (mRemoteCallback != null) {
            mRemoteCallback.onResult(false, null);
        }
    }

    /**
     * 保证API不以/开头，避免构建url时重复出现/
     *
     * @return
     */
    private String getShfDispatcher() {
        String shfDispatcher = ConfigPreference.readShfDispatcher();
        if (TextUtils.isEmpty(shfDispatcher)) {
            shfDispatcher = "api/v1/dispatcher";
        }
        if (shfDispatcher.startsWith("/")) {
            shfDispatcher = shfDispatcher.replaceFirst("/", "");
        }
        return shfDispatcher;
    }

    private void doRequest(List<String> hosts, int hostIndex, int retryCount) {
        isRequesting = true;
        if (hostIndex >= hosts.size() || retryCount > RETRY_TOTAL_COUNT) {
            handleError("[SHF] request all failed, please check your hosts");
            return;
        }

        String host = hosts.get(hostIndex);
        RemoteRequest remoteRequest = buildRemoteRequest();
        String requestJson = remoteRequest.toJson();
        byte[] bytes = AES.encryptByGCM(requestJson.getBytes(), AES.MODE256);
        if (bytes == null) {
            handleError("[SHF] request all failed, errors happen while encrypting request body");
            return;
        }
        MediaType mediaType = MediaType.parse("application/octet-stream");
        RequestBody requestBody = RequestBody.create(mediaType, bytes);
        Map<String, String> query = new HashMap<>();
        query.put("enc", AES.enc());
        query.put("nonce", AESKeyStore.getIvParams());
        HttpClient instance = HttpClient.getInstance();
        String url = instance.buildUrl(host, getShfDispatcher(), query);
        LogUtil.d(TAG, "[SHF] URL[%s] request start: %s", url, requestJson);
        instance.getApi()
                .getGameInfo(url, requestBody)
                .compose(instance.ioSchedulers())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull ResponseBody data) {
                        try {
                            RemoteConfig remoteConfig = new RemoteConfig();
                            String result = data.string();
                            BaseResponse<RemoteConfig> response = new BaseResponse<RemoteConfig>().fromJson(result, remoteConfig);
                            LogUtil.d(TAG, "[SHF] URL[%s] request success: %s", url, result);
                            isRequesting = false;
                            if (mRemoteCallback != null) {
                                mRemoteCallback.onResult(true, response.getData());
                            }
                        } catch (IOException e) {
                            onError(e);
                        }

                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        LogUtil.e(TAG, e, "[SHF] URL[%s] request failed: retry=%d, message=%s", url, retryCount, e.getMessage());
                        if (NetworkUtil.isConnected(mContext)) {
                            if (retryCount == RETRY_TOTAL_COUNT) {
                                String domain = URLUtilX.parseHost(host);
                                if (!DeviceUtil.isDomainAvailable(domain)) {
                                    setHostValid(host, false);
                                    LogUtil.e(TAG, "[SHF] Host[%s] is not available", host);
                                } else {
                                    LogUtil.d(TAG, "[SHF] Host[%s] is available", host);
                                }
                            }
                        }
                        retryRequest(hosts, hostIndex, retryCount);
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private void retryRequest(List<String> hosts, int hostIndex, int retryCount) {
        if (retryCount < RETRY_TOTAL_COUNT) {
            // Retry current URL
            doRequest(hosts, hostIndex, retryCount + 1);
        } else {
            // Move to the next URL
            doRequest(hosts, hostIndex + 1, 0);
        }
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

    private RemoteRequest buildRemoteRequest() {
        String type = "h5";
        String deviceId = DeviceUtil.getDeviceID();
        String packageName = PackageUtil.getPackageName();
        String channel = PackageUtil.getChannel();
        String parentBrd = PackageUtil.getParentBrand();
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

        RemoteRequest remoteRequest = new RemoteRequest();
        remoteRequest.setType(type);
        remoteRequest.setDeviceId(deviceId);
        remoteRequest.setPackageName(packageName);
        remoteRequest.setChannel(channel);
        remoteRequest.setParentBrd(parentBrd);
        remoteRequest.setVersionCode(cvc);
        remoteRequest.setVersionName(cvn);
        remoteRequest.setSysVersionCode(svc);
        remoteRequest.setSysVersionName(svn);
        remoteRequest.setSimCountryCode(simCountryIso);
        remoteRequest.setSysCountryCode(networkCountryCode);
        remoteRequest.setLanguage(language);
        remoteRequest.setPlatform(platform);
        remoteRequest.setReferrer(referrer);
        remoteRequest.setDeviceInfo("");

        return remoteRequest;
    }

}
