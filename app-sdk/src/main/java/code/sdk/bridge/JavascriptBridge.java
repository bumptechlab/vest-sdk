package code.sdk.bridge;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.core.app.ActivityManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cocos.creator.walle.ExtraInfo;
import cocos.creator.walle.ExtraInfoReader;
import code.sdk.BuildConfig;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.EmulatorChecker;
import code.sdk.core.util.FileUtil;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.httpdns.HttpDnsHttpListener;
import code.sdk.httpdns.HttpDnsMgr;
import code.sdk.httpdns.HttpDnsWsListener;
import code.sdk.core.manager.AdjustManager;
import code.sdk.manager.OneSignalManager;
import code.sdk.core.manager.ThinkingDataManager;
import code.sdk.network.download.DownloadTask;
import code.sdk.core.util.CocosPreferenceUtil;
import code.sdk.util.KeyChainUtil;
import code.util.AppGlobal;
import code.util.LogUtil;


public class JavascriptBridge {
    public static final String TAG = JavascriptBridge.class.getSimpleName();

    /**
     * 6: 支持HttpDns
     */
    private static final int BRIDGE_VERSION = 6;
    public static final String DIR_IMAGES = "images";
    public static final String PROMOTION_MATERIAL_FILENAME = "promotion_material_%s_%s";
    public static final String PROMOTION_IMAGE_FILENAME = "promotion_%d.jpg";
    public static final String PROMOTION_SHARE_FILENAME = "promotion_share_img.jpg";
    private Callback mCallback;

    @JavascriptInterface
    public static void nativeLog(String tag, String msg) {
        if (TextUtils.isEmpty(tag)) {
            tag = TAG;
        }
        LogUtil.d(tag, msg);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }


    /* interface -> non-callback */
    @JavascriptInterface
    public void copyText(String text) {
        if (TextUtils.isEmpty(text)) {
            ////ObfuscationStub5.inject();
            return;
        }
        ClipboardManager manager = (ClipboardManager) AppGlobal.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    @JavascriptInterface
    public String getCopiedText() {
        ClipboardManager manager = (ClipboardManager) AppGlobal.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager.hasPrimaryClip() && manager.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item clipData = manager.getPrimaryClip().getItemAt(0);
            ////ObfuscationStub6.inject();
            return TextUtils.isEmpty(clipData.getText()) ? "" : clipData.getText().toString();
        }
        ////ObfuscationStub7.inject();
        return "";
    }

    @JavascriptInterface
    public void showNativeToast(String toast) {
        Toast.makeText(AppGlobal.getApplication(), toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void initAdjustID(String adjustAppID) {
        LogUtil.d(TAG, "initAdjustID: " + adjustAppID);
        if (TextUtils.isEmpty(adjustAppID)) {
            ////ObfuscationStub8.inject();
            return;
        }

        String adjustAppIDCache = PreferenceUtil.readAdjustAppID();
        if (TextUtils.equals(adjustAppID, adjustAppIDCache)) {
            ////ObfuscationStub0.inject();
            return;
        }

        PreferenceUtil.saveAdjustAppID(adjustAppID);
        AdjustManager.initConfig(AppGlobal.getApplication(), adjustAppID);
    }

    @JavascriptInterface
    public void trackAdjustEvent(String eventToken, String jsonData) {
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(jsonData);
        } catch (JSONException e) {
            ////ObfuscationStub1.inject();
        }

        HashMap<String, String> s2sParams = new HashMap<>();
        if (jsonObj != null) {
            Iterator<String> keys = jsonObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonObj.optString(key);
                s2sParams.put(key, value);
            }
        } else {
            ////ObfuscationStub2.inject();
        }
        AdjustManager.trackEvent(eventToken, s2sParams);
    }

    @JavascriptInterface
    public void trackAdjustEventStart(String eventToken) {
        ////ObfuscationStub3.inject();
        AdjustManager.trackEventStart(eventToken);
    }

    @JavascriptInterface
    public void trackAdjustEventGreeting(String eventToken) {
        ////ObfuscationStub4.inject();
        AdjustManager.trackEventGreeting(eventToken);
    }

    @JavascriptInterface
    public void trackAdjustEventAccess(String eventToken) {
        ////ObfuscationStub5.inject();
        AdjustManager.trackEventAccess(eventToken);
    }

