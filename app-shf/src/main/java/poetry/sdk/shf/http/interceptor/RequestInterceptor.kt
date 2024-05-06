package poetry.sdk.shf.http.interceptor

import poetry.util.AES
import poetry.util.AES.encryptByGCM
import poetry.util.LogUtil.d
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * 加密请求数据
 */
class RequestInterceptor : Interceptor {

    private val TAG = RequestInterceptor::class.java.simpleName

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val method = request.method.lowercase(Locale.getDefault())
            .trim { it <= ' ' } //请求方式例如：get delete put post
        if (method == "post") {
            //构建新的请求体
            val requestBody = request.body
            //判断类型
            val contentType = requestBody!!.contentType()
            if (contentType != null) {
                /*如果是二进制上传  则不进行加密*/
                if (contentType.type.lowercase(Locale.getDefault()) == "multipart") {
                    return chain.proceed(request)
                }
            }
            // 获取请求的数据
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            val requestData = bodyToString(request)
            d(TAG, "intercept request body: %s", requestData)
            val bytes = encryptByGCM(requestData.toByteArray(StandardCharsets.UTF_8), AES.MODE256)
            if (null != bytes) {
                val mediaType = "application/octet-stream".toMediaType()
                val bodyBuilder = RequestBody.create(mediaType, bytes)
                //构建新的requestBuilder
                val newRequestBuilder = request.newBuilder()
                newRequestBuilder.post(bodyBuilder)
                request = newRequestBuilder.build()
            }
        }
        return chain.proceed(request)
    }

    /**
     * post 请求参数获取
     */
    private fun bodyToString(request: Request): String {
        val copy = request.newBuilder().build()
        val buffer = Buffer()
        try {
            copy.body!!.writeTo(buffer)
        } catch (e: IOException) {
            return "something error,when show requestBody"
        }
        return buffer.readUtf8()
    }
}