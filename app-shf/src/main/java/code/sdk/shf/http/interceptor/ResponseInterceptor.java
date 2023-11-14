package code.sdk.shf.http.interceptor;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import code.util.AES;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * 解密请求数据
 */
public class ResponseInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        //返回request
        Request request = chain.request();
        //返回response
        Response response = chain.proceed(request);
        //isSuccessful () ; 如果代码在[200..300]中，则返回true，这意味着请求已成功接收、理解和接受。
        if (response.isSuccessful()) {
            //返回ResponseBody
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                try {
                    //获取bodyString
                    BufferedSource source = responseBody.source();
                    source.request(Long.MAX_VALUE);
                    Buffer buffer = source.getBuffer();
                    MediaType contentType = responseBody.contentType();
                    byte[] body = buffer.clone().readByteArray();
                    //AES解码
                    String responseData = AES.decryptAsStringByGCM(body);
                    //生成新的ResponseBody
                    ResponseBody newResponseBody = ResponseBody.create(contentType, responseData);
                    //response
                    response = response.newBuilder().body(newResponseBody).build();
                } catch (Exception e) {
                    //如果发生异常直接返回
                    e.printStackTrace();
                    return response;
                }
            }
        }
        return response;
    }
}
