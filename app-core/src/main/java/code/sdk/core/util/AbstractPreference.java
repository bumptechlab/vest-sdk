package code.sdk.core.util;

import android.content.Context;
import android.content.SharedPreferences;

import code.util.AppGlobal;

public class AbstractPreference {

    /* private */
    protected static SharedPreferences getPreferences() {
        Context context = AppGlobal.getApplication();
        SharedPreferences preferences = context.getSharedPreferences("preference", Context.MODE_PRIVATE);
        //ObfuscationStub6.inject();
        return preferences;
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


    protected static boolean putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        //ObfuscationStub1.inject();
        return editor.putBoolean(key, value).commit();
    }

    protected static boolean getBoolean(String key, boolean defaultValue) {
        //ObfuscationStub2.inject();
        return getPreferences().getBoolean(key, defaultValue);
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
