package com.example.app.test;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import code.core.MainActivity;
import code.sdk.VestSDK;

public class AppTestMainActivity extends MainActivity {

    private static final String TAG = AppTestMainActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * custom your layout here, return 0 to use default
     */
    @Override
    public int getLayoutResource() {
        return R.layout.layout_custom_splash;
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VestSDK.getInstance().onDestroy();
    }

}
