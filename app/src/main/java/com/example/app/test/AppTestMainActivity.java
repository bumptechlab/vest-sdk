package com.example.app.test;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import code.core.MainActivity;

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
    }

    @Override
    public void onShowOfficialGame() {
        Log.d(TAG, "show official game");
    }

}
