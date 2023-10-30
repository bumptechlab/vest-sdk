package code.sdk.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.Gravity;

import java.io.File;

import code.sdk.bridge.BridgeCallback;
import code.sdk.bridge.JsBridgeImpl;
import code.sdk.core.util.FileUtil;
import code.sdk.core.util.PackageUtil;
import code.util.AppGlobal;
import code.util.LogUtil;

public class PromotionImageSynthesizer extends AsyncTask<Void, Void, String> {
    public static final String TAG = PromotionImageSynthesizer.class.getSimpleName();

    private String mQrCodeUrl;
    private int mSize = 0, mX = 0, mY = 0;
    private BridgeCallback mCallback;

    private Context context;

    public PromotionImageSynthesizer(Context context, String qrCodeUrl, int size, int x, int y, BridgeCallback callback) {
        this.context = context;
        mQrCodeUrl = qrCodeUrl;
        mSize = size;
        mX = x;
        mY = y;
        mCallback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        //ObfuscationStub0.inject();

        // convert url into bitmap
        Bitmap qrBitmap = QRCodeUtil.createQRCodeBitmap(mSize, mQrCodeUrl);
        // get material image as bitmap
        Bitmap materialBitmap = getPromotionMaterial();

        // synthesize
        BitmapDrawable[] bitmapDrawables = new BitmapDrawable[2];
        Resources resources = AppGlobal.getApplication().getResources();
        bitmapDrawables[0] = new BitmapDrawable(resources, materialBitmap);
        bitmapDrawables[1] = new BitmapDrawable(resources, qrBitmap);
        LayerDrawable layerDrawable = new LayerDrawable(bitmapDrawables);
        bitmapDrawables[1].setGravity(Gravity.LEFT | Gravity.TOP);
        layerDrawable.setLayerInset(0, 0, 0, 0, 0);
        layerDrawable.setLayerInset(1, mX, mY, 0, 0);

        Bitmap bitmap = Bitmap.createBitmap(layerDrawable.getIntrinsicWidth(),
                layerDrawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        layerDrawable.setBounds(0, 0, layerDrawable.getIntrinsicWidth(),
                layerDrawable.getIntrinsicHeight());
        layerDrawable.draw(canvas);

        return savePromotionImage(bitmap);
    }

    @Override
    protected void onPostExecute(String promotionImagePath) {
        //ObfuscationStub1.inject();

        File promotionImage = new File(promotionImagePath);
        boolean succeed = promotionImage.exists() && promotionImage.length() > 0;
        if (mCallback != null) {
            mCallback.synthesizePromotionImageDone(succeed);
        }
    }

    private Bitmap getPromotionMaterial() {
        //ObfuscationStub2.inject();

        Context context = AppGlobal.getApplication();
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        String fileName = String.format(JsBridgeImpl.PROMOTION_MATERIAL_FILENAME,
                PackageUtil.getPackageName(), PackageUtil.getChannel());
        return BitmapFactory.decodeFile(dir + File.separator + fileName);
    }

    private String savePromotionImage(Bitmap bitmap) {
        //ObfuscationStub3.inject();
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM);
        String fileName = String.format(JsBridgeImpl.PROMOTION_IMAGE_FILENAME, System.currentTimeMillis());
        String promotionImagePath = dir + File.separator + fileName;
        FileUtil.saveBitmap(promotionImagePath, bitmap);

        LogUtil.d(TAG, "synthesized image path = " + promotionImagePath);
        QFileExtKt.saveToAlbum(promotionImagePath, null);
        return promotionImagePath;
    }
}
