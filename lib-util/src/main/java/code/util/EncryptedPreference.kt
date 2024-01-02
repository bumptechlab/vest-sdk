package code.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import code.util.AppGlobal.getApplication

open class EncryptedPreference {
    companion object {
        private val TAG = EncryptedPreference::class.java.simpleName

        @JvmStatic
        protected fun getSharedPreferences(fileName: String): SharedPreferences {
            return try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    fileName,
                    masterKeyAlias,
                    getApplication(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                e.printStackTrace()
                LogUtil.e(TAG, e, "Fail to create encrypted shared preference")
                getApplication().getSharedPreferences(fileName, Context.MODE_PRIVATE)
            }
        }
    }
}