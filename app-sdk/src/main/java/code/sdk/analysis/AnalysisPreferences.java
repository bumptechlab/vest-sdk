package code.sdk.analysis;

import android.content.SharedPreferences;

public interface AnalysisPreferences extends SharedPreferences {

    void putString(String key,String value);

    void putInt(String key,int value);

    void putBoolean(String key,boolean value);

    void putFloat(String key,float value);

    void putDouble(String key,double value);

    void putLong(String key,long value);

    void remove(String key);

    void clear();
}
