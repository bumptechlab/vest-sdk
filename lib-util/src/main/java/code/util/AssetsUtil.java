package code.util;

import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AssetsUtil {


    public static String getAssetsBuildTime() {
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
                BufferedReader reader = null;
                try {
                    //读取一行数据
                    InputStream inputStream = assetManager.open(str);
                    byte[] bytes =new byte[92];
                    inputStream.read(bytes);
                    //根据自定义的文件特征获取时间数据
                    String time = AESUtil.decryptTime(bytes);
                    if (!TextUtils.isEmpty(time)) {
                        return time;
                    }
                } catch (IOException e) {

                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ex) {
                        }
                    }
                }
            }
        }
        return null;
    }
}
