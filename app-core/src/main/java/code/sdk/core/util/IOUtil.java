package code.sdk.core.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class IOUtil {
    public static final int BUFFER_SIZE = 4 * 1024;

    public static String readAssetContent(Context context, String file) {
        try {
            AssetManager assets = context.getAssets();
            InputStream inputStream = assets.open(file);
            return readInputStream(inputStream);
        } catch (Exception e) {
            //ObfuscationStub0.inject();
        }
        return null;
    }

    public static String readRawContent(Context context, int resId) {
        Resources resource = context.getResources();
        InputStream inputStream = resource.openRawResource(resId);
        return readInputStream(inputStream);
    }

    public static String readInputStream(InputStream inputStream) {
        InputStreamReader streamReader = null;
        try {
            streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            final StringWriter writer = new StringWriter();
            final char[] buffer = new char[BUFFER_SIZE];
            int charRead = streamReader.read(buffer);
            while (charRead > 0) {
                writer.write(buffer, 0, charRead);
                charRead = streamReader.read(buffer);
            }
            return writer.toString();
        } catch (Exception e) {
            //ObfuscationStub1.inject();
        } finally {
            IOUtil.close(inputStream);
            IOUtil.close(streamReader);
        }
        return null;
    }

    public static byte[] toByteArray(InputStream input) {
        byte[] buffer = null;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            buffer = new byte[1024 * 4];
            int n = 0;
            while ((n = input.read(buffer)) != -1) {
                output.write(buffer, 0, n);
            }
            buffer = output.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                //ObfuscationStub2.inject();
            }
        }
    }
}