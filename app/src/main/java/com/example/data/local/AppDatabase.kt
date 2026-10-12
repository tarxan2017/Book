package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.R
import com.example.data.local.dao.LivingPageDao
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.QuoteEntity
import com.example.data.local.entity.SurpriseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BookEntity::class,
        QuoteEntity::class,
        FriendEntity::class,
        SurpriseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun livingPageDao(): LivingPageDao

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
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.livingPageDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: LivingPageDao) {
            // Начальная книга из дизайна
            val sampleAlchemistText = """
Глава 12. Пустыня и Знаки

Юношу звали Сантьяго. Начинало смеркаться, когда он подошел со своей отарой к заброшенной полуразрушенной церкви. Купол ее давно обвалился, а на том месте, где когда-то была ризница, вырос огромный сикомор.

Сантьяго решил заночевать здесь. Он загнал овец сквозь полуразрушенные двери и загородил выход несколькими досками, чтобы животные не разбрелись ночью. В этих краях волков не водилось, но однажды овца все-таки убежала, и он потратил весь следующий день на поиски.

Он постелил на каменный пол свою куртку и лег, положив под голову книгу, которую только что прочитал. Перед сном он подумал, что надо бы завести книги потолще: и читать дольше, и подушка получается повыше.

Когда он проснулся, было еще темно. Взглянув наверх, он увидел сквозь полуобвалившуюся крышу сияющие звезды. 

«Иногда именно потеря показывает нам, что по-настоящему важно», — вспомнил он слова старого мудреца Мельхиседека. В пустыне тишина говорила громче любых слов. Все вокруг дышало великой Душой Мира.
            """.trimIndent()

            val alchemistBook = BookEntity(
                id = 1,
                title = "Алхимик",
                author = "Пауло Коэльо",
                coverRes = R.drawable.book_cover_alchemist,
                totalChapters = 22,
                currentChapter = 12,
                progressPercent = 54,
                generatedQuotesCount = 5,
                textContent = sampleAlchemistText,
                isCurrent = true
            )

            val littlePrinceBook = BookEntity(
                id = 2,
                title = "Маленький принц",
                author = "Антуан де Сент-Экзюпери",
                coverRes = R.drawable.open_book_magic,
                totalChapters = 27,
                currentChapter = 4,
                progressPercent = 18,
                generatedQuotesCount = 3,
                textContent = "Зорко одно лишь сердце. Самого главного глазами не увидишь. Твоя роза так дорога тебе потому, что ты отдавал ей все свое время.",
                isCurrent = false
            )

            dao.insertBooks(listOf(alchemistBook, littlePrinceBook))

            // Цитаты дня и прошлые цитаты из макета
            val quotes = listOf(
                QuoteEntity(
                    id = 1,
                    quoteText = "«Иногда именно потеря показывает нам, что по-настоящему важно.»",
                    author = "Пауло Коэльо",
                    bookTitle = "«Алхимик»",
                    dateLabel = "Сегодня",
                    bgRes = R.drawable.quote_bg_vertical,
                    videoDuration = "0:18",
                    isToday = true,
                    isFavorite = true
                ),
                QuoteEntity(
                    id = 2,
                    quoteText = "«Всё, что ты ищешь, тоже ищет тебя.»",
                    author = "Руми",
                    bookTitle = "«Поэмы»",
                    dateLabel = "24 окт",
                    bgRes = R.drawable.quote_bg_candle,
                    videoDuration = "0:22",
                    isToday = false,
                    isFavorite = true
                ),
                QuoteEntity(
                    id = 3,
                    quoteText = "«Зорко одно лишь сердце. Самого главного глазами не увидишь.»",
                    author = "Антуан де Сент-Экзюпери",
                    bookTitle = "«Маленький принц»",
                    dateLabel = "23 окт",
                    bgRes = R.drawable.open_book_magic,
                    videoDuration = "0:15",
                    isToday = false,
                    isFavorite = true
                ),
                QuoteEntity(
                    id = 4,
                    quoteText = "«Спокойствие — сильнее всяких бурь.»",
                    author = "Лев Толстой",
                    bookTitle = "«Война и мир»",
                    dateLabel = "22 окт",
                    bgRes = R.drawable.quote_bg_candle,
                    videoDuration = "0:20",
                    isToday = false,
                    isFavorite = false
                ),
                QuoteEntity(
                    id = 5,
                    quoteText = "«Учитесь властвовать собою.»",
                    author = "А.С. Пушкин",
                    bookTitle = "«Евгений Онегин»",
                    dateLabel = "21 окт",
                    bgRes = R.drawable.quote_bg_vertical,
                    videoDuration = "0:25",
                    isToday = false,
                    isFavorite = false
                )
            )
            dao.insertQuotes(quotes)

            // Друзья из дизайна
            val friends = listOf(
                FriendEntity(
                    id = 1,
                    name = "Маллика",
                    statusText = "Онлайн",
                    isOnline = true,
                    avatarRes = R.drawable.avatar_mallika
                ),
                FriendEntity(
                    id = 2,
                    name = "Джавидан",
                    statusText = "Был 2 ч. назад",
                    isOnline = false,
                    avatarRes = R.drawable.avatar_mallika
                ),
                FriendEntity(
                    id = 3,
                    name = "Саид",
                    statusText = "Был 5 ч. назад",
                    isOnline = false,
                    avatarRes = R.drawable.avatar_mallika
                ),
                FriendEntity(
                    id = 4,
                    name = "Аня",
                    statusText = "Была вчера",
                    isOnline = false,
                    avatarRes = R.drawable.avatar_mallika
                ),
                FriendEntity(
                    id = 5,
                    name = "Рустам",
                    statusText = "Был 1 день назад",
                    isOnline = false,
                    avatarRes = R.drawable.avatar_mallika
                )
            )
            dao.insertFriends(friends)

            // Начальный подарок-сюрприз от Маллики
            val initialSurprise = SurpriseEntity(
                id = 1,
                senderName = "Маллика",
                receiverName = "Вы",
                quoteText = "«Иногда именно потеря показывает нам, что по-настоящему важно.»",
                author = "Пауло Коэльо",
                bookTitle = "«Алхимик»",
                formatType = "OPEN_BOOK",
                durationSeconds = 4,
                isOpened = false
            )
            dao.insertSurprise(initialSurprise)
        }
    }
}
