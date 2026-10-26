package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.io.File

@Composable
fun MyBookScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }
    var tempTitle by remember { mutableStateOf("") }
    var tempAuthor by remember { mutableStateOf("") }
    var tempCoverUri by remember { mutableStateOf("") }

    var editingBook by remember { mutableStateOf<BookEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editAuthor by remember { mutableStateOf("") }
    var editCoverUri by remember { mutableStateOf("") }

    val bookPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingFileUri = uri
            tempTitle = ""
            tempAuthor = ""
            tempCoverUri = ""
            showAddDialog = true
        }
    }

    val coverPickerLauncherAdd = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) tempCoverUri = uri.toString() }

    val coverPickerLauncherEdit = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) editCoverUri = uri.toString() }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp)
            .testTag("my_book_screen_content")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                onClick = { showAddMenu = true },
                modifier = Modifier.testTag("my_book_add_button")
            ) {
                Icon(Icons.Default.Add, "Добавить", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (allBooks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "У вас пока нет книг.\nНажмите +, чтобы загрузить или написать.",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
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
                            if (book.isUserWritten) {
                                if (book.textContent.isBlank()) {
                                    viewModel.openWriter(book)
                                } else {
                                    viewModel.openReader(book)
                                }
                            } else {
                                viewModel.openMyBookDetail(book)
                            }
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
        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text("Добавить в библиотеку", fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold) },
            text = { Text("Вы хотите загрузить готовый файл книги или написать свою?") },
            confirmButton = {
                Button(
                    onClick = {
                        showAddMenu = false
                        bookPickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "application/x-fictionbook+xml", "application/zip", "application/x-zip-compressed", "text/xml", "application/octet-stream"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Загрузить файл") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddMenu = false
                    viewModel.openWriter()
                }) { Text("Написать книгу", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showAddDialog && pendingFileUri != null) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Новая книга", style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = tempTitle, onValueChange = { tempTitle = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = tempAuthor, onValueChange = { tempAuthor = it }, label = { Text("Автор") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { coverPickerLauncherAdd.launch("image/*") }) { Text("Выбрать обложку", color = MaterialTheme.colorScheme.primary) }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.loadBookFromFile(
                                    context = context, 
                                    uri = pendingFileUri!!, 
                                    title = tempTitle, 
                                    author = tempAuthor, 
                                    coverUri = tempCoverUri
                                )
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) { Text("Сохранить") }
                    }
                }
            }
        }
    }

    if (editingBook != null) {
        Dialog(onDismissRequest = { editingBook = null }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Редактировать", style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlayfairDisplayFontFamily, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = {
                            viewModel.deleteBook(context, editingBook!!)
                            editingBook = null
                        }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = editAuthor, onValueChange = { editAuthor = it }, label = { Text("Автор") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { coverPickerLauncherEdit.launch("image/*") }, modifier = Modifier.align(Alignment.Start)) {
                        Text("Сменить обложку", color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateBookDetails(
                                context = context, 
                                book = editingBook!!, 
                                newTitle = editTitle, 
                                newAuthor = editAuthor, 
                                newCoverUri = editCoverUri
                            )
                            editingBook = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Сохранить", fontWeight = FontWeight.Bold) }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { editingBook = null },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun BookShelfItem(
    book: BookEntity,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Column(modifier = Modifier.width(110.dp).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 160.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
        ) {
            if (book.coverUri.isNotBlank()) {
                // ИСПРАВЛЕНО: Надежная загрузка для Coil
                val imageModel: Any = when {
                    book.coverUri.startsWith("content://") -> book.coverUri
                    book.coverUri.startsWith("file://") -> Uri.parse(book.coverUri)
                    else -> File(book.coverUri)
                }
                
                AsyncImage(
                    model = imageModel,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                }
            }

            Surface(
                color = if (book.isUserWritten) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 8.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (book.isUserWritten) Icons.Default.RateReview else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (book.isUserWritten) MaterialTheme.colorScheme.onPrimary else Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (book.isUserWritten) "Черновик" else "Книга",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (book.isUserWritten) MaterialTheme.colorScheme.onPrimary else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }

            if (book.progressPercent > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 10.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text("${book.progressPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
