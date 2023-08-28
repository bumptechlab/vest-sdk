package code.util;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.text.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TlsSniSocketFactory extends SSLSocketFactory {
    private final String TAG = TlsSniSocketFactory.class.getSimpleName();
    HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
    String peerHost;

    public TlsSniSocketFactory(String peerHost) {
        this.peerHost = peerHost;
    }

    public TlsSniSocketFactory() {
    }

    @Override
    public Socket createSocket() {
        return null;
    }

    @Override
    public Socket createSocket(String host, int port) {
        return null;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
        return null;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) {
        return null;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
        return null;
    }

    // TLS layer
    @Override
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }

    @Override
    public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException {
        if (TextUtils.isEmpty(peerHost)) {
            peerHost = host;
        }
        LogUtil.i(TAG, "customized createSocket. host: " + peerHost);
        InetAddress address = plainSocket.getInetAddress();
        if (autoClose) {
            // we don't need the plainSocket
            plainSocket.close();
        }
        // create and connect SSL socket, but don't do hostname/certificate verification yet
        SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
        SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(address, port);
        // enable TLSv1.1/1.2 if available
        ssl.setEnabledProtocols(ssl.getSupportedProtocols());
        // set up SNI before the handshake
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            LogUtil.i(TAG, "Setting SNI hostname");
            sslSocketFactory.setHostname(ssl, peerHost);
        } else {
            LogUtil.d(TAG, "No documented SNI support on Android <4.2, trying with reflection");
            try {
                java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
                setHostnameMethod.invoke(ssl, peerHost);
            } catch (Exception e) {
                LogUtil.e(TAG, e, "SNI not useable");
            }
        }
        // verify hostname and certificate
        SSLSession session = ssl.getSession();
        if (!hostnameVerifier.verify(peerHost, session))
            throw new SSLPeerUnverifiedException("Cannot verify hostname: " + peerHost);
        LogUtil.i(TAG, "Established " + session.getProtocol() + " connection with " + session.getPeerHost() +
                " using " + session.getCipherSuite());
        return ssl;
    }
}
