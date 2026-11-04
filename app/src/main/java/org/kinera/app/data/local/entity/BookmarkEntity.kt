package org.kinera.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * A page the reader was bookmarked at. One bookmark per (manga, chapter,
 * page): toggling the same page again removes it.
 */
@Entity(tableName = "bookmarks", primaryKeys = ["manga_id", "chapter_id", "page"])
data class BookmarkEntity(
    @ColumnInfo(name = "manga_id") val mangaId: String,
    @ColumnInfo(name = "chapter_id") val chapterId: String,
    val page: Int,
    @ColumnInfo(name = "source_id") val sourceId: String,
    val title: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "chapter_label") val chapterLabel: String,
    @ColumnInfo(name = "page_url") val pageUrl: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
