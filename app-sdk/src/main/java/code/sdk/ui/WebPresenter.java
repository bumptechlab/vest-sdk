package code.sdk.ui;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.webkit.ValueCallback;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

import java.io.File;

import code.sdk.R;
import code.sdk.bridge.JavascriptBridge;
import code.sdk.core.Constant;
import code.sdk.core.manager.AdjustManager;
import code.sdk.core.util.CocosPreferenceUtil;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.FileUtil;
import code.sdk.core.util.IOUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.UIUtil;
import code.sdk.core.util.URLUtilX;
import code.sdk.httpdns.HttpDnsMgr;
import code.sdk.manage.OneSignalManager;
import code.sdk.manage.OneSignalNotificationListener;
import code.sdk.download.DownloadTask;
import code.sdk.common.PermissionUtils;
import code.sdk.util.PromotionImageSynthesizer;
import code.sdk.common.ShareUtil;
import code.util.AppGlobal;
import code.util.ImageUtil;
import code.util.LogUtil;

public class WebPresenter {
    public static final String TAG = WebPresenter.class.getSimpleName();

    private WebActivity mWebViewActivity;
    private String mUrl = "";
    private boolean isGame = false;
    private String mWebGlScript;
    private final int msg_check_feature = 20001;
    private final int msg_check_webView_compatibility = 20002;
    private String mImageUrl;

