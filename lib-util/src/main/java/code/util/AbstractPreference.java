package code.util;

import android.content.SharedPreferences;

public class AbstractPreference extends EncryptedPreference {

    /* private */
    protected static SharedPreferences getPreferences() {
        return getSharedPreferences("preference");
    }

    protected static boolean putString(String key, String value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        //ObfuscationStub7.inject();
        return editor.putString(key, value).commit();
    }

    protected static String getString(String key) {
        //ObfuscationStub8.inject();
        return getPreferences().getString(key, "");
    }

    protected static String getString(String key, String defaultValue) {
        //ObfuscationStub8.inject();
        return getPreferences().getString(key, defaultValue);
    }


    protected static boolean putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        //ObfuscationStub1.inject();
        return editor.putBoolean(key, value).commit();
    }

    protected static boolean getBoolean(String key, boolean defaultValue) {
        //ObfuscationStub2.inject();
        return getPreferences().getBoolean(key, defaultValue);
    }

    protected static boolean putLong(String key, long value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        //ObfuscationStub1.inject();
        return editor.putLong(key, value).commit();
    }

    protected static long getLong(String key, long defaultValue) {
        //ObfuscationStub2.inject();
        return getPreferences().getLong(key, defaultValue);
    }

    protected static boolean hasKey(String key) {
        //ObfuscationStub0.inject();
        return getPreferences().contains(key);
    }

    protected static boolean removeKey(String key) {
        //ObfuscationStub3.inject();
        SharedPreferences.Editor editor = getPreferences().edit();
        return editor.remove(key).commit();
    }
    /* private */

}
