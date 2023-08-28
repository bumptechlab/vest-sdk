package code.sdk.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import code.sdk.core.util.NetworkUtil;
import code.util.LogUtil;

public class NetworkReceiver extends BroadcastReceiver {

    private static final String TAG = NetworkReceiver.class.getSimpleName();
    public NetworkStateListener mNetworkStateListener = null;
    private boolean networkListenerInit = false;

    public NetworkReceiver(NetworkStateListener networkStateListener) {
        mNetworkStateListener = networkStateListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            LogUtil.d(TAG, "receive intentAction: " + intent.getAction());
            if (networkListenerInit) {
                boolean isConnected = NetworkUtil.isConnected(context);
                if (mNetworkStateListener != null) {
                    if (isConnected) {
                        LogUtil.d(TAG, "network connected!");
                        mNetworkStateListener.onNetworkConnected();
                    } else {
                        LogUtil.d(TAG, "network disconnected!");
                        mNetworkStateListener.onNetworkDisconnected();
                    }
                }
            }
            networkListenerInit = true;
        }
    }

    public interface NetworkStateListener {
        void onNetworkConnected();

        void onNetworkDisconnected();
    }
}
