package com.ldleo.mosk

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.ldleo.mosk.gecko.GeckoManager
import com.ldleo.mosk.model.*
import com.ldleo.mosk.network.PublicIpService
import com.ldleo.mosk.proxy.ProxyTester
import com.ldleo.mosk.storage.ProfileStore
import kotlin.random.Random

class MainActivity:AppCompatActivity() {
    private lateinit var profilesScreen:LinearLayout
    private lateinit var browserScreen:LinearLayout
    private lateinit var profileScroll:ScrollView
    private lateinit var profileContainer:LinearLayout
    private lateinit var tvEmpty:TextView
    private lateinit var tvNetwork:TextView
    private lateinit var tvActive:TextView
    private lateinit var geckoContainer:FrameLayout
    private lateinit var store:ProfileStore
    private lateinit var gecko:GeckoManager
    private val profiles=mutableListOf<BrowserProfile>()
    private var active:GeckoManager.Handle?=null
    private var activeProfile:BrowserProfile?=null

    override fun onCreate(b:Bundle?) {
        super.onCreate(b); setContentView(R.layout.activity_main)
        store=ProfileStore(this); gecko=GeckoManager(this); profiles+=store.load()
        profilesScreen=findViewById(R.id.profilesScreen); browserScreen=findViewById(R.id.browserScreen)
        profileScroll=findViewById(R.id.profileScroll); profileContainer=findViewById(R.id.profileContainer)
        tvEmpty=findViewById(R.id.tvEmpty); tvNetwork=findViewById(R.id.tvNetworkStatus)
        tvActive=findViewById(R.id.tvActive); geckoContainer=findViewById(R.id.geckoContainer)
        findViewById<TextView>(R.id.btnRefreshIp).setOnClickListener { refreshIp() }
        findViewById<Button>(R.id.btnProfiles).setOnClickListener { showProfiles() }
        findViewById<Button>(R.id.btnFlash).setOnClickListener { flash() }
        findViewById<Button>(R.id.btnNew).setOnClickListener { profileDialog(null) }
        findViewById<Button>(R.id.btnFlashBig).setOnClickListener { flash() }
        findViewById<Button>(R.id.btnNewBig).setOnClickListener { profileDialog(null) }
        findViewById<Button>(R.id.btnMinimize).setOnClickListener { minimize() }
        findViewById<Button>(R.id.btnClean).setOnClickListener { active?.let(gecko::clean) }
        render(); refreshIp()
    }

    private fun refreshIp() {
        tvNetwork.text="🌐 IP: consultando..."
        PublicIpService().fetch { r -> runOnUiThread {
            tvNetwork.text=r.fold(
                { "🌐 IP: ${it.ip} | ${it.country} (${it.code}) 🟢" },
                { "🌐 Red desconectada 🔴" })
        }}
    }

