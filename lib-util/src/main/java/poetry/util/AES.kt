package poetry.util

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES {
    private const val algorithm = "AES"
    private const val AES_GCM_MODE = "AES/GCM/NoPadding"

    /**
     * 使用GCM模式进行加密
     *
     * @param plaintext
     * @return
     */
    fun encryptByGCM(plaintext: ByteArray?, encValue: String, nonceValue: String): ByteArray? {
        try {
            val spec = SecretKeySpec(gcmKey(encValue), algorithm)
            val cipher = Cipher.getInstance(AES_GCM_MODE)
            val parameterSpec = IvParameterSpec(
                nonceValue.toByteArray()
            )
            cipher.init(Cipher.ENCRYPT_MODE, spec, parameterSpec)
            return cipher.doFinal(plaintext)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return null
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
    fun decryptAsStringByGCM(bytes: ByteArray?, nonceValue: String, encValue: String): String? {
        try {
            val parameterSpec = IvParameterSpec(
                nonceValue.toByteArray()
            )
            val cipher = Cipher.getInstance(AES_GCM_MODE)
            cipher.init(
                Cipher.DECRYPT_MODE, SecretKeySpec(gcmKey(encValue), algorithm), parameterSpec
            )
            val doFinal = cipher.doFinal(bytes)
            return String(doFinal)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        return null
    }
}