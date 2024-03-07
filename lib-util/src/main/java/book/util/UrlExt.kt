package book.util

import android.webkit.URLUtil
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

const val TIMEOUT = 2000
fun String?.isUrlAvailable(): Boolean {
    return if (!NetworkUtil.isConnected(AppGlobal.application!!)) {
        URLUtil.isValidUrl(this)
    } else try {
        val connection = URL(this).openConnection() as HttpURLConnection
        if (connection is HttpsURLConnection) {
            val httpsConnection = connection as HttpsURLConnection
            httpsConnection.setHostnameVerifier { host, session -> true }
            val trustAllCerts = arrayOfNulls<TrustManager>(1)
            trustAllCerts[0] = MySSLSocketClient.getX509TrustManager()
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, null)
            httpsConnection.sslSocketFactory = sc.socketFactory
        }
        connection.useCaches = false
        connection.instanceFollowRedirects = true
        connection.connectTimeout = TIMEOUT
        connection.readTimeout = TIMEOUT
        connection.connect()
        val code = connection.responseCode
        code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_BAD_REQUEST
    } catch (e: Exception) {
        false
    }
}