package poetry.sdk.core.util

import poetry.util.AbstractPreference

/**
 * cocos存储专用，key由cocos层指定
 * native层使用PreferenceUtil
 */
object CocosPreferenceUtil: AbstractPreference("pref_vest_cocos") {
    val TAG = CocosPreferenceUtil::class.java.simpleName
    const val KEY_USER_ID = "_int_user_id"
    const val KEY_COMMON_USER_ID = "_int_common_user_id"
    const val KEY_COCOS_FRAME_VERSION = "_int_cocos_frame_version"

    fun removeGameCache(): Boolean {
        if(preferences == null) return false
        return preferences!!.edit().clear().commit()
    }

    fun getAll(): Map<String, *>? {
        if(preferences == null) return null
        return preferences!!.all
    }

}
