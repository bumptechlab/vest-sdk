package poetry.sdk.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import poetry.res.ResourceLoader
import poetry.sdk.R
import poetry.sdk.base.BaseWebActivity
import poetry.sdk.bridge.JsBridgeCore
import poetry.sdk.core.util.Constant
import poetry.sdk.core.util.DeviceUtil
import poetry.sdk.core.util.PreferenceUtil
import poetry.sdk.core.util.Tester
import poetry.sdk.util.AndroidBug5497Workaround
import poetry.sdk.util.ScreenUtil
import poetry.util.LogUtil
import poetry.util.NetworkUtil
import poetry.util.ToastUtil
import poetry.util.base64ToBitmap
import java.net.URLDecoder
import java.util.Locale
import kotlin.system.exitProcess

/**
 * @note webview activity
 */
class WebActivity : BaseWebActivity() {
    private var mUrl = ""
    private var mLobbyUrl = ""
    private var isGame = false
    private var mIsShowWeb by mutableStateOf(true)
    private var mIsShowLoading by mutableStateOf(true)
    private var mIsShowHoverMenu by mutableStateOf(false)
    private var mIsOpenMenu by mutableStateOf(false)
    private var mShowNavBar = false
    private var mSafeCutout = false

    @Constant.HoverMenuDockType
    private var mMenuDockType = Constant.DOCK_LEFT

    @Constant.ScreenOrientation
    private var mScreenOrientation = Constant.LANDSCAPE
    lateinit var mWebView: WebView
    private var mWebViewUpdateDialog: AlertDialog? = null
    private var mLoadingError = false
    private var mLoadingFinish = false
    private var mLastBackTs: Long = 0
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mMaxWidth = 0
    private var mMaxHeight = 0
    private val mImageSize = 45.dp

