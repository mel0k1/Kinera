package org.kinera.app.data.mangadex.dto

import kotlinx.serialization.Serializable

@Serializable
data class MangaDexCollectionResponse<T>(
    val result: String,
    val data: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class MangaDexEntityResponse<T>(
    val result: String,
    val data: T,
)

@Serializable
data class MangaDexManga(
    val id: String,
    val type: String? = null,
    val attributes: MangaAttributes = MangaAttributes(),
    val relationships: List<MangaDexRelationship> = emptyList(),
)

@Serializable
data class MangaAttributes(
    val title: Map<String, String>? = null,
    val altTitles: List<Map<String, String>> = emptyList(),
    val description: Map<String, String>? = null,
    val status: String? = null,
    val tags: List<MangaDexTag> = emptyList(),
    val lastChapter: String? = null,
)

@Serializable
data class MangaDexTag(
    val id: String,
    val attributes: TagAttributes = TagAttributes(),
)

@Serializable
data class TagAttributes(
    val name: Map<String, String>? = null,
)

@Serializable
data class MangaDexRelationship(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributes? = null,
)

@Serializable
data class RelationshipAttributes(
    val name: String? = null,
    val fileName: String? = null,
)

@Serializable
data class MangaDexChapter(
    val id: String,
    val type: String? = null,
    val attributes: ChapterAttributes = ChapterAttributes(),
    val relationships: List<MangaDexRelationship> = emptyList(),
)

@Serializable
data class ChapterAttributes(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val translatedLanguage: String? = null,
    val externalUrl: String? = null,
    val pages: Int = 0,
    val publishAt: String? = null,
    val readableAt: String? = null,
)

@Serializable
data class AtHomeResponse(
    val baseUrl: String,
    val chapter: AtHomeChapter = AtHomeChapter(),
)

@Serializable
data class AtHomeChapter(
    val hash: String = "",
    val data: List<String> = emptyList(),
    val dataSaver: List<String> = emptyList(),
)
