package com.example.vest.sdk.app

import androidx.multidex.MultiDexApplication
import book.sdk.core.VestReleaseMode
import book.sdk.core.VestSDK

class AppApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        VestSDK.setLoggable(true)
        VestSDK.setReleaseMode(VestReleaseMode.MODE_VEST)
        VestSDK.init(baseContext, "config")
    }
}