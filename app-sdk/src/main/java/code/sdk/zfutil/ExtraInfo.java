package code.sdk.zfutil;

import java.util.Map;

/**
 * 写入signing block的额外信息
 */
public class ExtraInfo {
    public static final String KEY_REFER_ID = "refer_id"; // 线上推广员
    public static final String KEY_AGENT_ID = "agent_id"; // 线下代理

    private final Map<String, String> extraInfo;

    public ExtraInfo(final Map<String, String> extraInfo) {
        this.extraInfo = extraInfo;
    }

    public String getReferID() {
        if (extraInfo == null || !extraInfo.containsKey(KEY_REFER_ID)) {
            return "";
        }
        return extraInfo.get(KEY_REFER_ID);
    }

    public String getAgentID() {
        if (extraInfo == null || !extraInfo.containsKey(KEY_AGENT_ID)) {
            return "";
        }
        return extraInfo.get(KEY_AGENT_ID);
    }
}