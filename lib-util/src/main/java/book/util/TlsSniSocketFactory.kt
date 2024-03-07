package book.util

import android.net.SSLCertificateSocketFactory
import android.os.Build
import android.text.TextUtils
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class TlsSniSocketFactory : SSLSocketFactory {
    private val TAG = TlsSniSocketFactory::class.java.simpleName
    var hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
    var peerHost: String? = null

    constructor(peerHost: String?) {
        this.peerHost = peerHost
    }

    constructor()

    override fun createSocket(): Socket? {
        return null
    }

    override fun createSocket(host: String, port: Int): Socket? {
        return null
    }

    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket? {
        return null
    }

    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return null
    }

    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket? {
        return null
    }

    // TLS layer
    override fun getDefaultCipherSuites(): Array<String> {
        return emptyArray()
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return emptyArray()
    }

    @Throws(IOException::class)
    override fun createSocket(
        plainSocket: Socket,
        host: String,
        port: Int,
        autoClose: Boolean
    ): Socket {
        if (TextUtils.isEmpty(peerHost)) {
            peerHost = host
        }
        LogUtil.i(TAG, "customized createSocket. host: $peerHost")
        val address = plainSocket.inetAddress
        if (autoClose) {
            // we don't need the plainSocket
            plainSocket.close()
        }
        // create and connect SSL socket, but don't do hostname/certificate verification yet
        val sslSocketFactory =
            SSLCertificateSocketFactory.getDefault(0) as SSLCertificateSocketFactory
        val ssl = sslSocketFactory.createSocket(address, port) as SSLSocket
        // enable TLSv1.1/1.2 if available
        ssl.enabledProtocols = ssl.supportedProtocols
        // set up SNI before the handshake
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            LogUtil.i(TAG, "Setting SNI hostname")
            sslSocketFactory.setHostname(ssl, peerHost)
        } else {
            LogUtil.d(TAG, "No documented SNI support on Android <4.2, trying with reflection")
            try {
                val setHostnameMethod = ssl.javaClass.getMethod("setHostname", String::class.java)
                setHostnameMethod.invoke(ssl, peerHost)
            } catch (e: Exception) {
                LogUtil.e(TAG, e, "SNI not useable")
            }
        }
        // verify hostname and certificate
        val session = ssl.session
        if (!hostnameVerifier.verify(peerHost, session)) throw SSLPeerUnverifiedException(
            "Cannot verify hostname: $peerHost"
        )
        LogUtil.i(
            TAG, "Established " + session.protocol + " connection with " + session.peerHost +
                    " using " + session.cipherSuite
        )
        return ssl
    }
}