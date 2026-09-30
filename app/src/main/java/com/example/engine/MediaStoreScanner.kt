package com.example.engine

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.StashDao
import com.example.data.db.TrackerDao
import com.example.data.model.StashItem
import com.example.data.model.StashItemType
import com.example.data.model.TrackerItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreScanner(
    private val context: Context,
    private val stashDao: StashDao,
    private val trackerDao: TrackerDao
) {
    suspend fun scanAccessibleMedia(maxImages: Int = 100, maxDocs: Int = 50): Int =
        withContext(Dispatchers.IO) {
            var importedCount = 0

            // 1. Scan Images (Photos & Screenshots)
            try {
                importedCount += scanImages(maxImages)
            } catch (e: Exception) {
                Log.e("MediaStoreScanner", "Error scanning images", e)
            }

            // 2. Scan Documents (PDFs, Text)
            try {
                importedCount += scanDocuments(maxDocs)
            } catch (e: Exception) {
                Log.e("MediaStoreScanner", "Error scanning documents", e)
            }

            importedCount
        }

    private suspend fun scanImages(limit: Int): Int {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = resolver.query(
            collection,
            projection.toTypedArray(),
            null,
            null,
            sortOrder
        ) ?: return 0

        val urisToQuery = mutableListOf<String>()
        val rawItems = mutableListOf<ScannedRawItem>()

        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val bucketCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            } else -1

            var count = 0
            while (c.moveToNext() && count < limit) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "Image_$id"
                val mime = c.getString(mimeCol) ?: "image/jpeg"
                val dateSec = c.getLong(dateCol)
                val size = c.getLong(sizeCol)
                val bucket = if (bucketCol >= 0) c.getString(bucketCol) else null
                val relPath = if (relPathCol >= 0) c.getString(relPathCol) else null

                val contentUri = ContentUris.withAppendedId(collection, id).toString()
                urisToQuery.add(contentUri)

                val isScreenshot = isScreenshotDetection(name, bucket, relPath)

                rawItems.add(
                    ScannedRawItem(
                        uri = contentUri,
                        displayName = name,
                        mimeType = mime,
                        addedTimestamp = if (dateSec > 0) dateSec * 1000 else System.currentTimeMillis(),
                        fileSize = size,
                        isScreenshot = isScreenshot,
                        bucket = bucket
                    )
                )
                count++
            }
        }

        // Deduplicate against existing Room items
        val existing = stashDao.findItemsByUris(urisToQuery).map { it.uri }.toSet()
        val newItems = rawItems.filter { it.uri !in existing }

        var countAdded = 0
        for (item in newItems) {
            val analysis = SmartCharacterizer.analyze(
                originalName = item.displayName,
                mimeType = item.mimeType,
                isScreenshot = item.isScreenshot,
                textSnippet = item.bucket
            )

            val stashItem = StashItem(
                uri = item.uri,
                originalPath = item.bucket,
                title = analysis.title,
                itemType = analysis.itemType,
                category = analysis.category,
                tags = analysis.tags.joinToString(","),
                extractedText = item.bucket ?: "",
                vendor = analysis.vendor,
                product = analysis.product,
                person = analysis.person,
                detectedAmount = analysis.detectedAmount,
                currency = analysis.currency,
                eventDate = analysis.eventDate,
                expiryDate = analysis.expiryDate,
                addedTimestamp = item.addedTimestamp,
                modifiedTimestamp = item.addedTimestamp,
                isScreenshot = item.isScreenshot,
                fileSize = item.fileSize,
                mimeType = item.mimeType,
                isUncertainClassification = analysis.isUncertain
            )

            val insertedId = stashDao.insert(stashItem)
            countAdded++

            // Automatic tracker creation if high confidence
            if (analysis.suggestedTracker != null && analysis.suggestedTracker.isHighConfidence) {
                trackerDao.insert(
                    TrackerItem(
                        stashItemId = insertedId,
                        title = analysis.suggestedTracker.title,
                        trackerType = analysis.suggestedTracker.type,
                        targetDate = analysis.suggestedTracker.targetDate,
                        notes = analysis.suggestedTracker.promptReason
                    )
                )
            }
        }

        return countAdded
    }

    private suspend fun scanDocuments(limit: Int): Int {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.pdf'"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val cursor = resolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        ) ?: return 0

        val urisToQuery = mutableListOf<String>()
        val rawItems = mutableListOf<ScannedRawItem>()

        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

            var count = 0
            while (c.moveToNext() && count < limit) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "Document_$id"
                val mime = c.getString(mimeCol) ?: "application/pdf"
                val dateSec = c.getLong(dateCol)
                val size = c.getLong(sizeCol)

                val contentUri = ContentUris.withAppendedId(collection, id).toString()
                urisToQuery.add(contentUri)

                rawItems.add(
                    ScannedRawItem(
                        uri = contentUri,
                        displayName = name,
                        mimeType = mime,
                        addedTimestamp = if (dateSec > 0) dateSec * 1000 else System.currentTimeMillis(),
                        fileSize = size,
                        isScreenshot = false,
                        bucket = "Documents"
                    )
                )
                count++
            }
        }

        val existing = stashDao.findItemsByUris(urisToQuery).map { it.uri }.toSet()
        val newItems = rawItems.filter { it.uri !in existing }

        var countAdded = 0
        for (item in newItems) {
            val analysis = SmartCharacterizer.analyze(
                originalName = item.displayName,
                mimeType = item.mimeType,
                isScreenshot = false,
                textSnippet = item.displayName
            )

            val stashItem = StashItem(
                uri = item.uri,
                originalPath = item.displayName,
                title = analysis.title,
                itemType = analysis.itemType,
                category = analysis.category,
                tags = analysis.tags.joinToString(","),
                extractedText = item.displayName,
                vendor = analysis.vendor,
                product = analysis.product,
                person = analysis.person,
                detectedAmount = analysis.detectedAmount,
                currency = analysis.currency,
                eventDate = analysis.eventDate,
                expiryDate = analysis.expiryDate,
                addedTimestamp = item.addedTimestamp,
                modifiedTimestamp = item.addedTimestamp,
                isScreenshot = false,
                fileSize = item.fileSize,
                mimeType = item.mimeType,
                isUncertainClassification = analysis.isUncertain
            )

            val insertedId = stashDao.insert(stashItem)
            countAdded++

            if (analysis.suggestedTracker != null && analysis.suggestedTracker.isHighConfidence) {
                trackerDao.insert(
                    TrackerItem(
                        stashItemId = insertedId,
                        title = analysis.suggestedTracker.title,
                        trackerType = analysis.suggestedTracker.type,
                        targetDate = analysis.suggestedTracker.targetDate,
                        notes = analysis.suggestedTracker.promptReason
                    )
                )
            }
        }

        return countAdded
    }

    private fun isScreenshotDetection(name: String, bucket: String?, relPath: String?): Boolean {
        val lowerName = name.lowercase()
        val lowerBucket = bucket?.lowercase() ?: ""
        val lowerPath = relPath?.lowercase() ?: ""

        return lowerName.contains("screenshot") ||
                lowerName.contains("screen_shot") ||
                lowerBucket.contains("screenshot") ||
                lowerPath.contains("screenshot")
    }

    private data class ScannedRawItem(
        val uri: String,
        val displayName: String,
        val mimeType: String,
        val addedTimestamp: Long,
        val fileSize: Long,
        val isScreenshot: Boolean,
        val bucket: String?
    )
}
