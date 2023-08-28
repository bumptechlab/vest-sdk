package code.sdk.httpdns;


import android.util.Pair;

public interface HttpDnsIpListener {

    public void onGetIp(String tag, String host, Pair<Integer, String> ip);

}
