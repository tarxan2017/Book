package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val coverRes: Int,
    val totalChapters: Int,
    val currentChapter: Int,
    val progressPercent: Int,
    val generatedQuotesCount: Int,
    val textContent: String,
    val isCurrent: Boolean,
    val coverUri: String,
    // НОВЫЙ ФЛАГ: true = написана в редакторе, false = загружена из файла
    val isUserWritten: Boolean = false 
)
