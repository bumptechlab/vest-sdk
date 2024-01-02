package code.sdk.bridge

import java.io.File

interface BridgeCallback {
    fun loadUrl(url: String?)
    fun openUrlByBrowser(url: String?)
    fun openUrlByWebView(url: String?)
    fun openDataByWebView(type: Int, orientation: String?, hover: Boolean, data: String?)
    fun openApp(target: String?, fallbackUrl: String?)
    fun goBack()
    fun close()
    fun refresh()
    fun clearCache()
    fun saveImage(url: String?)
    fun saveImageDone(succeed: Boolean)
    fun savePromotionMaterialDone(succeed: Boolean)
    fun synthesizePromotionImage(qrCodeUrl: String?, size: Int, x: Int, y: Int)
    fun synthesizePromotionImageDone(succeed: Boolean)
    fun onHttpDnsHttpResponse(requestId: String?, response: String?)
    fun onHttpDnsWsResponse(requestId: String?, response: String?)
    fun shareUrl(url: String?)
    fun loginFacebook()
    fun logoutFacebook()
    fun preloadPromotionImageDone(succeed: Boolean)
    fun shareToWhatsApp(text: String?, file: File?)
}
