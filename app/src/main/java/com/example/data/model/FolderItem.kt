package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentFolderId"]),
        Index(value = ["name"]),
        Index(value = ["isTrashed"])
    ]
)
data class FolderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val parentFolderId: Long? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val colorHex: String? = null,
    val isTrashed: Boolean = false,
    val trashedTimestamp: Long? = null,
    val originalParentFolderId: Long? = null
) {
    fun trashDaysRemaining(fromMillis: Long = System.currentTimeMillis()): Long {
        val trashedAt = trashedTimestamp ?: return 30L
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        val expiryTime = trashedAt + thirtyDaysMillis
        val diff = expiryTime - fromMillis
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return days.coerceAtLeast(0L)
    }
}
