package code.sdk.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ClientCertRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

import cocos.creator.libwebview.CocosWebView;
import cocos.creator.libwebview.loader.LocalAssetLoader;
import code.sdk.BuildConfig;
import code.sdk.R;
import code.sdk.analysis.AnalysisWatcher;
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
import code.sdk.httpdns.HttpDnsMgr;
import code.sdk.manager.OneSignalManager;
import code.sdk.network.download.DownloadTask;
import code.sdk.receiver.NetworkReceiver;
import code.sdk.util.AndroidBug5497Workaround;
import code.sdk.util.PromotionImageSynthesizer;
import code.sdk.util.ShareUtil;
import code.sdk.core.util.URLUtilX;
import code.sdk.widget.HoverMenu;
import code.util.AppGlobal;
import code.util.ImageUtil;
import code.util.LogUtil;


/**
 * @note webview activity
 */
public class WebViewActivity extends BaseActivity {
    public static final String TAG = WebViewActivity.class.getSimpleName();
    public static final String KEY_URL = "key_url";
    public static final String KEY_GAME = "key_game";

    private static final int REQUEST_SAVE_IMAGE = 100;
    private static final int REQUEST_SYNTHESIZE_PROMOTION_IMAGE = 101;
    private static final int REQUEST_CODE_LOGIN_FACEBOOK = 102;
    private static final int REQUEST_CODE_FILE_CHOOSER = 103;
    private static final String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String FACEBOOK_LOGIN_ACTIVITY = "code.facebook.FacebookLoginActivity";
    private static final int MSG_CHECK_GAME_FEATURE = 1001;
    private static final int MSG_CHECK_WEBVIEW_COMPATIBILITY = 1002;
    private static final String PACKAGE_ANDROID_SYSTEM_WEBVIEW = "com.google.android.webview";
    private static final String MINI_ANDROID_SYSTEM_WEBVIEW_VERSION_NAME = "64.0.3282.29";
    private static final int MINI_ANDROID_SYSTEM_WEBVIEW_VERSION_CODE = 328202950;

    private String mUrl = "";
    private boolean isGame = false;
    private boolean mShowHoverMenu = false;
    private boolean mShowNavBar = false;
    private boolean mSafeCutout = false;
    @Constant.ScreenOrientation
    private String mScreenOrientation = Constant.LANDSCAPE;
    //游戏时长统计观察者
    private AnalysisWatcher mWatcher;

    private CocosWebView mWebView;
    private View mErrorLayout;
    private View mRefreshButton;
    private View mLoadingLayout;
    private HoverMenu mHoverMenu;
    private AlertDialog mWebViewUpdateDialog;

    private boolean mLoadingError;
    private long mLastBackTs;

