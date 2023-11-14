package code.sdk.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;


public class ImageUtil {

    public static Bitmap base64ToBitmap( String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static Drawable base64ToDrawable(Resources res, String base64Data) {
        return new BitmapDrawable(res, base64ToBitmap(base64Data));
    }

}
