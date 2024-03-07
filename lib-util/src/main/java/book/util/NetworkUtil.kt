package book.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager

object NetworkUtil {
   private val TAG = NetworkUtil::class.java.simpleName

    @SuppressLint("MissingPermission")
    fun isAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return (activeNetwork != null
                && activeNetwork.isConnectedOrConnecting)
    }

    @SuppressLint("MissingPermission")
    
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return (activeNetwork != null
                && activeNetwork.isConnected)
    }
}