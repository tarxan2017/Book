package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.SurpriseEntity
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun ReceiveSurpriseScreen(
    viewModel: MainViewModel,
    surprise: SurpriseEntity,
    modifier: Modifier = Modifier
) {
    var replyText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("receive_surprise_screen_content")
    ) {
        // Шапка: Аватар Маллики + Текст отправки + Крестик ✕
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.avatar_mallika),
                    contentDescription = surprise.senderName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = surprise.senderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Отправил(а) вам цитату",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "2 мин. назад",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { viewModel.navigateTo(Screen.Friends) },
                modifier = Modifier.testTag("receive_surprise_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Центральная карточка: анимированная книга со свечением (как на экране 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
        ) {
            Image(
                painter = painterResource(
                    id = if (surprise.formatType == "GIFT_BOX") R.drawable.gift_box_magic else R.drawable.open_book_magic
                ),
                contentDescription = "Сюрприз",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Кнопка Play превью (0:18)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .clickable {
                        val quote = QuoteEntity(
                            quoteText = surprise.quoteText,
                            author = surprise.author,
                            bookTitle = surprise.bookTitle,
                            dateLabel = "Сегодня",
                            bgRes = R.drawable.quote_bg_vertical,
                            videoDuration = "0:18"
                        )
                        viewModel.navigateTo(Screen.VideoPlayer(quote))
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Смотреть",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "0:18",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Цитата Пауло Коэльо «Алхимик»
        Text(
            text = surprise.quoteText,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${surprise.author} ${surprise.bookTitle}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Отправитель: ${surprise.senderName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Кнопка "Ответить цитатой"
        Button(
            onClick = { viewModel.navigateTo(Screen.SendSurprise()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("reply_with_quote_button")
        ) {
            Text(
                text = "Ответить цитатой",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Поле ввода сообщения "Ответить..." + кнопка "Сохранить"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Ответить...", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (replyText.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.speakText("Спасибо за цитату! $replyText")
                            replyText = ""
                        }) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Отправить",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clickable { /* Сохранить в избранное */ }
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Сохранить",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Сохранить",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
