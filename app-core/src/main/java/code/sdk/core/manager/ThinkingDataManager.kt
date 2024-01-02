package code.sdk.core.manager

import android.content.Context
import android.text.TextUtils
import cn.thinkingdata.android.ThinkingAnalyticsSDK
import cn.thinkingdata.android.ThinkingAnalyticsSDK.AutoTrackEventType
import code.sdk.core.VestCore
import code.sdk.core.manager.CocosManager.getCocosFrameVersionInt
import code.sdk.core.manager.CocosManager.getUserId
import code.sdk.core.util.ConfigPreference
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.PackageUtil
import code.util.JSONUtil.putJsonValue
import code.util.LogUtil.d
import code.util.LogUtil.w
import org.json.JSONObject
import java.util.Locale

object ThinkingDataManager {
    private val TAG = ThinkingDataManager::class.java.simpleName
    private var mTDSdk: ThinkingAnalyticsSDK? = null

    fun init(context: Context?) {
        val appId = ConfigPreference.readThinkingDataAppId()
        val serverUrl = ConfigPreference.readThinkingDataHost()
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(serverUrl)) {
            w(TAG, "[ThinkingData] init aborted! appId serverUrl empty")
            return
        }
        w(TAG, "[ThinkingData] init, appId = %s, serverUrl = %s", appId, serverUrl)
        //时间校准
        ThinkingAnalyticsSDK.calibrateTimeWithNtp("time.apple.com")
        mTDSdk = ThinkingAnalyticsSDK.sharedInstance(context, appId, serverUrl)
        if (mTDSdk == null) {
            //ObfuscationStub6.inject();
            w(TAG, "[ThinkingData] init failed!")
            return
        }
        initTDEvents()
        // setup #account_id or #distinct_id
        // we need to get deviceid async
        loginAccount()
    }

    fun initTDEvents() {
        // add Adjust session callback params
        // mTDSdk.enableThirdPartySharing(TDThirdPartyShareType.TD_ADJUST);
        // enable auto track
        val eventTypeList: MutableList<AutoTrackEventType> = ArrayList()
        eventTypeList.add(AutoTrackEventType.APP_INSTALL)
        eventTypeList.add(AutoTrackEventType.APP_START)
        eventTypeList.add(AutoTrackEventType.APP_END)
        eventTypeList.add(AutoTrackEventType.APP_CRASH)
        trackInitTrackEvent(eventTypeList)
    }

    private fun trackInitTrackEvent(eventTypeList: List<AutoTrackEventType>) {
        if (mTDSdk == null) {
            w(TAG, "[ThinkingData] not inited!")
            return
        }
        val extraProperties = JSONObject()
        putJsonValue(extraProperties, "region", getTargetCountry())
        putJsonValue(extraProperties, "build_version", PackageUtil.getBuildVersion())
        mTDSdk!!.enableAutoTrack(eventTypeList, extraProperties)
    }

    fun loginAccount() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            w(TAG, "[ThinkingData] not inited!")
            return
        }
        val accountId = getAccountId()
        d(TAG, "[ThinkingData] accountId = $accountId")
        if (!TextUtils.isEmpty(accountId)) {
            d(TAG, "[ThinkingData] login = $accountId")
            mTDSdk!!.login(accountId)
        } else {
            val deviceID = DeviceUtil.getDeviceID()
            d(TAG, "[ThinkingData] identity = $deviceID")
            mTDSdk!!.identify(deviceID)
        }
        d(TAG, "[ThinkingData] distinctId = %s, deviceId = %s", getTDDistinctId(), getTDDeviceId())
    }

    /**
     * logout will clear #account_id
     */
    fun logoutAccount() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            w(TAG, "[ThinkingData] not inited!")
            return
        }
        mTDSdk!!.logout()
    }

    /**
     * upload data immediately
     */
    fun flush() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            w(TAG, "[ThinkingData] not inited!")
            return
        }
        mTDSdk!!.flush()
    }

    fun getAccountId(): String {
        var userID = getUserId()
        if (userID.isNullOrEmpty()) {
            return ""
        }
        //cocos frame version 不存在认为是1.0版本，返回${country}-${userId}的账号形式
        //cocos frame version 是2.0.0及以上，值返回${userId}的账号形式
        val cocosFrameVersion = getCocosFrameVersionInt()
        if (cocosFrameVersion < 200) {
            userID = "${getTargetCountry()}-$userID"
        }
        return userID
    }

    fun getTargetCountry(): String = VestCore.getTargetCountry().uppercase(Locale.getDefault())
    fun getTDDistinctId(): String {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            w(TAG, "[ThinkingData] not inited!")
            return ""
        }
        return mTDSdk!!.distinctId
    }

    fun getTDDeviceId(): String {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            w(TAG, "[ThinkingData] not inited!")
            return ""
        }
        return mTDSdk!!.deviceId
    }

    fun trackEvent(event: String?, properties: JSONObject?) {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            w(TAG, "[ThinkingData] not inited!")
            return
        }
        d(TAG, "[ThinkingData] track event[%s]: %s", event, properties)
        mTDSdk!!.track(event, properties)
    }
}
