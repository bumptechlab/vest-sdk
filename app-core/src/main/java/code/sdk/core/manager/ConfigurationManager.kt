package code.sdk.core.manager

import android.content.Context
import android.text.TextUtils
import code.sdk.core.Configuration
import code.sdk.core.util.ConfigPreference
import code.util.AESUtil.decrypt
import code.util.IOUtil.toByteArray
import code.util.LogUtil.d
import code.util.LogUtil.w

class ConfigurationManager private constructor() {

    companion object {
        private val TAG = ConfigurationManager::class.java.simpleName

        val mInstance by lazy { ConfigurationManager() }

    }

    fun init(context: Context, configName: String?) {
        if (TextUtils.isEmpty(configName)) {
            w(TAG, "Config file is empty, init aborted")
            return
        }
        try {
            val assets = context.assets
            val inputStream = assets.open(configName!!)
            val assetsBytes = toByteArray(inputStream)
            configByBytes(assetsBytes)
        } catch (e: Exception) {
            //ObfuscationStub4.inject();
            w(TAG, "Fail to parse configuration: %s", e.message)
        }
    }

    private fun configByBytes(assetsBytes: ByteArray?) {
        val configurationJson = decryptConfig(assetsBytes)
        d(TAG, "configuration raw: $configurationJson")
        val configuration = Configuration.fromJson(configurationJson)
        if (configuration != null)
            initConfig(configuration)
    }

    private fun decryptConfig(assetsBytes: ByteArray?): String {
        //提取AES密钥和密文
        val keyBytes = ByteArray(44)
        val bodyBytes = ByteArray(assetsBytes!!.size - keyBytes.size)
        System.arraycopy(assetsBytes, 0, keyBytes, 0, keyBytes.size)
        System.arraycopy(assetsBytes, keyBytes.size, bodyBytes, 0, bodyBytes.size)
        val aesKey = String(keyBytes)
        val decryptBytes = decrypt(bodyBytes, aesKey)
        return String(decryptBytes)
    }

    private fun initConfig(configuration: Configuration) {
        ConfigPreference.saveChannel(configuration.channel)
        ConfigPreference.saveBrand(configuration.brand)
        ConfigPreference.saveTargetCountry(configuration.country)
        ConfigPreference.saveSHFBaseHost(configuration.shfBaseHost)
        ConfigPreference.saveSHFSpareHosts(configuration.shfSpareHosts)
        ConfigPreference.saveShfDispatcher(configuration.shfDispatcher)
        ConfigPreference.saveAdjustAppId(configuration.adjustAppId)
        ConfigPreference.saveAdjustEventStart(configuration.adjustEventStart)
        ConfigPreference.saveAdjustEventGreeting(configuration.adjustEventGreeting)
        ConfigPreference.saveAdjustEventAccess(configuration.adjustEventAccess)
        ConfigPreference.saveAdjustEventUpdated(configuration.adjustEventUpdated)
        ConfigPreference.saveThinkingDataAppId(configuration.thinkingDataAppId)
        ConfigPreference.saveThinkingDataHost(configuration.thinkingDataHost)
    }
}
