package book.sdk.ws

import android.content.Context
import android.os.Handler
import android.os.Looper
import book.sdk.util.OkHttpUtil.getOkHttpClient
import book.util.LogUtil.d
import book.util.NetworkUtil.isConnected
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class WsManager(builder: Builder) : IWsManager {
    private val RECONNECT_INTERVAL = 10 * 1000 //重连自增步长
    private val RECONNECT_MAX_TIME = (120 * 1000).toLong()//最大重连间隔
    private val mSocketId: String?
    private val mContext: Context
    private val wsUrl: String?
    private val mHost: String?
    private var mHeaders: HashMap<String, String>? = null
    private var mWebSocket: WebSocket? = null
    private var mOkHttpClient: OkHttpClient?
    private var mRequest: Request? = null
    private var mCurrentStatus = WsStatus.DISCONNECTED //websocket连接状态
    private val isNeedReconnect //是否需要断线自动重连
            : Boolean
    private var isManualClose = false //是否为手动关闭websocket连接
    private var wsStatusListener: WsStatusListener? = null
    private val mLock: Lock
    private val wsMainHandler = Handler(Looper.getMainLooper())
    private var reconnectCount = 3 //重连次数
    private val reconnectRunnable = Runnable {
        if (wsStatusListener != null) {
            wsStatusListener!!.onReconnect()
        }
        buildConnect()
    }
    private val mWebSocketListener: WebSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            mWebSocket = webSocket
            setCurrentStatus(WsStatus.CONNECTED)
            connected()
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post { wsStatusListener!!.onOpen(response) }
                } else {
                    wsStatusListener!!.onOpen(response)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post { wsStatusListener!!.onMessage(bytes) }
                } else {
                    wsStatusListener!!.onMessage(bytes)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post { wsStatusListener!!.onMessage(text) }
                } else {
                    wsStatusListener!!.onMessage(text)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post { wsStatusListener!!.onClosing(code, reason) }
                } else {
                    wsStatusListener!!.onClosing(code, reason)
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post { wsStatusListener!!.onClosed(code, reason) }
                } else {
                    wsStatusListener!!.onClosed(code, reason)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            tryReconnect()
            if (wsStatusListener != null) {
                if (Looper.myLooper() != Looper.getMainLooper()) {
                    wsMainHandler.post { wsStatusListener!!.onFailure(t, response) }
                } else {
                    wsStatusListener!!.onFailure(t, response)
                }
            }
        }
    }

    init {
        mContext = builder.mContext
        wsUrl = builder.wsUrl
        mHost = builder.mHost
        isNeedReconnect = builder.needReconnect
        mOkHttpClient = builder.mOkHttpClient
        mHeaders = builder.mHeaders
        mSocketId = builder.mSocketId
        mLock = ReentrantLock()
    }

    private fun initWebSocket() {
        mOkHttpClient = getOkHttpClient(mHost!!)
        if (mRequest == null) {
            val builder = Request.Builder().url(wsUrl!!)
            addHeader(builder, mHeaders)
            mRequest = builder.build()
        }
        try {
            mLock.lockInterruptibly()
            try {
                mOkHttpClient!!.newWebSocket(mRequest!!, mWebSocketListener)
            } finally {
                mLock.unlock()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun addHeader(builder: Request.Builder, headers: HashMap<String, String>?) {
        if (headers != null) {
            val keys: Iterator<String> = headers.keys.iterator()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = headers[key]
                if (!value.isNullOrEmpty()) {
                    builder.addHeader(key, value)
                }
            }
        }
    }

    override fun getWebSocket(): WebSocket? {
        return mWebSocket
    }

    fun setWsStatusListener(wsStatusListener: WsStatusListener?) {
        this.wsStatusListener = wsStatusListener
    }

    @Synchronized
    override fun isWsConnected(): Boolean {
        return mCurrentStatus == WsStatus.CONNECTED
    }

    @Synchronized
    override fun getCurrentStatus(): Int {
        return mCurrentStatus
    }

    @Synchronized
    override fun setCurrentStatus(currentStatus: Int) {
        mCurrentStatus = currentStatus
    }

    override fun startConnect() {
        isManualClose = false
        buildConnect()
    }

    override fun stopConnect() {
        isManualClose = true
        disconnect()
    }

    private fun tryReconnect() {
        if (!isNeedReconnect or isManualClose) {
            return
        }
        if (!isConnected(mContext)) {
            setCurrentStatus(WsStatus.DISCONNECTED)
            return
        }
        setCurrentStatus(WsStatus.RECONNECT)
        val delay = (reconnectCount * RECONNECT_INTERVAL).toLong()
        wsMainHandler.removeCallbacks(reconnectRunnable)
        wsMainHandler.postDelayed(
            reconnectRunnable,
            if (delay > RECONNECT_MAX_TIME) RECONNECT_MAX_TIME else delay
        )
        reconnectCount++
    }

    private fun cancelReconnect() {
        wsMainHandler.removeCallbacks(reconnectRunnable)
        reconnectCount = 0
    }

    private fun connected() {
        cancelReconnect()
    }

    private fun disconnect() {
        if (mCurrentStatus == WsStatus.DISCONNECTED) {
            return
        }
        cancelReconnect()
        if (mWebSocket != null) {
            val isClosed = mWebSocket!!.close(WsStatus.CODE.NORMAL_CLOSE, WsStatus.TIP.NORMAL_CLOSE)
            d("wsM", WsStatus.TIP.NORMAL_START)
            //非正常关闭连接
            if (!isClosed) {
                if (wsStatusListener != null) {
                    wsStatusListener!!.onClosed(WsStatus.CODE.ABNORMAL_CLOSE, WsStatus.TIP.ABNORMAL_CLOSE)
                    d("wsM", WsStatus.TIP.ABNORMAL_START)
                }
            }
        }
        setCurrentStatus(WsStatus.DISCONNECTED)
    }

    @Synchronized
    private fun buildConnect() {
        if (!isConnected(mContext)) {
            setCurrentStatus(WsStatus.DISCONNECTED)
            return
        }
        when (getCurrentStatus()) {
            WsStatus.CONNECTED, WsStatus.CONNECTING -> {}
            else -> {
                setCurrentStatus(WsStatus.CONNECTING)
                initWebSocket()
            }
        }
    }

    //发送消息
    override fun sendMessage(msg: String?): Boolean {
        return send(msg)
    }

    override fun sendMessage(byteString: ByteString?): Boolean {
        return send(byteString)
    }

    private fun send(msg: Any?): Boolean {
        var isSend = false
        if (mWebSocket != null && mCurrentStatus == WsStatus.CONNECTED) {
            if (msg is String) {
                isSend = mWebSocket!!.send((msg as String?)!!)
            } else if (msg is ByteString) {
                isSend = mWebSocket!!.send((msg as ByteString?)!!)
            }
            //发送消息失败，尝试重连
            if (!isSend) {
                tryReconnect()
            }
        }
        return isSend
    }

    class Builder(val mContext: Context) {
        var mSocketId: String? = null
        var wsUrl: String? = null
        var mHost: String? = null
        var needReconnect = true
        var mOkHttpClient: OkHttpClient? = null
        var mHeaders: HashMap<String, String>? = null
        fun socketId(socketId: String?): Builder {
            mSocketId = socketId
            return this
        }

        fun wsUrl(`val`: String?): Builder {
            wsUrl = `val`
            return this
        }

        fun host(host: String?): Builder {
            mHost = host
            return this
        }

        fun headers(headers: HashMap<String, String>?): Builder {
            mHeaders = headers
            return this
        }

        fun client(`val`: OkHttpClient?): Builder {
            mOkHttpClient = `val`
            return this
        }

        fun needReconnect(`val`: Boolean): Builder {
            needReconnect = `val`
            return this
        }

        fun build(): WsManager {
            return WsManager(this)
        }
    }

    abstract class WsStatusListener {
        fun onOpen(response: Response?) {}
        fun onMessage(text: String?) {}
        fun onMessage(bytes: ByteString?) {}
        fun onReconnect() {}
        fun onClosing(code: Int, reason: String?) {}
        fun onClosed(code: Int, reason: String?) {}
        fun onFailure(t: Throwable?, response: Response?) {}
    }
}
