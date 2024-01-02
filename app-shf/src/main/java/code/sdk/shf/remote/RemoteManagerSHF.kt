package code.sdk.shf.remote

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.webkit.URLUtil
import code.sdk.core.util.ConfigPreference
import code.sdk.core.util.DeviceUtil
import code.sdk.core.util.PackageUtil
import code.sdk.core.util.PreferenceUtil
import code.sdk.core.util.URLUtilX
import code.sdk.shf.http.BaseResponse
import code.sdk.shf.http.HttpClient
import code.util.AES
import code.util.AES.enc
import code.util.AES.encryptByGCM
import code.util.AESKeyStore.getIvParams
import code.util.LogUtil.d
import code.util.LogUtil.e
import code.util.NetworkUtil.isConnected
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.IOException

class RemoteManagerSHF {
    private lateinit var mContext: Context
    private var mRemoteCallback: RemoteCallback? = null
    private var isRequesting = false

    companion object {
       private val TAG = RemoteManagerSHF::class.java.simpleName
        private const val RETRY_TOTAL_COUNT = 3

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

    fun start(baseHost: String, spareHosts: Array<String?>?) {
        if (isRequesting) {
            d(TAG, "[SHF] is requesting, could not proceed another request")
            return
        }
        val hosts = initializeHosts(baseHost, spareHosts)
        if (hosts.isEmpty()) {
            handleError("[SHF] there are no valid hosts, abort requesting")
            return
        }
        doRequest(hosts, 0, 0)
    }

    private fun initializeHosts(baseHost: String, spareHosts: Array<String?>?): List<String> {
        val hosts: MutableList<String> = ArrayList()
        if (isHostValid(baseHost)) {
            hosts.add(baseHost)
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
        e(TAG, "[SHF] encounter an error: $errorMessage")
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
            var shfDispatcher = ConfigPreference.readShfDispatcher()
            if (TextUtils.isEmpty(shfDispatcher)) {
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
            handleError("[SHF] request all failed, please check your hosts")
            return
        }
        val host = hosts[hostIndex]
        val remoteRequest = buildRemoteRequest()
        val requestJson = remoteRequest.toJson()
        val bytes = encryptByGCM(requestJson.toByteArray(), AES.MODE256)
        if (bytes == null) {
            handleError("[SHF] request all failed, errors happen while encrypting request body")
            return
        }
        val mediaType = "application/octet-stream".toMediaType()
        val requestBody = bytes.toRequestBody(mediaType, 0, bytes.size)
        val query: MutableMap<String, String> = HashMap()
        query["enc"] = enc()
        query["nonce"] = getIvParams()
        val instance = HttpClient.mInstance
        val url = instance.buildUrl(host, shfDispatcher, query)
        d(TAG, "[SHF] URL[%s] request start: %s", url, requestJson)
        instance.api.getGameInfo(url, requestBody)
            .compose(instance.ioSchedulers())
            .subscribe(object : Observer<ResponseBody> {
                override fun onSubscribe(d: Disposable) {}
                override fun onNext(data: ResponseBody) {
                    try {
                        val remoteConfig = RemoteConfig()
                        val result = data.string()
                        val response = BaseResponse<RemoteConfig>().fromJson(result, remoteConfig)
                        d(TAG, "[SHF] URL[%s] request success: %s", url, result)
                        isRequesting = false
                        if (mRemoteCallback != null) {
                            mRemoteCallback!!.onResult(true, response.data)
                        }
                    } catch (e: IOException) {
                        onError(e)
                    }
                }

                override fun onError(e: Throwable) {
                    e(
                        TAG, e, "[SHF] URL[%s] request failed: retry=%d, message=%s",
                        url, retryCount, e.message
                    )
                    if (isConnected(mContext)) {
                        if (retryCount == RETRY_TOTAL_COUNT) {
                            val domain = URLUtilX.parseHost(host)
                            if (!DeviceUtil.isDomainAvailable(domain)) {
                                setHostValid(host, false)
                                e(TAG, "[SHF] Host[%s] is not available", host)
                            } else {
                                d(TAG, "[SHF] Host[%s] is available", host)
                            }
                        }
                    }
                    retryRequest(hosts, hostIndex, retryCount)
                }

                override fun onComplete() {}
            })
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
            val domain = URLUtilX.parseHost(url)
            isValid = PreferenceUtil.isDomainValid(domain)
        } else {
            e(TAG, "[SHF] isHostValid, Host[%s] is invalid", url)
        }
        return isValid
    }

    private fun setHostValid(url: String, isValid: Boolean) {
        if (URLUtil.isValidUrl(url)) {
            val domain = URLUtilX.parseHost(url)
            PreferenceUtil.saveDomainValid(domain, isValid)
        } else {
            e(TAG, "[SHF] setUrlValid, Host[%s] is invalid", url)
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
        val apiVersion = 2 //api版本
        val rKey = 1 //是否加密返回字段
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
        remoteRequest.deviceInfo = ""
        return remoteRequest
    }


}