package code.sdk.core.manager;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.thinkingdata.android.ThinkingAnalyticsSDK;
import code.sdk.core.VestCore;
import code.sdk.core.util.CocosPreferenceUtil;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.PackageUtil;
import code.util.JSONUtil;
import code.util.LogUtil;

public class ThinkingDataManager {
    public static final String TAG = ThinkingDataManager.class.getSimpleName();
    private static ThinkingAnalyticsSDK mTDSdk;

    public static void init(Context context) {
        String appId = ConfigPreference.readThinkingDataAppId();
        String serverUrl = ConfigPreference.readThinkingDataHost();
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(serverUrl)){
            LogUtil.w(TAG, "[ThinkingData] init aborted! appId serverUrl empty");
            return;
        }
        LogUtil.w(TAG, "[ThinkingData] init, appId = %s, serverUrl = %s", appId, serverUrl);
        //时间校准
        ThinkingAnalyticsSDK.calibrateTimeWithNtp("time.apple.com");
        mTDSdk = ThinkingAnalyticsSDK.sharedInstance(context, appId, serverUrl);
        if (mTDSdk == null) {
            //ObfuscationStub6.inject();
            LogUtil.w(TAG, "[ThinkingData] init failed!");
            return;
        }
        initTDEvents();
        // setup #account_id or #distinct_id
        // we need to get deviceid async
        loginAccount();
    }

    public static void initTDEvents() {
        // add Adjust session callback params
        // mTDSdk.enableThirdPartySharing(TDThirdPartyShareType.TD_ADJUST);
        // enable auto track
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_CRASH);
        trackInitTrackEvent(eventTypeList);
    }

    private static void trackInitTrackEvent(List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList) {
        if (mTDSdk == null) {
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        JSONObject extraProperties = new JSONObject();
        JSONUtil.putJsonValue(extraProperties, "region", getTargetCountry());
        JSONUtil.putJsonValue(extraProperties, "build_version", PackageUtil.getBuildVersion());
        mTDSdk.enableAutoTrack(eventTypeList, extraProperties);
    }

    public static void loginAccount() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        String accountId = getAccountId();
        LogUtil.d(TAG, "[ThinkingData] accountId = " + accountId);
        if (!TextUtils.isEmpty(accountId)) {
            LogUtil.d(TAG, "[ThinkingData] login = " + accountId);
            mTDSdk.login(accountId);
        } else {
            String deviceID = DeviceUtil.getDeviceID();
            LogUtil.d(TAG, "[ThinkingData] identity = " + deviceID);
            mTDSdk.identify(deviceID);
        }
        LogUtil.d(TAG, "[ThinkingData] distinctId = %s, deviceId = %s", getTDDistinctId(), getTDDeviceId());
    }

    /**
     * logout will clear #account_id
     */
    public static void logoutAccount() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        mTDSdk.logout();
    }

    /**
     * upload data immediately
     */
    public static void flush() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        mTDSdk.flush();
    }

    public static String getAccountId() {
        String userID = CocosManager.getUserId();
        if (TextUtils.isEmpty(userID)) {
            return "";
        }
        //cocos frame version 不存在认为是1.0版本，返回${country}-${userId}的账号形式
        //cocos frame version 是2.0.0及以上，值返回${userId}的账号形式
        int cocosFrameVersion = CocosManager.getCocosFrameVersion();
        if (cocosFrameVersion < 200) {
            userID = getTargetCountry() + "-" + userID;
        }
        return userID;
    }

    public static String getTargetCountry() {
        return VestCore.getTargetCountry().toUpperCase();
    }

    public static String getTDDistinctId() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return "";
        }
        return mTDSdk.getDistinctId();
    }

    public static String getTDDeviceId() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return "";
        }
        return mTDSdk.getDeviceId();
    }

    public static void trackEvent(String event, JSONObject properties) {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        LogUtil.d(TAG, "[ThinkingData] track event[%s]: %s", event, properties);
        mTDSdk.track(event, properties);
    }

}
