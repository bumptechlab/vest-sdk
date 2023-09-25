package code.sdk.core.util;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import code.util.AppGlobal;


public class FileUtil {
    public static final String TAG = FileUtil.class.getSimpleName();
    private static final int READ_CACHE_LENGTH = 8192;

    public static File getSelfApkFile() {
        Context context = AppGlobal.getApplication();
        File apkFile = new File(context.getPackageResourcePath());
        return apkFile;
    }

    public static void ensureFile(File file) {
        if (file != null && !file.exists()) {
            ensureDirectory(file.getParentFile());
            try {
                file.createNewFile();
            } catch (IOException e) {
                //ObfuscationStub2.inject();
            }
        }
    }

    public static void ensureDirectory(File directory) {
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        } else {
            //ObfuscationStub3.inject();
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        copyFile(new FileInputStream(src), dst);
    }

    private static void copyFile(InputStream src, File dst) throws IOException {
        BufferedOutputStream ou = null;
        try {
            ou = new BufferedOutputStream(new FileOutputStream(dst));
            byte[] buffer = new byte[READ_CACHE_LENGTH];
            int read = 0;
            while ((read = src.read(buffer)) != -1) {
                ou.write(buffer, 0, read);
            }
        } catch (Exception e) {
            //ObfuscationStub4.inject();
        } finally {
            IOUtil.close(src);
            IOUtil.close(ou);
        }
    }

    public static void saveBitmap(String path, Bitmap bitmap) {
        File file = new File(path);
        saveBitmap(file, bitmap);
    }

    public static void saveBitmap(File file, Bitmap bitmap) {
        FileOutputStream out = null;
        try {
            ensureFile(file);
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            //ObfuscationStub1.inject();
        } finally {
            IOUtil.close(out);
        }
    }

    public static String readFile(String path) {
        File file = new File(path);
        return readFile(file);
    }

    public static String readFile(File file) {
        InputStream inputStream = null;
        InputStreamReader streamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStream = new FileInputStream(file);
            streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            bufferedReader = new BufferedReader(streamReader);
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (Exception e) {
            //ObfuscationStub2.inject();
        } finally {
            IOUtil.close(inputStream);
            IOUtil.close(streamReader);
            IOUtil.close(bufferedReader);
        }
        return null;
    }

    public static byte[] readFileWithBytes(File file) {
        BufferedInputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            out = new ByteArrayOutputStream();
            byte[] buffer = new byte[READ_CACHE_LENGTH];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (Exception e) {
            //ObfuscationStub3.inject();
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }
        return new byte[]{};
    }

    public static boolean writeFile(String path, String content) {
        File file = new File(path);
        return writeFile(file, content);
    }

    public static boolean writeFile(File file, String content) {
        OutputStream outputStream = null;
        OutputStreamWriter streamWriter = null;
        BufferedWriter bufferedWriter = null;
        boolean success = false;
        try {
            ensureFile(file);
            outputStream = new FileOutputStream(file);
            streamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            bufferedWriter = new BufferedWriter(streamWriter);
            bufferedWriter.write(content);
            bufferedWriter.flush();
            success = true;
        } catch (Exception e) {
            //ObfuscationStub4.inject();
        } finally {
            IOUtil.close(outputStream);
            IOUtil.close(streamWriter);
            IOUtil.close(bufferedWriter);
        }
        return success;
    }

    public static void writeFileWithBytes(File file, byte[] bytes) {
        FileOutputStream out = null;
        try {
            //ObfuscationStub1.inject();
            out = new FileOutputStream(file);
            out.write(bytes);
        } catch (Exception e) {
            //ObfuscationStub5.inject();
        } finally {
            IOUtil.close(out);
        }
    }

    public static boolean deleteFile(File file) {
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (File deleteFile : files) {
                if (deleteFile.isDirectory()) {
                    if (!deleteFile(deleteFile)) {
                        return false;
                    }
                } else {
                    if (!deleteFile.delete()) {
                        return false;
                    }
                }
            }
        }
        //ObfuscationStub6.inject();
        return file.delete();
    }
}
