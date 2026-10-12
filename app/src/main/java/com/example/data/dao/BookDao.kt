package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC, id ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookFlowById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC LIMIT 1")
    fun getCurrentlyReadingBook(): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBooksCount(): Int
}
