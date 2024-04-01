package com.example.vest.sdk.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import book.sdk.core.VestInspectCallback
import book.sdk.core.VestSDK
import book.sdk.firebase.VestFirebase
import java.util.concurrent.TimeUnit

class SplashActivity : Activity() {

    private val TAG = SplashActivity::class.java.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_custom_splash)
        testVestFirebase()
    }

    private fun testVestFirebase() {
        VestFirebase.getInstance().apply {
            /**
             * setup the date of apk build
             * time format: yyyy-MM-dd HH:mm:ss
             */
            setReleaseTime("2023-11-29 10:23:20")

            /**
             * setup duration of silent period for requesting A/B switching starting from the date of apk build
             */
            setInspectDelayTime(0, TimeUnit.DAYS)

            /**
             * keep the name same as value on firebase control
             */
            setFirebaseKey("url")

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

        })
    }

    private fun gotoASide() {
        val intent = Intent(baseContext, ASideActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
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