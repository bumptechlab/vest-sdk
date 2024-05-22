package poetry.sdk.bridge

import android.text.TextUtils
import android.webkit.JavascriptInterface
import poetry.util.LogUtil

/**
 *
 * @author stefan
 * @date 2024-05-14
 */
class GWJsBridge(private val mCallback: BridgeCallback?) {
    private val mJsBridgeImpl: JsBridgeImpl = JsBridgeImpl(mCallback)

    @JavascriptInterface
    fun initAdjustID(adjustId: String?) {
        LogUtil.e(javaClass.name, "GWJsBridge.initAdjustID:$adjustId")
        //此处不作实现，Adjust已初始化
    }

    @JavascriptInterface
    fun close() {
        mCallback?.finish()
    }

    @JavascriptInterface
    fun goBack() {
        mCallback?.goBack()
    }

    @JavascriptInterface
    fun openUrlByWebview(url: String?) {
        if (TextUtils.isEmpty(url)) return
        mJsBridgeImpl.openUrlByWebView(url)
    }

    @JavascriptInterface
    fun trackAdjustEvent(eventToken: String?, data: String) {
        LogUtil.e(javaClass.name, "GWJsBridge.trackAdjustEvent:$eventToken,data=$data")
        mJsBridgeImpl.trackAdjustEvent(eventToken, data)
    }

    @JavascriptInterface
    fun openUrlByBrowser(url: String?) {
        if (TextUtils.isEmpty(url)) return
        mJsBridgeImpl.openUrlByBrowser(url)
    }

    @JavascriptInterface
    fun loginFacebook(json: String?) {
        LogUtil.i(javaClass.name, "GWJsBridge.postMessage:$json")
        //仅用作拦截，否则H5会跳转到无效网址
    }

    @JavascriptInterface
    fun startZalo(json: String?) {
        LogUtil.i(javaClass.name, "GWJsBridge.startZalo:$json")
        //仅用作拦截，否则H5会跳转到无效网址
    }

    @JavascriptInterface
    fun copyText(text: String?) {
        mJsBridgeImpl.copyText(text)
    }

    @JavascriptInterface
    fun refresh() {
        mCallback?.refresh()
    }

}