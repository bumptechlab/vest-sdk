package code.sdk.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.nio.charset.StandardCharsets;

import code.sdk.core.util.FileUtil;
import code.util.AESUtil;
import code.util.AppGlobal;
import code.util.LogUtil;

public class KeyChainUtil {
    public static final String TAG = KeyChainUtil.class.getSimpleName();

    private static final String sEncryptKey = "abcdef1234567890";

    public static void saveAccountInfo(String plainText) {
        try {
            byte[] encryptedBytes = AESUtil.encrypt(
                    plainText.getBytes(StandardCharsets.UTF_8), sEncryptKey);
            FileUtil.writeFileWithBytes(getAccountFile(), encryptedBytes);
        } catch (Exception e) {
            //ObfuscationStub3.inject();
            LogUtil.e(TAG, e.toString());
        }
    }

    public static String getAccountInfo() {
        return getAccountInfo(getAccountFile());
    }

    private static String getAccountInfo(File file) {
        try {
            byte[] encryptedBytes = FileUtil.readFileWithBytes(file);
            if (encryptedBytes.length == 0) {
                return "";
            }
            byte[] decryptedBytes = AESUtil.decrypt(encryptedBytes, sEncryptKey);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            //ObfuscationStub4.inject();
            LogUtil.e(TAG, e.toString());
        }
        return "";
    }

    private static File getAccountFile() {
        Context context = AppGlobal.getApplication();
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String fileName = context.getPackageName() + "-act.dat";
        return new File(dir, fileName);
    }
}
