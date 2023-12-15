package code.util;


import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    private static final String algorithm = "AES";
    private static final String AES_GCM_MODE = "AES/GCM/NoPadding";
    private static SecretKeySpec gcmSecretKey;
    public static final String GCM256 = "ag256";
    public static final String GCM192 = "ag192";
    public static final String GCM128 = "ag128";

    private static int currentMode = -1;

    public static final int MODE128 = 0;
    public static final int MODE192 = 1;
    public static final int MODE256 = 2;

    public static byte[] encryptByGCM(String plaintext, int mode) {
        return encryptByGCM(plaintext.getBytes(StandardCharsets.UTF_8), mode);
    }

    /**
     * 加密
     *
     * @param plaintext
     * @param mode
     * @return
     */
    public static byte[] encryptByGCM(byte[] plaintext, int mode) {
        try {
            SecretKeySpec spec = genKeyByMode(mode);
            Cipher cipher = Cipher.getInstance(AES_GCM_MODE);

            IvParameterSpec parameterSpec = new IvParameterSpec(AESKeyStore.getIvParams().getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, spec, parameterSpec);
            return cipher.doFinal(plaintext);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    private static SecretKeySpec genKeyByMode(int mode) {
        currentMode = mode;
        switch (mode) {
            case MODE128:
                gcmSecretKey = new SecretKeySpec(gcmKey(AESKeyStore.getGcm128Key()), algorithm);
                break;
            case MODE192:
                gcmSecretKey = new SecretKeySpec(gcmKey(AESKeyStore.getGcm192Key()), algorithm);
                break;
            case MODE256:
                gcmSecretKey = new SecretKeySpec(gcmKey(AESKeyStore.getGcm256Key()), algorithm);
                break;
        }
        return gcmSecretKey;
    }

    /**
     * 获取加密模式
     *
     * @return
     */
    public static String enc() {
        switch (currentMode) {
            case MODE128:
                return GCM128;
            case MODE192:
                return GCM192;
        }
        return GCM256;
    }

    public static String bytes2hex(byte[] bytes) {
        if (null == bytes) return null;
        StringBuilder s = new StringBuilder();
        for (byte aByte : bytes) {
            s.append(Integer.toHexString(aByte).replace("ffffff", "")).append(",");
        }
        return s.toString();
    }

    public static String nonce2hex() {
        StringBuilder s = new StringBuilder();
        String ivParams = AESKeyStore.getIvParams();
        for (char c : ivParams.toCharArray()) {
            s.append(Integer.toHexString(c).replace("ffffff", ""));
        }
        return s.toString();
    }

    /**
     * 使用GCM模式进行加密
     *
     * @param plaintext
     * @return
     */
    public static byte[] encryptByGCM(byte[] plaintext) {
        return encryptByGCM(plaintext, MODE256);
    }

    private static byte[] gcmKey(String hexTxt) {
        byte[] keys = new byte[hexTxt.length() / 2];
        int j = 0;
        for (int i = 0; i < hexTxt.length(); i += 2) {
            keys[j++] = (byte) Integer.parseInt(hexTxt.substring(i, i + 2), 16);
        }
        StringBuilder s = new StringBuilder();
        for (byte key : keys) {
            s.append(String.format("%02x", key));
        }
        return keys;
    }

    /**
     * 使用GCM模式解密
     *
     * @param bytes
     * @return
     */
    public static String decryptAsStringByGCM(byte[] bytes) {
        try {
            IvParameterSpec parameterSpec = new IvParameterSpec(AESKeyStore.getIvParams().getBytes());
            Cipher cipher = Cipher.getInstance(AES_GCM_MODE);
            cipher.init(Cipher.DECRYPT_MODE, gcmSecretKey, parameterSpec);
            byte[] doFinal = cipher.doFinal(bytes);
            return new String(doFinal);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }
}
