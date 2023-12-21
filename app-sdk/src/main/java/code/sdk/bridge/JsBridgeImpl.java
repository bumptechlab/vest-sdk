package code.sdk.bridge;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.core.app.ActivityManagerCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import code.sdk.common.DeviceMacUtil;
import code.sdk.core.manager.AdjustManager;
import code.sdk.core.manager.ThinkingDataManager;
import code.sdk.core.util.CocosPreferenceUtil;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.FileUtil;
import code.sdk.core.util.ImitateChecker;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.download.DownloadTask;
import code.sdk.util.KeyChainUtil;
import code.sdk.zfutil.ExtraInfo;
import code.sdk.zfutil.ExtraInfoReader;
import code.util.AppGlobal;
import code.util.LogUtil;

public class JsBridgeImpl implements BridgeInterface {

    private static final String TAG = JsBridgeImpl.class.getSimpleName();
    /**
     * 6: 支持HttpDns
     */
    private static final int BRIDGE_VERSION = 7;
    public static final String DIR_IMAGES = "images";
    public static final String PROMOTION_MATERIAL_FILENAME = "promotion_material_%s_%s";
    public static final String PROMOTION_IMAGE_FILENAME = "promotion_%d.jpg";
    public static final String PROMOTION_SHARE_FILENAME = "promotion_share_img.jpg";

    private BridgeCallback mCallback;

    public JsBridgeImpl(BridgeCallback callback) {
        mCallback = callback;
    }

    @Override
    public void nativeLog(String tag, String msg) {
        if (TextUtils.isEmpty(tag)) {
            tag = TAG;
        }
        LogUtil.d(tag, msg);
    }


    /* interface -> non-callback */
    @Override
    public void copyText(String text) {
        if (TextUtils.isEmpty(text)) {
            //ObfuscationStub5.inject();
            return;
        }
        ClipboardManager manager = (ClipboardManager) AppGlobal.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    @Override
    public String getCopiedText() {
        ClipboardManager manager = (ClipboardManager) AppGlobal.getApplication()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager.hasPrimaryClip() && manager.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item clipData = manager.getPrimaryClip().getItemAt(0);
            //ObfuscationStub6.inject();
            return TextUtils.isEmpty(clipData.getText()) ? "" : clipData.getText().toString();
        }
        //ObfuscationStub7.inject();
        return "";
    }

