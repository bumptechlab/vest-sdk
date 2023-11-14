package code.sdk.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Map;

import code.util.AppGlobal;

/**
 * cocos存储专用，key由cocos层指定
 * native层使用PreferenceUtil
 */
public class CocosPreferenceUtil {
    public static final String TAG = CocosPreferenceUtil.class.getSimpleName();

    public static final String KEY_INT_CHN = "_int_chn";
    public static final String KEY_INT_BRAND_CODE = "_int_brand_code";
    public static final String KEY_USER_ID = "_int_user_id";
    public static final String KEY_COMMON_USER_ID = "_int_common_user_id";
    public static final String KEY_COCOS_FRAME_VERSION = "_int_cocos_frame_version";

    /* public */
    public static boolean putString(String key, String value) {
        //ObfuscationStub0.inject();
        Editor editor = getPreferences().edit();
        return editor.putString(key, value).commit();
    }

    public static String getString(String key) {
        //ObfuscationStub1.inject();
        return getPreferences().getString(key, "");
    }

    public static Map<String, ?> getAll() {
        //ObfuscationStub2.inject();
        return getPreferences().getAll();
    }
    /* public */

    /* private */
    private static SharedPreferences getPreferences() {
        //ObfuscationStub3.inject();
        Context context = AppGlobal.getApplication();
        SharedPreferences preferences = context.getSharedPreferences("cocos", Context.MODE_PRIVATE);
        return preferences;
    }
    /* private */
}
