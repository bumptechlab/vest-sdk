package com.example.vest.sdk.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import book.sdk.core.VestInspectCallback
import book.sdk.core.VestSDK
import book.sdk.shf.VestSHF
import java.util.concurrent.TimeUnit

class SplashActivity : Activity() {

    private val TAG = SplashActivity::class.java.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_custom_splash)

        VestSHF.getInstance().apply {
            /**
             * setup the date of apk build
             * don't need to invoke this method if using vest-plugin, vest-plugin will setup release time automatically
             * if not, you need to invoke this method to setup release time
             * this method has the first priority when using both ways.
             * time format: yyyy-MM-dd HH:mm:ss
             */
            setReleaseTime("2023-11-29 10:23:20")

            /**
             * setup duration of silent period for requesting A/B switching starting from the date of apk build
             */
            setInspectDelayTime(5, TimeUnit.DAYS)

            /**
             * set true to check the remote and local url, this could make effect on A/B switching
             */
            setCheckUrl(true)

            /**
             * trying to request A/B switching, depends on setReleaseTime & setInspectDelayTime & backend config
             */
        }.inspect(this, object : VestInspectCallback {
            /**
             * showing A-side
             */
            override fun onShowASide(reason: Int) {
                Log.d(TAG, "show A-side activity")
                gotoASide()
                finish()
            }

            /**
             * showing B-side
             */
            override fun onShowBSide(url: String, launchResult: Boolean) {
                Log.d(TAG, "show B-side activity: $url, result: $launchResult")
                if (!launchResult) {
                    gotoASide()
                }
                finish()
            }

            private fun gotoASide() {
                val intent = Intent(baseContext, ASideActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        VestSDK.onPause()
    }

    override fun onResume() {
        super.onResume()
        VestSDK.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        VestSDK.onDestroy()
    }

}