package code.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager

object NetworkUtil {
   private val TAG = NetworkUtil::class.java.simpleName

    @SuppressLint("MissingPermission")
    fun isAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        //ObfuscationStub3.inject();
        return (activeNetwork != null
                && activeNetwork.isConnectedOrConnecting)
    }

    @SuppressLint("MissingPermission")
    
    fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        //ObfuscationStub4.inject();
        return (activeNetwork != null
                && activeNetwork.isConnected)
    }
}