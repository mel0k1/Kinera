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

    @Query("SELECT * FROM favourites ORDER BY created_at ASC")
    suspend fun getAll(): List<FavouriteEntity>

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

    @Query(
        "UPDATE favourites SET total_chapters = :total, last_chapter_id = :lastChapterId, " +
            "new_chapters = :newChapters, checked_at = :checkedAt WHERE manga_id = :mangaId",
    )
    suspend fun updateChapterInfo(
        mangaId: String,
        total: Int,
        lastChapterId: String?,
        newChapters: Int,
        checkedAt: Long,
    )

    @Query("UPDATE favourites SET new_chapters = 0 WHERE manga_id = :mangaId")
    suspend fun clearNewChapters(mangaId: String)
}

@Dao
interface CategoriesDao {

    @Query("SELECT * FROM categories ORDER BY position ASC, created_at ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position ASC, created_at ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE categories SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)
}
