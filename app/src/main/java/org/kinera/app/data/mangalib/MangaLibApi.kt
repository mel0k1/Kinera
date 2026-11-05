package org.kinera.app.data.mangalib

import org.kinera.app.data.mangalib.dto.MangaLibBranchRef
import org.kinera.app.data.mangalib.dto.MangaLibChapter
import org.kinera.app.data.mangalib.dto.MangaLibChapterPages
import org.kinera.app.data.mangalib.dto.MangaLibConstants
import org.kinera.app.data.mangalib.dto.MangaLibData
import org.kinera.app.data.mangalib.dto.MangaLibListResponse
import org.kinera.app.data.mangalib.dto.MangaLibManga
import org.kinera.app.data.mangalib.dto.MangaLibMangaShort
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Public JSON API of MangaLib (https://mangalib.me) — the largest Russian
 * manga catalog. A header "Site-Id: 1" is attached by an interceptor wired
 * in the DI graph. Endpoints verified against the live API.
 */
interface MangaLibApi {

    /** Catalog and search share one endpoint; [query] null lists by rating. */
    @GET("api/manga")
    suspend fun catalog(
        @Query("site_id[]") siteId: List<String> = listOf("1"),
        @Query("page") page: Int = 1,
        @Query("q") query: String? = null,
    ): MangaLibListResponse<MangaLibMangaShort>

    @GET("api/latest-updates")
    suspend fun latest(
        @Query("page") page: Int = 1,
    ): MangaLibListResponse<MangaLibMangaShort>

    @GET("api/manga/{slugUrl}")
    suspend fun manga(
        @Path("slugUrl") slugUrl: String,
        @Query("fields[]") fields: List<String> = DETAILS_FIELDS,
    ): MangaLibData<MangaLibManga>

    @GET("api/manga/{slugUrl}/chapters")
    suspend fun chapters(
        @Path("slugUrl") slugUrl: String,
    ): MangaLibData<List<MangaLibChapter>>

    @GET("api/manga/{slugUrl}/chapter")
    suspend fun pages(
        @Path("slugUrl") slugUrl: String,
        @Query("volume") volume: String,
        @Query("number") number: String,
        @Query("branch_id") branchId: Long? = null,
    ): MangaLibData<MangaLibChapterPages>

    /** Default branch of a multi-branch title, used to pick translations. */
    @GET("api/branches/{mangaId}")
    suspend fun branches(
        @Path("mangaId") mangaId: Long,
    ): MangaLibData<List<MangaLibBranchRef>>

    @GET("api/constants")
    suspend fun constants(
        @Query("fields[]") fields: List<String> = listOf("imageServers"),
    ): MangaLibData<MangaLibConstants>

    companion object {
        val DETAILS_FIELDS = listOf(
            "eng_name", "otherNames", "summary", "rate", "genres", "tags",
            "teams", "authors", "publisher", "userRating",
            "manga_status_id", "status_id", "artists",
        )
    }
}
