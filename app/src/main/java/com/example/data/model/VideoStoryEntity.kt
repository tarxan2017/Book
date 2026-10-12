package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_stories")
data class VideoStoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val quoteText: String,
    val author: String,
    val bookTitle: String,
    val visualMood: String, // "свеча", "страницы", "чай", "звезды"
    val narrationText: String,
    val timestamp: Long = System.currentTimeMillis()
)
