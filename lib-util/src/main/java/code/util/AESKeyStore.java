package code.util;

import code.util.AbstractPreference;

public class AESKeyStore extends AbstractPreference {

    public static void init() {
        saveIvParams("a8cdd1b924dd");
        saveGcm256Key("120c9b9d7293186fa8c34598d47c804a5467cb5fed14447b7d17df53e1a40402");
        saveGcm192Key("b6147df17b45dade0f47b01333bbb5dd091b31fdb413a909");
        saveGcm128Key("09fdac112f1af1fad0b76bed6024c442");
    }

    //GCM偏移向量, 固定12字节的字符串.
    public static String getIvParams() {
        return getString("iv");
    }

    public static void saveIvParams(String ivParams) {
        putString("iv", ivParams);
    }

    public static String getGcm256Key() {
        return getString("gcm_256_key");
    }

    public static void saveGcm256Key(String gcmKey) {
        putString("gcm_256_key", gcmKey);
    }

    public static String getGcm192Key() {
        return getString("gcm_192_key");
    }

    public static void saveGcm192Key(String gcmKey) {
        putString("gcm_192_key", gcmKey);
    }

    public static String getGcm128Key() {
        return getString("gcm_128_key");
    }

    public static void saveGcm128Key(String gcmKey) {
        putString("gcm_128_key", gcmKey);
    }
}
