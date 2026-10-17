package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.BookEntity
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun MyBookScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Состояние диалога ДОБАВЛЕНИЯ новой книги
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingPdfUri by remember { mutableStateOf<Uri?>(null) }
    var tempTitle by remember { mutableStateOf("") }
    var tempAuthor by remember { mutableStateOf("") }
    var tempCoverUri by remember { mutableStateOf("") }

    // Состояние диалога РЕДАКТИРОВАНИЯ существующей книги
    var editingBook by remember { mutableStateOf<BookEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editAuthor by remember { mutableStateOf("") }
    var editCoverUri by remember { mutableStateOf("") }

    // Лаунчер для выбора PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingPdfUri = uri
            tempTitle = ""
            tempAuthor = ""
            tempCoverUri = ""
            showAddDialog = true
        }
    }

    // Лаунчер для выбора обложки (для добавления)
    val coverPickerLauncherAdd = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) tempCoverUri = uri.toString()
    }

    // Лаунчер для выбора обложки (для редактирования)
    val coverPickerLauncherEdit = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) editCoverUri = uri.toString()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp)
            .testTag("my_book_screen_content")
    ) {
        // === ШАПКА ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои книги",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.testTag("my_book_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить книгу",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // === ПОЛКА С КНИГАМИ ===
        if (allBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "У вас пока нет книг.\nНажмите +, чтобы загрузить.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allBooks, key = { it.id }) { book ->
                    BookShelfItem(
                        book = book,
                        onClick = {
                            viewModel.setCurrentBook(book)
                            viewModel.navigateTo(Screen.Reader)
                        },
                        onEditClick = {
                            editingBook = book
                            editTitle = book.title
                            editAuthor = book.author
                            editCoverUri = book.coverUri
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // === УПРАВЛЕНИЕ КНИГАМИ ===
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { viewModel.navigateTo(Screen.BookList) }
                .testTag("manage_books_card")
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Управление книгами",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Удаление и смена текущей книги",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // === ДИАЛОГ: ДОБАВЛЕНИЕ НОВОЙ КНИГИ ===
    if (showAddDialog && pendingPdfUri != null) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Новая книга",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempAuthor,
                        onValueChange = { tempAuthor = it },
                        label = { Text("Автор") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { coverPickerLauncherAdd.launch("image/*") }) {
                        Text("Выбрать обложку", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.loadBookFromPdf(
                                    context = context,
                                    uri = pendingPdfUri!!,
                                    title = tempTitle,
                                    author = tempAuthor,
                                    coverUri = tempCoverUri
                                )
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }

    // === ДИАЛОГ: РЕДАКТИРОВАНИЕ КНИГИ ===
    if (editingBook != null) {
        Dialog(onDismissRequest = { editingBook = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Редактировать",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editAuthor,
                        onValueChange = { editAuthor = it },
                        label = { Text("Автор") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { coverPickerLauncherEdit.launch("image/*") }) {
                        Text("Сменить обложку", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingBook = null }) {
                            Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                // Передаем context, чтобы скопировать обложку
                                viewModel.updateBookDetails(
                                    context = context,
                                    book = editingBook!!,
                                    newTitle = editTitle,
                                    newAuthor = editAuthor,
                                    newCoverUri = editCoverUri
                                )
                                editingBook = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }
}

// === КОМПОНЕНТ: КАРТОЧКА КНИГИ НА ПОЛКЕ ===
@Composable
private fun BookShelfItem(
    book: BookEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick)
    ) {
        // Обложка с плейсхолдером
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 160.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        ) {
            if (book.coverUri.isNotBlank()) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Стильный плейсхолдер вместо Алхимика
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Кнопка редактирования (карандаш) в углу
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Процент прогресса внизу
            if (book.progressPercent > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 10.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "${book.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
