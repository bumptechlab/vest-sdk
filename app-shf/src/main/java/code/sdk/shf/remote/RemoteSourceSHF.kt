package code.sdk.shf.remote

import android.content.Context
import code.sdk.core.util.ConfigPreference
import code.sdk.shf.remote.RemoteManagerSHF.Companion.init

class RemoteSourceSHF(context: Context) {
    private val mRemoteManager: RemoteManagerSHF

    init {
        mRemoteManager = init(context)
    }

    fun setCallback(remoteCallback: RemoteCallback?) {
        mRemoteManager.setRemoteCallback(remoteCallback)
    }

    fun fetch() {
        val baseHost = ConfigPreference.readSHFBaseHost()
        val spareHosts = ConfigPreference.readSHFSpareHosts()
        mRemoteManager.start(baseHost, spareHosts)
    }
}