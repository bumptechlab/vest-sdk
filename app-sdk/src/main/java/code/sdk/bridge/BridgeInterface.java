package code.sdk.bridge;

public interface BridgeInterface {

    public void nativeLog(String tag, String msg);

    public void copyText(String text);

    public String getCopiedText();

    public void showNativeToast(String toast);

    public void initAdjustID(String adjustAppID);

    public void trackAdjustEvent(String eventToken, String jsonData);

    public void trackAdjustEventStart(String eventToken);

    public void trackAdjustEventGreeting(String eventToken);

    public void trackAdjustEventAccess(String eventToken);

    public void trackAdjustEventUpdated(String eventToken);

    public String getDeviceID();

    public String getDeviceInfoForLighthouse();

    public int getSystemVersionCode();

    public int getClientVersionCode();

    public String getPackageName();

    public String getAppName();

    public String getChannel();

    public String getBrand();

    public void saveGameUrl(String gameUrl);

    public void saveAccountInfo(String plainText);

    public String getAccountInfo();

    public String getAdjustDeviceID();

    public String getGoogleADID();

    public String getIDFA();

    public String getReferID();

    public String getAgentID();

    public void setCocosData(String key, String value);

    public String getCocosData(String key);

    public String getCocosAllData();

    public String getLighterHost();

    public int getBridgeVersion();

    public boolean isFacebookEnable();

    public String getTDTargetCountry();

    public void openUrlByBrowser(String url);

    public void openUrlByWebView(String url);

    public void openApp(String target, String fallbackUrl);

    public void loadUrl(String url);

    public void goBack();

    public void close();

    public void refresh();

    public void clearCache();

    public void saveImage(String url);

    public void savePromotionMaterial(String materialUrl);

    public void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y);

    public void shareUrl(String url);

    public void loginFacebook();

    public void logoutFacebook();

    public void preloadPromotionImage(String imageUrl);

    public void shareToWhatsApp(String text);

    public boolean isHttpDnsEnable();

    public String httpdns(String host);

    public void httpdnsInit(String hosts);

    public String httpdnsRequestSync(String req, byte[] body);

    public void httpdnsRequestAsync(String req, byte[] body);

    public void httpdnsWsOpen(String req);

    public String httpdnsWsSend(String req, byte[] body);

    public void httpdnsWsClose(String req);

    public String httpdnsWsConnected(String req);

    public String getBuildVersion();

    public void onAnalysisStart(String accid, long cretime);

    public void onAnalysisEnd();

    public String memoryInfo();

    public boolean isEmulator();

    public String commonData();

    public void exitApp();

    public void handleNotification();
}
