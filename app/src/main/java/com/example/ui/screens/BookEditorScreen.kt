package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.entity.BookEntity
import com.example.data.remote.model.BookCategories
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookEditorScreen(
    viewModel: MainViewModel,
    draftBook: BookEntity? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var bookTitle by remember { mutableStateOf(draftBook?.title ?: "") }
    var bookAuthor by remember { mutableStateOf(draftBook?.author ?: "") }
    var bookCategory by remember { mutableStateOf("Роман") }
    var coverUri by remember { mutableStateOf(draftBook?.coverUri ?: "") }

    var currentStep by remember { mutableIntStateOf(if (draftBook == null) 0 else 1) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) coverUri = uri.toString() }

    if (currentStep == 0) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.Center)
                    .offset(x = 120.dp, y = 50.dp)
                    .alpha(0.02f),
                tint = MaterialTheme.colorScheme.onBackground
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MyBook) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = "Создание книги",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(160.dp, 220.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { coverPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri.isNotBlank()) {
                        AsyncImage(model = coverUri, contentDescription = "Обложка", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, "Добавить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Обложка", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("Название книги") },
                    leadingIcon = { Icon(Icons.Default.Create, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bookAuthor,
                    onValueChange = { bookAuthor = it },
                    label = { Text("Автор") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Жанр", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookCategories.ALL.forEach { genre ->
                        FilterChip(
                            selected = bookCategory == genre,
                            onClick = { bookCategory = genre },
                            label = { Text(genre) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = { currentStep = 1 },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = bookTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Начать писать", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    } else {
        var controlsVisible by remember { mutableStateOf(true) }
        var showSettings by remember { mutableStateOf(false) }
        var showPreview by remember { mutableStateOf(false) }
        var editorTheme by remember { mutableStateOf("dark") }
        var fontSize by remember { mutableFloatStateOf(20f) }
        var showMenu by remember { mutableStateOf(false) }

        val isPublished = false 

        val initialPages = remember {
            if (draftBook != null && draftBook.textContent.isNotBlank()) {
                draftBook.textContent.split("\n\n---\n\n").map { TextFieldValue(it) }.toMutableStateList()
            } else {
                mutableStateListOf(TextFieldValue(""))
            }
        }
        val pages = remember { initialPages }
        val pagerState = rememberPagerState(pageCount = { pages.size })
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            if (pages.isNotEmpty()) focusRequester.requestFocus()
        }

        val currentText = pages.getOrNull(pagerState.currentPage)?.text ?: ""
        val wordCount = currentText.split(Regex("\\s+")).filter { it.isNotBlank() }.count()

        val (bgColor, textColor) = when (editorTheme) {
            "light" -> Color(0xFFFDFBF7) to Color(0xFF1E1E1E)
            "sepia" -> Color(0xFFF4ECD8) to Color(0xFF5B4636)
            else -> MaterialTheme.colorScheme.background to MaterialTheme.colorScheme.onBackground
        }

        fun applyFormatting(format: String) {
            val currentVal = pages[pagerState.currentPage]
            val start = currentVal.selection.start
            val end = currentVal.selection.end

            if (start != end) {
                val selected = currentVal.text.substring(start, end)
                val formatted = when(format) {
                    "bold" -> "**$selected**"
                    "italic" -> "*$selected*"
                    "center" -> "\n<center>$selected</center>\n"
                    else -> selected
                }
                val newText = currentVal.text.replaceRange(start, end, formatted)
                pages[pagerState.currentPage] = currentVal.copy(text = newText, selection = TextRange(start + formatted.length))
            } else {
                val formatted = when(format) {
                    "bold" -> "****"
                    "italic" -> "**"
                    "center" -> "\n<center></center>\n"
                    else -> ""
                }
                val newText = currentVal.text.replaceRange(start, start, formatted)
                pages[pagerState.currentPage] = currentVal.copy(text = newText, selection = TextRange(start + formatted.length / 2))
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                    controlsVisible = !controlsVisible
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { pageIndex ->
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = pageOffset * 15f
                            alpha = lerp(1f, 0.5f, pageOffset)
                            cameraDistance = 12f * density
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 100.dp)
                ) {
                    BasicTextField(
                        value = pages[pageIndex],
                        onValueChange = { newVal -> pages[pageIndex] = newVal },
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.5).sp,
                            fontFamily = FontFamily.Serif
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_text_field_$pageIndex")
                            .focusRequester(if (pageIndex == 0) focusRequester else remember { FocusRequester() }),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            if (draftBook == null) currentStep = 0 else viewModel.navigateTo(Screen.MyBook) 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Text(
                            text = bookTitle.ifBlank { "Без названия" },
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Меню", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isPublished) "Обновить книгу" else "Опубликовать (Скоро)") },
                                    onClick = { showMenu = false },
                                    enabled = false
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Сохранить и выйти") },
                                    onClick = {
                                        showMenu = false
                                        val finalText = pages.joinToString("\n\n---\n\n") { it.text }
                                        // ИСПРАВЛЕНО: Передаем draftBook, чтобы избежать дубликатов
                                        viewModel.saveDraft(context, draftBook, bookTitle, bookAuthor, finalText, coverUri)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 8.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { applyFormatting("bold") }) {
                                Icon(Icons.Default.FormatBold, "Жирный", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { applyFormatting("italic") }) {
                                Icon(Icons.Default.FormatItalic, "Курсив", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { applyFormatting("center") }) {
                                Icon(Icons.Default.FormatAlignCenter, "По центру", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            // НОВАЯ КНОПКА ПРЕДПРОСМОТРА
                            IconButton(onClick = { 
                                focusManager.clearFocus()
                                showPreview = true 
                            }) {
                                Icon(Icons.Default.Visibility, "Предпросмотр", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Стр. ${pagerState.currentPage + 1} из ${pages.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    pages.add(TextFieldValue(""))
                                    coroutineScope.launch { pagerState.animateScrollToPage(pages.size - 1) }
                                }) {
                                    Icon(Icons.Default.Add, "Добавить страницу", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { showSettings = true }) {
                                    Icon(Icons.Default.Settings, "Настройки", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (showSettings) {
                AlertDialog(
                    onDismissRequest = { showSettings = false },
                    title = { Text("Настройки редактора") },
                    text = {
                        Column {
                            Text("Тема оформления", fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = editorTheme == "dark", onClick = { editorTheme = "dark" }, label = { Text("Темная") })
                                FilterChip(selected = editorTheme == "light", onClick = { editorTheme = "light" }, label = { Text("Светлая") })
                                FilterChip(selected = editorTheme == "sepia", onClick = { editorTheme = "sepia" }, label = { Text("Сепия") })
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Размер шрифта: ${fontSize.toInt()}", fontWeight = FontWeight.Bold)
                            Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 14f..32f)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettings = false }) { Text("Готово") }
                    }
                )
            }

            // ДИАЛОГ ПРЕДПРОСМОТРА
            if (showPreview) {
                Dialog(onDismissRequest = { showPreview = false }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = bgColor,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Предпросмотр", style = MaterialTheme.typography.titleLarge, color = textColor, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showPreview = false }) {
                                    Icon(Icons.Default.Close, "Закрыть", tint = textColor)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Парсим и отображаем текст
                                val rawText = pages[pagerState.currentPage].text
                                val lines = rawText.split("\n")
                                
                                lines.forEach { line ->
                                    val isCentered = line.contains("<center>") && line.contains("</center>")
                                    val cleanLine = line.replace("<center>", "").replace("</center>", "").trim()
                                    
                                    if (cleanLine.isNotBlank()) {
                                        val annotatedString = buildAnnotatedString {
                                            var currentIndex = 0
                                            val regex = Regex("""\*\*(.*?)\*\*|\*(.*?)\*""")
                                            
                                            regex.findAll(cleanLine).forEach { match ->
                                                append(cleanLine.substring(currentIndex, match.range.first))
                                                if (match.groupValues[1].isNotEmpty()) {
                                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                                        append(match.groupValues[1])
                                                    }
                                                } else if (match.groupValues[2].isNotEmpty()) {
                                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                                        append(match.groupValues[2])
                                                    }
                                                }
                                                currentIndex = match.range.last + 1
                                            }
                                            append(cleanLine.substring(currentIndex))
                                        }
                                        
                                        Text(
                                            text = annotatedString,
                                            color = textColor,
                                            fontSize = fontSize.sp,
                                            fontFamily = FontFamily.Serif,
                                            lineHeight = (fontSize * 1.5).sp,
                                            textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}
