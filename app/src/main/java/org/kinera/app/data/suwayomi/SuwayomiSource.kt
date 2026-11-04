package org.kinera.app.data.suwayomi

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.serializer
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaPage
import org.kinera.app.core.model.MangaSource
import org.kinera.app.core.model.MangaState
import org.kinera.app.core.model.MangaTag
import org.kinera.app.core.model.Paged
import org.kinera.app.data.settings.ServerConnection
import org.kinera.app.data.settings.SettingsRepository
import org.kinera.app.data.suwayomi.dto.FetchMangaData
import org.kinera.app.data.suwayomi.dto.GraphQLRequest
import org.kinera.app.data.suwayomi.dto.MangaDetailsData
import org.kinera.app.data.suwayomi.dto.MangaDetailsNode
import org.kinera.app.data.suwayomi.dto.SourcesData
import java.util.Locale

/** One source installed on the Suwayomi server, for the Settings picker. */
data class SuwayomiSourceOption(
    val id: String,
    val name: String,
    val lang: String,
)

/**
 * Catalog source for a self-hosted Suwayomi server (Tachidesk).
 * Catalog, search and details go through the GraphQL API; page and cover
 * images are plain GETs under /api/v1 that the server serves with the same
 * basic-auth the OkHttp interceptor attaches.
 *
 * One Suwayomi server hosts many Tachiyomi sources; this source reads from
 * the one picked in Settings, falling back to the first English source and
 * then to the first listed one, so the app can treat the whole server as a
 * single catalog.
 */
