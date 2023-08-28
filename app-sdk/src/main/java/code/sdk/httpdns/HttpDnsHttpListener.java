package code.sdk.httpdns;

import org.json.JSONObject;

public interface HttpDnsHttpListener {

    public void onResponse(String requestId, JSONObject response);
}
