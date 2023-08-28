package code.sdk.core;

import android.content.Context;

import code.sdk.core.util.TestUtil;
import code.util.LogUtil;

public class VestSDK {
    private static String TAG = VestSDK.class.getSimpleName();
    public static VestSDK sInstance = null;
    private Context mContext;

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

//    public void inspect(Context context, VestInspectCallback vestInspectCallback) {
//        try {
//            Class<?> vestSHFClz = Class.forName("code.sdk.shf.VestSHF");
//            Object vestSHFInstance = vestSHFClz.newInstance();
//            Method inspectMethod = vestSHFClz.getDeclaredMethod("inspect", Context.class, VestInspectCallback.class);
//            inspectMethod.setAccessible(true);
//            inspectMethod.invoke(vestSHFInstance, context, vestInspectCallback);
//        } catch (Exception e) {
//            LogUtil.e(TAG, e, "Fail on invoking VestSHF.inspect(), please import 'vest-shf' library");
//        }
//    }

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
