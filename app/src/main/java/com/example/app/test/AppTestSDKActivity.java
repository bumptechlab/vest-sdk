package com.example.app.test;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import code.sdk.VestInspectCallback;
import code.sdk.VestSDK;

public class AppTestSDKActivity extends Activity {

    private static final String TAG = AppTestSDKActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_custom_splash);
        VestSDK.getInstance().inspect(this, new VestInspectCallback() {
            @Override
            public void onShowVestGame() {
                Log.d(TAG, "show vest game");
            }

            @Override
            public void onShowOfficialGame() {
                Log.d(TAG, "show official game");
            }
        });
    }

}
