package com.holaolvidon.androidclient.alarm

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Descarga el audio de una alarma una sola vez y lo guarda en el almacenamiento interno.
 * En llamadas sucesivas devuelve el archivo ya descargado sin volver a descargarlo.
 */
class AudioCache(context: Context) {
    private val dir = File(context.filesDir, "audio").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Devuelve el archivo local del audio (descargándolo si aún no existe) o `null` si no se pudo
     * obtener. El `urlAudio` del backend apunta a MinIO (`.../audios/{fileKey}`), por lo que la
     * descarga se hace a través del endpoint móvil del backend `{baseUrl}/mobile/audios/{fileKey}`.
     */
    fun getOrDownload(baseUrl: String, apiKey: String, urlAudio: String): File? {
        val fileKey = extractFileKey(urlAudio) ?: return null
        val ext = extensionOf(fileKey)
        val file = File(dir, "${sha256(fileKey)}$ext")

        // Descarga única: si ya existe (y no está vacío) se reutiliza.
        if (file.exists() && file.length() > 0L) return file

        val url = "${baseUrl.trimEnd('/')}/mobile/audios/$fileKey"
        val request = Request.Builder()
            .url(url)
            .header("X-API-KEY", apiKey)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
            }
            if (file.length() > 0L) file else null
        } catch (e: Exception) {
            null
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
