package code.util;

import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AssetsUtil {

    public static String getAssetData(String name) throws IOException {
        AssetManager assetManager = AppGlobal.getApplication().getAssets();

        // 打开assets文件夹下的文件，返回InputStream对象
        InputStream inputStream = assetManager.open(name);

        // 读取文件内容
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        // 关闭输入流
        inputStream.close();
        return stringBuilder.toString();
    }
}
