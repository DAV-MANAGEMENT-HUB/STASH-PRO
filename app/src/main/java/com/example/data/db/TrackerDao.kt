package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TrackerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    @Query("SELECT * FROM trackers ORDER BY targetDate ASC")
    fun getAllTrackers(): Flow<List<TrackerItem>>

    @Query("SELECT * FROM trackers WHERE isCompleted = 0 AND isConfirmedByUser = 1 ORDER BY targetDate ASC")
    fun getActiveTrackers(): Flow<List<TrackerItem>>

    @Query("SELECT * FROM trackers WHERE isCompleted = 0 AND isConfirmedByUser = 0 ORDER BY targetDate ASC")
    fun getSuggestedTrackers(): Flow<List<TrackerItem>>

    @Query("SELECT * FROM trackers WHERE isCompleted = 1 ORDER BY targetDate DESC")
    fun getCompletedTrackers(): Flow<List<TrackerItem>>

    @Query("SELECT * FROM trackers WHERE stashItemId = :stashItemId ORDER BY targetDate ASC")
    fun getTrackersForStashItem(stashItemId: Long): Flow<List<TrackerItem>>

    @Query("SELECT * FROM trackers WHERE id = :id")
    fun getTrackerById(id: Long): Flow<TrackerItem?>

    @Query("SELECT COUNT(*) FROM trackers WHERE isCompleted = 0")
    fun countActive(): Flow<Int>

    @Query("SELECT * FROM trackers WHERE isCompleted = 0")
    suspend fun getActiveTrackersList(): List<TrackerItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tracker: TrackerItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trackers: List<TrackerItem>): List<Long>

    @Update
    suspend fun update(tracker: TrackerItem)

    @Delete
    suspend fun delete(tracker: TrackerItem)

    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trackers WHERE stashItemId = :stashItemId")
    suspend fun deleteByStashItemId(stashItemId: Long)
}
