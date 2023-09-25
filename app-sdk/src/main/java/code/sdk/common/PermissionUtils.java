package code.sdk.common;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

/**
 * @author DeMon
 * Created on 2023/9/7.
 * E-mail demonl@binarywalk.com
 * Desc:
 */
public class PermissionUtils {


    /**
     * 使用作用域Android10 以上无需申请存储权限
     * @param context
     * @return
     */
    public static Boolean checkStoragePermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        } else {
            int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return permission == PackageManager.PERMISSION_GRANTED;
        }
    }




}
