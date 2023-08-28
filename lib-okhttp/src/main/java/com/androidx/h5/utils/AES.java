package com.androidx.h5.utils;


import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    private static final String GCM_256_KEY = "120c9b9d7293186fa8c34598d47c804a5467cb5fed14447b7d17df53e1a40402";
    private static final String GCM_192_KEY = "b6147df17b45dade0f47b01333bbb5dd091b31fdb413a909";
    private static final String GCM_128_KEY = "09fdac112f1af1fad0b76bed6024c442";

    private static final String algorithm = "AES";
    private static final String AES_GCM_MODE = "AES/GCM/NoPadding";
    private static SecretKeySpec gcmSecretKey;
    //GCM偏移向量, 固定12字节的字符串.
    public static final String NONCE = "a8cdd1b924dd";
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
            IvParameterSpec parameterSpec = new IvParameterSpec(NONCE.getBytes());
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
                gcmSecretKey = new SecretKeySpec(gcmKey(GCM_128_KEY), algorithm);
                break;
            case MODE192:
                gcmSecretKey = new SecretKeySpec(gcmKey(GCM_192_KEY), algorithm);
                break;
            case MODE256:
                gcmSecretKey = new SecretKeySpec(gcmKey(GCM_256_KEY), algorithm);
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
        if (null == bytes)return null;
        StringBuilder s = new StringBuilder();
        for (byte aByte : bytes) {
            s.append(Integer.toHexString(aByte).replace("ffffff","")).append(",");
        }
        return s.toString();
    }

    public static String nonce2hex() {
        StringBuilder s = new StringBuilder();
        for (char c : NONCE.toCharArray()) {
            s.append(Integer.toHexString(c).replace("ffffff",""));
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
            s.append(String.format("%02x",key));
        }
        System.out.println("加密秘钥: " + s);
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
            IvParameterSpec parameterSpec = new IvParameterSpec(NONCE.getBytes());
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
