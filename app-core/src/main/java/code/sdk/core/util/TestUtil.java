package code.sdk.core.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import code.sdk.core.BuildConfig;
import code.sdk.core.JumpCenter;
import code.util.AppGlobal;
import code.util.LogUtil;

public class TestUtil {
    public static final String TAG = TestUtil.class.getSimpleName();

    public static boolean isLoggable() {
        //ObfuscationStub7.inject();

        boolean isLoggable = false;
        if (PreferenceUtil.hasLoggable()) {//如果后门设置了日志开关，则使用后门日志开关
            isLoggable = PreferenceUtil.readLoggable();
        } else {
            isLoggable = BuildConfig.DEBUG;
        }
        return isLoggable;
    }

    public static void setLoggable(boolean loggable) {
        PreferenceUtil.saveLoggable(loggable);
    }

    public static boolean isFirebaseInspected() {
        //ObfuscationStub8.inject();

        boolean switcher = false;
        if (PreferenceUtil.hasFirebaseInspected()) {
            switcher = PreferenceUtil.readFirebaseInspected();
        } else {
            switcher = BuildConfig.DEBUG;
        }
        return switcher;
    }

    public static boolean handleIntent(Context context) {
        Activity activity = DeviceUtil.findActivity(context);
        if (activity == null) {
            Log.w(TAG, "[HandleIntent] Activity not found in context");
            return false;
        }
        Intent intent = activity.getIntent();
        if (intent == null) {
            Log.d(TAG, "[HandleIntent] intent is null");
            return false;
        }
        Log.d(TAG, "[HandleIntent] intent=" + intent);
        Bundle bundle = intent.getBundleExtra("bundle");
        printBundleInfo(bundle);
        if (bundle != null) {
            String channel = bundle.getString("game_channel", "").trim();
            String brand = bundle.getString("game_brd", "").trim();
            String installreferrer = bundle.getString("game_installreferrer", "").trim();
            boolean loggable = bundle.getBoolean("log", false);
            String gameUrl = bundle.getString("customizeUrl", "").trim();
            PreferenceUtil.saveChannel(channel);
            PreferenceUtil.saveBrand(brand);
            PreferenceUtil.saveInstallReferrer(installreferrer);
            TestUtil.setLoggable(loggable);
            LogUtil.setDebug(loggable);
            printDebugInfo();
            if (!TextUtils.isEmpty(gameUrl)) {
                DeviceUtil.finishActivitySafety(activity);
                JumpCenter.toWebViewActivity(context, gameUrl);
                return true;
            }
        }
        return false;
    }

    private static void printBundleInfo(Bundle bundle) {
        if (bundle == null) {
            Log.w(TAG, "[HandleIntent] bundle is null from intent");
            return;
        }
        Set<String> keySet = bundle.keySet();
        StringBuilder builder = new StringBuilder();
        Iterator<String> keyIterator = keySet.iterator();
        builder.append("{");
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Object value = bundle.get(key);
            builder.append(key + "=" + value);
            builder.append(", ");
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append("}");
        Log.d(TAG, "[HandleIntent] bundle=" + builder);
    }

    public static void printDebugInfo() {
        LogUtil.i(TAG, "[SDK] name: %s, version: %s, buildNumber: %s",
                BuildConfig.SDK_NAME,
                BuildConfig.SDK_VERSION,
                BuildConfig.BUILD_NUMBER);
        LogUtil.i(TAG, "[App] package: %s, versionCode: %d, versionName: %s, buildVersion:%s, brand: %s, channel: %s",
                PackageUtil.getPackageName(),
                PackageUtil.getPackageVersionCode(),
                PackageUtil.getPackageVersionName(),
                PackageUtil.getBuildVersion(),
                PackageUtil.getBrand(),
                PackageUtil.getChannel());
        LogUtil.i(TAG, "[Device] sdkInt: %d, sdkVersion: %s, isEmulator: %s",
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                EmulatorChecker.isEmulator());
        LogUtil.i(TAG, "[Adjust] APP_ID: %s, EVENT_START: %s, EVENT_GREETING: %s, EVENT_ACCESS: %s, EVENT_UPDATED: %s",
                ConfigPreference.readAdjustAppId(),
                ConfigPreference.readAdjustEventStart(),
                ConfigPreference.readAdjustEventGreeting(),
                ConfigPreference.readAdjustEventAccess(),
                ConfigPreference.readAdjustEventUpdated());
        LogUtil.i(TAG, "[ThinkingData] AppId: %s, Host: %s",
                ConfigPreference.readThinkingDataAppId(),
                ConfigPreference.readThinkingDataHost());
        LogUtil.i(TAG, "[HttpDns] AuthId: %s, AppId: %s, DesKey: %s, Ip: %s",
                ConfigPreference.readHttpDnsAuthId(),
                ConfigPreference.readHttpDnsAppId(),
                ConfigPreference.readHttpDnsDesKey(),
                ConfigPreference.readHttpDnsIp());
        LogUtil.i(TAG, "[Constant] CHN: %s", ConfigPreference.readChannel());
        LogUtil.i(TAG, "[Constant] BRD: %s", ConfigPreference.readBrand());
        LogUtil.i(TAG, "[Constant] SHF_BASE_HOST: %s", ConfigPreference.readSHFBaseHost());
        LogUtil.i(TAG, "[Constant] SHF_SPARE_HOSTS: %s", Arrays.asList(ConfigPreference.readSHFSpareHosts()));
        LogUtil.i(TAG, "[Constant] LIGHTER_HOST: %s", ConfigPreference.readLighterHost());
        LogUtil.i(TAG, "Keystore Hash: %s", String.join(",", PackageUtil.getKeystoreHashes(AppGlobal.getApplication())));
    }

}