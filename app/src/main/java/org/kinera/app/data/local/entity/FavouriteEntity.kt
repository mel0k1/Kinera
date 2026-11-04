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
)
