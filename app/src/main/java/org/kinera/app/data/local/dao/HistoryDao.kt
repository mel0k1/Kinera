package org.kinera.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.kinera.app.data.local.entity.HistoryEntity

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE manga_id = :mangaId LIMIT 1")
    suspend fun get(mangaId: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE manga_id = :mangaId")
    suspend fun delete(mangaId: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}
