package org.kinera.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.local.entity.FavouriteEntity

@Dao
interface FavouritesDao {

    @Query("SELECT * FROM favourites ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FavouriteEntity>>

    @Query("SELECT * FROM favourites WHERE manga_id = :mangaId LIMIT 1")
    suspend fun get(mangaId: String): FavouriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE manga_id = :mangaId)")
    fun observeContains(mangaId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favourite: FavouriteEntity)

    @Delete
    suspend fun delete(favourite: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE manga_id = :mangaId")
    suspend fun deleteById(mangaId: String)
}

@Dao
interface CategoriesDao {

    @Query("SELECT * FROM categories ORDER BY created_at ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}