    private val mWebChromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            LogUtil.w(TAG, "onReceivedTitle: $title")
            if (!mLoadingError) {
                mLoadingError = titleError(title)
            }
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            LogUtil.d(TAG, "onProgressChanged: $newProgress")
        }

    }
    private var mNetworkReceiver: NetworkReceiver? = null

    private val mWebViewClient: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            LogUtil.d(TAG, "shouldOverrideUrlLoading: %s", url)
            try {
                //第三方游戏防止重定向，直接关闭当前页面
                val uri = Uri.parse(url)
                val isThirdGame = uri.getBooleanQueryParameter("isThirdGame", false)
                LogUtil.d(TAG, "shouldOverrideUrlLoading: isThirdGame=%s", isThirdGame)
                if (url.isEmpty() || isThirdGame || URLDecoder.decode(url, "UTF-8") == mLobbyUrl) {
                    finish()
                } else {
                    view?.loadUrl(url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            doPageStart(view, url)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            doPageFinish(view, url)
        }

        override fun onReceivedError(
            view: WebView?, errorCode: Int, description: String?,
            failingUrl: String?
        ) {
            mLoadingError = true
            LogUtil.e(TAG, "onReceivedError: %d, description: %s", errorCode, description)
        }


        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?, handler: SslErrorHandler?,
            error: SslError?
        ) {
            if (error != null)
                LogUtil.e(TAG, "onReceivedSslError: %s", error.url)
            //handler.proceed();
        }


        override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
            super.onReceivedClientCertRequest(view, request)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null) {
                LogUtil.d(TAG, "onReceivedClientCertRequest: %s", request.host)
            }
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
            LogUtil.d(TAG, "onLoadResource: %s", url)
        }
    }

    private fun titleError(title: String?): Boolean {
        return !TextUtils.isEmpty(title) && (title!!.contains("404") || title.contains("500") || title.lowercase(
            Locale.getDefault()
        ).contains("error"))
    }

    private var mWebPresenter: WebPresenter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent == null) {
            finish()
            return
        }
        setScreenSize()
    }

    private fun setScreenSize() {
        val screenSize = ScreenUtil.getScreenSize()
        mScreenWidth = screenSize[0]
        mScreenHeight = screenSize[1]
        mMaxWidth = mScreenWidth - ScreenUtil.dp2px(mImageSize.value)
        mMaxHeight = mScreenHeight - ScreenUtil.dp2px(mImageSize.value)
        mIsOpenMenu = false
    }

    private fun loadUrl() {
        isGame = intent.getBooleanExtra(KEY_GAME, false)
        val url = intent.getStringExtra(KEY_URL)
        if (url.isNullOrEmpty()) {
            finish()
            return
        }
        mWebPresenter = WebPresenter(this@WebActivity)
        val type = intent.getIntExtra(KEY_TYPE, Constant.SELF)
        if (type == Constant.SELF) {
            mLobbyUrl = Uri.parse(url).getQueryParameter("lobbyUrl") ?: ""
            //加载自己的url
            mUrl = JsBridgeCore.formatUrlWithJsb(url)
            mWebPresenter!!.init(isGame, mUrl)
            mWebView.loadUrl(mUrl)
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
                mWebView.loadDataWithBaseURL(null, mUrl, "text/html", "utf-8", null)
            } else {
                //加载三方url
                mWebView.loadUrl(mUrl)
            }
        }
        LogUtil.d(TAG, "open url: %s", mUrl)
        doResume()
    }

    override fun initView() {
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.Black)
            ) {
                Web()
                Error()
                Loading(Modifier.align(Alignment.Center))
                Menu()
            }
        }
        AndroidBug5497Workaround.assistActivity(this)
    }

    @Composable
    private fun Web() {
        if (mIsShowWeb) {
            AndroidView(factory = {
                mWebView = WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
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
                    webViewClient = mWebViewClient
                    webChromeClient = mWebChromeClient
                    WebView.setWebContentsDebuggingEnabled(Tester.isLoggable())
                }
                loadUrl()
                mWebView
            }, modifier = Modifier.fillMaxSize())
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(key1 = Unit) {
                val observer = LifecycleEventObserver { source, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            mWebView.onResume()
                            webViewEvaluatescript("typeof onGameResume === 'function' && onGameResume();")
                        }

                        Lifecycle.Event.ON_PAUSE -> {
                            mWebView.onPause()
                        }

                        Lifecycle.Event.ON_DESTROY -> {
                            mWebView.destroy()
                        }

                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
        }
    }

    @Composable
    private fun Error() {
        if (!mIsShowWeb) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = ResourceLoader.strings.loading_error_tips,
                    color = colorResource(id = R.color.loading_error_text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = ResourceLoader.strings.refresh,
                    color = colorResource(id = R.color.loading_error_refresh_button),
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        mIsShowWeb = true
                        mWebView.reload()
                    }
                )
            }
        }
    }

    @Composable
    private fun Loading(modifier: Modifier) {
        AnimatedVisibility(
            visible = mIsShowLoading, modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .height(45.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Image(
                    modifier = Modifier.matchParentSize(),
                    bitmap = poetry.sdk.drawable.Drawables.LOADING_BG.base64ToBitmap()
                        .asImageBitmap(),
                    contentDescription = ""
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(20.dp),
                        strokeWidth = 1.dp
                    )
                    Text(
                        modifier = Modifier.padding(end = 15.dp),
                        text = "loading",
                        color = Color.White
                    )
                }
            }
        }
    }

    @Composable
    private fun Menu() {
        var offsetX by remember { mutableIntStateOf(0) }
        var offsetY by remember { mutableIntStateOf((mScreenHeight * 0.35f).toInt()) }
        var menuWidth by remember { mutableIntStateOf(ScreenUtil.dp2px(mImageSize.value)) }
        var scaleX by remember { mutableFloatStateOf(1f) }
        val modifier = Modifier
            .size(mImageSize)
            .clip(CircleShape)
        AnimatedVisibility(visible = mIsShowHoverMenu) {
            Box(
                modifier = Modifier
                    .height(mImageSize)
                    .offset {
                        //防止越界
                        if (offsetX < 0) {
                            offsetX = 0
                        }
                        if (offsetY < 0) {
                            offsetY = 0
                        }
                        if (offsetX > mMaxWidth) {
                            offsetX = mMaxWidth
                        }
                        if (offsetY > mMaxHeight) {
                            offsetY = mMaxHeight
                        }
                        IntOffset(offsetX, offsetY)
                    }
                    .graphicsLayer(scaleX = scaleX)
                    .onGloballyPositioned {
                        if (menuWidth != it.size.width) {
                            menuWidth = it.size.width
                            if (mMenuDockType == Constant.DOCK_RIGHT) {
                                offsetX = mScreenWidth - menuWidth
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(onDragStart = {
                            mIsOpenMenu = false
                            scaleX = 1f
                        }, onDrag = { change, offset ->
                            offsetX += offset.x.toInt()
                            offsetY += offset.y.toInt()
                        }, onDragEnd = {
                            offsetX = if (offsetX > (mScreenWidth / 2)) {
                                mMenuDockType = Constant.DOCK_RIGHT
                                scaleX = -1f
                                mMaxWidth
                            } else {
                                mMenuDockType = Constant.DOCK_LEFT
                                scaleX = 1f
                                0
                            }
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                if (mIsOpenMenu) {
                    Image(
                        modifier = Modifier.matchParentSize(),
                        bitmap = poetry.sdk.drawable.Drawables.MENU_EXPANDED_BG
                            .base64ToBitmap()
                            .asImageBitmap(),
                        contentDescription = null,
                    )
                }
                Row {
                    Image(
                        bitmap = (if (mIsOpenMenu) poetry.sdk.drawable.Drawables.MENU_EXPANDED else poetry.sdk.drawable.Drawables.MENU_SHRINKED)
                            .base64ToBitmap()
                            .asImageBitmap(),
                        contentDescription = null,
                        modifier = modifier
                            .clickable {
                                mIsOpenMenu = !mIsOpenMenu
                            }
                    )
                    if (mIsOpenMenu) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Image(
                            bitmap = poetry.sdk.drawable.Drawables.MENU_REFRESH
                                .base64ToBitmap()
                                .asImageBitmap(),
                            contentDescription = null,
                            modifier = modifier.clickable {
                                mIsShowLoading = true
                                mIsOpenMenu = false
                                mWebView.reload()
                            }
                        )
                        Image(
                            bitmap = poetry.sdk.drawable.Drawables.MENU_CLOSE
                                .base64ToBitmap()
                                .asImageBitmap(),
                            contentDescription = null,
                            modifier = modifier.clickable {
                                mIsOpenMenu = false
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun doPageStart(view: WebView?, url: String?) {
        LogUtil.d(TAG, "onPageStarted: %s, title: %s", url, view?.title)
        mLoadingError = false
        mLoadingFinish = false
    }

    private fun doPageFinish(view: WebView?, url: String?) {
        val title = view?.title
        if (!mLoadingError) {
            mLoadingError = titleError(title)
        }
        LogUtil.d(
            TAG,
            "onPageFinished: %s, title: %s, loadingError: %s",
            url,
            title,
            mLoadingError
        )
        //加入mLoadingFinish是为了处理onPageFinished()多次调用的情况
        if (!mLoadingFinish) {
            mIsShowLoading = false
            if (mLoadingError) {
                mIsShowWeb = false
            } else {
                mIsShowWeb = true
                // request focus ahead in case of any input behavior
                mWebView.requestFocus()
                mWebView.requestFocusFromTouch()
            }
            mWebPresenter?.checkWebViewCompatibility()
            if (mLoadingError) {
                registerNetworkListener()
            }
            mLoadingFinish = true;
        }
    }

    fun setShowHoverMenu(showHoverMenu: Boolean) {
        mIsShowHoverMenu = showHoverMenu
    }

    fun setShowNavBar(showNavBar: Boolean) {
        mShowNavBar = showNavBar
    }

    fun setSafeCutout(safeCutout: Boolean) {
        mSafeCutout = safeCutout
    }

    fun webViewEvaluatescript(script: String?) {
        mWebView.evaluateJavascript(script!!, null)
    }

    fun setScreenOrientation(screenOrientation: String) {
        mScreenOrientation = screenOrientation
    }

    override fun doResume() {
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
    }

    private fun setOrientation() {
        // orientation
        requestedOrientation = when (mScreenOrientation) {
            Constant.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Constant.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
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
        setScreenSize()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mWebPresenter != null) {
            mWebPresenter!!.onDestroy()
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
        builder.setMessage(ResourceLoader.strings.msg_update_android_system_webview)
        builder.setPositiveButton(ResourceLoader.strings.btn_confirm) { _, _ ->
            dismissWebViewUpdateDialog()
            DeviceUtil.openMarket(baseContext, WebConstant.SYSTEM_WEBVIEW_PACKAGE)
        }
        builder.setNegativeButton(ResourceLoader.strings.btn_cancel) { _, _ -> dismissWebViewUpdateDialog() }
        builder.setNeutralButton(ResourceLoader.strings.btn_never_ask) { _, _ ->
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
                ToastUtil.showShortToast(ResourceLoader.strings.one_more_click_go_back)
                mLastBackTs = System.currentTimeMillis()
            } else {
                finish()
                if (isGame) {
                    LogUtil.d(TAG, "kill process after exit game")
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
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
                LogUtil.d(TAG, "receive intentAction: " + intent.action)
                if (networkListenerInit) {
                    val isConnected = NetworkUtil.isConnected(context)
                    if (isConnected) {
                        LogUtil.d(TAG, "network connected!")
                        mIsShowLoading = true
                        mWebView.reload()
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
