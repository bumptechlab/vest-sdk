package code.sdk.bridge;

import android.net.Uri;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import code.sdk.core.util.PackageUtil;
import code.util.LogUtil;
import code.util.MD5;

/**
 * JsBridge的父级实现类
 */
public class JsBridgeCore extends Bridge {

    private static final String TAG = JsBridgeCore.class.getSimpleName();

    public JsBridgeCore(BridgeCallback callback) {
        super(new JsBridgeImpl(callback));
    }

    /**
     * @param request 方法请求格式如下：
     *                {
     *                "methodName": "setCocosData",
     *                "parameters": ["_int_2195730_promotion_guild_new", "1"]
     *                }
     * @return 方法执行结果统一用字符串表示
     */
    @Override
    public String post(String request) {
        if (TextUtils.isEmpty(request)) {
            return "";
        }
        String result = "";
        try {
            JSONObject requestJson = new JSONObject(request);
            String method = requestJson.optString("methodName");
            JSONArray paramsArray = requestJson.optJSONArray("parameters");
            String[] params = new String[]{};
            if (paramsArray != null) {
                params = new String[paramsArray.length()];
                for (int i = 0; i < paramsArray.length(); i++) {
                    params[i] = paramsArray.optString(i);
                }
            }
            result = dispatchRequest(method, params);
            LogUtil.d(TAG, "[JsBridge] --> %s(%s)", method, params == null ? "" : String.join(", ", params));
            LogUtil.d(TAG, "[JsBridge] <-- %s", TextUtils.isEmpty(result) ? "void" : result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 使用包名的md5后8位作为JsBridge命名空间
     *
     * @return
     */
    public static String getJsBridgeName() {
        String pkgMd5 = MD5.encrypt(PackageUtil.getPackageName());
        String jsBridgeName = pkgMd5.substring(pkgMd5.length() - 8, pkgMd5.length());
        return jsBridgeName;
    }

    /**
     * 从JsBridge中找出第一个使用@JavascriptInterface注解的方法作为跟H5的交互入口
     *
     * @return
     */
    public static String getJsBridgeInterface() {
        Class jsBridgeClass = JsBridge.class;
        Method[] methods = jsBridgeClass.getDeclaredMethods();
        String javascriptInterface = "";
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Annotation annotation = method.getAnnotation(JavascriptInterface.class);
            if (annotation != null) {
                javascriptInterface = method.getName();
                LogUtil.d(TAG, "[JsBridge] JavascriptInterface found in method: %s", jsBridgeClass.getName() + "." + method.getName());
                break;
            } else {
                LogUtil.d(TAG, "[JsBridge] JavascriptInterface not found in method: %s", jsBridgeClass.getName() + "." + method.getName());
            }
        }
        return javascriptInterface;
    }

    /**
     * 把JsBridge命名空间+交互方法传给H5，作为H5和native交互的接口
     *
     * @param url
     * @return
     */
    public static String formatUrlWithJsb(String url) {
        String newUrl = url;
        try {
            Uri uri = Uri.parse(url);
            String jsb = getJsBridgeName() + "." + getJsBridgeInterface();
            newUrl = uri.buildUpon().appendQueryParameter("jsb", jsb).build().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newUrl;
    }


}
