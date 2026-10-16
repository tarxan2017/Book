package com.example.data.remote.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    @DocumentId val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val statusText: String = "Читает в тишине",
    val photoUrl: String = "",
    val isOnline: Boolean = true,
    val friends: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null
)

data class CloudQuote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quoteText: String = "",
    val author: String = "",
    val bookTitle: String = "",
    val videoDuration: String = "0:18",
    @ServerTimestamp val createdAt: Date? = null
)

data class CloudSurprise(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val quoteText: String = "",
    val author: String = "",
    val bookTitle: String = "",
    val formatType: String = "GIFT_BOX",
    val durationSeconds: Int = 4,
    val isOpened: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null
)

data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    @ServerTimestamp val timestamp: Date? = null
)

data class PublicBook(
    @DocumentId val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val category: String = "",
    val pdfFileId: String = "",
    val coverFileId: String = "",
    val videoReviewUrl: String = "",
    val publisherId: String = "",
    val publisherName: String = "",
    @ServerTimestamp val createdAt: Date? = null
)

// Список категорий для публичной библиотеки
object BookCategories {
    val ALL = listOf(
        "Классика",
        "Фантастика",
        "Детектив",
        "Психология",
        "Саморазвитие",
        "Роман",
        "Нон-фикшн",
        "Бизнес",
        "История",
        "Поэзия"
    )
}
