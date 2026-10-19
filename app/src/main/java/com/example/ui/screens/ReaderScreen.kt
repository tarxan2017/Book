package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BookEntity
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min

private data class ReaderTheme(
    val name: String,
    val bg: Color,
    val text: Color,
    val surface: Color,
    val accent: Color
)

private val themeWhite = ReaderTheme("Белый", Color(0xFFFDFBF7), Color(0xFF2C2C2C), Color(0xFFF2ECE4), Color(0xFFE5B064))
private val themeSepia = ReaderTheme("Сепия", Color(0xFFF4ECD8), Color(0xFF5B4636), Color(0xFFEBDDBD), Color(0xFFC09A60))
private val themeDark = ReaderTheme("Чёрный", Color(0xFF130F0C), Color(0xFFE8E1D9), Color(0xFF1E1712), Color(0xFFE5B064))

// МОЩНАЯ ОЧИСТКА ТЕКСТА ДЛЯ TTS
private fun cleanTextForTts(text: String): String {
    return text
        // 1. Убираем ссылки (http, https, www)
        .replace(Regex("""(https?://|www\.)\S+"""), " ")
        // 2. Убираем длинные линии и разделители (___, ===, ---)
        .replace(Regex("""[_*=\-—]{3,}"""), " ")
        // 3. Убираем переносы слов в конце строки (кни-\nга -> книга)
        .replace(Regex("""-\r?\n"""), "")
        // 4. Убираем не-буквы, не-цифры и не-пунктуацию
        .replace(Regex("""[^а-яА-ЯёЁa-zA-Z0-9\s.,!?;:\-—"«»()]"""), " ")
        // 5. Убираем номера страниц (одинокие цифры на новой строке)
        .replace(Regex("""(?m)^\s*\d+\s*$"""), "")
        // 6. Принудительно ставим пробел после знаков препинания, если его нет (для пауз)
        .replace(Regex("""([.,!?;:])(?=[а-яА-ЯёЁa-zA-Z0-9«(])"""), "$1 ")
        // 7. Убираем лишние пробелы и переносы строк
        .replace(Regex("""\s+"""), " ")
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val fontSize by viewModel.readerFontSize.collectAsStateWithLifecycle()
    
    val isSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val isPaused by viewModel.isTtsPaused.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()

    val book = currentBook ?: BookEntity(
        id = 1, title = "Книга не выбрана", author = "", coverRes = 0,
        totalChapters = 1, currentChapter = 1, progressPercent = 0,
        textContent = "Текст книги не загружен."
    )

    var currentTheme by remember { mutableStateOf(themeDark) }
    var currentFontFamily by remember { mutableStateOf<FontFamily>(InterFontFamily) }
    
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    
    var showVoiceDialog by remember { mutableStateOf(false) }

    var isAutoScrolling by remember { mutableStateOf(false) }

    val pages = remember(book.textContent, fontSize) {
        if (book.textContent.isBlank()) listOf("Книга пуста")
        else chunkText(book.textContent, chunkSize = (1200 * (17f / fontSize)).toInt())
    }
    
    val currentPages by rememberUpdatedState(pages)

    val initialPage = remember {
        if (book.progressPercent > 0 && pages.isNotEmpty()) {
            val calculatedPage = (pages.size * (book.progressPercent / 100f)).toInt()
            calculatedPage.coerceIn(0, pages.size - 1)
        } else 0
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    var playingPage by remember { mutableIntStateOf(initialPage) }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000)
            controlsVisible = false
        }
    }

    // Сохранение прогресса
    LaunchedEffect(pagerState.currentPage) {
        val progress = ((pagerState.currentPage + 1).toFloat() / pages.size * 100).toInt()
        if (progress != book.progressPercent) {
            viewModel.saveReadingProgress(book, progress)
        }
    }

    // Слушатель окончания аудио
    DisposableEffect(Unit) {
        viewModel.setOnTtsFinishedListener {
            if (playingPage < currentPages.size - 1) {
                // ВАЖНО: Ставим флаг ДО запуска анимации, чтобы читалка не думала, что это ручной свайп
                isAutoScrolling = true
                playingPage++
                viewModel.speakText(cleanTextForTts(currentPages[playingPage]))
                
                coroutineScope.launch {
                    pagerState.animateScrollToPage(playingPage)
                    isAutoScrolling = false
                }
            } else {
                viewModel.stopSpeaking()
            }
        }
        onDispose { viewModel.stopSpeaking() }
    }

    // Предзагрузка следующей страницы
    LaunchedEffect(playingPage, isSpeaking, selectedVoice) {
        if (isSpeaking && playingPage < pages.size - 1) {
            viewModel.preloadNextTts(cleanTextForTts(pages[playingPage + 1]))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.bg)
            .testTag("reader_screen")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 80.dp)
                    .graphicsLayer {
                        rotationY = pageOffset * 25f
                        cameraDistance = 12f * density
                        alpha = 1f - pageOffset.absoluteValue * 0.4f
                    }
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                    }
            ) {
                Text(
                    text = pages[page],
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = currentFontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.6f).sp
                    ),
                    color = currentTheme.text
                )
            }
        }

        // === ВЕРХНЯЯ ПАНЕЛЬ (ПЛЕЕР) ===
        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = currentTheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            viewModel.stopSpeaking()
                            viewModel.navigateTo(Screen.MyBook) 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = currentTheme.text)
                        }

                        if (isSpeaking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.seekSpeaking(-15000) }) {
                                    Icon(Icons.Default.FastRewind, "Назад 15с", tint = currentTheme.text)
                                }
                                
                                IconButton(onClick = { 
                                    if (isPaused) viewModel.resumeSpeaking() else viewModel.pauseSpeaking()
                                }) {
                                    Icon(
                                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        "Плей/Пауза",
                                        tint = currentTheme.accent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                IconButton(onClick = { viewModel.seekSpeaking(15000) }) {
                                    Icon(Icons.Default.FastForward, "Вперед 15с", tint = currentTheme.text)
                                }

                                IconButton(onClick = { viewModel.stopSpeaking() }) {
                                    Icon(Icons.Default.Stop, "Стоп", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            IconButton(onClick = {
                                controlsVisible = true
                                showVoiceDialog = true
                            }) {
                                Icon(Icons.Default.VolumeUp, "Озвучить", tint = currentTheme.accent)
                            }
                        }
                    }

                    if (isSpeaking) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Светлана", 
                                color = if (selectedVoice == "ru-RU-SvetlanaNeural") currentTheme.accent else currentTheme.text.copy(alpha=0.5f),
                                modifier = Modifier.clickable { 
                                    if (selectedVoice != "ru-RU-SvetlanaNeural") {
                                        viewModel.setTtsVoice("ru-RU-SvetlanaNeural")
                                        viewModel.stopSpeaking()
                                        viewModel.speakText(cleanTextForTts(pages[playingPage]), forceVoice = "ru-RU-SvetlanaNeural")
                                    }
                                }.padding(8.dp)
                            )
                            Text(
                                "Дмитрий", 
                                color = if (selectedVoice == "ru-RU-DmitryNeural") currentTheme.accent else currentTheme.text.copy(alpha=0.5f),
                                modifier = Modifier.clickable { 
                                    if (selectedVoice != "ru-RU-DmitryNeural") {
                                        viewModel.setTtsVoice("ru-RU-DmitryNeural")
                                        viewModel.stopSpeaking()
                                        viewModel.speakText(cleanTextForTts(pages[playingPage]), forceVoice = "ru-RU-DmitryNeural")
                                    }
                                }.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // === НИЖНЯЯ ПАНЕЛЬ ===
        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val progress = ((pagerState.currentPage + 1).toFloat() / pages.size * 100).toInt()
            Surface(
                color = currentTheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$progress% (${pagerState.currentPage + 1} из ${pages.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = currentTheme.text.copy(alpha = 0.7f)
                    )
                    Row {
                        IconButton(onClick = { showSearchSheet = true }) {
                            Icon(Icons.Default.Search, "Поиск", tint = currentTheme.accent)
                        }
                        IconButton(onClick = { showTocSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.List, "Страницы", tint = currentTheme.accent)
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, "Настройки", tint = currentTheme.accent)
                        }
                    }
                }
            }
        }
    }

    // === ДИАЛОГ ВЫБОРА ГОЛОСА ===
    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            containerColor = currentTheme.surface,
            titleContentColor = currentTheme.text,
            textContentColor = currentTheme.text.copy(alpha = 0.7f),
            title = {
                Text(
                    text = "Выберите голос",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text("Каким голосом будем читать эту книгу?")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        viewModel.setTtsVoice("ru-RU-SvetlanaNeural")
                        playingPage = pagerState.currentPage
                        viewModel.speakText(cleanTextForTts(pages[playingPage]), forceVoice = "ru-RU-SvetlanaNeural")
                        showVoiceDialog = false
                    }) {
                        Text("Светлана", color = currentTheme.accent, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = {
                        viewModel.setTtsVoice("ru-RU-DmitryNeural")
                        playingPage = pagerState.currentPage
                        viewModel.speakText(cleanTextForTts(pages[playingPage]), forceVoice = "ru-RU-DmitryNeural")
                        showVoiceDialog = false
                    }) {
                        Text("Дмитрий", color = currentTheme.accent, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Отмена", color = currentTheme.text.copy(alpha = 0.5f))
                }
            }
        )
    }

    // === ШТОРКА ПОИСКА ===
    if (showSearchSheet) {
        var query by remember { mutableStateOf("") }
        val searchResults = remember(query) {
            if (query.length < 2) emptyList()
            else pages.mapIndexedNotNull { index, pageText ->
                if (pageText.contains(query, ignoreCase = true)) index else null
            }
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = sheetState,
            containerColor = currentTheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("Поиск по тексту", color = currentTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Введите слово...", color = currentTheme.text.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = currentTheme.text,
                        unfocusedTextColor = currentTheme.text,
                        focusedBorderColor = currentTheme.accent,
                        unfocusedBorderColor = currentTheme.text.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (query.length >= 2 && searchResults.isEmpty()) {
                    Text("Ничего не найдено", color = currentTheme.text, modifier = Modifier.padding(8.dp))
                } else if (searchResults.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.height(250.dp)) {
                        items(searchResults) { pageIndex ->
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSearchSheet = false
                                        coroutineScope.launch { pagerState.scrollToPage(pageIndex) }
                                        if (isSpeaking) {
                                            playingPage = pageIndex
                                            viewModel.stopSpeaking()
                                            viewModel.speakText(cleanTextForTts(pages[pageIndex]), forceVoice = selectedVoice)
                                        }
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Column {
                                    Text("Страница ${pageIndex + 1}", color = currentTheme.accent, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = pages[pageIndex].take(60) + "...",
                                        color = currentTheme.text.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // === ШТОРКА ОГЛАВЛЕНИЯ ===
    if (showTocSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState = sheetState,
            containerColor = currentTheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("Выбор страницы", color = currentTheme.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(pages.size) { pageIndex ->
                        Surface(
                            color = if (pageIndex == pagerState.currentPage) currentTheme.accent.copy(alpha = 0.2f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showTocSheet = false
                                    coroutineScope.launch { pagerState.scrollToPage(pageIndex) }
                                    if (isSpeaking) {
                                        playingPage = pageIndex
                                        viewModel.stopSpeaking()
                                        viewModel.speakText(cleanTextForTts(pages[pageIndex]), forceVoice = selectedVoice)
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Страница ${pageIndex + 1}", color = currentTheme.text)
                                if (pageIndex == pagerState.currentPage) {
                                    Text("Текущая", color = currentTheme.accent, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // === ШТОРКА НАСТРОЕК ===
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = currentTheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Text("Тема", color = currentTheme.text, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeChip(themeWhite, currentTheme.name) { currentTheme = themeWhite }
                    ThemeChip(themeSepia, currentTheme.name) { currentTheme = themeSepia }
                    ThemeChip(themeDark, currentTheme.name) { currentTheme = themeDark }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Размер", color = currentTheme.text, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { if (fontSize > 12f) viewModel.setReaderFontSize(fontSize - 1f) }, colors = ButtonDefaults.buttonColors(containerColor = currentTheme.bg), modifier = Modifier.size(40.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("-", color = currentTheme.text, fontSize = 20.sp) }
                        Text(text = fontSize.toInt().toString(), color = currentTheme.text, modifier = Modifier.padding(horizontal = 16.dp))
                        Button(onClick = { if (fontSize < 32f) viewModel.setReaderFontSize(fontSize + 1f) }, colors = ButtonDefaults.buttonColors(containerColor = currentTheme.bg), modifier = Modifier.size(40.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+", color = currentTheme.text, fontSize = 20.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Шрифт", color = currentTheme.text, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FontChip("Inter", currentFontFamily == InterFontFamily, currentTheme) { currentFontFamily = InterFontFamily }
                    FontChip("Playfair", currentFontFamily == PlayfairDisplayFontFamily, currentTheme) { currentFontFamily = PlayfairDisplayFontFamily }
                    FontChip("Serif", currentFontFamily == FontFamily.Serif, currentTheme) { currentFontFamily = FontFamily.Serif }
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(theme: ReaderTheme, currentThemeName: String, onClick: () -> Unit) {
    val isSelected = theme.name == currentThemeName
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) theme.accent else theme.bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.accent),
        modifier = Modifier.clickable(onClick = onClick).padding(1.dp)
    ) {
        Text(text = theme.name, color = if (isSelected) Color.White else theme.text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FontChip(name: String, isSelected: Boolean, theme: ReaderTheme, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) theme.accent.copy(alpha = 0.2f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) theme.accent else theme.text.copy(alpha = 0.2f)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            color = if (isSelected) theme.accent else theme.text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun chunkText(text: String, chunkSize: Int): List<String> {
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        var end = min(start + chunkSize, text.length)
        
        if (end < text.length) {
            val lastSentenceEnd = text.lastIndexOfAny(charArrayOf('.', '!', '?', '\n'), end)
            val lastSpace = text.lastIndexOf(' ', end)
            
            if (lastSentenceEnd > start + (chunkSize * 0.7).toInt()) {
                end = lastSentenceEnd + 1
            } else if (lastSpace > start) {
                end = lastSpace
            }
        }
        
        chunks.add(text.substring(start, end).trim())
        start = end
    }
    return chunks
}
