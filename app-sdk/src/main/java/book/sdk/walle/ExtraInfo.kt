package book.sdk.walle

/**
 * 写入signing block的额外信息
 */
class ExtraInfo(private val extraInfo: Map<String, String>?) {
    val KEY_REFER_ID = "refer_id" // 线上推广员
    val KEY_AGENT_ID = "agent_id" // 线下代理
    val referID: String?
        get() = if (extraInfo == null || !extraInfo.containsKey(KEY_REFER_ID)) {
            ""
        } else extraInfo[KEY_REFER_ID]
    val agentID: String?
        get() = if (extraInfo == null || !extraInfo.containsKey(KEY_AGENT_ID)) {
            ""
        } else extraInfo[KEY_AGENT_ID]

}