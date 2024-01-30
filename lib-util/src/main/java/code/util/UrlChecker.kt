package code.util

import android.webkit.URLUtil
import code.util.NetworkUtil.isConnected
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object UrlChecker {
    private const val TIMEOUT = 2000

    fun isUrlAvailable(url: String?): Boolean {
        return if (!isConnected(AppGlobal.application!!)) {
            URLUtil.isValidUrl(url)
        } else try {
            trustAllHttpsCertificates()
            HttpsURLConnection.setDefaultHostnameVerifier { _: String?, _: SSLSession? -> true }
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.instanceFollowRedirects = true
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            try {
                conn.connect()
            } catch (e: Exception) {
                return false
            }
            val code = conn.responseCode
            code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_BAD_REQUEST
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 信任SSL证书
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun trustAllHttpsCertificates() {
        val trustAllCerts = arrayOfNulls<TrustManager>(1)
        trustAllCerts[0] = MyTrustManager()
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, null)
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
    }

    private class MyTrustManager : TrustManager, X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
        }
    }
}