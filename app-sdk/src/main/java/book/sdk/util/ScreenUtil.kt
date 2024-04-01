package book.sdk.util

import android.content.Context
import android.os.Build
import android.view.WindowManager
import book.util.AppGlobal

object ScreenUtil {
   private val TAG = ScreenUtil::class.java.simpleName

    
    fun getScreenSize(): IntArray {
        val context: Context = AppGlobal.application!!
        val width: Int
        val height: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val windowMetrics = windowManager.currentWindowMetrics
            width = windowMetrics.bounds.width()
            height = windowMetrics.bounds.height()
        } else {
            val dm = context.resources.displayMetrics
            width = dm.widthPixels
            height = dm.heightPixels
        }
        return intArrayOf(width, height)
    }

    
    fun dp2px(dp: Float): Int {
        val context: Context = AppGlobal.application!!
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}
