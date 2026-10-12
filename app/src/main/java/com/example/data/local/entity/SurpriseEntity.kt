package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surprises")
data class SurpriseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val receiverName: String,
    val quoteText: String,
    val author: String,
    val bookTitle: String,
    val formatType: String = "GIFT_BOX", // GIFT_BOX или OPEN_BOOK
    val durationSeconds: Int = 4,
    val timestamp: Long = System.currentTimeMillis(),
    val isOpened: Boolean = false
)
