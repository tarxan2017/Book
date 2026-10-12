package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VideoStoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoStoryDao {
    @Query("SELECT * FROM video_stories ORDER BY timestamp DESC")
    fun getAllVideoStories(): Flow<List<VideoStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoStory(story: VideoStoryEntity): Long

    @Query("DELETE FROM video_stories WHERE id = :id")
    suspend fun deleteVideoStory(id: Long)

    @Query("SELECT COUNT(*) FROM video_stories")
    suspend fun getVideoStoriesCount(): Int
}