    @JavascriptInterface
    public void trackAdjustEventUpdated(String eventToken) {
        ////ObfuscationStub6.inject();
        AdjustManager.trackEventUpdated(eventToken);
    }

    @JavascriptInterface
    public String getDeviceID() {
        return DeviceUtil.getDeviceID();
    }

    @JavascriptInterface
    public String getDeviceInfoForLighthouse() {
        //return DeviceUtil.getDeviceInfoForLighthouse();
        return "";
    }

    @JavascriptInterface
    public int getSystemVersionCode() {
        return Build.VERSION.SDK_INT;
    }

    @JavascriptInterface
    public int getClientVersionCode() {
        return PackageUtil.getPackageVersionCode();
    }

    @JavascriptInterface
    public String getPackageName() {
        return PackageUtil.getPackageName();
    }

    @JavascriptInterface
    public String getAppName() {
        return PackageUtil.getAppName();
    }

    @JavascriptInterface
    public String getChannel() {
        return PackageUtil.getChannel();
    }

    @JavascriptInterface
    public String getBrand() {
        return PackageUtil.getBrand();
    }

    @JavascriptInterface
    public void saveGameUrl(String gameUrl) {
        if (!URLUtil.isValidUrl(gameUrl)) {
            ////ObfuscationStub7.inject();
            return;
        }

        LogUtil.d(TAG, "saveGameUrl = " + gameUrl);
        PreferenceUtil.saveGameUrl(gameUrl);
    }

    @JavascriptInterface
    public void saveAccountInfo(String plainText) {
        if (TextUtils.isEmpty(plainText)) {
            //ObfuscationStub8.inject();
            return;
        }

        LogUtil.d(TAG, "saveAccountInfo = " + plainText);
        KeyChainUtil.saveAccountInfo(plainText);
    }

    @JavascriptInterface
    public String getAccountInfo() {
        return KeyChainUtil.getAccountInfo();
    }

    @JavascriptInterface
    public String getAdjustDeviceID() {
        String adjustAdId = AdjustManager.getAdjustDeviceID();
        LogUtil.d(TAG, "getAdjustDeviceID: " + adjustAdId);
        return adjustAdId;
    }

    @JavascriptInterface
    public String getGoogleADID() {
        String googledAdId = DeviceUtil.getGoogleADID();
        LogUtil.d(TAG, "getGoogleADID: " + googledAdId);
        return googledAdId;
    }

    @JavascriptInterface
    public String getIDFA() {
        return ""; // iOS only
    }

    @JavascriptInterface
    public String getReferID() {
        ExtraInfo readInfo = ExtraInfoReader.get(FileUtil.getSelfApkFile());
        if (readInfo == null) {
            //ObfuscationStub0.inject();
            return "";
        }
        return readInfo.getReferID();
    }

    @JavascriptInterface
    public String getAgentID() {
        ExtraInfo readInfo = ExtraInfoReader.get(FileUtil.getSelfApkFile());
        if (readInfo == null) {
            //ObfuscationStub1.inject();
            return "";
        }
        return readInfo.getAgentID();
    }

    @JavascriptInterface
    public void setCocosData(String key, String value) {
        if (TextUtils.isEmpty(key)) {
            //ObfuscationStub2.inject();
            return;
        }

        LogUtil.d(TAG, "setCocosData, key = " + key + ", value = " + value);
        CocosPreferenceUtil.putString(key, value);

        if (CocosPreferenceUtil.KEY_USER_ID.equals(key)) {
            ThinkingDataManager.getInstance().loginAccount();
            OneSignalManager.setup();
        } else if (key.endsWith("_cur_month_lv") || key.endsWith("_last_month_lv") || key.endsWith("_latest_recharge")) {
            OneSignalManager.setup();
        }
    }

    @JavascriptInterface
    public String getCocosData(String key) {
        if (TextUtils.isEmpty(key)) {
            //ObfuscationStub3.inject();
            return "";
        }

        return CocosPreferenceUtil.getString(key);
    }

    @JavascriptInterface
    public String getCocosAllData() {
        Map<String, ?> map = CocosPreferenceUtil.getAll();
        JSONObject obj = new JSONObject(map);
        LogUtil.d(TAG, "getCocosAllData => " + obj);
        return obj.toString();
    }

    @JavascriptInterface
    public String getLighterHost() {
        //String lighterHost = ConfigPreference.readLighterHost();
        //LogUtil.d(TAG, "[Lighthouse] Host: " + lighterHost);
        //return lighterHost;
        return "";
    }

