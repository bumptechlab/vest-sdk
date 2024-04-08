package book.sdk.firebase


import book.util.LogUtil
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import org.json.JSONObject

object RemoteManagerFirebase {

    private val TAG = RemoteManagerFirebase::class.java.simpleName
    private lateinit var mFirebaseRC: FirebaseRemoteConfig

    @RemoteConfigFetchStatus
    private var mFirebaseRCFetchStatus = RC_FETCH_STATUS_UNDECIDED

    init {
        initFirebaseRC()
    }

    private fun initFirebaseRC() {
        mFirebaseRC = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 6 * 60 * 60 //开发阶段需要即时更新配置
            fetchTimeoutInSeconds = if (BuildConfig.DEBUG) 60 else 20 //开发阶段访问超时可以长一点，Release阶段最多20秒

        }
        mFirebaseRC.setConfigSettingsAsync(configSettings)
    }

    fun fetch() {
        fetchFirebaseRC()
    }

    private fun fetchFirebaseRC() {
        mFirebaseRC.fetchAndActivate()
            .addOnCompleteListener { task: Task<Boolean> ->
                if (task.isSuccessful) {
                    LogUtil.d(TAG, "OnComplete: success, result = " + task.result)
                } else {
                    LogUtil.d(TAG, "OnComplete: failed")
                }
            }.addOnSuccessListener { _ ->
                LogUtil.d(TAG, "OnSuccess")
                mFirebaseRCFetchStatus = RC_FETCH_STATUS_SUCCEEDED
            }.addOnFailureListener { e: Exception? ->
                LogUtil.e(TAG, "OnFailed ", e)
                mFirebaseRCFetchStatus = RC_FETCH_STATUS_FAILED
            }.addOnCanceledListener {
                LogUtil.d(TAG, "onCanceled")
            }
    }

    fun getRemoteConfig(): RemoteConfig? {
        val config: RemoteConfig? = getGoogleFirebaseRC()
        return if (config != null && config.f != RC_FETCH_STATUS_UNDECIDED
            //如果必须的参数没获取到，同样需要重试
            && (config.s
                    && config.c != null
                    && config.b != null
                    && config.l != null
                    || !config.s)
        ) config else null
    }

    private fun getGoogleFirebaseRC(): RemoteConfig? {
        return RemoteConfig(mFirebaseRCFetchStatus).apply {
            s = mFirebaseRC.getBoolean("s")
            val json = mFirebaseRC.getString("j")
            if (json.isEmpty()) {
                return@apply
            }
            try {
                JSONObject(json).apply {
                    l = optString("u")
                    c = optString("c")
                    b = optString("b")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
