package book.sdk.util

import android.content.Context
import android.os.Environment
import book.util.AESUtil
import book.util.AppGlobal
import book.util.LogUtil
import java.io.File
import java.nio.charset.StandardCharsets

object KeyChainUtil {
    private val TAG = KeyChainUtil::class.java.simpleName
    private const val sEncryptKey = "abcdef1234567890"
    fun saveAccountInfo(plainText: String) {
        try {
            val encryptedBytes = AESUtil.encrypt(
                plainText.toByteArray(StandardCharsets.UTF_8), sEncryptKey
            )
            accountFile?.writeBytes(encryptedBytes!!)
        } catch (e: Exception) {
            LogUtil.e(TAG, e.toString())
        }
    }

    fun getAccountInfo(): String {
        try {
            val encryptedBytes = accountFile?.readBytes()
            if (encryptedBytes!!.isEmpty()) {
                return ""
            }
            val decryptedBytes = AESUtil.decrypt(encryptedBytes, sEncryptKey)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            LogUtil.e(TAG, e.toString())
        }
        return ""
    }


    private val accountFile: File?
        get() {
            val context: Context? = AppGlobal.application
            val dir = context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val fileName = context?.packageName + "-act.dat"
            return File(dir, fileName)
        }
}
