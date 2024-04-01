package book.util

import android.content.Context
import android.content.SharedPreferences


open class AbstractPreference(fileName: String) : IPreference {

    var preferences: SharedPreferences? = null

    init {
        preferences = AppGlobal.application?.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    override fun putString(key: String, value: String?): Boolean {
        if (preferences == null) return false
        return preferences!!.edit().putString(key, value).commit()
    }

    override fun getString(key: String): String? {
        if (preferences == null) return ""
        return getString(key, "")
    }

    override fun getString(key: String, defaultValue: String?): String? {
        if (preferences == null) return defaultValue
        return preferences!!.getString(key, defaultValue)
    }

    override fun putBoolean(key: String, value: Boolean): Boolean {
        if (preferences == null) return false
        return preferences!!.edit().putBoolean(key, value).commit()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (preferences == null) return defaultValue
        return preferences!!.getBoolean(key, defaultValue)
    }

    override fun putLong(key: String, value: Long): Boolean {
        if (preferences == null) return false
        return preferences!!.edit().putLong(key, value).commit()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        if (preferences == null) return defaultValue
        return preferences!!.getLong(key, defaultValue)
    }

    override fun putInt(key: String, value: Int): Boolean {
        if (preferences == null) return false
        return preferences!!.edit().putInt(key, value).commit()
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        if (preferences == null) return defaultValue
        return preferences!!.getInt(key, defaultValue)
    }

    override fun hasKey(key: String): Boolean {
        if (preferences == null) return false
        return preferences!!.contains(key)
    }

    override fun removeKey(key: String): Boolean {
        if (preferences == null) return false
        return preferences!!.edit().remove(key).commit()
    }

}