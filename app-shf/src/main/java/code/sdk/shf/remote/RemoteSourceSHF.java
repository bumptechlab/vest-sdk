package code.sdk.shf.remote;

import android.content.Context;

import code.sdk.core.util.ConfigPreference;

public class RemoteSourceSHF {

    private RemoteManagerSHF mRemoteManager;

    public RemoteSourceSHF(Context context) {
        mRemoteManager = RemoteManagerSHF.init(context);
    }

    public void setCallback(RemoteCallback remoteCallback) {
        mRemoteManager.setRemoteCallback(remoteCallback);
    }

    public void fetch() {
        String baseHost = ConfigPreference.readSHFBaseHost();
        String[] spareHosts = ConfigPreference.readSHFSpareHosts();
        mRemoteManager.start(baseHost, spareHosts);
    }
}

