package code.sdk.util;

import android.content.Context;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import code.sdk.core.util.PackageUtil;
import code.util.LogUtil;

public class AssetsUtils {

    public static InputStream getEncryptFileStream(Context context) {
        return getDecryptFileStream(context);
    }

    private static InputStream getDecryptFileStream(Context context) {
        try {
            //1. 获取资源名称
            String fileName = PackageUtil.readMetaData("android.assets");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(fileName), StandardCharsets.UTF_8)
            );

            //2. 获取秘钥以及文件名称
            String content = "";
            String secret = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) { //过滤首行
                    if (null == secret) { //过滤秘钥行
                        secret = line;
                    } else {
                        content += line;
                    }
                }
            }

            //3. 解析秘钥
            if (null != secret) {
                String split = Base64.encodeToString(",".getBytes(), Base64.DEFAULT).trim();
                LogUtil.i("AssetsUtils", split);
                String[] result = secret.split(split);
                if (result.length != 2) {
                    LogUtil.i("AssetsUtils", "split size error!");
                    return null;
                }
                String key = result[1];
                LogUtil.i("AssetsUtils", "key=" + key);
                String trickName = new String(decrypt(result[0], key));
                LogUtil.i("AssetsUtils", "trickName=" + trickName + ",fileName=" + fileName);
                if (!fileName.equals(trickName)) {
                    LogUtil.i("AssetsUtils", "file error!");
                    return null;
                }
                return new ByteArrayInputStream(decrypt(content, key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] decrypt(String content, String decryptKey) throws Exception {
        byte[] decode = Base64.decode(content, Base64.NO_WRAP);
        byte[] keyCode = Base64.decode(decryptKey, Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, new byte[]{1, 2, 3, 4, 5, 7, 8, 9, 0});
        SecretKeySpec keySpec = new SecretKeySpec(keyCode, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        return cipher.doFinal(decode);
    }
}