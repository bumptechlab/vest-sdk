package poetry.sdk.bridge

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
            "copyText" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.copyText(params[0])
                }
            }

            "close"->{
                mBridgeInterface.close()
            }

            "refresh"->{
                mBridgeInterface.refresh()
            }

            "trackAdjustEvent" -> {
                if (params.size >= 2) {
                    mBridgeInterface.trackAdjustEvent(params[0], params[1])
                }
            }

            "getDeviceID" -> {
                result = mBridgeInterface.getDeviceID()
            }

            "getChannel" -> {
                result = mBridgeInterface.getChannel()
            }

            "getBrand" -> {
                result = mBridgeInterface.getBrand()
            }

            "getAdjustDeviceID" -> {
                result = mBridgeInterface.getAdjustDeviceID()
            }

            "getGoogleADID" -> {
                result = mBridgeInterface.getGoogleADID()
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

            "getBridgeVersion" -> {
                result = mBridgeInterface.getBridgeVersion().toString()
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

            "onWebViewLoadChanged" -> {
                if (params.isNotEmpty()) {
                    mBridgeInterface.onWebViewLoadChanged(params[0])
                }
            }
        }
        return result
    }
}
