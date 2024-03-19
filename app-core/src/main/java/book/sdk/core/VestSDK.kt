package book.sdk.core

import android.content.Context
import android.graphics.Bitmap.Config
import book.sdk.core.util.ConfigPreference
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

    /**
     * set mode of released app, the mode will determine config from server, default is MODE_VEST
     * MODE_VEST: this mode is set when app is published on GooglePlay
     * MODE_CHANNEL: this mode is set when app is published on landing page
     */
    @JvmStatic
    fun setReleaseMode(mode: VestReleaseMode) {
        ConfigPreference.saveReleaseMode(mode.mode)
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
    fun gotoBSide(context: Context, url: String): Boolean {
        return VestCore.toWebViewActivity(context, url)
    }

}
