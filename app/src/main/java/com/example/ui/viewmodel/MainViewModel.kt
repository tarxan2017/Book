package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
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
import com.example.data.remote.model.ChatMessage
import com.example.data.remote.model.PublicBook
import com.example.data.remote.model.UserProfile
import com.example.repository.LivingPageRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

const val ADMIN_UID = "AcWTQ3Gf3FYquad3wfSk1CcEvcI3"

sealed class Screen {
    object Home : Screen()
    object MyBook : Screen()
    object BookList : Screen()
    object Friends : Screen()
    object Settings : Screen()
    object Reader : Screen()
    object PublicLibrary : Screen()
    data class PublicBookDetail(val book: PublicBook) : Screen()
    data class VideoPlayer(val quote: QuoteEntity) : Screen()
    data class SendSurprise(val defaultFriend: FriendEntity? = null) : Screen()
    data class ReceiveSurprise(val surprise: SurpriseEntity) : Screen()
    data class Chat(val friend: UserProfile) : Screen()
    object OpenBookAnimation : Screen()
    object GiftAnimation : Screen()

    object AdminPanel : Screen()
    object AdminLibrary : Screen()
    object AdminModerators : Screen()
    object AdminStats : Screen()
    data class AdminBookEdit(val book: PublicBook) : Screen()
    object NotificationSettings : Screen()
}

