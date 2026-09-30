package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FolderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE isTrashed = 0 ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderItem>>

    @Query("SELECT * FROM folders WHERE isTrashed = 0 ORDER BY name ASC")
    suspend fun getAllFoldersDirect(): List<FolderItem>

    @Query("SELECT * FROM folders WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    fun getTrashedFolders(): Flow<List<FolderItem>>

    @Query("SELECT * FROM folders WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    suspend fun getTrashedFoldersDirect(): List<FolderItem>

    @Query("SELECT * FROM folders WHERE isTrashed = 0 AND ((:parentId IS NULL AND parentFolderId IS NULL) OR parentFolderId = :parentId) ORDER BY name ASC")
    fun getFoldersByParent(parentId: Long?): Flow<List<FolderItem>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun getFolderById(id: Long): Flow<FolderItem?>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderByIdDirect(id: Long): FolderItem?

    @Query("SELECT * FROM folders WHERE name = :name AND isTrashed = 0 AND ((:parentId IS NULL AND parentFolderId IS NULL) OR parentFolderId = :parentId) LIMIT 1")
    suspend fun findFolderByName(name: String, parentId: Long?): FolderItem?

    @Query("UPDATE folders SET isTrashed = 1, trashedTimestamp = :timestamp, originalParentFolderId = parentFolderId WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long)

    @Query("UPDATE folders SET isTrashed = 0, trashedTimestamp = NULL, parentFolderId = originalParentFolderId WHERE id = :id")
    suspend fun restoreFolder(id: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM folders WHERE isTrashed = 1")
    suspend fun emptyTrashedFolders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderItem): Long

    @Update
    suspend fun update(folder: FolderItem)

    @Delete
    suspend fun delete(folder: FolderItem)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
