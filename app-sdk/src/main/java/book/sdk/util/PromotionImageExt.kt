package book.sdk.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Environment
import android.view.Gravity
import book.sdk.bridge.JsBridgeImpl
import book.sdk.core.util.PackageUtil
import book.util.AppGlobal
import book.util.LogUtil
import book.util.saveToAlbum
import book.util.saveToFile
import java.io.File

val TAG = "PromotionImageExt"

/**
 * 耗时操作，外部要用协程
 */
suspend fun String?.createPromotionImage(
    size: Int,
    x: Int,
    y: Int,
    callback: (Boolean) -> Unit
) {
    val qrCodeUrl = this
    // convert url into bitmap
    val qrBitmap = QRCodeUtil.createQRCodeBitmap(size, qrCodeUrl)
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
    layerDrawable.setLayerInset(1, x, y, 0, 0)
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
    val promotionImagePath = savePromotionImage(bitmap)
    val promotionImage = File(promotionImagePath)
    val succeed = promotionImage.exists() && promotionImage.length() > 0
    callback(succeed)
}

val promotionMaterial: Bitmap
    get() {
        val context: Context = AppGlobal.application!!
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = JsBridgeImpl.PROMOTION_MATERIAL_FILENAME.format(
            PackageUtil.getPackageName(),
            PackageUtil.getChannel()
        )
        return BitmapFactory.decodeFile(dir.toString() + File.separator + fileName)
    }

fun savePromotionImage(bitmap: Bitmap): String {
    val promotionImageFile = promotionImageFile
    bitmap.saveToFile(promotionImageFile)
    LogUtil.d(TAG, "synthesized image path = $promotionImageFile")
    promotionImageFile.saveToAlbum(null)
    return promotionImageFile.absolutePath
}

val promotionImageFile: File
    get() {
        val dir = AppGlobal.application?.getExternalFilesDir(Environment.DIRECTORY_DCIM)
        val fileName = JsBridgeImpl.PROMOTION_IMAGE_FILENAME.format(System.currentTimeMillis())
        return File(dir, fileName)
    }

