package book.sdk.util

import book.sdk.core.util.TestUtil
import book.util.MySSLSocketClient.getX509TrustManager
import book.util.TlsSniSocketFactory
import book.util.TrueHostnameVerifier
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object OkHttpUtil {
    private val mHostOkHttpClient = HashMap<String, OkHttpClient?>()
    private const val CONNECT_TIMEOUT = 8000
    private const val READ_TIMEOUT = 8000
    fun getOkHttpClient(domain: String): OkHttpClient? {
        var okHttpClient = mHostOkHttpClient[domain]
        if (okHttpClient != null) {
            return okHttpClient
        }
        val interceptor = HttpLoggingInterceptor()
        if (TestUtil.isLoggable()) { //这行必须加 不然默认不打印
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE)
        }
        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .sslSocketFactory(TlsSniSocketFactory(domain), getX509TrustManager())
            .hostnameVerifier(TrueHostnameVerifier(domain))
            .connectTimeout(CONNECT_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
        mHostOkHttpClient[domain] = okHttpClient
        return okHttpClient
    }
}
