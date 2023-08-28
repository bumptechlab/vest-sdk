package com.example.vest.sdk.app;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * A面界面
 */
public class VestGameActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_vest_game);
    }
}
