package code.util

import android.content.Intent
import android.net.Uri
import java.io.File

object ImageUtil {
   private val TAG = ImageUtil::class.java.simpleName
    fun triggerScanning(imagePath: String) {
        triggerScanning(File(imagePath))
    }

    
    fun triggerScanning(image: File?) {
        val uri: Uri = try {
            Uri.fromFile(image)
        } catch (e: Exception) {
            //ObfuscationStub8.inject();
            return
        }
        AppGlobal.application!!.sendBroadcast(
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        )
    }
}