package cocos.creator.walle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ExtraInfoReader {

    private ExtraInfoReader() {
        super();
    }

    /**
     * easy api for get extra info.<br/>
     *
     * @param apkFile apk file
     * @return null if not found
     */
    public static ExtraInfo get(final File apkFile) {
        final Map<String, String> result = getMap(apkFile);
        if (result == null) {
            return null;
        }
        return new ExtraInfo(result);
    }

    /**
     * get extra info by map
     *
     * @param apkFile apk file
     * @return null if not found
     */
    public static Map<String, String> getMap(final File apkFile) {
        try {
            final String rawString = getRaw(apkFile);
            if (rawString == null) {
                return null;
            }
            final JSONObject jsonObject = new JSONObject(rawString);
            final Iterator keys = jsonObject.keys();
            final Map<String, String> result = new HashMap<String, String>();
            while (keys.hasNext()) {
                final String key = keys.next().toString();
                result.put(key, jsonObject.getString(key));
            }
            return result;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get raw string from block
     *
     * @param apkFile apk file
     * @return null if not found
     */
    public static String getRaw(final File apkFile) {
        return  PayloadReader.getString(apkFile, ApkUtil.EXTRA_INFO_BLOCK_ID);
    }
}
