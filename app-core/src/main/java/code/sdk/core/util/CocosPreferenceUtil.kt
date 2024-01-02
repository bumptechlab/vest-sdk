package code.sdk.core.util

import android.content.Context
import android.content.SharedPreferences
import code.util.AppGlobal.getApplication

/**
 * cocos存储专用，key由cocos层指定
 * native层使用PreferenceUtil
 */
object CocosPreferenceUtil {
    private val TAG = CocosPreferenceUtil::class.java.simpleName
    const val KEY_INT_CHN = "_int_chn"
    const val KEY_INT_BRAND_CODE = "_int_brand_code"
    const val KEY_USER_ID = "_int_user_id"
    const val KEY_COMMON_USER_ID = "_int_common_user_id"
    const val KEY_COCOS_FRAME_VERSION = "_int_cocos_frame_version"

    /* public */
    fun putString(key: String?, value: String?): Boolean {
        //ObfuscationStub0.inject();
        val editor = preferences.edit()
        return editor.putString(key, value).commit()
    }

    fun getString(key: String?): String? {
        //ObfuscationStub1.inject();
        return preferences.getString(key, "")
    }

    fun getAll(): Map<String, *> = preferences.all
    private val preferences: SharedPreferences
        get() {
            val context: Context = getApplication()
            return context.getSharedPreferences("cocos", Context.MODE_PRIVATE)
        }
}
