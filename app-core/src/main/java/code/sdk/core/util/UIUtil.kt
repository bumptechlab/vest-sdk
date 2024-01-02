package code.sdk.core.util

import android.os.Handler
import android.os.Looper

object UIUtil {
    private val TAG = UIUtil::class.java.simpleName

    private val sHandler by lazy { Handler(Looper.getMainLooper()) }


    fun runOnUiThread(runnable: Runnable) {
        runOnUiThreadDelay(runnable, 0)
    }

    fun runOnUiThreadDelay(runnable: Runnable, delayMillis: Long) {
        try {
            sHandler.postDelayed(NoExceptionRunnable(runnable), delayMillis)
        } catch (e: Exception) {
            //ObfuscationStub3.inject();
        }
    }

    /*
     * catch all exception in NoExceptionRunnable run
     * */
    private class NoExceptionRunnable(private val mRunnable: Runnable) : Runnable {
        override fun run() {
            try {
                mRunnable.run()
            } catch (e: Exception) {
                //ObfuscationStub1.inject();
            }
        }
    }
}