    private final String facebookLoginAct = "code.facebook.FacebookLoginActivity";
    public final String systemWebViewPackage = "com.google.android.webview";
    private final String miniSystemWebViewVersion = "64.0.3282.29";
    private final int miniSystemWebViewVersionCode = 328202950;
    public final String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == msg_check_feature) {
                String gameUrl = (String) msg.obj;
                handleCheckGameFeature(gameUrl);
            } else if (msg.what == msg_check_webView_compatibility) {
                handleCheckWebViewCompatibility();
            }
        }
    };


    private final JavascriptBridge.Callback mJsBridgeCb = new JavascriptBridge.Callback() {
        @Override
        public void openUrlByBrowser(String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mWebViewActivity.startActivity(intent);
            } catch (Exception e) {
            }
        }

        @Override
        public void openUrlByWebView(String url) {
            try {
                Intent intent = new Intent(mWebViewActivity, WebActivity.class);
                intent.putExtra(WebActivity.KEY_URL, url);
                mWebViewActivity.startActivity(intent);
            } catch (Exception e) {
            }
        }

        @Override
        public void openApp(String target, String fallbackUrl) {
            try {
                mWebViewActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
            } catch (Exception e) {
                e.printStackTrace();
                openUrlByBrowser(fallbackUrl);
            }
        }

        @Override
        public void loadUrl(String url) {
            UIUtil.runOnUiThread(() -> {
                mWebViewActivity.getWebView().loadUrl(url);
            });
        }

        @Override
        public void goBack() {
            UIUtil.runOnUiThread(() -> {
                if (mWebViewActivity.getWebView().canGoBack()) {
                    mWebViewActivity.getWebView().goBack();
                } else {
                    mWebViewActivity.finish();
                }
            });
        }

        @Override
        public void close() {
            UIUtil.runOnUiThread(() -> mWebViewActivity.finish());
        }

        @Override
        public void refresh() {
            UIUtil.runOnUiThread(() -> mWebViewActivity.getWebView().reload());
        }

        @Override
        public void clearCache() {
            UIUtil.runOnUiThread(() -> mWebViewActivity.getWebView().clearCache(true));
        }

        @Override
        public void saveImage(String url) {
            mImageUrl = url;
            if (PermissionUtils.checkStoragePermissions(mWebViewActivity)) {
                File dir = mWebViewActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM);
                FileUtil.ensureDirectory(dir);
                DownloadTask.getInstance().download(url, dir.getAbsolutePath(), new DownloadTask.OnDownloadListener() {
                    @Override
                    public void onDownloadSuccess(File saveFile) {
                        boolean succeed = saveFile != null && saveFile.length() > 0;
                        if (succeed) {
                            ImageUtil.triggerScanning(saveFile);
                        }
                        LogUtil.d(TAG, "saveImage - download succeed = " + succeed);
                        saveImageDone(succeed);
                    }

                    @Override
                    public void onDownloading(int progress) {
                        LogUtil.d(TAG, "saveImage - downloading = " + progress + "%");
                    }

                    @Override
                    public void onDownloadFailed() {
                        LogUtil.w(TAG, "saveImage - download failed");
                        saveImageDone(false);
                    }
                });
            } else {
                ActivityCompat.requestPermissions(mWebViewActivity, PERMISSIONS_STORAGE, mWebViewActivity.REQUEST_SAVE_IMAGE);
            }
        }

        @Override
        public void saveImageDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "saveImageDone = " + succeed);
                String script = succeed ? "Listener.send('SAVE_IMAGE_SUCCEED');" : "Listener.send('SAVE_IMAGE_FAILED');";
                mWebViewActivity.webViewEvaluatescript(script);
            });
        }

        @Override
        public void savePromotionMaterialDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "savePromotionMaterialDone = " + succeed);
                String script = succeed ? "Listener.send('SAVE_PROMOTION_MATERIAL_SUCCEED');" : "Listener.send('SAVE_PROMOTION_MATERIAL_FAILED');";
                mWebViewActivity.webViewEvaluatescript(script);
            });
        }

        @Override
        public void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y) {
            mWebViewActivity.mPromotionQrCodeUrl = qrCodeUrl;
            mWebViewActivity.mPromotionSize = size;
            mWebViewActivity.mPromotionX = x;
            mWebViewActivity.mPromotionY = y;
            if (PermissionUtils.checkStoragePermissions(mWebViewActivity)) {
                new PromotionImageSynthesizer(mWebViewActivity, qrCodeUrl, size, x, y, this).execute();
            } else {
                ActivityCompat.requestPermissions(mWebViewActivity, PERMISSIONS_STORAGE, mWebViewActivity.REQUEST_SYNTHESIZE_PROMOTION_IMAGE);
            }
        }

        @Override
        public void synthesizePromotionImageDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "synthesizePromotionImageDone = " + succeed);
                String script = succeed ? "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_SUCCEED');" : "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_FAILED');";
                mWebViewActivity.webViewEvaluatescript(script);
            });
        }

        @Override
        public void onHttpDnsHttpResponse(String requestId, String response) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "[HttpDns] onHttpDnsHttpResponse: %s", response);
                String script = String.format("Listener.send('HTTPDNS_HTTP_RESPONSE', '%s');", response);
                mWebViewActivity.webViewEvaluatescript(script);
            });
        }

        @Override
        public void onHttpDnsWsResponse(String requestId, String response) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "[HttpDns] onHttpDnsWsResponse: %s", response);
                String script = String.format("Listener.send('HTTPDNS_WS_RESPONSE', '%s');", response);
                mWebViewActivity.webViewEvaluatescript(script);
            });
        }

        @Override
        public void shareUrl(String url) {
            ShareUtil.sendText(mWebViewActivity, url);
        }

        @Override
        public void loginFacebook() {
            WebPresenter.this.loginFacebook();
        }

        @Override
        public void logoutFacebook() {
            WebPresenter.this.logoutFacebook();
        }

        @Override
        public void preloadPromotionImageDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "preloadPromotionImageDone = " + succeed);
                String script = succeed ? "Listener.send('PRELOAD_PROMOTION_IMAGE_SUCCEED');" : "Listener.send('PRELOAD_PROMOTION_IMAGE_FAILED');";
                mWebViewActivity.webViewEvaluatescript(script);
            });
        }

        @Override
        public void shareToWhatsApp(String text, File file) {
            ShareUtil.shareToWhatsApp(mWebViewActivity, text, file);
        }
    };

    private OneSignalNotificationListener mOneSignalNotificationListener = new OneSignalNotificationListener() {
        @Override
        public void onOpenNotification(JSONObject data) {
            UIUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LogUtil.d(TAG, "[OneSignal] Notification send to WebView: %s", data);
                    if (data != null && data.has("open_dialog")) {
                        JSONObject openDialogData = data.optJSONObject("open_dialog");
                        if (openDialogData != null) {
                            String script = String.format("Listener.send('OPEN_DIALOG', '%s');", openDialogData);
                            mWebViewActivity.webViewEvaluatescript(script);
                            LogUtil.d(TAG, "[OneSignal] evaluateJavascript: %s", script);
                        }
                    }
                }
            });
        }
    };


    public WebPresenter(WebActivity activity) {
        this.mWebViewActivity = activity;
    }

    public void init(boolean isGame, String url) {
        this.isGame = isGame;
        this.mUrl = url;
        HttpDnsMgr.init(AppGlobal.getApplication());
        mWebGlScript = IOUtil.readRawContent(mWebViewActivity.getBaseContext(), R.raw.webgl_script);
        LogUtil.d(TAG, "WebGlScript=" + mWebGlScript);
        JavascriptBridge jsBridge = new JavascriptBridge();
        jsBridge.setCallback(mJsBridgeCb);

        mWebViewActivity.getWebView().addJavascriptInterface(jsBridge, "jsBridge");

        Uri uri = Uri.parse(mUrl);
        // hover menu
        mWebViewActivity.setShowHoverMenu(uri.getBooleanQueryParameter(Constant.QUERY_PARAM_HOVER_MENU, false));
        // nav bar
        mWebViewActivity.setShowNavBar(uri.getBooleanQueryParameter(Constant.QUERY_PARAM_NAV_BAR, false));
        // save cutout
        mWebViewActivity.setSafeCutout(uri.getBooleanQueryParameter(Constant.QUERY_PARAM_SAFE_CUTOUT, false));
        // screen orientation
        String screenOrientation = uri.getQueryParameter(Constant.QUERY_PARAM_ORIENTATION);
        if (Constant.PORTRAIT.equals(screenOrientation) || Constant.UNSPECIFIED.equals(screenOrientation)) {
            mWebViewActivity.setScreenOrientation(screenOrientation);
        }

        if (isGame) {
            AdjustManager.trackEventAccess(null);
            OneSignalManager.setOneSignalNotificationListener(mOneSignalNotificationListener);
            OneSignalManager.init(mWebViewActivity);
            //等OneSignal初始化完成才能执行setup
            UIUtil.runOnUiThreadDelay(new Runnable() {
                @Override
                public void run() {
                    OneSignalManager.showPrompt();
                    OneSignalManager.setup();
                }
            }, 3000);
        }
    }


    /* Facebook Login START */
    private void loginFacebook() {
        launchFacebookSDK(true);
    }

    private void logoutFacebook() {
        launchFacebookSDK(false);
    }

    private void launchFacebookSDK(boolean isLogin) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClassName(mWebViewActivity.getBaseContext(), facebookLoginAct);
            intent.putExtra("is_login", isLogin);
            intent.putExtra("app_id", ConfigPreference.readFacebookAppId());
            intent.putExtra("client_token", ConfigPreference.readFacebookClientToken());
            mWebViewActivity.startActivityForResult(intent, mWebViewActivity.REQUEST_CODE_LOGIN_FACEBOOK);
            mWebViewActivity.overridePendingTransition(0, 0);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "FacebookLoginActivity not found, please check the class name: " + facebookLoginAct);
        }
    }

    public void notifyFacebookLoginResult(String result) {
        LogUtil.d(TAG, "Notify facebook login result: " + result);
        String script = String.format("Listener.send('FACEBOOK_LOGIN_RESULT', '%s');", result);
        mWebViewActivity.webViewEvaluatescript(script);
    }

    public void notifyFacebookLogoutResult(String result) {
        LogUtil.d(TAG, "Notify facebook logout result: " + result);
        String script = String.format("Listener.send('FACEBOOK_LOGOUT_RESULT', '%s');", result);
        mWebViewActivity.webViewEvaluatescript(script);
    }
    /* Facebook Login END */

    /**
     * 检查游戏特征值包括(延迟1min检查)
     * _int_chn
     * _int_brand_code
     */
    public void checkGameFeature(String gameUrl) {
        if (isGame) {
            mHandler.removeMessages(msg_check_feature);
            Message msg = Message.obtain();
            msg.what = msg_check_feature;
            msg.obj = gameUrl;
            mHandler.sendMessageDelayed(msg, 60000);
        }
    }

    public void clearGameFeature() {
        if (isGame) {
            CocosPreferenceUtil.putString(CocosPreferenceUtil.KEY_INT_CHN, "");
            CocosPreferenceUtil.putString(CocosPreferenceUtil.KEY_INT_BRAND_CODE, "");
        }
    }

    private void handleCheckGameFeature(String gameUrl) {
        String chn = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_INT_CHN);
        String brandCode = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_INT_BRAND_CODE);
        LogUtil.d(TAG, "check game feature: %s=%s, %s=%s", CocosPreferenceUtil.KEY_INT_CHN, chn, CocosPreferenceUtil.KEY_INT_BRAND_CODE, brandCode);
        if (TextUtils.isEmpty(chn) && TextUtils.isEmpty(brandCode)) {//特征值为空，不符合游戏特征(判定为不是我们自己的游戏)
            //当前加载的url跟缓存的url一样，并且不符合游戏特征，则清除掉缓存url
            String cacheGameUrl = PreferenceUtil.readGameUrl();
            LogUtil.d(TAG, "check game feature: is not validate game page: %s", gameUrl);
            if (isSameBaseUrl(gameUrl, cacheGameUrl)) {
                LogUtil.d(TAG, "check game feature: clear cached url: %s", cacheGameUrl);
                PreferenceUtil.saveGameUrl(null);
            } else {
                LogUtil.d(TAG, "check game feature: abort clearing cached url: %s", cacheGameUrl);
            }
        } else {
            LogUtil.d(TAG, "check game feature: is validate game page: %s", gameUrl);
        }
    }

    /* Check WebView Compatibility START */

    /**
     * 检查WebView兼容性（两个方面：WebGl和Android System WebView版本）
     */
    public void checkWebViewCompatibility() {
        if (isGame) {
            mHandler.removeMessages(msg_check_webView_compatibility);
            mHandler.sendEmptyMessageDelayed(msg_check_webView_compatibility, 5000);
        }
    }

    public void jsBrSaveImage() {
        mJsBridgeCb.saveImage(mImageUrl);
    }

    public void jsBrSaveImageDone(Boolean succeed) {
        mJsBridgeCb.saveImageDone(succeed);
    }

    public void jsBrSynthesizePromotionImageDone(boolean succeed) {
        mJsBridgeCb.synthesizePromotionImageDone(succeed);
    }

    public JavascriptBridge.Callback getJsBridge() {
        return mJsBridgeCb;
    }

    private void handleCheckWebViewCompatibility() {
        if (TextUtils.isEmpty(mWebGlScript)) {
            LogUtil.d(TAG, "abort checking WebView compatibility, WebGlScript is empty");
            return;
        }
        LogUtil.d(TAG, "start checking WebView compatibility...");
        boolean isWebViewCompatible = isWebViewCompatible();
        mWebViewActivity.getWebView().evaluateJavascript(mWebGlScript, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {//js与native交互的回调函数
                LogUtil.d(TAG, "isWebGlEnable = " + value);
                if ("false".equals(value) || !isWebViewCompatible) {
                    UIUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebViewActivity.showWebViewUpdateDialog();
                        }
                    });
                }
            }
        });
    }

    /**
     * 游戏需要的Android System WebView最小版本是：64.0.3282.29 - 328202950
     * 小于该版本，内核会报以下错误：
     * Uncaught SyntaxError: Invalid regular expression: /(?<bundle>.+)UiLanguage/:
     *
     * @return
     */
    private boolean isWebViewCompatible() {
        PackageInfo packageInfo = DeviceUtil.getPackageInfo(mWebViewActivity.getBaseContext(), systemWebViewPackage);
        boolean compatible = false;
        if (packageInfo != null) {
            if (packageInfo.versionCode >= miniSystemWebViewVersionCode) {
                compatible = true;
                LogUtil.i(TAG, "[Android System WebView] version: %s(%d) compatible for cocos game",
                        packageInfo.versionName, packageInfo.versionCode);
            } else {
                LogUtil.e(TAG, "[Android System WebView] version: %s(%d) not compatible for cocos game, need upgrade to %s(%d) or higher",
                        packageInfo.versionName, packageInfo.versionCode,
                        miniSystemWebViewVersion,
                        miniSystemWebViewVersionCode);
            }
        } else {
            LogUtil.i(TAG, "[Android System WebView] not installed");
        }
        return compatible;
    }

    private boolean isSameBaseUrl(String url1, String url2) {
        if (TextUtils.isEmpty(url1) || TextUtils.isEmpty(url2)) {
            return false;
        }
        String baseUrl1 = URLUtilX.getBaseUrl(url1);
        String baseUrl2 = URLUtilX.getBaseUrl(url2);
        if (!baseUrl1.endsWith("/")) {
            baseUrl1 = baseUrl1 + "/";
        }
        if (!baseUrl2.endsWith("/")) {
            baseUrl2 = baseUrl2 + "/";
        }
        return baseUrl1.equals(baseUrl2);
    }

    public void onDestroy() {
        mHandler.removeMessages(msg_check_webView_compatibility);
        mHandler.removeMessages(msg_check_feature);
    }
}
