package code.sdk.httpdns;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.tencent.msdk.dns.DnsConfig;
import com.tencent.msdk.dns.HttpDnsResponseObserver;
import com.tencent.msdk.dns.MSDKDnsResolver;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import code.sdk.core.manager.ThinkingDataManager;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.PreferenceUtil;
import code.sdk.ws.WsManager;
import code.sdk.util.OkHttpUtil;
import code.sdk.core.util.URLUtilX;
import code.util.AppGlobal;
import code.util.JSONUtil;
import code.util.LogUtil;
import code.util.NumberUtil;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.ByteString;

public class HttpDnsMgr {
    private static final String TAG = HttpDnsMgr.class.getSimpleName();

    private static boolean isHttpDnsInit = false;

    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_HTTPDNS = 2;

    public static final int CONNECT_SUCCESS = 0;
    public static final int CONNECT_FAIL = 1;

    //TD sample track rate must be less than 0.1
    private static final float TRACK_RATE = 0.1f;
    private static int mTrackCount = 0;
    private static int mRequestCount = 0;


    private static int mTrackNum1 = 5;
    private static int mTrackNum2 = 9;
    private static final HashMap<String, Integer> HOSTS_PARSE_TIMEOUT_COUNT = new HashMap<String, Integer>();

    public static void init(Context context, String... hosts) {
        if (!assertOk()) return;
        String appId = ConfigPreference.readHttpDnsAppId();
        String authId = ConfigPreference.readHttpDnsAuthId();
        String desKey = ConfigPreference.readHttpDnsDesKey();
        String ip = ConfigPreference.readHttpDnsIp();
        if (TextUtils.isEmpty(appId) || TextUtils.isEmpty(authId) || TextUtils.isEmpty(desKey) || TextUtils.isEmpty(ip)) {
            return;
        }
        DnsConfig.Builder dnsConfigBuilder = new DnsConfig.Builder()
                .appId(appId)
                //（必填）dns 解析 id，即授权 id，腾讯云官网（https://console.tencentcloud.com/httpdns）申请获得，用于域名解析鉴权
                .dnsId(authId)
                //（必填）dns 解析 key，即授权 id 对应的 key（加密密钥），在申请 SDK 后的邮箱里，腾讯云官网（https://console.tencentcloud.com/httpdns）申请获得，用于域名解析鉴权
                .dnsKey(desKey)
                //（必填）Channel为desHttp()或aesHttp()时使用 119.29.29.98（默认填写这个就行），channel为https()时使用 119.29.29.99
                .dnsIp(ip)
                //（可选）channel配置：基于 HTTP 请求的 DES 加密形式，默认为 desHttp()，另有 aesHttp()、https() 可选。（注意仅当选择 https 的 channel 需要选择 119.29.29.99 的dnsip并传入token，例如：.dnsIp('119.29.29.99').https().token('....') ）。
                .desHttp()
                //（可选，选择 https channel 时进行设置）腾讯云官网（https://console.tencentcloud.com/httpdns）申请获得，用于 HTTPS 校验。仅当选用https()时进行填写
                //.token(TOKEN)
                // (可选) IP 优选，以 IpRankItem(hostname, port) 组成的 List 配置, port（可选）默认值为 8080。例如：IpRankItem("qq.com", 443)。sdk 会根据配置项进行 socket 连接测速情况对解析 IP 进行排序，IP 优选不阻塞当前解析，在下次解析时生效。当前限制为最多 10 项。
                //.ipRankItems(ipRankItemList)
                //（可选）手动指定网络栈支持情况，仅进行 IPv4 解析传 1，仅进行 IPv6 解析传 2，进行 IPv4、IPv6 双栈解析传 3。默认为根据客户端本地网络栈支持情况发起对应的解析请求。
                .setCustomNetStack(1)
                //（可选）设置是否允许使用过期缓存，默认false，解析时先取未过期的缓存结果，不满足则等待解析请求完成后返回解析结果。
                // 设置为true时，会直接返回缓存的解析结果，没有缓存则返回0;0，用户可使用localdns（InetAddress）进行兜底。且在无缓存结果或缓存已过期时，会异步发起解析请求更新缓存。因异步API（getAddrByNameAsync，getAddrsByNameAsync）逻辑在回调中始终返回未过期的解析结果，设置为true时，异步API不可使用。建议使用同步API （getAddrByName，getAddrsByName）。
                .setUseExpiredIpEnable(true)
                //（可选）设置是否启用本地缓存（Room），默认false
                .setCachedIpEnable(true)
                //（可选）设置域名解析请求超时时间，默认为1000ms
                .timeoutMills(1000)
                //（可选）【V4.4.0 废弃】 sdk日志上报能力由控制台控制，查看具体指引
                .enableReport(true);

        if (hosts.length > 0) {
            //（可选）预解析域名，填写形式："baidu.com", "qq.com"，建议不要设置太多预解析域名，当前限制为最多 10 个域名。仅在初始化时触发。
            dnsConfigBuilder.preLookupDomains(hosts);
            //（可选）解析缓存自动刷新, 以域名形式进行配置，填写形式："baidu.com", "qq.com"。配置的域名会在 TTL * 75% 时自动发起解析请求更新缓存，实现配置域名解析时始终命中缓存。此项建议不要设置太多域名，当前限制为最多 10 个域名。与预解析分开独立配置。
            dnsConfigBuilder.persistentCacheDomains(hosts);
        }
        if (LogUtil.isDebug()) {
            //（可选）日志粒度，如开启Debug打印则传入"Log.VERBOSE"
            dnsConfigBuilder.logLevel(Log.VERBOSE);
        } else {
            dnsConfigBuilder.logLevel(Log.ERROR);
        }
        MSDKDnsResolver.getInstance().init(context, dnsConfigBuilder.build());
        MSDKDnsResolver.getInstance().enablePersistentCache(true);
        isHttpDnsInit = true;
    }

