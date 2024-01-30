package code.util

import android.content.Context
import com.tencent.mmkv.MMKV

open class AbstractPreference {
    companion object {
        @JvmStatic
        fun init(context: Context) {
            MMKV.initialize(context)
        }

        @JvmStatic
        protected val preferences: MMKV
            get() = MMKV.mmkvWithID("vest", MMKV.SINGLE_PROCESS_MODE, "34gj54hbh70sj34zm08cg2b34n")

        @JvmStatic
        protected fun putString(key: String, value: String?): Boolean {

            //ObfuscationStub7.inject();
            return preferences.encode(key, value)
        }

        @JvmStatic
        protected fun getString(key: String): String {
            //ObfuscationStub8.inject();
            return preferences.decodeString(key, "") ?: ""
        }

        @JvmStatic
        protected fun getString(key: String, defaultValue: String): String {
            //ObfuscationStub8.inject();
            return preferences.decodeString(key, defaultValue) ?: ""
        }

        @JvmStatic
        protected fun putBoolean(key: String, value: Boolean): Boolean {
            //ObfuscationStub1.inject();
            return preferences.encode(key, value)
        }

        @JvmStatic
        protected fun getBoolean(key: String, defaultValue: Boolean): Boolean {
            //ObfuscationStub2.inject();
            return preferences.decodeBool(key, defaultValue)
        }

        @JvmStatic
        protected fun putLong(key: String, value: Long): Boolean {
            //ObfuscationStub1.inject();
            return preferences.encode(key, value)
        }

        @JvmStatic
        protected fun getLong(key: String, defaultValue: Long): Long {
            //ObfuscationStub2.inject();
            return preferences.decodeLong(key, defaultValue)
        }

        @JvmStatic
        protected fun hasKey(key: String): Boolean {
            //ObfuscationStub0.inject();
            return preferences.containsKey(key)
        }

        @JvmStatic
        protected fun removeKey(key: String): Boolean {
            //ObfuscationStub3.inject();
            preferences.removeValueForKey(key)
            return true
        } /* private */
    }
}