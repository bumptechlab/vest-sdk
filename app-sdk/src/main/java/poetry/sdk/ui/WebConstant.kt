package poetry.sdk.ui

object WebConstant {

    val WEBGL_SCRIPT: String = """
    (function () {
        try {
            var canvas = document.createElement('canvas');
            return !!(window.WebGLRenderingContext && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')));
        } catch (e) {
            return false;
        }
    }())
    """
    val SYSTEM_WEBVIEW_PACKAGE = "com.google.android.webview"
    val MINI_SYSTEM_WEBVIEW_VERSION = "64.0.3282.29"
    val MINI_SYSTEM_WEBVIEW_VERSION_CODE = 328202950
}