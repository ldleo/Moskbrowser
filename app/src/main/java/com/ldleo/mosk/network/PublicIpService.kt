package com.ldleo.mosk.network
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class PublicIpInfo(val ip:String,val country:String,val code:String)

class PublicIpService {
    private val executor=Executors.newSingleThreadExecutor()
    fun fetch(callback:(Result<PublicIpInfo>)->Unit) {
        executor.execute {
            try {
                val c=URL("https://ipapi.co/json/").openConnection() as HttpURLConnection
                c.connectTimeout=5000; c.readTimeout=5000
                val j=JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                c.disconnect()
                callback(Result.success(PublicIpInfo(
                    j.optString("ip","?"),j.optString("country_name","?"),
                    j.optString("country_code","")
                )))
            } catch(t:Throwable) { callback(Result.failure(t)) }
        }
    }
}
