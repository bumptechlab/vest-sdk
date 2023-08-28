package code.sdk.core.util;

import android.content.res.Configuration;
import android.telephony.TelephonyManager;

public class Translate {
    public static final String TAG = Translate.class.getSimpleName();

    public static String toDeviceType(int uiMode) {
        String uiModeDes = "";
        switch (uiMode) {
            case Configuration.UI_MODE_TYPE_UNDEFINED:
                uiModeDes = "UNDEFINED";
                break;
            case Configuration.UI_MODE_TYPE_NORMAL:
                uiModeDes = "NORMAL";
                break;
            case Configuration.UI_MODE_TYPE_DESK:
                uiModeDes = "DESK";
                break;
            case Configuration.UI_MODE_TYPE_CAR:
                uiModeDes = "CAR";
                break;
            case Configuration.UI_MODE_TYPE_TELEVISION:
                uiModeDes = "TELEVISION";
                break;
            case Configuration.UI_MODE_TYPE_APPLIANCE:
                uiModeDes = "APPLIANCE";
                break;
            case Configuration.UI_MODE_TYPE_WATCH:
                uiModeDes = "WATCH";
                break;
            case Configuration.UI_MODE_TYPE_VR_HEADSET:
                uiModeDes = "VR_HEADSET";
                break;
            default:
                uiModeDes = "UNKNOWN(" + uiMode + ")";
                break;
        }
        return uiModeDes;
    }

    public static String toDataState(int dataState) {
        String dataStateDes = "";
        switch (dataState) {
            case TelephonyManager.DATA_DISCONNECTED:
                dataStateDes = "DISCONNECTED";
                break;
            case TelephonyManager.DATA_CONNECTING:
                dataStateDes = "CONNECTING";
                break;
            case TelephonyManager.DATA_CONNECTED:
                dataStateDes = "CONNECTED";
                break;
            case TelephonyManager.DATA_SUSPENDED:
                dataStateDes = "SUSPENDED";
                break;
            case TelephonyManager.DATA_DISCONNECTING:
                dataStateDes = "DISCONNECTING";
                break;
            default:
                //ObfuscationStub3.inject();
                dataStateDes = "UNKNOWN(" + dataState + ")";
        }
        return dataStateDes;
    }

    public static String toSimState(int simState) {
        String simStateDes = "";
        switch (simState) {
            case TelephonyManager.SIM_STATE_UNKNOWN:
                simStateDes = "UNKNOWN";
                break;
            case TelephonyManager.SIM_STATE_ABSENT:
                simStateDes = "ABSENT";
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                simStateDes = "PIN_REQUIRED";
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                simStateDes = "PUK_REQUIRED";
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                simStateDes = "NETWORK_LOCKED";
                break;
            case TelephonyManager.SIM_STATE_READY:
                simStateDes = "READY";
                break;
            case TelephonyManager.SIM_STATE_NOT_READY:
                simStateDes = "NOT_READY";
                break;
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                simStateDes = "PERM_DISABLED";
                break;
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                simStateDes = "CARD_IO_ERROR";
                break;
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                simStateDes = "CARD_RESTRICTED";
                break;
            default:
                //ObfuscationStub5.inject();
                simStateDes = "UNKNOWN(" + simState + ")";
                break;
        }
        return simStateDes;
    }

    public static String toPhoneType(int phoneType) {
        String phoneTypeDes = "";
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_NONE:
                phoneTypeDes = "NONE";
                break;
            case TelephonyManager.PHONE_TYPE_GSM:
                phoneTypeDes = "GSM";
                break;
            case TelephonyManager.PHONE_TYPE_CDMA:
                phoneTypeDes = "CDMA";
                break;
            case TelephonyManager.PHONE_TYPE_SIP:
                phoneTypeDes = "SIP";
                break;
            default:
                //ObfuscationStub6.inject();
                phoneTypeDes = "UNKNOWN(" + phoneType + ")";
                break;
        }
        return phoneTypeDes;
    }

    public static String toNetworkType(int networkType){
        String networkTypeName = "";
        switch (networkType) {
            case NetworkType.NETWORK_TYPE_UNKNOWN:
                networkTypeName = "UNKNOWN";
                break;
            case NetworkType.NETWORK_TYPE_OFFLINE:
                networkTypeName = "OFFLINE";
                break;
            case NetworkType.NETWORK_TYPE_WIFI:
                networkTypeName = "WIFI";
                break;
            case NetworkType.NETWORK_TYPE_2G:
                networkTypeName = "2G";
                break;
            case NetworkType.NETWORK_TYPE_3G:
                networkTypeName = "3G";
                break;
            case NetworkType.NETWORK_TYPE_4G:
                networkTypeName = "4G";
                break;
            case NetworkType.NETWORK_TYPE_5G_SA:
                networkTypeName = "5G_SA";
                break;
            case NetworkType.NETWORK_TYPE_5G_NSA:
                networkTypeName = "5G_NSA";
                break;
            case NetworkType.NETWORK_TYPE_CELLULAR_UNKNOWN:
                networkTypeName = "CELLULAR_UNKNOWN";
                break;
            case NetworkType.NETWORK_TYPE_ETHERNET:
                networkTypeName = "ETHERNET";
                break;
            case NetworkType.NETWORK_TYPE_OTHER:
                networkTypeName = "OTHER";
                break;
            default:
                //ObfuscationStub2.inject();
                networkTypeName = "UNKNOWN";
                break;
        }
        return networkTypeName;
    }
}
