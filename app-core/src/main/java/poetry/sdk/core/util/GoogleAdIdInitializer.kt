package poetry.sdk.core.util

import android.text.TextUtils
import poetry.util.AppGlobal
import poetry.util.LogUtil
import com.adjust.sdk.Adjust

object GoogleAdIdInitializer {
    private val TAG = GoogleAdIdInitializer::class.java.simpleName
    private var isWaitingGoogleAdId = false
    fun needUpdateGoogleAdId(): Boolean {
        val googleAdId = PreferenceUtil.readGoogleADID()
        return TextUtils.isEmpty(googleAdId)
    }

    fun init() {
        if (needUpdateGoogleAdId() && !isWaitingGoogleAdId) {
            LogUtil.d(TAG, "need update GoogleAdId")
            isWaitingGoogleAdId = true
            startGetAdjustGoogleAdId()
        } else {
            LogUtil.d(TAG, "no need update GoogleAdId")
        }
    }

    private fun startGetAdjustGoogleAdId() {
        Adjust.getGoogleAdId(AppGlobal.application) { s ->
            LogUtil.d(TAG, "onGoogleAdIdRead: %s", s)
            PreferenceUtil.saveGoogleADID(s)
            isWaitingGoogleAdId = false
        }
    }
}
