package org.kinera.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kinera.app.data.local.entity.BookmarkEntity

@Dao
interface BookmarksDao {

    @Query("SELECT * FROM bookmarks WHERE manga_id = :mangaId ORDER BY created_at ASC")
    fun observeForManga(mangaId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM bookmarks " +
            "WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page)",
    )
    fun observeContains(mangaId: String, chapterId: String, page: Int): Flow<Boolean>

    @Query(
        "SELECT * FROM bookmarks WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page LIMIT 1",
    )
    suspend fun get(mangaId: String, chapterId: String, page: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query(
        "DELETE FROM bookmarks WHERE manga_id = :mangaId AND chapter_id = :chapterId AND page = :page",
    )
    suspend fun delete(mangaId: String, chapterId: String, page: Int)

    @Query("DELETE FROM bookmarks WHERE manga_id = :mangaId")
    suspend fun deleteForManga(mangaId: String)
}
