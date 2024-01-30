package code.sdk.core.manager

import android.app.Application
import android.content.Context
import android.text.TextUtils
import code.sdk.core.BuildConfig
import code.sdk.core.VestCore
import code.sdk.core.util.ConfigPreference
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.PackageUtil
import code.sdk.core.util.PreferenceUtil
import code.sdk.core.util.TestUtil
import code.util.LogUtil.d
import code.util.LogUtil.w
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.adjust.sdk.LogLevel

object AdjustManager {
   private val TAG = AdjustManager::class.java.simpleName

    
    fun init(application: Application) {
        var adjustAppID = PreferenceUtil.readAdjustAppID()
        if (TextUtils.isEmpty(adjustAppID)) {
            adjustAppID = ConfigPreference.readAdjustAppId()
            PreferenceUtil.saveAdjustAppID(adjustAppID)
        }
        if (TextUtils.isEmpty(adjustAppID)) {
            w(TAG, "[Adjust] init aborted! appId empty")
            //ObfuscationStub0.inject();
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
        Adjust.addSessionCallbackParameter("ta_distinct_id", ThinkingDataManager.getTDDistinctId())
        Adjust.addSessionCallbackParameter("ta_account_id", ThinkingDataManager.getAccountId())
        updateCocosFrameVersion()
    }

    fun updateCocosFrameVersion() {
        Adjust.addSessionCallbackParameter("int_version", CocosManager.getCocosFrameVersion())
    }

    fun initAdjustSdk(context: Context?, appId: String?) {
        if (TextUtils.isEmpty(appId)) {
            w(TAG, "[Adjust] init aborted! appId empty")
            //ObfuscationStub0.inject();
            return
        }
        val environment =
            if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX else AdjustConfig.ENVIRONMENT_PRODUCTION
        val logLevel = if (TestUtil.isLoggable()) LogLevel.VERBOSE else LogLevel.INFO
        d(
            TAG, "[Adjust] init with config [appId: %s, environment: %s, logLevel: %s]",
            appId, environment, logLevel.name
        )
        val config = AdjustConfig(context, appId, environment)
        config.setLogLevel(logLevel)
        config.setOnEventTrackingSucceededListener { }
        config.setOnEventTrackingFailedListener { }
        config.setOnAttributionChangedListener { }
        config.setSendInBackground(true)

        //ObfuscationStub1.inject();
        Adjust.onCreate(config)
    }

    fun trackEvent(eventToken: String?, s2sParams: Map<String, String>?) {
        if (TextUtils.isEmpty(eventToken) || "undefined" == eventToken) {
            //ObfuscationStub2.inject();
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
        d(
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
            //ObfuscationStub4.inject();
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
            //ObfuscationStub5.inject();
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
            //ObfuscationStub6.inject();
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
            //ObfuscationStub7.inject();
            return
        }
        trackEvent(token, null)
        PreferenceUtil.saveAdjustEventRecordUpdated(token)
    }

    fun getAdjustDeviceID(): String {
        var adjustDeviceID = PreferenceUtil.readAdjustDeviceID()
        if (!TextUtils.isEmpty(adjustDeviceID)) {
            //ObfuscationStub8.inject();
            return adjustDeviceID
        }
        adjustDeviceID = Adjust.getAdid()
        if (!TextUtils.isEmpty(adjustDeviceID)) {
            //ObfuscationStub0.inject();
            PreferenceUtil.saveAdjustDeviceID(adjustDeviceID)
            return adjustDeviceID
        }
        return ""
    }
}
