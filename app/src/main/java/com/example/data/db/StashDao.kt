package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import kotlinx.coroutines.flow.Flow

@Dao
interface StashDao {

    @Query("SELECT * FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 ORDER BY addedTimestamp DESC")
    fun getAllItems(): Flow<List<StashItem>>

    @Query("SELECT * FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 ORDER BY addedTimestamp DESC")
    suspend fun getAllItemsDirect(): List<StashItem>

    @Query("SELECT * FROM stash_items WHERE id = :id")
    fun getItemById(id: Long): Flow<StashItem?>

    @Query("SELECT * FROM stash_items WHERE id = :id")
    suspend fun getItemByIdDirect(id: Long): StashItem?

    @Query("SELECT * FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 AND itemType = :type ORDER BY addedTimestamp DESC")
    fun getItemsByType(type: StashItemType): Flow<List<StashItem>>

    @Query("SELECT * FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 AND isScreenshot = 1 ORDER BY addedTimestamp DESC")
    fun getScreenshots(): Flow<List<StashItem>>

    @Query("SELECT * FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 AND category = :category ORDER BY addedTimestamp DESC")
    fun getItemsByCategory(category: String): Flow<List<StashItem>>

    @Query("SELECT * FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 AND ((:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId) ORDER BY addedTimestamp DESC")
    fun getItemsByFolder(folderId: Long?): Flow<List<StashItem>>

    @Query("""
        SELECT * FROM stash_items 
        WHERE isArchived = 0 AND (
            title LIKE '%' || :query || '%' 
            OR tags LIKE '%' || :query || '%'
            OR extractedText LIKE '%' || :query || '%'
            OR vendor LIKE '%' || :query || '%'
            OR product LIKE '%' || :query || '%'
            OR person LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR notes LIKE '%' || :query || '%'
            OR originalFolderPath LIKE '%' || :query || '%'
            OR originalLocation LIKE '%' || :query || '%'
            OR uri LIKE '%' || :query || '%'
        )
        ORDER BY addedTimestamp DESC
    """)
    fun searchAllWithTrash(query: String): Flow<List<StashItem>>

    @Query("""
        SELECT * FROM stash_items 
        WHERE isArchived = 0 AND isTrashed = 0 AND (
            title LIKE '%' || :query || '%' 
            OR tags LIKE '%' || :query || '%'
            OR extractedText LIKE '%' || :query || '%'
            OR vendor LIKE '%' || :query || '%'
            OR product LIKE '%' || :query || '%'
            OR person LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR notes LIKE '%' || :query || '%'
            OR uri LIKE '%' || :query || '%'
        )
        ORDER BY addedTimestamp DESC
    """)
    fun search(query: String): Flow<List<StashItem>>

    @Query("""
        SELECT * FROM stash_items 
        WHERE isTrashed = 1 AND (
            title LIKE '%' || :query || '%' 
            OR tags LIKE '%' || :query || '%'
            OR extractedText LIKE '%' || :query || '%'
            OR vendor LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR originalFolderPath LIKE '%' || :query || '%'
            OR originalLocation LIKE '%' || :query || '%'
        )
        ORDER BY trashedTimestamp DESC
    """)
    fun searchTrash(query: String): Flow<List<StashItem>>

    @Query("SELECT * FROM stash_items WHERE uri IN (:uris)")
    suspend fun findItemsByUris(uris: List<String>): List<StashItem>

    @Query("SELECT * FROM stash_items WHERE title = :title AND isTrashed = 0")
    suspend fun findItemsByTitle(title: String): List<StashItem>

    @Query("""
        SELECT * FROM stash_items 
        WHERE id != :excludeId 
          AND isArchived = 0 
          AND isTrashed = 0
          AND ((:vendor IS NOT NULL AND vendor = :vendor AND vendor != '') OR category = :category)
        ORDER BY addedTimestamp DESC 
        LIMIT 10
    """)
    fun findRelatedItems(excludeId: Long, vendor: String?, category: String): Flow<List<StashItem>>

