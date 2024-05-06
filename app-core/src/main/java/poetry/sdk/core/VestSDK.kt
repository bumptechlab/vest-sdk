package poetry.sdk.core

import android.content.Context
import poetry.sdk.core.util.ConfigPreference
import poetry.sdk.core.util.Tester
import poetry.util.LogUtil

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
            Tester.setLoggable(loggable)
            LogUtil.setDebug(Tester.isLoggable())
        } catch (e: Exception) {
            mLoggable = loggable
        }
    }

    /**
     * set mode of released apk, the mode will determine the config from server
     * MODE_VEST: this mode is set when A-side apk is ready
     * MODE_CHANNEL: this mode is set when B-side apk is ready
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
