package org.kinera.app.data.suwayomi

import kotlinx.serialization.json.JsonObject
import org.kinera.app.data.suwayomi.dto.GraphQLRequest
import org.kinera.app.data.suwayomi.dto.GraphQLResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Suwayomi GraphQL API (POST /api/graphql). The server URL is
 * user-configured, so every call takes the full [Url]. Responses arrive as
 * raw JSON objects and are decoded by [SuwayomiSource].
 */
interface SuwayomiApi {

    @POST
    suspend fun graphql(
        @Url url: String,
        @Body request: GraphQLRequest,
    ): GraphQLResponse<JsonObject>
}

/** GraphQL documents and input builders used by [SuwayomiSource]. */
object SuwayomiQueries {

    const val SOURCES = """
        query { sources { nodes { id name displayName lang iconUrl } } }
    """

    const val FETCH_MANGA = """
        mutation(${'$'}input: FetchSourceMangaInput!) {
            fetchSourceManga(input: ${'$'}input) {
                hasNextPage
                mangas { id title thumbnailUrl sourceId inLibrary initialized }
            }
        }
    """

    const val MANGA_DETAILS = """
        query(${'$'}id: Int!) {
            manga(id: ${'$'}id) {
                id title thumbnailUrl author artist description status genre realUrl sourceId
                chapters {
                    totalCount
                    nodes {
                        id name mangaId scanlator sourceOrder chapterNumber isRead uploadDate pageCount
                    }
                }
            }
        }
    """
}
