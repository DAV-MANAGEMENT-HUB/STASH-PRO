package com.example.engine

import com.example.data.model.StashItem

data class CategoryStorageUsage(
    val category: String,
    val totalBytes: Long,
    val count: Int,
    val percentage: Float
)

data class DuplicateGroup(
    val name: String,
    val sizePerFile: Long,
    val items: List<StashItem>,
    val reclaimableBytes: Long
)

data class StorageOverview(
    val totalActiveBytes: Long,
    val totalTrashBytes: Long,
    val totalItemCount: Int,
    val largestItems: List<StashItem>,
    val categoryBreakdown: List<CategoryStorageUsage>,
    val duplicateGroups: List<DuplicateGroup>
) {
    fun formattedActiveSize(): String = formatBytes(totalActiveBytes)
    fun formattedTrashSize(): String = formatBytes(totalTrashBytes)
    fun formattedTotalReclaimableDuplicates(): String =
        formatBytes(duplicateGroups.sumOf { it.reclaimableBytes })

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

object StorageAnalyzer {

    fun analyze(activeItems: List<StashItem>, trashedItems: List<StashItem>): StorageOverview {
        val totalActiveBytes = activeItems.sumOf { it.fileSize }
        val totalTrashBytes = trashedItems.sumOf { it.fileSize }

        // Largest items
        val largest = activeItems.filter { it.fileSize > 0 }
            .sortedByDescending { it.fileSize }
            .take(5)

        // Category breakdown
        val categoryGroups = activeItems.groupBy { it.category }
        val categoryBreakdown = categoryGroups.map { (cat, items) ->
            val bytes = items.sumOf { it.fileSize }
            val pct = if (totalActiveBytes > 0) (bytes.toFloat() / totalActiveBytes.toFloat()) * 100f else 0f
            CategoryStorageUsage(
                category = cat,
                totalBytes = bytes,
                count = items.size,
                percentage = pct
            )
        }.sortedByDescending { it.totalBytes }

        // Duplicate files detection
        val duplicateGroups = mutableListOf<DuplicateGroup>()
        val bySize = activeItems.filter { it.fileSize > 1024L }.groupBy { it.fileSize }
        for ((size, items) in bySize) {
            if (items.size > 1) {
                // Group by title or filename
                val subGroups = items.groupBy { it.title.trim().lowercase() }
                for ((name, matchingItems) in subGroups) {
                    if (matchingItems.size > 1) {
                        val wasted = (matchingItems.size - 1) * size
                        duplicateGroups.add(
                            DuplicateGroup(
                                name = matchingItems.first().title,
                                sizePerFile = size,
                                items = matchingItems,
                                reclaimableBytes = wasted
                            )
                        )
                    }
                }
            }
        }

        return StorageOverview(
            totalActiveBytes = totalActiveBytes,
            totalTrashBytes = totalTrashBytes,
            totalItemCount = activeItems.size,
            largestItems = largest,
            categoryBreakdown = categoryBreakdown,
            duplicateGroups = duplicateGroups
        )
    }
}
