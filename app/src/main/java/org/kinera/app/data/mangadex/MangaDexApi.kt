package org.kinera.app.data.mangadex

import org.kinera.app.data.mangadex.dto.AtHomeResponse
import org.kinera.app.data.mangadex.dto.MangaDexChapter
import org.kinera.app.data.mangadex.dto.MangaDexCollectionResponse
import org.kinera.app.data.mangadex.dto.MangaDexEntityResponse
import org.kinera.app.data.mangadex.dto.MangaDexManga
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MangaDexApi {

    @GET("manga")
    suspend fun listManga(
        @Query("title") title: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive"),
        @Query("hasAvailableChapters") hasAvailableChapters: String = "true",
        @QueryMap order: Map<String, String> = emptyMap(),
    ): MangaDexCollectionResponse<MangaDexManga>

    @GET("manga/{id}")
    suspend fun manga(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
    ): MangaDexEntityResponse<MangaDexManga>

    @GET("manga/{id}/feed")
    suspend fun feed(
        @Path("id") id: String,
        @Query("limit") limit: Int = 500,
        @Query("offset") offset: Int = 0,
        @Query("includes[]") includes: List<String> = listOf("scanlation_group"),
        @Query("contentRating[]") contentRating: List<String> = listOf("safe", "suggestive"),
        @Query("translatedLanguage[]") languages: List<String> = listOf("en", "ru"),
        @QueryMap order: Map<String, String> = mapOf("chapter" to "asc"),
    ): MangaDexCollectionResponse<MangaDexChapter>

    @GET("at-home/server/{chapterId}")
    suspend fun pages(@Path("chapterId") chapterId: String): AtHomeResponse
}
