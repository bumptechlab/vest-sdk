package com.example.vest.sdk.app

import androidx.multidex.MultiDexApplication
import poetry.sdk.core.VestReleaseMode
import poetry.sdk.core.VestSDK

class AppApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        VestSDK.setLoggable(BuildConfig.DEBUG)
        VestSDK.setReleaseMode(VestReleaseMode.MODE_VEST)
        VestSDK.init(baseContext, if (BuildConfig.FLAVOR == "vest") "config" else "config-firebase")
    }
}