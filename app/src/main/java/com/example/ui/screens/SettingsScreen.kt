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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val geminiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val videoKey by viewModel.videoApiKey.collectAsStateWithLifecycle()
    val isRussianSupported by viewModel.isRussianSupported.collectAsStateWithLifecycle()
    val remainingGenerations by viewModel.remainingGenerations.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    var inputGeminiKey by remember(geminiKey) { mutableStateOf(geminiKey) }
    var inputVideoKey by remember(videoKey) { mutableStateOf(videoKey) }
    var showAiDetails by remember { mutableStateOf(true) }
    var showCopyrightDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("settings_screen_content")
    ) {
        // Заголовок "Настройки"
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = PlayfairDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Карточка профиля / Google Cloud Аккаунт
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_profile_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.drawable.avatar_mallika),
                        contentDescription = "Аватар профиля",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = currentUser?.displayName ?: "Мой профиль",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentUser != null) (currentUser?.email ?: "Google Cloud подключен") else "Синхронизация Google Cloud",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentUser != null) {
                    OutlinedButton(
                        onClick = { viewModel.signOutFromGoogle() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Выйти", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Button(
                        onClick = { viewModel.signInWithGoogle(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Войти", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Переключатель темы: "Тема" -> [Тёмная] [Светлая (#FFF8E7)]
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().testTag("theme_selector_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Тема",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip(
                        label = "Тёмная",
                        isSelected = isDarkTheme,
                        onClick = { viewModel.toggleTheme(true) }
                    )
                    ThemeChip(
                        label = "Светлая",
                        isSelected = !isDarkTheme,
                        onClick = { viewModel.toggleTheme(false) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Пункты меню (как на экране 6)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsMenuRow(
                    icon = Icons.Default.Book,
                    title = "Книга",
                    subtitle = "Управление книгами",
                    onClick = { viewModel.navigateTo(Screen.MyBook) }
                )

                SettingsMenuRow(
                    icon = Icons.Default.People,
                    title = "Друзья",
                    subtitle = "Список и приглашения",
                    onClick = { viewModel.navigateTo(Screen.Friends) }
                )

                SettingsMenuRow(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления",
                    subtitle = "Цитаты, ответы, напоминания",
                    onClick = { /* Настройки уведомлений */ }
                )

                SettingsMenuRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Интеграции ИИ",
                    subtitle = "Модели и API-ключи",
                    onClick = { showAiDetails = !showAiDetails }
                )

                SettingsMenuRow(
                    icon = Icons.Default.Copyright,
                    title = "Авторские права на книги",
                    subtitle = "Правовая информация",
                    onClick = { showCopyrightDialog = !showCopyrightDialog }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Развернутый блок «Интеграции ИИ»
        if (showAiDetails) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("ai_integrations_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Интеграции ИИ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Модель выбора момента: Gemini
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Модель для выбора момента",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Gemini",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputGeminiKey,
                        onValueChange = { inputGeminiKey = it },
                        placeholder = { Text("Вставьте Gemini API ключ", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (inputGeminiKey != geminiKey) {
                                Button(
                                    onClick = { viewModel.setGeminiApiKey(inputGeminiKey) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text("ОК", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Бесплатный ключ на aistudio.google.com → Get API key",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Модель для генерации видео: Magic Hour
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Модель для генерации видео",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Magic Hour",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Лимит генераций в день: бейдж 5 и текущий остаток
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Лимит генераций в день",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Осталось сегодня: $remainingGenerations из 5",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (remainingGenerations <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetDailyLimitForDemo() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Сброс", style = MaterialTheme.typography.labelSmall)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (remainingGenerations <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "$remainingGenerations/5",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Проверка русского голоса TTS
                    Text(
                        text = "Озвучка книг на русском языке (TTS)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRussianSupported) "Русский голос готов к работе" else "Требуется установка русского голоса",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRussianSupported) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.speakText("Русская речь для чтения книг активна и готова к работе.") },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Проверить", style = MaterialTheme.typography.labelSmall)
                            }

                            if (!isRussianSupported) {
                                Button(
                                    onClick = { viewModel.openTtsSettings() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Настройки Android", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCopyrightDialog) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Юридическая информация и авторские права",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Произведения и цитаты классиков используются исключительно в познавательных и культурных целях (общественное достояние и добросовестное цитирование по ст. 1274 ГК РФ). Все права на зарегистрированные товарные знаки принадлежат их правообладателям.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ThemeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}
