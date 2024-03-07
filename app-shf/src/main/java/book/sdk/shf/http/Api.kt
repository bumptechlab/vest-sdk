package book.sdk.shf.http

import io.reactivex.rxjava3.core.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface Api {
    @POST
    fun getGameInfo(@Url url: String, @Body json: RequestBody): Observable<ResponseBody>
}