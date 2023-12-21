package code.sdk.core.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.UiModeManager;
import android.content.ContentResolver;
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
import java.net.URI;
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

    public static String gsfAndroidId(Context context) {
        String gsfId = "";
        try {
            Uri URI = Uri.parse("content://com.google.android.gsf.gservices");
            String ID_KEY = "android_id";
            String[] params = new String[]{ID_KEY};
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(URI, (String[]) null, (String) null, params, (String) null);
                if (cursor != null) {
                    Cursor c = cursor;
                    if (!c.moveToFirst() || c.getColumnCount() < 2) {
                        return null;
                    }
                    String id = c.getString(1);
                    if (!TextUtils.isEmpty(id) && id.equals("null")) {
                        gsfId = Long.toHexString(Long.parseLong(id));
                    }
                }
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }
        return gsfId;
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
