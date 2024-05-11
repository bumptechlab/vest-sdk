package poetry.sdk.shf.http


/**
 *
 * @author stefan
 * @date 2024-04-29
 */
data class InterfaceStyle(
    @InterfaceStyleMode
    var mode: Int, //每次请求会随机使用一种模式
    //0:不传任何参数,使用路径生成工具生成的nonce_value;
    //1:header中添加nonce kv;
    //2:Cookie中添加nonce kv;
    //3:使用路径传参nonce kv.
    var nonce: String,
    var nonce_value: String,//随机数
    var header: HashMap<String, String?>//header参数
)
