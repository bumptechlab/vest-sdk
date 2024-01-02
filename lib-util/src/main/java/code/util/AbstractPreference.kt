package code.util

import android.content.SharedPreferences

open class AbstractPreference : EncryptedPreference() {
    companion object {
        @JvmStatic
        protected val preferences: SharedPreferences
            get() = getSharedPreferences("preference")

        @JvmStatic
        protected fun putString(key: String, value: String?): Boolean {
            val editor = preferences.edit()
            //ObfuscationStub7.inject();
            return editor.putString(key, value).commit()
        }

        @JvmStatic
        protected fun getString(key: String): String {
            //ObfuscationStub8.inject();
            return preferences.getString(key, "") ?: ""
        }

        @JvmStatic
        protected fun getString(key: String, defaultValue: String): String {
            //ObfuscationStub8.inject();
            return preferences.getString(key, defaultValue) ?: ""
        }

        @JvmStatic
        protected fun putBoolean(key: String, value: Boolean): Boolean {
            val editor = preferences.edit()
            //ObfuscationStub1.inject();
            return editor.putBoolean(key, value).commit()
        }

        @JvmStatic
        protected fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            //ObfuscationStub2.inject();
            return preferences.getBoolean(key, defaultValue)
        }

        @JvmStatic
        protected fun putLong(key: String, value: Long): Boolean {
            val editor = preferences.edit()
            //ObfuscationStub1.inject();
            return editor.putLong(key, value).commit()
        }

        @JvmStatic
        protected fun getLong(key: String, defaultValue: Long): Long {
            //ObfuscationStub2.inject();
            return preferences.getLong(key, defaultValue)
        }

        @JvmStatic
        protected fun hasKey(key: String): Boolean {
            //ObfuscationStub0.inject();
            return preferences.contains(key)
        }

        @JvmStatic
        protected fun removeKey(key: String): Boolean {
            //ObfuscationStub3.inject();
            val editor = preferences.edit()
            return editor.remove(key).commit()
        } /* private */
    }
}