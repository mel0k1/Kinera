package org.kinera.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.kinera.app.data.local.dao.BookmarksDao
import org.kinera.app.data.local.dao.CategoriesDao
import org.kinera.app.data.local.dao.DownloadsDao
import org.kinera.app.data.local.dao.FavouritesDao
import org.kinera.app.data.local.dao.HistoryDao
import org.kinera.app.data.local.entity.BookmarkEntity
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.local.entity.DownloadEntity
import org.kinera.app.data.local.entity.FavouriteEntity
import org.kinera.app.data.local.entity.HistoryEntity

@Database(
    entities = [FavouriteEntity::class, HistoryEntity::class, CategoryEntity::class, DownloadEntity::class, BookmarkEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun favouritesDao(): FavouritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun downloadsDao(): DownloadsDao
    abstract fun bookmarksDao(): BookmarksDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `downloads` (" +
                        "`chapter_id` TEXT NOT NULL PRIMARY KEY, " +
                        "`manga_id` TEXT NOT NULL, " +
                        "`source_id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`chapter_label` TEXT NOT NULL, " +
                        "`cover_url` TEXT, " +
                        "`state` INTEGER NOT NULL, " +
                        "`progress` INTEGER NOT NULL, " +
                        "`total` INTEGER NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, " +
                        "`error` TEXT)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookmarks` (" +
                        "`manga_id` TEXT NOT NULL, " +
                        "`chapter_id` TEXT NOT NULL, " +
                        "`page` INTEGER NOT NULL, " +
                        "`source_id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`cover_url` TEXT, " +
                        "`chapter_label` TEXT NOT NULL, " +
                        "`page_url` TEXT, " +
                        "`created_at` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`manga_id`, `chapter_id`, `page`))",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Library auto-update bookkeeping for favourites.
                db.execSQL("ALTER TABLE `favourites` ADD COLUMN `total_chapters` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `favourites` ADD COLUMN `last_chapter_id` TEXT")
                db.execSQL("ALTER TABLE `favourites` ADD COLUMN `new_chapters` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `favourites` ADD COLUMN `checked_at` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
