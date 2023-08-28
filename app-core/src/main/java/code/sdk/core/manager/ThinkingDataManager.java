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
import code.util.LogUtil;

public class ThinkingDataManager {
    public static final String TAG = ThinkingDataManager.class.getSimpleName();
    private static ThinkingDataManager sInstance;
    private ThinkingAnalyticsSDK mTDSdk;

    public static ThinkingDataManager init(Context context) {
        if (sInstance == null) {
            sInstance = new ThinkingDataManager(context);
        }
        return sInstance;
    }

    private ThinkingDataManager(Context context) {
        initTDSdk(context);
    }

    public static ThinkingDataManager getInstance() {
        return sInstance;
    }

    private void initTDSdk(Context context) {
        String appId = ConfigPreference.readThinkingDataAppId();
        String serverUrl = ConfigPreference.readThinkingDataHost();
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

    private void initTDEvents() {
        // add Adjust session callback params
        // mTDSdk.enableThirdPartySharing(TDThirdPartyShareType.TD_ADJUST);
        // enable auto track
        List<ThinkingAnalyticsSDK.AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_INSTALL);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_START);
        eventTypeList.add(ThinkingAnalyticsSDK.AutoTrackEventType.APP_END);
        JSONObject extraProperties = new JSONObject();
        try {
            extraProperties.put("region", getTargetCountry());
            extraProperties.put("build_version", PackageUtil.getBuildVersion());
        } catch (Exception e) {
            //ObfuscationStub2.inject();
            LogUtil.e(TAG, e, "ThinkingData Format Error");
        }
        mTDSdk.enableAutoTrack(eventTypeList, extraProperties);
    }

    public void loginAccount() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        String accountId = getAccountId();
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
    public void logoutAccount() {
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
    public void flush() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        mTDSdk.flush();
    }

    public static String getAccountId() {
        String userID = CocosPreferenceUtil.getString(CocosPreferenceUtil.KEY_USER_ID);
        if (TextUtils.isEmpty(userID)) {
            return "";
        }
        return getTargetCountry() + "-" + userID;
    }

    public static String getTargetCountry() {
        return VestCore.getTargetCountry().toUpperCase();
    }

    public String getTDDistinctId() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return "";
        }
        return mTDSdk.getDistinctId();
    }

    public String getTDDeviceId() {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return "";
        }
        return mTDSdk.getDeviceId();
    }

    public void trackEvent(String event, JSONObject properties) {
        if (mTDSdk == null) {
            //ObfuscationStub7.inject();
            LogUtil.w(TAG, "[ThinkingData] not inited!");
            return;
        }
        LogUtil.d(TAG, "[ThinkingData] track event[%s]: %s", event, properties);
        mTDSdk.track(event, properties);
    }

}
