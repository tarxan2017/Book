package com.example.data.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

data class LiteraryAnalysisResult(
    val title: String,
    val polishedQuote: String,
    val narrationText: String,
    val visualMood: String // "свеча", "страницы", "чай", "звезды"
)

class GeminiClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeQuoteAndCreateScript(
        apiKey: String,
        sourceText: String,
        bookTitle: String = "",
        author: String = ""
    ): Result<LiteraryAnalysisResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Offline fallback with poetic parsing if key is not configured yet
            return@withContext Result.success(fallbackAnalysis(sourceText, bookTitle, author))
        }

        val prompt = """
Ты — чуткий литературный редактор и режиссер атмосферных видео-открыток в приложении «Живая Страница».
Проанализируй следующий книжный отрывок/цитату:
Книга: "$bookTitle", Автор: "$author"
Текст: "$sourceText"

Сформируй ответ СТРОГО в следующем формате из 4 строк (каждая строка начинается с маркера):
ЗАГОЛОВОК: [Короткое поэтическое название до 4 слов]
ЦИТАТА: [Самая емкая, красивая и точная мысль из текста на русском языке]
ГОЛОС: [1-2 предложения теплого, глубокого закадрового текста на русском языке для озвучки ролика]
НАСТРОЕНИЕ: [Выбери строго одно слово из четырех: свеча, страницы, чай, звезды]
""".trimIndent()

        val requestObj = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            )
        )

        try {
            val jsonBody = requestAdapter.toJson(requestObj)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Ошибка API: ${response.code}"))
            }

            val parsedResponse = responseAdapter.fromJson(responseBodyString)
            val generatedText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext Result.success(fallbackAnalysis(sourceText, bookTitle, author))

            Result.success(parseModelResponse(generatedText, sourceText, bookTitle, author))
        } catch (e: Exception) {
            // Graceful fallback to local poetic engine if network fails
            Result.success(fallbackAnalysis(sourceText, bookTitle, author))
        }
    }

    private fun parseModelResponse(
        text: String,
        fallbackText: String,
        bookTitle: String,
        author: String
    ): LiteraryAnalysisResult {
        var title = "Литературная мысль"
        var quote = fallbackText
        var narration = "Тихий вечер с мудрой книгой. Позвольте страницам звучать в вашем сердце."
        var mood = "свеча"

        val lines = text.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("ЗАГОЛОВОК:") -> {
                    title = trimmed.removePrefix("ЗАГОЛОВОК:").trim().ifEmpty { title }
                }
                trimmed.startsWith("ЦИТАТА:") -> {
                    quote = trimmed.removePrefix("ЦИТАТА:").trim().ifEmpty { quote }
                }
                trimmed.startsWith("ГОЛОС:") -> {
                    narration = trimmed.removePrefix("ГОЛОС:").trim().ifEmpty { narration }
                }
                trimmed.startsWith("НАСТРОЕНИЕ:") -> {
                    val rawMood = trimmed.removePrefix("НАСТРОЕНИЕ:").trim().lowercase()
                    mood = when {
                        rawMood.contains("звезд") -> "звезды"
                        rawMood.contains("страниц") -> "страницы"
                        rawMood.contains("чай") -> "чай"
                        else -> "свеча"
                    }
                }
            }
        }

        return LiteraryAnalysisResult(
            title = title,
            polishedQuote = quote,
            narrationText = narration,
            visualMood = mood
        )
    }

    private fun fallbackAnalysis(
        sourceText: String,
        bookTitle: String,
        author: String
    ): LiteraryAnalysisResult {
        val trimmed = sourceText.trim()
        val sentences = trimmed.split(Regex("[.!?]\\s+"))
        val primaryQuote = if (sentences.isNotEmpty() && sentences[0].isNotBlank()) {
            sentences[0].trim() + "."
        } else {
            trimmed
        }

        val mood = when {
            trimmed.contains("звезд", ignoreCase = true) || trimmed.contains("ночь", ignoreCase = true) -> "звезды"
            trimmed.contains("чай", ignoreCase = true) || trimmed.contains("утро", ignoreCase = true) -> "чай"
            trimmed.contains("книг", ignoreCase = true) || trimmed.contains("страниц", ignoreCase = true) -> "страницы"
            else -> "свеча"
        }

        val authorNote = if (author.isNotBlank()) "в строках $author" else "на страницах книги"
        return LiteraryAnalysisResult(
            title = if (bookTitle.isNotBlank()) "Отзвук: $bookTitle" else "Мудрость строки",
            polishedQuote = primaryQuote,
            narrationText = "Слова, что остаются навсегда. Прислушайтесь к тишине и красоте мысли $authorNote.",
            visualMood = mood
        )
    }
}
