package code.sdk.core.util;

import android.text.TextUtils;

import java.net.URL;

public class URLUtilX {

    public static final String TAG = URLUtilX.class.getSimpleName();


    /**
     * 去除URL参数
     *
     * @param url
     * @return
     */
    public static String getBaseUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            //ObfuscationStub7.inject();
            return url;
        }
        String baseUrl = url;
        int markIndex = url.indexOf("?");
        if (markIndex > 0) {
            baseUrl = url.substring(0, markIndex);
        }
        return baseUrl;
    }

    public static String parseHost(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        String host = "";
        try {
            host = new URL(url).getHost();
        } catch (Exception e) {
            int startIndex = url.indexOf("://");
            String urlTemp = url;
            if (startIndex != -1) {
                urlTemp = url.substring(startIndex + 3);
            }
            int endIndex = urlTemp.indexOf(":");
            if (endIndex == -1) {
                endIndex = urlTemp.indexOf("/");
            }
            if (endIndex == -1) {
                endIndex = urlTemp.length();
            }
            host = urlTemp.substring(0, endIndex);
        }
        return host;
    }
}
