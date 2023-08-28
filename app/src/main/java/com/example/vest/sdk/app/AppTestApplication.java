package com.example.vest.sdk.app;

import androidx.multidex.MultiDexApplication;

import code.sdk.core.VestSDK;


public class AppTestApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        VestSDK.setLoggable(true);
        VestSDK.init(getBaseContext(), "config");
    }

}
