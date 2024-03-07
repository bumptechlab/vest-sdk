package book.util

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

fun Bitmap?.saveToFile(file: File?) {
    var out: FileOutputStream? = null
    try {
        file.ensureFile()
        out = FileOutputStream(file)
        this?.compress(Bitmap.CompressFormat.PNG, 100, out)
    } catch (_: Exception) {
    } finally {
        out.safeClose()
    }
}

fun File?.scanToAlbum() {
    val uri: Uri = try {
        Uri.fromFile(this)
    } catch (e: Exception) {
        return
    }
    AppGlobal.application!!.sendBroadcast(
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
    )
}

fun String?.base64ToBitmap(): Bitmap {
    val bytes = Base64.decode(this, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun String?.base64ToDrawable(): Drawable {
    return BitmapDrawable(AppGlobal.application?.resources, this.base64ToBitmap())
}