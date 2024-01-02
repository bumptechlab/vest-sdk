package code.sdk.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import code.sdk.R
import code.sdk.SdkInit
import code.sdk.base.BaseWebActivity
import code.sdk.bridge.JsBridgeCore
import code.sdk.command.AssetLoaderManager
import code.sdk.common.ScreenUtil.dp2px
import code.sdk.core.Constant
import code.sdk.core.Constant.ScreenOrientation
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.PreferenceUtil
import code.sdk.core.util.TestUtil
import code.sdk.drawable.Drawables
import code.sdk.ui.FunctionMenu.OnMenuClickListener
import code.sdk.util.AndroidBug5497Workaround
import code.sdk.util.ImageUtil
import code.sdk.util.PromotionImageSynthesizer
import code.util.LogUtil.d
import code.util.LogUtil.e
import code.util.LogUtil.w
import code.util.NetworkUtil.isConnected
import java.util.Locale

/**
 * @note webview activity
 */
class WebActivity : BaseWebActivity() {
    private var mUrl = ""
    private var isGame = false
    val REQUEST_SAVE_IMAGE = 100
    val REQUEST_SYNTHESIZE_PROMOTION_IMAGE = 10001
    val REQUEST_CODE_FILE_CHOOSER = 10003
    private var mShowHoverMenu = false
    private var mShowNavBar = false
    private var mSafeCutout = false

    @ScreenOrientation
    private var mScreenOrientation = Constant.LANDSCAPE
    lateinit var webView: WebView
    private lateinit var mErrorLayout: View
    private lateinit var mRefreshButton: View
    private lateinit var mLoadingLayout: View
    private var mHoverMenu: FunctionMenu? = null
    private var mWebViewUpdateDialog: AlertDialog? = null
    private var mLoadingError = false
    private var mLastBackTs: Long = 0
    private var mUploadMessage: ValueCallback<Array<Uri?>>? = null

    private val mWebChromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            w(TAG, "onReceivedTitle: $title")
            if (!mLoadingError) {
                mLoadingError = titleError(title)
            }
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            d(TAG, "onProgressChanged: $newProgress")
        }

        // For [4.1, 5.0)
