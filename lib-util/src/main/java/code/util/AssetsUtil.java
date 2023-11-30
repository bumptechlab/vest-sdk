package code.util;

import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetsUtil {
    public static final int TIME_FLAG = 0x868686;
    public static final int JS_FLAG = 0x666666;

    public static String getAssetsFlagData(int flag) {
        AssetManager assetManager = AppGlobal.getApplication().getAssets();
        //获取assets目录所有文件名称
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (files != null) {
            for (String str : files) {
                InputStream inputStream = null;
                try {
                    //读取4字节数据
                    inputStream = assetManager.open(str);
                    byte[] bytes = new byte[8];
                    inputStream.read(bytes);
                    //根据自定义的文件特征获取时间数据
                    if (AESUtil.isAESData(bytes, flag)) {
                        // 读取文件内容
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                        String time = AESUtil.decryptTime(stringBuilder.toString().getBytes());
                        if (!TextUtils.isEmpty(time)) {
                            return time;
                        }
                    }
                } catch (Exception e) {

                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }
        return null;
    }
}
