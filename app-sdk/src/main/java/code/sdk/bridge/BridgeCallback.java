package code.sdk.bridge;

import java.io.File;

public interface BridgeCallback {
    void loadUrl(String url);

    void openUrlByBrowser(String url);

    void openUrlByWebView(String url);

    void openApp(String target, String fallbackUrl);

    void goBack();

    void close();

    void refresh();

    void clearCache();

    void saveImage(String url);

    void saveImageDone(boolean succeed);

    void savePromotionMaterialDone(boolean succeed);

    void synthesizePromotionImage(String qrCodeUrl, int size, int x, int y);

    void synthesizePromotionImageDone(boolean succeed);

    void onHttpDnsHttpResponse(String requestId, String response);

    void onHttpDnsWsResponse(String requestId, String response);

    void shareUrl(String url);

    void loginFacebook();

    void logoutFacebook();

    void preloadPromotionImageDone(boolean succeed);

    void shareToWhatsApp(String text, File file);
}
