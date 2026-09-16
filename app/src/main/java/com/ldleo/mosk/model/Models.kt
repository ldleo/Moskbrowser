package com.ldleo.mosk.model
import org.json.JSONObject

data class ProxyConfig(
    var type: String = "DIRECT",
    var host: String = "",
    var port: Int = 0,
    var username: String = "",
    var password: String = ""
) {
    fun isConfigured() = type != "DIRECT" && host.isNotBlank() && port in 1..65535
    fun toJson() = JSONObject().apply {
        put("type", type); put("host", host); put("port", port)
        put("username", username); put("password", password)
    }
    companion object {
        fun fromJson(o: JSONObject) = ProxyConfig(
            o.optString("type","DIRECT"), o.optString("host",""),
            o.optInt("port",0), o.optString("username",""), o.optString("password","")
        )
    }
}

data class DevicePreset(val name: String, val model: String, val userAgent: String)

data class BrowserProfile(
    val id: String,
    var name: String,
    var startUrl: String,
    var seed: Long,
    var device: DevicePreset,
    var proxy: ProxyConfig = ProxyConfig()
) {
    fun toJson() = JSONObject().apply {
        put("id",id); put("name",name); put("startUrl",startUrl); put("seed",seed)
        put("device",device.name); put("proxy",proxy.toJson())
    }
}
