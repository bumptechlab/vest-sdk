package code.util

import android.text.TextUtils
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

class TrueHostnameVerifier : HostnameVerifier {
    var domain: String? = null

    constructor(domain: String?) {
        this.domain = domain
    }

    constructor()

    override fun verify(hostname: String, session: SSLSession): Boolean {
        if (TextUtils.isEmpty(domain)) {
            domain = hostname
        }
        return HttpsURLConnection.getDefaultHostnameVerifier().verify(domain, session)
    }
}