package code.util;

import java.net.HttpURLConnection;
import java.net.URL;

public class UrlUtil {
    private static final int TIMEOUT = 2000;

    public static boolean isValidUrl(String web) {
        try {
            URL url = new URL(web);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            try {
                conn.connect();
            } catch (Exception e) {
                return false;
            }
            int code = conn.getResponseCode();
            if ((code >= HttpURLConnection.HTTP_OK) && (code < HttpURLConnection.HTTP_BAD_REQUEST)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
