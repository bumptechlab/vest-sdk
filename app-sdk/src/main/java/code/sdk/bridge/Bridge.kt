package code.sdk.bridge

import code.util.ByteUtil.stringToBytes
import code.util.NumberUtil.parseInt
import code.util.NumberUtil.parseLong

/**
 * 所有桥接方法的父类，按照方法名进行调用分发，方法实现类请继承实现接口: BridgeInterface
 */
abstract class Bridge(private val mBridgeInterface: BridgeInterface) {
    /**
     * 处理业务层上层（Cocos或者H5）发送的调用
     *
     * @param request
     * @return
     */
    abstract fun post(request: String?): String?
    protected fun dispatchRequest(method: String?, params: Array<String?>?): String? {
        if (params == null) {
            return ""
        }
        var result:String? = ""
        when (method) {
            "nativeLog" -> {
                if (params.size >= 2) {
                    mBridgeInterface.nativeLog(params[0], params[1])
                }
            }

            "copyText" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.copyText(params[0])
                }
            }

            "getCopiedText" -> {
                result = mBridgeInterface.getCopiedText()
            }

            "showNativeToast" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.showNativeToast(params[0])
                }
            }

            "initAdjustID" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.initAdjustID(params[0])
                }
            }

            "trackAdjustEvent" -> {
                if (params.size >= 2) {
                    mBridgeInterface.trackAdjustEvent(params[0], params[1])
                }
            }

            "trackAdjustEventStart" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.trackAdjustEventStart(params[0])
                }
            }

            "trackAdjustEventGreeting" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.trackAdjustEventGreeting(params[0])
                }
            }

            "trackAdjustEventAccess" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.trackAdjustEventAccess(params[0])
                }
            }

            "trackAdjustEventUpdated" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.trackAdjustEventUpdated(params[0])
                }
            }

            "getDeviceID" -> {
                result = mBridgeInterface.getDeviceID()
            }

            "getDeviceInfoForLighthouse" -> {
                result = mBridgeInterface.getDeviceInfoForLighthouse()
            }

            "getSystemVersionCode" -> {
                result = mBridgeInterface.getSystemVersionCode().toString()
            }

            "getClientVersionCode" -> {
                result = mBridgeInterface.getClientVersionCode().toString()
            }

            "getPackageName" -> {
                result = mBridgeInterface.getPackageName()
            }

            "getAppName" -> {
                result = mBridgeInterface.getAppName()
            }

            "getChannel" -> {
                result = mBridgeInterface.getChannel()
            }

            "getBrand" -> {
                result = mBridgeInterface.getBrand()
            }

            "saveGameUrl" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.saveGameUrl(params[0])
                }
            }

            "saveAccountInfo" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.saveAccountInfo(params[0])
                }
            }

            "getAccountInfo" -> {
                result = mBridgeInterface.getAccountInfo()
            }

            "getAdjustDeviceID" -> {
                result = mBridgeInterface.getAdjustDeviceID()
            }

            "getGoogleADID" -> {
                result = mBridgeInterface.getGoogleADID()
            }

            "getIDFA" -> {
                result = mBridgeInterface.getIDFA()
            }

            "getReferID" -> {
                result = mBridgeInterface.getReferID()
            }

            "getAgentID" -> {
                result = mBridgeInterface.getAgentID()
            }

            "setCocosData" -> {
                if (params.size >= 2) {
                    mBridgeInterface.setCocosData(params[0], params[1])
                }
            }

            "getCocosData" -> {
                if (params.isNotEmpty()) {
                    result = mBridgeInterface.getCocosData(params[0])
                }
            }

            "getCocosAllData" -> {
                result = mBridgeInterface.getCocosAllData()
            }

            "getLighterHost" -> {
                result = mBridgeInterface.getLighterHost()
            }

            "getBridgeVersion" -> {
                result = mBridgeInterface.getBridgeVersion().toString()
            }

            "isFacebookEnable" -> {
                result = mBridgeInterface.isFacebookEnable().toString()
            }

            "getTDTargetCountry" -> {
                result = mBridgeInterface.getTDTargetCountry()
            }

            "openUrlByBrowser" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.openUrlByBrowser(params[0])
                }
            }

            "openUrlByWebView" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.openUrlByWebView(params[0])
                }
            }

            "openApp" -> {
                if (params.size >= 2) {
                    mBridgeInterface.openApp(params[0], params[1])
                }
            }

            "loadUrl" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.loadUrl(params[0])
                }
            }

            "goBack" -> {
                mBridgeInterface.goBack()
            }

            "close" -> {
                mBridgeInterface.close()
            }

            "refresh" -> {
                mBridgeInterface.refresh()
            }

            "clearCache" -> {
                mBridgeInterface.clearCache()
            }

            "saveImage" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.saveImage(params[0])
                }
            }

            "savePromotionMaterial" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.savePromotionMaterial(params[0])
                }
            }

            "synthesizePromotionImage" -> {
                if (params.size >= 4) {
                    mBridgeInterface.synthesizePromotionImage(
                        params[0],
                        parseInt(params[1]),
                        parseInt(params[2]),
                        parseInt(params[3])
                    )
                }
            }

            "shareUrl" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.shareUrl(params[0])
                }
            }

            "loginFacebook" -> {
                mBridgeInterface.loginFacebook()
            }

            "logoutFacebook" -> {
                mBridgeInterface.logoutFacebook()
            }

            "preloadPromotionImage" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.preloadPromotionImage(params[0])
                }
            }

            "shareToWhatsApp" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.shareToWhatsApp(params[0])
                }
            }

            "isHttpDnsEnable" -> {
                result = mBridgeInterface.isHttpDnsEnable().toString()
            }

            "httpdns" -> {
                if (params.isNotEmpty()) {
                    result = mBridgeInterface.httpdns(params[0])
                }
            }

            "httpdnsInit" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.httpdnsInit(params[0])
                }
            }

            "httpdnsRequestSync" -> {
                if (params.size >= 2) {
                    result = mBridgeInterface.httpdnsRequestSync(
                        params[0], stringToBytes(params[1])
                    )
                }
            }

            "httpdnsRequestAsync" -> {
                if (params.size >= 2) {
                    mBridgeInterface.httpdnsRequestAsync(params[0], stringToBytes(params[1]))
                }
            }

            "httpdnsWsOpen" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.httpdnsWsOpen(params[0])
                }
            }

            "httpdnsWsSend" -> {
                if (params.size >= 2) {
                    result = mBridgeInterface.httpdnsWsSend(params[0], stringToBytes(params[1]))
                }
            }

            "httpdnsWsClose" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.httpdnsWsClose(params[0])
                }
            }

            "httpdnsWsConnected" -> {
                if (params.isNotEmpty()) {
                    result = mBridgeInterface.httpdnsWsConnected(params[0])
                }
            }

            "getBuildVersion" -> {
                result = mBridgeInterface.getBuildVersion()
            }

            "onAnalysisStart" -> {
                if (params.size >= 2) {
                    mBridgeInterface.onAnalysisStart(params[0], parseLong(params[1]))
                }
            }

            "onAnalysisEnd" -> {
                mBridgeInterface.onAnalysisEnd()
            }

            "memoryInfo" -> {
                result = mBridgeInterface.memoryInfo()
            }

            "isEmulator" -> {
                result = mBridgeInterface.isEmulator().toString()
            }

            "commonData" -> {
                result = mBridgeInterface.commonData()
            }

            "exitApp" -> {
                mBridgeInterface.exitApp()
            }

            "handleNotification" -> {
                mBridgeInterface.handleNotification()
            }

            "onWebViewLoadChanged" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.onWebViewLoadChanged(params[0])
                }
            }
        }
        return result
    }
}
