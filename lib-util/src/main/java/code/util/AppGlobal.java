package code.util;

import android.app.Application;

import java.lang.reflect.InvocationTargetException;

public class AppGlobal {
    public static final String TAG = AppGlobal.class.getSimpleName();
    private static Application sApp;

    public static Application getApplication() {
        if (sApp == null) {
            try {
                sApp = (Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null, (Object[]) null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sApp;
    }
}
