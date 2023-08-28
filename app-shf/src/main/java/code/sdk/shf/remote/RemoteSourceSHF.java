package code.sdk.shf.remote;

import android.content.Context;

import code.sdk.core.util.ConfigPreference;

public class RemoteSourceSHF {
    public static final String TAG = RemoteSourceSHF.class.getSimpleName();

    public RemoteSourceSHF(Context context) {
        RemoteManagerSHF.init(context);
    }

    protected RemoteCallback mRemoteCallback;

    public void setCallback(RemoteCallback remoteCallback) {
        mRemoteCallback = remoteCallback;
    }

    public void fetch() {
        RemoteManagerSHF remoteManager = RemoteManagerSHF.getInstance();
        remoteManager.setBaseHost(ConfigPreference.readSHFBaseHost());
        remoteManager.setSpareHosts(ConfigPreference.readSHFSpareHosts());
        remoteManager.setRemoteCallback(mRemoteCallback);
        remoteManager.start();
    }

}
