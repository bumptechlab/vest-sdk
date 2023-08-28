package code.sdk.core.util;

public class PreferenceUtil extends AbstractPreference {
    public static final String TAG = PreferenceUtil.class.getSimpleName();

    private static final String KEY_FIREBASE_INSPECTED = "key_firebase_inspected";
    private static final String KEY_LOGGABLE = "key_loggable";
    private static final String KEY_SWI = "key_switcher";
    private static final String KEY_GAME_URL = "key_game_url";
    private static final String KEY_DEVICE_ID = "key_device_ID";
    private static final String KEY_CHANNEL = "key_channel";
    private static final String KEY_BRAND = "key_brand";
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
    private static final String KEY_HTTPDNS_ENABLE = "key_httpdns_enable";
    private static final String KEY_TARGET_COUNTRY = "key_target_country";
    private static final String KEY_TEST_URL = "key_test_url";
    private static final String KEY_DOMAIN_VALID = "key_domain_valid_";

    /* public */
    public static boolean saveSwi(boolean switcher) {
        //ObfuscationStub5.inject();
        return putBoolean(KEY_SWI, switcher);
    }

    public static boolean readSwi() {
        //ObfuscationStub6.inject();
        return getBoolean(KEY_SWI, false);
    }

    public static boolean saveGameUrl(String gameUrl) {
        //ObfuscationStub7.inject();
        return putString(KEY_GAME_URL, gameUrl);
    }

    public static String readGameUrl() {
        //ObfuscationStub8.inject();
        return getString(KEY_GAME_URL);
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

    public static boolean saveBrand(String brand) {
        return putString(KEY_BRAND, brand);
    }

    public static String readBrand() {
        return getString(KEY_BRAND);
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

    public static boolean saveFirebaseInspected(boolean switcher) {
        return putBoolean(KEY_FIREBASE_INSPECTED, switcher);
    }

    public static boolean readFirebaseInspected() {
        return getBoolean(KEY_FIREBASE_INSPECTED, false);
    }

    public static boolean clearFirebaseInspected() {
        return removeKey(KEY_FIREBASE_INSPECTED);
    }

    public static boolean hasFirebaseInspected() {
        return hasKey(KEY_FIREBASE_INSPECTED);
    }


    public static boolean readShowWebViewUpdateDialog() {
        return getBoolean(KEY_SHOW_WEBVIEW_UPDATE_DIALOG, true);
    }

    public static boolean saveShowWebViewUpdateDialog(boolean show) {
        return putBoolean(KEY_SHOW_WEBVIEW_UPDATE_DIALOG, show);
    }

    public static boolean readHttpDnsEnable() {
        return getBoolean(KEY_HTTPDNS_ENABLE, false);
    }

    public static boolean saveHttpDnsEnable(boolean enable) {
        return putBoolean(KEY_HTTPDNS_ENABLE, enable);
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

    /* public */

}