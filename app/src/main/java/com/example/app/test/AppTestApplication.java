package com.example.app.test;

import androidx.multidex.MultiDexApplication;

import code.sdk.core.VestSDK;

import com.example.app.test.BuildConfig;

public class AppTestApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        VestSDK.init(getBaseContext(), "config-test");
        VestSDK.setLoggable(BuildConfig.DEBUG);
    }

}