    public static boolean isHttpDnsEnable() {
        return PreferenceUtil.readHttpDnsEnable() && assertOk();
    }

    public static void getAddrByNameAsync(String host, HttpDnsIpListener listener) {
        MSDKDnsResolver.getInstance().setHttpDnsResponseObserver(new HttpDnsResponseObserver() {

            @Override
            public void onHttpDnsResponse(String tag, String host, Object ipResultSemicolonSep) {
                long elapsedTime = System.currentTimeMillis() - NumberUtil.parseLong(tag);
                String ips = ipResultSemicolonSep.toString();
                String ip = parseIps(ips);
                trackHttpDnsResolveEvent(host, ip, elapsedTime);
                Pair<Integer, String> ipPair = null;
                if (TextUtils.isEmpty(ip)) {
                    ip = DeviceUtil.getINetAddress(host);
                    ipPair = new Pair<Integer, String>(TYPE_LOCAL, ip);
                    LogUtil.d(TAG, "[HttpDns] getAddrByNameAsync localDns: %s -> %s, elapsedTime: %d", host, ip, elapsedTime);
                } else {
                    ipPair = new Pair<Integer, String>(TYPE_HTTPDNS, ip);
                    LogUtil.d(TAG, "[HttpDns] getAddrByNameAsync httpDns: %s -> %s, elapsedTime: %d", host, ip, elapsedTime);
                }
                if (listener != null) {
                    listener.onGetIp(tag, host, ipPair);
                }
            }
        });
        String tag = String.valueOf(System.currentTimeMillis());
        MSDKDnsResolver.getInstance().getAddrByNameAsync(host, tag);
    }

