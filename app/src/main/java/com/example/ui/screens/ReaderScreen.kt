package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BookEntity
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min

// Темы для читалки
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val fontSize by viewModel.readerFontSize.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()

    val book = currentBook ?: BookEntity(
        id = 1, title = "Алхимик", author = "Пауло Коэльо", coverRes = 0,
        totalChapters = 1, currentChapter = 1, progressPercent = 0,
        textContent = "Текст книги не загружен."
    )

    // Состояние читалки
    var currentTheme by remember { mutableStateOf(themeDark) }
    var currentFontFamily by remember { mutableStateOf<FontFamily>(InterFontFamily) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // Состояние поиска
    var searchQuery by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    // Разбивка текста на страницы
    val pages = remember(book.textContent, fontSize) {
        if (book.textContent.isBlank()) listOf("Книга пуста")
        else chunkText(book.textContent, chunkSize = (1200 * (17f / fontSize)).toInt())
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    
    // Результаты поиска (индекс страницы + отрывок)
    val searchResults = remember(searchQuery, pages) {
        if (searchQuery.length < 2) emptyList()
        else pages.mapIndexedNotNull { index, text ->
            if (text.contains(searchQuery, ignoreCase = true)) {
                val snippetStart = maxOf(0, text.indexOf(searchQuery, ignoreCase = true) - 20)
                val snippetEnd = minOf(text.length, snippetStart + 80)
                val snippet = "..." + text.substring(snippetStart, snippetEnd).replace("\n", " ") + "..."
                Pair(index, snippet)
            } else null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.bg)
            .statusBarsPadding() // Чтобы не залезало под статус-бар
            .testTag("reader_screen")
    ) {
        // === ОСНОВНОЙ ТЕКСТ (СВАЙПЫ) ===
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 40.dp)
                    .graphicsLayer {
                        // 3D-эффект
                        rotationY = pageOffset * 25f
                        cameraDistance = 12f * density
                        alpha = 1f - pageOffset.absoluteValue * 0.4f
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showSettingsSheet = true }
                        )
                    }
            ) {
                // Название книги убрано, сразу идет текст
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

        // === ВЕРХНЯЯ ПАНЕЛЬ ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.MyBook) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = currentTheme.text.copy(alpha = 0.7f)
                )
            }

            // Название убрано, чтобы не дублировалось. Просто пустое место.
            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = {
                val textToRead = pages.getOrNull(pagerState.currentPage) ?: ""
                if (isSpeaking) viewModel.stopSpeaking() else viewModel.speakText(textToRead)
            }) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.VolumeUp,
                    contentDescription = "Озвучка",
                    tint = currentTheme.accent
                )
            }
        }

        // === НИЖНЯЯ ПАНЕЛЬ (ПРОГРЕСС) ===
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            val progress = ((pagerState.currentPage + 1).toFloat() / pages.size * 100).toInt()
            
            Text(
                text = "$progress% (${pagerState.currentPage + 1} из ${pages.size})",
                style = MaterialTheme.typography.labelMedium,
                color = currentTheme.text.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }

    // === ШТОРКА НАСТРОЕК ===
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { 
                showSettingsSheet = false 
                searchQuery = "" // Сбрасываем поиск при закрытии
            },
            sheetState = sheetState,
            containerColor = currentTheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                // --- ПОИСК ---
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск по тексту...", color = currentTheme.text.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = currentTheme.accent) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentTheme.accent,
                        unfocusedBorderColor = currentTheme.text.copy(alpha = 0.2f),
                        focusedContainerColor = currentTheme.bg.copy(alpha = 0.5f),
                        unfocusedContainerColor = currentTheme.bg.copy(alpha = 0.5f),
                        focusedTextColor = currentTheme.text,
                        unfocusedTextColor = currentTheme.text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Если есть поисковый запрос — показываем результаты
                if (searchQuery.length >= 2) {
                    if (searchResults.isEmpty()) {
                        Text("Ничего не найдено", color = currentTheme.text.copy(alpha = 0.6f))
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1),
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults.size) { i ->
                                val (pageIndex, snippet) = searchResults[i]
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = currentTheme.bg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pageIndex)
                                                showSettingsSheet = false
                                                searchQuery = ""
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Страница ${pageIndex + 1}", color = currentTheme.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(snippet, color = currentTheme.text, fontSize = 14.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                } else {
                    // --- ОГЛАВЛЕНИЕ (СЕТКА СТРАНИЦ) ---
                    Text("Перейти на страницу", color = currentTheme.text, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pages.size) { index ->
                            val isCurrent = pagerState.currentPage == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) currentTheme.accent else currentTheme.bg,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                            showSettingsSheet = false
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isCurrent) Color.White else currentTheme.text,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ТЕМЫ ---
                    Text("Тема оформления", color = currentTheme.text, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeChip(themeWhite, currentTheme.name) { currentTheme = themeWhite }
                        ThemeChip(themeSepia, currentTheme.name) { currentTheme = themeSepia }
                        ThemeChip(themeDark, currentTheme.name) { currentTheme = themeDark }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- РАЗМЕР ШРИФТА ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Размер шрифта", color = currentTheme.text, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { if (fontSize > 12f) viewModel.setReaderFontSize(fontSize - 1f) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.bg),
                                modifier = Modifier.size(40.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("-", color = currentTheme.text, fontSize = 20.sp) }
                            
                            Text(
                                text = fontSize.toInt().toString(),
                                color = currentTheme.text,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            
                            Button(
                                onClick = { if (fontSize < 32f) viewModel.setReaderFontSize(fontSize + 1f) },
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.bg),
                                modifier = Modifier.size(40.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) { Text("+", color = currentTheme.text, fontSize = 20.sp) }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- ШРИФТ ---
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
}

@Composable
private fun ThemeChip(theme: ReaderTheme, currentThemeName: String, onClick: () -> Unit) {
    val isSelected = theme.name == currentThemeName
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) theme.accent else theme.bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.accent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(1.dp)
    ) {
        Text(
            text = theme.name,
            color = if (isSelected) Color.White else theme.text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium
        )
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

// Функция разбивки текста на страницы
private fun chunkText(text: String, chunkSize: Int): List<String> {
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        var end = min(start + chunkSize, text.length)
        if (end < text.length) {
            val lastSpace = text.lastIndexOf(' ', end)
            if (lastSpace > start) {
                end = lastSpace
            }
        }
        chunks.add(text.substring(start, end).trim())
        start = end
    }
    return chunks
}
