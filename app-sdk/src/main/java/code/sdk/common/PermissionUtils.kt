package code.sdk.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * @author DeMon
 * Created on 2023/9/7.
 * E-mail demonl@binarywalk.com
 * Desc:
 */
object PermissionUtils {
    /**
     * 使用作用域Android10 以上无需申请存储权限
     * @param context
     * @return
     */
    
    fun checkStoragePermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            val permission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permission == PackageManager.PERMISSION_GRANTED
        }
    }
}
