package com.example.vest.sdk.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import code.sdk.core.VestInspectCallback;
import code.sdk.core.VestSDK;
import code.sdk.shf.VestSHF;

public class AppTestSDKActivity extends Activity {

    private static final String TAG = AppTestSDKActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_custom_splash);
        VestSHF.getInstance().inspect(this, new VestInspectCallback() {
            //这里跳转到A面，A面请自行实现
            @Override
            public void onShowVestGame() {
                Log.d(TAG, "show vest game");
                Intent intent = new Intent(getBaseContext(), VestGameActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                AppTestSDKActivity.this.finish();
            }

            //这里跳转到B面，B面由SDK提供，使用VestSDK.gotoGameActivity()方法跳转
            @Override
            public void onShowOfficialGame(String url) {
                Log.d(TAG, "show official game: " + url);
                VestSDK.gotoGameActivity(getBaseContext(), url);
                AppTestSDKActivity.this.finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VestSDK.getInstance().onDestroy();
    }
}
