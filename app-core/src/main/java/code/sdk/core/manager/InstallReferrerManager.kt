package code.sdk.core.manager

import android.content.Context
import android.os.RemoteException
import android.text.TextUtils
import code.sdk.core.util.PreferenceUtil
import code.util.AppGlobal
import code.util.LogUtil.d
import code.util.LogUtil.e
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

object InstallReferrerManager {
   private val TAG = InstallReferrerManager::class.java.simpleName
    const val INSTALL_REFERRER_UNKNOWN = "unknown"
    private var sInitStartTime: Long = 0
    fun initInstallReferrer() {
        sInitStartTime = System.currentTimeMillis()
        val context: Context? = AppGlobal.application
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        startConnection(referrerClient)
    }

    private fun startConnection(referrerClient: InstallReferrerClient) {
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        //ObfuscationStub1.inject();
                        d(TAG, "Connection established")
                        onInstallReferrerServiceConnected(referrerClient)
                        referrerClient.endConnection()
                    }

                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        //ObfuscationStub2.inject();
                        d(TAG, "API not available on the current Play Store app")
                        PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN)
                    }

                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        //ObfuscationStub3.inject();
                        d(TAG, "Connection couldn't be established")
                        PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN)
                    }
                }
                val costTime = System.currentTimeMillis() - sInitStartTime
                d(TAG, "init referrer finish: costTime=" + costTime + "ms" +
                        ", responseCode=" + responseCode)
            }

            override fun onInstallReferrerServiceDisconnected() {
                //ObfuscationStub4.inject();

                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                d(TAG, "init referrer connection closed")
            }
        })
    }

    private fun onInstallReferrerServiceConnected(referrerClient: InstallReferrerClient) {
        val response = try {
            referrerClient.installReferrer
        } catch (e: RemoteException) {
            //ObfuscationStub5.inject();
            e(TAG, e)
            PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN)
            return
        }
        val installReferrer = response.installReferrer
        d(TAG, "init install referrer = $installReferrer")
        if (TextUtils.isEmpty(installReferrer)) {
            PreferenceUtil.saveInstallReferrer(INSTALL_REFERRER_UNKNOWN)
        } else {
            PreferenceUtil.saveInstallReferrer(installReferrer)
        }
    }

    fun getInstallReferrer(): String {
            val installReferrer = PreferenceUtil.readInstallReferrer()
            d(TAG, "get install referrer = $installReferrer")
            return installReferrer
    }
}
