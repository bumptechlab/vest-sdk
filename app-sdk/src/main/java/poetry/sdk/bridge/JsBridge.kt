package poetry.sdk.bridge

import android.webkit.JavascriptInterface

/**
 * 这里只能定义一个JavascriptInterface方法
 */
class JsBridge(callback: BridgeCallback) : JsBridgeCore(callback) {

    private val TAG = JsBridge::class.java.simpleName

    @JavascriptInterface
    fun info(request: String?): String? {
        return post(request)
    }

}