    public static Pair<Integer, String> getAddrByName(String host) {
        String ip = getAddrFromHttpDns(host);
        Pair<Integer, String> ipPair = null;
        if (TextUtils.isEmpty(ip)) {
            long startTime = System.currentTimeMillis();
            ip = DeviceUtil.getINetAddress(host);
            long costTime = System.currentTimeMillis() - startTime;
            ipPair = new Pair<Integer, String>(TYPE_LOCAL, ip);
            LogUtil.d(TAG, "[HttpDns] parse LocalDns: %s -> %s (%dms)", host, ip, costTime);
        } else {
            ipPair = new Pair<Integer, String>(TYPE_HTTPDNS, ip);
        }
        return ipPair;
    }

    private static String getAddrFromHttpDns(String host) {
        if (TextUtils.isEmpty(host)) {
            return "";
        }
        if (!isHttpDnsInit) {
            return "";
        }
        long timeout = 500;
        long startTime = System.currentTimeMillis();
        String ips = MSDKDnsResolver.getInstance().getAddrByName(host);
        String ip = parseIps(ips);
        Integer parseTimeoutCountInteger = HOSTS_PARSE_TIMEOUT_COUNT.get(host);
        int parseTimeoutCount = parseTimeoutCountInteger == null ? 0 : parseTimeoutCountInteger;
        boolean isTimeout = false;
        if (TextUtils.isEmpty(ip) && parseTimeoutCount < 3) {
            //在500毫秒内尽量获取httpdns缓存结果(每个域名只有3次超时机会，防止HttpDns服务停止无法获取)
            while (TextUtils.isEmpty(ip)) {
                ips = MSDKDnsResolver.getInstance().getAddrByName(host);
                ip = parseIps(ips);
                if (!TextUtils.isEmpty(ip)) {
                    break;
                }
                if ((System.currentTimeMillis() - startTime) > timeout) {
                    isTimeout = true;
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            parseTimeoutCount++;
            HOSTS_PARSE_TIMEOUT_COUNT.put(host, parseTimeoutCount);
        }
        long costTime = System.currentTimeMillis() - startTime;
        if (isTimeout) {
            LogUtil.d(TAG, "[HttpDns] parse HttpDns timeout(%d): %s -> %s (%dms)", parseTimeoutCount, host, ips, costTime);
        } else {
            LogUtil.d(TAG, "[HttpDns] parse HttpDns: %s -> %s (%dms)", host, ips, costTime);
        }
        return ip;
    }

    private static String parseIps(String ips) {
        if (TextUtils.isEmpty(ips)) {
            return "";
        }
        String ip = "";
        if (ips.contains(";")) {
            ip = ips.substring(0, ips.indexOf(";"));
        } else {
            ip = ips;
        }
        if (ip.equalsIgnoreCase("0")) {
            ip = "";
        }
        return ip;
    }

    public static InputStream doHttpGetSync(String url) {
        if (!isHttpDnsInit) {
            LogUtil.w(TAG, "[HttpDns] HttpGet => HttpDns not init");
            return null;
        }
        InputStream inputStream = null;
        String oldHost = URLUtilX.parseHost(url);
        Pair<Integer, String> ipPair = getAddrByName(oldHost);
        String newHost = ipPair.second;
        String newUrl = url;
        if (!TextUtils.isEmpty(newHost)) {
            newUrl = url.replaceFirst(oldHost, newHost);
        }
        HashMap<String, String> headersMap = new HashMap<String, String>();
        headersMap.put("Host", oldHost);
        Call call = getOkHttpCall(newUrl, oldHost, "GET", null, headersMap);
        try {
            long startTime = System.currentTimeMillis();
            Response response = call.execute();
            long elapsedTime = System.currentTimeMillis() - startTime;
            int status = response.isSuccessful() ? CONNECT_SUCCESS : CONNECT_FAIL;
            if (response.isSuccessful() && response.body() != null) {
                inputStream = response.body().byteStream();
            }
            if (response.isSuccessful()) {
                LogUtil.d(TAG, "[HttpDns] HttpGet => success: %s", url);
            } else {
                LogUtil.e(TAG, "[HttpDns] HttpGet => fail(%d): %s", response.code(), url);
            }
            if (ipPair.first == TYPE_HTTPDNS) {
                trackConnectEvent(oldHost, newHost, status, elapsedTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, e, "[HttpDns] HttpGet => fail: %s", url);
        }
        return inputStream;
    }

    public static JSONObject doHttpRequestSync(
            String url,
            String method,
            byte[] body,
            JSONObject headers
    ) {
        JSONObject responseJson = new JSONObject();
        if (!isHttpDnsInit) {
            JSONUtil.putJsonValue(responseJson, "event", "onerror");
            JSONUtil.putJsonValue(responseJson, "message", "HttpDns not init");
            LogUtil.w(TAG, "[HttpDns] httpSync => HttpDns not init");
            return responseJson;
        }
        String oldHost = URLUtilX.parseHost(url);
        Pair<Integer, String> ipPair = getAddrByName(oldHost);
        String newHost = ipPair.second;
        String newUrl = url;
        if (!TextUtils.isEmpty(newHost)) {
            newUrl = url.replaceFirst(oldHost, newHost);
        }
        LogUtil.d(TAG, "[HttpDns] httpSync => request[%s]: %s", method, newUrl);
        HashMap<String, String> headersMap = new HashMap<String, String>();
        headersMap.put("Host", oldHost);
        if (headers != null) {
            Iterator<String> keys = headers.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                headersMap.put(key, headers.optString(key, ""));
            }
        }
        Call call = getOkHttpCall(newUrl, oldHost, method, body, headersMap);
        try {
            long startTime = System.currentTimeMillis();
            Response response = call.execute();
            long elapsedTime = System.currentTimeMillis() - startTime;
            int status = response.isSuccessful() ? CONNECT_SUCCESS : CONNECT_FAIL;
            if (ipPair.first == TYPE_HTTPDNS) {
                trackConnectEvent(oldHost, newHost, status, elapsedTime);
            }
            JSONUtil.putJsonValue(responseJson, "event", "onready");
            JSONUtil.putJsonValue(responseJson, "message", response.message());
            JSONUtil.putJsonValue(responseJson, "status", response.code());
            if (response.body() != null) {
                byte[] bodyBytes = response.body().bytes();
                if (bodyBytes.length > 0) {
                    JSONUtil.putJsonValue(responseJson, "body", Base64.encodeToString(bodyBytes, Base64.NO_WRAP));
                }
            }
            LogUtil.d(TAG, "[HttpDns] httpSync => response: %s", responseJson);
        } catch (SocketTimeoutException e) {
            JSONUtil.putJsonValue(responseJson, "event", "ontimeout");
            JSONUtil.putJsonValue(responseJson, "message", e.getMessage());
            LogUtil.e(TAG, e, "[HttpDns] httpSync => request timeout: %s", responseJson);
        } catch (IOException e) {
            JSONUtil.putJsonValue(responseJson, "event", "onerror");
            JSONUtil.putJsonValue(responseJson, "message", e.getMessage());
            LogUtil.e(TAG, e, "[HttpDns] httpSync => request error: %s", responseJson);
        }
        return responseJson;
    }

    public static void doHttpRequestAsync(
            String id,
            String url,
            String method,
            byte[] body,
            JSONObject headers,
            HttpDnsHttpListener callback
    ) {
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(url)) {
            LogUtil.d(TAG, "[HttpDns] httpAsync => request: id or url is empty, abort");
            return;
        }
        if (!isHttpDnsInit) {
            JSONObject responseJson = new JSONObject();
            JSONUtil.putJsonValue(responseJson, "event", "onerror");
            JSONUtil.putJsonValue(responseJson, "message", "HttpDns not init");
            LogUtil.w(TAG, "[HttpDns] httpAsync => HttpDns not init");
            if (callback != null) {
                callback.onResponse(id, responseJson);
            }
            return;
        }
        long statTime = System.currentTimeMillis();
        String oldHost = URLUtilX.parseHost(url);
        Pair<Integer, String> ipPair = getAddrByName(oldHost);
        String newHost = ipPair.second;
        String newUrl = url;
        if (!TextUtils.isEmpty(newHost)) {
            newUrl = url.replaceFirst(oldHost, newHost);
        }
        LogUtil.d(TAG, "[HttpDns] httpAsync => request[%s]: %s", method, newUrl);
        HashMap<String, String> headersMap = new HashMap<String, String>();
        headersMap.put("Host", oldHost);
        if (headers != null) {
            Iterator<String> keys = headers.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                headersMap.put(key, headers.optString(key, ""));
            }
        }
        Call call = getOkHttpCall(newUrl, oldHost, method, body, headersMap);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                long elapsedTime = System.currentTimeMillis() - statTime;
                if (ipPair.first == TYPE_HTTPDNS) {
                    trackConnectEvent(oldHost, newHost, CONNECT_FAIL, elapsedTime);
                }
                JSONObject responseJson = new JSONObject();
                try {
                    JSONUtil.putJsonValue(responseJson, "id", id);
                    if (error != null) {
                        JSONUtil.putJsonValue(responseJson, "message", error.getMessage());
                    }
                    if (error instanceof SocketTimeoutException) {
                        JSONUtil.putJsonValue(responseJson, "event", "ontimeout");
                    } else {
                        JSONUtil.putJsonValue(responseJson, "event", "onerror");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.onResponse(id, responseJson);
                }
                LogUtil.e(TAG, error, "[HttpDns] httpAsync => request error: %s", responseJson);
            }

            @Override
            public void onResponse(Call call, Response response) {
                long elapsedTime = System.currentTimeMillis() - statTime;
                int status = (response != null && response.isSuccessful()) ? CONNECT_SUCCESS : CONNECT_FAIL;
                if (ipPair.first == TYPE_HTTPDNS) {
                    trackConnectEvent(oldHost, newHost, status, elapsedTime);
                }
                JSONObject responseJson = new JSONObject();
                try {
                    JSONUtil.putJsonValue(responseJson, "id", id);
                    JSONUtil.putJsonValue(responseJson, "event", "onready");
                    JSONUtil.putJsonValue(responseJson, "message", response.message());
                    JSONUtil.putJsonValue(responseJson, "status", response.code());
                    if (response != null && response.body() != null) {
                        byte[] bodyBytes = response.body().bytes();
                        if (bodyBytes != null && bodyBytes.length > 0) {
                            JSONUtil.putJsonValue(responseJson, "body", Base64.encodeToString(bodyBytes, Base64.NO_WRAP));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.onResponse(id, responseJson);
                }
                LogUtil.d(TAG, "[HttpDns] httpAsync => response: %s", responseJson);
            }

        });
    }

