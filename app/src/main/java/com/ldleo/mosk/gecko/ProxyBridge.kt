package com.ldleo.mosk.gecko
import android.content.Context
import android.util.Log
import com.ldleo.mosk.model.ProxyConfig
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import java.util.concurrent.atomic.AtomicReference

class ProxyBridge(context:Context, private val runtime:GeckoRuntime) {
    companion object {
        private const val ID="proxy@mosk.local"
        private const val APP="mosk"
        private const val URI="resource://android/assets/mosk_proxy/"
    }
    private val port=AtomicReference<WebExtension.Port?>(null)
    init {
        runtime.getWebExtensionController().ensureBuiltIn(URI,ID).accept(
            { ext ->
                ext.setMessageDelegate(object:WebExtension.MessageDelegate {
                    override fun onConnect(p:WebExtension.Port) {
                        if(p.name==APP) port.set(p)
                    }
                },APP)
            },
            { e -> Log.e("MoskProxy","extension install failed",e) }
        )
    }
    fun set(p:ProxyConfig) {
        port.get()?.postMessage(JSONObject().apply {
            put("type","setProxy"); put("proxyType",p.type); put("host",p.host)
            put("port",p.port); put("username",p.username); put("password",p.password)
        })
    }
    fun clear() {
        port.get()?.postMessage(JSONObject().put("type","clearProxy"))
    }
}
