package code.sdk.bridge;

import android.webkit.JavascriptInterface;

/**
 * 这里只能定义一个JavascriptInterface方法
 */
public class JsBridge extends JsBridgeCore {
    public static final String TAG = JsBridge.class.getSimpleName();

    public JsBridge(BridgeCallback callback) {
        super(callback);
    }

    @JavascriptInterface
    public String fly(String request) {
        return post(request);
    }

}
