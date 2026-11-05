package org.kinera.app.data.desume

import kotlinx.serialization.Serializable
import org.kinera.app.data.desume.dto.DesuMeManga
import org.kinera.app.data.desume.dto.DesuMeMangaShort
import org.kinera.app.data.desume.dto.DesuMePages
import retrofit2.http.GET
import retrofit2.http.Url

@Serializable
data class DesuMeList<T>(
    val response: List<T> = emptyList(),
)

@Serializable
data class DesuMeOne<T>(
    val response: T,
)

/**
 * Public JSON API of Desu (https://desu.city), a Russian anime & manga
 * portal with a VK-shaped response envelope: every payload is wrapped in
 * {"response": ...}. Endpoints follow the reference DesuMe parser of the
 * Kotatsu project.
 *
 * The portal migrates between mirrors, so all calls take full [Url]s and
 * the source resolves the working host at runtime (see DesuMeSource).
 */
interface DesuMeApi {

    /** Catalog and search share one endpoint; [url] carries all parameters. */
    @GET
    suspend fun catalog(@Url url: String): DesuMeList<DesuMeMangaShort>

    @GET
    suspend fun manga(@Url url: String): DesuMeOne<DesuMeManga>

    @GET
    suspend fun pages(@Url url: String): DesuMeOne<DesuMePages>
}
