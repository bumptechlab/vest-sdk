package poetry.sdk.bridge

interface BridgeCallback {
    fun goBack()
    fun finish()
    fun refresh()
    fun openUrlByBrowser(url: String?)
    fun openUrlByWebView(url: String?)
    fun openDataByWebView(type: Int, orientation: String?, hover: Boolean, data: String?)
}
