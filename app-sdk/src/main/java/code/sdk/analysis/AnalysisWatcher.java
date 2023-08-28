package code.sdk.analysis;

import android.content.Context;

import java.util.List;
import java.util.Map;

import code.sdk.analysis.remote.AnalysisRequest;
import code.sdk.analysis.remote.AnalysisSourceSHF;
import code.sdk.core.util.DeviceUtil;
import code.sdk.core.util.PackageUtil;
import code.sdk.core.util.PreferenceUtil;

//统计分析: 游戏时长/最后一次登录时间/用户ID/
public class AnalysisWatcher {

    //请求接口
    private AnalysisRequest remoteRequest;

    //最后一次登录时间
    private long loginTime;
    //上次一次时长
    private long lastDuration = -1;
    //游戏是否结束
    private boolean isEndAnalysis = false;
    //游戏是否开始
    private boolean isAnalysisStart = false;

    private Context context;

    public AnalysisWatcher(Context context) {
        this.context = context;
        if (null == remoteRequest) {
            remoteRequest = new AnalysisRequest();
            String deviceId = DeviceUtil.getDeviceID();
            String packageName = PackageUtil.getPackageName();
            String channel = PackageUtil.getChannel();
            String brandCode = PackageUtil.getBrand();
            List<String> simCountryIsoList = DeviceUtil.getAllSimCountryIso(context);
            String simCountryIso = String.join(",", simCountryIsoList);
            String networkCountryCode = DeviceUtil.getNetworkCountryCode(context);

            String platform = "android";
            String referrer = PreferenceUtil.readInstallReferrer();
            remoteRequest.setDeviceId(deviceId);
            remoteRequest.setPackageName(packageName);
            remoteRequest.setChannel(channel);
            remoteRequest.setBrandCode(brandCode);
            remoteRequest.setSimCountryCode(simCountryIso);
            remoteRequest.setSysCountryCode(networkCountryCode);
            remoteRequest.setPlatform(platform);
            remoteRequest.setReferrer(referrer);
        }
    }

    //游戏开始:用户开始游戏
    public void onStart() {
        loginTime = System.currentTimeMillis();
        lastDuration = -1;
        remoteRequest.setLastLoginTime(loginTime);
        isEndAnalysis = false;
        isAnalysisStart = true;
    }

    //游戏结束:用户退出游戏,回到大厅
    public void onEnd() {
        isEndAnalysis = true;
        isAnalysisStart = false;
        //当次时长
        long currentDuration = System.currentTimeMillis() - loginTime;
        if (lastDuration != -1) {
            currentDuration += lastDuration;
            lastDuration = -1;
        }
        String language = DeviceUtil.getLanguage(context);
        remoteRequest.setGameDuration(currentDuration);
        remoteRequest.setLanguage(language);
        doAnalysis();
    }

    //游戏恢复:用户从后台将APP切到前台时触发
    public void onResume() {
        if (!isEndAnalysis && isAnalysisStart) {
            loginTime = System.currentTimeMillis();
            remoteRequest.setLastLoginTime(loginTime);
        }
    }

    //游戏暂停:用户切APP到后台时触发
    public void onStop() {
        if (!isEndAnalysis && isAnalysisStart) {
            lastDuration = System.currentTimeMillis() - loginTime;
        }
    }

    //请求失败的记录
    public void clearCache() {
        AnalysisStore store = AnalysisStore.instance(context);
        for (Map.Entry<String, AnalysisRequest> entry : store.getData().entrySet()) {
            AnalysisSourceSHF remoteSource = new AnalysisSourceSHF(context);
            remoteSource.setAnalysisCallback((code -> {
                if (code == 0) {
                    store.remove(entry.getKey());
                }
            }));
            remoteSource.fetch(entry.getValue());
        }
    }

    public void onDestroy() {
        this.context = null;
    }

    public void bindAccountInfo(String accid, long cretime) {
        if (null != remoteRequest) {
            remoteRequest.setGameId(accid);
            remoteRequest.setCreateTime(cretime);
        } else {
            ////ObfuscationStub0.inject();
        }
    }

    //上传数据
    private void doAnalysis() {
        AnalysisSourceSHF remoteSource = new AnalysisSourceSHF(context);
        remoteSource.setAnalysisCallback(this::dealAnalysisResult);
        remoteSource.fetch(remoteRequest);
    }

    //将上传失败的记录保存在本地,下次启动时候再次上传
    private void dealAnalysisResult(int code) {
        if (code != 0) {
            AnalysisStore.instance(context).saveData(remoteRequest);
        }
    }
}
