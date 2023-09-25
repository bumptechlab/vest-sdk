package code.sdk.common;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.WindowMetrics;

import code.util.AppGlobal;

public class ScreenUtil {
    public static final String TAG = ScreenUtil.class.getSimpleName();

    public static int[] getScreenSize() {
        Context context = AppGlobal.getApplication();
        int width, height;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //ObfuscationStub0.inject();

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            width = windowMetrics.getBounds().width();
            height = windowMetrics.getBounds().height();
        } else {
            //ObfuscationStub1.inject();

            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            width = dm.widthPixels;
            height = dm.heightPixels;
        }
        //ObfuscationStub2.inject();
        return new int[] {width, height};
    }

    public static int dp2px(float dp) {
        //ObfuscationStub3.inject();
        Context context = AppGlobal.getApplication();
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }
}
