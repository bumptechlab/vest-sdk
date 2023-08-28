package code.sdk.core.util;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class NetworkType {
    public static final String TAG = NetworkType.class.getSimpleName();

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            NETWORK_TYPE_UNKNOWN,
            NETWORK_TYPE_OFFLINE,
            NETWORK_TYPE_WIFI,
            NETWORK_TYPE_2G,
            NETWORK_TYPE_3G,
            NETWORK_TYPE_4G,
            NETWORK_TYPE_5G_SA,
            NETWORK_TYPE_5G_NSA,
            NETWORK_TYPE_CELLULAR_UNKNOWN,
            NETWORK_TYPE_ETHERNET,
            NETWORK_TYPE_OTHER
    })
    public @interface NetworkTypeEnum {
    }

    /**
     * Unknown network type.
     */
    public static final int NETWORK_TYPE_UNKNOWN = 0;
    /**
     * No network connection.
     */
    public static final int NETWORK_TYPE_OFFLINE = 1;
    /**
     * Network type for a Wifi connection.
     */
    public static final int NETWORK_TYPE_WIFI = 2;
    /**
     * Network type for a 2G cellular connection.
     */
    public static final int NETWORK_TYPE_2G = 3;
    /**
     * Network type for a 3G cellular connection.
     */
    public static final int NETWORK_TYPE_3G = 4;
    /**
     * Network type for a 4G cellular connection.
     */
    public static final int NETWORK_TYPE_4G = 5;
    /**
     * Network type for a 5G stand-alone (SA) cellular connection.
     */
    public static final int NETWORK_TYPE_5G_SA = 9;
    /**
     * Network type for a 5G non-stand-alone (NSA) cellular connection.
     */
    public static final int NETWORK_TYPE_5G_NSA = 10;
    /**
     * Network type for cellular connections which cannot be mapped to one of {@link
     * #NETWORK_TYPE_2G}, {@link #NETWORK_TYPE_3G}, or {@link #NETWORK_TYPE_4G}.
     */
    public static final int NETWORK_TYPE_CELLULAR_UNKNOWN = 6;
    /**
     * Network type for an Ethernet connection.
     */
    public static final int NETWORK_TYPE_ETHERNET = 7;
    /**
     * Network type for other connections which are not Wifi or cellular (e.g. VPN, Bluetooth).
     */
    public static final int NETWORK_TYPE_OTHER = 8;
}

/**
 * Network connection type. One of {@link #NETWORK_TYPE_UNKNOWN}, {@link #NETWORK_TYPE_OFFLINE},
 * {@link #NETWORK_TYPE_WIFI}, {@link #NETWORK_TYPE_2G}, {@link #NETWORK_TYPE_3G}, {@link
 * #NETWORK_TYPE_4G}, {@link #NETWORK_TYPE_5G_SA}, {@link #NETWORK_TYPE_5G_NSA}, {@link
 * #NETWORK_TYPE_CELLULAR_UNKNOWN}, {@link #NETWORK_TYPE_ETHERNET} or {@link #NETWORK_TYPE_OTHER}.
 */

