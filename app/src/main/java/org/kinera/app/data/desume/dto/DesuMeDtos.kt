package org.kinera.app.data.desume.dto

import kotlinx.serialization.Serializable

/** One entry of the catalog listing. */
@Serializable
data class DesuMeMangaShort(
    val id: Long,
    val url: String? = null,
    val russian: String? = null,
    val name: String? = null,
    val image: DesuMeImage? = null,
    val status: String? = null,
    val score: Double? = null,
    val genres: String? = null,
    val description: String? = null,
)

@Serializable
data class DesuMeImage(
    val preview: String? = null,
    val original: String? = null,
)

/** Full entry with chapters and localized genres. */
@Serializable
data class DesuMeManga(
    val id: Long = 0,
    val url: String? = null,
    val russian: String? = null,
    val name: String? = null,
    val status: String? = null,
    val description: String? = null,
    val image: DesuMeImage? = null,
    val genres: List<DesuMeGenre> = emptyList(),
    val chapters: DesuMeChapters? = null,
)

@Serializable
data class DesuMeGenre(
    val text: String? = null,
    val russian: String? = null,
)

@Serializable
data class DesuMeChapters(
    val list: List<DesuMeChapter> = emptyList(),
)

@Serializable
data class DesuMeChapter(
    val id: Long,
    val vol: Int? = null,
    val ch: Double? = null,
    val title: String? = null,
    val date: Long = 0,
)

@Serializable
data class DesuMePages(
    val list: List<DesuMePage> = emptyList(),
)

@Serializable
data class DesuMePage(
    val id: Long = 0,
    val img: String,
)
