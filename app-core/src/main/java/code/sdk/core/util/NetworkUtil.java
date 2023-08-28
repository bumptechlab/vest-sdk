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

    @NetworkType.NetworkTypeEnum
    public static int getNetworkType(Context context) {
        NetworkInfo networkInfo;
        @Nullable
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return NetworkType.NETWORK_TYPE_UNKNOWN;
        }
        try {
            networkInfo = connectivityManager.getActiveNetworkInfo();
        } catch (SecurityException e) {
            //ObfuscationStub5.inject();

            // Expected if permission was revoked.
            return NetworkType.NETWORK_TYPE_UNKNOWN;
        }
        if (networkInfo == null || !networkInfo.isConnected()) {
            return NetworkType.NETWORK_TYPE_OFFLINE;
        }
        switch (networkInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI:
                return NetworkType.NETWORK_TYPE_WIFI;
            case ConnectivityManager.TYPE_WIMAX:
                return NetworkType.NETWORK_TYPE_4G;
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                return getMobileNetworkType(networkInfo);
            case ConnectivityManager.TYPE_ETHERNET:
                return NetworkType.NETWORK_TYPE_ETHERNET;
            default:
                //ObfuscationStub6.inject();
                return NetworkType.NETWORK_TYPE_OTHER;
        }
    }

    @NetworkType.NetworkTypeEnum
    private static int getMobileNetworkType(NetworkInfo networkInfo) {
        switch (networkInfo.getSubtype()) {
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return NetworkType.NETWORK_TYPE_2G;
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return NetworkType.NETWORK_TYPE_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NetworkType.NETWORK_TYPE_4G;
            case TelephonyManager.NETWORK_TYPE_NR:
                return Build.VERSION.SDK_INT >= 29 ? NetworkType.NETWORK_TYPE_5G_SA : NetworkType.NETWORK_TYPE_UNKNOWN;
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return NetworkType.NETWORK_TYPE_WIFI;
            case TelephonyManager.NETWORK_TYPE_GSM:
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
            default: // Future mobile network types.
                //ObfuscationStub7.inject();
                return NetworkType.NETWORK_TYPE_CELLULAR_UNKNOWN;
        }
    }

}
