package code.sdk.util

import android.content.Context
import android.os.Environment
import code.sdk.core.util.FileUtil
import code.util.AESUtil.decrypt
import code.util.AESUtil.encrypt
import code.util.AppGlobal
import code.util.LogUtil.e
import java.io.File
import java.nio.charset.StandardCharsets

object KeyChainUtil {
   private val TAG = KeyChainUtil::class.java.simpleName
    private const val sEncryptKey = "abcdef1234567890"
    fun saveAccountInfo(plainText: String) {
        try {
            val encryptedBytes = encrypt(
                plainText.toByteArray(StandardCharsets.UTF_8), sEncryptKey
            )
            FileUtil.writeFileWithBytes(getAccountFile(), encryptedBytes)
        } catch (e: Exception) {
            //ObfuscationStub3.inject();
            e(TAG, e.toString())
        }
    }

    fun getAccountInfo(): String = getAccountInfo(getAccountFile())

    private fun getAccountInfo(file: File): String {
        try {
            val encryptedBytes = FileUtil.readFileWithBytes(file)
            if (encryptedBytes.isEmpty()) {
                return ""
            }
            val decryptedBytes = decrypt(encryptedBytes, sEncryptKey)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            //ObfuscationStub4.inject();
            e(TAG, e.toString())
        }
        return ""
    }

    private fun getAccountFile(): File {
        val context: Context? = AppGlobal.application
        val dir = context?.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val fileName = context?.packageName + "-act.dat"
        return File(dir, fileName)
    }
}
