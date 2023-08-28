package code.sdk.util;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import code.sdk.core.util.TestUtil;
import code.util.MySSLSocketClient;
import code.util.TlsSniSocketFactory;
import code.util.TrueHostnameVerifier;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpUtil {
    private static HashMap<String, OkHttpClient> mHostOkHttpClient = new HashMap<>();
    private static final int CONNECT_TIMEOUT = 8000;
    private static final int READ_TIMEOUT = 8000;

    public static OkHttpClient getOkHttpClient(String domain) {
        OkHttpClient okHttpClient = mHostOkHttpClient.get(domain);
        if (okHttpClient != null) {
            return okHttpClient;
        }
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (TestUtil.isLoggable()) { //这行必须加 不然默认不打印
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .sslSocketFactory(new TlsSniSocketFactory(domain), MySSLSocketClient.getX509TrustManager())
                .hostnameVerifier(new TrueHostnameVerifier(domain))
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(false)
                .build();
        mHostOkHttpClient.put(domain, okHttpClient);
        return okHttpClient;
    }
}
