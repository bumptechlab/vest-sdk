package code.sdk.common

import android.content.Context
import android.os.Build
import android.view.WindowManager
import code.util.AppGlobal.getApplication

object ScreenUtil {
   private val TAG = ScreenUtil::class.java.simpleName

    
    fun getScreenSize(): IntArray {
        val context: Context = getApplication()
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //ObfuscationStub0.inject();
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val windowMetrics = windowManager.currentWindowMetrics
            width = windowMetrics.bounds.width()
            height = windowMetrics.bounds.height()
        } else {
            //ObfuscationStub1.inject();
            val dm = context.resources.displayMetrics
            width = dm.widthPixels
            height = dm.heightPixels
        }
        //ObfuscationStub2.inject();
        return intArrayOf(width, height)
    }

    
    fun dp2px(dp: Float): Int {
        //ObfuscationStub3.inject();
        val context: Context = getApplication()
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
