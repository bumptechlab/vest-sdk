package code.sdk.shf;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import code.util.AppGlobal;
import code.util.LogUtil;
import code.sdk.core.util.NetworkUtil;
import code.sdk.core.util.PreferenceUtil;

/**
 * 校验从审核服务或者Firebase缓存下来的游戏链接是不是还可用
 * 如果不可用,不可用了就删除sp的缓存，下次进入会重新更新.
 */
public class GameUrlValidatorRunnable implements Runnable {
    public static final String TAG = GameUrlValidatorRunnable.class.getSimpleName();

    private URL mGameUrl;

    public GameUrlValidatorRunnable(String gameUrl) {
        try {
            mGameUrl = new URL(gameUrl);
        } catch (MalformedURLException e) {
            LogUtil.d(TAG, "malformed url, skip game url validation");
            PreferenceUtil.saveGameUrl(null);

            //ObfuscationStub2.inject();
        }
    }

    @Override
    public void run() {
        if (!NetworkUtil.isConnected(AppGlobal.getApplication())) {
            LogUtil.d(TAG, "no network, skip game url validation");
            //ObfuscationStub3.inject();
            return;
        }

        try {
            HostnameVerifier hv = (urlHostName, session) -> true;
            trustAllHttpsCertificates();
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
            HttpURLConnection connection = (HttpURLConnection) mGameUrl.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.connect();
            int responseCode = connection.getResponseCode();
            LogUtil.d(TAG, "game url[" + mGameUrl + "] response code = " + responseCode);
            if (HttpURLConnection.HTTP_OK <= responseCode
                    && responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                LogUtil.d(TAG, "game url[" + mGameUrl + "] validated");
                //ObfuscationStub5.inject();
            } else {
                LogUtil.d(TAG, "game url[" + mGameUrl + "] validate failed = " + responseCode);
                PreferenceUtil.saveGameUrl(null);
            }
        } catch (Exception e) {
            LogUtil.e(TAG, e.toString());
            LogUtil.d(TAG, "game url[" + mGameUrl + "] connect failed");
            PreferenceUtil.saveGameUrl(null);

            //ObfuscationStub6.inject();
        }
    }

    /**
     * 信任SSL证书
     *
     * @throws Exception
     */
    private static void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    static class miTM implements TrustManager, X509TrustManager {
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
            return;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }
    }
}
