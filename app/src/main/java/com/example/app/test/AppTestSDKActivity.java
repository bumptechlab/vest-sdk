package com.example.app.test;

import android.app.Activity;
import android.content.Intent;
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
                Intent intent = new Intent(getBaseContext(), VestGameActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onShowOfficialGame() {
                Log.d(TAG, "show official game");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VestSDK.getInstance().onDestroy();
    }
}
