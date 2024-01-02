package code.util

import android.app.Application

object AppGlobal {
    private val TAG = AppGlobal::class.java.simpleName
    private var sApp: Application? = null

    fun getApplication(): Application {
        if (sApp == null) {
            try {
                sApp = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication")
                    .invoke(null) as Application
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return sApp!!
    }

}