@OptIn(ExperimentalCoroutinesApi::class)
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

    val isAdmin: StateFlow<Boolean> = currentUser
        .map { user -> user?.uid == ADMIN_UID }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val moderators: StateFlow<List<String>> = repository.observeModerators()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canManageBooks: StateFlow<Boolean> = combine(
        isAdmin, moderators, currentUser
    ) { admin, mods, user ->
        admin || (user != null && mods.contains(user.uid))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cloudFriends: StateFlow<List<UserProfile>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else repository.observeFriends(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityUsers: StateFlow<List<UserProfile>> = repository.observeCommunityUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _foundUser = MutableStateFlow<UserProfile?>(null)
    val foundUser: StateFlow<UserProfile?> = _foundUser.asStateFlow()

    private val _isSearchingFriend = MutableStateFlow(false)
    val isSearchingFriend: StateFlow<Boolean> = _isSearchingFriend.asStateFlow()

    val publicBooks: StateFlow<List<PublicBook>> = repository.observePublicBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPublicBooks: StateFlow<List<PublicBook>> = repository.observeRecentPublicBooks(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _coverUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val coverUrls: StateFlow<Map<String, String>> = _coverUrls.asStateFlow()

    private val _selectedPublicBook = MutableStateFlow<PublicBook?>(null)
    val selectedPublicBook: StateFlow<PublicBook?> = _selectedPublicBook.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _isUpdatingBook = MutableStateFlow(false)
    val isUpdatingBook: StateFlow<Boolean> = _isUpdatingBook.asStateFlow()

    private val _editingBook = MutableStateFlow<PublicBook?>(null)
    val editingBook: StateFlow<PublicBook?> = _editingBook.asStateFlow()

    val notifyBooks: StateFlow<Boolean> = repository.notifyBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifyFriends: StateFlow<Boolean> = repository.notifyFriends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dndEnabled: StateFlow<Boolean> = repository.dndEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedVoice = MutableStateFlow("ru-RU-SvetlanaNeural")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    val isTtsPaused: StateFlow<Boolean> = repository.ttsManager.isPaused
    val isTtsSpeaking: StateFlow<Boolean> = repository.ttsManager.isSpeaking

    val ttsSpeed: StateFlow<Float> = repository.ttsSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    init {
        viewModelScope.launch {
            publicBooks.collect { books ->
                val currentMap = _coverUrls.value.toMutableMap()
                var changed = false

                for (book in books) {
                    if (book.coverFileId.isNotBlank() && !currentMap.containsKey(book.coverFileId)) {
                        val url = repository.getCoverUrl(book.coverFileId)
                        if (url != null) {
                            currentMap[book.coverFileId] = url
                            changed = true
                        }
                    }
                }

                if (changed) _coverUrls.value = currentMap
            }
        }

        viewModelScope.launch {
            currentUser.collect { user ->
                if (user != null) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            viewModelScope.launch {
                                repository.saveFcmToken(user.uid, token)
                            }
                        }
                    }
                }
            }
        }
    }

    fun setTtsVoice(voice: String) {
        _selectedVoice.value = voice
    }

    fun speakText(text: String, forceVoice: String? = null) {
        repository.ttsManager.speak(
            text = text, 
            voice = forceVoice ?: _selectedVoice.value, 
            speed = ttsSpeed.value
        )
    }

    fun preloadNextTts(text: String) {
        repository.ttsManager.preloadNext(text, _selectedVoice.value, ttsSpeed.value)
    }

    fun pauseSpeaking() {
        repository.ttsManager.pause()
    }

    fun resumeSpeaking() {
        repository.ttsManager.resume()
    }

    fun seekSpeaking(deltaMs: Int) {
        repository.ttsManager.seek(deltaMs)
    }

    fun setOnTtsFinishedListener(listener: () -> Unit) {
        repository.ttsManager.onAudioFinishedListener = listener
    }

    fun stopSpeaking() {
        repository.stopSpeech()
    }

    fun getCoverUrl(coverFileId: String): String? =
        if (coverFileId.isBlank()) null else _coverUrls.value[coverFileId]

    fun openPublicBook(book: PublicBook) {
        _selectedPublicBook.value = book
        _currentScreen.value = Screen.PublicBookDetail(book)
    }

    fun openPublicLibrary() {
        _currentScreen.value = Screen.PublicLibrary
    }

    fun downloadPublicBook(context: Context, book: PublicBook) {
        viewModelScope.launch {
            _isDownloading.value = true
            val result = repository.downloadPublicBook(context, book)
            _isDownloading.value = false

            result.onSuccess {
                _userMessage.value = "Книга скачана! Открой «Мою книгу»."
                _currentScreen.value = Screen.MyBook
            }.onFailure { error ->
                _userMessage.value = "Ошибка скачивания: ${error.localizedMessage}"
            }
        }
    }

    fun findUserByUid(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _isSearchingFriend.value = true
            _foundUser.value = null
            val profile = repository.findUserByUid(uid.trim())
            _foundUser.value = profile
            _isSearchingFriend.value = false
            if (profile == null) {
                _userMessage.value = "Пользователь с таким ID не найден"
            } else if (profile.userId == currentUser.value?.uid) {
                _userMessage.value = "Это ваш ID"
                _foundUser.value = null
            } else if (cloudFriends.value.any { it.userId == profile.userId }) {
                _userMessage.value = "Этот пользователь уже у вас в друзьях"
                _foundUser.value = null
            }
        }
    }

    fun addFriendByUid(friendUid: String) {
        val myUid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            repository.addFriend(myUid, friendUid)
            _foundUser.value = null
            _userMessage.value = "Друг добавлен!"
        }
    }

    private val _chatFriend = MutableStateFlow<UserProfile?>(null)
    val chatFriend: StateFlow<UserProfile?> = _chatFriend.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = _chatFriend
        .flatMapLatest { friend ->
            val me = currentUser.value
            if (friend == null || me == null) flowOf(emptyList())
            else repository.observeMessages(me.uid, friend.userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun inviteFriend(context: Context) {
        val myUid = currentUser.value?.uid ?: return
        val inviteText = "Мой ID в «Живой Странице»: $myUid\n\nСкачай приложение и вставь этот ID в разделе «Друзья»."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться ID"))
    }

    fun openChatWith(friend: UserProfile) {
        _chatFriend.value = friend
        _currentScreen.value = Screen.Chat(friend)
    }

    fun sendChatMessage(text: String) {
        val friend = _chatFriend.value ?: return
        val me = currentUser.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(me.uid, friend.userId, text)
        }
    }

    fun signInWithGoogle(context: Context) {
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

    fun setNotifyBooks(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotifyBooks(enabled)
        }
    }

    fun setNotifyFriends(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotifyFriends(enabled)
        }
    }

    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDndEnabled(enabled)
        }
    }

    fun addModerator(uid: String, name: String) {
        viewModelScope.launch {
            repository.addModerator(uid, name)
            _userMessage.value = "Модератор добавлен"
        }
    }

    fun removeModerator(uid: String) {
        viewModelScope.launch {
            repository.removeModerator(uid)
            _userMessage.value = "Модератор удалён"
        }
    }

    fun setCurrentBook(book: BookEntity) {
        viewModelScope.launch {
            repository.setCurrentBook(book)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            _userMessage.value = "Книга удалена: ${book.title}"
        }
    }

    fun updateBookDetails(context: Context, book: BookEntity, newTitle: String, newAuthor: String, newCoverUri: String) {
        viewModelScope.launch {
            var finalCoverUri = newCoverUri
            
            if (newCoverUri.isNotBlank() && newCoverUri.startsWith("content://")) {
                try {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(newCoverUri))
                    val file = File(context.filesDir, "cover_${book.id}_${System.currentTimeMillis()}.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    finalCoverUri = file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val updated = book.copy(
                title = newTitle.trim().ifBlank { "Без названия" },
                author = newAuthor.trim().ifBlank { "Неизвестный автор" },
                coverUri = finalCoverUri
            )
            repository.updateBook(updated)
            _userMessage.value = "Книга обновлена"
        }
    }

    fun saveReadingProgress(book: BookEntity, percent: Int) {
        viewModelScope.launch {
            repository.updateBook(book.copy(progressPercent = percent))
        }
    }

    fun openAdminPanel() { _currentScreen.value = Screen.AdminPanel }
    fun openAdminLibrary() { _currentScreen.value = Screen.AdminLibrary }
    fun openAdminModerators() { _currentScreen.value = Screen.AdminModerators }
    fun openAdminStats() { _currentScreen.value = Screen.AdminStats }

    fun openBookEditor(book: PublicBook) {
        _editingBook.value = book
        _currentScreen.value = Screen.AdminBookEdit(book)
    }

    fun deletePublicBook(bookId: String) {
        if (bookId.isBlank()) return
        if (!canManageBooks.value) {
            _userMessage.value = "Недостаточно прав для удаления"
            return
        }
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                repository.deletePublicBook(bookId)
                _userMessage.value = "Книга удалена из общей библиотеки"
                _currentScreen.value = Screen.AdminLibrary
            } catch (e: Exception) {
                _userMessage.value = "Ошибка удаления: ${e.localizedMessage}"
            } finally {
                _isDeleting.value = false
            }
        }
    }

    fun updatePublicBook(book: PublicBook) {
        if (book.id.isBlank()) return
        if (!canManageBooks.value) {
            _userMessage.value = "Недостаточно прав для редактирования"
            return
        }
        viewModelScope.launch {
            _isUpdatingBook.value = true
            try {
                repository.updatePublicBook(book)
                _editingBook.value = book
                _userMessage.value = "Изменения сохранены"
            } catch (e: Exception) {
                _userMessage.value = "Ошибка обновления: ${e.localizedMessage}"
            } finally {
                _isUpdatingBook.value = false
            }
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

            var finalCoverUri = coverUri
            if (coverUri.isNotBlank() && coverUri.startsWith("content://")) {
                try {
                    val inputStream = context.contentResolver.openInputStream(Uri.parse(coverUri))
                    val file = File(context.filesDir, "cover_new_${System.currentTimeMillis()}.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    finalCoverUri = file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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
                    coverUri = finalCoverUri
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

    fun clearMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSpeech()
    }
}
