package code.sdk.shf.http;

import android.text.TextUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import code.sdk.core.util.ConfigPreference;
import code.sdk.shf.http.interceptor.RequestInterceptor;
import code.sdk.shf.http.interceptor.ResponseInterceptor;
import code.util.LogUtil;
import code.util.MySSLSocketClient;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class HttpClient {
    private static final int DEFAULT_READ_TIMEOUT = 8000; //读取超时，单位毫秒
    private static final int DEFAULT_WRITE_TIMEOUT = 8000; //写入超时，单位毫秒
    private static final int DEFAULT_CONNECT_TIMEOUT = 8000; //连接超时，单位毫秒

    private HttpClient() {

    }

    private static HttpClient mHttpClient = null;
    private static Api mApi = null;

    public static HttpClient getInstance() {
        if (mHttpClient == null) {
            synchronized (HttpClient.class) {
                if (mHttpClient == null) {
                    mHttpClient = new HttpClient();
                }
            }
        }
        return mHttpClient;
    }

    public Api getApi() {
        if (mApi == null) {
            mApi = getRetrofit().create(Api.class);
        }
        return mApi;
    }

    private Retrofit getRetrofit() {
        String url = ConfigPreference.readSHFBaseHost();
        if (TextUtils.isEmpty(url)) {
            url = "https://getRetrofit.com";
        }
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    private OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (LogUtil.isDebug()) { //这行必须加 不然默认不打印
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        return new OkHttpClient.Builder()
                .addInterceptor(new ResponseInterceptor())
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .sslSocketFactory(MySSLSocketClient.getSSLSocketFactory(), MySSLSocketClient.getX509TrustManager())
                .hostnameVerifier(MySSLSocketClient.getHostnameVerifier())
                .build();
    }

    public String buildUrl(String host, String api, Map<String, String> query) {
        HttpUrl.Builder builder = HttpUrl.parse(host).newBuilder();
        builder.addEncodedPathSegment(api);
        if (query != null && !query.isEmpty()) {
            for (Map.Entry<String, String> entrySet : query.entrySet()) {
                builder.addEncodedQueryParameter(entrySet.getKey(), entrySet.getValue());
            }
        }
        return builder.build().toString();
    }


    public <T> ObservableTransformer<T, T> ioSchedulers() {
        return upstream -> upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


}
