package poetry.sdk.shf.http

import io.reactivex.rxjava3.core.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Url

interface Api {
    @POST
    fun postGameInfo(
        @Url url: String,
        @Body json: RequestBody,
        @HeaderMap header: Map<String, String?>
    ): Observable<ResponseBody>

    @PUT
    fun putGameInfo(
        @Url url: String,
        @Body json: RequestBody,
        @HeaderMap header: Map<String, String?>
    ): Observable<ResponseBody>

    @PATCH
    fun patchGameInfo(
        @Url url: String,
        @Body json: RequestBody,
        @HeaderMap header: Map<String, String?>
    ): Observable<ResponseBody>

}