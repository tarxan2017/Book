package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.QuoteEntity
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val fontSize by viewModel.readerFontSize.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val remainingGenerations by viewModel.remainingGenerations.collectAsStateWithLifecycle()

    val book = currentBook ?: com.example.data.local.entity.BookEntity(
        id = 1,
        title = "Алхимик",
        author = "Пауло Коэльо",
        coverRes = R.drawable.book_cover_alchemist,
        totalChapters = 22,
        currentChapter = 12,
        progressPercent = 54,
        textContent = "«Иногда именно потеря показывает нам, что по-настоящему важно», — вспомнил юноша слова старого мудреца. В пустыне тишина говорила громче любых слов. Все вокруг дышало великой Душой Мира."
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("reader_screen_content")
    ) {
        // Шапка читалки
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MyBook) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlayfairDisplayFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Глава ${book.currentChapter} из ${book.totalChapters}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Регулировка размера шрифта
                    IconButton(onClick = {
                        val newSize = if (fontSize > 14f) fontSize - 2f else 22f
                        viewModel.setReaderFontSize(newSize)
                    }) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Размер шрифта",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Кнопка озвучки книги на русском языке (TTS)
                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakText(book.textContent)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.VolumeUp,
                            contentDescription = "Озвучка книги",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Текст книги на теплой кремовой бумаге
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Text(
                text = "Глава ${book.currentChapter}. Пустыня и Знаки",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSize + 6).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            Text(
                text = book.textContent,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.6f).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Карточка создания живого видео из цитаты
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Вдохновил отрывок?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Превратите любимую мысль в кинематографический видео-ролик 9:16 с озвучкой",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.generateNewQuote()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (remainingGenerations <= 0) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (remainingGenerations <= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (remainingGenerations <= 0) "Лимит на сегодня исчерпан (0/5)" else "Оживить цитату в видео",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
