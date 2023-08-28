package code.sdk.core.manager;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import code.sdk.core.Configuration;
import code.sdk.core.util.ConfigPreference;
import code.sdk.core.util.IOUtil;
import code.util.AESUtil;
import code.util.LogUtil;

public class ConfigurationManager {

    private static final String TAG = ConfigurationManager.class.getSimpleName();

    public static void init(Context context, String configName) {
        if (TextUtils.isEmpty(configName)) {
            LogUtil.e(TAG, "Config file is empty, init aborted");
            return;
        }
        try {
            AssetManager assets = context.getAssets();
            InputStream inputStream = assets.open(configName);
            byte[] assetsBytes = IOUtil.toByteArray(inputStream);
            String configurationJson = decryptConfig(assetsBytes);
            LogUtil.d(TAG, "configuration raw: " + configurationJson);
            Configuration configuration = Configuration.fromJson(configurationJson);
            initConfig(configuration);
        } catch (Exception e) {
            //ObfuscationStub4.inject();
            LogUtil.e(TAG, e, "Fail to parse configuration");
        }
    }


    private static String decryptConfig(byte[] assetsBytes) {
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

    private static void initConfig(Configuration configuration) {
        ConfigPreference.saveChannel(configuration.getChannel());
        ConfigPreference.saveBrand(configuration.getBrand());
        ConfigPreference.saveTargetCountry(configuration.getCountry());
        ConfigPreference.saveLighterHost(configuration.getLighterHost());
        ConfigPreference.saveSHFBaseHost(configuration.getShfBaseHost());
        ConfigPreference.saveSHFSpareHosts(configuration.getShfSpareHosts());
        ConfigPreference.saveAdjustAppId(configuration.getAdjustAppId());
        ConfigPreference.saveAdjustEventStart(configuration.getAdjustEventStart());
        ConfigPreference.saveAdjustEventGreeting(configuration.getAdjustEventGreeting());
        ConfigPreference.saveAdjustEventAccess(configuration.getAdjustEventAccess());
        ConfigPreference.saveAdjustEventUpdated(configuration.getAdjustEventUpdated());
        ConfigPreference.saveFacebookAppId(configuration.getFacebookAppId());
        ConfigPreference.saveFacebookClientToken(configuration.getFacebookClientToken());
        ConfigPreference.saveThinkingDataAppId(configuration.getThinkingDataAppId());
        ConfigPreference.saveThinkingDataHost(configuration.getThinkingDataHost());
        ConfigPreference.saveHttpDnsAuthId(configuration.getHttpdnsAuthId());
        ConfigPreference.saveHttpDnsAppId(configuration.getHttpdnsAppId());
        ConfigPreference.saveHttpDnsDesKey(configuration.getHttpdnsDesKey());
        ConfigPreference.saveHttpDnsIp(configuration.getHttpdnsIp());
    }
}
