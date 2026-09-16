package com.ldleo.mosk.storage
import android.content.Context
import com.ldleo.mosk.model.*
import org.json.JSONArray
import java.util.UUID
import kotlin.random.Random

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("mosk_profiles", Context.MODE_PRIVATE)
    fun load(): MutableList<BrowserProfile> {
        val a = JSONArray(prefs.getString("profiles","[]") ?: "[]")
        val out = mutableListOf<BrowserProfile>()
        for (i in 0 until a.length()) {
            val o=a.getJSONObject(i)
            val d=DeviceCatalog.all.firstOrNull { it.name==o.optString("device") } ?: DeviceCatalog.all.first()
            out += BrowserProfile(
                o.getString("id"), o.optString("name","Perfil"),
                o.optString("startUrl","https://example.com"),
                o.optLong("seed",Random.nextLong()), d,
                ProxyConfig.fromJson(o.optJSONObject("proxy") ?: org.json.JSONObject())
            )
        }
        return out
    }
    fun save(list: List<BrowserProfile>) {
        val a=JSONArray(); list.forEach { a.put(it.toJson()) }
        prefs.edit().putString("profiles",a.toString()).apply()
    }
    fun newId(prefix:String="profile")="${prefix}_${UUID.randomUUID()}"
}
