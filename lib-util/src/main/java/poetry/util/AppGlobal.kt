package poetry.util

import android.app.Application

object AppGlobal {
    private val TAG = AppGlobal::class.java.simpleName
    @JvmStatic
    var application: Application? = null
        get() {
            if (field == null) {
                try {
                    field = Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null) as Application
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return field
        }
}
