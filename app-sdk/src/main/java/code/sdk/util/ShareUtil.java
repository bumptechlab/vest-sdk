package code.sdk.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

import code.sdk.core.util.DeviceUtil;

public class ShareUtil {
    public static final String TAG = ShareUtil.class.getSimpleName();

    public static void sendText(Context context, String text) {
        //ObfuscationStub4.inject();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        context.startActivity(shareIntent);
    }

    public static void sendImage(Context context, File file) {
        //ObfuscationStub5.inject();

        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        sendIntent.setType("image/*");
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);

        Intent shareIntent = Intent.createChooser(sendIntent, "Share File");
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        context.startActivity(shareIntent);
    }

    public static void shareToWhatsApp(Context context, String text, File file) {
        Uri uri = null;
        if (file != null && file.exists()) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        }
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.SUBJECT", "WhatsApp");
        intent.putExtra("android.intent.extra.TEXT", text);
        if (uri != null) {
            intent.setType("image/*");
            intent.putExtra("android.intent.extra.STREAM", uri);
        } else {
            intent.setType("text/plain");
        }
        intent.setPackage("com.whatsapp");
        intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        try {
            context.startActivity(intent);
        } catch (Exception unused) {
            unused.printStackTrace();
            DeviceUtil.openMarket(context, "com.whatsapp");
        }
    }
}
