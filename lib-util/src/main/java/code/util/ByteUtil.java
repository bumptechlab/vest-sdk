package code.util;

import android.text.TextUtils;

public class ByteUtil {
    public static final String TAG = ByteUtil.class.getSimpleName();

    /**
     * byte数组转16进制字符串
     *
     * @param bytes byte数组
     * @return 16进制字符串
     */
    public static String byteArrayToHexStr(byte[] bytes) {
        String strHex;
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            strHex = Integer.toHexString(aByte & 0xFF);
            sb.append(" ").append((strHex.length() == 1) ? "0" : "").append(strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }

    /**
     * byte字节转int
     *
     * @param b byte字节
     * @return int
     */
    public static int byteToInt(byte b) {
        int x = b & 0xff;
        if (x == 127) {
            return 0;
        }
        return x;
    }

    public static byte intToByte(int value) {
        return (byte) (value & 0xFF);
    }

    public static byte[] stringToBytes(String byteText) {
        if (TextUtils.isEmpty(byteText)) {
            return null;
        }
        byte[] bytes = null;
        try {
            String[] bytesArray = byteText.split(",");
            bytes = new byte[bytesArray.length];
            for (int i = 0; i < bytesArray.length; i++) {
                int byteInt = Integer.parseInt(bytesArray[i]);
                bytes[i] = intToByte(byteInt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
