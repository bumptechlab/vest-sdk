package code.sdk.download;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface DownloadApi {

    @GET
    @Streaming
    Observable<ResponseBody> download(@Url String url);
}
