package code.sdk

import android.annotation.SuppressLint
import android.content.Context

/**
 * @author DeMon
 * Created on 2023/9/7.
 * E-mail demonl@binarywalk.com
 * Desc:
 */
@SuppressLint("StaticFieldLeak")
object SdkInit {
    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    lateinit var mContext: Context
}