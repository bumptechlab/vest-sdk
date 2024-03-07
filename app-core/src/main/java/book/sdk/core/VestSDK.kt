package book.sdk.core

import android.content.Context
import book.sdk.core.util.TestUtil
import book.util.LogUtil

object VestSDK {
    private val TAG = VestSDK::class.java.simpleName
    private var mLoggable: Boolean? = null

    /**
     * init vest-sdk with this method at the main entrance of application
     *
     * @param context
     * @param configAssets
     * @return
     */
    @JvmStatic
    fun init(context: Context, configAssets: String?) {
        LogUtil.d(TAG, "[Vest-SDK] init")
        VestCore.init(context, configAssets, mLoggable)
    }

    /**
     * enable printing log or not
     *
     * @param loggable
     */
    @JvmStatic
    fun setLoggable(loggable: Boolean) {
        try {
            TestUtil.setLoggable(loggable)
            LogUtil.setDebug(TestUtil.isLoggable())
        } catch (e: Exception) {
            mLoggable = loggable
        }
    }

    @JvmStatic
    fun onCreate() {
        LogUtil.d(TAG, "[Vest-SDK] onCreate")
        VestCore.onCreate()
    }

    @JvmStatic
    fun onResume() {
        LogUtil.d(TAG, "[Vest-SDK] onResume")
        VestCore.onResume()
    }

    @JvmStatic
    fun onPause() {
        LogUtil.d(TAG, "[Vest-SDK] onPause")
        VestCore.onPause()
    }

    @JvmStatic
    fun onDestroy() {
        LogUtil.d(TAG, "[Vest-SDK] onDestroy")
        VestCore.onDestroy()
    }

    /**
     * method for opening inner WebView with specified url,
     * usually used for launching B side after VestSHF.inspect completed
     *
     * @param context
     * @param url
     */
    @JvmStatic
    fun gotoGameActivity(context: Context, url: String) {
        VestCore.toWebViewActivity(context, url)
    }
}
