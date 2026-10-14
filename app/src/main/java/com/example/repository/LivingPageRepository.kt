package com.example.repository

import android.content.Context
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
import com.example.data.remote.model.ChatMessage
import com.example.data.remote.model.CloudQuote
import com.example.data.remote.model.CloudSurprise
import com.example.data.remote.model.UserProfile
import com.example.data.tts.RussianTtsManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class LivingPageRepository(
    private val dao: LivingPageDao,
    private val preferencesManager: PreferencesManager,
    private val dailyGenerationService: DailyGenerationService,
    private val geminiApiClient: GeminiApiClient,
    val ttsManager: RussianTtsManager,
    private val cloudFirestoreService: CloudFirestoreService,
    private val authManager: GoogleAuthManager
) {

    // === АВТОРИЗАЦИЯ ===

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

    // === ДРУЗЬЯ (ОБЛАКО) ===

    fun observeFriends(userId: String): Flow<List<UserProfile>> =
        cloudFirestoreService.observeFriends(userId)

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        cloudFirestoreService.observeUserProfile(userId)

    suspend fun addFriend(myUid: String, friendUid: String) =
        cloudFirestoreService.addFriend(myUid, friendUid)

    // === ЧАТ ===

    suspend fun sendMessage(fromId: String, toId: String, text: String) =
        cloudFirestoreService.sendMessage(fromId, toId, text)

    fun observeMessages(myId: String, friendId: String): Flow<List<ChatMessage>> =
        cloudFirestoreService.observeMessages(myId, friendId)

    // === КНИГИ ===

    val allBooks: Flow<List<BookEntity>> = dao.getAllBooks()
    val currentBook: Flow<BookEntity?> = dao.getCurrentBook()

    suspend fun updateBook(book: BookEntity) = dao.updateBook(book)

    suspend fun addBook(book: BookEntity) {
        dao.clearCurrentBook()
        dao.insertBook(book)
    }

    suspend fun setCurrentBook(book: BookEntity) {
        dao.clearCurrentBook()
        dao.setCurrentBookById(book.id)
    }

    suspend fun deleteBook(book: BookEntity) {
        dao.deleteBook(book)
    }

    // === ЦИТАТЫ ===

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

    // === ДРУЗЬЯ (ЛОКАЛЬНО — старый слой) ===

    val allFriends: Flow<List<FriendEntity>> = dao.getAllFriends()

    // === СЮРПРИЗЫ ===

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

    // === НАСТРОЙКИ ===

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

    // === ИИ ===

    suspend fun generateQuoteWithGemini(
        bookTitle: String,
        author: String,
        customPrompt: String
    ): Result<String> {
        val userKey = geminiApiKey.first()
        return geminiApiClient.generateLiteraryThought(bookTitle, author, customPrompt, userKey)
    }

    // === ОЗВУЧКА ===

    fun speakRussian(text: String, speed: Float = 1.0f) = ttsManager.speak(text, speed)
    fun stopSpeech() = ttsManager.stop()
    fun openTtsSettings() = ttsManager.openTtsSettings()
}
