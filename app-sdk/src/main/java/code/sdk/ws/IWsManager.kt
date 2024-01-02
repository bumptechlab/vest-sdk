package code.sdk.ws

import okhttp3.WebSocket
import okio.ByteString

internal interface IWsManager {
    fun getWebSocket(): WebSocket?
    fun startConnect()
    fun stopConnect()
    fun isWsConnected(): Boolean
    fun getCurrentStatus(): Int
    fun setCurrentStatus(currentStatus: Int)
    fun sendMessage(msg: String?): Boolean
    fun sendMessage(byteString: ByteString?): Boolean
}
