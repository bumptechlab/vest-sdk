package poetry.sdk.shf.remote

import android.content.Context
import android.os.Build
import android.webkit.URLUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import poetry.sdk.core.VestReleaseMode
import poetry.sdk.core.util.ConfigPreference
import poetry.sdk.core.util.DeviceUtil
import poetry.sdk.core.util.PackageUtil
import poetry.sdk.core.util.PreferenceUtil
import poetry.sdk.shf.http.Api
import poetry.sdk.shf.http.BaseResponse
import poetry.sdk.shf.http.HttpClient
import poetry.sdk.shf.http.InterfaceStyle
import poetry.sdk.shf.http.InterfaceStyleMode
import poetry.sdk.shf.http.MODE_COOKIE
import poetry.sdk.shf.http.MODE_HEADER
import poetry.sdk.shf.http.MODE_NON
import poetry.sdk.shf.http.MODE_PATH
import poetry.util.AES
import poetry.util.LogUtil
import poetry.util.NetworkUtil
import poetry.util.parseHost
import java.io.IOException
import java.util.Random

class RemoteManagerSHF {
    private lateinit var mContext: Context
    private var mRemoteCallback: RemoteCallback? = null
    private var isRequesting = false

    companion object {
        private val TAG = RemoteManagerSHF::class.java.simpleName
        private const val RETRY_TOTAL_COUNT = 3
        const val SHF_API_ENCRYPT = true
        const val SHF_API_VERSION = 4

        @Synchronized
        fun init(context: Context): RemoteManagerSHF {
            val instance = InstanceHolder.INSTANCE
            instance.mContext = context
            return instance
        }

        @Synchronized
        fun getInstance() = InstanceHolder.INSTANCE
    }

    private object InstanceHolder {
        val INSTANCE = RemoteManagerSHF()
    }

    fun setRemoteCallback(remoteCallback: RemoteCallback?) {
        mRemoteCallback = remoteCallback
    }

    fun start(baseHost: String?, spareHosts: Array<String?>?) {
        if (isRequesting) {
            LogUtil.d(TAG, "[Vest-SHF] is requesting, could not proceed another request")
            return
        }
        val hosts = initializeHosts(baseHost, spareHosts)
        if (hosts.isEmpty()) {
            handleError("[Vest-SHF] there are no valid hosts, abort requesting")
            return
        }
        doRequest(hosts, 0, 0)
    }

    private fun initializeHosts(baseHost: String?, spareHosts: Array<String?>?): List<String> {
        val hosts: MutableList<String> = ArrayList()
        if (isHostValid(baseHost)) {
            hosts.add(baseHost!!)
        }
        if (spareHosts != null) {
            for (spareHost in spareHosts) {
                if (isHostValid(spareHost)) {
                    hosts.add(spareHost!!)
                }
            }
        }
        return hosts
    }

    private fun handleError(errorMessage: String) {
        LogUtil.e(TAG, "[Vest-SHF] encounter an error: $errorMessage")
        isRequesting = false
        mRemoteCallback?.onResult(false, null)

    }

    /**
     * 保证API不以/开头，避免构建url时重复出现/
     *
     * @return
     */
    private val shfDispatcher: String
        get() {
            var shfDispatcher = ConfigPreference.readInterfaceDispatcher()
            if (shfDispatcher.isNullOrEmpty()) {
                shfDispatcher = "api/v1/dispatcher"
            }
            if (shfDispatcher.startsWith("/")) {
                shfDispatcher = shfDispatcher.replaceFirst("/".toRegex(), "")
            }
            return shfDispatcher
        }

