package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.datastore.DailyGenerationServiceImpl
import com.example.data.datastore.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.data.remote.GeminiApiClient
import com.example.data.remote.TelegramBotService
import com.example.data.tts.RussianTtsManager
import com.example.repository.LivingPageRepository
import com.example.ui.components.LivingPageBottomNavBar
import com.example.ui.screens.AdminBookEditScreen
import com.example.ui.screens.AdminLibraryScreen
import com.example.ui.screens.AdminModeratorsScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AdminStatsScreen
import com.example.ui.screens.BookListScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.GiftAnimationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyBookScreen
import com.example.ui.screens.OpenBookAnimationScreen
import com.example.ui.screens.PublicBookDetailScreen
import com.example.ui.screens.PublicLibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.ReceiveSurpriseScreen
import com.example.ui.screens.SendSurpriseScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.LivingPageTheme
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private lateinit var ttsManager: RussianTtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)
        val dailyGenerationService = DailyGenerationServiceImpl(applicationContext)
        val geminiApiClient = GeminiApiClient()
        ttsManager = RussianTtsManager(applicationContext)
        val cloudFirestoreService = com.example.data.remote.CloudFirestoreService(applicationContext)
        val authManager = com.example.data.auth.GoogleAuthManager(applicationContext)
        val telegramBotService = TelegramBotService(applicationContext)

        val repository = LivingPageRepository(
            dao = database.livingPageDao(),
            preferencesManager = preferencesManager,
            dailyGenerationService = dailyGenerationService,
            geminiApiClient = geminiApiClient,
            ttsManager = ttsManager,
            cloudFirestoreService = cloudFirestoreService,
            authManager = authManager,
            telegramBotService = telegramBotService
        )

        setContent {
            val mainViewModel = remember {
                MainViewModel(repository)
            }

            val isDarkTheme by mainViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val currentScreen by mainViewModel.currentScreen.collectAsStateWithLifecycle()
            val userMessage by mainViewModel.userMessage.collectAsStateWithLifecycle()
            val showLimitDialog by mainViewModel.showLimitReachedDialog.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(userMessage) {
                userMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    mainViewModel.clearMessage()
                }
            }

            LivingPageTheme(darkTheme = isDarkTheme) {
                val showBottomBar = currentScreen is Screen.Home ||
                        currentScreen is Screen.MyBook ||
                        currentScreen is Screen.Friends ||
                        currentScreen is Screen.Settings

                BackHandler(enabled = currentScreen !is Screen.Home) {
                    when (currentScreen) {
                        is Screen.Reader -> mainViewModel.navigateTo(Screen.MyBook)
                        is Screen.BookList -> mainViewModel.navigateTo(Screen.Settings)
                        is Screen.Chat -> mainViewModel.navigateTo(Screen.Friends)
                        is Screen.PublicLibrary -> mainViewModel.navigateTo(Screen.Home)
                        is Screen.PublicBookDetail -> mainViewModel.navigateTo(Screen.PublicLibrary)
                        is Screen.VideoPlayer -> mainViewModel.navigateTo(Screen.Home)
                        is Screen.SendSurprise -> mainViewModel.navigateTo(Screen.Friends)
                        is Screen.ReceiveSurprise -> mainViewModel.navigateTo(Screen.Friends)
                        is Screen.OpenBookAnimation, is Screen.GiftAnimation -> mainViewModel.navigateTo(Screen.Home)
                        // === АДМИН-ПАНЕЛЬ ===
                        is Screen.AdminPanel -> mainViewModel.navigateTo(Screen.Settings)
                        is Screen.AdminLibrary -> mainViewModel.navigateTo(Screen.AdminPanel)
                        is Screen.AdminModerators -> mainViewModel.navigateTo(Screen.AdminPanel)
                        is Screen.AdminStats -> mainViewModel.navigateTo(Screen.AdminPanel)
                        is Screen.AdminBookEdit -> mainViewModel.navigateTo(Screen.AdminLibrary)
                        else -> mainViewModel.navigateTo(Screen.Home)
                    }
                }

                if (showLimitDialog) {
                    AlertDialog(
                        onDismissRequest = { mainViewModel.dismissLimitDialog() },
                        title = {
                            Text(
                                text = "Лимит на сегодня исчерпан",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlayfairDisplayFontFamily),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text = {
                            Text(
                                text = "Вы использовали все 5 видео-генераций на сегодня. Создание новых роликов временно заблокировано и обновится завтра.\n\nВы можете продолжать читать книги в уютной читалке или делиться уже сохраненными цитатами.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { mainViewModel.dismissLimitDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Понятно")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { mainViewModel.resetDailyLimitForDemo() }
                            ) {
                                Text("Сбросить лимит (Тест)")
                            }
                        }
                    )
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            LivingPageBottomNavBar(
                                currentScreen = currentScreen,
                                onNavigate = { screen -> mainViewModel.navigateTo(screen) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> HomeScreen(viewModel = mainViewModel)
                            is Screen.MyBook -> MyBookScreen(viewModel = mainViewModel)
                            is Screen.BookList -> BookListScreen(viewModel = mainViewModel)
                            is Screen.Friends -> FriendsScreen(viewModel = mainViewModel)
                            is Screen.Settings -> SettingsScreen(viewModel = mainViewModel)
                            is Screen.Reader -> ReaderScreen(viewModel = mainViewModel)
                            is Screen.PublicLibrary -> PublicLibraryScreen(viewModel = mainViewModel)
                            is Screen.PublicBookDetail -> PublicBookDetailScreen(
                                viewModel = mainViewModel,
                                book = screen.book
                            )
                            is Screen.Chat -> ChatScreen(
                                viewModel = mainViewModel,
                                friendName = screen.friend.displayName.ifBlank { "Читатель" },
                                friendPhotoUrl = screen.friend.photoUrl
                            )
                            is Screen.VideoPlayer -> VideoPlayerScreen(viewModel = mainViewModel, quote = screen.quote)
                            is Screen.SendSurprise -> SendSurpriseScreen(viewModel = mainViewModel, defaultFriend = screen.defaultFriend)
                            is Screen.ReceiveSurprise -> ReceiveSurpriseScreen(viewModel = mainViewModel, surprise = screen.surprise)
                            is Screen.OpenBookAnimation -> OpenBookAnimationScreen(viewModel = mainViewModel)
                            is Screen.GiftAnimation -> GiftAnimationScreen(viewModel = mainViewModel)
                            // === АДМИН-ПАНЕЛЬ ===
                            is Screen.AdminPanel -> AdminPanelScreen(viewModel = mainViewModel)
                            is Screen.AdminLibrary -> AdminLibraryScreen(viewModel = mainViewModel)
                            is Screen.AdminModerators -> AdminModeratorsScreen(viewModel = mainViewModel)
                            is Screen.AdminStats -> AdminStatsScreen(viewModel = mainViewModel)
                            is Screen.AdminBookEdit -> AdminBookEditScreen(
                                viewModel = mainViewModel,
                                book = screen.book
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
    }
}
