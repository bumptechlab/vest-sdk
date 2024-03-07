package book.sdk.core.util

import com.tencent.mmkv.MMKV

/**
 * cocos存储专用，key由cocos层指定
 * native层使用PreferenceUtil
 */
object CocosPreferenceUtil {
    private val TAG = CocosPreferenceUtil::class.java.simpleName
    const val KEY_USER_ID = "_int_user_id"
    const val KEY_COMMON_USER_ID = "_int_common_user_id"
    const val KEY_COCOS_FRAME_VERSION = "_int_cocos_frame_version"

    /* public */
    fun putString(key: String?, value: String?): Boolean {
        return preferences.encode(key, value)
    }

    fun getString(key: String?): String? {
        return preferences.decodeString(key, "")
    }

    fun removeGameCache(): Boolean {
        preferences.clearAll()
        return true
    }

    fun getAll(): Map<String, *> {
        val allKeys = preferences.allKeys()
        val map = hashMapOf<String, String?>()
        allKeys?.forEach {
            map[it] = getString(it)
        }
        return map
    }

    private val preferences: MMKV
        get() {
            return MMKV.mmkvWithID("cocos")
        }
}
