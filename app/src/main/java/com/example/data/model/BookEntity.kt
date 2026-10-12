package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val description: String,
    val category: String,
    val totalPages: Int = 1,
    val currentPage: Int = 1,
    val fullText: String,
    val isFavorite: Boolean = false,
    val lastReadTimestamp: Long = 0L
)
