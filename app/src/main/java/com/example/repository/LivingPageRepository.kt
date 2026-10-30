package com.example.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.BookParser
import com.example.data.auth.GoogleAuthManager
import com.example.data.datastore.DailyGenerationService
import com.example.data.datastore.DailyUsage
import com.example.data.datastore.PreferencesManager
import com.example.data.local.dao.LivingPageDao
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.SurpriseEntity
import com.example.data.remote.CloudFirestoreService
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.TelegramBotService
import com.example.data.remote.model.ChatMessage
import com.example.data.remote.model.CloudQuote
import com.example.data.remote.model.CloudSurprise
import com.example.data.remote.model.Comment
import com.example.data.remote.model.PublicBook
import com.example.data.remote.model.UserProfile
import com.example.data.tts.RussianTtsManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

class LivingPageRepository(
    private val dao: LivingPageDao,
    private val preferencesManager: PreferencesManager,
    private val dailyGenerationService: DailyGenerationService,
    private val geminiApiClient: GeminiApiClient,
    val ttsManager: RussianTtsManager,
    private val cloudFirestoreService: CloudFirestoreService,
    private val authManager: GoogleAuthManager,
    private val telegramBotService: TelegramBotService
) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        val result = authManager.signInWithGoogle(context)
        result.onSuccess { user ->
            cloudFirestoreService.syncUserProfile(
                UserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Читатель",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    statusText = "В сети"
                )
            )
        }
        return result
    }

    fun signOut() = authManager.signOut()

    suspend fun saveFcmToken(userId: String, token: String) =
        cloudFirestoreService.saveFcmToken(userId, token)

    fun observeFriends(userId: String): Flow<List<UserProfile>> =
        cloudFirestoreService.observeFriends(userId)

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        cloudFirestoreService.observeUserProfile(userId)

    suspend fun addFriend(myUid: String, friendUid: String) =
        cloudFirestoreService.addFriend(myUid, friendUid)

    suspend fun findUserByUid(uid: String): UserProfile? =
        cloudFirestoreService.findUserByUid(uid)

    fun observeCommunityUsers(): Flow<List<UserProfile>> =
        cloudFirestoreService.observeCommunityUsers()

    suspend fun sendMessage(fromId: String, toId: String, text: String) =
        cloudFirestoreService.sendMessage(fromId, toId, text)

    fun observeMessages(myId: String, friendId: String): Flow<List<ChatMessage>> =
        cloudFirestoreService.observeMessages(myId, friendId)

    val allBooks: Flow<List<BookEntity>> = dao.getAllBooks()
    val currentBook: Flow<BookEntity?> = dao.getCurrentBook()

    suspend fun updateBook(book: BookEntity) = dao.updateBook(book)

    suspend fun addBook(book: BookEntity) {
        dao.clearCurrentBook()
        dao.insertBook(book)
    }

    suspend fun setCurrentBook(book: BookEntity) {
        dao.clearCurrentBook()
        dao.setCurrentBookById(book.id.toLong())
    }

    suspend fun deleteBook(book: BookEntity) {
        dao.deleteBook(book)
    }

    val allQuotes: Flow<List<QuoteEntity>> = dao.getAllQuotes()
    val todayQuote: Flow<QuoteEntity?> = dao.getTodayQuote()

    suspend fun addQuote(quote: QuoteEntity) {
        dao.insertQuote(quote)
        val user = currentUser.value
        if (user != null) {
            try {
                cloudFirestoreService.saveQuoteToCloud(
                    userId = user.uid,
                    quote = CloudQuote(
                        userId = user.uid,
                        quoteText = quote.quoteText,
                        author = quote.author,
                        bookTitle = quote.bookTitle,
                        videoDuration = quote.videoDuration
                    )
                )
            } catch (_: Exception) {}
        }
    }

    suspend fun toggleFavoriteQuote(quote: QuoteEntity) {
        dao.updateQuote(quote.copy(isFavorite = !quote.isFavorite))
    }

    val allFriends: Flow<List<FriendEntity>> = dao.getAllFriends()

    val allSurprises: Flow<List<SurpriseEntity>> = dao.getAllSurprises()
    val latestUnopenedSurprise: Flow<SurpriseEntity?> = dao.getLatestUnopenedSurprise()

    suspend fun sendSurprise(surprise: SurpriseEntity) {
        dao.insertSurprise(surprise)
        val user = currentUser.value
        try {
            cloudFirestoreService.sendSurprise(
                CloudSurprise(
                    senderId = user?.uid ?: "local_user",
                    senderName = user?.displayName ?: surprise.senderName,
                    receiverId = "friend_${surprise.receiverName}",
                    receiverName = surprise.receiverName,
                    quoteText = surprise.quoteText,
                    author = surprise.author,
                    bookTitle = surprise.bookTitle,
                    formatType = surprise.formatType,
                    durationSeconds = surprise.durationSeconds
                )
            )
        } catch (_: Exception) {}
    }

    suspend fun markSurpriseOpened(surprise: SurpriseEntity) {
        dao.updateSurprise(surprise.copy(isOpened = true))
    }

    fun observePublicBooks(): Flow<List<PublicBook>> =
        cloudFirestoreService.observePublicBooks()

    fun observeRecentPublicBooks(limit: Long = 5): Flow<List<PublicBook>> =
        cloudFirestoreService.observeRecentPublicBooks(limit)

    suspend fun publishBook(book: PublicBook): String =
        cloudFirestoreService.publishBook(book)

    suspend fun updatePublicBook(book: PublicBook) =
        cloudFirestoreService.updatePublicBook(book)

    suspend fun deletePublicBook(bookId: String) =
        cloudFirestoreService.deletePublicBook(bookId)

    suspend fun getCoverUrl(coverFileId: String): String? {
        if (coverFileId.isBlank()) return null
        return telegramBotService.getFileUrl(coverFileId)
    }

    suspend fun downloadPublicBook(context: Context, book: PublicBook): Result<Uri> {
        // === ПРОВЕРКА НА ДУБЛИКАТ ===
        val existingBooks = dao.getAllBooks().first()
        val existingBook = existingBooks.find { 
            it.title.trim().equals(book.title.trim(), ignoreCase = true) && 
            it.author.trim().equals(book.author.trim(), ignoreCase = true) 
        }

        // ВСЕГДА СКАЧИВАЕМ ЗАНОВО, ЧТОБЫ ПОЛУЧИТЬ СВЕЖУЮ ВЕРСИЮ
        if (book.pdfFileId.isBlank()) {
            return Result.failure(Exception("У этой книги нет файла"))
        }

        val fileUri = telegramBotService.downloadByFileId(
            fileId = book.pdfFileId,
            fileName = "book_${book.id}.bin"
        ) ?: return Result.failure(Exception("Не удалось скачать файл"))

        val coverUri = if (book.coverFileId.isNotBlank()) {
            telegramBotService.downloadByFileId(
                fileId = book.coverFileId,
                fileName = "cover_${book.id}.jpg"
            )
        } else {
            null
        }

        val bookParser = BookParser(context)
        val textResult = bookParser.extractTextFromBook(fileUri)
        val textContent = textResult.getOrNull() ?: ""

        if (textContent.isBlank()) {
            return Result.failure(Exception("Не удалось извлечь текст. Возможно, файл поврежден или это сканы без текстового слоя."))
        }

        dao.clearCurrentBook()

        if (existingBook != null) {
            val updatedBook = existingBook.copy(
                textContent = textContent,
                coverUri = coverUri?.toString() ?: existingBook.coverUri,
                isCurrent = true,
                isUserWritten = false, 
                publicBookId = book.id
            )
            dao.updateBook(updatedBook)
            dao.setCurrentBookById(updatedBook.id.toLong())
        } else {
            val localBook = BookEntity(
                title = book.title.ifBlank { "Без названия" },
                author = book.author.ifBlank { "Неизвестный автор" },
                coverRes = com.example.R.drawable.book_cover_alchemist,
                totalChapters = 1,
                currentChapter = 1,
                progressPercent = 0,
                generatedQuotesCount = 0,
                textContent = textContent,
                isCurrent = true,
                coverUri = coverUri?.toString() ?: "",
                isUserWritten = false,
                publicBookId = book.id
            )
            dao.insertBook(localBook)
        }

        return Result.success(fileUri)
    }

    suspend fun publishOrUpdateBook(
        context: Context,
        book: BookEntity,
        category: String,
        description: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = currentUser.value
            val publisherId = user?.uid ?: "anonymous"
            val publisherName = user?.displayName ?: "Автор"

            val txtFile = File(context.cacheDir, "book_${book.id}.txt")
            txtFile.writeText(book.textContent)

            val txtFileId = telegramBotService.uploadFile(txtFile, txtFile.name)
                ?: throw Exception("Не удалось загрузить текст книги в облако")

            var coverFileId = ""
            if (book.coverUri.isNotBlank()) {
                val coverPath = Uri.parse(book.coverUri).path
                if (coverPath != null) {
                    val coverFile = File(coverPath)
                    if (coverFile.exists()) {
                        coverFileId = telegramBotService.uploadFile(coverFile, coverFile.name) ?: ""
                    }
                }
            }

            // ПРОВЕРКА НА ДУБЛИКАТ В ОБЛАКЕ ПЕРЕД ПУБЛИКАЦИЕЙ
            var finalId = book.publicBookId
            if (finalId.isBlank()) {
                val allCloudBooks = cloudFirestoreService.observePublicBooks().first()
                val existingCloudBook = allCloudBooks.find { 
                    it.title.trim().equals(book.title.trim(), ignoreCase = true) && 
                    it.author.trim().equals(book.author.trim(), ignoreCase = true) 
                }
                if (existingCloudBook != null) {
                    finalId = existingCloudBook.id
                }
            }

            val publicBook = PublicBook(
                id = finalId,
                title = book.title,
                author = book.author,
                description = description,
                category = category,
                pdfFileId = txtFileId,
                coverFileId = coverFileId,
                publisherId = publisherId,
                publisherName = publisherName,
                createdAt = if (finalId.isBlank()) Date() else null
            )

            if (finalId.isBlank()) {
                val newId = cloudFirestoreService.publishBook(publicBook)
                dao.updateBook(book.copy(publicBookId = newId))
            } else {
                cloudFirestoreService.updatePublicBook(publicBook)
                if (book.publicBookId != finalId) {
                    dao.updateBook(book.copy(publicBookId = finalId))
                }
            }

            txtFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Publish", "Ошибка публикации", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // НОВЫЙ МЕТОД: Запрос к AI-советнику
    // ==========================================
    suspend fun getAiAdvice(prompt: String, context: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("prompt", prompt)
                put("context", context)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("https://livingpage-ai.taxo2015.workers.dev")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorObj = try { JSONObject(responseBody) } catch (e: Exception) { null }
                val errorMsg = errorObj?.optString("error") ?: "Ошибка сервера: ${response.code}"
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val resultText = jsonResponse.optString("result")
            
            if (resultText.isBlank()) {
                Result.failure(Exception("AI вернул пустой ответ"))
            } else {
                Result.success(resultText)
            }
        } catch (e: Exception) {
            Log.e("AI_Error", "Ошибка запроса к AI", e)
            Result.failure(e)
        }
    }

    fun observeModerators(): Flow<List<String>> =
        cloudFirestoreService.observeModerators()

    suspend fun addModerator(uid: String, name: String) =
        cloudFirestoreService.addModerator(uid, name)

    suspend fun removeModerator(uid: String) =
        cloudFirestoreService.removeModerator(uid)

    suspend fun isModerator(uid: String): Boolean =
        cloudFirestoreService.isModerator(uid)

    val isDarkTheme: Flow<Boolean> = preferencesManager.isDarkTheme
    suspend fun setDarkTheme(isDark: Boolean) = preferencesManager.setDarkTheme(isDark)

    val dailyUsage: Flow<DailyUsage> = dailyGenerationService.usageFlow
    suspend fun canGenerateVideo(): Boolean = dailyGenerationService.canGenerate()
    suspend fun consumeDailyGeneration(): Boolean = dailyGenerationService.tryConsumeGeneration()
    suspend fun resetDailyLimit() = dailyGenerationService.resetUsageForToday()

    val geminiApiKey: Flow<String> = preferencesManager.geminiApiKey
    suspend fun setGeminiApiKey(key: String) = preferencesManager.setGeminiApiKey(key)

    val videoApiKey: Flow<String> = preferencesManager.videoApiKey
    suspend fun setVideoApiKey(key: String) = preferencesManager.setVideoApiKey(key)

    val readerFontSize: Flow<Float> = preferencesManager.readerFontSize
    suspend fun setReaderFontSize(size: Float) = preferencesManager.setReaderFontSize(size)

    val ttsSpeed: Flow<Float> = preferencesManager.ttsSpeed
    suspend fun setTtsSpeed(speed: Float) = preferencesManager.setTtsSpeed(speed)

    val notifyBooks: Flow<Boolean> = preferencesManager.notifyBooks
    suspend fun setNotifyBooks(enabled: Boolean) = preferencesManager.setNotifyBooks(enabled)

    val notifyFriends: Flow<Boolean> = preferencesManager.notifyFriends
    suspend fun setNotifyFriends(enabled: Boolean) = preferencesManager.setNotifyFriends(enabled)

    val dndEnabled: Flow<Boolean> = preferencesManager.dndEnabled
    suspend fun setDndEnabled(enabled: Boolean) = preferencesManager.setDndEnabled(enabled)

    suspend fun generateQuoteWithGemini(
        bookTitle: String,
        author: String,
        customPrompt: String
    ): Result<String> {
        val userKey = geminiApiKey.first()
        return geminiApiClient.generateLiteraryThought(bookTitle, author, customPrompt, userKey)
    }

    fun speakRussian(text: String, voice: String = "ru-RU-SvetlanaNeural", speed: Float = 1.0f) = ttsManager.speak(text, voice, speed)
    fun stopSpeech() = ttsManager.stop()
    fun openTtsSettings() = ttsManager.openTtsSettings()

    suspend fun getComments(bookId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://livingpage-api.taxo2015.workers.dev/api/comments?bookId=$bookId")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Ошибка сервера: ${response.code}")

            val bodyString = response.body?.string() ?: throw Exception("Пустой ответ")
            val jsonArray = JSONArray(bodyString)
            val commentsList = mutableListOf<Comment>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                commentsList.add(
                    Comment(
                        id = obj.optInt("id"),
                        book_id = obj.optString("book_id"),
                        user_id = obj.optString("user_id"),
                        user_name = obj.optString("user_name"),
                        text = obj.optString("text"),
                        created_at = obj.optLong("created_at")
                    )
                )
            }
            Result.success(commentsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postComment(bookId: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = currentUser.value
            val userId = user?.uid ?: "anonymous"
            val userName = user?.displayName ?: "Читатель"

            val json = JSONObject().apply {
                put("bookId", bookId)
                put("userId", userId)
                put("userName", userName)
                put("text", text)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://livingpage-api.taxo2015.workers.dev/api/comments")
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Ошибка сервера: ${response.code}")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = currentUser.value
            val userId = user?.uid ?: "anonymous"

            val json = JSONObject().apply {
                put("commentId", commentId)
                put("userId", userId)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("https://livingpage-api.taxo2015.workers.dev/api/comments")
                .delete(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Ошибка сервера: ${response.code}")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
