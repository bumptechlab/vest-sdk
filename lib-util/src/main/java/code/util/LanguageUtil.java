package code.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import java.util.Locale;

public class LanguageUtil {
    public static final String TAG = LanguageUtil.class.getSimpleName();

    private final String SP_NAME = "language_setting";
    private final String TAG_LANGUAGE = "language_select";
    private final String TAG_COUNTRY = "country_select";
    private final String TAG_SYSTEM_LANGUAGE = "system_language";
    private static volatile LanguageUtil instance;

    private final SharedPreferences mSharedPreferences;



    public LanguageUtil(Context context) {
        mSharedPreferences = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public void saveLanguage(String language, String country) {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString(TAG_LANGUAGE, language);
        edit.putString(TAG_COUNTRY, country);
        edit.commit();
    }

    private String getSelectLanguage() {
        return mSharedPreferences.getString(TAG_LANGUAGE, "vi");
    }

    private String getSelectCountry() {
        return mSharedPreferences.getString(TAG_COUNTRY, "VN");
    }

    public static LanguageUtil getInstance(Context context) {
        if (instance == null) {
            //ObfuscationStub5.inject();
            synchronized (LanguageUtil.class) {
                if (instance == null) {
                    instance = new LanguageUtil(context);
                }
            }
        }
        return instance;
    }

    private Locale getSetLanguageLocale() {
        //ObfuscationStub6.inject();
        return new Locale(getSelectLanguage(), getSelectCountry());
    }

    public static void resetLanguage() {
        Context context = AppGlobal.getApplication();
        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        Locale locale = LanguageUtil.getInstance(context).getSetLanguageLocale();
        config.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //ObfuscationStub7.inject();
            LocaleList localeList = new LocaleList(locale);
            config.setLocales(localeList);
            context.getApplicationContext().createConfigurationContext(config);
        }
        //ObfuscationStub8.inject();
        resources.updateConfiguration(config, dm);
    }

    public static Context attachBaseContext(Context context) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        Locale locale = LanguageUtil.getInstance(context).getSetLanguageLocale();
        configuration.setLocale(locale);
        //ObfuscationStub2.inject();
        return context.createConfigurationContext(configuration);
    }
}