package code.sdk.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import code.sdk.R;
import code.sdk.SdkInit;
import code.sdk.base.BaseWebActivity;
import code.sdk.bridge.JsBridge;
import code.sdk.command.AssetLoaderManager;
import code.sdk.core.Constant;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.NetworkUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.util.AndroidBug5497Workaround;
import code.sdk.util.PromotionImageSynthesizer;
import code.util.LogUtil;


/**
 * @note webview activity
 */
public class WebActivity extends BaseWebActivity {
    public static final String TAG = WebActivity.class.getSimpleName();
    public static final String KEY_URL = "key_path_url_value";
    public static final String KEY_GAME = "key_is_game_value";
    private String mUrl = "";
    private boolean isGame = false;

    public final int REQUEST_SAVE_IMAGE = 100;
    public final int REQUEST_SYNTHESIZE_PROMOTION_IMAGE = 10001;
    public final int REQUEST_CODE_FILE_CHOOSER = 10003;


    private boolean mShowHoverMenu = false;
    private boolean mShowNavBar = false;
    private boolean mSafeCutout = false;
    @Constant.ScreenOrientation
    private String mScreenOrientation = Constant.LANDSCAPE;

    private WebView mWebView;
    private View mErrorLayout;
    private View mRefreshButton;
    private View mLoadingLayout;
    private FunctionMenu mHoverMenu;
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
    public String mPromotionQrCodeUrl;

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
            return AssetLoaderManager.getInstance(WebActivity.this).shouldInterceptRequest(request.getUrl());
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            LogUtil.d(TAG, "shouldInterceptRequest: %s", url);
            return AssetLoaderManager.getInstance(WebActivity.this).shouldInterceptRequest(Uri.parse(url));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            LogUtil.d(TAG, "onPageStarted: %s", url);
            doPageStart();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            doPageFinish(view, url);
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
            //handler.proceed();
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

    public int mPromotionSize;
    public int mPromotionX;
    public int mPromotionY;

    private WebPresenter mWebPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SdkInit.mContext = this;
        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        isGame = intent.getBooleanExtra(KEY_GAME, false);
        String url = intent.getStringExtra(KEY_URL);
        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }
        mUrl = JsBridge.formatUrlWithJsb(url);
        LogUtil.d(TAG, "open url: %s", mUrl);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
        setContentView(R.layout.activity_webview);
        mErrorLayout = findViewById(R.id.layout_error);
        mRefreshButton = findViewById(R.id.error_refresh_button);
        mRefreshButton.setOnClickListener(view -> {
            mLoadingLayout.setVisibility(View.VISIBLE);
            mWebView.reload();
        });
        mLoadingLayout = findViewById(R.id.layout_loading);

        mWebView = findViewById(R.id.web_view);
//        mWebView.initWebView(mWebViewClient, mWebChromeClient);
        setWebView();

        // fullscreen webview activity can NOT use adjustPan/adjustResize input mode
        AndroidBug5497Workaround.assistActivity(this);
        mWebPresenter = new WebPresenter(WebActivity.this);
        mWebPresenter.init(isGame, mUrl);
        mWebView.loadUrl(mUrl);
    }

    private void doPageStart() {
        mLoadingError = false;
        mWebPresenter.clearGameFeature();
    }

    private void doPageFinish(WebView view, String url) {
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
        mWebPresenter.checkGameFeature(url);
        mWebPresenter.checkWebViewCompatibility();
        if (mLoadingError) {
            registerNetworkListener();
        }
    }

    private void setWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);//支持自动适配
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);//支持通过js打开新窗口

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setWebChromeClient(mWebChromeClient);

    }


    public void setShowHoverMenu(boolean showHoverMenu) {
        this.mShowHoverMenu = showHoverMenu;
    }

    public void setShowNavBar(boolean showNavBar) {
        this.mShowNavBar = showNavBar;
    }

    public void setSafeCutout(boolean safeCutout) {
        this.mSafeCutout = safeCutout;
    }

    public WebView getWebView() {
        return this.mWebView;
    }

    public void webViewEvaluatescript(String script) {
        mWebView.evaluateJavascript(script, null);
    }

    public void setScreenOrientation(String screenOrientation) {
        this.mScreenOrientation = screenOrientation;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SAVE_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mWebPresenter.jsBrSaveImage();
            } else {
                LogUtil.d(TAG, "saveImage - download succeed = " + false);
                mWebPresenter.jsBrSaveImageDone(false);
            }
        } else if (requestCode == REQUEST_SYNTHESIZE_PROMOTION_IMAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new PromotionImageSynthesizer(this, mPromotionQrCodeUrl, mPromotionSize, mPromotionX, mPromotionY, mWebPresenter.getJsBridge()).execute();
            } else {
                mWebPresenter.jsBrSynthesizePromotionImageDone(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void doResume() {
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
        setOrientation();
        mWebView.onResume();
        mWebView.evaluateJavascript("typeof onGameResume === 'function' && onGameResume();", null);
    }

    private void setOrientation() {
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
    }

    private void showHoverMenu() {
        if (mHoverMenu == null) {
            mHoverMenu = new FunctionMenu(this);
            mHoverMenu.setMenuListener(new FunctionMenu.OnMenuClickListener() {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebPresenter.onDestroy();
        mWebView.removeJavascriptInterface(JsBridge.getJsBridgeName());
        mWebView.destroy();
        if (mNetworkReceiver != null) {
            unregisterReceiver(mNetworkReceiver);
        }
    }


    public void showWebViewUpdateDialog() {
        if (isFinishing()) {
            return;
        }
        if (!PreferenceUtil.readShowWebViewUpdateDialog()) {
            return;
        }
        if (null != mWebViewUpdateDialog && mWebViewUpdateDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(WebActivity.this, R.style.AppDialog);
        builder.setMessage(R.string.msg_update_android_system_webview);
        builder.setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissWebViewUpdateDialog();
                DeviceUtil.openMarket(getBaseContext(), mWebPresenter.systemWebViewPackage);
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
        mNetworkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

    class NetworkReceiver extends BroadcastReceiver {
        private boolean networkListenerInit = false;

        public NetworkReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                LogUtil.d(TAG, "receive intentAction: " + intent.getAction());
                if (networkListenerInit) {
                    boolean isConnected = NetworkUtil.isConnected(context);
                    if (isConnected) {
                        LogUtil.d(TAG, "network connected!");
                        mLoadingLayout.setVisibility(View.VISIBLE);
                        mWebView.reload();
                    }
                }
                networkListenerInit = true;
            }
        }

    }

}