    @JavascriptInterface
    public int getBridgeVersion() {
        return BRIDGE_VERSION;
    }

    @JavascriptInterface
    public boolean isFacebookEnable() {
        return false;
    }

    @JavascriptInterface
    public String getTDTargetCountry() {
        return ThinkingDataManager.getTargetCountry();
    }

    @JavascriptInterface
    public void openUrlByBrowser(String url) {
        //ObfuscationStub4.inject();
        LogUtil.d(TAG, "openUrlByBrowser: " + url);
        if (mCallback != null) {
            mCallback.openUrlByBrowser(url);
        }
    }

    @JavascriptInterface
    public void openUrlByWebView(String url) {
        //ObfuscationStub5.inject();
        LogUtil.d(TAG, "openUrlByWebView: " + url);
        if (mCallback != null) {
            //ObfuscationStub1.inject();
            mCallback.openUrlByWebView(url);
        }
    }

    @JavascriptInterface
    public void openApp(String target, String fallbackUrl) {
        //ObfuscationStub6.inject();

        if (mCallback != null) {
            //ObfuscationStub3.inject();
            mCallback.openApp(target, fallbackUrl);
        }
    }

    @JavascriptInterface
    public void loadUrl(String url) {
        //ObfuscationStub7.inject();

        if (mCallback != null) {
            //ObfuscationStub3.inject();
            mCallback.loadUrl(url);
        }
    }

    @JavascriptInterface
    public void goBack() {
        //ObfuscationStub8.inject();

        if (mCallback != null) {
            mCallback.goBack();
        }
    }

    @JavascriptInterface
    public void close() {
        //ObfuscationStub0.inject();

        if (mCallback != null) {
            mCallback.close();
        }
    }

    @JavascriptInterface
    public void refresh() {
        //ObfuscationStub1.inject();

        if (mCallback != null) {
            mCallback.refresh();
        }
    }

    @JavascriptInterface
    public void clearCache() {
        //ObfuscationStub2.inject();

        if (mCallback != null) {
            mCallback.clearCache();
        }
    }

    @JavascriptInterface
    public void saveImage(String url) {
        //ObfuscationStub3.inject();

        if (mCallback != null) {
            mCallback.saveImage(url);
        }
    }

