# android_client — Cliente Android de alarmas

Cliente Android nativo (Kotlin + Jetpack Compose) para el servicio de alarmas del backend de
`hola_olvidon`. Permite conectar con la **URL + clave de API**, elegir **uno o varios tenants** de
una lista y **suscribirse** a las alarmas que quieras, sonando (notificación + sonido + audio
opcional) a la `horaProgramada` de cada alarma suscrita.

## Cómo funciona

1. En la pantalla de conexión se indican solo **dos datos**: la **URL del servidor** y la **clave
   de API (`X-API-KEY`)**.
2. Al pulsar *Conectar*, la app consulta `GET /mobile/tenants` y muestra la **lista de tenants**.
3. Marca uno o varios tenants; la app obtiene sus alarmas (`GET /tenants/:id`) y las lista.
4. Activa el **switch** de cada alarma para suscribirte. Las alarmas suscritas se programan
   localmente (`AlarmManager`) y suenan a su hora.

> El backend **no** expone WebSocket/SSE/push; por eso el cliente hace *polling* (cada 30 s) para
> refrescar las alarmas de los tenants seleccionados.

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

### Compilar y publicar con GitHub Actions

El repo incluye `.github/workflows/build-apk.yml`. En cada push compila la APK y la publica como
**release** de GitHub (archivo `.apk` directo, sin comprimir, con el nombre del repositorio):

- **Descargar**: pestaña **Releases** del repositorio → release **latest** → `android_cliente_app.apk`.
- También se puede lanzar a mano desde **Actions → Build APK → Run workflow**.

El release se sobrescribe en cada build (tag `latest`), así que siempre hay un único enlace directo:

```
https://github.com/<TU_USUARIO>/android_cliente_app/releases/latest
```

## Configuración

| Campo | Valor por defecto | Nota |
|---|---|---|
| URL del servidor | `http://10.0.2.2:3000` | `10.0.2.2` apunta al host desde el **emulador**; desde un **dispositivo físico** usar la IP LAN (ej. `http://192.168.1.10:3000`) |
| Clave de API | `clave_secreta_movil` | Valor de `MOBILE_API_KEY` (ver `Backend/docker-compose.yml`); se envía en el header `X-API-KEY` |

La lista de tenants se obtiene automáticamente del backend; no hace falta conocer los IDs a mano.

## Permisos en tiempo de ejecución

- **Android 13+**: permiso de notificaciones (`POST_NOTIFICATIONS`).
- **Android 12+**: acceso a alarmas exactas (`SCHEDULE_EXACT_ALARM`); la app abre la pantalla del
  sistema para otorgarlo si no está concedido.

## Estructura

```
app/src/main/java/com/holaolvidon/androidclient/
├── MainActivity.kt          # host Compose + petición de permisos
├── data/
│   ├── Models.kt            # TenantSummary, Tenant, Alarm
│   ├── ApiClient.kt         # GET /mobile/tenants y /tenants/{id} (OkHttp + org.json)
│   └── Settings.kt          # persistencia (SharedPreferences)
├── ui/
│   ├── AppViewModel.kt      # estado + polling + suscripciones + programación
│   ├── AppRoot.kt           # conmuta Conexión / Principal
│   ├── ConnectScreen.kt     # URL + clave de API
│   ├── MainScreen.kt        # tenants (selección) + alarmas (suscripción)
│   └── theme/               # tema Material 3
└── alarm/
    ├── AlarmScheduler.kt    # programa/cancela alarmas exactas
    ├── AlarmReceiver.kt     # dispara notificación + audio
    └── NotificationHelper.kt# canal y notificación de alarma
```

## Notas sobre el backend

- Se añadió el endpoint `GET /mobile/tenants` (protegido con `X-API-KEY`) para listar los tenants
  desde la app móvil. Requiere **recompilar/reiniciar el backend** para que esté disponible
  (`docker compose up --build`, o el equivalente en tu entorno).
- `GET /tenants/:id` ya **no** devuelve el hash `password` del tenant (se omitió en la respuesta).
- `GET /alarms/tenant/:tenantId` sigue estando duplicado en `alarms.controller.ts` (la variante con
  JWT eclipsa la de `X-API-KEY`). La app no usa esa ruta, pero convendría corregir el duplicado.

## Posibles mejoras

- Reprogramar alarmas tras un reinicio del dispositivo (receptor `BOOT_COMPLETED`).
- Usar `WorkManager` para el sondeo periódico en segundo plano.
- Sonido de `urlAudio` vía un *foreground service* (más robusto que el `goAsync()` actual).
