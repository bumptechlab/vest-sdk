package code.sdk.core.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import code.util.AppGlobal;

public class PackageUtil {
    public static final String TAG = PackageUtil.class.getSimpleName();

    public static Intent getLaunchIntentForPackage(String packageName) {
        //ObfuscationStub2.inject();
        Context context = AppGlobal.getApplication();
        PackageManager pm = context.getPackageManager();
        return pm.getLaunchIntentForPackage(packageName);
    }

    public static String getPackageName() {
        return AppGlobal.getApplication().getPackageName();
    }

    public static String getPackageVersionName() {
        Context context = AppGlobal.getApplication();
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            //ObfuscationStub3.inject();
        }
        return "";
    }

    public static int getPackageVersionCode() {
        Context context = AppGlobal.getApplication();
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            //ObfuscationStub4.inject();
        }
        return 0;
    }

    public static String getChannel() {
        String channel = PreferenceUtil.readChannel();
        if (!TextUtils.isEmpty(channel)) {
            //ObfuscationStub5.inject();
            return channel;
        }

        channel = ConfigPreference.readChannel();
//        channel = readMetaData("chn");
        PreferenceUtil.saveChannel(channel);
        return channel;
    }

    public static String getBrand() {
        String brand = PreferenceUtil.readBrand();
        if (!TextUtils.isEmpty(brand)) {
            //ObfuscationStub6.inject();
            return brand;
        }
        brand = ConfigPreference.readBrand();
//        brand = readMetaData("brd");
        PreferenceUtil.saveBrand(brand);
        return brand;
    }

    public static String getBuildVersion() {
        String version = PackageUtil.readMetaData("build.version");
        byte[] versionBytes = Base64.decode(version, Base64.DEFAULT);
        return new String(versionBytes);
    }

    public static String readMetaData(String key) {
        Context context = AppGlobal.getApplication();
        PackageManager pm = context.getPackageManager();
        String value = "";
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Object data = applicationInfo.metaData.get(key);
            if (data instanceof String) {
                //ObfuscationStub7.inject();
                value = (String) data;
                //ObfuscationStub8.inject();
            } else if (data instanceof Integer) {
                //ObfuscationStub0.inject();
                value = String.valueOf((Integer) data);
            } else if (data instanceof Float) {
                //ObfuscationStub1.inject();
                value = String.valueOf((Float) data);
            }
        } catch (Exception e) {
            //ObfuscationStub2.inject();
        }
        return value;
    }

    public static String getAppName() {
        String appName = PreferenceUtil.readAppName();
        if (!TextUtils.isEmpty(appName)) {
            //ObfuscationStub3.inject();
            return appName;
        }

        Context context = AppGlobal.getApplication();
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(
                    context.getPackageName(), 0);
            appName = pi.applicationInfo.loadLabel(pm).toString();
            PreferenceUtil.saveAppName(appName);
        } catch (Exception e) {
            //ObfuscationStub4.inject();
        }
        return appName;
    }

    public static List<String> getKeystoreHashes(Context context) {
        List<String> hashList = new ArrayList<>();
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String hash = Base64.encodeToString(md.digest(), Base64.DEFAULT);
                hashList.add(hash);
            }
        } catch (Exception e) {
            //ObfuscationStub5.inject();
        }
        return hashList;
    }
}
