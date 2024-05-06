package poetry.sdk.firebase

import poetry.util.LogUtil
import kotlinx.coroutines.delay

class RemoteSourceFirebase(private val onResult: suspend (Boolean, RemoteConfig?) -> Unit = { success, remoteConfig -> }) {
    // 请求RC的过程中，轮询结果
    private val INTERVAL_CHECK_RC = 1000L
    private val MAX_CHECK_RC_COUNT = 10
    private var mCheckRCCount = 0
    suspend fun fetch() {
        RemoteManagerFirebase.fetch()
        delay(INTERVAL_CHECK_RC)
        checkRemoteConfigFirebase()
    }

    private suspend fun checkRemoteConfigFirebase() {
        if (mCheckRCCount++ >= MAX_CHECK_RC_COUNT) {
            LogUtil.d(TAG, "[Vest-Firebase] give up RC checking：$mCheckRCCount")
            onResult(false, null)
            return
        }
        val config = RemoteManagerFirebase.getRemoteConfig()
        if (config == null) {
            LogUtil.d(TAG, String.format("[Vest-Firebase] check config = null"))
            delay(INTERVAL_CHECK_RC)
            checkRemoteConfigFirebase()
            return
        }
        LogUtil.d(TAG, String.format("[Vest-Firebase] check config = $config"))
        if (RC_FETCH_STATUS_FAILED == config.f) {
            onResult(false, null)
        } else if (RC_FETCH_STATUS_SUCCEEDED == config.f) {
            onResult(true, config)
        } else {
            assert(false)
        }
    }

    companion object {
        val TAG = RemoteSourceFirebase::class.java.simpleName
    }
}
