package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val quoteText: String,
    val author: String,
    val bookTitle: String,
    val dateLabel: String,
    val bgRes: Int,
    val videoDuration: String = "0:18",
    val isFavorite: Boolean = false,
    val isToday: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
