package book.sdk.core.util

import book.sdk.core.VestCore
import book.util.AbstractPreference
import book.util.AssetsUtil
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

object PreferenceUtil : AbstractPreference("pref_vest") {
    private val TAG = PreferenceUtil::class.java.simpleName
    private const val KEY_LOGGABLE = "key_loggable"
    private const val KEY_SWITCHER = "key_switcher"
    private const val KEY_GAME_URL = "key_game_url"
    private const val KEY_GAME_URLS = "key_game_urls"
    private const val KEY_DEVICE_ID = "key_device_ID"
    private const val KEY_CHANNEL = "key_channel"
    private const val KEY_PARENT_BRAND = "key_parent_brand"
    private const val KEY_CHILD_BRAND = "key_child_brand"
    private const val KEY_APP_NAME = "key_app_name"
    private const val KEY_ADJUST_DEVICE_ID = "key_adjust_device_ID"
    private const val KEY_GOOGLE_AD_ID = "key_google_AD_ID"
    private const val KEY_ADJUST_APP_ID = "key_adjust_app_ID"
    private const val KEY_ADJUST_EVENT_START = "key_adjust_event_start_"
    private const val KEY_ADJUST_EVENT_GREETING = "key_adjust_event_greeting_"
    private const val KEY_ADJUST_EVENT_ACCESS = "key_adjust_event_access_"
    private const val KEY_ADJUST_EVENT_UPDATED = "key_adjust_event_updated_"
    private const val KEY_INSTALL_REFERRER = "key_install_referrer"
    private const val KEY_SHOW_WEBVIEW_UPDATE_DIALOG = "key_show_webview_update_dialog"
    private const val KEY_TARGET_COUNTRY = "key_target_country"
    private const val KEY_TEST_URL = "key_test_url"
    private const val KEY_DOMAIN_VALID = "key_domain_valid_"
    private const val KEY_INSPECT_DELAY = "key_inspect_delay"
    private const val KEY_BUILD_TIME = "key_build_time"
    private const val KEY_WEB_VIEW_TYPE = "key_web_view_type"

    fun saveSwitcher(switcher: Boolean): Boolean {
        return putBoolean(KEY_SWITCHER, switcher)
    }

    fun readSwitcher(): Boolean {
        return getBoolean(KEY_SWITCHER, false)
    }

    fun saveGameUrl(gameUrl: String): Boolean {
        return putString(KEY_GAME_URL, gameUrl)
    }

    fun readGameUrl(): String {
        return getString(KEY_GAME_URL)
    }

    fun saveGameUrls(gameUrl: String?): List<String> {
        putString(KEY_GAME_URLS, gameUrl)
        return parseJsonArray(gameUrl)
    }

    fun readGameUrls(): List<String> {
        return parseJsonArray(getString(KEY_GAME_URLS))
    }

    private fun parseJsonArray(json: String?): List<String> {
        var jsonList: List<String> = ArrayList()
        if (!json.isNullOrEmpty()) {
            val jsonArray =
                json.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            jsonList = listOf(*jsonArray)
        }
        return jsonList
    }

    fun saveDeviceID(deviceID: String?): Boolean {
        return putString(KEY_DEVICE_ID, deviceID)
    }

    fun readDeviceID(): String {
        return getString(KEY_DEVICE_ID)
    }

    fun saveChannel(channel: String?): Boolean {
        return putString(KEY_CHANNEL, channel)
    }

    fun readChannel(): String {
        return getString(KEY_CHANNEL)
    }

    fun saveParentBrand(brand: String?): Boolean {
        return putString(KEY_PARENT_BRAND, brand)
    }

    fun readParentBrand(): String {
        return getString(KEY_PARENT_BRAND)
    }

    fun saveChildBrand(brand: String?): Boolean {
        return putString(KEY_CHILD_BRAND, brand)
    }

    fun readChildBrand(): String {
        return getString(KEY_CHILD_BRAND)
    }

    fun saveAppName(appName: String?): Boolean {
        return putString(KEY_APP_NAME, appName)
    }

    fun readAppName(): String {
        return getString(KEY_APP_NAME)
    }

    fun saveAdjustDeviceID(adjustDeviceID: String?): Boolean {
        return putString(KEY_ADJUST_DEVICE_ID, adjustDeviceID)
    }

    fun readAdjustDeviceID(): String {
        return getString(KEY_ADJUST_DEVICE_ID)
    }

    fun saveGoogleADID(googleADID: String?): Boolean {
        return putString(KEY_GOOGLE_AD_ID, googleADID)
    }

    fun readGoogleADID(): String {
        return getString(KEY_GOOGLE_AD_ID)
    }

    fun saveAdjustAppID(adjustAppID: String?): Boolean {
        return putString(KEY_ADJUST_APP_ID, adjustAppID)
    }

