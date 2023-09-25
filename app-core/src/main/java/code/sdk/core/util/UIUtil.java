package code.sdk.core.util;

import android.os.Handler;
import android.os.Looper;

public class UIUtil {
    public static final String TAG = UIUtil.class.getSimpleName();

    private static volatile Handler sHandler;

    private static void ensureHandler() {
        if (sHandler == null) {
            //ObfuscationStub0.inject();
            synchronized (UIUtil.class) {
                if (sHandler == null) {
                    //ObfuscationStub1.inject();
                    sHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
    }

    public static void runOnUiThread(Runnable runnable) {
        runOnUiThreadDelay(runnable, 0);
    }

    public static void runOnUiThreadDelay(Runnable runnable, long delayMillis) {
        try {
            ensureHandler();
            sHandler.postDelayed(new NoExceptionRunnable(runnable), delayMillis);
        } catch (Exception e) {
            //ObfuscationStub3.inject();
        }
    }

    /*
     * catch all exception in NoExceptionRunnable run
     * */
    private static class NoExceptionRunnable implements Runnable {

        private final Runnable mRunnable;

        public NoExceptionRunnable(Runnable runnable) {
            mRunnable = runnable;
        }

        @Override
        public void run() {
            try {
                mRunnable.run();
            } catch (Exception e) {
                //ObfuscationStub1.inject();
            }
        }
    }
}
