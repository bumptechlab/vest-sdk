package code.sdk.command;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;


public class AssetLoaderManager {
    private static volatile AssetLoaderManager sInstance;

    private final String COCOS_H5_ENGINE_NAME = "cocos2d-js-min";

    private Context mContext;

    public static AssetLoaderManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized(AssetLoaderManager.class) {
                if (sInstance == null) {
                    sInstance = new AssetLoaderManager(context);
                }
            }
        }
        return sInstance;
    }

    private AssetLoaderManager(Context context) {
        mContext = context;
    }

    public WebResourceResponse shouldInterceptRequest(@NonNull Uri uri) {
        String path = uri.getPath();
        if (!TextUtils.isEmpty(path) && path.contains(COCOS_H5_ENGINE_NAME)) {
            AssetManager assets = mContext.getAssets();
            try {
                InputStream in = assets.open(COCOS_H5_ENGINE_NAME + ".js");
                return new WebResourceResponse(
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(path),
                        "utf-8", in);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
