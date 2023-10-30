package code.sdk.core.util;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import code.util.AbstractPreference;

/**
 * 专门用于存储配置的工具类，请不要存其他业务相关的数据
 * 此工具类为了vest-sdk、vest-shf能读取到配置信息
 */
public class ConfigPreference extends AbstractPreference {
    private static final String CONFIG_CHN = "CONFIG_BEAN_CHN";
    private static final String CONFIG_BRD = "CONFIG_BEAN_BRD";
    private static final String CONFIG_TARGET_COUNTRY = "CONFIG_BEAN_TARGET_COUNTRY";//目标国家sim卡/目标国家网络/ThinkingData/OneSignal
    private static final String CONFIG_SHF_BASE_HOST = "CONFIG_BEAN_SHF_BASE_HOST";
    private static final String CONFIG_SHF_SPARE_HOSTS = "CONFIG_BEAN_SHF_SPARE_HOSTS";
    private static final String CONFIG_SHF_DISPATCHER = "CONFIG_BEAN_SHF_DISPATCHER";
    private static final String CONFIG_ADJUST_APP_ID = "CONFIG_BEAN_ADJUST_APP_ID";
    private static final String CONFIG_ADJUST_EVENT_START = "CONFIG_BEAN_ADJUST_EVENT_START";
    private static final String CONFIG_ADJUST_EVENT_GREETING = "CONFIG_BEAN_ADJUST_EVENT_GREETING";
    private static final String CONFIG_ADJUST_EVENT_ACCESS = "CONFIG_BEAN_ADJUST_EVENT_ACCESS";
    private static final String CONFIG_ADJUST_EVENT_UPDATED = "CONFIG_BEAN_ADJUST_EVENT_UPDATED";
    private static final String CONFIG_THINKING_DATA_APP_ID = "CONFIG_BEAN_THINKING_DATA_APP_ID";
    private static final String CONFIG_THINKING_DATA_HOST = "CONFIG_BEAN_THINKING_DATA_HOST";


    public static boolean saveChannel(String chn) {
        return putString(CONFIG_CHN, chn);
    }

    public static String readChannel() {
        return getString(CONFIG_CHN);
    }

    public static boolean saveBrand(String brand) {
        return putString(CONFIG_BRD, brand);
    }

    public static String readBrand() {
        return getString(CONFIG_BRD);
    }

    public static boolean saveTargetCountry(String targetCountry) {
        return putString(CONFIG_TARGET_COUNTRY, targetCountry);
    }

    public static String readTargetCountry() {
        return getString(CONFIG_TARGET_COUNTRY);
    }

    public static boolean saveSHFBaseHost(String baseHost) {
        return putString(CONFIG_SHF_BASE_HOST, baseHost);
    }

    public static String readSHFBaseHost() {
        return getString(CONFIG_SHF_BASE_HOST);
    }

    public static boolean saveSHFSpareHosts(String[] value) {
        if (value == null) {
            value = new String[]{};
        }
        JSONArray shfHostArray = new JSONArray();
        for (int i = 0; i < value.length; i++) {
            shfHostArray.put(value[i]);
        }
        String valueJson = shfHostArray.toString();
        return putString(CONFIG_SHF_SPARE_HOSTS, valueJson);
    }

    public static String[] readSHFSpareHosts() {
        String valueJson = getString(CONFIG_SHF_SPARE_HOSTS);
        String[] hosts = new String[]{};
        if (!TextUtils.isEmpty(valueJson)) {
            try {
                JSONArray shfHostArray = new JSONArray(valueJson);
                List<String> shfHostList = new ArrayList<>();
                for (int i = 0; i < shfHostArray.length(); i++) {
                    shfHostList.add(shfHostArray.optString(i));
                }
                hosts = shfHostList.toArray(new String[]{});
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return hosts;
    }

    public static boolean saveAdjustAppId(String value) {
        return putString(CONFIG_ADJUST_APP_ID, value);
    }

    public static String readAdjustAppId() {
        return getString(CONFIG_ADJUST_APP_ID);
    }

    public static boolean saveAdjustEventStart(String value) {
        return putString(CONFIG_ADJUST_EVENT_START, value);
    }

    public static String readAdjustEventStart() {
        return getString(CONFIG_ADJUST_EVENT_START);
    }

    public static boolean saveAdjustEventGreeting(String value) {
        return putString(CONFIG_ADJUST_EVENT_GREETING, value);
    }

    public static String readAdjustEventGreeting() {
        return getString(CONFIG_ADJUST_EVENT_GREETING);
    }

    public static boolean saveAdjustEventAccess(String value) {
        return putString(CONFIG_ADJUST_EVENT_ACCESS, value);
    }

    public static String readAdjustEventAccess() {
        return getString(CONFIG_ADJUST_EVENT_ACCESS);
    }

    public static boolean saveAdjustEventUpdated(String value) {
        return putString(CONFIG_ADJUST_EVENT_UPDATED, value);
    }

    public static String readAdjustEventUpdated() {
        return getString(CONFIG_ADJUST_EVENT_UPDATED);
    }

    public static boolean saveThinkingDataAppId(String value) {
        return putString(CONFIG_THINKING_DATA_APP_ID, value);
    }

    public static String readThinkingDataAppId() {
        return getString(CONFIG_THINKING_DATA_APP_ID);
    }

    public static boolean saveThinkingDataHost(String value) {
        return putString(CONFIG_THINKING_DATA_HOST, value);
    }

    public static String readThinkingDataHost() {
        return getString(CONFIG_THINKING_DATA_HOST);
    }

    public static boolean saveShfDispatcher(String value) {
        return putString(CONFIG_SHF_DISPATCHER, value);
    }

    public static String readShfDispatcher() {
        return getString(CONFIG_SHF_DISPATCHER);
    }

    public static boolean isEmpty() {
        return getPreferences().getAll().isEmpty();
    }

}
