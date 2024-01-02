package com.example.vest.sdk.app

import androidx.multidex.MultiDexApplication
import code.sdk.core.VestSDK

class AppTestApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        VestSDK.setLoggable(true)
        VestSDK.init(baseContext, "config")
    }
}