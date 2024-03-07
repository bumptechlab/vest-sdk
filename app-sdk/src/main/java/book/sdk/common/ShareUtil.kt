package book.sdk.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import book.sdk.core.util.DeviceUtil
import java.io.File

object ShareUtil {
   private val TAG = ShareUtil::class.java.simpleName
    
    fun sendText(context: Context, text: String?) {
        val sendIntent = Intent()
        sendIntent.setAction(Intent.ACTION_SEND)
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        sendIntent.setType("text/plain")
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    fun sendImage(context: Context, file: File?) {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file!!)
        } else {
            Uri.fromFile(file)
        }
        val sendIntent = Intent()
        sendIntent.setAction(Intent.ACTION_SEND)
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        sendIntent.setType("image/*")
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        val shareIntent = Intent.createChooser(sendIntent, "Share File")
        val resInfoList = context.packageManager.queryIntentActivities(shareIntent,
            PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(shareIntent)
    }

    
    fun shareToWhatsApp(context: Context, text: String?, file: File?) {
        val uri: Uri? = null
        //        if (file != null && file.exists()) {
//            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
//        }
        val intent = Intent()
        intent.setAction("android.intent.action.SEND")
        intent.putExtra("android.intent.extra.SUBJECT", "WhatsApp")
        intent.putExtra("android.intent.extra.TEXT", text)
        if (uri != null) {
            intent.setType("image/*")
            intent.putExtra("android.intent.extra.STREAM", uri)
        } else {
            intent.setType("text/plain")
        }
        intent.setPackage("com.whatsapp")
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        try {
            context.startActivity(intent)
        } catch (unused: Exception) {
            unused.printStackTrace()
            DeviceUtil.openMarket(context, "com.whatsapp")
        }
    }
}
