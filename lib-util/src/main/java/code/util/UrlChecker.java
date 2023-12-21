package code.util;

import android.webkit.URLUtil;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class UrlChecker {
    private static final int TIMEOUT = 2000;

    public static boolean isUrlAvailable(String url) {
        if (!NetworkUtil.isConnected(AppGlobal.getApplication())) {
            return URLUtil.isValidUrl(url);
        }
        try {
            trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier((urlHostName, session) -> true);
            URL URL = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) URL.openConnection();
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

    /**
     * 信任SSL证书
     *
     * @throws Exception
     */
    private static void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        trustAllCerts[0] = new MyTrustManager();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private static class MyTrustManager implements TrustManager, X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
        }
    }

}
