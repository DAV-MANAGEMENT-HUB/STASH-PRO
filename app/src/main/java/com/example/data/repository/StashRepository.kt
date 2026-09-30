package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.FolderDao
import com.example.data.db.StashDao
import com.example.data.db.TrackerDao
import com.example.data.model.FolderItem
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.TrackerItem
import com.example.data.model.TrackerType
import com.example.engine.LinkMetadataFetcher
import com.example.engine.MediaStoreScanner
import com.example.engine.ReminderNotificationManager
import com.example.engine.SmartCharacterizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class StashRepository(
    private val context: Context,
    private val stashDao: StashDao,
    private val trackerDao: TrackerDao,
    private val folderDao: FolderDao,
    private val notificationManager: ReminderNotificationManager
) {
    private val mediaScanner = MediaStoreScanner(context, stashDao, trackerDao)

    // Active Library Streams
    val allItems: Flow<List<StashItem>> = stashDao.getAllItems()
    val screenshots: Flow<List<StashItem>> = stashDao.getScreenshots()
    val totalCount: Flow<Int> = stashDao.countAll()
    val screenshotCount: Flow<Int> = stashDao.countScreenshots()

    // Trackers Streams
    val allTrackers: Flow<List<TrackerItem>> = trackerDao.getAllTrackers()
    val activeTrackers: Flow<List<TrackerItem>> = trackerDao.getActiveTrackers()
    val completedTrackers: Flow<List<TrackerItem>> = trackerDao.getCompletedTrackers()
    val activeTrackerCount: Flow<Int> = trackerDao.countActive()

    // Trash Streams
    val trashedItems: Flow<List<StashItem>> = stashDao.getTrashedItems()
    val trashedCount: Flow<Int> = stashDao.countTrashed()
    val trashedFolders: Flow<List<FolderItem>> = folderDao.getTrashedFolders()

    // Folder Streams
    val allFolders: Flow<List<FolderItem>> = folderDao.getAllFolders()

    val suggestedTrackers: Flow<List<TrackerItem>> = trackerDao.getSuggestedTrackers()

    fun getItemById(id: Long): Flow<StashItem?> = stashDao.getItemById(id)

    fun getTrackersForItem(itemId: Long): Flow<List<TrackerItem>> =
        trackerDao.getTrackersForStashItem(itemId)

    fun getRelatedItems(itemId: Long, vendor: String?, category: String): Flow<List<StashItem>> =
        stashDao.findRelatedItems(itemId, vendor, category)

    fun search(query: String): Flow<List<StashItem>> = stashDao.search(query)

    fun getItemsInFolder(folderId: Long?): Flow<List<StashItem>> =
        stashDao.getItemsByFolder(folderId)

    suspend fun scanDeviceMedia(): Int = withContext(Dispatchers.IO) {
        mediaScanner.scanAccessibleMedia()
    }

    // --- MANUAL INGESTION ---

    suspend fun addManualMedia(
        uri: Uri,
        name: String?,
        mimeType: String?,
        isScreenshot: Boolean = false,
        folderId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val displayName = name ?: uri.lastPathSegment ?: "Item_${System.currentTimeMillis()}"
        val analysis = SmartCharacterizer.analyze(
            originalName = displayName,
            mimeType = mimeType,
            isScreenshot = isScreenshot,
            textSnippet = displayName
        )

        val item = StashItem(
            uri = uri.toString(),
            originalPath = uri.path,
            title = analysis.title,
            itemType = analysis.itemType,
            category = analysis.category,
            tags = analysis.tags.joinToString(","),
            extractedText = displayName,
            vendor = analysis.vendor,
            product = analysis.product,
            person = analysis.person,
            detectedAmount = analysis.detectedAmount,
            currency = analysis.currency,
            eventDate = analysis.eventDate,
            expiryDate = analysis.expiryDate,
            isScreenshot = isScreenshot,
            mimeType = mimeType,
            isUncertainClassification = analysis.isUncertain,
            folderId = folderId
        )

        val id = stashDao.insert(item)

        if (analysis.suggestedTracker != null && analysis.suggestedTracker.isHighConfidence) {
            val tracker = TrackerItem(
                stashItemId = id,
                title = analysis.suggestedTracker.title,
                trackerType = analysis.suggestedTracker.type,
                targetDate = analysis.suggestedTracker.targetDate,
                notes = analysis.suggestedTracker.promptReason
            )
            trackerDao.insert(tracker)
            notificationManager.postTrackerNotification(tracker)
        }

        id
    }

    suspend fun addLink(
        url: String,
        userCategory: String? = null,
        folderId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val metadata = LinkMetadataFetcher.fetchMetadata(url)
        val analysis = SmartCharacterizer.analyze(
            originalName = metadata.title,
            mimeType = "text/html",
            isScreenshot = false,
            textSnippet = metadata.description,
            rawUrl = url
        )

        val item = StashItem(
            uri = metadata.url,
            originalPath = metadata.domain,
            title = metadata.title,
            itemType = StashItemType.LINK,
            category = userCategory ?: "Links",
            tags = analysis.tags.joinToString(","),
            extractedText = metadata.description ?: metadata.url,
            vendor = analysis.vendor ?: metadata.domain,
            mimeType = "text/html",
            notes = metadata.description,
            folderId = folderId
        )

        stashDao.insert(item)
    }

    suspend fun addNote(
        title: String,
        content: String,
        category: String = "Other",
        folderId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val analysis = SmartCharacterizer.analyze(
            originalName = title,
            mimeType = "text/plain",
            isScreenshot = false,
            textSnippet = content
        )

        val item = StashItem(
            uri = "stash://note/${System.currentTimeMillis()}",
            title = title.ifBlank { "Untitled Note" },
            itemType = StashItemType.NOTE,
            category = if (category != "Other") category else analysis.category,
            tags = analysis.tags.joinToString(","),
            extractedText = content,
            notes = content,
            vendor = analysis.vendor,
            eventDate = analysis.eventDate,
            expiryDate = analysis.expiryDate,
            mimeType = "text/plain",
            folderId = folderId
        )

        val id = stashDao.insert(item)

        if (analysis.suggestedTracker != null && analysis.suggestedTracker.isHighConfidence) {
            trackerDao.insert(
                TrackerItem(
                    stashItemId = id,
                    title = analysis.suggestedTracker.title,
                    trackerType = analysis.suggestedTracker.type,
                    targetDate = analysis.suggestedTracker.targetDate,
                    notes = analysis.suggestedTracker.promptReason
                )
            )
        }

        id
    }

    // --- ITEM ACTIONS & FILE OPERATIONS ---

    suspend fun updateItem(item: StashItem) = withContext(Dispatchers.IO) {
        stashDao.update(item.copy(modifiedTimestamp = System.currentTimeMillis()))
    }

    suspend fun renameItem(item: StashItem, newTitle: String) = withContext(Dispatchers.IO) {
        if (newTitle.isNotBlank()) {
            stashDao.update(item.copy(title = newTitle.trim(), modifiedTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun replaceMedia(
        item: StashItem,
        newUri: Uri,
        newName: String?,
        newMimeType: String?,
        newSize: Long
    ) = withContext(Dispatchers.IO) {
        val displayName = newName ?: newUri.lastPathSegment ?: item.title
        val analysis = SmartCharacterizer.analyze(
            originalName = displayName,
            mimeType = newMimeType,
            isScreenshot = item.isScreenshot,
            textSnippet = displayName
        )

        val updated = item.copy(
            uri = newUri.toString(),
            originalPath = newUri.path,
            title = if (item.title.isBlank() || item.title == "Untitled") analysis.title else item.title,
            mimeType = newMimeType,
            fileSize = newSize,
            modifiedTimestamp = System.currentTimeMillis()
        )
        stashDao.update(updated)
    }

    suspend fun togglePin(item: StashItem) = withContext(Dispatchers.IO) {
        stashDao.setPinned(item.id, !item.isPinned)
    }

    suspend fun togglePinBulk(ids: List<Long>, pin: Boolean) = withContext(Dispatchers.IO) {
        stashDao.setPinnedBulk(ids, pin)
    }

    suspend fun moveItemsToFolder(ids: List<Long>, folderId: Long?) = withContext(Dispatchers.IO) {
        stashDao.updateItemsFolder(ids, folderId)
    }

    suspend fun copyItem(item: StashItem, targetFolderId: Long?): StashItem = withContext(Dispatchers.IO) {
        // If copying to the same location, append " (Copy)"
        val newTitle = if (item.folderId == targetFolderId) {
            "${item.title} (Copy)"
        } else {
            item.title
        }

        val copiedItem = item.copy(
            id = 0L, // new auto-generate ID
            title = newTitle,
            folderId = targetFolderId,
            addedTimestamp = System.currentTimeMillis(),
            modifiedTimestamp = System.currentTimeMillis(),
            isTrashed = false,
            trashedTimestamp = null
        )
        val newId = stashDao.insert(copiedItem)
        copiedItem.copy(id = newId)
    }

    suspend fun copyItemsBulk(items: List<StashItem>, targetFolderId: Long?): List<StashItem> =
        withContext(Dispatchers.IO) {
            items.map { copyItem(it, targetFolderId) }
        }

    // --- TRASH & RETENTION OPERATIONS ---

    suspend fun moveToTrash(item: StashItem, locationName: String? = null) = withContext(Dispatchers.IO) {
        val folder = item.folderId?.let { folderDao.getFolderByIdDirect(it) }
        val location = locationName ?: folder?.name ?: "Library"
        val folderPath = folder?.name
        stashDao.moveToTrash(item.id, System.currentTimeMillis(), location, folderPath)
    }

    suspend fun moveToTrashBulk(items: List<StashItem>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        items.forEach { item ->
            val folder = item.folderId?.let { folderDao.getFolderByIdDirect(it) }
            val folderPath = folder?.name
            stashDao.moveToTrash(item.id, now, folder?.name ?: "Library", folderPath)
        }
    }

    data class RestoreOutcome(
        val restoredCount: Int,
        val fallbackCount: Int
    )

    suspend fun restoreItem(item: StashItem): Boolean = withContext(Dispatchers.IO) {
        // Check if original folder still exists
        val originalFolderExists = item.originalFolderId?.let {
            folderDao.getFolderByIdDirect(it) != null
        } ?: true

        val targetFolder = if (originalFolderExists) item.originalFolderId else null

        // Handle conflict if active item with same title exists in target location
        val existingWithSameTitle = stashDao.findItemsByTitle(item.title)
        val finalTitle = if (existingWithSameTitle.any { it.id != item.id && it.folderId == targetFolder }) {
            "${item.title} (Restored)"
        } else {
            item.title
        }

        stashDao.update(
            item.copy(
                isTrashed = false,
                trashedTimestamp = null,
                folderId = targetFolder,
                title = finalTitle,
                modifiedTimestamp = System.currentTimeMillis()
            )
        )
        originalFolderExists
    }

    suspend fun restoreItemsBulk(items: List<StashItem>): RestoreOutcome = withContext(Dispatchers.IO) {
        var fallbackCount = 0
        items.forEach { item ->
            val folderExists = restoreItem(item)
            if (!folderExists && item.originalFolderId != null) {
                fallbackCount++
            }
        }
        RestoreOutcome(restoredCount = items.size, fallbackCount = fallbackCount)
    }

    suspend fun restoreAllTrash(): RestoreOutcome = withContext(Dispatchers.IO) {
        val trashed = stashDao.getTrashedItemsDirect()
        restoreItemsBulk(trashed)
    }

    suspend fun deletePermanently(item: StashItem) = withContext(Dispatchers.IO) {
        trackerDao.deleteByStashItemId(item.id)
        stashDao.deleteById(item.id)
    }

    suspend fun deletePermanentlyBulk(items: List<StashItem>) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            trackerDao.deleteByStashItemId(item.id)
        }
        stashDao.deletePermanentlyBulk(items.map { it.id })
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashed = stashDao.getTrashedItemsDirect()
        trashed.forEach { item ->
            trackerDao.deleteByStashItemId(item.id)
        }
        stashDao.emptyTrash()
    }

    suspend fun cleanupExpiredTrash(): Int = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
        val deletedCount = stashDao.deleteExpiredTrash(thirtyDaysAgo)
        if (deletedCount > 0) {
            Log.i("StashRepository", "Cleaned up $deletedCount expired items from Trash (30-day policy).")
        }
        deletedCount
    }

    // --- FOLDER OPERATIONS ---

    suspend fun createFolder(name: String, parentId: Long? = null, colorHex: String? = null): Long =
        withContext(Dispatchers.IO) {
            val cleanName = name.trim().ifBlank { "New Folder" }
            val existing = folderDao.findFolderByName(cleanName, parentId)
            if (existing != null) {
                existing.id
            } else {
                folderDao.insert(
                    FolderItem(
                        name = cleanName,
                        parentFolderId = parentId,
                        colorHex = colorHex
                    )
                )
            }
        }

    suspend fun renameFolder(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val folder = folderDao.getFolderByIdDirect(id) ?: return@withContext
        folderDao.update(folder.copy(name = newName.trim().ifBlank { folder.name }))
    }

    suspend fun deleteFolder(folder: FolderItem) = withContext(Dispatchers.IO) {
        // Move folder to trash with retention policy
        moveFolderToTrash(folder)
    }

    suspend fun moveFolderToTrash(folder: FolderItem) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        folderDao.moveToTrash(folder.id, now)
        stashDao.moveFolderContentsToTrash(folder.id, now, folder.name)
    }

    suspend fun restoreFolder(folder: FolderItem) = withContext(Dispatchers.IO) {
        folderDao.restoreFolder(folder.id)
        stashDao.restoreFolderContentsFromTrash(folder.id)
    }

    suspend fun deleteFolderPermanently(folder: FolderItem) = withContext(Dispatchers.IO) {
        stashDao.deleteFolderContentsPermanently(folder.id)
        folderDao.deletePermanently(folder.id)
    }

    // --- TRACKERS ---

    suspend fun confirmSuggestedTracker(tracker: TrackerItem) = withContext(Dispatchers.IO) {
        trackerDao.update(tracker.copy(isConfirmedByUser = true))
    }

    suspend fun dismissSuggestedTracker(tracker: TrackerItem) = withContext(Dispatchers.IO) {
        trackerDao.delete(tracker)
    }

    suspend fun addTracker(
        title: String,
        type: TrackerType,
        targetDate: Long,
        stashItemId: Long? = null,
        reminderDays: Int = 7,
        notes: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val tracker = TrackerItem(
            stashItemId = stashItemId,
            title = title,
            trackerType = type,
            targetDate = targetDate,
            reminderDaysBefore = reminderDays,
            notes = notes
        )
        trackerDao.insert(tracker)
    }

    suspend fun toggleTrackerCompleted(tracker: TrackerItem) = withContext(Dispatchers.IO) {
        trackerDao.update(tracker.copy(isCompleted = !tracker.isCompleted))
    }

    suspend fun deleteTracker(tracker: TrackerItem) = withContext(Dispatchers.IO) {
        trackerDao.delete(tracker)
    }
}
