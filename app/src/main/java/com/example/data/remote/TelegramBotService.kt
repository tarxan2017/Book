package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Сервис для работы с Telegram через Cloudflare Worker.
 * - getFileUrl: получает прямую ссылку на файл (для обложек).
 * - downloadByFileId: скачивает файл через Worker (для PDF).
 */
class TelegramBotService(private val context: Context) {

    private val workerBaseUrl = "https://book.taxo2015.workers.dev"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Получает прямую ссылку на файл в Telegram по file_id.
     * Используется для обложек (чтобы Coil мог загрузить картинку).
     */
    suspend fun getFileUrl(fileId: String): String? = withContext(Dispatchers.IO) {
        if (fileId.isBlank()) return@withContext null

        val url = "$workerBaseUrl/api/getFile?file_id=$fileId"

        try {
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e("TelegramBotService", "getFileUrl fail: ${response.code}, $body")
                    return@withContext null
                }

                val json = JSONObject(body)
                if (json.has("file_url")) json.getString("file_url") else null
            }
        } catch (e: Exception) {
            Log.e("TelegramBotService", "getFileUrl error", e)
            null
        }
    }

    /**
     * Скачивает файл из Telegram по его file_id через Worker.
     * Worker сам обращается к Telegram и отдаёт нам бинарные данные.
     */
    suspend fun downloadByFileId(fileId: String, fileName: String): Uri? = withContext(Dispatchers.IO) {
        if (fileId.isBlank() || fileName.isBlank()) return@withContext null

        val url = "$workerBaseUrl/api/download?file_id=$fileId"

        try {
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("TelegramBotService", "download fail: ${response.code}")
                    return@withContext null
                }

                val dir = File(context.filesDir, "telegram_downloads")
                if (!dir.exists()) dir.mkdirs()

                val outputFile = File(dir, fileName)
                response.body?.byteStream()?.use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }

                val size = outputFile.length()
                Log.d("TelegramBotService", "saved $size bytes to ${outputFile.absolutePath}")
                if (size == 0L) return@withContext null

                Uri.fromFile(outputFile)
            }
        } catch (e: Exception) {
            Log.e("TelegramBotService", "download error", e)
            null
        }
    }
}
