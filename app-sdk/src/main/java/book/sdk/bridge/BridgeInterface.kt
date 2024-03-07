package book.sdk.bridge

interface BridgeInterface {
    fun nativeLog(tag: String?, msg: String?)
    fun copyText(text: String?)
    fun getCopiedText(): String?
    fun showNativeToast(toast: String?)
    fun initAdjustID(adjustAppID: String?)
    fun trackAdjustEvent(eventToken: String?, jsonData: String?)
    fun trackAdjustEventStart(eventToken: String?)
    fun trackAdjustEventGreeting(eventToken: String?)
    fun trackAdjustEventAccess(eventToken: String?)
    fun trackAdjustEventUpdated(eventToken: String?)
    fun getDeviceID(): String?
    fun getDeviceInfoForLighthouse(): String?
    fun getSystemVersionCode(): Int
    fun getClientVersionCode(): Int
    fun getPackageName(): String?
    fun getAppName(): String?
    fun getChannel(): String?
    fun getBrand(): String?
    fun saveGameUrl(gameUrl: String?)
    fun saveAccountInfo(plainText: String?)
    fun getAccountInfo(): String?
    fun getAdjustDeviceID(): String?
    fun getGoogleADID(): String?
    fun getIDFA(): String?
    fun getReferID(): String?
    fun getAgentID(): String?
    fun setCocosData(key: String?, value: String?)
    fun getCocosData(key: String?): String?
    fun getCocosAllData(): String?
    fun getLighterHost(): String?
    fun getBridgeVersion(): Int
    fun isFacebookEnable(): Boolean
    fun getTDTargetCountry(): String?
    fun openUrlByBrowser(url: String?)
    fun openUrlByWebView(url: String?)
    fun openApp(target: String?, fallbackUrl: String?)
    fun loadUrl(url: String?)
    fun goBack()
    fun close()
    fun refresh()
    fun clearCache()
    fun saveImage(url: String?)
    fun savePromotionMaterial(materialUrl: String?)
    fun synthesizePromotionImage(qrCodeUrl: String?, size: Int, x: Int, y: Int)
    fun shareUrl(url: String?)
    fun loginFacebook()
    fun logoutFacebook()
    fun preloadPromotionImage(imageUrl: String?)
    fun shareToWhatsApp(text: String?)
    fun isHttpDnsEnable(): Boolean
    fun httpdns(host: String?): String?
    fun httpdnsInit(hosts: String?)
    fun httpdnsRequestSync(req: String?, body: ByteArray?): String?
    fun httpdnsRequestAsync(req: String?, body: ByteArray?)
    fun httpdnsWsOpen(req: String?)
    fun httpdnsWsSend(req: String?, body: ByteArray?): String?
    fun httpdnsWsClose(req: String?)
    fun httpdnsWsConnected(req: String?): String?
    fun getBuildVersion(): String?
    fun onAnalysisStart(accid: String?, cretime: Long)
    fun onAnalysisEnd()
    fun memoryInfo(): String?
    fun isEmulator(): Boolean
    fun commonData(): String?
    fun exitApp()
    fun handleNotification()
    fun onWebViewLoadChanged(json: String?)
}
