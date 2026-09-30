package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.StashItemType
import com.example.data.model.TrackerConfidence
import com.example.data.model.TrackerType

class Converters {
    @TypeConverter
    fun fromStashItemType(value: StashItemType): String = value.name

    @TypeConverter
    fun toStashItemType(value: String): StashItemType {
        return try {
            StashItemType.valueOf(value)
        } catch (_: Exception) {
            StashItemType.OTHER
        }
    }

    @TypeConverter
    fun fromTrackerType(value: TrackerType): String = value.name

    @TypeConverter
    fun toTrackerType(value: String): TrackerType {
        return try {
            TrackerType.valueOf(value)
        } catch (_: Exception) {
            TrackerType.CUSTOM
        }
    }

    @TypeConverter
    fun fromTrackerConfidence(value: TrackerConfidence): String = value.name

    @TypeConverter
    fun toTrackerConfidence(value: String): TrackerConfidence {
        return try {
            TrackerConfidence.valueOf(value)
        } catch (_: Exception) {
            TrackerConfidence.CONFIRMED
        }
    }
}
