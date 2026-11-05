package org.kinera.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "manga_id") val mangaId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val title: String,
    @ColumnInfo(name = "alt_title") val altTitle: String?,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "manga_json") val mangaJson: String,
    /** Chapter count at the last library update. */
    @ColumnInfo(name = "total_chapters") val totalChapters: Int = 0,
    /** Id of the newest chapter at the last update; null before the first run. */
    @ColumnInfo(name = "last_chapter_id") val lastChapterId: String? = null,
    /** Chapters that appeared since the user last opened the title. */
    @ColumnInfo(name = "new_chapters") val newChapters: Int = 0,
    /** Timestamp of the last successful library update for this title. */
    @ColumnInfo(name = "checked_at") val checkedAt: Long = 0,
)
