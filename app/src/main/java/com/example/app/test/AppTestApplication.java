package com.example.app.test;

import code.core.MainApplication;
import code.sdk.VestSDK;

public class AppTestApplication extends MainApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        //输出sdk日志开关，release模式请关闭
        VestSDK.setLoggable(BuildConfig.DEBUG);
    }

    @Override
    public String getConfigAsset() {
        return "config";
    }
}
