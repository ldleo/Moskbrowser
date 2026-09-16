package com.ldleo.mosk.gecko
import android.content.Context
import android.widget.FrameLayout
import android.view.ViewGroup
import com.ldleo.mosk.model.BrowserProfile
import org.mozilla.geckoview.*

class GeckoManager(private val context:Context) {
    companion object { private var runtime:GeckoRuntime?=null }
    private val geckoRuntime:GeckoRuntime by lazy {
        runtime ?: synchronized(GeckoManager::class.java) {
            runtime ?: GeckoRuntime.create(
                context,
                GeckoRuntimeSettings.Builder()
                    .javaScriptEnabled(true).webFontsEnabled(true).build()
            ).also { runtime=it }
        }
    }
    private val proxy by lazy { ProxyBridge(context,geckoRuntime) }

    data class Handle(
        val profileId:String,
        val session:GeckoSession,
        val view:GeckoView,
        var canGoBack:Boolean=false
    )

    fun open(p:BrowserProfile, container:FrameLayout):Handle {
        val settings=GeckoSessionSettings.Builder()
            .usePrivateMode(true)
            .contextId(p.id)
            .suspendMediaWhenInactive(true)
            .userAgentOverride(p.device.userAgent)
            .build()

        val s=GeckoSession(settings)
        val h=Handle(p.id,s,GeckoView(context))

        s.setContentDelegate(object:GeckoSession.ContentDelegate{})
        s.setNavigationDelegate(object:GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session:GeckoSession, canGoBack:Boolean) {
                h.canGoBack=canGoBack
            }
        })
        s.setPromptDelegate(object:GeckoSession.PromptDelegate {
            override fun onAuthPrompt(
                session:GeckoSession,
                prompt:GeckoSession.PromptDelegate.AuthPrompt
            ):GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val proxyFlag=GeckoSession.PromptDelegate.AuthPrompt.AuthOptions.Flags.PROXY
                return if(p.proxy.username.isNotBlank() &&
                    (prompt.authOptions.flags and proxyFlag)!=0) {
                    GeckoResult.fromValue(prompt.confirm(p.proxy.username,p.proxy.password))
                } else null
            }
        })

        s.open(geckoRuntime)
        s.setActive(true)
        h.view.setSession(s)

        container.removeAllViews()
        container.addView(h.view,FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))

        if(p.proxy.isConfigured()) proxy.set(p.proxy) else proxy.clear()
        s.loadUri(p.startUrl)
        return h
    }

    fun hibernate(h:Handle) {
        h.session.setActive(false)
        h.session.flushSessionState()
        h.view.releaseSession()
    }

    fun resume(h:Handle,container:FrameLayout) {
        container.removeAllViews()
        container.addView(h.view,FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
        h.view.setSession(h.session)
        h.session.setActive(true)
    }

    fun clean(h:Handle) {
        h.session.purgeHistory()
        h.session.flushSessionState()
        h.session.reload(0)
    }
}
