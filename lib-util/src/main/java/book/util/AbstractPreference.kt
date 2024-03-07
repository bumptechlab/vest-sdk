package book.util

import com.tencent.mmkv.MMKV

open class AbstractPreference(fileName: String) : IPreference {

    private val TAG = AbstractPreference::class.java.simpleName
    private var preferences: MMKV? = null

    init {
        val mmkvRoot = MMKV.initialize(AppGlobal.application)
        LogUtil.d(TAG, "init MMKV with file name: $fileName, root dir: $mmkvRoot")
        preferences = MMKV.mmkvWithID(
            fileName,
            MMKV.SINGLE_PROCESS_MODE,
            "34gj54hbh70sj34zm08cg2b34n"
        )
    }

    override fun putString(key: String, value: String?): Boolean {
        return preferences?.encode(key, value) ?: false
    }

    override fun getString(key: String): String {
        return preferences?.decodeString(key, "") ?: ""
    }

    override fun getString(key: String, defaultValue: String): String {
        return preferences?.decodeString(key, defaultValue) ?: ""
    }

    override fun putBoolean(key: String, value: Boolean): Boolean {
        return preferences?.encode(key, value) ?: false
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return preferences?.decodeBool(key, defaultValue) ?: false
    }

    override fun putLong(key: String, value: Long): Boolean {
        return preferences?.encode(key, value) ?: false
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return preferences?.decodeLong(key, defaultValue) ?: 0
    }

    override fun putInt(key: String, value: Int): Boolean {
        return preferences?.encode(key, value) ?: false
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return preferences?.decodeInt(key, defaultValue) ?: 0
    }

    override fun hasKey(key: String): Boolean {
        return preferences?.containsKey(key) ?: false
    }

    override fun removeKey(key: String): Boolean {
        preferences?.removeValueForKey(key)
        return true
    }

}