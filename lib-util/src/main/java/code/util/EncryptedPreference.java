package code.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class EncryptedPreference {

    private static final String TAG = EncryptedPreference.class.getSimpleName();

    protected static SharedPreferences getSharedPreferences(String fileName) {
        SharedPreferences sharedPreferences = null;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    fileName,
                    masterKeyAlias,
                    AppGlobal.getApplication(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            LogUtil.e(TAG, e, "Fail to create encrypted shared preference");
            sharedPreferences = AppGlobal.getApplication().getSharedPreferences(fileName, Context.MODE_PRIVATE);
        }
        return sharedPreferences;
    }

}
