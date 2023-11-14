package code.sdk.download;

import android.text.TextUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import code.sdk.core.util.ConfigPreference;
import code.util.LogUtil;
import code.util.MySSLSocketClient;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;

public class DownloadClient {
    private static final int DEFAULT_READ_TIMEOUT = 15000; //读取超时，单位毫秒
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000; //连接超时，单位毫秒

    private DownloadClient() {

    }

    private static DownloadClient mHttpClient = null;
    private static DownloadApi mApi = null;

    public static DownloadClient getInstance() {
        if (mHttpClient == null) {
            synchronized (DownloadClient.class) {
                if (mHttpClient == null) {
                    mHttpClient = new DownloadClient();
                }
            }
        }
        return mHttpClient;
    }

    public DownloadApi getApi() {
        if (mApi == null) {
            mApi = getRetrofit().create(DownloadApi.class);
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
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    public <T> ObservableTransformer<T, T> ioSchedulers() {
        return upstream -> upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
