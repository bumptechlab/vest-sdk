package code.sdk.core.manager;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.adjust.sdk.Adjust;

import code.util.LogUtil;

public class AdjustLifecycleCallbacks extends SimpleLifecycleCallbacks {

    private static final String TAG = AdjustLifecycleCallbacks.class.getSimpleName();

    @Override
    public void onActivityResumed(Activity activity) {
        LogUtil.d(TAG, "[Adjust] onActivityResumed");
        Adjust.onResume();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        LogUtil.d(TAG, "[Adjust] onActivityPaused");
        Adjust.onPause();
    }

}
