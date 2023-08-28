package com.androidx.h5.utils;

import java.util.concurrent.TimeUnit;

import code.util.LogUtil;
import code.util.MySSLSocketClient;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpUtil {

    private static OkHttpClient sOkHttpClient;
    private static final int DEFAULT_READ_TIMEOUT = 8000; //读取超时，单位毫秒
    private static final int DEFAULT_CONNECT_TIMEOUT = 8000; //连接超时，单位毫秒

    public static OkHttpClient getOkHttpClient() {
        if (sOkHttpClient != null) {
            return sOkHttpClient;
        }
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (LogUtil.isDebug()) { //这行必须加 不然默认不打印
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        sOkHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .sslSocketFactory(MySSLSocketClient.getSSLSocketFactory(), MySSLSocketClient.getX509TrustManager())
                .hostnameVerifier(MySSLSocketClient.getHostnameVerifier())
                .build();
        return sOkHttpClient;
    }
}
