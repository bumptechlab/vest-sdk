package code.sdk.core.util

import android.text.TextUtils
import code.util.AbstractPreference
import org.json.JSONArray
import org.json.JSONException

/**
 * 专门用于存储配置的工具类，请不要存其他业务相关的数据
 * 此工具类为了vest-sdk、vest-shf能读取到配置信息
 */
object ConfigPreference : AbstractPreference() {
    private const val CONFIG_CHN = "CONFIG_BEAN_CHN"
    private const val CONFIG_BRD = "CONFIG_BEAN_BRD"
    private const val CONFIG_TARGET_COUNTRY = "CONFIG_BEAN_TARGET_COUNTRY" //目标国家sim卡/目标国家网络/ThinkingData/OneSignal
    private const val CONFIG_SHF_BASE_HOST = "CONFIG_BEAN_SHF_BASE_HOST"
    private const val CONFIG_SHF_SPARE_HOSTS = "CONFIG_BEAN_SHF_SPARE_HOSTS"
    private const val CONFIG_SHF_DISPATCHER = "CONFIG_BEAN_SHF_DISPATCHER"
    private const val CONFIG_ADJUST_APP_ID = "CONFIG_BEAN_ADJUST_APP_ID"
    private const val CONFIG_ADJUST_EVENT_START = "CONFIG_BEAN_ADJUST_EVENT_START"
    private const val CONFIG_ADJUST_EVENT_GREETING = "CONFIG_BEAN_ADJUST_EVENT_GREETING"
    private const val CONFIG_ADJUST_EVENT_ACCESS = "CONFIG_BEAN_ADJUST_EVENT_ACCESS"
    private const val CONFIG_ADJUST_EVENT_UPDATED = "CONFIG_BEAN_ADJUST_EVENT_UPDATED"
    private const val CONFIG_THINKING_DATA_APP_ID = "CONFIG_BEAN_THINKING_DATA_APP_ID"
    private const val CONFIG_THINKING_DATA_HOST = "CONFIG_BEAN_THINKING_DATA_HOST"
    fun saveChannel(chn: String?): Boolean {
        return putString(CONFIG_CHN, chn)
    }

    fun readChannel(): String {
        return getString(CONFIG_CHN)
    }

    fun saveBrand(brand: String?): Boolean {
        return putString(CONFIG_BRD, brand)
    }

    fun readBrand(): String {
        return getString(CONFIG_BRD)
    }

    fun saveTargetCountry(targetCountry: String?): Boolean {
        return putString(CONFIG_TARGET_COUNTRY, targetCountry)
    }

    fun readTargetCountry(): String {
        return getString(CONFIG_TARGET_COUNTRY)
    }

    fun saveSHFBaseHost(baseHost: String?): Boolean {
        return putString(CONFIG_SHF_BASE_HOST, baseHost)
    }

    fun readSHFBaseHost(): String {
        return getString(CONFIG_SHF_BASE_HOST)
    }

    fun saveSHFSpareHosts(value: Array<String?>?): Boolean {
        var v = value
        if (v == null) {
            v = arrayOf()
        }
        val shfHostArray = JSONArray()
        for (i in v.indices) {
            shfHostArray.put(v[i])
        }
        val valueJson = shfHostArray.toString()
        return putString(CONFIG_SHF_SPARE_HOSTS, valueJson)
    }

    fun readSHFSpareHosts(): Array<String?> {
        val valueJson = getString(CONFIG_SHF_SPARE_HOSTS)
        var hosts = arrayOf<String?>()
        if (!TextUtils.isEmpty(valueJson)) {
            try {
                val shfHostArray = JSONArray(valueJson)
                val shfHostList= ArrayList<String>()
                for (i in 0 until shfHostArray.length()) {
                    shfHostList.add(shfHostArray.optString(i))
                }
                hosts = shfHostList.toArray(arrayOf<String>())
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return hosts
    }

    fun saveAdjustAppId(value: String?): Boolean {
        return putString(CONFIG_ADJUST_APP_ID, value)
    }

    fun readAdjustAppId(): String {
        return getString(CONFIG_ADJUST_APP_ID)
    }

    fun saveAdjustEventStart(value: String?): Boolean {
        return putString(CONFIG_ADJUST_EVENT_START, value)
    }

    fun readAdjustEventStart(): String {
        return getString(CONFIG_ADJUST_EVENT_START)
    }

    fun saveAdjustEventGreeting(value: String?): Boolean {
        return putString(CONFIG_ADJUST_EVENT_GREETING, value)
    }

    fun readAdjustEventGreeting(): String {
        return getString(CONFIG_ADJUST_EVENT_GREETING)
    }

    fun saveAdjustEventAccess(value: String?): Boolean {
        return putString(CONFIG_ADJUST_EVENT_ACCESS, value)
    }

    fun readAdjustEventAccess(): String {
        return getString(CONFIG_ADJUST_EVENT_ACCESS)
    }

    fun saveAdjustEventUpdated(value: String?): Boolean {
        return putString(CONFIG_ADJUST_EVENT_UPDATED, value)
    }

    fun readAdjustEventUpdated(): String {
        return getString(CONFIG_ADJUST_EVENT_UPDATED)
    }

    fun saveThinkingDataAppId(value: String?): Boolean {
        return putString(CONFIG_THINKING_DATA_APP_ID, value)
    }

    fun readThinkingDataAppId(): String {
        return getString(CONFIG_THINKING_DATA_APP_ID)
    }

    fun saveThinkingDataHost(value: String?): Boolean {
        return putString(CONFIG_THINKING_DATA_HOST, value)
    }

    fun readThinkingDataHost(): String {
        return getString(CONFIG_THINKING_DATA_HOST)
    }

    fun saveShfDispatcher(value: String?): Boolean {
        return putString(CONFIG_SHF_DISPATCHER, value)
    }

    fun readShfDispatcher(): String {
        return getString(CONFIG_SHF_DISPATCHER)
    }

    val isEmpty: Boolean
        get() = preferences.all.isEmpty()
}
