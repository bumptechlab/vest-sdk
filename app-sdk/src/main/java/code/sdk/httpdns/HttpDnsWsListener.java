package code.sdk.httpdns;

import org.json.JSONObject;

public interface HttpDnsWsListener {
    public void onResponse(String requestId, JSONObject response);
}
