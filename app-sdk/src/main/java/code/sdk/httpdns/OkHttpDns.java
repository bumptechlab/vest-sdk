package code.sdk.httpdns;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Dns;

public class OkHttpDns implements Dns {

    public static final String TAG = OkHttpDns.class.getSimpleName();

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null) {
            throw new UnknownHostException("hostname == null");
        } else {
            try {
                List<InetAddress> mInetAddressesList = new ArrayList<>();
                String httpDnsIp = HttpDnsMgr.getAddrByName(hostname).second;
                InetAddress httpDnsAddress = InetAddress.getByName(httpDnsIp);
                mInetAddressesList.add(httpDnsAddress);
                return mInetAddressesList;
            } catch (NullPointerException var4) {
                UnknownHostException unknownHostException = new UnknownHostException("Broken system behaviour");
                unknownHostException.initCause(var4);
                throw unknownHostException;
            }
        }
    }

}