    private fun doRequest(hosts: List<String>, hostIndex: Int, retryCount: Int) {
        isRequesting = true
        if (hostIndex >= hosts.size || retryCount > RETRY_TOTAL_COUNT) {
            handleError("[Vest-SHF] request all failed, please check your hosts")
            return
        }
        val host = hosts[hostIndex]
        val remoteRequest = buildRemoteRequest()
        val requestJson = remoteRequest.toJson()

        val interfaceStyle = genSHFInterfaceStyle()

        val bytes = AES.encryptByGCM(
            requestJson.toByteArray(),
            ConfigPreference.readInterfaceEncValue()!!,
            interfaceStyle.nonce_value
        )
        if (bytes == null) {
            handleError("[Vest-SHF] request all failed, errors happen while encrypting request body")
            return
        }
        val mediaType = "application/octet-stream".toMediaType()
        val requestBody = bytes.toRequestBody(mediaType, 0, bytes.size)
        val query: MutableMap<String, String> = HashMap()
        if (interfaceStyle.mode == MODE_PATH) {
            query[interfaceStyle.nonce] = interfaceStyle.nonce_value
        }
        val instance = HttpClient.mInstance
        val url = instance.buildUrl(host, shfDispatcher, query)
        LogUtil.d(TAG, "[Vest-SHF] URL[%s] request start: %s", url, requestJson)
        instance.api?.getGameInfo(url, requestBody, interfaceStyle.header)
            ?.compose(instance.ioSchedulers())?.subscribe(object : Observer<ResponseBody> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(data: ResponseBody) {
                    try {
                        val remoteConfig = RemoteConfig()
                        val result = data.string()
                        val response = BaseResponse<RemoteConfig>().fromJson(result, remoteConfig)
                        LogUtil.d(TAG, "[Vest-SHF] URL[%s] request success: %s", url, result)
                        isRequesting = false
                        if (mRemoteCallback != null) {
                            mRemoteCallback!!.onResult(true, response.data)
                        }
                    } catch (e: IOException) {
                        onError(e)
                    }
                }

                override fun onError(e: Throwable) {
                    LogUtil.e(
                        TAG,
                        e,
                        "[Vest-SHF] URL[%s] request failed: retry=%d, message=%s",
                        url,
                        retryCount,
                        e.message
                    )
                    if (NetworkUtil.isConnected(mContext)) {
                        if (retryCount == RETRY_TOTAL_COUNT) {
                            val domain = host.parseHost()
                            CoroutineScope(Dispatchers.IO).launch {
                                if (!DeviceUtil.isDomainAvailable(domain)) {
                                    setHostValid(host, false)
                                    LogUtil.e(TAG, "[Vest-SHF] Host[%s] is not available", host)
                                } else {
                                    LogUtil.d(TAG, "[Vest-SHF] Host[%s] is available", host)
                                }
                            }

                        }
                    }
                    retryRequest(hosts, hostIndex, retryCount)
                }

                override fun onComplete() {}
            })
    }

    private fun Api.getGameInfo(
        url: String, requestBody: RequestBody, header: HashMap<String, String?>
    ): Observable<ResponseBody> {
        return when (Random().nextInt(3)) {
            0 -> patchGameInfo(url, requestBody, header)
            1 -> putGameInfo(url, requestBody, header)
            else -> postGameInfo(url, requestBody, header)
        }
    }

    private fun genSHFInterfaceStyle(): InterfaceStyle {
        val map = java.util.HashMap<String, String?>()
        @InterfaceStyleMode val mode = Random().nextInt(4)
        val nonce: String = ConfigPreference.readInterfaceNonce()
        var nonceValue: String? = null
        when (mode) {
            MODE_NON -> nonceValue = ConfigPreference.readInterfaceNonceValue()

            MODE_HEADER -> {
                nonceValue = getRandomNonce()
                map[nonce] = nonceValue
            }

            MODE_COOKIE -> {
                nonceValue = getRandomNonce()
                map["Cookie"] = "$nonce=$nonceValue"
            }

            MODE_PATH -> {
                nonceValue = getRandomNonce()
            }
        }
        return InterfaceStyle(mode, nonce, nonceValue!!, map)
    }

    fun getRandomNonce(): String? {
        val characters = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = Random()
        val sb = StringBuilder()
        for (i in 0..11) {
            sb.append(characters[random.nextInt(characters.length)])
        }
        return sb.toString()
    }

    private fun retryRequest(hosts: List<String>, hostIndex: Int, retryCount: Int) {
        if (retryCount < RETRY_TOTAL_COUNT) {
            // Retry current URL
            doRequest(hosts, hostIndex, retryCount + 1)
        } else {
            // Move to the next URL
            doRequest(hosts, hostIndex + 1, 0)
        }
    }

    private fun isHostValid(url: String?): Boolean {
        var isValid = false
        if (URLUtil.isValidUrl(url)) {
            val domain = url.parseHost()
            isValid = PreferenceUtil.isDomainValid(domain)
        } else {
            LogUtil.e(TAG, "[Vest-SHF] isHostValid, Host[%s] is invalid", url)
        }
        return isValid
    }

    private fun setHostValid(url: String, isValid: Boolean) {
        if (URLUtil.isValidUrl(url)) {
            val domain = url.parseHost()
            PreferenceUtil.saveDomainValid(domain, isValid)
        } else {
            LogUtil.e(TAG, "[Vest-SHF] setUrlValid, Host[%s] is invalid", url)
        }
    }

    private fun buildRemoteRequest(): RemoteRequest {
        val type = "h5"
        val deviceId = DeviceUtil.getDeviceID()
        val packageName = PackageUtil.getPackageName()
        val channel = PackageUtil.getChannel()
        val parentBrd = PackageUtil.getParentBrand()
        val cvc = PackageUtil.getPackageVersionCode()
        val cvn = PackageUtil.getPackageVersionName()
        val svc = Build.VERSION.SDK_INT
        val svn = Build.VERSION.RELEASE
        val simCountryIsoList = DeviceUtil.getAllSimCountryIso(mContext)
        val simCountryIso = java.lang.String.join(",", simCountryIsoList)
        val networkCountryCode = DeviceUtil.getNetworkCountryCode(mContext)
        val language = DeviceUtil.getLanguage(mContext)
        val platform = "android"
        val referrer = PreferenceUtil.readInstallReferrer()
        val apiVersion = SHF_API_VERSION //api版本
        val rKey = if (SHF_API_ENCRYPT) 1 else 0 //是否加密返回字段
        val appType = when (ConfigPreference.readReleaseMode()) {
            VestReleaseMode.MODE_VEST.mode -> "apk"
            VestReleaseMode.MODE_CHANNEL.mode -> "landing"
            else -> ""
        }
        val remoteRequest = RemoteRequest()
        remoteRequest.version = apiVersion
        remoteRequest.type = type
        remoteRequest.deviceId = deviceId
        remoteRequest.packageName = packageName
        remoteRequest.channel = channel
        remoteRequest.parentBrd = parentBrd
        remoteRequest.versionCode = cvc
        remoteRequest.versionName = cvn
        remoteRequest.sysVersionCode = svc
        remoteRequest.sysVersionName = svn
        remoteRequest.simCountryCode = simCountryIso
        remoteRequest.sysCountryCode = networkCountryCode
        remoteRequest.language = language
        remoteRequest.platform = platform
        remoteRequest.referrer = referrer
        remoteRequest.rkey = rKey
        remoteRequest.appType = appType
        return remoteRequest
    }


}