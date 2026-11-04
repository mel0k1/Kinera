package org.kinera.app.data.komga.dto

import kotlinx.serialization.Serializable

/** Spring-style page wrapper returned by Komga. */
@Serializable
data class KomgaPageDto<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val number: Int = 0,
    val size: Int = content.size,
    val last: Boolean = true,
)

@Serializable
data class KomgaSeries(
    val id: String,
    val name: String = "",
    val booksCount: Int = 0,
    val metadata: KomgaSeriesMetadata = KomgaSeriesMetadata(),
    val booksMetadata: KomgaBooksMetadata = KomgaBooksMetadata(),
)

@Serializable
data class KomgaSeriesMetadata(
    val status: String = "UNKNOWN",
    val summary: String? = null,
)

@Serializable
data class KomgaBooksMetadata(
    val authors: List<KomgaAuthor> = emptyList(),
    val summary: String? = null,
)

@Serializable
data class KomgaAuthor(
    val name: String = "",
    val role: String = "",
)

@Serializable
data class KomgaBook(
    val id: String,
    val name: String = "",
    val metadata: KomgaBookMetadata = KomgaBookMetadata(),
    val media: KomgaBookMedia = KomgaBookMedia(),
)

@Serializable
data class KomgaBookMetadata(
    val title: String? = null,
    val number: String? = null,
    val numberSort: Double? = null,
    val releaseDate: String? = null,
)

@Serializable
data class KomgaBookMedia(
    val status: String = "UNKNOWN",
    val pagesCount: Int = 0,
)
