package code.sdk.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.os.AsyncTask
import android.os.Environment
import android.view.Gravity
import code.sdk.bridge.BridgeCallback
import code.sdk.bridge.JsBridgeImpl
import code.sdk.core.util.FileUtil
import code.sdk.core.util.PackageUtil
import code.util.AppGlobal
import code.util.LogUtil.d
import java.io.File

class PromotionImageSynthesizer(
    private val context: Context,
    private val mQrCodeUrl: String?,
    size: Int,
    x: Int,
    y: Int,
    callback: BridgeCallback?
) : AsyncTask<Void?, Void?, String>() {

   private val TAG = PromotionImageSynthesizer::class.java.simpleName
    private var mSize = 0
    private var mX = 0
    private var mY = 0
    private val mCallback: BridgeCallback?

    init {
        mSize = size
        mX = x
        mY = y
        mCallback = callback
    }

    override fun doInBackground(vararg params: Void?): String {
        //ObfuscationStub0.inject();

        // convert url into bitmap
        val qrBitmap = QRCodeUtil.createQRCodeBitmap(mSize, mQrCodeUrl)
        // get material image as bitmap
        val materialBitmap = promotionMaterial

        // synthesize
        val bitmapDrawables = arrayOfNulls<BitmapDrawable>(2)
        val resources = AppGlobal.application?.resources
        bitmapDrawables[0] = BitmapDrawable(resources, materialBitmap)
        bitmapDrawables[1] = BitmapDrawable(resources, qrBitmap)
        val layerDrawable = LayerDrawable(bitmapDrawables)
        bitmapDrawables[1]!!.gravity = Gravity.LEFT or Gravity.TOP
        layerDrawable.setLayerInset(0, 0, 0, 0, 0)
        layerDrawable.setLayerInset(1, mX, mY, 0, 0)
        val bitmap = Bitmap.createBitmap(
            layerDrawable.intrinsicWidth,
            layerDrawable.intrinsicHeight, Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        layerDrawable.setBounds(
            0, 0, layerDrawable.intrinsicWidth,
            layerDrawable.intrinsicHeight
        )
        layerDrawable.draw(canvas)
        return savePromotionImage(bitmap)
    }

    override fun onPostExecute(promotionImagePath: String) {
        //ObfuscationStub1.inject();
        val promotionImage = File(promotionImagePath)
        val succeed = promotionImage.exists() && promotionImage.length() > 0
        mCallback?.synthesizePromotionImageDone(succeed)
    }

    private val promotionMaterial: Bitmap
        get() {
            //ObfuscationStub2.inject();
            val context: Context = AppGlobal.application!!
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val fileName = String.format(
                JsBridgeImpl.PROMOTION_MATERIAL_FILENAME,
                PackageUtil.getPackageName(), PackageUtil.getChannel()
            )
            return BitmapFactory.decodeFile(dir.toString() + File.separator + fileName)
        }

    private fun savePromotionImage(bitmap: Bitmap): String {
        //ObfuscationStub3.inject();
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
        val fileName =
            String.format(JsBridgeImpl.PROMOTION_IMAGE_FILENAME, System.currentTimeMillis())
        val promotionImagePath = dir.toString() + File.separator + fileName
        FileUtil.saveBitmap(promotionImagePath, bitmap)
        d(TAG, "synthesized image path = $promotionImagePath")
        promotionImagePath.saveToAlbum(null)
        return promotionImagePath
    }

}