//        fun openFileChooser(
//            uploadMsg: ValueCallback<Uri?>?,
//            acceptType: String?,
//            capture: String?
//        ) {
//            mUploadMessage = uploadMsg
//            val i = Intent(Intent.ACTION_GET_CONTENT)
//            i.addCategory(Intent.CATEGORY_OPENABLE)
//            val type = if (TextUtils.isEmpty(acceptType)) "*/*" else acceptType!!
//            i.setType(type)
//            startActivityForResult(
//                Intent.createChooser(i, "File Chooser"),
//                REQUEST_CODE_FILE_CHOOSER
//            )
//        }


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri?>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            mUploadMessage = filePathCallback
            val intent = Intent(Intent.ACTION_PICK)
            if (fileChooserParams != null && fileChooserParams.acceptTypes != null && fileChooserParams.acceptTypes.isNotEmpty()) {
                intent.setType(java.lang.String.join(",", *fileChooserParams.acceptTypes))
            } else {
                intent.setType("*/*")
            }
            startActivityForResult(
                Intent.createChooser(intent, "File Chooser"),
                REQUEST_CODE_FILE_CHOOSER
            )
            return true
        }
    }
    private var mNetworkReceiver: NetworkReceiver? = null

    @JvmField
    var mPromotionQrCodeUrl: String? = null
    private val mWebViewClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            d(TAG, "shouldOverrideUrlLoading: %s", url)
            return if (url != null && (!(url.startsWith("http") || url.startsWith("https")))) {
                true
            } else super.shouldOverrideUrlLoading(view, url)
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            return if (request != null) {
                d(TAG, "shouldInterceptRequest[%s]: %s", request.method, request.url)
                AssetLoaderManager.mInstance.shouldInterceptRequest(request.url)
            } else {
                val resourceRequest: WebResourceRequest? = null
                super.shouldInterceptRequest(view, resourceRequest)
            }
        }

        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            d(TAG, "shouldInterceptRequest: %s", url)
            return AssetLoaderManager.mInstance.shouldInterceptRequest(Uri.parse(url))
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            d(TAG, "onPageStarted: %s", url)
            doPageStart()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (view != null && url != null)
                doPageFinish(view, url)
        }

        override fun onReceivedError(
            view: WebView?, errorCode: Int, description: String?,
            failingUrl: String?
        ) {
            mLoadingError = true
            e(TAG, "onReceivedError: %d, description: %s", errorCode, description)
        }


        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?, handler: SslErrorHandler?,
            error: SslError?
        ) {
            if (error != null)
                e(TAG, "onReceivedSslError: %s", error.url)
            //handler.proceed();
        }


        override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
            super.onReceivedClientCertRequest(view, request)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null) {
                d(TAG, "onReceivedClientCertRequest: %s", request.host)
            }
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            d(TAG, "onLoadResource: %s", url)
        }
    }

    private fun titleError(title: String?): Boolean {
        return !TextUtils.isEmpty(title) && (title!!.contains("404") || title.contains("500") || title.lowercase(
            Locale.getDefault()
        ).contains("error"))
    }

    var mPromotionSize = 0
    var mPromotionX = 0
    var mPromotionY = 0
    private var mWebPresenter: WebPresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        SdkInit.mContext = this
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent == null) {
            finish()
            return
        }
        isGame = intent.getBooleanExtra(KEY_GAME, false)
        val url = intent.getStringExtra(KEY_URL)
        if (url.isNullOrEmpty()) {
            finish()
            return
        }
        mWebPresenter = WebPresenter(this@WebActivity)
        val type = intent.getIntExtra(KEY_TYPE, Constant.SELF)
        if (type == Constant.SELF) {
            //加载自己的url
            mUrl = JsBridgeCore.formatUrlWithJsb(url)
            mWebPresenter!!.init(isGame, mUrl)
            webView.loadUrl(mUrl)
        } else {
            //加载三方网址
            mUrl = url
            // hover menu
            setShowHoverMenu(intent.getBooleanExtra(KEY_HOVER, false))
            // screen orientation
            val screenOrientation = intent.getStringExtra(KEY_ORIENTATION)
            if (Constant.PORTRAIT == screenOrientation || Constant.UNSPECIFIED == screenOrientation) {
                setScreenOrientation(screenOrientation)
            }
            if (type == Constant.OTHER_CODE) {
                //加载三方HTML源代码
                webView.loadDataWithBaseURL(null, mUrl, "text/html", "utf-8", null)
            } else {
                //加载三方url
                webView.loadUrl(mUrl)
            }
        }
        d(TAG, "open url: %s", mUrl)
    }

    override fun initView() {
        setContentView(createView())
        mRefreshButton.setOnClickListener { view: View? ->
            mLoadingLayout.visibility = View.VISIBLE
            webView.reload()
        }
        //        mWebView.initWebView(mWebViewClient, mWebChromeClient);
        setWebView()

        // fullscreen webview activity can NOT use adjustPan/adjustResize input mode
        AndroidBug5497Workaround.assistActivity(this)
    }

    private fun createView(): View {
        val relativeLayout = RelativeLayout(this)
        relativeLayout.setBackgroundColor(Color.BLACK)
        relativeLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        relativeLayout.keepScreenOn = true
        webView = WebView(this)
        relativeLayout.addView(
            webView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )
        val linearLayout = LinearLayout(this)
        mErrorLayout = linearLayout
        linearLayout.gravity = Gravity.CENTER
        linearLayout.setBackgroundColor(Color.WHITE)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.visibility = View.INVISIBLE
        val textView = TextView(this)
        textView.setTextColor(ContextCompat.getColor(this, R.color.loading_error_text))
        textView.textSize = 18f
        textView.setText(R.string.loading_error_tips)
        textView.setTypeface(null, Typeface.BOLD)
        linearLayout.addView(
            textView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        val refresh = TextView(this)
        mRefreshButton = refresh
        refresh.setTextColor(ContextCompat.getColor(this, R.color.loading_error_refresh_button))
        refresh.textSize = 18f
        refresh.setText(R.string.refresh)
        val refreshParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        refreshParams.topMargin = dp2px(32f)
        linearLayout.addView(refresh, refreshParams)
        relativeLayout.addView(
            linearLayout,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )
        val loading = LinearLayout(this)
        mLoadingLayout = loading
        loading.background = ImageUtil.base64ToDrawable(resources, Drawables.LOADING_BG)
        loading.gravity = Gravity.CENTER_VERTICAL
        val progressBar = ProgressBar(this)
        progressBar.isIndeterminate = true
        val pbParams = LinearLayout.LayoutParams(dp2px(25f), dp2px(25f))
        pbParams.setMargins(dp2px(10f), 0, dp2px(10f), 0)
        loading.addView(progressBar, pbParams)
        val tvLoading = TextView(this)
        tvLoading.text = "loading"
        tvLoading.setTextColor(Color.WHITE)
        val tvParams = LinearLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        tvParams.marginEnd = dp2px(15f)
        loading.addView(tvLoading, tvParams)
        val loadingParams =
            RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, dp2px(45f))
        loadingParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        relativeLayout.addView(loading, loadingParams)
        return relativeLayout
    }

    private fun doPageStart() {
        mLoadingError = false
        mWebPresenter?.clearGameFeature()
    }

    private fun doPageFinish(view: WebView, url: String) {
        val title = view.title
        if (!mLoadingError) {
            mLoadingError = titleError(title)
        }
        d(TAG, "onPageFinished: %s, title: %s,  loadingError: %s", url, title, mLoadingError)
        mLoadingLayout.visibility = View.INVISIBLE
        if (mLoadingError) {
            mErrorLayout.visibility = View.VISIBLE
            webView.visibility = View.INVISIBLE
        } else {
            mErrorLayout.visibility = View.INVISIBLE
            webView.visibility = View.VISIBLE
            // request focus ahead in case of any input behavior
            webView.requestFocus()
            webView.requestFocusFromTouch()
        }
        d(TAG, "Loading layout visibility: " + mLoadingLayout.visibility)
        mWebPresenter?.checkGameFeature(url)
        mWebPresenter?.checkWebViewCompatibility()
        if (mLoadingError) {
            registerNetworkListener()
        }
    }

    private fun setWebView() {
        val settings = webView.settings
        settings.defaultTextEncodingName = "utf-8"
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowContentAccess = true
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.databaseEnabled = true
        settings.useWideViewPort = true //支持自动适配
        settings.loadWithOverviewMode = true
        settings.javaScriptCanOpenWindowsAutomatically = true //支持通过js打开新窗口
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.webViewClient = mWebViewClient
        webView.webChromeClient = mWebChromeClient
        WebView.setWebContentsDebuggingEnabled(TestUtil.isLoggable())
    }

    fun setShowHoverMenu(showHoverMenu: Boolean) {
        mShowHoverMenu = showHoverMenu
    }

    fun setShowNavBar(showNavBar: Boolean) {
        mShowNavBar = showNavBar
    }

    fun setSafeCutout(safeCutout: Boolean) {
        mSafeCutout = safeCutout
    }

    fun webViewEvaluatescript(script: String?) {
        webView.evaluateJavascript(script!!, null)
    }

    fun setScreenOrientation(screenOrientation: String) {
        mScreenOrientation = screenOrientation
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SAVE_IMAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mWebPresenter?.jsBrSaveImage()
            } else {
                d(TAG, "saveImage - download succeed = " + false)
                mWebPresenter?.jsBrSaveImageDone(false)
            }
        } else if (requestCode == REQUEST_SYNTHESIZE_PROMOTION_IMAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PromotionImageSynthesizer(
                    this,
                    mPromotionQrCodeUrl,
                    mPromotionSize,
                    mPromotionX,
                    mPromotionY,
                    mWebPresenter?.jsBridge
                ).execute()
            } else {
                mWebPresenter!!.jsBrSynthesizePromotionImageDone(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun doResume() {
        // hover menu
        if (mShowHoverMenu) {
            showHoverMenu()
        } else {
            hideHoverMenu()
        }
        // nav bar
        if (mShowNavBar) {
            showNavBar()
        } else {
            hideNavBar()
        }
        // safe cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val window = window
            val layoutParams = window.attributes
            if (mSafeCutout) {
                //永远不允许应用程序的内容延伸到刘海区域
                layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            } else {
                //不管手机处于横屏还是竖屏模式，都允许应用程序的内容延伸到刘海区域
                layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = layoutParams
        }
        setOrientation()
        webView.onResume()
        webView.evaluateJavascript("typeof onGameResume === 'function' && onGameResume();", null)
    }

    private fun setOrientation() {
        // orientation
        requestedOrientation = when (mScreenOrientation) {
            Constant.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Constant.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun showHoverMenu() {
        if (mHoverMenu == null) {
            mHoverMenu = FunctionMenu(this)
            mHoverMenu!!.setMenuListener(object : OnMenuClickListener {
                override fun onRefresh() {
                    mLoadingLayout.visibility = View.VISIBLE
                    webView.reload()
                }

                override fun onClose() {
                    finish()
                }
            })
            mHoverMenu!!.show()
        }
    }

    private fun hideHoverMenu() {
        mHoverMenu?.hide()
    }

    private fun showNavBar() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    private fun hideNavBar() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mHoverMenu?.resetPositionDelayed()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            if (mUploadMessage == null) {
                return
            }
            if (resultCode != RESULT_OK || data == null) {
                mUploadMessage!!.onReceiveValue(null)
                mUploadMessage = null
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val uriArray: Array<Uri?>
                val dataString = data.dataString
                if (!TextUtils.isEmpty(dataString)) {
                    uriArray = arrayOf(Uri.parse(dataString))
                    mUploadMessage!!.onReceiveValue(uriArray)
                    return
                }
                val clipData = data.clipData
                if (clipData != null) {
                    uriArray = arrayOfNulls(clipData.itemCount)
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        uriArray[i] = item.uri
                    }
                    mUploadMessage!!.onReceiveValue(uriArray)
                    return
                }
                mUploadMessage!!.onReceiveValue(null)
            } else {
                mUploadMessage!!.onReceiveValue(arrayOf(data.data))
            }
            mUploadMessage = null
        }
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mWebPresenter != null) {
            mWebPresenter!!.onDestroy()
        }
        if (webView != null) {
            webView.removeJavascriptInterface(JsBridgeCore.getJsBridgeName())
            webView.destroy()
        }
        if (mNetworkReceiver != null) {
            unregisterReceiver(mNetworkReceiver)
        }
    }

    fun showWebViewUpdateDialog() {
        if (isFinishing) {
            return
        }
        if (!PreferenceUtil.readShowWebViewUpdateDialog()) {
            return
        }
        if (null != mWebViewUpdateDialog && mWebViewUpdateDialog!!.isShowing) {
            return
        }
        val builder = AlertDialog.Builder(this@WebActivity, R.style.AppDialog)
        builder.setMessage(R.string.msg_update_android_system_webview)
        builder.setPositiveButton(R.string.btn_confirm) { _, _ ->
            dismissWebViewUpdateDialog()
            DeviceUtil.openMarket(baseContext, mWebPresenter!!.systemWebViewPackage)
        }
        builder.setNegativeButton(R.string.btn_cancel) { _, _ -> dismissWebViewUpdateDialog() }
        builder.setNeutralButton(R.string.btn_never_ask) { _, _ ->
            dismissWebViewUpdateDialog()
            PreferenceUtil.saveShowWebViewUpdateDialog(false)
        }
        mWebViewUpdateDialog = builder.create()
        mWebViewUpdateDialog?.show()
    }

    private fun dismissWebViewUpdateDialog() {
        if (null != mWebViewUpdateDialog && mWebViewUpdateDialog!!.isShowing) {
            try {
                mWebViewUpdateDialog!!.dismiss()
                mWebViewUpdateDialog = null
            } catch (_: Exception) {
            }
        }
    }

    /* WebView Compatibility END */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis() - mLastBackTs > 2000) {
                Toast.makeText(this, R.string.one_more_click_go_back, Toast.LENGTH_SHORT).show()
                mLastBackTs = System.currentTimeMillis()
            } else {
                finish()
                if (isGame) {
                    d(TAG, "kill process after exit game")
                    System.exit(0)
                    Process.killProcess(Process.myPid())
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun registerNetworkListener() {
        if (mNetworkReceiver != null) {
            return
        }
        mNetworkReceiver = NetworkReceiver()
        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(mNetworkReceiver, filter)
    }

    internal inner class NetworkReceiver : BroadcastReceiver() {
        private var networkListenerInit = false
        override fun onReceive(context: Context, intent: Intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
                d(TAG, "receive intentAction: " + intent.action)
                if (networkListenerInit) {
                    val isConnected = isConnected(context)
                    if (isConnected) {
                        d(TAG, "network connected!")
                        mLoadingLayout.visibility = View.VISIBLE
                        webView.reload()
                    }
                }
                networkListenerInit = true
            }
        }
    }

    companion object {
        private val TAG = WebActivity::class.java.simpleName
        const val KEY_URL = "key_path_url_value"
        const val KEY_GAME = "key_is_game_value"
        const val KEY_TYPE = "key_type_value"
        const val KEY_ORIENTATION = "key_orientation_value"
        const val KEY_HOVER = "key_hover_value"
    }
}
