package code.sdk.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.webkit.ValueCallback
import androidx.core.app.ActivityCompat
import code.sdk.R
import code.sdk.bridge.BridgeCallback
import code.sdk.bridge.JsBridge
import code.sdk.bridge.JsBridgeCore
import code.sdk.bridge.JsBridgeCore.Companion.getJsBridgeName
import code.sdk.common.PermissionUtils.checkStoragePermissions
import code.sdk.common.ShareUtil.sendText
import code.sdk.common.ShareUtil.shareToWhatsApp
import code.sdk.core.Constant
import code.sdk.core.manager.AdjustManager
import code.sdk.core.util.CocosPreferenceUtil
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.FileUtil
import code.sdk.core.util.PreferenceUtil
import code.sdk.core.util.UIUtil
import code.sdk.core.util.URLUtilX
import code.sdk.download.DownloadTask
import code.sdk.download.DownloadTask.OnDownloadListener
import code.sdk.ui.WebActivity
import code.sdk.util.PromotionImageSynthesizer
import code.util.IOUtil.readRawContent
import code.util.ImageUtil.triggerScanning
import code.util.LogUtil.d
import code.util.LogUtil.e
import code.util.LogUtil.i
import code.util.LogUtil.w
import java.io.File

class WebPresenter(private val mWebViewActivity: WebActivity) {
   private val TAG = WebPresenter::class.java.simpleName

