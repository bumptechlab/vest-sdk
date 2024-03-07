package book.util

interface IPreference {

    fun putString(key: String, value: String?): Boolean

    fun getString(key: String): String

    fun getString(key: String, defaultValue: String): String

    fun putBoolean(key: String, value: Boolean): Boolean

    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putLong(key: String, value: Long): Boolean

    fun getLong(key: String, defaultValue: Long): Long

    fun putInt(key: String, value: Int): Boolean

    fun getInt(key: String, defaultValue: Int): Int

    fun hasKey(key: String): Boolean

    fun removeKey(key: String): Boolean
}