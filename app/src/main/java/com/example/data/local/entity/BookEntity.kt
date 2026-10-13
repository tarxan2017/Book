package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverRes: Int,
    val totalChapters: Int = 22,
    val currentChapter: Int = 12,
    val progressPercent: Int = 54,
    val generatedQuotesCount: Int = 5,
    val textContent: String = "",
    val isCurrent: Boolean = false,
    // Новое поле: URI выбранной обложки (если пользователь выбрал вручную)
    val coverUri: String = ""
)
