package poetry.util

import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object ToastUtil {
    private var sToast: Toast? = null

    fun showLongToast(resId: Int, vararg args: Any?) {
        showLongToast(AppGlobal.application?.getString(resId, args))
    }

    fun showLongToast(format: String?, vararg args: Any?) {
        MainScope().launch(Dispatchers.Main) {
            val toast = String.format(format!!, *args)
            if (sToast != null) {
                sToast!!.cancel()
            }
            sToast = Toast.makeText(AppGlobal.application, toast, Toast.LENGTH_LONG)
            sToast!!.show()
        }
    }

    fun showShortToast(resId: Int, vararg args: Any?) {
        showShortToast(AppGlobal.application?.getString(resId, args))
    }

    fun showShortToast(format: String?, vararg args: Any?) {
        MainScope().launch(Dispatchers.Main) {
            val toast = String.format(format!!, *args)
            if (sToast != null) {
                sToast!!.cancel()
            }
            sToast = Toast.makeText(AppGlobal.application, toast, Toast.LENGTH_SHORT)
            sToast!!.show()
        }
    }
}