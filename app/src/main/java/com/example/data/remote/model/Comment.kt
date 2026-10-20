package com.example.data.remote.model

data class Comment(
    val id: Int = 0,
    val book_id: String = "",
    val user_id: String = "",
    val user_name: String = "",
    val text: String = "",
    val created_at: Long = 0
)
