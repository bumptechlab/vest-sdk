package code.sdk.core.util

import android.text.TextUtils
import code.util.AppGlobal
import code.util.LogUtil.d
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
            d(TAG, "need update GoogleAdId")
            isWaitingGoogleAdId = true
            startGetAdjustGoogleAdId()
        } else {
            d(TAG, "no need update GoogleAdId")
        }
    }

    private fun startGetAdjustGoogleAdId() {
        Adjust.getGoogleAdId(AppGlobal.application) { s ->
            d(TAG, "onGoogleAdIdRead: %s", s)
            PreferenceUtil.saveGoogleADID(s)
            isWaitingGoogleAdId = false
        }
    }
}
