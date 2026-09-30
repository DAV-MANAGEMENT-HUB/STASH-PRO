package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "stash_items",
    indices = [
        Index(value = ["uri"], unique = false),
        Index(value = ["category"]),
        Index(value = ["itemType"]),
        Index(value = ["isScreenshot"]),
        Index(value = ["addedTimestamp"]),
        Index(value = ["isTrashed"]),
        Index(value = ["folderId"])
    ]
)
data class StashItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uri: String,
    val originalPath: String? = null,
    val title: String,
    val itemType: StashItemType,
    val category: String,
    val tags: String = "",
    val extractedText: String? = null,
    val vendor: String? = null,
    val product: String? = null,
    val person: String? = null,
    val detectedAmount: Double? = null,
    val currency: String? = null,
    val eventDate: Long? = null,
    val expiryDate: Long? = null,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = System.currentTimeMillis(),
    val isScreenshot: Boolean = false,
    val fileSize: Long = 0L,
    val mimeType: String? = null,
    val isUncertainClassification: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val notes: String? = null,
    val relatedItemIds: String = "",
    val isTrashed: Boolean = false,
    val trashedTimestamp: Long? = null,
    val folderId: Long? = null,
    val originalFolderId: Long? = null,
    val originalLocation: String? = null,
    val originalFolderPath: String? = null,
    val fileHash: String? = null
) {
    fun getTagList(): List<String> =
        tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    fun getRelatedIds(): List<Long> =
        relatedItemIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }

    /**
     * Calculates days remaining until permanent deletion (30 days retention policy).
     */
    fun trashDaysRemaining(fromMillis: Long = System.currentTimeMillis()): Long {
        val trashedAt = trashedTimestamp ?: return 30L
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        val expiryTime = trashedAt + thirtyDaysMillis
        val diff = expiryTime - fromMillis
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return days.coerceAtLeast(0L)
    }

    fun isTrashExpired(fromMillis: Long = System.currentTimeMillis()): Boolean {
        val trashedAt = trashedTimestamp ?: return false
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        return fromMillis >= (trashedAt + thirtyDaysMillis)
    }

    fun formattedSize(): String {
        if (fileSize <= 0) return ""
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
            else -> "$fileSize B"
        }
    }
}
