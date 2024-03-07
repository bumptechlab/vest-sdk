package book.sdk.core.manager

import android.app.Activity
import android.os.Process
import book.sdk.core.util.DeviceUtil
import java.util.Stack
import kotlin.system.exitProcess

class ActivityManager private constructor() {
    private val mActivityStack: Stack<Activity> = Stack()

    companion object {
        val mInstance by lazy { ActivityManager() }
    }

    fun push(activity: Activity?) {
        if (activity == null) {
            return
        }
        mActivityStack.push(activity)
    }

    fun remove(activity: Activity?) {
        if (activity == null) {
            return
        }
        mActivityStack.remove(activity)
    }

    fun isActivityEmpty(): Boolean = mActivityStack.isEmpty()

    fun finishAll() {
        try {
            while (!mActivityStack.isEmpty()) {
                val activity = mActivityStack.pop()
                if (activity != null) {
                    DeviceUtil.finishActivitySafety(activity)
                }
            }
            Process.killProcess(Process.myPid())
            exitProcess(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