    @Override
    public void showNativeToast(String toast) {
        Toast.makeText(AppGlobal.getApplication(), toast, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void initAdjustID(String adjustAppID) {
        if (TextUtils.isEmpty(adjustAppID)) {
            //ObfuscationStub8.inject();
            return;
        }

        String adjustAppIDCache = PreferenceUtil.readAdjustAppID();
        if (TextUtils.equals(adjustAppID, adjustAppIDCache)) {
            //ObfuscationStub0.inject();
            return;
        }

        PreferenceUtil.saveAdjustAppID(adjustAppID);
        AdjustManager.initAdjustSdk(AppGlobal.getApplication(), adjustAppID);
    }

    @Override
    public void trackAdjustEvent(String eventToken, String jsonData) {
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject(jsonData);
        } catch (JSONException e) {
            //ObfuscationStub1.inject();
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
            //ObfuscationStub2.inject();
        }
        AdjustManager.trackEvent(eventToken, s2sParams);
    }

    @Override
    public void trackAdjustEventStart(String eventToken) {
        //ObfuscationStub3.inject();
        AdjustManager.trackEventStart(eventToken);
    }

    @Override
    public void trackAdjustEventGreeting(String eventToken) {
        //ObfuscationStub4.inject();
        AdjustManager.trackEventGreeting(eventToken);
    }

    @Override
    public void trackAdjustEventAccess(String eventToken) {
        //ObfuscationStub5.inject();
        AdjustManager.trackEventAccess(eventToken);
    }

    @Override
    public void trackAdjustEventUpdated(String eventToken) {
        //ObfuscationStub6.inject();
        AdjustManager.trackEventUpdated(eventToken);
    }

    @Override
    public String getDeviceID() {
        return DeviceUtil.getDeviceID();
    }

    @Override
    public String getDeviceInfoForLighthouse() {
        //return DeviceUtil.getDeviceInfoForLighthouse();
        return "";
    }

    @Override
    public int getSystemVersionCode() {
        return Build.VERSION.SDK_INT;
    }

    @Override
    public int getClientVersionCode() {
        return PackageUtil.getPackageVersionCode();
    }

    @Override
    public String getPackageName() {
        return PackageUtil.getPackageName();
    }

    @Override
    public String getAppName() {
        return PackageUtil.getAppName();
    }

    @Override
    public String getChannel() {
        return PackageUtil.getChannel();
    }

    @Override
    public String getBrand() {
        return PackageUtil.getChildBrand();
    }

    @Override
    public void saveGameUrl(String gameUrl) {
        if (!URLUtil.isValidUrl(gameUrl)) {
            //ObfuscationStub7.inject();
            return;
        }
        PreferenceUtil.saveGameUrl(gameUrl);
    }

    @Override
    public void saveAccountInfo(String plainText) {
        if (TextUtils.isEmpty(plainText)) {
            //ObfuscationStub8.inject();
            return;
        }
        KeyChainUtil.saveAccountInfo(plainText);
    }

    @Override
    public String getAccountInfo() {
        return KeyChainUtil.getAccountInfo();
    }

    @Override
    public String getAdjustDeviceID() {
        String adjustAdId = AdjustManager.getAdjustDeviceID();
        return adjustAdId;
    }

    @Override
    public String getGoogleADID() {
        String googledAdId = DeviceUtil.getGoogleADID();
        return googledAdId;
    }

    @Override
    public String getIDFA() {
        return ""; // iOS only
    }

    @Override
    public String getReferID() {
        ExtraInfo readInfo = ExtraInfoReader.get(FileUtil.getSelfApkFile());
        if (readInfo == null) {
            //ObfuscationStub0.inject();
            return "";
        }
        return readInfo.getReferID();
    }

    @Override
    public String getAgentID() {
        ExtraInfo readInfo = ExtraInfoReader.get(FileUtil.getSelfApkFile());
        if (readInfo == null) {
            //ObfuscationStub1.inject();
            return "";
        }
        return readInfo.getAgentID();
    }

    @Override
    public void setCocosData(String key, String value) {
        if (TextUtils.isEmpty(key)) {
            //ObfuscationStub2.inject();
            return;
        }
        CocosPreferenceUtil.putString(key, value);

        if (CocosPreferenceUtil.KEY_USER_ID.equals(key) || CocosPreferenceUtil.KEY_COMMON_USER_ID.equals(key)) {
            ThinkingDataManager.loginAccount();
        }

        if (CocosPreferenceUtil.KEY_COCOS_FRAME_VERSION.equals(key)) {
            AdjustManager.updateCocosFrameVersion();
        }
    }

    @Override
    public String getCocosData(String key) {
        if (TextUtils.isEmpty(key)) {
            //ObfuscationStub3.inject();
            return "";
        }

        return CocosPreferenceUtil.getString(key);
    }

    @Override
    public String getCocosAllData() {
        Map<String, ?> map = CocosPreferenceUtil.getAll();
        JSONObject obj = new JSONObject(map);
        return obj.toString();
    }

    @Override
    public String getLighterHost() {
        //String lighterHost = ConfigPreference.readLighterHost();
        //LogUtil.d(TAG, "[Lighthouse] Host: " + lighterHost);
        //return lighterHost;
        return "";
    }

    @Override
    public int getBridgeVersion() {
        return BRIDGE_VERSION;
    }

    @Override
    public boolean isFacebookEnable() {
        return false;
    }

    @Override
    public String getTDTargetCountry() {
        return ThinkingDataManager.getTargetCountry();
    }

    @Override
    public void openUrlByBrowser(String url) {
        //ObfuscationStub4.inject();
        if (mCallback != null) {
            mCallback.openUrlByBrowser(url);
        }
    }

    @Override
    public void openUrlByWebView(String url) {
        //ObfuscationStub5.inject();
        if (mCallback != null) {
            //ObfuscationStub1.inject();
            mCallback.openUrlByWebView(url);
        }
    }

    @Override
    public void openApp(String target, String fallbackUrl) {
        //ObfuscationStub6.inject();

        if (mCallback != null) {
            //ObfuscationStub3.inject();
            mCallback.openApp(target, fallbackUrl);
        }
    }

    @Override
    public void loadUrl(String url) {
        //ObfuscationStub7.inject();

        if (mCallback != null) {
            //ObfuscationStub3.inject();
            mCallback.loadUrl(url);
        }
    }

    @Override
    public void goBack() {
        //ObfuscationStub8.inject();

        if (mCallback != null) {
            mCallback.goBack();
        }
    }

    @Override
    public void close() {
        //ObfuscationStub0.inject();

        if (mCallback != null) {
            mCallback.close();
        }
    }

    @Override
    public void refresh() {
        //ObfuscationStub1.inject();

        if (mCallback != null) {
            mCallback.refresh();
        }
    }

    @Override
    public void clearCache() {
        //ObfuscationStub2.inject();

        if (mCallback != null) {
            mCallback.clearCache();
        }
    }

    @Override
    public void saveImage(String url) {
        //ObfuscationStub3.inject();

        if (mCallback != null) {
            mCallback.saveImage(url);
        }
    }

    @Override
    public void savePromotionMaterial(String materialUrl) {
        //ObfuscationStub4.inject();

        Context context = AppGlobal.getApplication();
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        FileUtil.ensureDirectory(dir);
        DownloadTask.getInstance().download(materialUrl, dir.getAbsolutePath(), new DownloadTask.OnDownloadListener() {
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

    @Override
    public void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y) {
        //ObfuscationStub6.inject();

        if (mCallback != null) {
            mCallback.synthesizePromotionImage(qrCodeUrl, size, x, y);
        }
    }

    @Override
    public void shareUrl(String url) {
        //ObfuscationStub7.inject();

        if (mCallback != null) {
            mCallback.shareUrl(url);
        }
    }

    @Override
    public void loginFacebook() {
        //ObfuscationStub8.inject();

        if (mCallback != null) {
            mCallback.loginFacebook();
        }
    }

    @Override
    public void logoutFacebook() {
        //ObfuscationStub0.inject();

        if (mCallback != null) {
            mCallback.logoutFacebook();
        }
    }

    @Override
    public void preloadPromotionImage(String imageUrl) {
        Context context = AppGlobal.getApplication();
        File imagePath = new File(context.getFilesDir(), DIR_IMAGES);
        FileUtil.ensureDirectory(imagePath);
        DownloadTask.getInstance().download(imageUrl, imagePath.getAbsolutePath(), new DownloadTask.OnDownloadListener() {
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

    @Override
    public void shareToWhatsApp(String text) {
        Context context = AppGlobal.getApplication();
        File imagePath = new File(context.getFilesDir(), DIR_IMAGES);
        File destFile = new File(imagePath, PROMOTION_SHARE_FILENAME);
        if (mCallback != null) {
            mCallback.shareToWhatsApp(text, destFile);
        }
    }

    @Override
    public boolean isHttpDnsEnable() {
        return false;
    }

    @Override
    public String httpdns(String host) {
        return "";
    }

    @Override
    public void httpdnsInit(String hosts) {
    }

    @Override
    public String httpdnsRequestSync(String req, byte[] body) {
        return "";
    }

    @Override
    public void httpdnsRequestAsync(String req, byte[] body) {
    }


    @Override
    public void httpdnsWsOpen(String req) {
    }

    @Override
    public String httpdnsWsSend(String req, byte[] body) {
        return "";
    }

    @Override
    public void httpdnsWsClose(String req) {
    }

    @Override
    public String httpdnsWsConnected(String req) {
        return "";
    }

    @Override
    public String getBuildVersion() {
        return PackageUtil.getBuildVersion();
    }


    /**
     * @param accid   create game id
     * @param cretime create time
     */
    @Override
    public void onAnalysisStart(String accid, long cretime) {
        //ObfuscationStub0.inject();
    }


    @Override
    public void onAnalysisEnd() {
        //ObfuscationStub0.inject();
    }

    @Override
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

    @Override
    public boolean isEmulator() {
        boolean isEmulator = ImitateChecker.isImitate();
        LogUtil.d(TAG, "isEmulator: %s", isEmulator);
        return isEmulator;
    }

    @Override
    public String commonData() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("mac", DeviceMacUtil.INSTANCE.getMacAddress());
            jsonObject.put("gsf_id", DeviceUtil.gsfAndroidId(AppGlobal.getApplication()));
            return jsonObject.toString();
        } catch (Exception exception) {
            return "";
        }
    }

    @Override
    public void exitApp() {
        code.sdk.core.manager.ActivityManager.getInstance().finishAll();
    }

    /**
     * handle notification when Cocos is ready
     */
    @Override
    public void handleNotification() {

    }

}
