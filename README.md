# android_client — Cliente Android de alarmas

Cliente Android nativo (Kotlin + Jetpack Compose) para el servicio de alarmas del backend de
`hola_olvidon`. Permite **suscribirse a un tenant** (por su `id` + clave de API) y recibir **todas
las alarmas que contiene**, listándolas y **sonando** (notificación + sonido + audio opcional)
cuando se alcanza la `horaProgramada` de una alarma activa.

## Cómo funciona

1. En la pantalla de configuración se indican la **URL del servidor**, la **clave de API
   (`X-API-KEY`)** y el **ID del tenant**.
2. Al pulsar *Conectar*, la app hace polling de `GET /tenants/{id}` (con el header `X-API-KEY`)
   cada N segundos (por defecto 30).
3. Cada respuesta trae el tenant con su lista de alarmas. La app las muestra y programa una
   **alarma local** (`AlarmManager`, exacta) por cada alarma activa con hora futura.
4. Al llegar la hora, se muestra una **notificación** (sonido de alarma + vibración) y, si la
   alarma tiene `urlAudio`, se intenta reproducir ese audio (best-effort).

> El backend **no** expone WebSocket/SSE/push; por eso el cliente usa *polling*. Las alarmas se
> reprograman en cada refresco y también cada vez que se abre la app (se pierden si el dispositivo
> se reinicia hasta que la app se vuelve a abrir).

## Requisitos para compilar

- **Android Studio** (recomendado, trae su propio JDK 21) o línea de comandos con:
  - **JDK 17+** (Android Studio usa JBR 21; no usar JDK 25 con Gradle 8.11).
  - **Android SDK** (compileSdk 35). El `sdk.dir` se configura en `local.properties`
    (Android Studio lo genera automáticamente).

## Compilar el APK

Con Android Studio:

1. *Open* → seleccionar la carpeta `android_client`.
2. Esperar la sincronización de Gradle.
3. *Build → Build APK(s)*, o ejecutar sobre un emulador/dispositivo.

Por línea de comandos (con `sdk.dir` configurado en `local.properties`):

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

La APK de debug queda en: `app/build/outputs/apk/debug/app-debug.apk`.

### Compilar con GitHub Actions (sin instalar nada localmente)

El repo incluye `.github/workflows/build-apk.yml`, que compila la APK en un runner de GitHub:

1. Sube la carpeta `android_client` como repositorio propio en GitHub:
   ```bash
   cd android_client
   git init
   git add -A
   git commit -m "Cliente Android de alarmas"
   git branch -M main
   git remote add origin https://github.com/<TU_USUARIO>/android_client.git
   git push -u origin main
   ```
2. El workflow se dispara automáticamente con el push (también se puede lanzar a mano
   desde la pestaña **Actions → Build APK → Run workflow**).
3. Descarga la APK: **Actions → el run verde → sección *Artifacts* → `app-debug`** (un `.zip`
   que contiene `app-debug.apk`).

> El runner de GitHub usa el SDK de Android preinstalado y `actions/setup-java` con JDK 17;
> no hace falta configurar `local.properties` en CI (AGP localiza el SDK vía `ANDROID_HOME`).

## Configuración de conexión

| Campo | Valor por defecto | Nota |
|---|---|---|
| URL del servidor | `http://10.0.2.2:3000` | `10.0.2.2` apunta al host desde el **emulador**; desde un **dispositivo físico** usar la IP LAN de la máquina (ej. `http://192.168.1.10:3000`) |
| Clave de API | `clave_secreta_movil` | Valor de `MOBILE_API_KEY` (ver `Backend/docker-compose.yml`) |
| ID del tenant | *(vacío)* | El UUID del tenant al que suscribirse |
| Intervalo de sondeo | `30` | Segundos entre consultas |

Para obtener el **ID del tenant**: en el panel admin (`Frontend`), o vía `GET /tenants` con JWT de
administrador.

## Permisos en tiempo de ejecución

- **Android 13+**: permiso de notificaciones (`POST_NOTIFICATIONS`).
- **Android 12+**: acceso a alarmas exactas (`SCHEDULE_EXACT_ALARM`); la app abre la pantalla del
  sistema para otorgarlo si no está concedido.

## Estructura

```
app/src/main/java/com/holaolvidon/androidclient/
├── MainActivity.kt          # host Compose + petición de permisos
├── data/
│   ├── Models.kt            # Tenant, Alarm
│   ├── ApiClient.kt         # GET /tenants/{id} con X-API-KEY (OkHttp + org.json)
│   └── Settings.kt          # persistencia (SharedPreferences)
├── ui/
│   ├── AppViewModel.kt      # estado + polling + reprogramación
│   ├── AppRoot.kt           # conmuta Configuración / Alarmas
│   ├── SettingsScreen.kt    # formulario de conexión
│   ├── AlarmsScreen.kt      # lista de alarmas
│   └── theme/               # tema Material 3
└── alarm/
    ├── AlarmScheduler.kt    # programa/cancela alarmas exactas
    ├── AlarmReceiver.kt     # dispara notificación + audio
    └── NotificationHelper.kt# canal y notificación de alarma
```

## Notas sobre el backend (observaciones, no bloqueantes)

- `GET /tenants/:id` también devuelve el hash `password` del tenant; este cliente lo ignora, pero
  convendría omitirlo del lado servidor.
- `GET /alarms/tenant/:tenantId` está definido dos veces en `alarms.controller.ts` (la variante con
  JWT eclipsa la de `X-API-KEY`). Este cliente no usa esa ruta, pero vale la pena corregir el
  duplicado.

## Posibles mejoras

- Reprogramar alarmas tras un reinicio del dispositivo (receptor `BOOT_COMPLETED`).
- Usar `WorkManager` para el sondeo periódico en segundo plano.
- Sonido de `urlAudio` vía un *foreground service* (más robusto que el `goAsync()` actual).
