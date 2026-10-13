package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.PdfParser
import com.example.data.datastore.DailyUsage
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.SurpriseEntity
import com.example.repository.LivingPageRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object MyBook : Screen()
    object Friends : Screen()
    object Settings : Screen()
    object Reader : Screen()
    data class VideoPlayer(val quote: QuoteEntity) : Screen()
    data class SendSurprise(val defaultFriend: FriendEntity? = null) : Screen()
    data class ReceiveSurprise(val surprise: SurpriseEntity) : Screen()
    object OpenBookAnimation : Screen()
    object GiftAnimation : Screen()
}

class MainViewModel(
    private val repository: LivingPageRepository
) : ViewModel() {

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBook: StateFlow<BookEntity?> = repository.currentBook
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allQuotes: StateFlow<List<QuoteEntity>> = repository.allQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayQuote: StateFlow<QuoteEntity?> = repository.todayQuote
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allFriends: StateFlow<List<FriendEntity>> = repository.allFriends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = repository.currentUser

    fun signInWithGoogle(context: android.content.Context) {
        viewModelScope.launch {
            val result = repository.signInWithGoogle(context)
            result.onSuccess { user ->
                _userMessage.value = "Вход выполнен: ${user.displayName ?: user.email}"
            }.onFailure { error ->
                _userMessage.value = "Ошибка входа: ${error.localizedMessage}"
            }
        }
    }

    fun signOutFromGoogle() {
        repository.signOut()
        _userMessage.value = "Вы вышли из профиля"
    }

    val dailyUsage: StateFlow<DailyUsage> = repository.dailyUsage
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DailyUsage(usedCount = 1, maxDaily = 5, remainingCount = 4, isLimitReached = false, lastDate = "")
        )

    val remainingGenerations: StateFlow<Int> = dailyUsage
        .map { it.remainingCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val isGenerationLimitReached: StateFlow<Boolean> = dailyUsage
        .map { it.isLimitReached }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showLimitReachedDialog = MutableStateFlow(false)
    val showLimitReachedDialog: StateFlow<Boolean> = _showLimitReachedDialog.asStateFlow()

    val geminiApiKey: StateFlow<String> = repository.geminiApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val videoApiKey: StateFlow<String> = repository.videoApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val readerFontSize: StateFlow<Float> = repository.readerFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 17f)

    val ttsSpeed: StateFlow<Float> = repository.ttsSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val isTtsSpeaking: StateFlow<Boolean> = repository.ttsManager.isSpeaking
    val isRussianSupported: StateFlow<Boolean> = repository.ttsManager.isRussianSupported

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun dismissLimitDialog() {
        _showLimitReachedDialog.value = false
    }

    fun resetDailyLimitForDemo() {
        viewModelScope.launch {
            repository.resetDailyLimit()
            _showLimitReachedDialog.value = false
            _userMessage.value = "Лимит генераций сброшен (доступно 5 из 5)"
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            repository.setDarkTheme(isDark)
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            repository.setGeminiApiKey(key)
            _userMessage.value = "Ключ Gemini сохранен"
        }
    }

    fun setVideoApiKey(key: String) {
        viewModelScope.launch {
            repository.setVideoApiKey(key)
            _userMessage.value = "Ключ Magic Hour сохранен"
        }
    }

    fun setReaderFontSize(size: Float) {
        viewModelScope.launch {
            repository.setReaderFontSize(size)
        }
    }

    fun generateNewQuote() {
        viewModelScope.launch {
            val canGenerate = repository.canGenerateVideo()
            if (!canGenerate) {
                _showLimitReachedDialog.value = true
                _userMessage.value = "Суточный лимит 5/день исчерпан. Генерация заблокирована."
                return@launch
            }

            val consumed = repository.consumeDailyGeneration()
            if (!consumed) {
                _showLimitReachedDialog.value = true
                _userMessage.value = "Суточный лимит 5/день исчерпан. Генерация заблокирована."
                return@launch
            }

            _isAiThinking.value = true
            val book = currentBook.value
            val title = book?.title ?: "Алхимик"
            val author = book?.author ?: "Пауло Коэльо"

            val result = repository.generateQuoteWithGemini(
                bookTitle = title,
                author = author,
                customPrompt = "Вдохновляющая мысль для видеоролика о поиске своего пути и знаках судьбы"
            )

            delay(1200)
            _isAiThinking.value = false

            result.onSuccess { quoteText ->
                val newQuote = QuoteEntity(
                    quoteText = quoteText,
                    author = author,
                    bookTitle = "«$title»",
                    dateLabel = "Сегодня",
                    bgRes = R.drawable.quote_bg_vertical,
                    videoDuration = "0:24",
                    isToday = true,
                    isFavorite = true
                )
                repository.addQuote(newQuote)
                _userMessage.value = "Видео с цитатой создано! (Осталось: ${dailyUsage.value.remainingCount})"
                _currentScreen.value = Screen.VideoPlayer(newQuote)
            }.onFailure { error ->
                _userMessage.value = "Ошибка генерации: ${error.localizedMessage}"
            }
        }
    }

    /**
     * Загрузка книги из PDF с указанием названия, автора и обложки.
     * Если пользователь не ввёл название/автора — используются значения по умолчанию.
     */
    fun loadBookFromPdf(
        context: Context,
        uri: Uri,
        title: String = "",
        author: String = "",
        coverUri: String = ""
    ) {
        viewModelScope.launch {
            _isAiThinking.value = true
            _userMessage.value = "Читаем PDF-файл..."

            val pdfParser = PdfParser(context)
            val result = pdfParser.extractTextFromPdf(uri)

            result.onSuccess { text ->
                val newBook = BookEntity(
                    title = title.ifBlank { "Загруженная книга" },
                    author = author.ifBlank { "Неизвестный автор" },
                    coverRes = R.drawable.book_cover_alchemist,
                    totalChapters = 1,
                    currentChapter = 1,
                    progressPercent = 0,
                    generatedQuotesCount = 0,
                    textContent = text,
                    isCurrent = true,
                    coverUri = coverUri
                )
                repository.addBook(newBook)
                _userMessage.value = "Книга успешно загружена! Текст готов к чтению."
            }.onFailure { error ->
                _userMessage.value = "Ошибка чтения PDF: ${error.localizedMessage}"
            }

            _isAiThinking.value = false
        }
    }

    fun sendSurpriseGift(
        receiver: FriendEntity,
        formatType: String,
        durationSeconds: Int,
        quote: QuoteEntity? = null
    ) {
        viewModelScope.launch {
            val q = quote ?: todayQuote.value
            val surprise = SurpriseEntity(
                senderName = "Вы",
                receiverName = receiver.name,
                quoteText = q?.quoteText ?: "«Иногда именно потеря показывает нам, что по-настоящему важно.»",
                author = q?.author ?: "Пауло Коэльо",
                bookTitle = q?.bookTitle ?: "«Алхимик»",
                formatType = formatType,
                durationSeconds = durationSeconds,
                isOpened = false
            )
            repository.sendSurprise(surprise)
            _userMessage.value = "Сюрприз отправлен для ${receiver.name}!"
            _currentScreen.value = Screen.Friends
        }
    }

    fun speakText(text: String) {
        repository.speakRussian(text, ttsSpeed.value)
    }

    fun stopSpeaking() {
        repository.stopSpeech()
    }

    fun openTtsSettings() {
        repository.openTtsSettings()
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSpeech()
    }
}
