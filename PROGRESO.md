# PROGRESO — Galería Kuze

Documento de traspaso: sirve para que otra IA o otra persona continúe el proyecto sin contexto previo.
Última actualización: 2026-11-18.

## 1. Qué es

App Android nativa (Kotlin + Jetpack Compose) para ver las fotos y videos del servidor casero
(miniserve) desde el mismo Wi-Fi de casa. Estética **Material 3 Expressive**: tema menta claro
(con modo oscuro opcional), tipografía grande, chips tipo píldora, tarjetas muy redondeadas y
barra inferior flotante.

- Repo: `https://github.com/kuuzeeai/galeria-kuze`
- Paquete Android: `com.kuze.galeria`
- minSdk 26 · targetSdk 36 · compileSdk 36
- AGP 8.13.2 · Kotlin 2.3.21 · Compose BOM 2025.12.01 · Gradle 8.14.3 (JDK 17)

## 2. Estado actual (etapas 1 a 5 terminadas)

1. Ajustes cifrados (URL, usuario, clave) con `EncryptedSharedPreferences` y prueba de conexión.
2. Cuadrícula de fotos con miniaturas en línea, **3 por fila**, agrupada por día.
3. Pestaña Carpetas con cada carpeta como álbum; filtros Todo / Fotos / Videos.
4. Visor con zoom (pellizco y doble toque), deslizar entre fotos y videos en streaming.
5. Caché de miniaturas de 500 MB, lista guardada en el teléfono, deslizar para refrescar y
   mensaje claro cuando no hay Wi-Fi de casa.

Extra ya hecho: **miniaturas generadas en el servidor** (ver sección 4), que es lo que hace que
el catálogo cargue rápido.

Pendiente: **etapa 6 (borrar)**, ver sección 7.

## 3. Servidor casero (celular viejo)

- Modelo: Samsung SM-A127M, corre Termux. IP `192.168.101.36`.
- Fotos en `/storage/emulated/0/Kuz nj` (copiadas por Syncthing, solo lectura para la app).
- miniserve 0.35 sirve esa carpeta en `http://192.168.101.36:8080` con autenticación básica.
  La clave está en `~/.miniserve.env` (variable `MS_AUTH`), **nunca imprimirla**.
- Sesiones tmux: `opencode` (no tocar), `fotos` (miniserve 8080), `thumbs` (miniserve 8081),
  `thumbs-gen` (generador de miniaturas en bucle).
- Arranque automático: `~/.termux/boot/inicio.sh` (hay copia `inicio.sh.bak-2026-11-18`).

### HTML que devuelve miniserve (parser de la app)

- Carpetas: `<tr class="entry-type-directory">` con `<a class="directory" href="/Camera/">Camera/</a>`.
- Archivos: `<tr class="entry-type-file">` con `<a class="file" href="/Camera/IMG_....jpg">`.
- Fecha real: `td.date-cell > span` con formato `2026-11-18 04:28:29 -05:00`
  (el texto "2 hours ago" es solo adorno).
- Soporta `Range` (HTTP 206), por eso los videos se ven en streaming.
- La app arma las URLs con `HttpUrl.resolve(...)`, así los espacios (`Videos `) y acentos quedan
  codificados correctamente.

## 4. Miniaturas (importante)

Objetivo: no bajar la foto completa para navegar.

- Generador: `~/bin/kuze-thumbs.sh` en el celular viejo.
  - Lee `/storage/emulated/0/Kuz nj`, escribe en `~/.thumbs` con la misma estructura de carpetas
    y extensión `.jpg` (`Camera/IMG_1.jpg` → `~/.thumbs/Camera/IMG_1.jpg`).
  - 300 px de ancho, calidad ~70: **~25–35 KB por miniatura**.
  - Se salta archivos ya generados y borra miniaturas de fotos eliminadas.
  - Con `--loop` repite cada 15 minutos (sesión tmux `thumbs-gen`).
- Servidor: miniserve en el puerto **8081** sirviendo `~/.thumbs`, con la misma autenticación.
- La app pide `http://IP:8081/<carpeta>/<nombre>.jpg`; si esa miniatura no existe (404) cae
  automáticamente a la foto original. Al abrir una foto en el visor siempre se pide la original.
