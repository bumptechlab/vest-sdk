package book.util

object AESKeyStore : AbstractPreference("pref_aes") {

    private val TAG = AESKeyStore::class.java.simpleName

    init {
        LogUtil.d(TAG, "init AESKeyStore data")
        saveIvParams("a8cdd1b924dd")
        saveGcm256Key("120c9b9d7293186fa8c34598d47c804a5467cb5fed14447b7d17df53e1a40402")
        saveGcm192Key("b6147df17b45dade0f47b01333bbb5dd091b31fdb413a909")
        saveGcm128Key("09fdac112f1af1fad0b76bed6024c442")
    }

    //GCM偏移向量, 固定12字节的字符串.

    fun getIvParams(): String {
        return getString("iv")
    }


    fun saveIvParams(ivParams: String) {
        putString("iv", ivParams)
    }


    fun getGcm256Key(): String {
        return getString("gcm_256_key")
    }


    fun saveGcm256Key(gcmKey: String) {
        putString("gcm_256_key", gcmKey)
    }


    fun getGcm192Key(): String {
        return getString("gcm_192_key")
    }

    fun saveGcm192Key(gcmKey: String) {
        putString("gcm_192_key", gcmKey)
    }


    fun getGcm128Key(): String {
        return getString("gcm_128_key")
    }

    fun saveGcm128Key(gcmKey: String) {
        putString("gcm_128_key", gcmKey)
    }
}