class SuwayomiSource(
    private val api: SuwayomiApi,
    private val settings: SettingsRepository,
    private val json: Json,
) {

    val source: MangaSource = MangaSource.SUWAYOMI

    private suspend fun server(): ServerConnection {
        val server = settings.snapshot.value.suwayomi
        check(server.isConfigured) { "Suwayomi server is not configured" }
        return server
    }

    private suspend fun <T> request(
        query: String,
        variables: JsonObject? = null,
        decode: (JsonObject) -> T,
    ): T {
        val response = api.graphql(
            url = "${server().url}/api/graphql",
            request = GraphQLRequest(query.trimIndent(), variables),
        )
        return decode(response.requireData())
    }

    /**
     * Sources installed on the server, for the picker in Settings. Sorted by
     * display name so the list is stable between refreshes.
     */
    suspend fun availableSources(): List<SuwayomiSourceOption> {
        val data = request(SuwayomiQueries.SOURCES) { json.decodeData<SourcesData>(it) }
        val options = data.sources.nodes.map { node ->
            SuwayomiSourceOption(
                id = node.id,
                name = node.displayName?.takeIf { it.isNotBlank() }
                    ?: node.name?.takeIf { it.isNotBlank() }
                    ?: node.id,
                lang = node.lang.orEmpty(),
            )
        }
        check(options.isNotEmpty()) { "Suwayomi server has no sources installed" }
        return options.sortedBy { it.name.lowercase() }
    }

    /**
     * Id of the Suwayomi source used as catalog. Cached per preference
     * value: the user's pick wins, then the first English source, then the
     * first listed one. A missing pick (source removed on the server)
     * silently falls back, so the catalog never dead-ends.
     */
    private suspend fun preferredSourceId(): String {
        val preference = settings.snapshot.value.suwayomiSourceId
        preferredSourceIdCache?.takeIf { preferredSourceIdCacheFor == preference }?.let { return it }
        val data = request(SuwayomiQueries.SOURCES) { json.decodeData<SourcesData>(it) }
        val nodes = data.sources.nodes
        val chosen = nodes.firstOrNull { it.id == preference }
            ?: nodes.firstOrNull { it.lang.equals("en", ignoreCase = true) }
            ?: nodes.firstOrNull()
            ?: error("Suwayomi server has no sources installed")
        preferredSourceIdCache = chosen.id
        preferredSourceIdCacheFor = preference
        return chosen.id
    }

    @Volatile
    private var preferredSourceIdCache: String? = null

    @Volatile
    private var preferredSourceIdCacheFor: String? = null

    suspend fun popular(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        fetch(offset, type = "POPULAR")

    suspend fun latest(offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        fetch(offset, type = "LATEST")

    suspend fun search(query: String, offset: Int, limit: Int = PAGE_SIZE): Paged<Manga> =
        fetch(offset, type = "SEARCH", searchQuery = query)

    suspend fun details(mangaId: String): Manga = mangaNode(mangaId).toDomain()

    suspend fun chapters(mangaId: String): List<MangaChapter> =
        mangaNode(mangaId).chapters.nodes
            .map { it.toDomain() }
            .sortedWith(compareBy { it.number?.toDoubleOrNull() ?: Double.MAX_VALUE })

    /**
     * Page URLs. The server serves them through the legacy REST route
     * GET /api/v1/manga/{mangaId}/chapter/{sourceOrder}/page/{index};
     * pages are 0-indexed, the total count comes from the chapter node.
     */
    suspend fun pages(chapterId: String): List<MangaPage> {
        val (mangaDbId, sourceOrder) = parseChapterId(chapterId)
            ?: error("Malformed Suwayomi chapter id: $chapterId")
        val server = server()
        val manga = mangaNode(mangaId(mangaDbId))
        val chapter = manga.chapters.nodes.firstOrNull { it.sourceOrder == sourceOrder }
            ?: error("Chapter not found on server: sourceOrder $sourceOrder")
        check(chapter.pageCount > 0) { "Chapter has no readable pages" }
        return (0 until chapter.pageCount).map { index ->
            MangaPage(
                url = "${server.url}/api/v1/manga/$mangaDbId/chapter/$sourceOrder/page/$index",
                index = index,
            )
        }
    }

    private suspend fun mangaNode(mangaId: String): MangaDetailsNode {
        val dbId = mangaId.removePrefix(MANGA_ID_PREFIX).toLongOrNull()
            ?: error("Malformed Suwayomi manga id: $mangaId")
        return request(
            SuwayomiQueries.MANGA_DETAILS,
            variables = buildJsonObject { put("id", dbId) },
        ) { json.decodeData<MangaDetailsData>(it) }.manga ?: error("Manga not found: $dbId")
    }

    private suspend fun fetch(
        offset: Int,
        type: String,
        searchQuery: String? = null,
    ): Paged<Manga> {
        val server = server()
        val page = offset / PAGE_SIZE
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("source", preferredSourceId())
                put("page", page)
                put("type", type)
                if (!searchQuery.isNullOrBlank()) put("query", searchQuery)
            }
        }
        val data = request(SuwayomiQueries.FETCH_MANGA, variables) {
            json.decodeData<FetchMangaData>(it)
        }
        val payload = data.fetchSourceManga
        return Paged(
            items = payload.mangas.map { node ->
                Manga(
                    id = MANGA_ID_PREFIX + node.id,
                    source = source,
                    title = node.title.ifBlank { "Untitled" },
                    coverUrl = node.thumbnailUrl ?: "${server.url}/api/v1/manga/${node.id}/thumbnail",
                    state = MangaState.UNKNOWN,
                )
            },
            offset = page * PAGE_SIZE,
            total = -1,
            hasNext = payload.hasNextPage,
        )
    }

    private fun MangaDetailsNode.toDomain(): Manga = Manga(
        id = MANGA_ID_PREFIX + id,
        source = source,
        title = title.ifBlank { "Untitled" },
        coverUrl = thumbnailUrl,
        description = description?.takeIf { it.isNotBlank() },
        tags = genre.map { MangaTag(id = it, title = it) },
        state = mapState(status),
        author = author,
        chaptersCount = chapters.totalCount,
    )

    private fun org.kinera.app.data.suwayomi.dto.ChapterNode.toDomain(): MangaChapter = MangaChapter(
        id = chapterId(mangaId, sourceOrder),
        source = source,
        number = formatNumber(chapterNumber),
        title = name.takeIf { it.isNotBlank() },
        volume = null,
        scanlator = scanlator,
        uploadedAt = uploadDate ?: 0L,
        pagesCount = pageCount,
    )

    private fun mapState(status: String?): MangaState = when (status) {
        "ONGOING" -> MangaState.ONGOING
        "COMPLETED" -> MangaState.FINISHED
        "ON_HIATUS" -> MangaState.PAUSED
        "CANCELLED" -> MangaState.CANCELLED
        else -> MangaState.UNKNOWN
    }

    private fun formatNumber(value: Double): String? {
        if (value <= 0.0) return null
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    companion object {
        const val PAGE_SIZE = 24
        const val MANGA_ID_PREFIX = "swyd:"
        const val CHAPTER_ID_PREFIX = "swydch:"

        fun mangaId(dbId: Long) = "$MANGA_ID_PREFIX$dbId"

        fun chapterId(mangaDbId: Long, sourceOrder: Int) =
            "$CHAPTER_ID_PREFIX$mangaDbId/$sourceOrder"

        /** Splits "swydch:12/3" into (12, 3). */
        fun parseChapterId(chapterId: String): Pair<Long, Int>? {
            val rest = chapterId.removePrefix(CHAPTER_ID_PREFIX)
            val slash = rest.indexOf('/')
            if (slash <= 0 || slash == rest.length - 1) return null
            val mangaDbId = rest.substring(0, slash).toLongOrNull() ?: return null
            val sourceOrder = rest.substring(slash + 1).toIntOrNull() ?: return null
            return mangaDbId to sourceOrder
        }
    }
}

private inline fun <reified T> Json.decodeData(data: JsonObject): T =
    decodeFromJsonElement(serializer<T>(), data)