    @JavascriptInterface
    public void savePromotionMaterial(String materialUrl) {
        //ObfuscationStub4.inject();

        Context context = AppGlobal.getApplication();
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        FileUtil.ensureDirectory(dir);
        DownloadTask.download(materialUrl, dir.getAbsolutePath(), new DownloadTask.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File saveFile) {
                String fileName = String.format(PROMOTION_MATERIAL_FILENAME,
                        PackageUtil.getPackageName(), PackageUtil.getChannel());
                File destFile = new File(dir, fileName);
                boolean succeed = saveFile.renameTo(destFile);
                LogUtil.d(TAG, "savePromotionMaterial - download succeed = " + succeed);
                if (mCallback != null) {
                    mCallback.savePromotionMaterialDone(succeed);
                }
            }

            @Override
            public void onDownloading(int progress) {
                LogUtil.d(TAG, "savePromotionMaterial - downloading = " + progress + "%");
            }

            @Override
            public void onDownloadFailed() {
                LogUtil.w(TAG, "savePromotionMaterial - download failed");
                if (mCallback != null) {
                    //ObfuscationStub5.inject();
                    mCallback.savePromotionMaterialDone(false);
                }
            }
        });
    }

    @JavascriptInterface
    public void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y) {
        //ObfuscationStub6.inject();

        if (mCallback != null) {
            mCallback.synthesizePromotionImage(qrCodeUrl, size, x, y);
        }
    }

    @JavascriptInterface
    public void shareUrl(String url) {
        //ObfuscationStub7.inject();

        if (mCallback != null) {
            mCallback.shareUrl(url);
        }
    }

    @JavascriptInterface
    public void loginFacebook() {
        //ObfuscationStub8.inject();

        if (mCallback != null) {
            mCallback.loginFacebook();
        }
    }

    @JavascriptInterface
    public void logoutFacebook() {
        //ObfuscationStub0.inject();

        if (mCallback != null) {
            mCallback.logoutFacebook();
        }
    }

    @JavascriptInterface
    public void preloadPromotionImage(String imageUrl) {
        Context context = AppGlobal.getApplication();
        File imagePath = new File(context.getFilesDir(), DIR_IMAGES);
        FileUtil.ensureDirectory(imagePath);
        DownloadTask.download(imageUrl, imagePath.getAbsolutePath(), new DownloadTask.OnDownloadListener() {
            @Override
            public void onDownloadSuccess(File saveFile) {
                File destFile = new File(imagePath, PROMOTION_SHARE_FILENAME);
                boolean succeed = saveFile.renameTo(destFile);
                LogUtil.d(TAG, "preloadPromotionImage - download succeed = " + succeed);
                if (mCallback != null) {
                    mCallback.preloadPromotionImageDone(succeed);
                }
            }

            @Override
            public void onDownloading(int progress) {
                LogUtil.d(TAG, "preloadPromotionImage - downloading = " + progress + "%");
            }

            @Override
            public void onDownloadFailed() {
                LogUtil.w(TAG, "preloadPromotionImage - download failed");
                if (mCallback != null) {
                    mCallback.preloadPromotionImageDone(false);
                }
            }
        });
    }

    @JavascriptInterface
    public void shareToWhatsApp(String text) {
        Context context = AppGlobal.getApplication();
        File imagePath = new File(context.getFilesDir(), DIR_IMAGES);
        File destFile = new File(imagePath, PROMOTION_SHARE_FILENAME);
        if (mCallback != null) {
            mCallback.shareToWhatsApp(text, destFile);
        }
    }

    @JavascriptInterface
    public boolean isHttpDnsEnable() {
        return HttpDnsMgr.isHttpDnsEnable();
    }

    @JavascriptInterface
    public String httpdns(String host) {
        Pair<Integer, String> ipPair = HttpDnsMgr.getAddrByName(host);
        return ipPair.second;
    }

    @JavascriptInterface
    public void httpdnsInit(String hosts) {
        LogUtil.d(TAG, "[HttpDns] init: %s", hosts);
        try {
            JSONArray hostsJson = new JSONArray(hosts);
            List<String> hostsList = new ArrayList<String>();
            for (int i = 0; i < hostsJson.length(); i++) {
                hostsList.add(hostsJson.optString(i));
            }
            HttpDnsMgr.init(AppGlobal.getApplication(), hostsList.toArray(new String[]{}));
        } catch (Exception e) {
            LogUtil.e(TAG, e, "[HttpDns] init fail");
            //ObfuscationStub7.inject();
        }
    }

    @JavascriptInterface
    public String httpdnsRequestSync(String req, byte[] body) {
        LogUtil.w(TAG, "[HttpDns] httpdnsRequestSync: %s", req);
        String responseJson = "";
        try {
            JSONObject reqJson = new JSONObject(req);
            String url = reqJson.optString("url");
            String method = reqJson.optString("method");
            JSONObject headerJson = reqJson.optJSONObject("header");
            responseJson = HttpDnsMgr.doHttpRequestSync(url, method, body, headerJson).toString();
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub3.inject();
        }
        return responseJson;
    }

    @JavascriptInterface
    public void httpdnsRequestAsync(String req, byte[] body) {
        LogUtil.w(TAG, "[HttpDns] httpdnsRequestAsync: %s", req);
        try {
            JSONObject reqJson = new JSONObject(req);
            String id = reqJson.optString("id");
            String url = reqJson.optString("url");
            String method = reqJson.optString("method");
            JSONObject headerJson = reqJson.optJSONObject("header");
            HttpDnsMgr.doHttpRequestAsync(id, url, method, body, headerJson, new HttpDnsHttpListener() {
                        @Override
                        public void onResponse(String requestId, JSONObject response) {
                            if (mCallback != null) {
                                mCallback.onHttpDnsHttpResponse(requestId, response.toString());
                            }
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub8.inject();
        }
    }


    @JavascriptInterface
    public void httpdnsWsOpen(String req) {
        LogUtil.w(TAG, "[HttpDns] httpdnsWsOpen: %s", req);
        try {
            JSONObject reqJson = new JSONObject(req);
            String id = reqJson.optString("id");
            String url = reqJson.optString("url");
            JSONObject headerJson = reqJson.optJSONObject("header");
            HttpDnsMgr.doWsOpen(id, url, headerJson, new HttpDnsWsListener() {
                @Override
                public void onResponse(String requestId, JSONObject response) {
                    if (mCallback != null) {
                        mCallback.onHttpDnsWsResponse(requestId, response.toString());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub2.inject();
        }
    }

    @JavascriptInterface
    public String httpdnsWsSend(String req, byte[] body) {
        LogUtil.w(TAG, "[HttpDns] httpdnsWsSend: %s", req);
        String responseJson = "";
        try {
            JSONObject reqJson = new JSONObject(req);
            String id = reqJson.optString("id");
            responseJson = HttpDnsMgr.doWsSend(id, body).toString();
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub1.inject();
        }
        return responseJson;
    }

    @JavascriptInterface
    public void httpdnsWsClose(String req) {
        LogUtil.w(TAG, "[HttpDns] httpdnsWsClose: %s", req);
        try {
            JSONObject reqJson = new JSONObject(req);
            String id = reqJson.optString("id");
            HttpDnsMgr.doWsClose(id);
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub1.inject();
        }
    }

    @JavascriptInterface
    public String httpdnsWsConnected(String req) {
        LogUtil.w(TAG, "[HttpDns] httpdnsWsConnected: %s", req);
        String responseJson = "";
        try {
            JSONObject reqJson = new JSONObject(req);
            String id = reqJson.optString("id");
            responseJson = HttpDnsMgr.isWsConnected(id).toString();
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub5.inject();
        }
        return responseJson;
    }

    @JavascriptInterface
    public String getBuildVersion() {
        return PackageUtil.getBuildVersion();
    }


    /**
     * @param accid   create game id
     * @param cretime create time
     */
    @JavascriptInterface
    public void onAnalysisStart(String accid, long cretime) {
        //ObfuscationStub0.inject();
        if (mCallback != null) {
            mCallback.onStart(accid, cretime);
        }
    }


    @JavascriptInterface
    public void onAnalysisEnd() {
        //ObfuscationStub0.inject();
        if (mCallback != null) {
            mCallback.onEnd();
        }
    }

    @JavascriptInterface
    public String memoryInfo() {
        try {
            Context context = AppGlobal.getApplication();
            ActivityManager result = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            result.getMemoryInfo(mi);
            JSONObject json = new JSONObject();
            json.put("DeviceTotalMem", mi.totalMem);//设备实际最大内存
            json.put("DeviceAvailMem", mi.availMem); //设备可用内存
            json.put("isLowMemDevice", ActivityManagerCompat.isLowRamDevice(result)); //是否低内存设备
            Runtime r = Runtime.getRuntime();
            json.put("AppTotalMemory", r.totalMemory()); //App最大可用内存
            json.put("AppMaxMemory", r.maxMemory()); //App当前可用内存
            json.put("AppFreeMemory", r.freeMemory()); //App当前空闲内存
            LogUtil.i(TAG, "getMemInfo: " + json.toString());
            return json.toString();
        } catch (Exception e) {
            //ObfuscationStub5.inject();
            e.printStackTrace();
            LogUtil.e(TAG, "logMem:获取内存信息失败：" + e.getMessage());
        }
        return "";
    }

    @JavascriptInterface
    public boolean isEmulator() {
        boolean isEmulator = EmulatorChecker.isEmulator();
        LogUtil.d(TAG, "isEmulator: %s", isEmulator);
        return isEmulator;
    }

    @JavascriptInterface
    public void exitApp() {
        code.sdk.core.manager.ActivityManager.getInstance().finishAll();
    }

    /* interface -> callback */
    public interface Callback {
        void loadUrl(String url);

        void openUrlByBrowser(String url);

        void openUrlByWebView(String url);

        void openApp(String target, String fallbackUrl);

        void goBack();

        void close();

        void refresh();

        void clearCache();

        void saveImage(String url);

        void saveImageDone(boolean succeed);

        void savePromotionMaterialDone(boolean succeed);

        void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y);

        void synthesizePromotionImageDone(boolean succeed);

        void onHttpDnsHttpResponse(String requestId, String response);

        void onHttpDnsWsResponse(String requestId, String response);

        void shareUrl(String url);

        void loginFacebook();

        void logoutFacebook();

        void preloadPromotionImageDone(boolean succeed);

        void shareToWhatsApp(String text, File file);

        void onStart(String accid, long cretime);

        void onEnd();
    }
    /* interface -> callback */
}
