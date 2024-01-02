package code.sdk.shf.inspector

import android.text.TextUtils
import code.sdk.core.manager.InstallReferrerManager
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.GoogleAdIdInitializer
import code.sdk.core.util.PreferenceUtil
import code.util.LogUtil.d

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
                d(TAG, "[InitInspector] inspect timeout!")
                break
            }
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                //ObfuscationStub7.inject();
            }
        }
        if (GoogleAdIdInitializer.needUpdateGoogleAdId()) {
            PreferenceUtil.saveGoogleADID(DeviceUtil.getDeviceID())
        }
        if (TextUtils.isEmpty(installReferrer)) {
            d(TAG, "[InitInspector] install referrer empty!")
            return false
        }
        return true
    }
}