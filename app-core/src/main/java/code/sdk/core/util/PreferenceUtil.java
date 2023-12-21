package code.sdk.core.util;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import code.util.AbstractPreference;
import code.util.AssetsUtil;

public class PreferenceUtil extends AbstractPreference {
    public static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String KEY_LOGGABLE = "key_loggable";
    private static final String KEY_SWITCHER = "key_switcher";
    private static final String KEY_GAME_URL = "key_game_url";
    private static final String KEY_GAME_URLS = "key_game_urls";
    private static final String KEY_DEVICE_ID = "key_device_ID";
    private static final String KEY_CHANNEL = "key_channel";
    private static final String KEY_PARENT_BRAND = "key_parent_brand";

    private static final String KEY_CHILD_BRAND = "key_child_brand";
    private static final String KEY_APP_NAME = "key_app_name";
    private static final String KEY_ADJUST_DEVICE_ID = "key_adjust_device_ID";
    private static final String KEY_GOOGLE_AD_ID = "key_google_AD_ID";
    private static final String KEY_ADJUST_APP_ID = "key_adjust_app_ID";
    private static final String KEY_ADJUST_EVENT_START = "key_adjust_event_start_";
    private static final String KEY_ADJUST_EVENT_GREETING = "key_adjust_event_greeting_";
    private static final String KEY_ADJUST_EVENT_ACCESS = "key_adjust_event_access_";
    private static final String KEY_ADJUST_EVENT_UPDATED = "key_adjust_event_updated_";
    private static final String KEY_INSTALL_REFERRER = "key_install_referrer";
    private static final String KEY_SHOW_WEBVIEW_UPDATE_DIALOG = "key_show_webview_update_dialog";
    private static final String KEY_TARGET_COUNTRY = "key_target_country";
    private static final String KEY_TEST_URL = "key_test_url";
    private static final String KEY_DOMAIN_VALID = "key_domain_valid_";
    private static final String KEY_INSPECT_DELAY = "key_inspect_delay";
    private static final String KEY_BUILD_TIME = "key_build_time";

    public static boolean saveSwitcher(boolean switcher) {
        //ObfuscationStub5.inject();
        return putBoolean(KEY_SWITCHER, switcher);
    }

    public static boolean readSwitcher() {
        //ObfuscationStub6.inject();
        return getBoolean(KEY_SWITCHER, false);
    }

    public static boolean saveGameUrl(String gameUrl) {
        //ObfuscationStub7.inject();
        return putString(KEY_GAME_URL, gameUrl);
    }

    public static String readGameUrl() {
        //ObfuscationStub8.inject();
        return getString(KEY_GAME_URL);
    }

    public static List<String> saveGameUrls(String gameUrl) {
        putString(KEY_GAME_URLS, gameUrl);
        //ObfuscationStub7.inject();
        return parseJsonArray(gameUrl);
    }

    public static List<String> readGameUrls() {
        //ObfuscationStub8.inject();
        return parseJsonArray(getString(KEY_GAME_URLS));
    }

    private static List<String> parseJsonArray(String json) {
        List<String> jsonList = new ArrayList<>();
        if (!TextUtils.isEmpty(json)) {
            String[] jsonArray = json.split("\\|");
            jsonList = Arrays.asList(jsonArray);
        }
        return jsonList;
    }

    public static boolean saveDeviceID(String deviceID) {
        return putString(KEY_DEVICE_ID, deviceID);
    }

    public static String readDeviceID() {
        return getString(KEY_DEVICE_ID);
    }

    public static boolean saveChannel(String channel) {
        return putString(KEY_CHANNEL, channel);
    }

    public static String readChannel() {
        return getString(KEY_CHANNEL);
    }

    public static boolean saveParentBrand(String brand) {
        return putString(KEY_PARENT_BRAND, brand);
    }

    public static String readParentBrand() {
        return getString(KEY_PARENT_BRAND);
    }

    public static boolean saveChildBrand(String brand) {
        return putString(KEY_CHILD_BRAND, brand);
    }

    public static String readChildBrand() {
        return getString(KEY_CHILD_BRAND);
    }

    public static boolean saveAppName(String appName) {
        return putString(KEY_APP_NAME, appName);
    }

    public static String readAppName() {
        return getString(KEY_APP_NAME);
    }

    public static boolean saveAdjustDeviceID(String adjustDeviceID) {
        return putString(KEY_ADJUST_DEVICE_ID, adjustDeviceID);
    }

    public static String readAdjustDeviceID() {
        return getString(KEY_ADJUST_DEVICE_ID);
    }

    public static boolean saveGoogleADID(String googleADID) {
        return putString(KEY_GOOGLE_AD_ID, googleADID);
    }

