package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY timestamp DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteQuotes(): Flow<List<QuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun deleteQuote(id: Long)

    @Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuote(): QuoteEntity?

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun getQuotesCount(): Int
}
