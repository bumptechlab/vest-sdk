package code.sdk.core;

import android.content.Context;

import code.sdk.core.util.TestUtil;
import code.util.LogUtil;

public class VestSDK {
    private static String TAG = VestSDK.class.getSimpleName();
    public static VestSDK sInstance = null;

    /**
     * init vest-sdk with this method at the main entrance of application
     *
     * @param context
     * @param configAssets
     * @return
     */
    public static VestSDK init(Context context, String configAssets) {
        if (sInstance == null) {
            sInstance = new VestSDK(context, configAssets);
        }
        return sInstance;
    }

    public static VestSDK getInstance() {
        return sInstance;
    }

    private VestSDK(Context context, String configAssets) {
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

    public void onCreate() {
        VestCore.onCreate();
    }

    public void onDestroy() {
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
