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
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
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
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import code.sdk.R
import code.sdk.SdkInit
import code.sdk.base.BaseWebActivity
import code.sdk.bridge.JsBridgeCore
import code.sdk.command.AssetLoaderManager
import code.sdk.common.ScreenUtil
import code.sdk.core.Constant
import code.sdk.core.Constant.ScreenOrientation
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.PreferenceUtil
import code.sdk.core.util.TestUtil
import code.sdk.drawable.Drawables
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
    private var mIsShowWeb by mutableStateOf(true)
    private var mIsShowLoading by mutableStateOf(true)
    private var mIsShowHoverMenu by mutableStateOf(false)
    private var mIsOpenMenu by mutableStateOf(false)
    private var mShowNavBar = false
    private var mSafeCutout = false

    @Constant.HoverMenuDockType
    private var mMenuDockType = Constant.DOCK_LEFT

    @ScreenOrientation
    private var mScreenOrientation = Constant.LANDSCAPE
    lateinit var mWebView: WebView
    private var mWebViewUpdateDialog: AlertDialog? = null
    private var mLoadingError = false
    private var mLastBackTs: Long = 0
    private var mUploadMessage: ValueCallback<Array<Uri?>>? = null
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mMaxWidth = 0
    private var mMaxHeight = 0
    private val mImageSize = 45.dp

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
        d(TAG, "open url: %s", mUrl)
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
                    val settings = settings
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
                    WebView.setWebContentsDebuggingEnabled(TestUtil.isLoggable())
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
                    text = stringResource(id = R.string.loading_error_tips),
                    color = colorResource(id = R.color.loading_error_text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(id = R.string.refresh),
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
                    bitmap = ImageUtil.base64ToBitmap(Drawables.LOADING_BG).asImageBitmap(),
                    contentDescription = ""
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color= Color.Blue,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .size(25.dp)
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
                        bitmap = ImageUtil
                            .base64ToBitmap(Drawables.MENU_EXPANDED_BG)
                            .asImageBitmap(),
                        contentDescription = null,
                    )
                }
                Row {
                    Image(
                        bitmap = ImageUtil
                            .base64ToBitmap(if (mIsOpenMenu) Drawables.MENU_EXPANDED else Drawables.MENU_SHRINKED)
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
                            bitmap = ImageUtil
                                .base64ToBitmap(Drawables.MENU_REFRESH)
                                .asImageBitmap(),
                            contentDescription = null,
                            modifier = modifier.clickable {
                                mIsShowLoading = true
                                mIsOpenMenu = false
                                mWebView.reload()
                            }
                        )
                        Image(
                            bitmap = ImageUtil
                                .base64ToBitmap(Drawables.MENU_CLOSE)
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
        mIsShowLoading = false
        if (mLoadingError) {
            mIsShowWeb = false
        } else {
            mIsShowWeb = true
            // request focus ahead in case of any input behavior
            mWebView.requestFocus()
            mWebView.requestFocusFromTouch()
        }
        mWebPresenter?.checkGameFeature(url)
        mWebPresenter?.checkWebViewCompatibility()
        if (mLoadingError) {
            registerNetworkListener()
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
