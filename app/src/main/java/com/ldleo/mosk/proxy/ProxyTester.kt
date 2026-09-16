package com.ldleo.mosk.proxy
import com.ldleo.mosk.model.ProxyConfig
import org.json.JSONObject
import java.net.*
import java.util.concurrent.Executors

data class ProxyTestResult(val ip:String,val country:String)

class ProxyTester {
    private val executor=Executors.newSingleThreadExecutor()
    fun test(p:ProxyConfig, callback:(Result<ProxyTestResult>)->Unit) {
        executor.execute {
            try {
                require(p.isConfigured()) { "Configura servidor y puerto" }
                val pt=if(p.type.startsWith("SOCKS")) Proxy.Type.SOCKS else Proxy.Type.HTTP
                val proxy=Proxy(pt,InetSocketAddress(p.host,p.port))
                val c=URL("http://ip-api.com/json/?fields=query,country")
                    .openConnection(proxy) as HttpURLConnection
                c.connectTimeout=7000; c.readTimeout=7000
                if(p.username.isNotBlank() && !p.type.startsWith("SOCKS")) {
                    val token=android.util.Base64.encodeToString(
                        "${p.username}:${p.password}".toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    c.setRequestProperty("Proxy-Authorization","Basic $token")
                }
                val j=JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                c.disconnect()
                callback(Result.success(ProxyTestResult(
                    j.optString("query","?"),j.optString("country","?")
                )))
            } catch(t:Throwable) { callback(Result.failure(t)) }
        }
    }
}