    @Query("SELECT COUNT(*) FROM stash_items WHERE isArchived = 0 AND isTrashed = 0")
    fun countAll(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stash_items WHERE isArchived = 0 AND isTrashed = 0 AND isScreenshot = 1")
    fun countScreenshots(): Flow<Int>

    // STORAGE STATS
    @Query("SELECT SUM(fileSize) FROM stash_items WHERE isTrashed = 0")
    fun sumActiveFileSize(): Flow<Long?>

    @Query("SELECT SUM(fileSize) FROM stash_items WHERE isTrashed = 1")
    fun sumTrashedFileSize(): Flow<Long?>

    @Query("SELECT * FROM stash_items WHERE isTrashed = 0 AND fileSize > 0 ORDER BY fileSize DESC LIMIT :limit")
    fun getLargestItems(limit: Int): Flow<List<StashItem>>

    // TRASH OPERATIONS
    @Query("SELECT * FROM stash_items WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    fun getTrashedItems(): Flow<List<StashItem>>

    @Query("SELECT * FROM stash_items WHERE isTrashed = 1 ORDER BY trashedTimestamp DESC")
    suspend fun getTrashedItemsDirect(): List<StashItem>

    @Query("SELECT * FROM stash_items WHERE isTrashed = 1 AND ((:originalFolderId IS NULL AND originalFolderId IS NULL) OR originalFolderId = :originalFolderId) ORDER BY trashedTimestamp DESC")
    fun getTrashedItemsByOriginalFolder(originalFolderId: Long?): Flow<List<StashItem>>

    @Query("SELECT COUNT(*) FROM stash_items WHERE isTrashed = 1")
    fun countTrashed(): Flow<Int>

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 1, 
            trashedTimestamp = :timestamp, 
            originalFolderId = folderId,
            originalLocation = :location,
            originalFolderPath = :path,
            folderId = NULL
        WHERE id = :id
    """)
    suspend fun moveToTrash(id: Long, timestamp: Long, location: String?, path: String?)

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 1, 
            trashedTimestamp = :timestamp, 
            originalFolderId = folderId,
            originalLocation = 'Library',
            folderId = NULL
        WHERE id IN (:ids)
    """)
    suspend fun moveToTrashBulk(ids: List<Long>, timestamp: Long)

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 1, 
            trashedTimestamp = :timestamp,
            originalFolderId = folderId,
            originalLocation = :folderName,
            originalFolderPath = :folderName,
            folderId = NULL
        WHERE folderId = :folderId
    """)
    suspend fun moveFolderContentsToTrash(folderId: Long, timestamp: Long, folderName: String)

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 0, 
            trashedTimestamp = NULL,
            folderId = originalFolderId
        WHERE id = :id
    """)
    suspend fun restoreFromTrash(id: Long)

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 0, 
            trashedTimestamp = NULL,
            folderId = originalFolderId
        WHERE id IN (:ids)
    """)
    suspend fun restoreFromTrashBulk(ids: List<Long>)

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 0, 
            trashedTimestamp = NULL,
            folderId = originalFolderId
        WHERE originalFolderId = :folderId AND isTrashed = 1
    """)
    suspend fun restoreFolderContentsFromTrash(folderId: Long)

    @Query("""
        UPDATE stash_items 
        SET isTrashed = 0, 
            trashedTimestamp = NULL,
            folderId = originalFolderId
        WHERE isTrashed = 1
    """)
    suspend fun restoreAllTrash()

    @Query("DELETE FROM stash_items WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM stash_items WHERE id IN (:ids)")
    suspend fun deletePermanentlyBulk(ids: List<Long>)

    @Query("DELETE FROM stash_items WHERE originalFolderId = :folderId AND isTrashed = 1")
    suspend fun deleteFolderContentsPermanently(folderId: Long)

    @Query("DELETE FROM stash_items WHERE isTrashed = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM stash_items WHERE isTrashed = 1 AND trashedTimestamp <= :cutoffTimestamp")
    suspend fun deleteExpiredTrash(cutoffTimestamp: Long): Int

    // FOLDER OPERATIONS
    @Query("UPDATE stash_items SET folderId = :folderId WHERE id = :id")
    suspend fun updateItemFolder(id: Long, folderId: Long?)

    @Query("UPDATE stash_items SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun updateItemsFolder(ids: List<Long>, folderId: Long?)

    @Query("UPDATE stash_items SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE stash_items SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun setPinnedBulk(ids: List<Long>, isPinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: StashItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StashItem>): List<Long>

    @Update
    suspend fun update(item: StashItem)

    @Delete
    suspend fun delete(item: StashItem)

    @Query("DELETE FROM stash_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
