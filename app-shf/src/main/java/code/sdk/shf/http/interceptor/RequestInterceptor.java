package code.sdk.shf.http.interceptor;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import code.util.AES;
import code.util.LogUtil;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;


/**
 * 加密请求数据
 */
public class RequestInterceptor implements Interceptor {

    private static final String TAG = RequestInterceptor.class.getSimpleName();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Charset charset = StandardCharsets.UTF_8;
        String method = request.method().toLowerCase().trim();//请求方式例如：get delete put post

        if (method.equals("post")) {
            //构建新的请求体
            RequestBody requestBody = request.body();
            //判断类型
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(charset);
                /*如果是二进制上传  则不进行加密*/
                if (contentType.type().toLowerCase().equals("multipart")) {
                    return chain.proceed(request);
                }
            }
            // 获取请求的数据
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);
            String requestData = bodyToString(request);
            LogUtil.d(TAG, "intercept request body: %s", requestData);
            byte[] bytes = AES.encryptByGCM(requestData.getBytes(StandardCharsets.UTF_8), AES.MODE256);
            if (null != bytes) {
                MediaType mediaType = MediaType.parse("application/octet-stream");
                RequestBody bodyBuilder = RequestBody.create(mediaType, bytes);
                //构建新的requestBuilder
                Request.Builder newRequestBuilder = request.newBuilder();
                newRequestBuilder.post(bodyBuilder);
                request = newRequestBuilder.build();
            }
        }
        return chain.proceed(request);
    }

    /**
     * post 请求参数获取
     */
    private String bodyToString(final Request request) {
        final Request copy = request.newBuilder().build();
        final Buffer buffer = new Buffer();
        try {
            copy.body().writeTo(buffer);
        } catch (IOException e) {
            return "something error,when show requestBody";
        }
        return buffer.readUtf8();
    }

}