    private static HashMap<String, WsManager> mWsHashMap = new HashMap<String, WsManager>();

    public static void doWsOpen(String id, String url, JSONObject headers, HttpDnsWsListener callback) {
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(url)) {
            LogUtil.w(TAG, "[HttpDns] socket[%s] => open[%s]: id or url is empty, aborted", id, url);
            return;
        }
        if (!isHttpDnsInit) {
            JSONObject responseJson = new JSONObject();
            JSONUtil.putJsonValue(responseJson, "event", "onerror");
            JSONUtil.putJsonValue(responseJson, "message", "HttpDns not init");
            LogUtil.w(TAG, "[HttpDns] socket[%s] => HttpDns not init", id);
            if (callback != null) {
                callback.onResponse(id, responseJson);
            }
            return;
        }
        WsManager wsManagerCache = mWsHashMap.get(id);
        if (wsManagerCache != null && wsManagerCache.isWsConnected()) {
            LogUtil.w(TAG, "[HttpDns] socket[%s] => open[%s]: socket already open, aborted", id, url);
            return;
        }
        long startTime = System.currentTimeMillis();
        String oldHost = URLUtilX.parseHost(url);
        Pair<Integer, String> ipPair = getAddrByName(oldHost);
        String newHost = ipPair.second;
        String newUrl = url;
        if (!TextUtils.isEmpty(newHost)) {
            newUrl = url.replaceFirst(oldHost, newHost);
        }
        HashMap<String, String> headersMap = new HashMap<String, String>();
        headersMap.put("Host", oldHost);
        if (headers != null) {
            Iterator<String> keys = headers.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                headersMap.put(key, headers.optString(key, ""));
            }
        }
        LogUtil.d(TAG, "[HttpDns] socket[%s] => open[%s->%s]: %s, header: %s", id, oldHost, newHost, newUrl, headersMap);
        WsManager wsManager = new WsManager.Builder(AppGlobal.getApplication())
                .socketId(id)
                .wsUrl(newUrl)
                .host(oldHost)
                .headers(headersMap)
                .needReconnect(false)
                .build();
        WsManager.WsStatusListener wsStatusListener = new WsManager.WsStatusListener() {

            @Override
            public void onOpen(Response response) {
                super.onOpen(response);
                mWsHashMap.put(id, wsManager);
                JSONObject responseJson = new JSONObject();
                try {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    int status = (response != null && response.isSuccessful()) ? CONNECT_SUCCESS : CONNECT_FAIL;
                    if (ipPair.first == TYPE_HTTPDNS) {
                        trackConnectEvent(oldHost, newHost, status, elapsedTime);
                    }
                    JSONUtil.putJsonValue(responseJson, "id", id);
                    JSONUtil.putJsonValue(responseJson, "event", "onopen");
                    if (response != null) {
                        JSONUtil.putJsonValue(responseJson, "message", response.message());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.onResponse(id, responseJson);
                }
                LogUtil.d(TAG, "[HttpDns] socket[%s] => onOpen[%s->%s]: %s", id, oldHost, newHost, responseJson);
            }

            @Override
            public void onClosed(int code, String reason) {
                super.onClosed(code, reason);
                mWsHashMap.remove(id);
                JSONObject responseJson = new JSONObject();
                JSONUtil.putJsonValue(responseJson, "id", id);
                JSONUtil.putJsonValue(responseJson, "event", "onclose");
                JSONUtil.putJsonValue(responseJson, "message", reason);
                if (callback != null) {
                    callback.onResponse(id, responseJson);
                }
                LogUtil.d(TAG, "[HttpDns] socket[%s] => onClosed[%s->%s]: %s", id, oldHost, newHost, responseJson);
            }

            @Override
            public void onClosing(int code, String reason) {
                super.onClosing(code, reason);
                mWsHashMap.remove(id);
                LogUtil.d(TAG, "[HttpDns] socket[%s] => onClosing[%s->%s]: code=%d, reason=%s", id, oldHost, newHost, code, reason);
            }

            @Override
            public void onFailure(Throwable t, Response response) {
                super.onFailure(t, response);
                if (t instanceof EOFException) {
                    return;
                }
                mWsHashMap.remove(id);
                JSONObject responseJson = new JSONObject();
                try {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (ipPair.first == TYPE_HTTPDNS) {
                        trackConnectEvent(oldHost, newHost, CONNECT_FAIL, elapsedTime);
                    }
                    JSONUtil.putJsonValue(responseJson, "id", id);
                    if (t instanceof SocketTimeoutException) {
                        JSONUtil.putJsonValue(responseJson, "event", "ontimeout");
                    } else {
                        JSONUtil.putJsonValue(responseJson, "event", "onerror");
                    }
                    if (response != null) {
                        JSONUtil.putJsonValue(responseJson, "message", response.message());
                    }
                    if (t != null) {
                        JSONUtil.putJsonValue(responseJson, "error", t.getMessage());
                    }
                    if (response != null && response.body() != null) {
                        byte[] bodyBytes = response.body().bytes();
                        if (bodyBytes != null && bodyBytes.length > 0) {
                            JSONUtil.putJsonValue(responseJson, "body", Base64.encodeToString(bodyBytes, Base64.NO_WRAP));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (callback != null) {
                    callback.onResponse(id, responseJson);
                }
                LogUtil.e(TAG, t, "[HttpDns] socket[%s] => onFailure[%s->%s]: %s", id, oldHost, newHost, responseJson);
            }

            @Override
            public void onMessage(ByteString bytes) {
                super.onMessage(bytes);

                if (callback != null) {
                    callback.onResponse(id, getResponseJson(bytes,id));
                }
            }

            @Override
            public void onReconnect() {
                super.onReconnect();
                LogUtil.d(TAG, "[HttpDns] socket[%s] => onReconnect[%s->%s]", id, oldHost, newHost);
            }
        };
        wsManager.setWsStatusListener(wsStatusListener);
        wsManager.startConnect();
    }

    private static JSONObject getResponseJson(ByteString bytes,String id) {
        JSONObject responseJson = new JSONObject();
        try {
            JSONUtil.putJsonValue(responseJson, "id", id);
            JSONUtil.putJsonValue(responseJson, "event", "onmessage");
            byte[] bodyBytes = bytes.toByteArray();
            if (bodyBytes != null) {
                JSONUtil.putJsonValue(responseJson, "body", Base64.encodeToString(bodyBytes, Base64.NO_WRAP));
            }
            LogUtil.d(TAG, "[HttpDns] mTrackNum1[" + mTrackNum1 +"] => mTrackNum2" + mTrackNum2 + "[%s->%s]");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseJson;
    }

    public static JSONObject doWsSend(String id, byte[] body) {
        JSONObject responseJson = new JSONObject();
        if (TextUtils.isEmpty(id)) {
            JSONUtil.putJsonValue(responseJson, "success", false);
            LogUtil.w(TAG, "[HttpDns] socket[%s] => send: id is empty, aborted", id);
            return responseJson;
        }
        if (body == null || body.length == 0) {
            JSONUtil.putJsonValue(responseJson, "success", false);
            LogUtil.w(TAG, "[HttpDns] socket[%s] => send: body is empty, aborted", id);
            return responseJson;
        }
        WsManager wsManager = mWsHashMap.get(id);
        if (wsManager == null || !wsManager.isWsConnected()) {
            JSONUtil.putJsonValue(responseJson, "success", false);
            LogUtil.w(TAG, "[HttpDns] socket[%s] => send: socket is not connected, aborted", id);
            return responseJson;
        }
        ByteString bodyByteString = ByteString.of(body, 0, body.length);
        boolean success = wsManager.sendMessage(bodyByteString);
        LogUtil.d(TAG, "[HttpDns] socket[%s] => send %s: %s", id, success ? "success" : "fail", Arrays.toString(body));
        JSONUtil.putJsonValue(responseJson, "success", success);
        return responseJson;
    }

    public static void doWsClose(String id) {
        if (TextUtils.isEmpty(id)) {
            LogUtil.w(TAG, "[HttpDns] socket[%s] => close: id is empty, aborted", id);
            return;
        }
        WsManager wsManager = mWsHashMap.get(id);
        if (wsManager == null || !wsManager.isWsConnected()) {
            LogUtil.w(TAG, "[HttpDns] socket[%s] => close: socket is not connected, aborted", id);
            return;
        }
        wsManager.stopConnect();
    }

    public static JSONObject isWsConnected(String id) {
        JSONObject responseJson = new JSONObject();
        if (TextUtils.isEmpty(id)) {
            LogUtil.w(TAG, "[HttpDns] socket[%s] => isWsConnected: id is empty, aborted", id);
            JSONUtil.putJsonValue(responseJson, "connected", false);
            return responseJson;
        }
        WsManager wsManager = mWsHashMap.get(id);
        if (wsManager == null) {
            LogUtil.w(TAG, "[HttpDns] socket[%s] => isWsConnected: socket not open, aborted", id);
            JSONUtil.putJsonValue(responseJson, "connected", false);
            return responseJson;
        }
        JSONUtil.putJsonValue(responseJson, "connected", wsManager.isWsConnected());
        return responseJson;
    }

    private static Call getOkHttpCall(
            String url,
            String domain,
            String method,
            byte[] body,
            HashMap<String, String> headers
    ) {
        Request.Builder requestBuilder = new Request.Builder();
        addHeader(requestBuilder, headers);
        Request request = null;
        if ("POST".equalsIgnoreCase(method)) {
            RequestBody requestBody = buildRequestBody(body);
            request = requestBuilder.url(url).post(requestBody).build();
        } else {
            request = requestBuilder.url(url).get().build();
        }
        OkHttpClient okHttpClient = OkHttpUtil.getOkHttpClient(domain);
        return okHttpClient.newCall(request);
    }

    private static void addHeader(Request.Builder builder, HashMap<String, String> headers) {
        if (headers != null) {
            Iterator<String> keys = headers.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = headers.get(key);
                if (!TextUtils.isEmpty(value)) {
                    builder.addHeader(key, value);
                }
            }
        }
    }

    private static RequestBody buildRequestBody(byte[] body) {
        RequestBody requestBody = null;
        if (null != body) {
            MediaType mediaType = MediaType.parse("application/x-protobuf");
            requestBody = RequestBody.create(mediaType, body);
        }
        return requestBody;
    }

    private static void trackHttpDnsResolveEvent(String host, String ip, long duration) {
        JSONArray ipList = new JSONArray();
        ipList.put(ip);
        JSONObject trackJson = new JSONObject();
        JSONUtil.putJsonValue(trackJson, "domain", host);
        JSONUtil.putJsonValue(trackJson, "ips", ipList);
        JSONUtil.putJsonValue(trackJson, "duration", duration);
        ThinkingDataManager.trackEvent("SysNetwork_ResolveDNS", trackJson);
    }

    /**
     * 因为网络事件太多，需要采样上报，API上报率在0.1左右
     */
    private static void trackConnectEvent(String host, String ip, int status, long duration) {
        mRequestCount++;
        if (mRequestCount > 0 && mTrackCount > 0) {
            float mCurTrackRate = mTrackCount * 1.0f / mRequestCount;
            if (mCurTrackRate > TRACK_RATE) {
                LogUtil.d(TAG, "[HttpDns] track connectEvent aborted: trackCount=%d, requestCount=%d, rate=%f", mTrackCount, mRequestCount, mCurTrackRate);
                return;
            }
        }

        ThinkingDataManager.trackEvent("SysNetwork_Connect", getConnectTrackJson(host,ip,status,duration));
        mTrackCount++;
    }

    private static JSONObject getConnectTrackJson(String host, String ip, int status, long duration) {
        JSONObject trackJson = new JSONObject();
        JSONUtil.putJsonValue(trackJson, "domain", host);
        JSONUtil.putJsonValue(trackJson, "ip", ip);
        JSONUtil.putJsonValue(trackJson, "status", status);
        JSONUtil.putJsonValue(trackJson, "duration", duration);
        return trackJson;
    }


    /**
     * 通过反射断言，引用方是否引入了HttpDns
     * 如果没引用则不使用HttpDns
     *
     * @return
     */
    private static boolean assertOk() {
        try {
            Class oneClz = Class.forName("com.tencent.msdk.dns.DnsConfig");
            return true;
        } catch (Exception e) {
            //LogUtil.w(TAG, e, "[HttpDns]assert fail");
        }
        return false;
    }
}