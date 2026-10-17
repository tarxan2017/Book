package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.remote.model.ChatMessage
import com.example.data.remote.model.CloudQuote
import com.example.data.remote.model.CloudSurprise
import com.example.data.remote.model.PublicBook
import com.example.data.remote.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CloudFirestoreService(
    context: Context
) {
    private val databaseId: String = context.getString(R.string.firestore_database_id)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)

    // === СЮРПРИЗЫ ===

    fun observeUserSurprises(userId: String): Flow<List<CloudSurprise>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("surprises")
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(CloudSurprise::class.java) } ?: emptyList()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendSurprise(surprise: CloudSurprise) {
        val collection = db.collection("surprises")
        val docRef = if (surprise.id.isNotBlank()) collection.document(surprise.id) else collection.document()
        docRef.set(surprise.copy(id = docRef.id)).await()
    }

    // === ПРОФИЛЬ ===

    suspend fun syncUserProfile(userProfile: UserProfile) {
        if (userProfile.userId.isBlank()) return

        val safeData = mapOf(
            "displayName" to userProfile.displayName,
            "photoUrl" to userProfile.photoUrl,
            "statusText" to userProfile.statusText,
            "isOnline" to userProfile.isOnline
        )

        db.collection("users")
            .document(userProfile.userId)
            .set(safeData, SetOptions.merge())
            .await()
    }

    // НОВЫЙ МЕТОД: Сохранение токена FCM для пушей
    suspend fun saveFcmToken(userId: String, token: String) {
        if (userId.isBlank() || token.isBlank()) return
        try {
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
        } catch (_: Exception) {
            // Игнорируем, если пользователя еще нет в базе
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserProfile::class.java))
            }

        awaitClose { listener.remove() }
    }

    suspend fun findUserByUid(uid: String): UserProfile? {
        if (uid.isBlank()) return null
        return try {
            val doc = db.collection("users").document(uid.trim()).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveQuoteToCloud(userId: String, quote: CloudQuote) {
        if (userId.isBlank()) return
        val collection = db.collection("users").document(userId).collection("quotes")
        val docRef = if (quote.id.isNotBlank()) collection.document(quote.id) else collection.document()
        docRef.set(quote.copy(id = docRef.id, userId = userId)).await()
    }

    // === ДРУЗЬЯ ===

    suspend fun addFriend(myUid: String, friendUid: String) {
        if (myUid.isBlank() || friendUid.isBlank() || myUid == friendUid) return

        val myRef = db.collection("users").document(myUid)
        val friendRef = db.collection("users").document(friendUid)

        db.runTransaction { transaction ->
            transaction.update(myRef, "friends", FieldValue.arrayUnion(friendUid))
            transaction.update(friendRef, "friends", FieldValue.arrayUnion(myUid))
        }.await()
    }

    fun observeFriends(userId: String): Flow<List<UserProfile>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendIds = snapshot?.toObject(UserProfile::class.java)?.friends ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val limited = friendIds.take(30)
                val profiles = mutableListOf<UserProfile>()
                var completed = 0

                limited.forEach { friendId ->
                    db.collection("users").document(friendId)
                        .get()
                        .addOnSuccessListener { doc ->
                            doc.toObject(UserProfile::class.java)?.let { profiles.add(it) }
                            completed++
                            if (completed == limited.size) {
                                trySend(profiles)
                            }
                        }
                        .addOnFailureListener {
                            completed++
                            if (completed == limited.size) {
                                trySend(profiles)
                            }
                        }
                }
            }

        awaitClose { listener.remove() }
    }

    // === СООБЩЕСТВО ===

    fun observeCommunityUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = db.collection("users")
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) } ?: emptyList()
                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    // === ЧАТ ===

    suspend fun sendMessage(fromId: String, toId: String, text: String) {
        if (fromId.isBlank() || toId.isBlank() || text.isBlank()) return

        val chatId = listOf(fromId, toId).sorted().joinToString("_")
        val message = ChatMessage(
            senderId = fromId,
            receiverId = toId,
            text = text
        )
        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .await()
    }

    fun observeMessages(myId: String, friendId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (myId.isBlank() || friendId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val chatId = listOf(myId, friendId).sorted().joinToString("_")
        val listener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // === ПУБЛИЧНАЯ БИБЛИОТЕКА ===

    fun observePublicBooks(): Flow<List<PublicBook>> = callbackFlow {
        val listener = db.collection("public_books")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val books = snapshot?.documents?.mapNotNull { it.toObject(PublicBook::class.java) } ?: emptyList()
                trySend(books)
            }

        awaitClose { listener.remove() }
    }

    fun observeRecentPublicBooks(limit: Long = 5): Flow<List<PublicBook>> = callbackFlow {
        val listener = db.collection("public_books")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val books = snapshot?.documents?.mapNotNull { it.toObject(PublicBook::class.java) } ?: emptyList()
                trySend(books)
            }

        awaitClose { listener.remove() }
    }

    suspend fun publishBook(book: PublicBook): String {
        val collection = db.collection("public_books")
        val docRef = collection.document()
        docRef.set(book.copy(id = docRef.id)).await()
        return docRef.id
    }

    suspend fun updatePublicBook(book: PublicBook) {
        if (book.id.isBlank()) return
        db.collection("public_books").document(book.id).set(book).await()
    }

    suspend fun deletePublicBook(bookId: String) {
        if (bookId.isBlank()) return
        db.collection("public_books").document(bookId).delete().await()
    }

    // === МОДЕРАТОРЫ ===

    fun observeModerators(): Flow<List<String>> = callbackFlow {
        val listener = db.collection("moderators")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val uids = snapshot?.documents?.map { it.id } ?: emptyList()
                trySend(uids)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addModerator(uid: String, name: String) {
        if (uid.isBlank()) return
        val data = mapOf("name" to name)
        db.collection("moderators").document(uid).set(data).await()
    }

    suspend fun removeModerator(uid: String) {
        if (uid.isBlank()) return
        db.collection("moderators").document(uid).delete().await()
    }

    suspend fun isModerator(uid: String): Boolean {
        if (uid.isBlank()) return false
        return try {
            val doc = db.collection("moderators").document(uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }
}
