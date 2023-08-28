package code.util;

import android.content.Intent;
import android.net.Uri;

import java.io.File;

public class ImageUtil {
    public static final String TAG = ImageUtil.class.getSimpleName();

    public static void triggerScanning(String imagePath) {
        triggerScanning(new File(imagePath));
    }

    public static void triggerScanning(File image) {
        Uri uri;
        try {
            uri = Uri.fromFile(image);
        } catch (Exception e) {
            //ObfuscationStub8.inject();
            return;
        }
        AppGlobal.getApplication().sendBroadcast(
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }
}
