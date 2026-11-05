package org.kinera.app.data.mangalib.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class MangaLibData<T>(
    val data: T,
)

/* { data: [...], meta: { has_next_page } } */

@Serializable
data class MangaLibListResponse<T>(
    val data: List<T> = emptyList(),
    val meta: MangaLibPageMeta = MangaLibPageMeta(),
)

@Serializable
data class MangaLibPageMeta(
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
)

/* catalog / search / latest items */

@Serializable
data class MangaLibMangaShort(
    val name: String? = null,
    @SerialName("rus_name") val rusName: String? = null,
    @SerialName("eng_name") val engName: String? = null,
    @SerialName("slug_url") val slugUrl: String = "",
    val cover: MangaLibCover = MangaLibCover(),
)

@Serializable
data class MangaLibCover(
    val default: String? = null,
    val thumbnail: String? = null,
)

/* manga details */

@Serializable
data class MangaLibManga(
    val id: Long = 0,
    val name: String? = null,
    @SerialName("rus_name") val rusName: String? = null,
    @SerialName("eng_name") val engName: String? = null,
    @SerialName("slug_url") val slugUrl: String = "",
    val cover: MangaLibCover = MangaLibCover(),
    val status: MangaLibLabel? = null,
    val genres: List<MangaLibName> = emptyList(),
    val tags: List<MangaLibName> = emptyList(),
    val authors: List<MangaLibName> = emptyList(),
    val artists: List<MangaLibName> = emptyList(),
    val otherNames: List<String> = emptyList(),
    val summary: JsonElement? = null,
) {
    val summaryText: String?
        get() = when (val s = summary) {
            null -> null
            is JsonPrimitive -> s.contentOrNull?.takeIf { it.isNotBlank() } ?: s.toString()
            is JsonObject -> s["content"]?.jsonPrimitive?.contentOrNull
                ?: s.values.firstOrNull()?.jsonPrimitive?.contentOrNull
            else -> null
        }
}

@Serializable
data class MangaLibLabel(
    val id: Int = 0,
    val label: String? = null,
)

@Serializable
data class MangaLibName(
    val name: String? = null,
)

/* chapters */

@Serializable
data class MangaLibChapter(
    val id: Long = 0,
    @SerialName("branches_count") val branchesCount: Int = 0,
    val branches: List<MangaLibBranch> = emptyList(),
    val name: String? = null,
    val number: String? = null,
    val volume: String? = null,
)

@Serializable
data class MangaLibBranch(
    @SerialName("branch_id") val branchId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val teams: List<MangaLibTeam> = emptyList(),
    @SerialName("restricted_view") val restrictedView: MangaLibRestrictedView? = null,
) {
    val isOpen: Boolean get() = restrictedView?.isOpen ?: true
}

@Serializable
data class MangaLibTeam(
    val name: String? = null,
)

@Serializable
data class MangaLibRestrictedView(
    @SerialName("is_open") val isOpen: Boolean = true,
)

@Serializable
data class MangaLibBranchRef(
    val id: Long = 0,
)

/* chapter pages */

@Serializable
data class MangaLibChapterPages(
    val pages: List<MangaLibPage> = emptyList(),
)

@Serializable
data class MangaLibPage(
    /** 1-based position inside the chapter. */
    val slug: Int = 0,
    /** Server-relative path, e.g. "//manga/<slug>/chapters/<id>/x.jpg". */
    val url: String = "",
)

/* constants */

@Serializable
data class MangaLibConstants(
    val imageServers: List<MangaLibImageServer> = emptyList(),
    val genres: List<MangaLibGenre> = emptyList(),
)

@Serializable
data class MangaLibGenre(
    val id: Int = 0,
    val name: String? = null,
    @SerialName("site_ids") val siteIds: List<Int> = emptyList(),
)

@Serializable
data class MangaLibImageServer(
    val id: String = "",
    val url: String = "",
    @SerialName("site_ids") val siteIds: List<Int> = emptyList(),
)
