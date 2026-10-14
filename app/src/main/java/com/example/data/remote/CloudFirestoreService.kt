package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.remote.model.ChatMessage
import com.example.data.remote.model.CloudQuote
import com.example.data.remote.model.CloudSurprise
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

    /**
     * Сохраняет профиль. Обновляет ТОЛЬКО displayName, photoUrl, statusText, isOnline.
     * Поле friends НЕ трогает — чтобы при входе не стирать список друзей.
     */
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

    /**
     * Ищет пользователя по UID (Document ID в коллекции users).
     */
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
}
