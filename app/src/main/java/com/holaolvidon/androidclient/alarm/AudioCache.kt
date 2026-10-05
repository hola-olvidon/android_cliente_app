package com.holaolvidon.androidclient.alarm

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Descarga el audio de una alarma una sola vez y lo guarda en el almacenamiento interno.
 * En llamadas sucesivas devuelve el archivo ya descargado sin volver a descargarlo.
 *
 * La descarga es atómica (a un archivo temporal que luego se renombra), de modo que una descarga
 * interrumpida —frecuente con archivos "pesados" o red lenta— nunca deja un archivo a medias que
 * se confunda con un audio válido y haga fallar la reproducción.
 */
class AudioCache(context: Context) {
    private val dir = File(context.filesDir, "audio").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Evita descargar en paralelo el mismo audio desde hilos distintos (p. ej. prefetch + alarma). */
    private val locks = ConcurrentHashMap<String, Any>()

    /**
     * Devuelve el archivo local del audio (descargándolo si aún no existe) o `null` si no se pudo
     * obtener. El `urlAudio` del backend apunta a MinIO (`.../audios/{fileKey}`), por lo que la
     * descarga se hace a través del endpoint móvil del backend `{baseUrl}/mobile/audios/{fileKey}`.
     */
    fun getOrDownload(baseUrl: String, apiKey: String, urlAudio: String): File? {
        val fileKey = extractFileKey(urlAudio) ?: return null
        val file = File(dir, "${sha256(fileKey)}${extensionOf(fileKey)}")

        // Descarga única: si ya existe (y no está vacío) se reutiliza.
        if (file.exists() && file.length() > 0L) return file

        val lock = locks.computeIfAbsent(fileKey) { Any() }
        synchronized(lock) {
            // Recheck dentro del lock: otro hilo pudo descargarlo mientras esperábamos.
            if (file.exists() && file.length() > 0L) return file

            val tmp = File(dir, "${file.name}.${System.nanoTime()}.part")
            try {
                download(baseUrl, apiKey, fileKey, tmp)
                return if (tmp.length() > 0L && tmp.renameTo(file)) file else null
            } catch (e: Exception) {
                return null
            } finally {
                if (tmp.exists()) tmp.delete()
                locks.remove(fileKey, lock)
            }
        }
    }

    /** Descarga el contenido de `{baseUrl}/mobile/audios/{fileKey}` en [tmp]. */
    private fun download(baseUrl: String, apiKey: String, fileKey: String, tmp: File) {
        val url = "${baseUrl.trimEnd('/')}/mobile/audios/$fileKey"
        val request = Request.Builder()
            .url(url)
            .header("X-API-KEY", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return
            response.body?.byteStream()?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
        }
    }

    private fun extractFileKey(urlAudio: String): String? =
        urlAudio.substringAfterLast('/').takeIf { it.isNotBlank() }

    private fun extensionOf(fileKey: String): String {
        val ext = fileKey.substringAfterLast('.', "")
        return if (ext.isNotBlank() && ext.length <= 5 && '.' !in ext) ".$ext" else ""
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
