package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BookDao
import com.example.data.dao.QuoteDao
import com.example.data.dao.VideoStoryDao
import com.example.data.model.BookEntity
import com.example.data.model.QuoteEntity
import com.example.data.model.VideoStoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [BookEntity::class, QuoteEntity::class, VideoStoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun quoteDao(): QuoteDao
    abstract fun videoStoryDao(): VideoStoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "living_page_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Preload books and quotes in background coroutine
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).apply {
                                    bookDao().insertBooks(InitialData.defaultBooks)
                                    quoteDao().insertQuotes(InitialData.defaultQuotes)
                                    videoStoryDao().insertVideoStory(InitialData.defaultStory)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
