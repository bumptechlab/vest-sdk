package code.sdk.analysis.remote;

import android.content.Context;

import code.sdk.core.util.ConfigPreference;

/**
 * 用于上传接口
 */
public class AnalysisSourceSHF {
    public static final String TAG = AnalysisSourceSHF.class.getSimpleName();

    public AnalysisSourceSHF(Context context) {
        AnalysisManagerSHF.init(context);
    }

    protected AnalysisCallback mAnalysisCallback;

    public void setAnalysisCallback(AnalysisCallback dotCallback) {
        this.mAnalysisCallback = dotCallback;
    }


    public void fetch(AnalysisRequest analysisRequest) {
        AnalysisManagerSHF remoteManager = AnalysisManagerSHF.getInstance();
        remoteManager.setHosts(ConfigPreference.readSHFSpareHosts());
        remoteManager.setRequest(analysisRequest);
        remoteManager.setAnalysisCallback(mAnalysisCallback);
        remoteManager.start();
    }

}
