package code.sdk.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.ResponseBody;


public class DownloadTask {
    public static final String TAG = DownloadTask.class.getSimpleName();

    private static volatile DownloadTask mInstance;

    public static DownloadTask getInstance() {
        if (mInstance == null) {
            synchronized (DownloadTask.class) {
                if (mInstance == null) {
                    mInstance = new DownloadTask();
                }
            }
        }
        return mInstance;
    }

    private DownloadTask() {

    }

    /**
     * @param url      下载连接
     * @param saveDir  储存下载文件的SDCard目录
     * @param listener 下载监听
     */
    public void download(final String url, final String saveDir, final OnDownloadListener listener) {
        DownloadClient instance = DownloadClient.getInstance();
        instance.getApi().download(url)
                .map(responseBody -> saveFile(responseBody, saveDir, listener))
                .compose(instance.ioSchedulers())
                .subscribe(new Observer<File>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@NonNull File file) {
                        if (file == null) {
                            listener.onDownloadFailed();
                        }else{
                            listener.onDownloadSuccess(file);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        listener.onDownloadFailed();
                    }

                    @Override
                    public void onComplete() {}

                });
    }


    private File saveFile(ResponseBody responseBody, String saveDir, OnDownloadListener listener) {
        File img = null;
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len = 0;
        FileOutputStream fos = null;
        try {
            is = responseBody.byteStream();
            long total = responseBody.contentLength();
            File saveFile = new File(saveDir, System.currentTimeMillis() + ".jpg");
            fos = new FileOutputStream(saveFile);
            long sum = 0;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
                sum += len;
                int progress = (int) (sum * 1.0f / total * 100);
                listener.onDownloading(progress);
            }
            fos.flush();
            img = saveFile;
        } catch (Exception e) {

        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
        return img;
    }

    /**
     * @param url
     * @return 从下载连接中解析出文件名
     */
    public String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onDownloadSuccess(File saveFile);

        /**
         * @param progress 下载进度
         */
        void onDownloading(int progress);

        /**
         * 下载失败
         */
        void onDownloadFailed();
    }
}
