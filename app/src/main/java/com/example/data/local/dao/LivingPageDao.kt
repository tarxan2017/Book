package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.SurpriseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LivingPageDao {

    // Книги
    @Query("SELECT * FROM books ORDER BY isCurrent DESC, id ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentBook(): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    // Цитаты
    @Query("SELECT * FROM quotes ORDER BY isToday DESC, timestamp DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isToday = 1 LIMIT 1")
    fun getTodayQuote(): Flow<QuoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Update
    suspend fun updateQuote(quote: QuoteEntity)

    // Друзья
    @Query("SELECT * FROM friends ORDER BY isOnline DESC, lastActionTimestamp DESC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>)

    // Сюрпризы
    @Query("SELECT * FROM surprises ORDER BY timestamp DESC")
    fun getAllSurprises(): Flow<List<SurpriseEntity>>

    @Query("SELECT * FROM surprises WHERE isOpened = 0 ORDER BY timestamp DESC LIMIT 1")
    fun getLatestUnopenedSurprise(): Flow<SurpriseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurprise(surprise: SurpriseEntity): Long

    @Update
    suspend fun updateSurprise(surprise: SurpriseEntity)
}