    public static String readGoogleADID() {
        return getString(KEY_GOOGLE_AD_ID);
    }

    public static boolean saveAdjustAppID(String adjustAppID) {
        return putString(KEY_ADJUST_APP_ID, adjustAppID);
    }

    public static String readAdjustAppID() {
        return getString(KEY_ADJUST_APP_ID);
    }

    public static boolean saveAdjustEventRecordStart(String eventToken) {
        return putBoolean(KEY_ADJUST_EVENT_START + eventToken, true);
    }

    public static boolean readAdjustEventRecordStart(String eventToken) {
        return getBoolean(KEY_ADJUST_EVENT_START + eventToken, false);
    }

    public static boolean saveAdjustEventRecordGreeting(String eventToken) {
        return putBoolean(KEY_ADJUST_EVENT_GREETING + eventToken, true);
    }

    public static boolean readAdjustEventRecordGreeting(String eventToken) {
        return getBoolean(KEY_ADJUST_EVENT_GREETING + eventToken, false);
    }

    public static boolean saveAdjustEventRecordAccess(String eventToken) {
        return putBoolean(KEY_ADJUST_EVENT_ACCESS + eventToken, true);
    }

    public static boolean readAdjustEventRecordAccess(String eventToken) {
        return getBoolean(KEY_ADJUST_EVENT_ACCESS + eventToken, false);
    }

    public static boolean saveAdjustEventRecordUpdated(String eventToken) {
        return putBoolean(KEY_ADJUST_EVENT_UPDATED + eventToken, true);
    }

    public static boolean readAdjustEventRecordUpdated(String eventToken) {
        return getBoolean(KEY_ADJUST_EVENT_UPDATED + eventToken, false);
    }

    public static boolean saveInstallReferrer(String installReferrer) {
        return putString(KEY_INSTALL_REFERRER, installReferrer);
    }

    public static String readInstallReferrer() {
        return getString(KEY_INSTALL_REFERRER);
    }

    public static boolean saveTestUrl(String testUrl) {
        return putString(KEY_TEST_URL, testUrl);
    }

    public static String readTestUrl() {
        return getString(KEY_TEST_URL);
    }

    public static boolean saveLoggable(boolean loggable) {
        return putBoolean(KEY_LOGGABLE, loggable);
    }

    public static boolean readLoggable() {
        return getBoolean(KEY_LOGGABLE, false);
    }

    public static boolean clearLoggable() {
        return removeKey(KEY_LOGGABLE);
    }

    public static boolean hasLoggable() {
        return hasKey(KEY_LOGGABLE);
    }

    public static boolean readShowWebViewUpdateDialog() {
        return getBoolean(KEY_SHOW_WEBVIEW_UPDATE_DIALOG, true);
    }

    public static boolean saveShowWebViewUpdateDialog(boolean show) {
        return putBoolean(KEY_SHOW_WEBVIEW_UPDATE_DIALOG, show);
    }

    public static String readTargetCountry() {
        return getString(KEY_TARGET_COUNTRY);
    }

    public static boolean saveTargetCountry(String targetCountry) {
        return putString(KEY_TARGET_COUNTRY, targetCountry);
    }


    public static boolean saveDomainValid(String domain, boolean valid) {
        return putBoolean(KEY_DOMAIN_VALID + domain, valid);
    }

    public static boolean isDomainValid(String host) {
        return getBoolean(KEY_DOMAIN_VALID + host, true);
    }

    public static boolean saveInspectDelay(long delayTime) {
        SharedPreferences.Editor editor = getPreferences().edit();
        //ObfuscationStub1.inject();
        return editor.putLong(KEY_INSPECT_DELAY, delayTime).commit();
    }

    public static long getInspectDelay() {
        long defaultDelay = TimeUnit.DAYS.toMillis(5);
        return getPreferences().getLong(KEY_INSPECT_DELAY, defaultDelay);
    }

    public static boolean saveReleaseTime(String delayTime) {
        SharedPreferences.Editor editor = getPreferences().edit();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = formatter.parse(delayTime);
            return editor.putLong(KEY_BUILD_TIME, date.getTime()).commit();
        } catch (ParseException e) {
        }
        return false;
    }

    public static long getReleaseTime() {
        return getPreferences().getLong(KEY_BUILD_TIME, 0);
    }


    /**
     * 获取延时开始时间
     *
     * @return 返回具体时间，如果返回0代表不需要延时
     */
    public static long getInspectStartTime() {
        long time = getReleaseTime();
        if (time > 0)
            return time;

        try {
            String buildTIme = AssetsUtil.getAssetsFlagData(AssetsUtil.TIME_FLAG);
            return Long.parseLong(buildTIme);
        } catch (Exception e) {
        }
        return 0;
    }

}
