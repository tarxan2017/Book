package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val author: String,
    val bookTitle: String,
    val page: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = true,
    val visualMood: String = "свеча" // свеча, страницы, чай, звезды
)
