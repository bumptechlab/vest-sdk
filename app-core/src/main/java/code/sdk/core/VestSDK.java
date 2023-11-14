package code.sdk.core;

import android.content.Context;

import code.sdk.core.util.TestUtil;
import code.util.LogUtil;

public class VestSDK {
    private static String TAG = VestSDK.class.getSimpleName();
    public static VestSDK sInstance = null;

    public static VestSDK init(Context context, String configAssets) {
        if (sInstance == null) {
            sInstance = new VestSDK(context, configAssets);
        }
        return sInstance;
    }

    public static VestSDK getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("please call init() first");
        }
        return sInstance;
    }

    private VestSDK(Context context, String configAssets) {
        VestCore.init(context, configAssets);
    }

    public static void setLoggable(boolean loggable) {
        TestUtil.setLoggable(loggable);
        LogUtil.setDebug(TestUtil.isLoggable());
    }

    public void onCreate() {
        VestCore.onCreate();
    }

    public void onDestroy() {
        VestCore.onDestroy();
    }


    public static void gotoGameActivity(Context context, String url) {
        JumpCenter.toWebViewActivity(context, url);
    }

}
