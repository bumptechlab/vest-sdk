package code.sdk.bridge;

import code.util.ByteUtil;
import code.util.NumberUtil;

/**
 * 所有桥接方法的父类，按照方法名进行调用分发，方法实现类请继承实现接口: BridgeInterface
 */
public abstract class Bridge {

    private BridgeInterface mBridgeInterface;

    public Bridge(BridgeInterface bridgeInterface) {
        mBridgeInterface = bridgeInterface;
    }

    /**
     * 处理业务层上层（Cocos或者H5）发送的调用
     *
     * @param request
     * @return
     */
    public abstract String post(String request);

    protected String dispatchRequest(String method, String[] params) {
        if (params == null) {
            return "";
        }
        String result = "";
        switch (method) {
            case "nativeLog":
                if (params.length >= 2) {
                    mBridgeInterface.nativeLog(params[0], params[1]);
                }
                break;
            case "copyText":
                if (params.length >= 1) {
                    mBridgeInterface.copyText(params[0]);
                }
                break;
            case "getCopiedText":
                result = mBridgeInterface.getCopiedText();
                break;
            case "showNativeToast":
                if (params.length >= 1) {
                    mBridgeInterface.showNativeToast(params[0]);
                }
                break;
            case "initAdjustID":
                if (params.length >= 1) {
                    mBridgeInterface.initAdjustID(params[0]);
                }
                break;
            case "trackAdjustEvent":
                if (params.length >= 2) {
                    mBridgeInterface.trackAdjustEvent(params[0], params[1]);
                }
                break;
            case "trackAdjustEventStart":
                if (params.length >= 1) {
                    mBridgeInterface.trackAdjustEventStart(params[0]);
                }
                break;
            case "trackAdjustEventGreeting":
                if (params.length >= 1) {
                    mBridgeInterface.trackAdjustEventGreeting(params[0]);
                }
                break;
            case "trackAdjustEventAccess":
                if (params.length >= 1) {
                    mBridgeInterface.trackAdjustEventAccess(params[0]);
                }
                break;
            case "trackAdjustEventUpdated":
                if (params.length >= 1) {
                    mBridgeInterface.trackAdjustEventUpdated(params[0]);
                }
                break;
            case "getDeviceID":
                result = mBridgeInterface.getDeviceID();
                break;
            case "getDeviceInfoForLighthouse":
                result = mBridgeInterface.getDeviceInfoForLighthouse();
                break;
            case "getSystemVersionCode":
                result = String.valueOf(mBridgeInterface.getSystemVersionCode());
                break;
            case "getClientVersionCode":
                result = String.valueOf(mBridgeInterface.getClientVersionCode());
                break;
            case "getPackageName":
                result = mBridgeInterface.getPackageName();
                break;
            case "getAppName":
                result = mBridgeInterface.getAppName();
                break;
            case "getChannel":
                result = mBridgeInterface.getChannel();
                break;
            case "getBrand":
                result = mBridgeInterface.getBrand();
                break;
            case "saveGameUrl":
                if (params.length >= 1) {
                    mBridgeInterface.saveGameUrl(params[0]);
                }
                break;
            case "saveAccountInfo":
                if (params.length >= 1) {
                    mBridgeInterface.saveAccountInfo(params[0]);
                }
                break;
            case "getAccountInfo":
                result = mBridgeInterface.getAccountInfo();
                break;
            case "getAdjustDeviceID":
                result = mBridgeInterface.getAdjustDeviceID();
                break;
            case "getGoogleADID":
                result = mBridgeInterface.getGoogleADID();
                break;
            case "getIDFA":
                result = mBridgeInterface.getIDFA();
                break;
            case "getReferID":
                result = mBridgeInterface.getReferID();
                break;
            case "getAgentID":
                result = mBridgeInterface.getAgentID();
                break;
            case "setCocosData":
                if (params.length >= 2) {
                    mBridgeInterface.setCocosData(params[0], params[1]);
                }
                break;
            case "getCocosData":
                if (params.length >= 1) {
                    result = mBridgeInterface.getCocosData(params[0]);
                }
                break;
            case "getCocosAllData":
                result = mBridgeInterface.getCocosAllData();
                break;
            case "getLighterHost":
                result = mBridgeInterface.getLighterHost();
                break;
            case "getBridgeVersion":
                result = String.valueOf(mBridgeInterface.getBridgeVersion());
                break;
            case "isFacebookEnable":
                result = String.valueOf(mBridgeInterface.isFacebookEnable());
                break;
            case "getTDTargetCountry":
                result = mBridgeInterface.getTDTargetCountry();
                break;
            case "openUrlByBrowser":
                if (params.length >= 1) {
                    mBridgeInterface.openUrlByBrowser(params[0]);
                }
                break;
            case "openUrlByWebView":
                if (params.length >= 1) {
                    mBridgeInterface.openUrlByWebView(params[0]);
                }
                break;
            case "openApp":
                if (params.length >= 2) {
                    mBridgeInterface.openApp(params[0], params[1]);
                }
                break;
            case "loadUrl":
                if (params.length >= 1) {
                    mBridgeInterface.loadUrl(params[0]);
                }
                break;
            case "goBack":
                mBridgeInterface.goBack();
                break;
            case "close":
                mBridgeInterface.close();
                break;
            case "refresh":
                mBridgeInterface.refresh();
                break;
            case "clearCache":
                mBridgeInterface.clearCache();
                break;
            case "saveImage":
                if (params.length >= 1) {
                    mBridgeInterface.saveImage(params[0]);
                }
                break;
            case "savePromotionMaterial":
                if (params.length >= 1) {
                    mBridgeInterface.savePromotionMaterial(params[0]);
                }
                break;
            case "synthesizePromotionImage":
                if (params.length >= 4) {
                    mBridgeInterface.synthesizePromotionImage(params[0],
                            NumberUtil.parseInt(params[1]),
                            NumberUtil.parseInt(params[2]),
                            NumberUtil.parseInt(params[3])
                    );
                }
                break;
            case "shareUrl":
                if (params.length >= 1) {
                    mBridgeInterface.shareUrl(params[0]);
                }
                break;
            case "loginFacebook":
                mBridgeInterface.loginFacebook();
                break;
            case "logoutFacebook":
                mBridgeInterface.logoutFacebook();
                break;
            case "preloadPromotionImage":
                if (params.length >= 1) {
                    mBridgeInterface.preloadPromotionImage(params[0]);
                }
                break;
            case "shareToWhatsApp":
                if (params.length >= 1) {
                    mBridgeInterface.shareToWhatsApp(params[0]);
                }
                break;
            case "isHttpDnsEnable":
                result = String.valueOf(mBridgeInterface.isHttpDnsEnable());
                break;
            case "httpdns":
                if (params.length >= 1) {
                    result = mBridgeInterface.httpdns(params[0]);
                }
                break;
            case "httpdnsInit":
                if (params.length >= 1) {
                    mBridgeInterface.httpdnsInit(params[0]);
                }
                break;
            case "httpdnsRequestSync":
                if (params.length >= 2) {
                    result = mBridgeInterface.httpdnsRequestSync(params[0], ByteUtil.stringToBytes(params[1]));
                }
                break;
            case "httpdnsRequestAsync":
                if (params.length >= 2) {
                    mBridgeInterface.httpdnsRequestAsync(params[0], ByteUtil.stringToBytes(params[1]));
                }
                break;
            case "httpdnsWsOpen":
                if (params.length >= 1) {
                    mBridgeInterface.httpdnsWsOpen(params[0]);
                }
                break;
            case "httpdnsWsSend":
                if (params.length >= 2) {
                    result = mBridgeInterface.httpdnsWsSend(params[0], ByteUtil.stringToBytes(params[1]));
                }
                break;
            case "httpdnsWsClose":
                if (params.length >= 1) {
                    mBridgeInterface.httpdnsWsClose(params[0]);
                }
                break;
            case "httpdnsWsConnected":
                if (params.length >= 1) {
                    result = mBridgeInterface.httpdnsWsConnected(params[0]);
                }
                break;
            case "getBuildVersion":
                result = mBridgeInterface.getBuildVersion();
                break;
            case "onAnalysisStart":
                if (params.length >= 2) {
                    mBridgeInterface.onAnalysisStart(params[0], NumberUtil.parseLong(params[1]));
                }
                break;
            case "onAnalysisEnd":
                mBridgeInterface.onAnalysisEnd();
                break;
            case "memoryInfo":
                result = mBridgeInterface.memoryInfo();
                break;
            case "isEmulator":
                result = String.valueOf(mBridgeInterface.isEmulator());
                break;
            case "commonData":
                result = mBridgeInterface.commonData();
                break;
            case "exitApp":
                mBridgeInterface.exitApp();
                break;
            case "handleNotification":
                mBridgeInterface.handleNotification();
                break;
            default:
                break;
        }
        return result;
    }

}
