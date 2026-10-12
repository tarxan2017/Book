package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.remote.model.CloudQuote
import com.example.data.remote.model.CloudSurprise
import com.example.data.remote.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CloudFirestoreService(
    context: Context
) {
    private val databaseId: String = context.getString(R.string.firestore_database_id)
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)

    // Наблюдение за входящими и исходящими сюрпризами пользователя
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

    // Отправка сюрприза в облако Google Cloud
    suspend fun sendSurprise(surprise: CloudSurprise) {
        val collection = db.collection("surprises")
        val docRef = if (surprise.id.isNotBlank()) collection.document(surprise.id) else collection.document()
        docRef.set(surprise.copy(id = docRef.id)).await()
    }

    // Синхронизация профиля пользователя в Google Cloud
    suspend fun syncUserProfile(userProfile: UserProfile) {
        if (userProfile.userId.isBlank()) return
        db.collection("users")
            .document(userProfile.userId)
            .set(userProfile)
            .await()
    }

    // Сохранение цитаты в облачный профиль
    suspend fun saveQuoteToCloud(userId: String, quote: CloudQuote) {
        if (userId.isBlank()) return
        val collection = db.collection("users").document(userId).collection("quotes")
        val docRef = if (quote.id.isNotBlank()) collection.document(quote.id) else collection.document()
        docRef.set(quote.copy(id = docRef.id, userId = userId)).await()
    }

    // Наблюдение за онлайн-друзьями в литературном сообществе
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
}
