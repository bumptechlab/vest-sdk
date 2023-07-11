package com.example.app.test;

import code.core.MainApplication;
import code.sdk.VestSDK;

public class AppTestApplication extends MainApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        VestSDK.setLoggable(BuildConfig.DEBUG);
    }

    @Override
    public String getConfigAsset() {
        return "config-test";
    }
}
