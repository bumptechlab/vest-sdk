package code.sdk.shf.http;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface Api {

    @POST
    Observable<ResponseBody> getGameInfo(@Url String url, @Body RequestBody json);

}
