package org.kinera.app.data.suwayomi.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: JsonObject? = null,
)

@Serializable
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLError> = emptyList(),
) {
    fun requireData(): T =
        data ?: error(errors.firstOrNull()?.message ?: "Empty GraphQL response")
}

@Serializable
data class GraphQLError(
    val message: String? = null,
)

/* sources { nodes { ... } } */

@Serializable
data class SourcesData(
    val sources: SourceNodeList = SourceNodeList(),
)

@Serializable
data class SourceNodeList(
    val nodes: List<SourceNode> = emptyList(),
)

@Serializable
data class SourceNode(
    val id: String,
    val name: String? = null,
    val displayName: String? = null,
    val lang: String? = null,
    val iconUrl: String? = null,
)

/* fetchSourceManga(input: ...) { ... } */

@Serializable
data class FetchMangaData(
    val fetchSourceManga: FetchSourceMangaPayload = FetchSourceMangaPayload(),
)

@Serializable
data class FetchSourceMangaPayload(
    val hasNextPage: Boolean = false,
    val mangas: List<MangaNode> = emptyList(),
)

@Serializable
data class MangaNode(
    val id: Long = 0,
    val title: String = "",
    val thumbnailUrl: String? = null,
    val sourceId: String = "",
    val inLibrary: Boolean = false,
)

/* manga(id: ...) { ... } */

@Serializable
data class MangaDetailsData(
    val manga: MangaDetailsNode? = null,
)

@Serializable
data class MangaDetailsNode(
    val id: Long = 0,
    val title: String = "",
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val status: String? = null,
    val genre: List<String> = emptyList(),
    val realUrl: String? = null,
    val sourceId: String = "",
    val chapters: ChapterNodeList = ChapterNodeList(),
)

@Serializable
data class ChapterNodeList(
    val totalCount: Int = 0,
    val nodes: List<ChapterNode> = emptyList(),
)

@Serializable
data class ChapterNode(
    val id: Long = 0,
    val name: String = "",
    val mangaId: Long = 0,
    val scanlator: String? = null,
    val sourceOrder: Int = 0,
    val chapterNumber: Double = 0.0,
    val isRead: Boolean = false,
    val uploadDate: Long? = null,
    val pageCount: Int = 0,
)
