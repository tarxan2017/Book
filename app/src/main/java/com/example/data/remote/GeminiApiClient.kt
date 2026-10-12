package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// DTO для Gemini REST API
@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiApiClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service = retrofit.create(GeminiService::class.java)

    suspend fun generateLiteraryThought(
        bookTitle: String,
        author: String,
        customPrompt: String,
        userApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !userApiKey.isNullOrBlank() -> userApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            // Атмосферный литературный fallback в стиле Коэльо и классики
            val fallbackThoughts = listOf(
                "«Иногда именно потеря показывает нам, что по-настоящему важно. Пустыня учит слушать сердце, а ветер доносит шепот звезд.»",
                "«Когда ты чего-нибудь желаешь очень сильно, вся Вселенная помогает тебе достигнуть этого.»",
                "«Жизнь измеряется не количеством вдохов, а моментами, когда от красоты перехватывает дыхание.»",
                "«Самые глубокие реки текут с наименьшим шумом, а самые мудрые книги читаются в тишине.»"
            )
            return@withContext Result.success(fallbackThoughts.random())
        }

        try {
            val promptText = """
Ты — литературный наставник и режиссер приложения «Живая Страница».
Книга: "$bookTitle", Автор: "$author".
Запрос/отрывок: "$customPrompt".
Сформулируй одну глубокую, вдохновляющую цитату или краткое философское размышление для видео-ролика на чистом русском языке.
Текст должен быть оформлен в кавычках «...», содержать не более 2-3 предложений, звучать поэтично, тепло и кинематографично.
Не добавляй никакого английского текста и служебных пометок.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = promptText)))
                )
            )

            val response = service.generateContent(apiKey, request)
            val generatedText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (!generatedText.isNullOrBlank()) {
                Result.success(generatedText)
            } else {
                Result.success("«В каждой прочитанной странице мы находим частицу собственной души.»")
            }
        } catch (e: Exception) {
            // Если запрос не прошел, возвращаем вдохновляющую цитату
            Result.success("«Иногда именно потеря показывает нам, что по-настоящему важно.»")
        }
    }
}
