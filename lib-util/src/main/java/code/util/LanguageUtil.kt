package code.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.LocaleList
import code.util.AppGlobal.getApplication
import java.util.Locale

class LanguageUtil(context: Context) {
    private val SP_NAME = "language_setting"
    private val TAG_LANGUAGE = "language_select"
    private val TAG_COUNTRY = "country_select"
    private val TAG_SYSTEM_LANGUAGE = "system_language"
    private val mSharedPreferences: SharedPreferences

    init {
        mSharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    fun saveLanguage(language: String?, country: String?) {
        val edit = mSharedPreferences.edit()
        edit.putString(TAG_LANGUAGE, language)
        edit.putString(TAG_COUNTRY, country)
        edit.commit()
    }

    private fun getSelectLanguage(): String {
        return mSharedPreferences.getString(TAG_LANGUAGE, "vi") ?: ""
    }

    private fun getSelectCountry(): String {
        return mSharedPreferences.getString(TAG_COUNTRY, "VN") ?: ""
    }

    private fun getSetLanguageLocale(): Locale {
        return Locale(getSelectLanguage(), getSelectCountry())
    }


    companion object {
       private val TAG = LanguageUtil::class.java.simpleName

        @Volatile
        private var instance: LanguageUtil? = null
        fun getInstance(context: Context): LanguageUtil {
            if (instance == null) {
                synchronized(LanguageUtil::class.java) {
                    if (instance == null) {
                        instance = LanguageUtil(context)
                    }
                }
            }
            return instance!!
        }

        fun resetLanguage() {
            val context: Context = getApplication()
            val resources = context.resources
            val dm = resources.displayMetrics
            val config = resources.configuration
            val locale = getInstance(context).getSetLanguageLocale()
            config.locale = locale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //ObfuscationStub7.inject();
                val localeList = LocaleList(locale)
                config.setLocales(localeList)
                context.applicationContext.createConfigurationContext(config)
            }
            //ObfuscationStub8.inject();
            resources.updateConfiguration(config, dm)
        }

        fun attachBaseContext(context: Context): Context {
            val resources = context.resources
            val configuration = resources.configuration
            val locale = getInstance(context).getSetLanguageLocale()
            configuration.setLocale(locale)
            //ObfuscationStub2.inject();
            return context.createConfigurationContext(configuration)
        }
    }
}