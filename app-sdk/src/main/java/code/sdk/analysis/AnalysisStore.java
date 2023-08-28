package code.sdk.analysis;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import code.sdk.analysis.remote.AnalysisRequest;

public class AnalysisStore implements AnalysisPreferences {

    private static AnalysisStore mStore;
    private final SharedPreferences preferences;
    private static final String KEY = "analysis_key";

    private AnalysisStore(Context applicationContext) {
        preferences = applicationContext.getSharedPreferences("", Context.MODE_PRIVATE);
    }

    public static AnalysisStore instance(Context context) {
        if (null == mStore) mStore = new AnalysisStore(context.getApplicationContext());
        return mStore;
    }


    @Override
    public Map<String, ?> getAll() {
        return preferences.getAll();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return preferences.getString(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return preferences.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return preferences.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        return preferences.contains(key);
    }

    @Override
    public Editor edit() {
        return preferences.edit();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    public void saveData(AnalysisRequest remoteRequest) {
        String data = remoteRequest.toJson();
        putString(KEY + "_" + remoteRequest.getLastLoginTime(), data);
    }

    public Map<String, AnalysisRequest> getData() {
        HashMap<String, AnalysisRequest> hashMap = new HashMap<>();
        Map<String, ?> map = getAll();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().contains(KEY)) {
                String jsonValue = (String) entry.getValue();
                AnalysisRequest analysisRequest = AnalysisRequest.fromJson(jsonValue);
                hashMap.put(entry.getKey(), analysisRequest);
            }
        }
        return hashMap;
    }

    @Override
    public void putString(String key, String value) {
        edit().putString(key, value).apply();
    }

    @Override
    public void putInt(String key, int value) {
        edit().putInt(key, value).apply();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        edit().putBoolean(key, value).apply();
    }

    @Override
    public void putFloat(String key, float value) {
        edit().putFloat(key, value).apply();
    }

    @Override
    public void putDouble(String key, double value) {
        throw new IllegalArgumentException("edit can not put double value to sp ~");
    }

    @Override
    public void putLong(String key, long value) {
        edit().putLong(key, value).apply();
    }

    @Override
    public void remove(String key) {
        edit().remove(key).apply();
    }

    @Override
    public void clear() {
        edit().clear().apply();
    }
}
