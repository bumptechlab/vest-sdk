package code.sdk.core

import android.content.Context
import code.sdk.core.JumpCenter.toWebViewActivity
import code.sdk.core.util.TestUtil
import code.sdk.core.util.TestUtil.isLoggable
import code.util.LogUtil.d
import code.util.LogUtil.setDebug

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
        d(TAG, "[Vest-SDK] init")
        VestCore.init(context, configAssets,mLoggable)
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
            setDebug(isLoggable())
        } catch (e: Exception) {
            mLoggable = loggable
        }
    }
    @JvmStatic
    fun onCreate() {
        d(TAG, "[Vest-SDK] onCreate")
        VestCore.onCreate()
    }
    @JvmStatic
    fun onResume() {
        d(TAG, "[Vest-SDK] onResume")
        VestCore.onResume()
    }

    @JvmStatic
    fun onPause() {
        d(TAG, "[Vest-SDK] onPause")
        VestCore.onPause()
    }
    @JvmStatic
    fun onDestroy() {
        d(TAG, "[Vest-SDK] onDestroy")
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
        toWebViewActivity(context, url)
    }
}
