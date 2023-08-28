package com.androidx.h5.http;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.androidx.h5.utils.AES;
import com.androidx.h5.utils.OkHttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import code.util.LogUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpRequest {

    private static final String TAG = "HttpRequest";

    private HashMap<String, String> mHeadersMap = new HashMap<>();//请求头参数
    private HashMap<String, String> mQueryMap = new HashMap<>();//请求链接参数
    private HashMap<String, String> mBodyMap = new HashMap<>();//请求表单参数
    private boolean isLoggable = false;
    private byte[] mFormData = null;                   //表单数据
    private String mMethod = "GET";
    private String mHost = "";
    private String mApi = "";

    /**
     * 不允许直接实例化
     */
    private HttpRequest() {

    }

    private void setHost(String host) {
        mHost = host;
    }

    private void setApi(String api) {
        mApi = api;
    }

    private void setMethod(String method) {
        mMethod = method;
    }

    private void appendQuery(String key, String value) {
        mQueryMap.put(key, value);
    }

    private void appendBody(String key, String value) {
        mBodyMap.put(key, value);
    }

    private void appendFormData(byte[] formData) {
        mFormData = formData;
    }

    private void appendHeader(String key, String value) {
        mHeadersMap.put(key, value);
    }

    private void setLoggable(boolean loggable) {
        this.isLoggable = loggable;
    }


    public static class Builder {

        private HttpRequest sInstance;

        public Builder() {
            sInstance = new HttpRequest();
        }

        public Builder setHost(String host) {
            sInstance.setHost(host);
            return this;
        }

        public Builder setApi(String host) {
            sInstance.setApi(host);
            return this;
        }

        public Builder appendQuery(String key, String value) {
            sInstance.appendQuery(key, value);
            return this;
        }

        public Builder appendBody(String key, String value) {
            sInstance.appendBody(key, value);
            return this;
        }

        public Builder appendFormData(byte[] formData) {
            sInstance.appendFormData(formData);
            return this;
        }

        public Builder setMethod(String method) {
            sInstance.setMethod(method);
            return this;
        }

        public Builder setLoggable(boolean loggable) {
            sInstance.setLoggable(loggable);
            return this;
        }

        public HttpRequest build() {
            return sInstance;
        }
    }

    /**
     * 开始请求网络，这是一个同步方法
     *
     * @return
     */
    public String start() {
        Call call = getOkHttpCall();
        String data = null;
        try {
            Response response = call.execute();
            data = parseDataFromResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * 开始请求网络，这是一个异步方法
     *
     * @param httpCallback
     */
    public void startAsync(HttpCallback httpCallback) {
        Call call = getOkHttpCall();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (null != httpCallback) {
                    httpCallback.onFailure(0, e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200) {
                    String data = parseDataFromResponse(response);
                    if (null != httpCallback) {
                        httpCallback.onSuccess(data);
                    }
                } else {
                    if (null != httpCallback) {
                        httpCallback.onFailure(response.code(), response.message());
                    }
                }
            }
        });
    }

    private String parseDataFromResponse(Response response) {
        String data = null;
        try {
            if (response.code() == 200 && response.body() != null) {
                byte[] bytes = response.body().bytes();
                String responseContent = AES.decryptAsStringByGCM(bytes);
                LogUtil.d(TAG, "response=" + responseContent);
                if (!TextUtils.isEmpty(responseContent)) {
                    data = responseContent;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public String getUrl(){
        return buildUrl(mHost, mApi);
    }

    private Call getOkHttpCall() {
        String url = buildUrl(mHost, mApi);
        Log.d(TAG, "url=" + url);
        Request.Builder requestBuilder = new Request.Builder();
        buildHeader(requestBuilder);

        Request request = null;
        if ("POST".equalsIgnoreCase(mMethod)) {
            RequestBody requestBody = buildRequestBody();
            request = requestBuilder.url(url).post(requestBody).build();
        } else {
            request = requestBuilder.url(url).get().build();
        }
        OkHttpClient okHttpClient = OkHttpUtil.getOkHttpClient();
        Call call = okHttpClient.newCall(request);
        return call;
    }

    private String buildUrl(String host, String api) {
        HttpUrl.Builder builder = HttpUrl.parse(host).newBuilder();
        builder.addEncodedPathSegment(api);
        for (Map.Entry<String, String> entrySet : mQueryMap.entrySet()) {
            builder.addEncodedQueryParameter(entrySet.getKey(), entrySet.getValue());
        }
        return builder.build().toString();
    }

    private void buildHeader(Request.Builder builder) {
        for (Map.Entry<String, String> entrySet : mHeadersMap.entrySet()) {
            builder.addHeader(entrySet.getKey(), entrySet.getValue());
        }
    }

    private RequestBody buildRequestBody() {
        RequestBody requestBody = null;
        if (null != mFormData) {
            MediaType mediaType = MediaType.parse("application/octet-stream");
            requestBody = RequestBody.create(mediaType, mFormData);
        }
        return requestBody;
    }

}
