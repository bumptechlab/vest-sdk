package code.sdk.download

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class DownloadTask private constructor() {
    /**
     * @param url      下载连接
     * @param saveDir  储存下载文件的SDCard目录
     * @param listener 下载监听
     */
    fun download(url: String?, saveDir: String, listener: OnDownloadListener) {
        val instance = DownloadClient.mInstance
        instance.getApi().download(url)
            .map { t -> saveFile(t, saveDir, listener) }
            .compose(instance.ioSchedulers())
            .subscribe(object : Observer<File> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(file: File) {
                    if (!file.exists()) {
                        listener.onDownloadFailed()
                    } else {
                        listener.onDownloadSuccess(file)
                    }
                }

                override fun onError(e: Throwable) {
                    listener.onDownloadFailed()
                }

                override fun onComplete() {}
            })
    }

    private fun saveFile(
        responseBody: ResponseBody,
        saveDir: String,
        listener: OnDownloadListener
    ): File {
        val img = File(saveDir, System.currentTimeMillis().toString() + ".jpg")
        var inputStream: InputStream? = null
        val buf = ByteArray(2048)
        var len: Int
        var fos: FileOutputStream? = null
        try {
            inputStream = responseBody.byteStream()
            val total = responseBody.contentLength()

            fos = FileOutputStream(img)
            var sum: Long = 0
            while (inputStream.read(buf).also { len = it } != -1) {
                fos.write(buf, 0, len)
                sum += len.toLong()
                val progress = (sum * 1.0f / total * 100).toInt()
                listener.onDownloading(progress)
            }
            fos.flush()
        } catch (_: Exception) {
        } finally {
            try {
                inputStream?.close()
            } catch (_: IOException) {
            }
            try {
                fos?.close()
            } catch (_: IOException) {
            }
        }
        return img
    }

    /**
     * @param url
     * @return 从下载连接中解析出文件名
     */
    fun getNameFromUrl(url: String): String {
        return url.substring(url.lastIndexOf("/") + 1)
    }

    interface OnDownloadListener {
        /**
         * 下载成功
         */
        fun onDownloadSuccess(saveFile: File)

        /**
         * @param progress 下载进度
         */
        fun onDownloading(progress: Int)

        /**
         * 下载失败
         */
        fun onDownloadFailed()
    }

    companion object {
       private val TAG = DownloadTask::class.java.simpleName

        val mInstance by lazy { DownloadTask() }
    }
}