    val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    val systemWebViewPackage = "com.google.android.webview"
    private var mUrl = ""
    private var isGame = false
    private var mWebGlScript: String? = null
    private val msg_check_feature = 20001
    private val msg_check_webView_compatibility = 20002
    private var mImageUrl: String? = null
    private val miniSystemWebViewVersion = "64.0.3282.29"
    private val miniSystemWebViewVersionCode = 328202950
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == msg_check_feature) {
                val gameUrl = msg.obj as String
                handleCheckGameFeature(gameUrl)
            } else if (msg.what == msg_check_webView_compatibility) {
                handleCheckWebViewCompatibility()
            }
        }
    }
    val jsBridge = object : BridgeCallback {
        override fun openUrlByBrowser(url: String?) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setData(Uri.parse(url))
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
            if (!url.isNullOrEmpty())
                UIUtil.runOnUiThread { mWebViewActivity.webView.loadUrl(url) }
        }

        override fun goBack() {
            UIUtil.runOnUiThread {
                if (mWebViewActivity.webView.canGoBack()) {
                    mWebViewActivity.webView.goBack()
                } else {
                    mWebViewActivity.finish()
                }
            }
        }

        override fun close() {
            UIUtil.runOnUiThread { mWebViewActivity.finish() }
        }

        override fun refresh() {
            UIUtil.runOnUiThread { mWebViewActivity.webView.reload() }
        }

        override fun clearCache() {
            UIUtil.runOnUiThread { mWebViewActivity.webView.clearCache(true) }
        }

        override fun saveImage(url: String?) {
            mImageUrl = url
            if (checkStoragePermissions(mWebViewActivity)) {
                val dir = mWebViewActivity.getExternalFilesDir(Environment.DIRECTORY_DCIM)
                FileUtil.ensureDirectory(dir)
                DownloadTask.mInstance.download(url, dir!!.absolutePath, object : OnDownloadListener {
                    override fun onDownloadSuccess(saveFile: File) {
                        val succeed = saveFile.length() > 0
                        if (succeed) {
                            triggerScanning(saveFile)
                        }
                        d(TAG, "saveImage - download succeed = $succeed")
                        saveImageDone(succeed)
                    }

                    override fun onDownloading(progress: Int) {
                        d(TAG, "saveImage - downloading = $progress%")
                    }

                    override fun onDownloadFailed() {
                        w(TAG, "saveImage - download failed")
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
            UIUtil.runOnUiThread {
                d(TAG, "saveImageDone = $succeed")
                val script =
                    if (succeed) "Listener.send('SAVE_IMAGE_SUCCEED');" else "Listener.send('SAVE_IMAGE_FAILED');"
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun savePromotionMaterialDone(succeed: Boolean) {
            UIUtil.runOnUiThread {
                d(TAG, "savePromotionMaterialDone = $succeed")
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
                PromotionImageSynthesizer(mWebViewActivity, qrCodeUrl, size, x, y, this).execute()
            } else {
                ActivityCompat.requestPermissions(
                    mWebViewActivity,
                    PERMISSIONS_STORAGE,
                    mWebViewActivity.REQUEST_SYNTHESIZE_PROMOTION_IMAGE
                )
            }
        }

        override fun synthesizePromotionImageDone(succeed: Boolean) {
            UIUtil.runOnUiThread {
                d(TAG, "synthesizePromotionImageDone = $succeed")
                val script =
                    if (succeed) "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_SUCCEED');" else "Listener.send('SYNTHESIZE_PROMOTION_IMAGE_FAILED');"
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun onHttpDnsHttpResponse(requestId: String?, response: String?) {
            UIUtil.runOnUiThread {
                d(TAG, "[HttpDns] onHttpDnsHttpResponse: %s", response)
                val script =
                    String.format("Listener.send('HTTPDNS_HTTP_RESPONSE', '%s');", response)
                mWebViewActivity.webViewEvaluatescript(script)
            }
        }

        override fun onHttpDnsWsResponse(requestId: String?, response: String?) {
            UIUtil.runOnUiThread {
                d(TAG, "[HttpDns] onHttpDnsWsResponse: %s", response)
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
            UIUtil.runOnUiThread {
                d(TAG, "preloadPromotionImageDone = $succeed")
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
        mUrl = url
        mWebGlScript = readRawContent(mWebViewActivity.baseContext, R.raw.webgl_script)
        d(TAG, "WebGlScript=$mWebGlScript")
        val jsBridge = JsBridge(jsBridge)
        mWebViewActivity.webView.addJavascriptInterface(jsBridge, JsBridgeCore.getJsBridgeName())
        val uri = Uri.parse(mUrl)
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

    /**
     * 检查游戏特征值包括(延迟1min检查)
     * _int_chn
     * _int_brand_code
     */
    fun checkGameFeature(gameUrl: String?) {
        if (isGame) {
            mHandler.removeMessages(msg_check_feature)
            val msg = Message.obtain()
            msg.what = msg_check_feature
            msg.obj = gameUrl
            mHandler.sendMessageDelayed(msg, 60000)
        }
    }

    fun clearGameFeature() {
        if (isGame) {
            CocosPreferenceUtil.putString(CocosPreferenceUtil.KEY_INT_CHN, "")
            CocosPreferenceUtil.putString(CocosPreferenceUtil.KEY_INT_BRAND_CODE, "")
        }
    }

    private fun handleCheckGameFeature(gameUrl: String) {
        val chn = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_INT_CHN)
        val brandCode = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_INT_BRAND_CODE)
        d(
            TAG,
            "check game feature: %s=%s, %s=%s",
            CocosPreferenceUtil.KEY_INT_CHN,
            chn,
            CocosPreferenceUtil.KEY_INT_BRAND_CODE,
            brandCode
        )
        if (TextUtils.isEmpty(chn) && TextUtils.isEmpty(brandCode)) { //特征值为空，不符合游戏特征(判定为不是我们自己的游戏)
            //当前加载的url跟缓存的url一样，并且不符合游戏特征，则清除掉缓存url
            val cacheGameUrl = PreferenceUtil.readGameUrl()
            d(TAG, "check game feature: is not validate game page: %s", gameUrl)
            if (isSameBaseUrl(gameUrl, cacheGameUrl)) {
                d(TAG, "check game feature: clear cached url: %s", cacheGameUrl)
                //PreferenceUtil.saveGameUrl(null);
            } else {
                d(TAG, "check game feature: abort clearing cached url: %s", cacheGameUrl)
            }
        } else {
            d(TAG, "check game feature: is validate game page: %s", gameUrl)
        }
    }
    /* Check WebView Compatibility START */
    /**
     * 检查WebView兼容性（两个方面：WebGl和Android System WebView版本）
     */
    fun checkWebViewCompatibility() {
        if (isGame) {
            mHandler.removeMessages(msg_check_webView_compatibility)
            mHandler.sendEmptyMessageDelayed(msg_check_webView_compatibility, 5000)
        }
    }

    fun jsBrSaveImage() {
        jsBridge.saveImage(mImageUrl)
    }

    fun jsBrSaveImageDone(succeed: Boolean?) {
        jsBridge.saveImageDone(succeed!!)
    }

    fun jsBrSynthesizePromotionImageDone(succeed: Boolean) {
        jsBridge.synthesizePromotionImageDone(succeed)
    }

    private fun handleCheckWebViewCompatibility() {
        if (TextUtils.isEmpty(mWebGlScript)) {
            d(TAG, "abort checking WebView compatibility, WebGlScript is empty")
            return
        }
        d(TAG, "start checking WebView compatibility...")
        val isWebViewCompatible = isWebViewCompatible
        mWebViewActivity.webView
            .evaluateJavascript(mWebGlScript!!) { value -> //js与native交互的回调函数
                d(TAG, "isWebGlEnable = $value")
                if ("false" == value || !isWebViewCompatible) {
                    UIUtil.runOnUiThread { mWebViewActivity.showWebViewUpdateDialog() }
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
                DeviceUtil.getPackageInfo(mWebViewActivity.baseContext, systemWebViewPackage)
            var compatible = false
            if (packageInfo != null) {
                if (packageInfo.versionCode >= miniSystemWebViewVersionCode) {
                    compatible = true
                    i(
                        TAG, "[Android System WebView] version: %s(%d) compatible for cocos game",
                        packageInfo.versionName, packageInfo.versionCode
                    )
                } else {
                    e(
                        TAG,
                        "[Android System WebView] version: %s(%d) not compatible for cocos game, need upgrade to %s(%d) or higher",
                        packageInfo.versionName,
                        packageInfo.versionCode,
                        miniSystemWebViewVersion,
                        miniSystemWebViewVersionCode
                    )
                }
            } else {
                i(TAG, "[Android System WebView] not installed")
            }
            return compatible
        }

    private fun isSameBaseUrl(url1: String, url2: String): Boolean {
        if (TextUtils.isEmpty(url1) || TextUtils.isEmpty(url2)) {
            return false
        }
        var baseUrl1 = URLUtilX.getBaseUrl(url1)
        var baseUrl2 = URLUtilX.getBaseUrl(url2)
        if (!baseUrl1.endsWith("/")) {
            baseUrl1 = "$baseUrl1/"
        }
        if (!baseUrl2.endsWith("/")) {
            baseUrl2 = "$baseUrl2/"
        }
        return baseUrl1 == baseUrl2
    }

    fun onDestroy() {
        mHandler.removeMessages(msg_check_webView_compatibility)
        mHandler.removeMessages(msg_check_feature)
    }
}
