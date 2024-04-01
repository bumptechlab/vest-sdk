package book.sdk.core.manager

import android.app.Application
import android.content.Context
import android.text.TextUtils
import book.sdk.core.BuildConfig
import book.sdk.core.VestCore
import book.sdk.core.util.ConfigPreference
import book.sdk.core.util.DeviceUtil
import book.sdk.core.util.PackageUtil
import book.sdk.core.util.PreferenceUtil
import book.sdk.core.util.Tester
import book.util.LogUtil
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.LogLevel
import java.util.Locale

object AdjustManager {
   private val TAG = AdjustManager::class.java.simpleName

    
    fun init(application: Application) {
        var adjustAppID = PreferenceUtil.readAdjustAppID()
        if (TextUtils.isEmpty(adjustAppID)) {
            adjustAppID = ConfigPreference.readAdjustAppId()
            PreferenceUtil.saveAdjustAppID(adjustAppID)
        }
        if (TextUtils.isEmpty(adjustAppID)) {
            LogUtil.w(TAG, "[Adjust] init aborted! appId empty")
            return
        }
        initAdjustSdk(application, adjustAppID)
        application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
        initParams()
    }

    
    fun initParams() {
        //内置的数据可以马上初始化
        val deviceID = DeviceUtil.getDeviceID()
        Adjust.addSessionCallbackParameter("aid", deviceID)
        Adjust.addSessionCallbackParameter("app_chn", PackageUtil.getChannel())
        //以下数据需要服务器返回
        Adjust.addSessionCallbackParameter("app_brd", PackageUtil.getChildBrand())
        Adjust.addSessionCallbackParameter("app_country", VestCore.getTargetCountry())
        Adjust.addSessionCallbackParameter("ta_distinct_id", DeviceUtil.getDeviceID())
        Adjust.addSessionCallbackParameter("ta_account_id", getAccountId())
        updateCocosFrameVersion()
    }

    fun getAccountId(): String {
        var userID = CocosManager.getUserId()
        if (userID.isNullOrEmpty()) {
            return ""
        }
        val targetCountry = VestCore.getTargetCountry()?.uppercase(Locale.getDefault())
        //cocos frame version 不存在认为是1.0版本，返回${country}-${userId}的账号形式
        //cocos frame version 是2.0.0及以上，值返回${userId}的账号形式
        val cocosFrameVersion = CocosManager.getCocosFrameVersionInt()
        if (cocosFrameVersion < 200) {
            userID = "${targetCountry}-$userID"
        }
        return userID
    }

    fun updateCocosFrameVersion() {
        Adjust.addSessionCallbackParameter("int_version", CocosManager.getCocosFrameVersion())
    }

    fun initAdjustSdk(context: Context?, appId: String?) {
        if (TextUtils.isEmpty(appId)) {
            LogUtil.w(TAG, "[Adjust] init aborted! appId empty")
            return
        }
        val environment =
            if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
        val logLevel = if (Tester.isLoggable()) LogLevel.VERBOSE else LogLevel.INFO
        LogUtil.d(
            TAG, "[Adjust] init with config [appId: %s, environment: %s, logLevel: %s]",
            appId, environment, logLevel.name
        )
        val config = AdjustConfig(context, appId, environment)
        config.setLogLevel(logLevel)
        config.setOnEventTrackingSucceededListener { }
        config.setOnEventTrackingFailedListener { }
        config.setOnAttributionChangedListener { }
        config.setSendInBackground(true)

        Adjust.onCreate(config)
    }

    fun trackEvent(eventToken: String?, s2sParams: Map<String, String>?) {
        if (TextUtils.isEmpty(eventToken) || "undefined" == eventToken) {
            return
        }
        val adjustEvent = AdjustEvent(eventToken)
        if (!s2sParams.isNullOrEmpty()) {
            val entries = s2sParams.entries
            val iterator = entries.iterator()
            while (iterator.hasNext()) {
                val (key, value) = iterator.next()
                adjustEvent.addCallbackParameter(key, value)
            }
        }
        LogUtil.d(
            TAG, "[Adjust] trackEvent: %s, params: %s", eventToken,
            s2sParams?.toString() ?: ""
        )
        Adjust.trackEvent(adjustEvent)
    }

    /**
     * 记录程序启动
     *
     * @param eventToken
     */
    fun trackEventStart(eventToken: String?) {
        var token = eventToken
        if (TextUtils.isEmpty(token)) {
            token = ConfigPreference.readAdjustEventStart()
        }
        if ("undefined" == token) {
            return
        }
        val record = PreferenceUtil.readAdjustEventRecordStart(token)
        if (record) {
            return
        }
        trackEvent(token, null)
        PreferenceUtil.saveAdjustEventRecordStart(token)
    }

    /**
     * 记录SHF开关获取成功
     *
     * @param eventToken
     */
    fun trackEventGreeting(eventToken: String?) {
        var token = eventToken
        if (TextUtils.isEmpty(token)) {
            token = ConfigPreference.readAdjustEventGreeting()
        }
        if ("undefined" == token) {
            return
        }
        val record = PreferenceUtil.readAdjustEventRecordGreeting(token)
        if (record) {
            return
        }
        trackEvent(token, null)
        PreferenceUtil.saveAdjustEventRecordGreeting(token)
    }

    /**
     * 记录进入到B面游戏
     *
     * @param eventToken
     */
    fun trackEventAccess(eventToken: String?) {
        var token = eventToken
        if (TextUtils.isEmpty(token)) {
            token = ConfigPreference.readAdjustEventAccess()
        }
        if ("undefined" == token) {
            return
        }
        val record = PreferenceUtil.readAdjustEventRecordAccess(token)
        if (record) {
            return
        }
        trackEvent(token, null)
        PreferenceUtil.saveAdjustEventRecordAccess(token)
    }

    /**
     * 记录热更完成事件
     *
     * @param eventToken
     */
    fun trackEventUpdated(eventToken: String?) {
        var token = eventToken
        if (TextUtils.isEmpty(token)) {
            token = ConfigPreference.readAdjustEventUpdated()
        }
        if ("undefined" == token) {
            return
        }
        val record = PreferenceUtil.readAdjustEventRecordUpdated(token)
        if (record) {
            return
        }
        trackEvent(token, null)
        PreferenceUtil.saveAdjustEventRecordUpdated(token)
    }

    fun getAdjustDeviceID(): String? {
        var adjustDeviceID = PreferenceUtil.readAdjustDeviceID()
        if (!TextUtils.isEmpty(adjustDeviceID)) {
            return adjustDeviceID
        }
        adjustDeviceID = Adjust.getAdid()
        if (!TextUtils.isEmpty(adjustDeviceID)) {
            PreferenceUtil.saveAdjustDeviceID(adjustDeviceID)
            return adjustDeviceID
        }
        return ""
    }
}
