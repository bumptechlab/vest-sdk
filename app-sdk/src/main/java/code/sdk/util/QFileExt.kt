package code.sdk.util

import android.content.*
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor

import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileUtils
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import code.sdk.SdkInit
import java.io.*
import java.net.URLConnection


/**
 * @author DeMon
 * Created on 2020/10/23.
 * E-mail demonl@binarywalk.com
 * Desc:
 */


/**
 * 将Uri转为File
 */
fun Uri?.uriToFile(): File? {
    this ?: return null
    Log.i("FileExt", "uriToFile: $this")
    return when (scheme) {
        ContentResolver.SCHEME_FILE -> {
            File(this.path)
        }

        ContentResolver.SCHEME_CONTENT -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getFileFromUriQ()
            } else {
                getFileFromUriN()
            }
        }

        else -> {
            File(toString())
        }
    }
}

/**
 * 根据Uri获取File，AndroidQ及以上可用
 * AndroidQ中只有沙盒中的文件可以直接根据绝对路径获取File，非沙盒环境是无法根据绝对路径访问的
 * 因此先判断Uri是否是沙盒中的文件，如果是直接拼接绝对路径访问，否则使用[saveFileByUri]复制到沙盒中生成File
 */
fun Uri.getFileFromUriQ(): File? {

    var file: File? = getFileFromMedia()
    if (file == null) {
        file = getFileFromDocuments()
    }
    val flag = file?.exists() ?: false
    return if (!flag) {
        this.saveFileByUri()
    } else {
        file
    }
}

/**
 * 根据Uri获取File，AndroidN~AndroidQ可用
 */
fun Uri.getFileFromUriN(): File? {

    var file = getFileFromMedia()
    val uri = this
    Log.i("FileExt", "getFileFromUriN: $uri ${uri.authority} ${uri.path}")
    val authority = uri.authority
    val path = uri.path
    /**
     * fileProvider{@xml/file_paths}授权的Uri
     */
    if (file == null && authority != null && authority.startsWith(SdkInit.mContext.packageName) && path != null) {
        //这里的值来自你的provider_paths.xml，如果不同需要自己进行添加修改
        val externals = mutableListOf(
            "/external",
            "/external_path",
            "/beta_external_files_path",
            "/external_cache_path",
            "/beta_external_path",
            "/external_files",
            "/internal"
        )
        externals.forEach {
            if (path.startsWith(it)) {
                //如果你在provider_paths.xml中修改了path，需要自己进行修改
                val newFile = File("${Environment.getExternalStorageDirectory().absolutePath}/${path.replace(it, "")}")
                if (newFile.exists()) {
                    file = newFile
                }
            }
        }
    }
    /**
     * Intent.ACTION_OPEN_DOCUMENT选择的文件Uri
     */
    if (file == null) {
        file = getFileFromDocuments()
    }
    val flag = file?.exists() ?: false
    return if (!flag) {
        //形如content://com.android.providers.downloads.documents/document/582的下载内容中的文件
        //无法根据Uri获取到真实路径的文件，统一使用saveFileByUri()方法获取File
        uri.saveFileByUri()
    } else {
        file
    }
}

/**
 * media类型的Uri，相册中选择得到的uri，
 * 形如content://media/external/images/media/11560
 */
fun Uri.getFileFromMedia(): File? {
    var file: File? = null
    val authority = this.authority ?: "'"
    if (authority.startsWith("media")) {
        getDataColumn()?.run {
            file = File(this)
        }
    }
    return if (file?.exists() == true) {
        file
    } else {
        null
    }
}

/**
 * Intent.ACTION_OPEN_DOCUMENT选择的文件Uri
 */
