package code.sdk.core.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;

public class NetworkUtil {
    public static final String TAG = NetworkUtil.class.getSimpleName();

    public static boolean isAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        //ObfuscationStub3.inject();
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        //ObfuscationStub4.inject();
        return activeNetwork != null
                && activeNetwork.isConnected();
    }

}
