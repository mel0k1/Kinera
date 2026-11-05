package org.kinera.app.data.remanga.dto

import kotlinx.serialization.Serializable

/** One entry of the catalog / search results list. */
@Serializable
data class RemangaMangaShort(
    val id: Long = 0,
    val dir: String,
    val main_name: String,
    val secondary_name: String? = null,
    val cover: RemangaCover? = null,
    val status: RemangaLabel? = null,
    val avg_rating: Double? = null,
    val genres: List<RemangaGenre> = emptyList(),
)

/** Full title, served by the details endpoint. */
@Serializable
data class RemangaManga(
    val dir: String,
    val main_name: String,
    val secondary_name: String? = null,
    val another_name: String? = null,
    val description: String? = null,
    val cover: RemangaCover? = null,
    val status: RemangaLabel? = null,
    val age_limit: RemangaLabel? = null,
    val branches: List<RemangaBranch> = emptyList(),
    val genres: List<RemangaGenre> = emptyList(),
    val count_chapters: Int = 0,
)

@Serializable
data class RemangaCover(
    val low: String? = null,
    val mid: String? = null,
    val high: String? = null,
)

@Serializable
data class RemangaLabel(
    val id: Int = 0,
    val name: String? = null,
)

@Serializable
data class RemangaGenre(
    val id: Int = 0,
    val name: String,
)

/** Translation branch; chapters hang off its id. */
@Serializable
data class RemangaBranch(
    val id: Long = 0,
    val publishers: List<RemangaPublisher> = emptyList(),
)

@Serializable
data class RemangaPublisher(
    val id: Long = 0,
    val name: String? = null,
)

/** One chapter of a branch, as returned by the chapters listing. */
@Serializable
data class RemangaChapter(
    val id: Long,
    val chapter: Double? = null,
    val tome: Int? = null,
    val name: String? = null,
    val upload_date: String? = null,
    val is_paid: Boolean = false,
    val is_bought: Boolean = false,
    val publishers: List<RemangaPublisher> = emptyList(),
    val branch_id: Long? = null,
)

/** Page list of one chapter: flat objects or two-page spreads. */
@Serializable
data class RemangaChapterPages(
    val pages: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    val pub_date: String? = null,
)
