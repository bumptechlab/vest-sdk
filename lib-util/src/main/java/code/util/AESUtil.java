package code.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {
    private static final String TAG = AESUtil.class.getSimpleName();
    private static final String KEY_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/GCM/NoPadding";// 默认的加密算法
    private static final String CHARSET = "UTF-8";

    /**
     * AES 加密操作
     *
     * @param content     待加密内容
     * @param encryptPass 加密密码
     * @return 返回Base64转码后的加密数据
     */
    public static String encrypt(String content, String encryptPass) {
        try {
            byte[] contentBytes = content.getBytes(CHARSET);
            byte[] encryptData = encrypt(contentBytes, encryptPass);
            return Base64.encodeToString(encryptData, Base64.NO_WRAP);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    public static byte[] encrypt(byte[] contentBytes, String encryptPass) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            byte[] iv = new byte[12];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            //LogUtil.d(TAG, "encrypt iv: " + Arrays.toString(iv));
            GCMParameterSpec params = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(encryptPass), params);
            byte[] encryptData = cipher.doFinal(contentBytes);
            assert encryptData.length == contentBytes.length + 16;
            byte[] message = new byte[12 + contentBytes.length + 16];
            System.arraycopy(iv, 0, message, 0, 12);
            System.arraycopy(encryptData, 0, message, 12, encryptData.length);
            return message;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    /**
     * AES 解密操作
     *
     * @param base64Content
     * @param encryptPass
     * @return
     */
    public static String decrypt(String base64Content, String encryptPass) {
        try {
            byte[] contentBytes = Base64.decode(base64Content, Base64.NO_WRAP);
            byte[] decryptData = decrypt(contentBytes, encryptPass);
            return new String(decryptData, CHARSET);
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    public static byte[] decrypt(byte[] contentBytes, String encryptPass) {
        try {
            if (contentBytes.length < 12 + 16)
                throw new IllegalArgumentException();
            byte[] iv = new byte[12];
            System.arraycopy(contentBytes, 0, iv, 0, 12);
            GCMParameterSpec params = new GCMParameterSpec(128, iv);
            //LogUtil.d(TAG, "decrypt iv: " + Arrays.toString(iv));
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(encryptPass), params);
            byte[] decryptData = cipher.doFinal(contentBytes, 12, contentBytes.length - 12);
            return decryptData;
        } catch (Exception e) {
            LogUtil.e(TAG, e);
        }
        return null;
    }

    /**
     * 生成加密秘钥
     * AndroidP以上无法使用SecureRandom.getInstance(SHA1PRNG, "Crypto")生成密钥
     * 原因：The Crypto provider has been deleted in Android P (and was deprecated in Android N), so the code will crash.
     * 这个是因为Crypto provider 在Android9.0中已经被Google删除了，调用的话就会发生crash。
     * 方案：使用Google适配方案InsecureSHA1PRNGKeyDerivator
     */
    private static SecretKeySpec getSecretKey(String password) {
        byte[] passwordBytes = password.getBytes(StandardCharsets.US_ASCII);
        byte[] keyBytes = InsecureSHA1PRNGKeyDerivator.deriveInsecureKey(passwordBytes, 16);
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    public static String decryptTime(byte[] content) {
        try {
            byte[] bytes = Base64.decode(content, Base64.DEFAULT);
            byte[] headerBytes = new byte[4];
            System.arraycopy(bytes, 0, headerBytes, 0, headerBytes.length);
            if (bytesToInt(headerBytes, 0) == 0x868686) {
                //获取密钥
                byte[] keyBytes = new byte[24];
                System.arraycopy(bytes, headerBytes.length, keyBytes, 0, keyBytes.length);
                //获取数据
                byte[] dataBytes = new byte[bytes.length - keyBytes.length - headerBytes.length];
                System.arraycopy(bytes, headerBytes.length + keyBytes.length, dataBytes, 0, dataBytes.length);
                //解密
                byte[] decryptData = decrypt(dataBytes, new String(keyBytes, StandardCharsets.UTF_8));
                return new String(decryptData, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int bytesToInt(byte[] src, int offset) {
        int value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

}
