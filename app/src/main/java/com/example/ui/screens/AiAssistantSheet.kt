package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    viewModel: MainViewModel,
    currentPageText: String,
    previousPageText: String,
    onDismiss: () -> Unit
) {
    val isGenerating by viewModel.isAiGenerating.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customPrompt by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    // Собираем контекст для отправки
    val contextToSend = buildString {
        if (previousPageText.isNotBlank()) {
            append("[Предыдущая страница]:\n$previousPageText\n\n")
        }
        append("[Текущая страница]:\n$currentPageText")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI-Советник",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Закрыть", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Выберите действие или задайте свой вопрос. AI учтет контекст текущей и предыдущей страницы.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Если ответа еще нет и не идет загрузка — показываем кнопки пресетов
            if (!showCustomInput && aiResponse == null && !isGenerating) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PresetButton("💡 Идеи", Modifier.weight(1f)) {
                        viewModel.askAi("Придумай 2 варианта развития событий в этой сцене. Оформи их строго как 'Вариант 1' и 'Вариант 2'.", contextToSend)
                    }
                    PresetButton("✍️ Стиль", Modifier.weight(1f)) {
                        viewModel.askAi("Проанализируй стиль текста. Дай один конкретный совет, как сделать его более литературным и живым.", contextToSend)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PresetButton("💬 Диалог", Modifier.weight(1f)) {
                        viewModel.askAi("Посмотри на диалоги. Как сделать речь персонажей более естественной? Дай 2 варианта.", contextToSend)
                    }
                    PresetButton("❓ Свой вопрос", Modifier.weight(1f)) {
                        showCustomInput = true
                    }
                }
            }

            // Поле для своего вопроса
            if (showCustomInput && aiResponse == null && !isGenerating) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    label = { Text("Что вы хотите спросить?") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showCustomInput = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Назад", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (customPrompt.isNotBlank()) {
                                viewModel.askAi(customPrompt, contextToSend)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Спросить")
                    }
                }
            }

            // Индикатор загрузки
            if (isGenerating) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI думает...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Отображение ответа AI (разбитое на блоки)
            if (aiResponse != null && !isGenerating) {
                Spacer(modifier = Modifier.height(24.dp))
                
                val blocks = aiResponse!!.split("\n\n").filter { it.isNotBlank() }
                
                blocks.forEach { block ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = block,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { clipboardManager.setText(AnnotatedString(block)) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("📋 Скопировать этот блок", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.clearAiResponse()
                        showCustomInput = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Новый запрос")
                }
            }
        }
    }
}

@Composable
private fun PresetButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
