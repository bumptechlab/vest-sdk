package code.sdk.core.manager;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.adjust.sdk.OnEventTrackingFailedListener;
import com.adjust.sdk.OnEventTrackingSucceededListener;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import code.sdk.core.BuildConfig;
import code.sdk.core.VestCore;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.core.util.TestUtil;
import code.util.LogUtil;

public class AdjustManager {
    public static final String TAG = AdjustManager.class.getSimpleName();

    public static void init(Application application) {
        String adjustAppID = PreferenceUtil.readAdjustAppID();
        if (TextUtils.isEmpty(adjustAppID)) {
            adjustAppID = ConfigPreference.readAdjustAppId();
            PreferenceUtil.saveAdjustAppID(adjustAppID);
        }
        initAdjustSdk(application, adjustAppID);
        application.registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());
        initParams();
        trackEventStart(null);
    }

    public static void initParams() {
        //内置的数据可以马上初始化
        String deviceID = DeviceUtil.getDeviceID();
        Adjust.addSessionCallbackParameter("aid", deviceID);
        Adjust.addSessionCallbackParameter("app_chn", PackageUtil.getChannel());
        //以下数据需要服务器返回
        Adjust.addSessionCallbackParameter("app_brd", PackageUtil.getChildBrand());
        Adjust.addSessionCallbackParameter("app_country", VestCore.getTargetCountry());
        Adjust.addSessionCallbackParameter("ta_distinct_id", ThinkingDataManager.getTDDistinctId());
        Adjust.addSessionCallbackParameter("ta_account_id", ThinkingDataManager.getAccountId());
        updateCocosFrameVersion();
    }

    public static void updateCocosFrameVersion() {
        Adjust.addSessionCallbackParameter("int_version", CocosManager.getCocosFrameVersion());
    }

    public static void initAdjustSdk(Context context, String appId) {
        if (TextUtils.isEmpty(appId)) {
            LogUtil.w(TAG, "[Adjust] init aborted! appId empty");
            //ObfuscationStub0.inject();
            return;
        }
        String environment = BuildConfig.DEBUG ? AdjustConfig.ENVIRONMENT_SANDBOX : AdjustConfig.ENVIRONMENT_PRODUCTION;
        LogLevel logLevel = TestUtil.isLoggable() ? LogLevel.VERBOSE : LogLevel.INFO;
        LogUtil.d(TAG, "[Adjust] init with config [appId: %s, environment: %s, logLevel: %s]",
                appId, environment, logLevel.name());

        AdjustConfig config = new AdjustConfig(context, appId, environment);
        config.setLogLevel(logLevel);
        config.setOnEventTrackingSucceededListener(new OnEventTrackingSucceededListener() {
            @Override
            public void onFinishedEventTrackingSucceeded(AdjustEventSuccess adjustEventSuccess) {
            }
        });
        config.setOnEventTrackingFailedListener(new OnEventTrackingFailedListener() {
            @Override
            public void onFinishedEventTrackingFailed(AdjustEventFailure adjustEventFailure) {

            }
        });
        config.setOnAttributionChangedListener(new OnAttributionChangedListener() {
            @Override
            public void onAttributionChanged(AdjustAttribution adjustAttribution) {
            }
        });
        config.setSendInBackground(true);

        //ObfuscationStub1.inject();
        Adjust.onCreate(config);
    }

    public static void trackEvent(String eventToken, Map<String, String> s2sParams) {
        if (TextUtils.isEmpty(eventToken)) {
            //ObfuscationStub2.inject();
            return;
        }

        AdjustEvent adjustEvent = new AdjustEvent(eventToken);
        if (s2sParams != null && !s2sParams.isEmpty()) {
            Set<Map.Entry<String, String>> entries = s2sParams.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                adjustEvent.addCallbackParameter(entry.getKey(), entry.getValue());
            }
        }

        LogUtil.d(TAG, "[Adjust] trackEvent: %s, params: %s", eventToken,
                s2sParams == null ? "" : s2sParams.toString());
        Adjust.trackEvent(adjustEvent);
    }

    public static void trackEventStart(String eventToken) {
        if (TextUtils.isEmpty(eventToken)) {
            eventToken = ConfigPreference.readAdjustEventStart();
        }

        boolean record = PreferenceUtil.readAdjustEventRecordStart(eventToken);
        if (record) {
            //ObfuscationStub4.inject();
            return;
        }

        trackEvent(eventToken, null);
        PreferenceUtil.saveAdjustEventRecordStart(eventToken);
    }

    public static void trackEventGreeting(String eventToken) {
        if (TextUtils.isEmpty(eventToken)) {
            eventToken = ConfigPreference.readAdjustEventGreeting();
        }

        boolean record = PreferenceUtil.readAdjustEventRecordGreeting(eventToken);
        if (record) {
            //ObfuscationStub5.inject();
            return;
        }

        trackEvent(eventToken, null);
        PreferenceUtil.saveAdjustEventRecordGreeting(eventToken);
    }

    public static void trackEventAccess(String eventToken) {
        if (TextUtils.isEmpty(eventToken)) {
            eventToken = ConfigPreference.readAdjustEventAccess();
        }

        boolean record = PreferenceUtil.readAdjustEventRecordAccess(eventToken);
        if (record) {
            //ObfuscationStub6.inject();
            return;
        }

        trackEvent(eventToken, null);
        PreferenceUtil.saveAdjustEventRecordAccess(eventToken);
    }

    public static void trackEventUpdated(String eventToken) {
        if (TextUtils.isEmpty(eventToken)) {
            eventToken = ConfigPreference.readAdjustEventUpdated();
        }

        boolean record = PreferenceUtil.readAdjustEventRecordUpdated(eventToken);
        if (record) {
            //ObfuscationStub7.inject();
            return;
        }

        trackEvent(eventToken, null);
        PreferenceUtil.saveAdjustEventRecordUpdated(eventToken);
    }

    public static String getAdjustDeviceID() {
        String adjustDeviceID = PreferenceUtil.readAdjustDeviceID();
        if (!TextUtils.isEmpty(adjustDeviceID)) {
            //ObfuscationStub8.inject();
            return adjustDeviceID;
        }

        adjustDeviceID = Adjust.getAdid();
        if (!TextUtils.isEmpty(adjustDeviceID)) {
            //ObfuscationStub0.inject();
            PreferenceUtil.saveAdjustDeviceID(adjustDeviceID);
            return adjustDeviceID;
        }
        return "";
    }

}