fun Uri.getFileFromDocuments(): File? {
    grantPermissions(SdkInit.mContext)
    val uriId = when {
        DocumentsContract.isDocumentUri(SdkInit.mContext, this) -> {
            Log.i("FileExt", "getFileFromDocuments: isDocumentUri")
            DocumentsContract.getDocumentId(this)
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && DocumentsContract.isTreeUri(this) -> {
            Log.i("FileExt", "getFileFromDocuments: isTreeUri")
            DocumentsContract.getTreeDocumentId(this)
        }

        else -> null
    }
    Log.i("FileExt", "getFileFromDocuments: $uriId")
    uriId ?: return null
    var file: File? = null
    val split: List<String> = uriId.split(":")
    if (split.size < 2) return null
    when {
        //文件存在沙盒中，可直接拼接全路径访问
        //判断依据目前是Android/data/包名，不够严谨
        split[1].contains("Android/data/${SdkInit.mContext.packageName}") -> {
            file = File("${Environment.getExternalStorageDirectory().absolutePath}/${split[1]}")
        }

        isExternalStorageDocument() -> { //内部存储设备中选择
            if (split.size > 1) file = File("${Environment.getExternalStorageDirectory().absolutePath}/${split[1]}")
        }

        isDownloadsDocument() -> { //下载内容中选择
            if (uriId.startsWith("raw:")) {
                file = File(split[1])
            } else {
                //MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            //content://com.android.providers.downloads.documents/document/582
        }

        isMediaDocument() -> { //多媒体中选择
            var contentUri: Uri? = null
            when (split[0]) {
                "image" -> {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                "video" -> {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }

                "audio" -> {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            }
            Log.i("FileExt", "isDocumentUri contentUri: $contentUri")
            contentUri?.run {
                val uri = ContentUris.withAppendedId(this, split[1].toLong())
                Log.i("FileExt", "isDocumentUri media: $uri")
                uri.getDataColumn()?.run {
                    file = File(this)
                }

            }
        }
    }
    return if (file?.exists() == true) {
        file
    } else {
        null
    }
}

/**
 * 根据Uri查询文件路径
 * Android4.4之前都可用，Android4.4之后只有从多媒体中选择的文件可用
 */
fun Uri?.getDataColumn(): String? {

    if (this == null) return null
    var str: String? = null
    var cursor: Cursor? = null
    try {
        cursor = SdkInit.mContext.contentResolver.query(this, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
        cursor?.run {
            if (this.moveToFirst()) {
                val index = this.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                if (index != -1) str = this.getString(index)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor?.close()
    }
    Log.i("FileExt", "getDataColumn: $str")
    return str
}

/**
 * 根据Uri将文件保存File到沙盒中
 * 此方法能解决部分Uri无法获取到File的问题
 * 但是会造成文件冗余，可以根据实际情况，决定是否需要删除
 */
fun Uri.saveFileByUri(): File? {

    //文件夹uri，不复制直接return null
    if (isDirectory()) return null
    try {
        val inputStream = SdkInit.mContext.contentResolver.openInputStream(this)
        val fileName = this.getFileName() ?: "${System.currentTimeMillis()}.${getExtensionByUri()}"
        val file = File(SdkInit.mContext.getCacheChildDir(null), fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        val fos = FileOutputStream(file)
        val bis = BufferedInputStream(inputStream)
        val bos = BufferedOutputStream(fos)
        val byteArray = ByteArray(1024)
        var bytes = bis.read(byteArray)
        while (bytes > 0) {
            bos.write(byteArray, 0, bytes)
            bos.flush()
            bytes = bis.read(byteArray)
        }
        bos.close()
        fos.close()
        return file
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

/**
 * 将图片保存至相册，兼容AndroidQ
 *
 * @param name 图片名称
 */
fun File?.saveToAlbum(name: String? = null): Boolean {

    if (this == null || !exists()) return false
    Log.i("FileExt", "saveToAlbum: ${this.absolutePath}")
    runCatching {
        val values = ContentValues()
        val resolver = SdkInit.mContext.contentResolver
        val fileName = name?.run {
            this
        } ?: run {
            this.name
        }
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, fileName.getMimeTypeByFileName())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //AndroidQ更新图库需要将拍照后保存至沙盒的原图copy到系统多媒体
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val saveUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (saveUri != null) {
                val out = resolver.openOutputStream(saveUri)
                val input = FileInputStream(this)
                if (out != null) {
                    FileUtils.copy(input, out) //直接调用系统方法保存
                }
                out?.close()
                input.close()
            }
        } else {
            //作用域内的文件多媒体无法显示
            //会抛异常：UNIQUE constraint failed: files._data (code 2067)
            if (this.absolutePath.isAndroidDataFile()) {
                val file = getFileInPublicDir(fileName, Environment.DIRECTORY_PICTURES)
                //AndroidQ以下作用域的需要将文件复制到公共目录，再插入多媒体中
                this.copyFile(file)
                values.put(MediaStore.MediaColumns.DATA, file.absolutePath)
            } else {
                //AndroidQ以下非作用域的直接将文件路径插入多媒体中即可
                values.put(MediaStore.MediaColumns.DATA, this.absolutePath)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
        return true
    }.onFailure {
        it.printStackTrace()
    }
    return false
}

fun String?.saveToAlbum(name: String? = null): Boolean {
    this ?: return false
    return File(this).saveToAlbum(name)
}


/**
 * Uri授权，解决Android12和部分手机Uri无法读取访问问题
 */
fun Uri?.grantPermissions(context: Context, intent: Intent = Intent()) {
    this ?: return
    val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    for (resolveInfo in resInfoList) {
        val packageName = resolveInfo.activityInfo.packageName
        context.grantUriPermission(packageName, this, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/**
 * new一个用于保存在公有目录的文件，不会创建空文件，用于拍照，裁剪路径
 * 公有目录无需读写权限也可操作媒体文件：图片，适配，音频
 * @param name 文件名
 * @param dir 公有文件目录
 *  @see android.os.Environment.DIRECTORY_DOWNLOADS
 * @see android.os.Environment.DIRECTORY_DCIM,
 * @see android.os.Environment.DIRECTORY_MUSIC,
 * @see android.os.Environment.DIRECTORY_PODCASTS,
 * @see android.os.Environment.DIRECTORY_RINGTONES,
 * @see android.os.Environment.DIRECTORY_ALARMS,
 * @see android.os.Environment.DIRECTORY_NOTIFICATIONS,
 * @see android.os.Environment.DIRECTORY_PICTURES,
 * @see android.os.Environment.DIRECTORY_MOVIES,
 * @see android.os.Environment.DIRECTORY_DOCUMENTS
 */
fun getFileInPublicDir(name: String, type: String = Environment.DIRECTORY_DOCUMENTS): File {
    return File(Environment.getExternalStoragePublicDirectory(type), name)
}

/**
 * 创建用于保存在公有目录的文件uri，会创建空文件
 * @param name 文件名
 * @param dir 公有文件目录
 *  @see android.os.Environment.DIRECTORY_DOWNLOADS
 * @see android.os.Environment.DIRECTORY_DCIM,
 * @see android.os.Environment.DIRECTORY_MUSIC,
 * @see android.os.Environment.DIRECTORY_PODCASTS,
 * @see android.os.Environment.DIRECTORY_RINGTONES,
 * @see android.os.Environment.DIRECTORY_ALARMS,
 * @see android.os.Environment.DIRECTORY_NOTIFICATIONS,
 * @see android.os.Environment.DIRECTORY_PICTURES,
 * @see android.os.Environment.DIRECTORY_MOVIES,
 * @see android.os.Environment.DIRECTORY_DOCUMENTS
 * @return uri
 */
fun Context.createUriInPublicDir(name: String, dir: String = Environment.DIRECTORY_DOCUMENTS): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues() //内容
        val resolver = contentResolver //内容解析器
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name) //文件名
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "*/*") //文件类型
        //存放picture目录
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        val file = File(Environment.getExternalStoragePublicDirectory(dir), name)
        file.createNewFile()
        Uri.fromFile(file)
    }
}


/**
 * getFilesDir和getCacheDir是在手机自带的一块存储区域(internal storage)，通常比较小，SD卡取出也不会影响到，App的sqlite数据库和SharedPreferences都存储在这里。所以这里应该存放特别私密重要的东西。
 *
 * getExternalFilesDir和getExternalCacheDir是在SD卡下(external storage)，在sdcard/Android/data/包名/files和sdcard/Android/data/包名/cache下，会跟随App卸载被删除。
 *
 * @param type The type of files directory to return. May be {@code null}
 *            for the root of the files directory or one of the following
 *            constants for a subdirectory
 * @see android.os.Environment.DIRECTORY_MUSIC,
 * @see android.os.Environment.DIRECTORY_PODCASTS,
 * @see android.os.Environment.DIRECTORY_RINGTONES,
 * @see android.os.Environment.DIRECTORY_ALARMS,
 * @see android.os.Environment.DIRECTORY_NOTIFICATIONS,
 * @see android.os.Environment.DIRECTORY_PICTURES,
 * @see android.os.Environment.DIRECTORY_MOVIES
 */
fun Context.getExternalOrFilesDir(type: String?): File {
    // 如果获取为空则改为getFilesDir
    val dir = getExternalFilesDir(type) ?: filesDir
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

/**
 * getExternalOrFilesDir().getAbsolutePath()
 * @see getExternalOrFilesDir
 */
fun Context.getExternalOrFilesDirPath(type: String?): String {
    return getExternalOrFilesDir(type).absolutePath
}

/**
 * getFilesDir和getCacheDir是在手机自带的一块存储区域(internal storage)，通常比较小，SD卡取出也不会影响到，App的sqlite数据库和SharedPreferences都存储在这里。所以这里应该存放特别私密重要的东西。
 *
 * getExternalFilesDir和getExternalCacheDir是在SD卡下(external storage)，在sdcard/Android/data/包名/files和sdcard/Android/data/包名/cache下，会跟随App卸载被删除。
 */
fun Context.getExternalOrCacheDir(): File {
    // 如果获取为空则改为getCacheDir
    val dir = externalCacheDir ?: cacheDir
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

fun Context.getExternalOrCacheDirPath(): String {
    return getExternalOrCacheDir().absolutePath
}


/**
 * 在缓存目录下新键子目录
 */
fun Context.getCacheChildDir(child: String?): File {
    val name = if (TextUtils.isEmpty(child)) {
        "app"
    } else {
        child
    }
    val file = File(getExternalOrCacheDir(), name)
    file.mkdirs()
    return file
}

/**
 * 是否是当前作用域内的文件
 */
fun String?.isScopeFile(): Boolean {

    this ?: return false
    //内部存储
    val filesDirString = SdkInit.mContext.filesDir.parent
    Log.i("FileExt", "isScopeFile: file=$this,filesDirString=$filesDirString")
    if (!filesDirString.isNullOrEmpty() && this.contains(filesDirString)) {
        return File(this).exists()
    }
    //外部存储
    val externalFilesDirString = SdkInit.mContext.getExternalFilesDir(null)?.parent
    Log.i("FileExt", "isScopeFile: file=$this,externalFilesDirString=$externalFilesDirString")
    if (!externalFilesDirString.isNullOrEmpty() && this.contains(externalFilesDirString)) {
        return File(this).exists()
    }
    return false
}

fun File?.isScopeFile(): Boolean {
    this ?: return false
    return this.absolutePath.isScopeFile()
}

/**
 * 是否是以下作用域父文件夹内的文件，如华为手机：
 * 手机内部存储：/data/user/0/
 * 手机外部存储：/storage/emulated/0/Android/data/
 * ps:不同手机可能不一致，主要是看filesDir，getExternalFilesDir的返回结果
 */
fun String?.isAndroidDataFile(): Boolean {

    this ?: return false
    //内部存储
    val filesDirString = SdkInit.mContext.filesDir.parent
    //Log.i("FileExt", "isAndroidDataFile: file=$this,filesDirString=$filesDirString")
    if (!filesDirString.isNullOrEmpty()) {
        val dir = File(filesDirString).parent
        if (!dir.isNullOrEmpty() && this.contains(dir)) {
            return File(this).exists()
        }
    }
    //外部存储
    val externalFilesDirString = SdkInit.mContext.getExternalFilesDir(null)?.parent
    //Log.i("FileExt", "isAndroidDataFile: file=$this,externalFilesDirString=$externalFilesDirString")
    if (!externalFilesDirString.isNullOrEmpty()) {
        val dir = File(externalFilesDirString).parent
        if (!dir.isNullOrEmpty() && this.contains(dir)) {
            return File(this).exists()
        }
    }
    return false
}

fun File?.isAndroidDataFile(): Boolean {
    this ?: return false
    return this.absolutePath.isAndroidDataFile()
}

/**
 * 判断Uri是否存在
 */
fun Uri?.isFileExists(): Boolean {

    if (this == null) return false
    Log.i("FileExt", "isFileExists: $this")
    var afd: AssetFileDescriptor? = null
    return try {
        afd = SdkInit.mContext.contentResolver.openAssetFileDescriptor(this, "r")
        afd != null
    } catch (e: FileNotFoundException) {
        false
    } finally {
        afd?.close()
    }
}

/**
 * Uri是否在内部存储设备中
 */
fun Uri.isExternalStorageDocument() = "com.android.externalstorage.documents" == this.authority

/**
 * Uri是否在下载内容中
 */
fun Uri.isDownloadsDocument() = "com.android.providers.downloads.documents" == this.authority

/**
 * Uri是否在多媒体中
 */
fun Uri.isMediaDocument() = "com.android.providers.media.documents" == this.authority

/**
 * 判断uri是否是文件夹
 */
fun Uri.isDirectory(): Boolean {
    val paths: List<String> = pathSegments
    return paths.size >= 2 && "tree" == paths[0]

}

/**
 * 根据Uri获取文件名
 */
fun Uri.getFileName(): String? {

    val documentFile = DocumentFile.fromSingleUri(SdkInit.mContext, this)
    return documentFile?.name
}

/**
 * 根据Uri获取MimeType
 */
fun Uri.getMimeTypeByUri(): String? {

    return SdkInit.mContext.contentResolver.getType(this)
}


/**
 * 根据MimeType获取拓展名
 */
fun String.getExtensionByMimeType(): String {
    var ext = ""
    runCatching {
        ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(this) ?: ""
    }.onFailure {
        it.printStackTrace()
    }
    return ext
}

/**
 * 根据Uri获取扩展名
 */
fun Uri.getExtensionByUri() =
    this.getMimeTypeByUri()?.getExtensionByMimeType()

/**
 * 根据文件名获取扩展名
 */
fun String.getExtensionByFileName() =
    this.getMimeTypeByFileName().getExtensionByMimeType()

/**
 * 根据文件名获取MimeType
 */
fun String.getMimeTypeByFileName(): String {
    var mimeType = ""
    runCatching {
        mimeType = URLConnection.getFileNameMap().getContentTypeFor(this)
    }.onFailure {
        it.printStackTrace()
    }
    return mimeType
}

/**
 * 复制文件
 */
fun File?.copyFile(dest: File): Boolean {
    this ?: return false
    var input: InputStream? = null
    var output: OutputStream? = null
    try {
        if (!dest.exists()) {
            dest.createNewFile()
        }
        input = FileInputStream(this)
        output = FileOutputStream(dest)
        val buf = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buf).also { bytesRead = it } > 0) {
            output.write(buf, 0, bytesRead)
        }
        output.flush()
        Log.i("FileExt", "copyFile succeed: ${dest.absolutePath}")
        return true
    } catch (e: Exception) {
        Log.d("FileExt", "copyFile error: " + e.message)
        e.printStackTrace()
    } finally {
        try {
            input?.close()
            output?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return false
}