- Dependencia instalada en el viejo: `ffmpeg` (paquete de Termux) para redimensionar.

## 5. Estructura de la app

```
app/src/main/java/com/kuze/galeria/
├─ MainActivity.kt            navegación entre pantallas, cabecera, chips, barra inferior
├─ data/
│  ├─ Models.kt               Media, FolderRef, DirListing, UnauthorizedException
│  ├─ SettingsStore.kt        URL, usuario, clave y tema guardados cifrados
│  ├─ ServerApi.kt            OkHttp + auth básica, parser Jsoup, URL de miniaturas
│  ├─ MediaRepository.kt      recorre todas las carpetas y devuelve la lista ordenada
│  ├─ IndexCache.kt           guarda la lista en un archivo (abre rápido / sin conexión)
│  └─ AppState.kt             estado: items, loading, error, progreso, tema
└─ ui/
   ├─ Theme.kt                colores claro/oscuro, formas y tipografía Expressive
   ├─ Expressive.kt           botones redondos, chips, cabecera, barra flotante, hero, mensajes
   ├─ PhotosScreen.kt         cuadrícula 3 columnas, ThumbImage con fallback, agrupación por día
   ├─ FoldersScreen.kt        carpetas como tarjetas de álbum
   ├─ ViewerScreen.kt         pager, zoom, reproductor ExoPlayer con OkHttp
   ├─ SettingsScreen.kt       formulario de conexión
   └─ ImageLoaderProvider.kt  Coil + OkHttp compartido + caché de disco 500 MB
```

Notas de comportamiento:

- La lista completa de archivos se lee al refrescar y queda en memoria + `filesDir/media-index.tsv`.
- Caché de miniaturas en `cacheDir/img`, máximo 500 MB.
- Si cambia la URL del servidor, el índice guardado se descarta solo (guarda la URL en su cabecera).
- Errores de red se muestran como "Conéctate al Wi-Fi de casa"; 401 como usuario/clave incorrectos.

## 6. Cómo compilar e instalar

Se compila **solo en GitHub Actions** (el usuario no tiene PC). Workflow: `.github/workflows/build.yml`.

```
cd ~/tmp-work
git clone --depth 1 https://github.com/kuuzeeai/galeria-kuze.git
cd galeria-kuze
# editar
git add -A && git commit -m "..." && git push
```

El push a `main` dispara el build. Para revisar y bajar la APK:

```
gh run list
gh run view <ID> --log-failed
gh run download <ID> --name galeria-kuze-debug -D apk
adb install -r apk/app-debug.apk
```

El workflow instala `platforms;android-36`, `build-tools;36.0.0`, genera el wrapper de Gradle
8.14.3 y sube el artefacto `galeria-kuze-debug`. También se copia a `Ai/galeria-kuze-debug.apk`
del teléfono para instalarla desde el administrador de archivos.

## 7. Pendiente: etapa 6 (borrar) — solo cuando el usuario lo pida

- "Eliminar para mí": oculta en la app (lista de URLs ocultas en SharedPreferences). No borra nada.
- "Eliminar para todos": borra el archivo del servidor. Es permanente.
- Antes de implementar:
  1. Confirmar que miniserve soporta `-R, --rm-files` (en 0.35 sí existe).
  2. Copia de `inicio.sh` y añadir `--rm-files` a la sesión `fotos`. Reiniciar **solo** `fotos`.
  3. Descubrir el método HTTP que usa miniserve para borrar y probarlo con una carpeta `_prueba`
     creada para eso, nunca con fotos reales. Borrar la carpeta al terminar.
- Advertencias: nunca pulsar "Revertir cambios locales" en Syncthing del viejo (volvería a bajar
  las fotos borradas). No se borran carpetas completas, solo archivos.

## 8. Reglas para quien continúe

- No tocar ni mover `/storage/emulated/0/Kuz nj` ni la configuración de Syncthing.
- No matar la sesión tmux `opencode`; nunca usar `pkill`.
- No imprimir ni pedir claves. `~/.miniserve.env` se carga dentro del comando, sin mostrarla.
- Avisar antes de cualquier cambio en el celular viejo o que pueda borrar datos.
- La app es solo lectura hasta que se implemente la etapa 6.
