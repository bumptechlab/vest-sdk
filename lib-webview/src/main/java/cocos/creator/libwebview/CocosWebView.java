package cocos.creator.libwebview;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;

import code.util.LogUtil;
import code.webview.BuildConfig;

public class CocosWebView extends WebView {
    public CocosWebView(@NonNull Context context) {
        this(context, null);
    }

    public CocosWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private final String COCOS_H5_ENGINE_NAME = "cocos2d-js";

    @SuppressLint("SetJavaScriptEnabled")
    public void initWebView(WebViewClient webViewClient, WebChromeClient webChromeClient) {
        //关闭硬件加速可解决黑屏、语法报错等问题。目前方案还是先引导用户升级Android System WebView来解决
        //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        WebSettings settings = getSettings();
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

        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);

        setWebContentsDebuggingEnabled(LogUtil.isDebug());
    }


    /**
     * 将本地Cocos引擎文件写入web
     *
     * @param uri
     * @param inputStream
     * @return
     */
    public WebResourceResponse shouldInterceptUrlEvent(Uri uri, InputStream inputStream) {
        String path = uri.getPath();
        if (inputStream == null) return null;
        try {
            return new WebResourceResponse(
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(path),
                    "utf-8", inputStream);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isCocosEngineUrl(Uri uri) {
        String path = uri.getPath();
        return !TextUtils.isEmpty(path) && path.contains(COCOS_H5_ENGINE_NAME);
    }

}