    fun readAdjustAppID(): String {
        return getString(KEY_ADJUST_APP_ID)
    }

    fun saveAdjustEventRecordStart(eventToken: String?): Boolean {
        return putBoolean(KEY_ADJUST_EVENT_START + eventToken, true)
    }

    fun readAdjustEventRecordStart(eventToken: String?): Boolean {
        return getBoolean(KEY_ADJUST_EVENT_START + eventToken, false)
    }

    fun saveAdjustEventRecordGreeting(eventToken: String?): Boolean {
        return putBoolean(KEY_ADJUST_EVENT_GREETING + eventToken, true)
    }

    fun readAdjustEventRecordGreeting(eventToken: String?): Boolean {
        return getBoolean(KEY_ADJUST_EVENT_GREETING + eventToken, false)
    }

    fun saveAdjustEventRecordAccess(eventToken: String?): Boolean {
        return putBoolean(KEY_ADJUST_EVENT_ACCESS + eventToken, true)
    }

    fun readAdjustEventRecordAccess(eventToken: String?): Boolean {
        return getBoolean(KEY_ADJUST_EVENT_ACCESS + eventToken, false)
    }

    fun saveAdjustEventRecordUpdated(eventToken: String?): Boolean {
        return putBoolean(KEY_ADJUST_EVENT_UPDATED + eventToken, true)
    }

    fun readAdjustEventRecordUpdated(eventToken: String?): Boolean {
        return getBoolean(KEY_ADJUST_EVENT_UPDATED + eventToken, false)
    }

    fun saveInstallReferrer(installReferrer: String?): Boolean {
        return putString(KEY_INSTALL_REFERRER, installReferrer)
    }

    fun readInstallReferrer(): String {
        return getString(KEY_INSTALL_REFERRER)
    }

    fun saveTestUrl(testUrl: String?): Boolean {
        return putString(KEY_TEST_URL, testUrl)
    }

    fun readTestUrl(): String {
        return getString(KEY_TEST_URL)
    }

    fun saveLoggable(loggable: Boolean): Boolean {
        return putBoolean(KEY_LOGGABLE, loggable)
    }

    fun readLoggable(): Boolean {
        return getBoolean(KEY_LOGGABLE, false)
    }

    fun clearLoggable(): Boolean {
        return removeKey(KEY_LOGGABLE)
    }

    fun hasLoggable(): Boolean {
        return hasKey(KEY_LOGGABLE)
    }

    fun readShowWebViewUpdateDialog(): Boolean {
        return getBoolean(KEY_SHOW_WEBVIEW_UPDATE_DIALOG, true)
    }

    fun saveShowWebViewUpdateDialog(show: Boolean): Boolean {
        return putBoolean(KEY_SHOW_WEBVIEW_UPDATE_DIALOG, show)
    }

    fun readTargetCountry(): String {
        return getString(KEY_TARGET_COUNTRY)
    }

    fun saveTargetCountry(targetCountry: String?): Boolean {
        return putString(KEY_TARGET_COUNTRY, targetCountry)
    }

    fun saveDomainValid(domain: String, valid: Boolean): Boolean {
        return putBoolean(KEY_DOMAIN_VALID + domain, valid)
    }

    fun isDomainValid(host: String): Boolean {
        return getBoolean(KEY_DOMAIN_VALID + host, true)
    }

    fun saveInspectDelay(delayTime: Long): Boolean {
        return putLong(KEY_INSPECT_DELAY, delayTime)
    }

    fun getInspectDelay(): Long {
        val defaultDelay = TimeUnit.DAYS.toMillis(5)
        return getLong(KEY_INSPECT_DELAY, defaultDelay)
    }

    fun saveWebViewType(type: String?): Boolean {
        return putString(KEY_WEB_VIEW_TYPE, type)
    }

    fun getWebViewType(): String? {
        return getString(KEY_WEB_VIEW_TYPE, VestCore.WEBVIEW_TYPE_INNER)
    }

    fun saveReleaseTime(delayTime: String?): Boolean {
        val formatter =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault(Locale.Category.FORMAT))
        try {
            val date = formatter.parse(delayTime!!)
            return putLong(KEY_BUILD_TIME, date!!.time)
        } catch (_: Exception) {
        }
        return false
    }

    private fun getReleaseTime(): Long = getLong(KEY_BUILD_TIME, 0)

    /**
     * 获取延时开始时间
     *
     * @return 返回具体时间，如果返回0代表不需要延时
     */
    fun getInspectStartTime(): Long {
        val time = getReleaseTime()
        if (time > 0) return time
        try {
            val buildTIme = AssetsUtil.getAssetsFlagData(AssetsUtil.TIME_FLAG)
            return buildTIme!!.toLong()
        } catch (_: Exception) {
        }
        return 0
    }
}
