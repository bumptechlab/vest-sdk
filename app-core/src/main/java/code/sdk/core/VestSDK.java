package code.sdk.core;

import android.content.Context;

import code.sdk.core.util.TestUtil;
import code.util.LogUtil;

public class VestSDK {
    private static String TAG = VestSDK.class.getSimpleName();

    /**
     * init vest-sdk with this method at the main entrance of application
     *
     * @param context
     * @param configAssets
     * @return
     */
    public static void init(Context context, String configAssets) {
        LogUtil.d(TAG, "[Vest-SDK] init");
        VestCore.init(context, configAssets);
    }

    /**
     * enable printing log or not
     *
     * @param loggable
     */
    public static void setLoggable(boolean loggable) {
        TestUtil.setLoggable(loggable);
        LogUtil.setDebug(TestUtil.isLoggable());
    }

    public static void onCreate() {
        LogUtil.d(TAG, "[Vest-SDK] onCreate");
        VestCore.onCreate();
    }

    public static void onResume() {
        LogUtil.d(TAG, "[Vest-SDK] onResume");
        VestCore.onResume();
    }

    public static void onPause() {
        LogUtil.d(TAG, "[Vest-SDK] onPause");
        VestCore.onPause();
    }

    public static void onDestroy() {
        LogUtil.d(TAG, "[Vest-SDK] onDestroy");
        VestCore.onDestroy();
    }


    /**
     * method for opening inner WebView with specified url,
     * usually used for launching B side after VestSHF.inspect completed
     *
     * @param context
     * @param url
     */
    public static void gotoGameActivity(Context context, String url) {
        JumpCenter.toWebViewActivity(context, url);
    }

}
