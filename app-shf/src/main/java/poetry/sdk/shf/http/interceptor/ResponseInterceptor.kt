package poetry.sdk.shf.http.interceptor

import android.text.TextUtils
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import poetry.sdk.core.util.ConfigPreference
import poetry.util.AES
import java.io.IOException
import java.util.StringTokenizer


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
                    //获取BodyString
                    val source = responseBody.source()
                    source.request(Long.MAX_VALUE)
                    val buffer = source.buffer
                    val contentType = responseBody.contentType()
                    val body = buffer.clone().readByteArray()
                    val nonceValue = getNonceValue(request)
                    //AES解码
                    val responseData = AES.decryptAsStringByGCM(
                        body, nonceValue, ConfigPreference.readInterfaceEncValue()!!
                    )
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

    private fun getNonceValue(request: Request): String {
        val nonce = ConfigPreference.readInterfaceNonce()

        //MODE_HEADER
        val headerNonceValue = request.headers[nonce]
        if (!TextUtils.isEmpty(headerNonceValue)) return headerNonceValue!!

        //MODE_COOKIE
        val cookie = request.headers["Cookie"]
        var cookieNonceValue: String? = null
        if (cookie != null) {
            val tokenizer = StringTokenizer(cookie, ",")
            while (tokenizer.hasMoreTokens()) {
                val token = tokenizer.nextToken()
                val index = token.indexOf('=')
                if (index != -1) {
                    val key = token.substring(0, index)
                    val value = token.substring(index + 1)
                    println("Key: $key, Value: $value")
                    if (key == nonce) {
                        cookieNonceValue = value
                        break
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(cookieNonceValue)) return cookieNonceValue!!

        //MODE_PATH
        val pathNonceValue = request.url.queryParameter(nonce)
        if (!TextUtils.isEmpty(pathNonceValue)) return pathNonceValue!!

        //MODE_NON
        return ConfigPreference.readInterfaceNonceValue()!!
    }
}