    private fun render() {
        profileContainer.removeAllViews()
        val empty=profiles.isEmpty()
        tvEmpty.visibility=if(empty) View.VISIBLE else View.GONE
        profileScroll.visibility=if(empty) View.GONE else View.VISIBLE
        profiles.forEach { p ->
            val row=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL; setPadding(14,14,6,14)
                setBackgroundResource(R.drawable.bg_card)
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=12}
            }
            val info=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            }
            val title=TextView(this).apply {
                text=p.name; textSize=17f; setTextColor(getColor(R.color.mosk_text))
            }
            val proxy=if(p.proxy.isConfigured()) "${p.proxy.type}://${p.proxy.host}:${p.proxy.port}" else "Directo"
            val sub=TextView(this).apply {
                text="${p.device.name}\n$proxy"; textSize=12f; setTextColor(getColor(R.color.mosk_muted))
            }
            info.addView(title); info.addView(sub)
            row.addView(info)
            row.addView(Button(this).apply{ text="Abrir";isAllCaps=false;setOnClickListener{open(p)}})
            row.addView(Button(this).apply{ text="✏️";isAllCaps=false;setOnClickListener{profileDialog(p)}})
            row.addView(Button(this).apply{ text="🗑️";isAllCaps=false;setOnClickListener{
                profiles.remove(p);store.save(profiles);render()
            }})
            profileContainer.addView(row)
        }
    }

    private fun showProfiles() {
        val d=Dialog(this)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,20,20,20);setBackgroundColor(getColor(R.color.mosk_card))}
        profiles.forEach { p -> box.addView(Button(this).apply{
            text="${p.name} — ${p.device.name}";isAllCaps=false;setOnClickListener{d.dismiss();open(p)}
        })}
        box.addView(Button(this).apply{text="+ Nuevo perfil";isAllCaps=false;setOnClickListener{d.dismiss();profileDialog(null)}})
        d.setContentView(box);d.show()
    }

    private fun profileDialog(existing:BrowserProfile?) {
        val d=Dialog(this);d.setContentView(R.layout.dialog_new_profile)
        val title=d.findViewById<TextView>(R.id.tvDialogTitle)
        val name=d.findViewById<EditText>(R.id.etName)
        val url=d.findViewById<EditText>(R.id.etUrl)
        val error=d.findViewById<TextView>(R.id.tvUrlError)
        val spinner=d.findViewById<Spinner>(R.id.spinnerDevice)
        val proxyText=d.findViewById<TextView>(R.id.tvProxyStatus)
        val names=DeviceCatalog.all.map{it.name}
        spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
        var proxy=existing?.proxy ?: ProxyConfig()
        existing?.let {
            title.text="Editar perfil";name.setText(it.name);url.setText(it.startUrl)
            spinner.setSelection(names.indexOf(it.device.name).coerceAtLeast(0))
        }
        fun updateProxy(){proxyText.text=if(proxy.isConfigured())"${proxy.type}://${proxy.host}:${proxy.port}" else "Sin proxy (directo)"}
        updateProxy()
        proxyText.setOnClickListener{proxyDialog(proxy){proxy=it;updateProxy()}}
        fun save(open:Boolean) {
            val u=url.text.toString().trim()
            if(!u.startsWith("http://")&&!u.startsWith("https://")){error.visibility=View.VISIBLE;return}
            error.visibility=View.GONE
            val dev=DeviceCatalog.all[spinner.selectedItemPosition]
            val p=if(existing==null){
                BrowserProfile(store.newId(),name.text.toString().trim().ifBlank{"Perfil ${profiles.size+1}"},u,Random.nextLong(),dev,proxy)
            }else{
                existing.name=name.text.toString().trim().ifBlank{existing.name}
                existing.startUrl=u;existing.device=dev;existing.proxy=proxy;existing
            }
            if(existing==null)profiles+=p
            store.save(profiles);render();d.dismiss();if(open)open(p)
        }
        d.findViewById<Button>(R.id.btnSave).setOnClickListener{save(false)}
        d.findViewById<Button>(R.id.btnSaveOpen).setOnClickListener{save(true)}
        d.findViewById<TextView>(R.id.btnCancel).setOnClickListener{d.dismiss()}
        d.show()
    }

    private fun proxyDialog(current:ProxyConfig,done:(ProxyConfig)->Unit) {
        val d=Dialog(this);d.setContentView(R.layout.dialog_proxy)
        val rg=d.findViewById<RadioGroup>(R.id.rgProtocol)
        val host=d.findViewById<EditText>(R.id.etHost);val port=d.findViewById<EditText>(R.id.etPort)
        val user=d.findViewById<EditText>(R.id.etUser);val pass=d.findViewById<EditText>(R.id.etPass)
        val result=d.findViewById<TextView>(R.id.tvProxyResult)
        when(current.type){ "HTTP"->rg.check(R.id.rbHttp);"HTTPS"->rg.check(R.id.rbHttps);"SOCKS5"->rg.check(R.id.rbSocks5);"SOCKS4"->rg.check(R.id.rbSocks4)}
        host.setText(current.host);if(current.port>0)port.setText(current.port.toString())
        user.setText(current.username);pass.setText(current.password)
        fun type()=when(rg.checkedRadioButtonId){R.id.rbHttp->"HTTP";R.id.rbHttps->"HTTPS";R.id.rbSocks5->"SOCKS5";R.id.rbSocks4->"SOCKS4";else->"DIRECT"}
        fun config()=ProxyConfig(type(),host.text.toString().trim(),port.text.toString().toIntOrNull()?:0,user.text.toString(),pass.text.toString())
        d.findViewById<Button>(R.id.btnTest).setOnClickListener{
            result.text="⏳ Probando..."
            ProxyTester().test(config()){r->runOnUiThread{result.text=r.fold(
                {"✅ IP: ${it.ip} | País: ${it.country}"},{"❌ ${it.message ?: "Conexión rechazada"}"})
            }}}
        d.findViewById<Button>(R.id.btnClear).setOnClickListener{done(ProxyConfig());d.dismiss()}
        d.findViewById<Button>(R.id.btnSave).setOnClickListener{done(config());d.dismiss()}
        d.findViewById<TextView>(R.id.btnCancel).setOnClickListener{d.dismiss()}
        d.show()
    }

    private fun flash() {
        open(BrowserProfile(store.newId("flash"),"⚡ Sesión Flash","https://example.com",Random.nextLong(),DeviceCatalog.all.random(),ProxyConfig()))
    }
    private fun open(p:BrowserProfile) {
        active?.let{gecko.hibernate(it)}
        activeProfile=p;profilesScreen.visibility=View.GONE;browserScreen.visibility=View.VISIBLE
        tvActive.text="🟢 ${p.name} (${p.device.name})"
        active=gecko.open(p,geckoContainer)
    }
    private fun minimize() {
        active?.let{gecko.hibernate(it)};geckoContainer.removeAllViews()
        active=null;activeProfile=null;browserScreen.visibility=View.GONE;profilesScreen.visibility=View.VISIBLE
        refreshIp()
    }
    override fun onPause(){super.onPause();active?.session?.setActive(false);active?.session?.flushSessionState()}
    override fun onResume(){super.onResume();active?.session?.setActive(true);refreshIp()}
    override fun onBackPressed(){
        if(browserScreen.visibility==View.VISIBLE){
            val h=active
            if(h!=null && h.canGoBack){
                h.session.goBack()
            }else{
                minimize()
            }
        }else super.onBackPressed()
    }
}
