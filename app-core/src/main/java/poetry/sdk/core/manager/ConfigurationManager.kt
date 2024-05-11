package poetry.sdk.core.manager

import android.content.Context
import android.text.TextUtils
import poetry.sdk.core.domain.Configuration
import poetry.sdk.core.domain.toConfiguration
import poetry.sdk.core.util.ConfigPreference
import poetry.util.AESUtil
import poetry.util.LogUtil

class ConfigurationManager private constructor() {

    companion object {
        private val TAG = ConfigurationManager::class.java.simpleName
        fun init(context: Context, configName: String?) {
            if (TextUtils.isEmpty(configName)) {
                LogUtil.w(TAG, "Config file is empty, init aborted")
                return
            }
            try {
                val assets = context.assets
                val inputStream = assets.open(configName!!)
                val assetsBytes = inputStream.readBytes()
                configByBytes(assetsBytes)
            } catch (e: Exception) {
                LogUtil.w(TAG, "Fail to parse configuration: %s", e.message)
            }
        }

        private fun configByBytes(assetsBytes: ByteArray?) {
            val configurationJson = decryptConfig(assetsBytes)
            LogUtil.d(TAG, "configuration raw: $configurationJson")
            val configuration = configurationJson.toConfiguration()
            println("configuration = ${configuration}")
            if (configuration != null) {
                initConfig(configuration)
            }
        }

        private fun decryptConfig(assetsBytes: ByteArray?): String {
            //提取AES密钥和密文
            val keyBytes = ByteArray(44)
            val bodyBytes = ByteArray(assetsBytes!!.size - keyBytes.size)
            System.arraycopy(assetsBytes, 0, keyBytes, 0, keyBytes.size)
            System.arraycopy(assetsBytes, keyBytes.size, bodyBytes, 0, bodyBytes.size)
            val aesKey = String(keyBytes)
            val decryptBytes = AESUtil.decrypt(bodyBytes, aesKey)
            return String(decryptBytes)
        }

        private fun initConfig(configuration: Configuration) {
            ConfigPreference.apply {
                saveChannel(configuration.channel)
                saveBrand(configuration.brand)
                saveTargetCountry(configuration.country)
                saveSHFBaseHost(configuration.shfBaseHost)
                saveSHFSpareHosts(configuration.shfSpareHosts)
                saveShfDispatcher(configuration.shfDispatcher)
                saveAdjustAppId(configuration.adjustAppId)
                saveAdjustEventStart(configuration.adjustEventStart)
                saveAdjustEventGreeting(configuration.adjustEventGreeting)
                saveAdjustEventAccess(configuration.adjustEventAccess)
                saveAdjustEventUpdated(configuration.adjustEventUpdated)
                saveStringList(
                    configuration.firebaseIRWhiteDeviceList, CONFIG_FIREBASE_WHITE_DEVICE
                )
                saveStringList(configuration.blackDeviceList, CONFIG_BLACK_DEVICE)
                saveInterfaceDispatcher(configuration.interfaceDispatcher)
                saveInterfaceEnc(configuration.interfaceEnc)
                saveInterfaceEncValue(configuration.interfaceEncValue)
                saveInterfaceNonce(configuration.interfaceNonce)
                saveInterfaceNonceValue(configuration.interfaceNonceValue)
            }
        }

    }


}
