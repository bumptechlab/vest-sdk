package code.sdk.core.manager

import android.app.Activity
import com.adjust.sdk.Adjust

class AdjustLifecycleCallbacks : SimpleLifecycleCallbacks() {

    private val TAG = AdjustLifecycleCallbacks::class.java.simpleName
    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
    }
}
