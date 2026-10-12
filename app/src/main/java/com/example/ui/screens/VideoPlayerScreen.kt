package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.QuoteEntity
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    viewModel: MainViewModel,
    quote: QuoteEntity,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentSeconds by remember { mutableFloatStateOf(12f) }
    val totalSeconds = 30f
    var replyText by remember { mutableStateOf("") }

    // Анимация воспроизведения таймкода
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            currentSeconds = (currentSeconds + 1f) % totalSeconds
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // Фоновое вертикальное видео 9:16 с кинематографическим светом
        Image(
            painter = painterResource(id = quote.bgRes),
            contentDescription = "Видеоряд 9:16",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Градиентное затемнение для читаемости цитаты
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Верхняя панель: Крестик ✕ и кнопка звука (как на экране 10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Home) },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("video_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { viewModel.speakText(quote.quoteText) },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Озвучить цитату",
                    tint = Color.White
                )
            }
        }

        // Центральная плавающая кнопка воспроизведения при паузе
        if (!isPlaying) {
            IconButton(
                onClick = { isPlaying = true },
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Воспроизвести",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Нижняя часть: Элементы управления, цитата и действия (как на экране 10)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Таймлайн и кнопки управления: || 0:12/0:30 [ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Пауза",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val formattedCurrent = String.format("0:%02d", currentSeconds.toInt())
                Text(
                    text = "$formattedCurrent / 0:30",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                LinearProgressIndicator(
                    progress = { currentSeconds / totalSeconds },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { /* Полноэкранный режим */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Полный экран",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Текст цитаты в Playfair Display (как на экране 10)
            Text(
                text = quote.quoteText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = PlayfairDisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 30.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${quote.author} ${quote.bookTitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2D9CC)
            )

            Spacer(modifier = Modifier.height(18.dp))

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
                    .height(48.dp)
            ) {
                Text(
                    text = "Ответить цитатой",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Поле "Ответить...", кнопки "Сохранить", "Поделиться"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Ответить...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { /* Сохранить */ },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Сохранить",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { /* Поделиться */ },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
