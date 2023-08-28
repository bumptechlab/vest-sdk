package code.util;

import org.json.JSONObject;

public class JSONUtil {

    public static void putJsonValue(JSONObject jsonObject, String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (Exception e) {

        }
    }

}
