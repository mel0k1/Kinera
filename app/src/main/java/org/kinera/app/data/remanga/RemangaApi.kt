package org.kinera.app.data.remanga

import kotlinx.serialization.Serializable
import org.kinera.app.data.remanga.dto.RemangaChapter
import org.kinera.app.data.remanga.dto.RemangaChapterPages
import org.kinera.app.data.remanga.dto.RemangaGenre
import org.kinera.app.data.remanga.dto.RemangaManga
import org.kinera.app.data.remanga.dto.RemangaMangaShort
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class RemangaList<T>(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList(),
)

/** Answer of the forms endpoint that exposes the site-wide genre list. */
@Serializable
data class RemangaForms(
    val content: RemangaFormsContent = RemangaFormsContent(),
)

@Serializable
data class RemangaFormsContent(
    val genres: List<RemangaGenre> = emptyList(),
)

/**
 * Public JSON API of Remanga (https://remanga.org), a large Russian manga
 * catalog. Endpoints verified against the live service; covers and page
 * images live under /media/ on the api host unless served by absolute CDN
 * links.
 */
interface RemangaApi {

    /** Browsing without a query: rated catalog with an ordering. */
    @GET("api/v2/search/catalog")
    suspend fun catalog(
        @Query("page") page: Int = 1,
        @Query("count") count: Int = RemangaSource.PAGE_SIZE,
        @Query("ordering") ordering: String,
        @Query("genres") genres: List<String>? = null,
        @Query("status") statuses: List<Int>? = null,
    ): RemangaList<RemangaMangaShort>

    /** Search shares the shape but lives one level up. */
    @GET("api/v2/search")
    suspend fun search(
        @Query("page") page: Int = 1,
        @Query("count") count: Int = RemangaSource.PAGE_SIZE,
        @Query("query") query: String,
        @Query("ordering") ordering: String = ORDER_RATING,
        @Query("genres") genres: List<String>? = null,
        @Query("status") statuses: List<Int>? = null,
    ): RemangaList<RemangaMangaShort>

    /** Site-wide genre list served by the title forms endpoint. */
    @GET("api/forms/titles/")
    suspend fun genreForms(
        @Query("get") get: String = "genres",
    ): RemangaForms

    @GET("api/v2/titles/{dir}/")
    suspend fun manga(
        @Path("dir") dir: String,
    ): RemangaManga

    /** Chapters of one translation branch, newest first, paginated. */
    @GET("api/v2/titles/chapters/")
    suspend fun chapters(
        @Query("branch_id") branchId: Long,
        @Query("page") page: Int = 1,
        @Query("count") count: Int = 500,
    ): RemangaList<RemangaChapter>

    @GET("api/v2/titles/chapters/{id}/")
    suspend fun pages(
        @Path("id") chapterId: Long,
    ): RemangaChapterPages

    companion object {
        const val ORDER_RATING = "-rating"
        const val ORDER_UPDATED = "-chapter_date"
    }
}
