package org.kinera.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kinera.app.data.local.entity.DownloadEntity

@Dao
interface DownloadsDao {

    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE manga_id = :mangaId ORDER BY created_at DESC")
    fun observeForManga(mangaId: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE chapter_id = :chapterId LIMIT 1")
    suspend fun get(chapterId: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE chapter_id = :chapterId AND state = 2)")
    fun observeDownloaded(chapterId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Query(
        "UPDATE downloads SET state = :state, progress = :progress, total = :total, error = :error " +
            "WHERE chapter_id = :chapterId",
    )
    suspend fun updateState(chapterId: String, state: Int, progress: Int, total: Int, error: String?)

    @Query("UPDATE downloads SET progress = :progress WHERE chapter_id = :chapterId")
    suspend fun updateProgress(chapterId: String, progress: Int)

    @Query("DELETE FROM downloads WHERE chapter_id = :chapterId")
    suspend fun delete(chapterId: String)

    @Query("DELETE FROM downloads WHERE manga_id = :mangaId")
    suspend fun deleteForManga(mangaId: String)
}
