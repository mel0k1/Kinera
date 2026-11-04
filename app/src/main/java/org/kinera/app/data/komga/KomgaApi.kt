package org.kinera.app.data.komga

import org.kinera.app.data.komga.dto.KomgaBook
import org.kinera.app.data.komga.dto.KomgaPageDto
import org.kinera.app.data.komga.dto.KomgaSeries
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Komga REST API. The base URL is user-configured, so every endpoint takes
 * the full [Url]; Retrofit only needs a placeholder baseUrl at creation.
 */
interface KomgaApi {

    @GET
    suspend fun series(
        @Url url: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
    ): KomgaPageDto<KomgaSeries>

    @GET
    suspend fun seriesById(@Url url: String): KomgaSeries

    @GET
    suspend fun books(
        @Url url: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("media_status") mediaStatus: List<String> = listOf("READY"),
        @Query("sort") sort: String? = "numberSort,asc",
    ): KomgaPageDto<KomgaBook>

    @GET
    suspend fun bookById(@Url url: String): KomgaBook
}
