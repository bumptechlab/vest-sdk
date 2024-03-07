package book.sdk.core.manager

import android.app.Activity
import book.sdk.core.SimpleLifecycleCallbacks
import com.adjust.sdk.Adjust

class AdjustLifecycleCallbacks : SimpleLifecycleCallbacks() {

    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
    }
}
