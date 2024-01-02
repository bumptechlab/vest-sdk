package code.sdk.ws

object WsStatus {
    const val CONNECTED = 1
    const val CONNECTING = 0
    const val RECONNECT = 2
    const val DISCONNECTED = -1

    internal object CODE {
        const val NORMAL_CLOSE = 1000
        const val ABNORMAL_CLOSE = 1001
    }

    internal object TIP {
        const val NORMAL_CLOSE = "normal close"
        const val ABNORMAL_CLOSE = "abnormal close"
        const val NORMAL_START = "normal start"
        const val ABNORMAL_START = "abnormal start"
    }
}
