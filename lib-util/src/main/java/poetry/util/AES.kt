package poetry.util

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES {
    private const val algorithm = "AES"
    private const val AES_GCM_MODE = "AES/GCM/NoPadding"
    private var gcmSecretKey: SecretKeySpec? = null
    const val GCM256 = "ag256"
    const val GCM192 = "ag192"
    const val GCM128 = "ag128"
    private var currentMode = -1
    const val MODE128 = 0
    const val MODE192 = 1
    const val MODE256 = 2
    fun encryptByGCM(plaintext: String, mode: Int): ByteArray? {
        return encryptByGCM(plaintext.toByteArray(StandardCharsets.UTF_8), mode)
    }
    /**
     * 加密
     *
     * @param plaintext
     * @param mode
     * @return
     */
    /**
     * 使用GCM模式进行加密
     *
     * @param plaintext
     * @return
     */
    
    @JvmOverloads
    fun encryptByGCM(plaintext: ByteArray?, mode: Int = MODE256): ByteArray? {
        try {
            val spec = genKeyByMode(mode)
            val cipher = Cipher.getInstance(AES_GCM_MODE)
            val parameterSpec = IvParameterSpec(
                AESKeyStore.getIvParams()?.toByteArray()
            )
            cipher.init(Cipher.ENCRYPT_MODE, spec, parameterSpec)
            return cipher.doFinal(plaintext)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return null
    }

    private fun genKeyByMode(mode: Int): SecretKeySpec? {
        currentMode = mode
        when (mode) {
            MODE128 -> gcmSecretKey = SecretKeySpec(gcmKey(AESKeyStore.getGcm128Key()), algorithm)
            MODE192 -> gcmSecretKey = SecretKeySpec(gcmKey(AESKeyStore.getGcm192Key()), algorithm)
            MODE256 -> gcmSecretKey = SecretKeySpec(gcmKey(AESKeyStore.getGcm256Key()), algorithm)
        }
        return gcmSecretKey
    }

    /**
     * 获取加密模式
     *
     * @return
     */
    
    fun enc(): String {
        when (currentMode) {
            MODE128 -> return GCM128
            MODE192 -> return GCM192
        }
        return GCM256
    }

    fun bytes2hex(bytes: ByteArray?): String? {
        if (null == bytes) return null
        val s = StringBuilder()
        for (aByte in bytes) {
            s.append(Integer.toHexString(aByte.toInt()).replace("ffffff", "")).append(",")
        }
        return s.toString()
    }

    fun nonce2hex(): String {
        val s = StringBuilder()
        val ivParams = AESKeyStore.getIvParams()
        for (c in ivParams!!.toCharArray()) {
            s.append(Integer.toHexString(c.code).replace("ffffff", ""))
        }
        return s.toString()
    }

    private fun gcmKey(hexTxt: String?): ByteArray {
        val keys = ByteArray(hexTxt!!.length / 2)
        var j = 0
        var i = 0
        while (i < hexTxt.length) {
            keys[j++] = hexTxt.substring(i, i + 2).toInt(16).toByte()
            i += 2
        }
        val s = StringBuilder()
        for (key in keys) {
            s.append(String.format("%02x", key))
        }
        return keys
    }

    /**
     * 使用GCM模式解密
     *
     * @param bytes
     * @return
     */
    fun decryptAsStringByGCM(bytes: ByteArray?): String? {
        try {
            val parameterSpec = IvParameterSpec(
                AESKeyStore.getIvParams()?.toByteArray()
            )
            val cipher = Cipher.getInstance(AES_GCM_MODE)
            cipher.init(Cipher.DECRYPT_MODE, gcmSecretKey, parameterSpec)
            val doFinal = cipher.doFinal(bytes)
            return String(doFinal)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return null
    }
}