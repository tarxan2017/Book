package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val statusText: String,
    val isOnline: Boolean = false,
    val avatarRes: Int,
    val lastActionTimestamp: Long = System.currentTimeMillis()
)
