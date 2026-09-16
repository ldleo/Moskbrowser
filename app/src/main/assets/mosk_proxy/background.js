let current = { type:"DIRECT", host:"", port:0, username:"", password:"" };
const nativePort = browser.runtime.connectNative("mosk");

function applyProxy(p) {
  current = p;
  if (!current.host || !current.port || current.type === "DIRECT") {
    return browser.proxy.settings.clear({});
  }

  const v = { proxyType:"manual", proxyDNS:true, passthrough:"<local>" };

  if (current.type === "HTTP") {
    v.http = `${current.host}:${current.port}`;
    v.httpProxyAll = true;
  } else if (current.type === "HTTPS") {
    v.http = `${current.host}:${current.port}`;
    v.ssl = `${current.host}:${current.port}`;
    v.httpProxyAll = true;
  } else if (current.type === "SOCKS5") {
    v.socks = `${current.host}:${current.port}`;
    v.socksVersion = 5;
    v.proxyDNS = true;
  } else if (current.type === "SOCKS4") {
    v.socks = `${current.host}:${current.port}`;
    v.socksVersion = 4;
    v.proxyDNS = false;
  }

  return browser.proxy.settings.set({ value:v });
}

browser.webRequest.onAuthRequired.addListener(
  details => {
    if (!details.isProxy || !current.username) return {};
    return { authCredentials:{
      username:current.username,
      password:current.password
    }};
  },
  {urls:["<all_urls>"]},
  ["blocking"]
);

nativePort.onMessage.addListener(message => {
  if (!message) return;
  if (message.type === "setProxy") {
    applyProxy({
      type:message.proxyType || "DIRECT",
      host:message.host || "",
      port:Number(message.port || 0),
      username:message.username || "",
      password:message.password || ""
    }).catch(console.error);
  }
  if (message.type === "clearProxy") {
    current={type:"DIRECT",host:"",port:0,username:"",password:""};
    browser.proxy.settings.clear({}).catch(console.error);
  }
});
