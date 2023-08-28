package code.sdk.core.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.UiModeManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.LocaleList;
import android.provider.Settings.Secure;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;

import code.sdk.core.manager.InstallReferrerManager;
import code.util.AppGlobal;
import code.util.LogUtil;

public class DeviceUtil {
    public static final String TAG = DeviceUtil.class.getSimpleName();

    /* public */
    private static final String XIAOMI_VIRTUAL_DEVICEID_NULL = "0000000000000000";


    /**
     * 判断是否有能同步获取到的设备ID（包括sp、file存储）
     *
     * @return Pair <deviceId,isReadFromFile>
     */
    public static Pair<String, Boolean> preGetDeviceID() {
        String deviceID = PreferenceUtil.readDeviceID();

        boolean isReadFromFile = false;
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = readDeviceIDFromFile();
            LogUtil.d(TAG, "getDeviceId: CacheFile: %s", deviceID);
            isReadFromFile = !TextUtils.isEmpty(deviceID);
        }

        //GSF ID
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = getGsfAndroidId();
            LogUtil.d(TAG, "getDeviceId: GoogleServiceFrameworkId: %s", deviceID);
        }

        //Android ID
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = getAndroidID();
            LogUtil.d(TAG, "getDeviceId: AndroidId: %s", deviceID);
            if (XIAOMI_VIRTUAL_DEVICEID_NULL.equals(deviceID)) {
                deviceID = null;
            }
        }

        if (!TextUtils.isEmpty(deviceID)) {
            saveDeviceID(deviceID, isReadFromFile);
        }
        return Pair.create(deviceID, isReadFromFile);
    }

    /**
     * get device id orderly
     * warn:avoid getting the deviceID before Adjust.OnDeviceIdsRead(in MainApplication) callback!
     * <p>
     * 1.GSF ID
     * 2.ANDROID_ID(XIAOMI can shutdown/reset the virtual ANDROID_ID )
     * 3.Adjust ID
     * 4.UUID
     *
     * @return device id
     */
    public static String getDeviceID() {
        Pair<String, Boolean> pair = preGetDeviceID();

        String deviceID = pair.first;
        boolean isReadFromFile = pair.second;

        if (TextUtils.isEmpty(deviceID)) {
            deviceID = getGoogleADID();
            LogUtil.d(TAG, "getDeviceId: GoogleADId: %s", deviceID);
        }
        //UUID
        if (TextUtils.isEmpty(deviceID)) {
            deviceID = UUID.randomUUID().toString();
            LogUtil.d(TAG, "getDeviceId: UUID: %s", deviceID);
        }

        saveDeviceID(deviceID, isReadFromFile);
        LogUtil.d(TAG, "device-id:" + deviceID);
        return deviceID;
    }

    public static String getGoogleADID() {
        //ObfuscationStub2.inject();
        String googleADID = PreferenceUtil.readGoogleADID();
        if (!TextUtils.isEmpty(googleADID)) {
            return googleADID;
        }
        return "";
    }

    private static void saveDeviceID(String deviceID, boolean isReadFromFile) {
        PreferenceUtil.saveDeviceID(deviceID);
        if (!isReadFromFile) {
            saveDeviceIDToFile(deviceID);
        }
    }

    /**
     * get Google Service Framework id
     *
     * @return gsf id
     */
    private static String getGsfAndroidId() {
        try {
            Uri URI = Uri.parse("content://com.google.android.gsf.gservices");
            String ID_KEY = "android_id";
            String params[] = {ID_KEY};
            Cursor c = AppGlobal.getApplication().getContentResolver().query(URI, null, null, params, null);
            if (c == null || !c.moveToFirst() || c.getColumnCount() < 2)
                return null;
            String id = c.getString(1);
            if (TextUtils.isEmpty(id) || "null".equals(id)) return null;
            return Long.toHexString(Long.parseLong(id));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDeviceInfoForLighthouse() {
        TreeMap<String, String> infoMap = buildDeviceInfoMap(AppGlobal.getApplication());
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            if (!TextUtils.isEmpty(entry.getValue())) {
                builder.append("[" + entry.getKey() + ":" + entry.getValue() + "]");
                builder.append(",");
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    private static TreeMap<String, String> buildDeviceInfoMap(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Configuration configuration = context.getResources().getConfiguration();
        Locale appLocale = configuration.locale;
        TimeZone timeZone = TimeZone.getDefault();
        String simCarrierIdName = "N/A";
        int simCarrierId = -1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (!TextUtils.isEmpty(tm.getSimCarrierIdName())) {
                simCarrierIdName = tm.getSimCarrierIdName().toString();
            }
            simCarrierId = tm.getSimCarrierId();
        }
        List<String> simCountryIsoList = getAllSimCountryIso(context);
        String simCountryIso = String.join(",", simCountryIsoList);
        StringBuilder lanListBuilder = new StringBuilder();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            LocaleList localeList = LocaleList.getDefault();
            if (null != localeList) {
                for (int i = 0; i < localeList.size(); i++) {
                    Locale locale = localeList.get(i);
                    lanListBuilder.append(locale.getLanguage() + "-" + locale.getCountry());
                    if (i != localeList.size() - 1) {
                        lanListBuilder.append(",");
                    }
                }
            }
        }

        TreeMap<String, String> infoMap = new TreeMap<>();
        //App信息
        infoMap.put("pkg", PackageUtil.getPackageName());
        infoMap.put("chn", PackageUtil.getChannel());
        infoMap.put("brd", PackageUtil.getBrand());
        infoMap.put("referrer", InstallReferrerManager.getInstallReferrer());

        //SIM卡信息
        infoMap.put("sim_state", getSimState(context));//SIM卡状态
        infoMap.put("sim_country_iso", simCountryIso);//SIM卡国家码（可读取双卡）
        infoMap.put("sim_slot_count", String.valueOf(getSimSlotCount(context)));//SIM卡槽数量
        infoMap.put("sim_operator", tm.getSimOperator());//运营商代码
        infoMap.put("sim_operator_name", tm.getSimOperatorName());//运营商名称
        infoMap.put("sim_carrier_id_name", simCarrierIdName);//运营商ID名称
        infoMap.put("sim_carrier_id", String.valueOf(simCarrierId));//运营商ID

        //设备信息
        infoMap.put("aid", getAndroidID()); //android Id
        infoMap.put("manufacturer", Build.MANUFACTURER);//设备生厂商
        infoMap.put("model", Build.MODEL);//设备型号
        infoMap.put("device", Build.DEVICE);//设备名称
        infoMap.put("sdk_int", String.valueOf(Build.VERSION.SDK_INT));//系统SDK
        infoMap.put("sdk_name", Build.VERSION.RELEASE);//系统SDK名称
        infoMap.put("sdk_code_name", Build.VERSION.CODENAME);//系统SDK发布名称
        infoMap.put("device_type", getDeviceType(context));//设备类型(电视，Watch，手机等)
        infoMap.put("device_resolution", getScreenResolution(context));//设备分辨率
        infoMap.put("is_pad", String.valueOf(isPad(context)));//设备是否为Pad
        infoMap.put("ui_mode", String.valueOf(configuration.uiMode));

        //网络信息
        infoMap.put("network_operator", tm.getNetworkOperator());//网络运营商代码
        infoMap.put("network_operator_name", tm.getNetworkOperatorName());//网络运营商名称
        infoMap.put("phone_type", getPhoneType(context));//网络制式
        infoMap.put("network_country_iso", tm.getNetworkCountryIso());//信号基站国家码
        infoMap.put("data_state", getDataState(context));//移动网络连接状态
        infoMap.put("mnc", String.valueOf(configuration.mnc));
        infoMap.put("mcc", String.valueOf(configuration.mcc));

        //时区
        infoMap.put("tz_id", timeZone.getID());
        infoMap.put("tz_name", timeZone.getDisplayName() + " " + timeZone.getDisplayName(false, TimeZone.SHORT));
        infoMap.put("tz_dst", String.valueOf(timeZone.getDSTSavings()));
        infoMap.put("tz_offset", String.valueOf(timeZone.getRawOffset()));

        //App地区语言（App内部可自行切换）
        infoMap.put("app_country", appLocale.getCountry());
        infoMap.put("app_display_country", appLocale.getDisplayCountry());
        infoMap.put("app_iso3_country", appLocale.getISO3Country());
        infoMap.put("app_language", appLocale.getLanguage());
        infoMap.put("app_display_language", appLocale.getDisplayLanguage());
        infoMap.put("app_iso3_language", appLocale.getISO3Language());
        infoMap.put("app_locate", appLocale.getDisplayName());
        infoMap.put("app_variant", appLocale.getVariant());
        infoMap.put("app_display_variant", appLocale.getDisplayVariant());

        //系统地区语言
        Locale sysLocale = Locale.getDefault();
        infoMap.put("sys_country", sysLocale.getCountry());
        infoMap.put("sys_display_country", sysLocale.getDisplayCountry());
        infoMap.put("sys_language", sysLocale.getLanguage());
        infoMap.put("sys_display_language", sysLocale.getDisplayLanguage());
        infoMap.put("sys_iso3_country", sysLocale.getISO3Country());
        infoMap.put("sys_iso3_language", sysLocale.getISO3Language());
        infoMap.put("sys_language_list", lanListBuilder.toString());

        return infoMap;
    }

    public static String getSimCountryCode(Context context) {
        //ObfuscationStub7.inject();
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String simCountryCode = telManager.getSimCountryIso();
        return TextUtils.isEmpty(simCountryCode) ? "" : simCountryCode;
    }

    public static String getNetworkCountryCode(Context context) {
        //ObfuscationStub8.inject();
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String networkCountryCode = telManager.getNetworkCountryIso();
        return TextUtils.isEmpty(networkCountryCode) ? "" : networkCountryCode;
    }

    public static List<String> getAllSimCountryIso(Context context) {
        List<String> countryIso = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            int slotCount = subManager.getActiveSubscriptionInfoCountMax();
            for (int i = 0; i < slotCount; i++) {
                int subscriptionId = getSubIdBySlotId(context, i);
                String curCountryIso = getSimCountryIsoBySubId(context, subscriptionId);
                //get SimCountryIso by phoneId if can not get by subId
                if (TextUtils.isEmpty(curCountryIso)) {
                    curCountryIso = getSimCountryIsoByPhoneId(context, i);
                }

                if (!TextUtils.isEmpty(curCountryIso)) {
                    countryIso.add(curCountryIso);
                }
            }
        }
        //we can get default country iso at least
        if (countryIso.isEmpty()) {
            countryIso.add(getSimCountryCode(context));
        }
        return countryIso;
    }

    private static int getSubIdBySlotId(Context context, int slotIndex) {
        int subId = -1;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            Object obj = ReflectionUtil.invokeMethod(sm, "android.telephony.SubscriptionManager", "getSubId", new Class[]{int.class}, slotIndex);
            int[] subIds = obj == null ? new int[]{} : (int[]) obj;
            if (subIds.length > 0) {
                subId = subIds[0];
            }
        }
        return subId;
    }

    /**
     * sdk版本<=29用这个方法获取
     *
     * @param context
     * @param subId
     * @return
     */
    public static String getSimCountryIsoBySubId(Context context, int subId) {
        //ObfuscationStub2.inject();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Object simCountryIsoObj = ReflectionUtil.invokeMethod(tm,
                "android.telephony.TelephonyManager", "getSimCountryIso",
                new Class[]{int.class}, subId);
        String simCountryIso = simCountryIsoObj == null ? "" : (String) simCountryIsoObj;
        return simCountryIso;
    }

    /**
     * sdk版本>29用这个方法获取，但是phoneId不确定，可取0-10之间数值
     *
     * @param context
     * @param phoneId
     * @return
     */
    public static String getSimCountryIsoByPhoneId(Context context, int phoneId) {
        //ObfuscationStub3.inject();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Object simCountryIsoObj = ReflectionUtil.invokeMethod(tm,
                "android.telephony.TelephonyManager", "getSimCountryIsoForPhone",
                new Class[]{int.class}, phoneId);
        String simCountryIso = simCountryIsoObj == null ? "" : (String) simCountryIsoObj;
        return simCountryIso;
    }

    public static String getMemory(String type) {
        Context context = AppGlobal.getApplication();
        if (TextUtils.isEmpty(type)) {
            //ObfuscationStub0.inject();
            type = "total";
        }

        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            if (manager == null) {
                //ObfuscationStub1.inject();
                return "0";
            }
            manager.getMemoryInfo(info);
            if (TextUtils.equals(type, "total")) {
                //ObfuscationStub2.inject();
                return DeviceUtil.byteToMBString(info.totalMem);
            } else {
                //ObfuscationStub3.inject();
                return DeviceUtil.byteToMBString(info.availMem);
            }
        } catch (Exception e) {
        }
        return "0";
    }

    public static String byteToMBString(long size) {
        long MB = 1024 * 1024;//定义MB的计算常量
        DecimalFormat df = new DecimalFormat("0.00");//格式化小数
        String resultSize = "";
        if (size / MB >= 1) {
            resultSize = df.format(size / (float) MB);
        }
        return resultSize;
    }

    public static String getLanguage(Context context) {
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        String language = "";
        if (locale != null) {
            language = locale.getLanguage();
        }
        if (TextUtils.isEmpty(language)) {
            language = Locale.getDefault().getLanguage();
        }
        return TextUtils.isEmpty(language) ? "" : language.toLowerCase();
    }

    public static String getDeviceInfoForSHF(Context context) {
        TreeMap<String, String> infoMap = buildDeviceInfoMap(context);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : infoMap.entrySet()) {
            if (!TextUtils.isEmpty(entry.getValue())) {
                builder.append(entry.getKey() + "=" + entry.getValue());
                builder.append("&");
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }


    /**
     * 是否是平板
     *
     * @param context 上下文
     * @return 是平板则返回true，反之返回false
     */
    public static boolean isPad(Context context) {
        //ObfuscationStub0.inject();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
        double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
        double screenInches = Math.sqrt(x + y); // 屏幕尺寸
        return screenInches >= 7.0;
    }

    public static String getScreenResolution(Context context) {
        //ObfuscationStub1.inject();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        return dm.widthPixels + "x" + dm.heightPixels;
    }

    public static String getDeviceType(Context context) {
        //ObfuscationStub2.inject();
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        int uiMode = uiModeManager.getCurrentModeType();
        return Translate.toDeviceType(uiMode);
    }

    public static String getDataState(Context context) {
        //ObfuscationStub3.inject();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int dataState = telephonyManager.getDataState();
        return Translate.toDataState(dataState);
    }

    public static String getSimState(Context context) {
        //ObfuscationStub4.inject();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telephonyManager.getSimState();
        return Translate.toSimState(simState);
    }

    public static int getSimSlotCount(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simSlotCount = 1;
        if (Build.VERSION.SDK_INT < 23) {
        } else if (23 <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < 30) {
            simSlotCount = telephonyManager.getPhoneCount();
        } else if (Build.VERSION.SDK_INT >= 30) {
            simSlotCount = telephonyManager.getActiveModemCount();
        }
        return simSlotCount;
    }

    public static String getPhoneType(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneType = telephonyManager.getPhoneType();
        return Translate.toPhoneType(phoneType);
    }


    public static String getNetworkType(Context context) {
        int networkType = NetworkUtil.getNetworkType(context);
        return Translate.toNetworkType(networkType);
    }

    public static boolean openMarket(Context context, String packageName) {
        boolean success = openGooglePlay(context, packageName);
        if (!success) {
            LogUtil.d(TAG, "Open GooglePlay fail, use build-in market");
            success = openBuildInMarket(context, packageName);
        }
        return success;
    }

    public static void finishActivitySafety(Activity activity) {
        try {
            activity.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean openGooglePlay(Context context, String packageName) {
        boolean success = false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            intent.setPackage("com.android.vending");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                success = true;
            } else {//没有应用市场，通过浏览器跳转到Google Play
                Intent intent2 = new Intent(Intent.ACTION_VIEW);
                intent2.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                if (intent2.resolveActivity(context.getPackageManager()) != null) {
                    intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent2);
                    success = true;
                } else {
                    LogUtil.w(TAG, "Can not find any component to open GooglePlay");
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Open GooglePlay fail", e);
        }
        return success;
    }


    public static boolean openBuildInMarket(Context context, String packageName) {
        boolean success = false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + packageName)); //跳转到应用市场，非Google Play市场一般情况也实现了这个接口
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            success = true;
        } catch (Exception e) {
            LogUtil.e(TAG, "Open BuildIn Market fail", e);
        }
        return success;
    }

    public static PackageInfo getPackageInfo(Context context, String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Exception e) {
            e.printStackTrace();
            //ObfuscationStub6.inject();
        }
        return packageInfo;
    }

    public static String getINetAddress(String host) {
        String hostAddress = "";
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            hostAddress = inetAddress.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hostAddress;
    }

    public static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if (context instanceof ContextWrapper) {
            ContextWrapper wrapper = (ContextWrapper) context;
            return findActivity(wrapper.getBaseContext());
        } else {
            return null;
        }
    }

    public static boolean isDomainAvailable(String host) {
        boolean isAvailable = false;
        try {
            InetAddress ReturnStr = java.net.InetAddress.getByName(host);
            String IPAddress = ReturnStr.getHostAddress();
            isAvailable = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isAvailable;
    }

    /* public */

    /* private */

    private static boolean saveDeviceIDToFile(String deviceId) {
        if (TextUtils.isEmpty(deviceId)) {
            //ObfuscationStub4.inject();
            return false;
        }
        return FileUtil.writeFile(getDeviceIdFile(), deviceId);
    }

    private static String readDeviceIDFromFile() {
        return FileUtil.readFile(getDeviceIdFile());
    }

    private static String getDeviceIdFile() {
        Context context = AppGlobal.getApplication();
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String fileName = "did.dat";
        File file = new File(dir, fileName);
        return file.getAbsolutePath();
    }

    private static String getAndroidID() {
        Context context = AppGlobal.getApplication();
        @SuppressLint("HardwareIds") String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        return androidId;
    }
    /* private */
}
