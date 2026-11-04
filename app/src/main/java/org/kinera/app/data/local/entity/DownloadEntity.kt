package org.kinera.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per chapter queued for offline reading. Files themselves live in
 * the download directory managed by [org.kinera.app.data.downloads.DownloadStorage].
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    @ColumnInfo(name = "manga_id") val mangaId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val title: String,
    @ColumnInfo(name = "chapter_label") val chapterLabel: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    val state: Int,
    val progress: Int,
    val total: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val error: String?,
) {
    val isDone: Boolean get() = state == DownloadState.DONE
    val isActive: Boolean get() = state == DownloadState.QUEUED || state == DownloadState.RUNNING
}

object DownloadState {
    const val QUEUED = 0
    const val RUNNING = 1
    const val DONE = 2
    const val FAILED = 3
}
