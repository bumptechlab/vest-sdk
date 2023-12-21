package com.example.vest.sdk.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import code.sdk.core.VestInspectCallback;
import code.sdk.core.VestSDK;
import code.sdk.shf.VestSHF;

public class AppTestSDKActivity extends Activity {

    private static final String TAG = AppTestSDKActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_custom_splash);

        /**
         * setup the date of apk build
         * don't need to invoke this method if using vest-plugin, vest-plugin will setup release time automatically
         * if not, you need to invoke this method to setup release time
         * this method has the first priority when using both ways.
         * time format: yyyy-MM-dd HH:mm:ss
         */
        VestSHF.getInstance().setReleaseTime("2023-11-29 10:23:20");

        /**
         * setup duration of silent period for requesting A/B switching starting from the date of apk build
         */
        VestSHF.getInstance().setInspectDelayTime(5, TimeUnit.DAYS);
        /**
         * set true to check the remote and local url, this could make effect on A/B switching
         */
        VestSHF.getInstance().setCheckUrl(true);
        /**
         * trying to request A/B switching, depends on setReleaseTime & setInspectDelayTime & backend config
         */
        VestSHF.getInstance().inspect(this, new VestInspectCallback() {
            /**
             * showing A side
             */
            @Override
            public void onShowVestGame(int reason) {
                Log.d(TAG, "show vest game");
                Intent intent = new Intent(getBaseContext(), VestGameActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                AppTestSDKActivity.this.finish();
            }

            /**
             * showing B side
             */
            @Override
            public void onShowOfficialGame(String url) {
                Log.d(TAG, "show official game: " + url);
                VestSDK.gotoGameActivity(getBaseContext(), url);
                AppTestSDKActivity.this.finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        VestSDK.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VestSDK.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VestSDK.onDestroy();
    }
}
