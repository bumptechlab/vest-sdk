package code.sdk.command;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import androidx.annotation.NonNull;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import code.util.AssetsUtil;


public class AssetLoaderManager {
    private static volatile AssetLoaderManager sInstance;

    private final String COCOS_H5_ENGINE_NAME = "cocos2d-js-min";


    public static AssetLoaderManager getInstance() {
        if (sInstance == null) {
            synchronized (AssetLoaderManager.class) {
                if (sInstance == null) {
                    sInstance = new AssetLoaderManager();
                }
            }
        }
        return sInstance;
    }

    private AssetLoaderManager() {
    }

    public WebResourceResponse shouldInterceptRequest(@NonNull Uri uri) {
        String path = uri.getPath();
        if (!TextUtils.isEmpty(path) && path.contains(COCOS_H5_ENGINE_NAME)) {
            try {
                String jsString = AssetsUtil.getAssetsFlagData(AssetsUtil.JS_FLAG);
                InputStream in = new ByteArrayInputStream(jsString.getBytes(StandardCharsets.UTF_8));
                return new WebResourceResponse(
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(path),
                        "utf-8", in);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
