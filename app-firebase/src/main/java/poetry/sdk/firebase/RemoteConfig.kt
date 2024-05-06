package poetry.sdk.firebase

class RemoteConfig(var f: String) {
    var l: String? = null
    var c: String? = null
    var b: String? = null
    var s: Boolean = false
    var bl: String? = null
    override fun toString(): String {
        return "RemoteConfig(f='$f', l=$l, c=$c, b=$b, s=$s, bl=$bl)"
    }

}