    private ValueCallback mUploadMessage;
    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            LogUtil.w(TAG, "onReceivedTitle: " + title);
            if (!mLoadingError) {
                mLoadingError = titleError(title);
            }
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            LogUtil.d(TAG, "onProgressChanged: " + newProgress);
        }

        // For [4.1, 5.0)
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            String type = TextUtils.isEmpty(acceptType) ? "*/*" : acceptType;
            i.setType(type);
            startActivityForResult(Intent.createChooser(i, "File Chooser"), REQUEST_CODE_FILE_CHOOSER);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            mUploadMessage = filePathCallback;
            Intent intent = new Intent(Intent.ACTION_PICK);
            if (fileChooserParams != null && fileChooserParams.getAcceptTypes() != null && fileChooserParams.getAcceptTypes().length > 0) {
                intent.setType(String.join(",", fileChooserParams.getAcceptTypes()));
            } else {
                intent.setType("*/*");
            }
            startActivityForResult(Intent.createChooser(intent, "File Chooser"), REQUEST_CODE_FILE_CHOOSER);
            return true;
        }
    };
    private NetworkReceiver mNetworkReceiver;
    private String mImageUrl;
    private String mPromotionQrCodeUrl;
    private String mWebGlScript;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_CHECK_GAME_FEATURE) {
                String gameUrl = (String) msg.obj;
                handleCheckGameFeature(gameUrl);
            } else if (msg.what == MSG_CHECK_WEBVIEW_COMPATIBILITY) {
                handleCheckWebViewCompatibility();
            }
        }
    };
    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LogUtil.d(TAG, "shouldOverrideUrlLoading: %s", url);
            if (!(url.startsWith("http") || url.startsWith("https"))) {
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            LogUtil.d(TAG, "shouldInterceptRequest[%s]: %s", request.getMethod(), request.getUrl());
//            InputStream stream = null;
//            if (mWebView.isCocosEngineUrl(request.getUrl())) {
//                stream = AssetsUtils.getEncryptFileStream(WebViewActivity.this);
//            }
//            else {
//                //对静态资源进行HttpDns域名解析(网络请求太慢，暂停使用)
//                if ("GET".equalsIgnoreCase(request.getMethod())) {
//                    stream = HttpDnsMgr.doHttpGetSync(request.getUrl().toString());
//                }
//            }
//            return mWebView.shouldInterceptUrlEvent(request.getUrl(), stream);
            return LocalAssetLoader.getInstance(WebViewActivity.this).shouldInterceptRequest(request.getUrl());
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            LogUtil.d(TAG, "shouldInterceptRequest: %s", url);
//            InputStream stream = null;
//            if (mWebView.isCocosEngineUrl(Uri.parse(url))) {
//                stream = AssetsUtils.getEncryptFileStream(WebViewActivity.this);
//            }
//            else {
//                stream = HttpDnsMgr.doHttpGetSync(url);
//            }
//            return mWebView.shouldInterceptUrlEvent(Uri.parse(url), stream);
            return LocalAssetLoader.getInstance(WebViewActivity.this).shouldInterceptRequest(Uri.parse(url));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mLoadingError = false;
            clearGameFeature();
            LogUtil.d(TAG, "onPageStarted: %s", url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            String title = view.getTitle();
            if (!mLoadingError) {
                mLoadingError = titleError(title);
            }
            LogUtil.d(TAG, "onPageFinished: %s, title: %s,  loadingError: %s", url, title, mLoadingError);
            mLoadingLayout.setVisibility(View.INVISIBLE);
            if (mLoadingError) {
                mErrorLayout.setVisibility(View.VISIBLE);
                mWebView.setVisibility(View.INVISIBLE);
            } else {
                mErrorLayout.setVisibility(View.INVISIBLE);
                mWebView.setVisibility(View.VISIBLE);
                // request focus ahead in case of any input behavior
                mWebView.requestFocus();
                mWebView.requestFocusFromTouch();
            }
            LogUtil.d(TAG, "Loading layout visibility: " + mLoadingLayout.getVisibility());
            checkGameFeature(url);
            checkWebViewCompatibility();
            if (mLoadingError) {
                registerNetworkListener();
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            mLoadingError = true;
            LogUtil.e(TAG, "onReceivedError: %d, description: %s", errorCode, description);
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            LogUtil.e(TAG, "onReceivedSslError: %s", error.getUrl());
            handler.proceed();
        }


        @Override
        public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
            super.onReceivedClientCertRequest(view, request);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LogUtil.d(TAG, "onReceivedClientCertRequest: %s", request.getHost());
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            LogUtil.d(TAG, "onLoadResource: %s", url);
        }
    };

    private boolean titleError(String title) {
        if (!TextUtils.isEmpty(title) && (title.contains("404") || title.contains("500") || title.toLowerCase().contains("error"))) {
            return true;
        }
        return false;
    }

    private int mPromotionSize;
    private int mPromotionX;
    private int mPromotionY;
    private final JavascriptBridge.Callback mJsBridgeCb = new JavascriptBridge.Callback() {
        @Override
        public void openUrlByBrowser(String url) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                WebViewActivity.this.startActivity(intent);
            } catch (Exception e) {
            }
        }

        @Override
        public void openUrlByWebView(String url) {
            try {
                Intent intent = new Intent(WebViewActivity.this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.KEY_URL, url);
                WebViewActivity.this.startActivity(intent);
            } catch (Exception e) {
            }
        }

        @Override
        public void openApp(String target, String fallbackUrl) {
            try {
                WebViewActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
            } catch (Exception e) {
                e.printStackTrace();
                openUrlByBrowser(fallbackUrl);
            }
        }

        @Override
        public void loadUrl(String url) {
            UIUtil.runOnUiThread(() -> {
                mWebView.loadUrl(url);
            });
        }

        @Override
        public void goBack() {
            UIUtil.runOnUiThread(() -> {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    finish();
                }
            });
        }

        @Override
        public void close() {
            UIUtil.runOnUiThread(() -> finish());
        }

        @Override
        public void refresh() {
            UIUtil.runOnUiThread(() -> mWebView.reload());
        }

        @Override
        public void clearCache() {
            UIUtil.runOnUiThread(() -> mWebView.clearCache(true));
        }

        @Override
        public void saveImage(String url) {
            mImageUrl = url;
            int permission = ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                FileUtil.ensureDirectory(dir);
                DownloadTask.download(url, dir.getAbsolutePath(), new DownloadTask.OnDownloadListener() {
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
                ActivityCompat.requestPermissions(WebViewActivity.this, PERMISSIONS_STORAGE, REQUEST_SAVE_IMAGE);
            }
        }

        @Override
        public void saveImageDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "saveImageDone = " + succeed);
                String script = succeed ? "Listener.send('SAVE_IMAGE_SUCCEED');" : "Listener.send('SAVE_IMAGE_FAILED');";
                mWebView.evaluateJavascript(script, null);
            });
        }

        @Override
        public void savePromotionMaterialDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "savePromotionMaterialDone = " + succeed);
                String script = succeed ? "Listener.send('SAVE_PROMOTION_MATERIAL_SUCCEED');" : "Listener.send('SAVE_PROMOTION_MATERIAL_FAILED');";
                mWebView.evaluateJavascript(script, null);
            });
        }

        @Override
        public void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y) {
            mPromotionQrCodeUrl = qrCodeUrl;
            mPromotionSize = size;
            mPromotionX = x;
            mPromotionY = y;
            int permission = ContextCompat.checkSelfPermission(WebViewActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                new PromotionImageSynthesizer(qrCodeUrl, size, x, y, this).execute();
            } else {
                ActivityCompat.requestPermissions(WebViewActivity.this, PERMISSIONS_STORAGE, REQUEST_SYNTHESIZE_PROMOTION_IMAGE);
            }
        }

        @Override
        public void synthesizePromotionImageDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "synthesizePromotionImageDone = " + succeed);
                String script = succeed ? "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_SUCCEED');" : "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_FAILED');";
                mWebView.evaluateJavascript(script, null);
            });
        }

        @Override
        public void onHttpDnsHttpResponse(String requestId, String response) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "[HttpDns] onHttpDnsHttpResponse: %s", response);
                String script = String.format("Listener.send('HTTPDNS_HTTP_RESPONSE', '%s');", response);
                mWebView.evaluateJavascript(script, null);
            });
        }

        @Override
        public void onHttpDnsWsResponse(String requestId, String response) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "[HttpDns] onHttpDnsWsResponse: %s", response);
                String script = String.format("Listener.send('HTTPDNS_WS_RESPONSE', '%s');", response);
                mWebView.evaluateJavascript(script, null);
            });
        }

        @Override
        public void shareUrl(String url) {
            ShareUtil.sendText(WebViewActivity.this, url);
        }

        @Override
        public void loginFacebook() {
            WebViewActivity.this.loginFacebook();
        }

        @Override
        public void logoutFacebook() {
            WebViewActivity.this.logoutFacebook();
        }

        @Override
        public void preloadPromotionImageDone(boolean succeed) {
            UIUtil.runOnUiThread(() -> {
                LogUtil.d(TAG, "preloadPromotionImageDone = " + succeed);
                String script = succeed ? "Listener.send('PRELOAD_PROMOTION_IMAGE_SUCCEED');" : "Listener.send('PRELOAD_PROMOTION_IMAGE_FAILED');";
                mWebView.evaluateJavascript(script, null);
            });
        }

        @Override
        public void shareToWhatsApp(String text, File file) {
            ShareUtil.shareToWhatsApp(WebViewActivity.this, text, file);
        }

        @Override
        public void onStart(String accid, long cretime) {
            if (null != mWatcher) {
                mWatcher.bindAccountInfo(accid, cretime);
                mWatcher.onStart();
            }
        }

        @Override
        public void onEnd() {
            if (null != mWatcher) mWatcher.onEnd();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        isGame = intent.getBooleanExtra(KEY_GAME, false);
        mUrl = intent.getStringExtra(KEY_URL);
        LogUtil.d(TAG, "open url: %s", mUrl);
        if (TextUtils.isEmpty(mUrl)) {
            finish();
            return;
        }
        HttpDnsMgr.init(AppGlobal.getApplication());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        initParameters();
        mErrorLayout = findViewById(R.id.layout_error);
        mRefreshButton = findViewById(R.id.error_refresh_button);
        mRefreshButton.setOnClickListener(view -> {
            mLoadingLayout.setVisibility(View.VISIBLE);
            mWebView.reload();
        });
        mLoadingLayout = findViewById(R.id.layout_loading);

        mWebView = findViewById(R.id.web_view);
        mWebView.initWebView(mWebViewClient, mWebChromeClient);
        initJavaScriptBridge();
        mWebView.loadUrl(mUrl);

        mWatcher = new AnalysisWatcher(this.getApplicationContext());
        mWatcher.clearCache();

        parseQueryParameters();

        // fullscreen webview activity can NOT use adjustPan/adjustResize input mode
        AndroidBug5497Workaround.assistActivity(this);

        if (isGame) {
            AdjustManager.trackEventAccess(null);
            OneSignalManager.init(this);
            OneSignalManager.showPrompt();
            OneSignalManager.setup();
        }
    }

    private void initParameters() {
        mWebGlScript = IOUtil.readRawContent(getBaseContext(), R.raw.webgl_script);
        LogUtil.d(TAG, "WebGlScript=" + mWebGlScript);
    }

    private void parseQueryParameters() {
        Uri uri = Uri.parse(mUrl);
        // hover menu
        mShowHoverMenu = uri.getBooleanQueryParameter(Constant.QUERY_PARAM_HOVER_MENU, false);
        // nav bar
        mShowNavBar = uri.getBooleanQueryParameter(Constant.QUERY_PARAM_NAV_BAR, false);
        // save cutout
        mSafeCutout = uri.getBooleanQueryParameter(Constant.QUERY_PARAM_SAFE_CUTOUT, false);
        // screen orientation
        String screenOrientation = uri.getQueryParameter(Constant.QUERY_PARAM_ORIENTATION);
        if (Constant.PORTRAIT.equals(screenOrientation) || Constant.UNSPECIFIED.equals(screenOrientation)) {
            mScreenOrientation = screenOrientation;
        }
    }

    private void initJavaScriptBridge() {
        JavascriptBridge jsBridge = new JavascriptBridge();
        jsBridge.setCallback(mJsBridgeCb);

        mWebView.addJavascriptInterface(jsBridge, "jsBridge");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SAVE_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mJsBridgeCb.saveImage(mImageUrl);
            } else {
                LogUtil.d(TAG, "saveImage - download succeed = " + false);
                mJsBridgeCb.saveImageDone(false);
            }
        } else if (requestCode == REQUEST_SYNTHESIZE_PROMOTION_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new PromotionImageSynthesizer(mPromotionQrCodeUrl, mPromotionSize, mPromotionX, mPromotionY, mJsBridgeCb).execute();
            } else {
                mJsBridgeCb.synthesizePromotionImageDone(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // hover menu
        if (mShowHoverMenu) {
            showHoverMenu();
        } else {
            hideHoverMenu();
        }
        // nav bar
        if (mShowNavBar) {
            showNavBar();
        } else {
            hideNavBar();
        }
        // safe cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Window window = getWindow();
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            if (mSafeCutout) {
                //永远不允许应用程序的内容延伸到刘海区域
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            } else {
                //不管手机处于横屏还是竖屏模式，都允许应用程序的内容延伸到刘海区域
                layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            window.setAttributes(layoutParams);
        }

        // orientation
        switch (mScreenOrientation) {
            case Constant.PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Constant.LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
        }

        mWebView.onResume();
        mWebView.evaluateJavascript("typeof onGameResume === 'function' && onGameResume();", null);
        if (null != mWatcher) mWatcher.onResume();
    }

    private void showHoverMenu() {
        if (mHoverMenu == null) {
            mHoverMenu = new HoverMenu(this);
            mHoverMenu.setMenuListener(new HoverMenu.OnMenuClickListener() {
                @Override
                public void onRefresh() {
                    mLoadingLayout.setVisibility(View.VISIBLE);
                    mWebView.reload();
                }

                @Override
                public void onClose() {
                    finish();
                }
            });
            mHoverMenu.show();
        }
    }

    private void hideHoverMenu() {
        if (mHoverMenu != null) {
            mHoverMenu.hide();
        }
    }

    private void showNavBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void hideNavBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide navigation bar
        );
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mHoverMenu != null) {
            mHoverMenu.resetPositionDelayed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            if (mUploadMessage == null) {
                return;
            }
            if (resultCode != RESULT_OK || data == null) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri[] uriArray;
                String dataString = data.getDataString();
                if (!TextUtils.isEmpty(dataString)) {
                    uriArray = new Uri[]{Uri.parse(dataString)};
                    mUploadMessage.onReceiveValue(uriArray);
                    return;
                }
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    uriArray = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        uriArray[i] = item.getUri();
                    }
                    mUploadMessage.onReceiveValue(uriArray);
                    return;
                }
                mUploadMessage.onReceiveValue(null);
            } else {
                mUploadMessage.onReceiveValue(data.getData());
            }

            mUploadMessage = null;
        } else if (requestCode == REQUEST_CODE_LOGIN_FACEBOOK) {
            if (resultCode == RESULT_OK) {
                boolean isLogin = data.getBooleanExtra("is_login", false);
                String result = data.getStringExtra("result");
                if (isLogin) {
                    LogUtil.d(TAG, "Facebook login complete...");
                    notifyFacebookLoginResult(result);
                } else {
                    LogUtil.d(TAG, "Facebook logout complete...");
                    notifyFacebookLogoutResult(result);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (null != mWatcher) mWatcher.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_CHECK_WEBVIEW_COMPATIBILITY);
        mHandler.removeMessages(MSG_CHECK_GAME_FEATURE);
        mWebView.removeJavascriptInterface("jsBridge");
        mWebView.destroy();
        mWatcher.onDestroy();
        if (mNetworkReceiver != null) {
            unregisterReceiver(mNetworkReceiver);
        }
    }

    /* Facebook Login START */
    private void loginFacebook() {
        if (!BuildConfig.FACEBOOK_ENABLE) {
            LogUtil.w(TAG, "Facebook login disable...");
            return;
        }
        launchFacebookSDK(true);
    }

    private void logoutFacebook() {
        if (!BuildConfig.FACEBOOK_ENABLE) {
            LogUtil.w(TAG, "Facebook logout disable...");
            return;
        }
        launchFacebookSDK(false);
    }

    private void launchFacebookSDK(boolean isLogin) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setClassName(getBaseContext(), FACEBOOK_LOGIN_ACTIVITY);
            intent.putExtra("is_login", isLogin);
            intent.putExtra("app_id", ConfigPreference.readFacebookAppId());
            intent.putExtra("client_token", ConfigPreference.readFacebookClientToken());
            startActivityForResult(intent, REQUEST_CODE_LOGIN_FACEBOOK);
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "FacebookLoginActivity not found, please check the class name: " + FACEBOOK_LOGIN_ACTIVITY);
        }
    }

    private void notifyFacebookLoginResult(String result) {
        LogUtil.d(TAG, "Notify facebook login result: " + result);
        String script = String.format("Listener.send('FACEBOOK_LOGIN_RESULT', '%s');", result);
        mWebView.evaluateJavascript(script, null);
    }

    private void notifyFacebookLogoutResult(String result) {
        LogUtil.d(TAG, "Notify facebook logout result: " + result);
        String script = String.format("Listener.send('FACEBOOK_LOGOUT_RESULT', '%s');", result);
        mWebView.evaluateJavascript(script, null);
    }
    /* Facebook Login END */

    /**
     * 检查游戏特征值包括(延迟1min检查)
     * _int_chn
     * _int_brand_code
     */
    private void checkGameFeature(String gameUrl) {
        if (isGame) {
            mHandler.removeMessages(MSG_CHECK_GAME_FEATURE);
            Message msg = Message.obtain();
            msg.what = MSG_CHECK_GAME_FEATURE;
            msg.obj = gameUrl;
            mHandler.sendMessageDelayed(msg, 60000);
        }
    }

    private void clearGameFeature() {
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
            if (URLUtilX.isSameBaseUrl(gameUrl, cacheGameUrl)) {
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
    private void checkWebViewCompatibility() {
        if (isGame) {
            mHandler.removeMessages(MSG_CHECK_WEBVIEW_COMPATIBILITY);
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_WEBVIEW_COMPATIBILITY, 5000);
        }
    }

    private void handleCheckWebViewCompatibility() {
        if (TextUtils.isEmpty(mWebGlScript)) {
            LogUtil.d(TAG, "abort checking WebView compatibility, WebGlScript is empty");
            return;
        }
        LogUtil.d(TAG, "start checking WebView compatibility...");
        boolean isWebViewCompatible = isWebViewCompatible();
        mWebView.evaluateJavascript(mWebGlScript, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {//js与native交互的回调函数
                LogUtil.d(TAG, "isWebGlEnable = " + value);
                if ("false".equals(value) || !isWebViewCompatible) {
                    UIUtil.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWebViewUpdateDialog();
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
        PackageInfo packageInfo = DeviceUtil.getPackageInfo(getBaseContext(), PACKAGE_ANDROID_SYSTEM_WEBVIEW);
        boolean compatible = false;
        if (packageInfo != null) {
            if (packageInfo.versionCode >= MINI_ANDROID_SYSTEM_WEBVIEW_VERSION_CODE) {
                compatible = true;
                LogUtil.i(TAG, "[Android System WebView] version: %s(%d) compatible for cocos game",
                        packageInfo.versionName, packageInfo.versionCode);
            } else {
                LogUtil.e(TAG, "[Android System WebView] version: %s(%d) not compatible for cocos game, need upgrade to %s(%d) or higher",
                        packageInfo.versionName, packageInfo.versionCode,
                        MINI_ANDROID_SYSTEM_WEBVIEW_VERSION_NAME,
                        MINI_ANDROID_SYSTEM_WEBVIEW_VERSION_CODE);
            }
        } else {
            LogUtil.i(TAG, "[Android System WebView] not installed");
        }
        return compatible;
    }

    private void showWebViewUpdateDialog() {
        if (isFinishing()) {
            return;
        }
        if (!PreferenceUtil.readShowWebViewUpdateDialog()) {
            return;
        }
        if (null != mWebViewUpdateDialog && mWebViewUpdateDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(WebViewActivity.this, R.style.AppDialog);
        builder.setMessage(R.string.msg_update_android_system_webview);
        builder.setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissWebViewUpdateDialog();
                DeviceUtil.openMarket(getBaseContext(), PACKAGE_ANDROID_SYSTEM_WEBVIEW);
            }
        });
        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissWebViewUpdateDialog();
            }
        });
        builder.setNeutralButton(R.string.btn_never_ask, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissWebViewUpdateDialog();
                PreferenceUtil.saveShowWebViewUpdateDialog(false);
            }
        });
        mWebViewUpdateDialog = builder.create();
        mWebViewUpdateDialog.show();
    }

    private void dismissWebViewUpdateDialog() {
        if (null != mWebViewUpdateDialog && mWebViewUpdateDialog.isShowing()) {
            try {
                mWebViewUpdateDialog.dismiss();
                mWebViewUpdateDialog = null;
            } catch (Exception e) {
            }
        }
    }
    /* WebView Compatibility END */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mLastBackTs) > 2000) {
                Toast.makeText(this, R.string.one_more_click_go_back, Toast.LENGTH_SHORT).show();
                mLastBackTs = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void registerNetworkListener() {
        if (mNetworkReceiver != null) {
            return;
        }
        mNetworkReceiver = new NetworkReceiver(new NetworkReceiver.NetworkStateListener() {
            @Override
            public void onNetworkConnected() {
                mLoadingLayout.setVisibility(View.VISIBLE);
                mWebView.reload();
            }

            @Override
            public void onNetworkDisconnected() {
            }
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

}
