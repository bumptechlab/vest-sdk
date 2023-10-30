package code.sdk.core.manager;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import code.sdk.core.Configuration;
import code.sdk.core.util.ConfigPreference;
import code.util.IOUtil;
import code.util.AESUtil;
import code.util.LogUtil;

public class ConfigurationManager {

    private static final String TAG = ConfigurationManager.class.getSimpleName();

    private static volatile ConfigurationManager INSTANCE;

    private ConfigurationManager() {

    }

    public static ConfigurationManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ConfigurationManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConfigurationManager();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context, String configName) {
        if (TextUtils.isEmpty(configName)) {
            LogUtil.e(TAG, "Config file is empty, init aborted");
            return;
        }
        try {
            AssetManager assets = context.getAssets();
            InputStream inputStream = assets.open(configName);
            byte[] assetsBytes = IOUtil.toByteArray(inputStream);
            configByBytes(assetsBytes);
        } catch (Exception e) {
            //ObfuscationStub4.inject();
            LogUtil.e(TAG, e, "Fail to parse configuration");
        }
    }

    private void configByBytes(byte[] assetsBytes) {
        String configurationJson = decryptConfig(assetsBytes);
        LogUtil.d(TAG, "configuration raw: " + configurationJson);
        Configuration configuration = Configuration.fromJson(configurationJson);
        initConfig(configuration);
    }


    private String decryptConfig(byte[] assetsBytes) {
        //提取AES密钥和密文
        byte[] keyBytes = new byte[44];
        byte[] bodyBytes = new byte[assetsBytes.length - keyBytes.length];
        System.arraycopy(assetsBytes, 0, keyBytes, 0, keyBytes.length);
        System.arraycopy(assetsBytes, keyBytes.length, bodyBytes, 0, bodyBytes.length);

        String aesKey = new String(keyBytes, StandardCharsets.UTF_8);
        byte[] decryptBytes = AESUtil.decrypt(bodyBytes, aesKey);
        String configurationJson = new String(decryptBytes);
        return configurationJson;
    }

    private void initConfig(Configuration configuration) {
        ConfigPreference.saveChannel(configuration.getChannel());
        ConfigPreference.saveBrand(configuration.getBrand());
        ConfigPreference.saveTargetCountry(configuration.getCountry());
        ConfigPreference.saveSHFBaseHost(configuration.getShfBaseHost());
        ConfigPreference.saveSHFSpareHosts(configuration.getShfSpareHosts());
        ConfigPreference.saveShfDispatcher(configuration.getShfDispatcher());
        ConfigPreference.saveAdjustAppId(configuration.getAdjustAppId());
        ConfigPreference.saveAdjustEventStart(configuration.getAdjustEventStart());
        ConfigPreference.saveAdjustEventGreeting(configuration.getAdjustEventGreeting());
        ConfigPreference.saveAdjustEventAccess(configuration.getAdjustEventAccess());
        ConfigPreference.saveAdjustEventUpdated(configuration.getAdjustEventUpdated());
        ConfigPreference.saveThinkingDataAppId(configuration.getThinkingDataAppId());
        ConfigPreference.saveThinkingDataHost(configuration.getThinkingDataHost());
    }
}
