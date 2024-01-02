package code.sdk.shf.http.interceptor

import code.util.AES.decryptAsStringByGCM
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

/**
 * 解密请求数据
 */
class ResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        //返回request
        val request: Request = chain.request()
        //返回response
        var response: Response = chain.proceed(request)
        //isSuccessful () ; 如果代码在[200..300]中，则返回true，这意味着请求已成功接收、理解和接受。
        if (response.isSuccessful) {
            //返回ResponseBody
            val responseBody = response.body
            if (responseBody != null) {
                response = try {
                    //获取bodyString
                    val source = responseBody.source()
                    source.request(Long.MAX_VALUE)
                    val buffer = source.buffer
                    val contentType = responseBody.contentType()
                    val body = buffer.clone().readByteArray()
                    //AES解码
                    val responseData = decryptAsStringByGCM(body)
                    //生成新的ResponseBody
                    val newResponseBody = ResponseBody.create(contentType, responseData!!)
                    //response
                    response.newBuilder().body(newResponseBody).build()
                } catch (e: Exception) {
                    //如果发生异常直接返回
                    e.printStackTrace()
                    return response
                }
            }
        }
        return response
    }
}