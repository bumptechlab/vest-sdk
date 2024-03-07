package book.sdk.download

import android.text.TextUtils
import book.sdk.core.util.ConfigPreference
import book.util.LogUtil.isDebug
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import java.util.concurrent.TimeUnit

class DownloadClient private constructor() {
    fun getApi(): DownloadApi {
        if (mApi == null) {
            mApi = retrofit.create(DownloadApi::class.java)
        }
        return mApi!!
    }

    private val retrofit: Retrofit
        get() {
            var url = ConfigPreference.readSHFBaseHost()
            if (TextUtils.isEmpty(url)) {
                url = "https://getRetrofit.com"
            }
            return Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
        }
    private val okHttpClient: OkHttpClient
        get() {
            val interceptor = HttpLoggingInterceptor()
            if (isDebug()) { //这行必须加 不然默认不打印
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            } else {
                interceptor.setLevel(HttpLoggingInterceptor.Level.NONE)
            }
            return OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }

    fun <T : Any> ioSchedulers(): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream: Observable<T> ->
            upstream.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        }
    }

    companion object {
        private const val DEFAULT_READ_TIMEOUT = 15000 //读取超时，单位毫秒
        private const val DEFAULT_CONNECT_TIMEOUT = 15000 //连接超时，单位毫秒
        private var mApi: DownloadApi? = null

        val mInstance by lazy { DownloadClient() }


    }
}
