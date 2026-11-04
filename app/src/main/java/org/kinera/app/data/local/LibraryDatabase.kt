package org.kinera.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.kinera.app.data.local.dao.CategoriesDao
import org.kinera.app.data.local.dao.FavouritesDao
import org.kinera.app.data.local.dao.HistoryDao
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.local.entity.FavouriteEntity
import org.kinera.app.data.local.entity.HistoryEntity

@Database(
    entities = [FavouriteEntity::class, HistoryEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun favouritesDao(): FavouritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun categoriesDao(): CategoriesDao
}
