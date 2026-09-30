package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.FolderItem
import com.example.data.model.StashItem
import com.example.data.model.TrackerItem

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create folders table if missing
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `folders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`parentFolderId` INTEGER, " +
                "`createdTimestamp` INTEGER NOT NULL, " +
                "`colorHex` TEXT, " +
                "`isTrashed` INTEGER NOT NULL, " +
                "`trashedTimestamp` INTEGER, " +
                "`originalParentFolderId` INTEGER)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_parentFolderId` ON `folders` (`parentFolderId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_name` ON `folders` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_folders_isTrashed` ON `folders` (`isTrashed`)")

        // Ensure stash_items table has folder and trash columns
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `folderId` INTEGER")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `originalFolderId` INTEGER")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `originalLocation` TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `originalFolderPath` TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `fileHash` TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `isTrashed` INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `stash_items` ADD COLUMN `trashedTimestamp` INTEGER")
        } catch (_: Exception) {}
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_stash_items_isTrashed` ON `stash_items` (`isTrashed`)")
        } catch (_: Exception) {}
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_stash_items_folderId` ON `stash_items` (`folderId`)")
        } catch (_: Exception) {}

        // Ensure trackers table has new columns
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `confidence` TEXT NOT NULL DEFAULT 'CONFIRMED'")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `sourceDescription` TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `isConfirmedByUser` INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `packageName` TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `amount` REAL")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `currency` TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `trackers` ADD COLUMN `recurrenceIntervalMonths` INTEGER")
        } catch (_: Exception) {}
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_2_3.migrate(db)
    }
}

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_2_3.migrate(db)
    }
}

@Database(
    entities = [
        StashItem::class,
        TrackerItem::class,
        FolderItem::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun stashDao(): StashDao
    abstract fun trackerDao(): TrackerDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stash_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
