package com.androidx.h5.http;

public interface HttpCallback {

    public void onSuccess(String data);

    public void onFailure(int code, String message);
}
