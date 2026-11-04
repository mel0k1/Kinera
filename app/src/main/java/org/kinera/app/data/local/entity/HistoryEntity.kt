package org.kinera.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "manga_id") val mangaId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val title: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    @ColumnInfo(name = "chapter_label") val chapterLabel: String,
    val page: Int,
    val total: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
