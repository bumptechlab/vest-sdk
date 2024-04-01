package book.sdk.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import book.sdk.bridge.BridgeCallback
import book.sdk.bridge.JsBridge
import book.sdk.core.Constant
import book.sdk.core.manager.AdjustManager
import book.sdk.core.util.DeviceUtil
import book.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebPresenter(private val mWebViewActivity: WebActivity) {
    private val TAG = WebPresenter::class.java.simpleName

    private var mUrl = ""
    private var isGame = false
    private var mCoroutineJob: Job? = null

    val mJsBridge = object : BridgeCallback {
        override fun openUrlByBrowser(url: String?) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse(url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
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
                val intent = Intent(mWebViewActivity, WebActivity::class.java).apply {
                    putExtra(WebActivity.KEY_TYPE, type)
                    putExtra(WebActivity.KEY_ORIENTATION, orientation)
                    putExtra(WebActivity.KEY_HOVER, hover)
                    putExtra(WebActivity.KEY_URL, data)
                }
                mWebViewActivity.startActivity(intent)
            } catch (_: Exception) {
            }
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
