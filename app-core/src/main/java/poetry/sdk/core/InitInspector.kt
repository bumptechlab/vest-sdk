package poetry.sdk.core

import android.text.TextUtils
import poetry.sdk.core.manager.InstallReferrerManager
import poetry.sdk.core.util.DeviceUtil
import poetry.sdk.core.util.GoogleAdIdInitializer
import poetry.sdk.core.util.PreferenceUtil
import poetry.util.LogUtil

/**
 * check install referrer & googleAdId
 */
class InitInspector {
    private val TAG = InitInspector::class.java.simpleName
    private val TIMEOUT: Long = 3000
    fun inspect(): Boolean {
        var installReferrer = InstallReferrerManager.getInstallReferrer()
        val startTime = System.currentTimeMillis()
        while (TextUtils.isEmpty(installReferrer) || GoogleAdIdInitializer.needUpdateGoogleAdId()) {
            installReferrer = InstallReferrerManager.getInstallReferrer()
            GoogleAdIdInitializer.init()
            if (System.currentTimeMillis() - startTime > TIMEOUT) {
                LogUtil.d(TAG, "[InitInspector] inspect timeout!")
                break
            }
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
            }
        }
        if (GoogleAdIdInitializer.needUpdateGoogleAdId()) {
            PreferenceUtil.saveGoogleADID(DeviceUtil.getDeviceID())
        }

        LogUtil.d(TAG, "[InitInspector] GSF Id: ${DeviceUtil.gsfAndroidId}")
        LogUtil.d(TAG, "[InitInspector] Google Ad Id: ${DeviceUtil.googleAdId}")
        if (TextUtils.isEmpty(installReferrer)) {
            LogUtil.d(TAG, "[InitInspector] install referrer empty!")
            return false
        }
        return true
    }
}