# Mosk

Mosk es una aplicación Android basada en Mozilla GeckoView, no en Android System WebView.

## Incluido

- GeckoView `158.0.20260915042311`
- Java 17
- Perfiles privados
- Un solo perfil visible/activo
- Hibernación best-effort
- HTTP / HTTPS / SOCKS4 / SOCKS5
- Usuario/contraseña de proxy
- Probador de proxy
- IP + país
- Sesión Flash
- Clean
- GitHub Actions

## Limitación importante

La API de proxy de Firefox es una configuración del navegador/runtime. No debe asumirse que existe un proxy independiente garantizado para cada `GeckoSession`.

Por eso esta primera arquitectura mantiene **una sola sesión activa/visible** y cambia el proxy global al activar el perfil. Las sesiones anteriores se marcan inactivas y se separan de la vista. Esto no es una garantía de que Android mantenga sockets de sesiones hibernadas congelados indefinidamente.

## Compilar localmente

Requiere JDK 17 y Gradle 8.11:

```bash
gradle assembleDebug --no-daemon
```

APK:
`app/build/outputs/apk/debug/app-debug.apk`

## GitHub

```bash
git init
git add .
git commit -m "Initial Mosk GeckoView"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/Newmosk.git
git push -u origin main

git tag v0.1.0
git push origin v0.1.0
```

El workflow `.github/workflows/build.yml` compila el APK en cada push y crea un Release para tags `v*`.

## Próxima fase

1. Icono Mosk final en todas las densidades.
2. Almacenamiento cifrado de credenciales.
3. Recuperación más fuerte después de muerte del proceso.
4. Tests instrumentados.
5. WebExtension de privacidad/fingerprint específica para Gecko.
6. Pruebas de proxy en dispositivos reales.
7. Firma release.
