package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "trackers",
    indices = [
        Index(value = ["stashItemId"]),
        Index(value = ["targetDate"]),
        Index(value = ["isCompleted"]),
        Index(value = ["trackerType"])
    ]
)
data class TrackerItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val stashItemId: Long? = null,
    val title: String,
    val trackerType: TrackerType,
    val targetDate: Long,
    val reminderDaysBefore: Int = 7,
    val isRecurring: Boolean = false,
    val recurrenceIntervalMonths: Int? = null,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val amount: Double? = null,
    val currency: String? = null,
    val confidence: TrackerConfidence = TrackerConfidence.CONFIRMED,
    val sourceDescription: String? = null,
    val isConfirmedByUser: Boolean = true,
    val packageName: String? = null
) {
    /**
     * Returns remaining days from now.
     * Negative means overdue/expired.
     */
    fun daysRemaining(fromMillis: Long = System.currentTimeMillis()): Long {
        val diff = targetDate - fromMillis
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun isNeedsAttention(thresholdDays: Long = 3): Boolean {
        if (isCompleted) return false
        val days = daysRemaining()
        return days <= thresholdDays
    }

    fun isExpiringSoon(thresholdDays: Long = 30): Boolean {
        val days = daysRemaining()
        return !isCompleted && days in 0..thresholdDays
    }

    fun isOverdue(): Boolean {
        return !isCompleted && daysRemaining() < 0
    }
}
