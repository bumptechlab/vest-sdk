package book.sdk.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.app.ActivityCompat
import book.sdk.bridge.BridgeCallback
import book.sdk.bridge.JsBridge
import book.sdk.common.PermissionUtils.checkStoragePermissions
import book.sdk.common.ShareUtil.sendText
import book.sdk.common.ShareUtil.shareToWhatsApp
import book.sdk.core.Constant
import book.sdk.core.manager.AdjustManager
import book.sdk.core.util.DeviceUtil
import book.sdk.download.DownloadTask
import book.sdk.download.DownloadTask.OnDownloadListener
import book.sdk.util.createPromotionImage
import book.util.LogUtil
import book.util.ensureDirectory
import book.util.scanToAlbum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WebPresenter(private val mWebViewActivity: WebActivity) {
    private val TAG = WebPresenter::class.java.simpleName

    private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private var mUrl = ""
    private var isGame = false
    private var mImageUrl: String? = null
    private var mCoroutineJob: Job? = null

    val mJsBridge = object : BridgeCallback {
        override fun openUrlByBrowser(url: String?) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                mWebViewActivity.startActivity(intent)
            } catch (_: Exception) {
            }
        }

        override fun openUrlByWebView(url: String?) {
            try {
                val intent = Intent(mWebViewActivity, WebActivity::class.java)
                intent.putExtra(WebActivity.KEY_URL, url)
                mWebViewActivity.startActivity(intent)
            } catch (_: Exception) {
            }
        }

        override fun openDataByWebView(
            type: Int,
            orientation: String?,
            hover: Boolean,
            data: String?
        ) {
            try {
                val intent = Intent(mWebViewActivity, WebActivity::class.java)
                intent.putExtra(WebActivity.KEY_TYPE, type)
                intent.putExtra(WebActivity.KEY_ORIENTATION, orientation)
                intent.putExtra(WebActivity.KEY_HOVER, hover)
                intent.putExtra(WebActivity.KEY_URL, data)
                mWebViewActivity.startActivity(intent)
            } catch (_: Exception) {
            }
        }

        override fun openApp(target: String?, fallbackUrl: String?) {
            try {
                mWebViewActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
            } catch (e: Exception) {
                e.printStackTrace()
                openUrlByBrowser(fallbackUrl)
            }
        }

        override fun loadUrl(url: String?) {
            if (!url.isNullOrEmpty()) {
                MainScope().launch(Dispatchers.Main) {
                    LogUtil.d(TAG, "loadUrl, current thread: ${Thread.currentThread().name}");
                    mWebViewActivity.mWebView.loadUrl(url)
                }
            }
        }

        override fun goBack() {
            MainScope().launch(Dispatchers.Main) {
                if (mWebViewActivity.mWebView.canGoBack()) {
                    mWebViewActivity.mWebView.goBack()
                } else {
                    mWebViewActivity.finish()
                }
            }
        }

        override fun close() {
            MainScope().launch(Dispatchers.Main) {
                mWebViewActivity.finish()
            }
        }

        override fun refresh() {
            MainScope().launch(Dispatchers.Main) {
                mWebViewActivity.mWebView.reload()
            }
        }

        override fun clearCache() {
            MainScope().launch(Dispatchers.Main) {
                mWebViewActivity.mWebView.clearCache(true)
            }
        }

        override fun saveImage(url: String?) {
            mImageUrl = url
            if (checkStoragePermissions(mWebViewActivity)) {
                val dir = mWebViewActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM)
                dir.ensureDirectory()
                DownloadTask.mInstance.download(
                    url,
                    dir!!.absolutePath,
                    object : OnDownloadListener {
                        override fun onDownloadSuccess(saveFile: File) {
                            val succeed = saveFile.length() > 0
                            if (succeed) {
                                saveFile.scanToAlbum()
                            }
                            LogUtil.d(TAG, "saveImage - download succeed = $succeed")
                            saveImageDone(succeed)
                        }

                        override fun onDownloading(progress: Int) {
                            LogUtil.d(TAG, "saveImage - downloading = $progress%")
                        }

                        override fun onDownloadFailed() {
                            LogUtil.w(TAG, "saveImage - download failed")
                            saveImageDone(false)
                        }
                    })
            } else {
                ActivityCompat.requestPermissions(
                    mWebViewActivity,
                    PERMISSIONS_STORAGE,
                    mWebViewActivity.REQUEST_SAVE_IMAGE
                )
            }
        }

        override fun saveImageDone(succeed: Boolean) {
            MainScope().launch(Dispatchers.Main) {
                LogUtil.d(TAG, "saveImageDone = $succeed")
                val script =
                    if (succeed) "Listener.send('SAVE_IMAGE_SUCCEED');" else "Listener.send('SAVE_IMAGE_FAILED');"
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun savePromotionMaterialDone(succeed: Boolean) {
            MainScope().launch(Dispatchers.Main) {
                LogUtil.d(TAG, "savePromotionMaterialDone = $succeed")
                val script =
                    if (succeed) "Listener.send('SAVE_PROMOTION_MATERIAL_SUCCEED');" else "Listener.send('SAVE_PROMOTION_MATERIAL_FAILED');"
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun synthesizePromotionImage(qrCodeUrl: String?, size: Int, x: Int, y: Int) {
            mWebViewActivity.mPromotionQrCodeUrl = qrCodeUrl
            mWebViewActivity.mPromotionSize = size
            mWebViewActivity.mPromotionX = x
            mWebViewActivity.mPromotionY = y
            if (checkStoragePermissions(mWebViewActivity)) {
                MainScope().launch(Dispatchers.IO) {
                    qrCodeUrl.createPromotionImage(size, x, y) { success ->
                        synthesizePromotionImageDone(success)
                    }
                }
            } else {
                ActivityCompat.requestPermissions(
                    mWebViewActivity,
                    PERMISSIONS_STORAGE,
                    mWebViewActivity.REQUEST_SYNTHESIZE_PROMOTION_IMAGE
                )
            }
        }

        override fun synthesizePromotionImageDone(succeed: Boolean) {
            MainScope().launch(Dispatchers.Main) {
                LogUtil.d(TAG, "synthesizePromotionImageDone = $succeed")
                val script =
                    if (succeed) "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_SUCCEED');" else "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_FAILED');"
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun onHttpDnsHttpResponse(requestId: String?, response: String?) {
            MainScope().launch(Dispatchers.Main) {
                LogUtil.d(TAG, "[HttpDns] onHttpDnsHttpResponse: %s", response)
                val script =
                    String.format("Listener.send('HTTPDNS_HTTP_RESPONSE', '%s');", response)
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun onHttpDnsWsResponse(requestId: String?, response: String?) {
            MainScope().launch(Dispatchers.Main) {
                LogUtil.d(TAG, "[HttpDns] onHttpDnsWsResponse: %s", response)
                val script = String.format("Listener.send('HTTPDNS_WS_RESPONSE', '%s');", response)
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun shareUrl(url: String?) {
            sendText(mWebViewActivity, url)
        }

        override fun loginFacebook() {}
        override fun logoutFacebook() {}
        override fun preloadPromotionImageDone(succeed: Boolean) {
            MainScope().launch(Dispatchers.Main) {
                LogUtil.d(TAG, "preloadPromotionImageDone = $succeed")
                val script =
                    if (succeed) "Listener.send('PRELOAD_PROMOTION_IMAGE_SUCCEED');" else "Listener.send('PRELOAD_PROMOTION_IMAGE_FAILED');"
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun shareToWhatsApp(text: String?, file: File?) {
            shareToWhatsApp(mWebViewActivity, text, file)
        }
    }

    fun init(isGame: Boolean, url: String) {
        this.isGame = isGame
        this.mUrl = url
        val jsBridge = JsBridge(mJsBridge)
        val uri = Uri.parse(mUrl)
        val jsb = uri.getQueryParameter("jsb")
        val jsbNamespace =
            if (jsb?.contains(".")!!) jsb?.substring(0, jsb.indexOf(".")) else "jsBridge"
        LogUtil.d(TAG, "jsb: $jsb, namespace: $jsbNamespace")
        mWebViewActivity.mWebView.addJavascriptInterface(jsBridge, jsbNamespace!!)
        // hover menu
        mWebViewActivity.setShowHoverMenu(
            uri.getBooleanQueryParameter(
                Constant.QUERY_PARAM_HOVER_MENU,
                false
            )
        )
        // nav bar
        mWebViewActivity.setShowNavBar(
            uri.getBooleanQueryParameter(
                Constant.QUERY_PARAM_NAV_BAR,
                false
            )
        )
        // save cutout
        mWebViewActivity.setSafeCutout(
            uri.getBooleanQueryParameter(
                Constant.QUERY_PARAM_SAFE_CUTOUT,
                false
            )
        )
        // screen orientation
        val screenOrientation = uri.getQueryParameter(Constant.QUERY_PARAM_ORIENTATION)
        if (Constant.PORTRAIT == screenOrientation || Constant.UNSPECIFIED == screenOrientation) {
            mWebViewActivity.setScreenOrientation(screenOrientation)
        }
        if (isGame) {
            AdjustManager.trackEventAccess(null)
        }
    }

    /* Check WebView Compatibility START */
    /**
     * 检查WebView兼容性（两个方面：WebGl和Android System WebView版本）
     */
    fun checkWebViewCompatibility() {
        if (isGame) {
            handleCheckWebViewCompatibility()
        }
    }

    fun jsBrSaveImage() {
        mJsBridge.saveImage(mImageUrl)
    }

    fun jsBrSaveImageDone(succeed: Boolean?) {
        mJsBridge.saveImageDone(succeed!!)
    }

    fun jsBrSynthesizePromotionImageDone(succeed: Boolean) {
        mJsBridge.synthesizePromotionImageDone(succeed)
    }


    private fun handleCheckWebViewCompatibility() {
        LogUtil.d(TAG, "start handleCheckWebViewCompatibility...")
        mCoroutineJob = CoroutineScope(Dispatchers.IO).launch {
            delay(5000)
            LogUtil.d(TAG, "start checking WebView Compatibility...")
            val isWebViewCompatible = isWebViewCompatible
            //切换到UI线程
            withContext(Dispatchers.Main) {
                LogUtil.d(TAG, "start checking WebView WebGL enable..")
                mWebViewActivity.mWebView
                    .evaluateJavascript(WebConstant.WEBGL_SCRIPT) { isWebGlEnable -> //js与native交互的回调函数
                        LogUtil.d(
                            TAG,
                            "isWebGlEnable = $isWebGlEnable, isWebViewCompatible = $isWebViewCompatible"
                        )
                        if ("false".equals(isWebGlEnable, false) || !isWebViewCompatible) {
                            mWebViewActivity.showWebViewUpdateDialog()
                        }
                    }
            }
        }
    }

    /**
     * 游戏需要的Android System WebView最小版本是：64.0.3282.29 - 328202950
     * 小于该版本，内核会报以下错误：
     * Uncaught SyntaxError: Invalid regular expression: /(?<bundle>.+)UiLanguage/:
     *
     * @return
    </bundle> */
    private val isWebViewCompatible: Boolean
        get() {
            val packageInfo =
                DeviceUtil.getPackageInfo(
                    mWebViewActivity.baseContext,
                    WebConstant.SYSTEM_WEBVIEW_PACKAGE
                )
            var compatible = false
            if (packageInfo != null) {
                if (packageInfo.versionCode >= WebConstant.MINI_SYSTEM_WEBVIEW_VERSION_CODE) {
                    compatible = true
                    LogUtil.i(
                        TAG, "[Android System WebView] version: %s(%d) compatible for cocos game",
                        packageInfo.versionName, packageInfo.versionCode
                    )
                } else {
                    LogUtil.e(
                        TAG,
                        "[Android System WebView] version: %s(%d) not compatible for cocos game, need upgrade to %s(%d) or higher",
                        packageInfo.versionName,
                        packageInfo.versionCode,
                        WebConstant.MINI_SYSTEM_WEBVIEW_VERSION,
                        WebConstant.MINI_SYSTEM_WEBVIEW_VERSION_CODE
                    )
                }
            } else {
                LogUtil.i(TAG, "[Android System WebView] not installed")
            }
            return compatible
        }

    fun onDestroy() {
        if (mCoroutineJob != null) {
            if (mCoroutineJob?.isActive!!) {
                LogUtil.d(TAG, "coroutine job is active, cancel now")
                mCoroutineJob?.cancel()
            } else {
                LogUtil.d(TAG, "coroutine job is not active")
            }
        } else {
            LogUtil.d(TAG, "coroutine job is null")
        }

    }
}
