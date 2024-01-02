package code.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AESUtil {
    private val TAG = AESUtil::class.java.simpleName
    private const val KEY_ALGORITHM = "AES"
    private const val DEFAULT_CIPHER_ALGORITHM = "AES/GCM/NoPadding" // 默认的加密算法
    private const val CHARSET = "UTF-8"

    /**
     * AES 加密操作
     *
     * @param content     待加密内容
     * @param encryptPass 加密密码
     * @return 返回Base64转码后的加密数据
     */
    fun encrypt(content: String, encryptPass: String): String? {
        try {
            val contentBytes = content.toByteArray(charset(CHARSET))
            val encryptData = encrypt(contentBytes, encryptPass)
            return Base64.encodeToString(encryptData, Base64.NO_WRAP)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        return null
    }

    
    fun encrypt(contentBytes: ByteArray, encryptPass: String): ByteArray? {
        try {
            val cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM)
            val iv = ByteArray(12)
            val secureRandom = SecureRandom()
            secureRandom.nextBytes(iv)
            //LogUtil.d(TAG, "encrypt iv: " + Arrays.toString(iv));
            val params = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(encryptPass), params)
            val encryptData = cipher.doFinal(contentBytes)
            assert(encryptData.size == contentBytes.size + 16)
            val message = ByteArray(12 + contentBytes.size + 16)
            System.arraycopy(iv, 0, message, 0, 12)
            System.arraycopy(encryptData, 0, message, 12, encryptData.size)
            return message
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        return null
    }

    /**
     * AES 解密操作
     *
     * @param base64Content
     * @param encryptPass
     * @return
     */
    fun decrypt(base64Content: String?, encryptPass: String): String? {
        try {
            val contentBytes = Base64.decode(base64Content, Base64.NO_WRAP)
            val decryptData = decrypt(contentBytes, encryptPass)
            return String(decryptData)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }
        return null
    }

    
    fun decrypt(contentBytes: ByteArray, encryptPass: String): ByteArray {
        try {
            require(contentBytes.size >= 12 + 16)
            val iv = ByteArray(12)
            System.arraycopy(contentBytes, 0, iv, 0, 12)
            val params =
                GCMParameterSpec(128, iv)
            //LogUtil.d(TAG, "decrypt iv: " + Arrays.toString(iv));
            val cipher =
                Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(encryptPass),
                params
            )
            return cipher.doFinal(contentBytes, 12, contentBytes.size - 12)
        } catch (e: Exception) {
            LogUtil.e(TAG, e)
        }

        return byteArrayOf(0)
    }

    /**
     * 生成加密秘钥
     * AndroidP以上无法使用SecureRandom.getInstance(SHA1PRNG, "Crypto")生成密钥
     * 原因：The Crypto provider has been deleted in Android P (and was deprecated in Android N), so the code will crash.
     * 这个是因为Crypto provider 在Android9.0中已经被Google删除了，调用的话就会发生crash。
     * 方案：使用Google适配方案InsecureSHA1PRNGKeyDerivator
     */
    private fun getSecretKey(password: String): SecretKeySpec {
        val passwordBytes = password.toByteArray(StandardCharsets.US_ASCII)
        val keyBytes = InsecureSHA1PRNGKeyDerivator.deriveInsecureKey(passwordBytes, 16)
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    
    fun isAESData(headerBytes: ByteArray?, flag: Int): Boolean {
        try {
            return bytesToInt(Base64.decode(headerBytes, Base64.DEFAULT), 0) == flag
        } catch (e: Exception) {
        }
        return false
    }

    
    fun decryptTime(content: ByteArray?): String? {
        try {
            val bytes = Base64.decode(content, Base64.DEFAULT)
            //获取密钥
            val keyBytes = ByteArray(24)
            System.arraycopy(bytes, 0, keyBytes, 0, keyBytes.size)
            //获取数据
            val dataBytes = ByteArray(bytes.size - keyBytes.size)
            System.arraycopy(bytes, keyBytes.size, dataBytes, 0, dataBytes.size)
            //解密
            return String(decrypt(dataBytes, String(keyBytes)))
        } catch (e: Exception) {
        }
        return null
    }

    private fun bytesToInt(src: ByteArray, offset: Int): Int {
        try {
            return (src[offset].toInt() and 0xFF shl 24
                    or (src[offset + 1].toInt() and 0xFF shl 16)
                    or (src[offset + 2].toInt() and 0xFF shl 8)
                    or (src[offset + 3].toInt() and 0xFF))
        } catch (e: Exception) {
        }
        return -1
    }
}