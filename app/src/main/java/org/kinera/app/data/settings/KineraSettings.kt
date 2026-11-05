package org.kinera.app.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ReaderMode { WEBTOON, PAGED }

/** Connection details of one self-hosted server (Komga / Suwayomi). */
data class ServerConnection(
    val url: String = "",
    val user: String = "",
    val password: String = "",
) {
    val isConfigured: Boolean
        get() = url.isNotBlank()

    /** Host[:port] of the server, lowercase; empty when unconfigured. */
    val host: String
        get() = runCatching {
            java.net.URI(normalizeUrl(url)).host?.lowercase().orEmpty()
        }.getOrNull().orEmpty()

    /** Explicit port from the stored URL; scheme default when none was given. */
    val port: Int
        get() = runCatching {
            val uri = java.net.URI(normalizeUrl(url))
            uri.port.takeIf { it != -1 } ?: when (uri.scheme) {
                "https" -> 443
                else -> 80
            }
        }.getOrNull() ?: -1

    companion object {
        fun normalizeUrl(raw: String): String {
            var url = raw.trim()
            if (url.isBlank()) return ""
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            return url.trimEnd('/')
        }
    }
}

data class KineraSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = false,
    val readerMode: ReaderMode = ReaderMode.WEBTOON,
    val keepScreenOn: Boolean = true,
    val webtoonZoom: Boolean = true,
    /** Right-to-left page order for the paged reader (manga style). */
    val readerRtl: Boolean = false,
    /** MangaDex languages with available chapters, e.g. ["en", "ru"]. Empty = all languages. */
    val mangadexLanguages: Set<String> = setOf("en", "ru"),
    /** MangaLib status ids kept in the catalog, e.g. [1, 2]. Empty = all statuses. */
    val mangalibStatuses: Set<Int> = emptySet(),
    /** MangaLib genre ids kept in the catalog. Empty = all genres. */
    val mangalibGenres: Set<Int> = emptySet(),
    /** Preferred source id inside the Suwayomi server; empty = automatic pick. */
    val suwayomiSourceId: String = "",
    /** Periodic background refresh of the library (new chapter badges). */
    val libraryUpdateEnabled: Boolean = true,
    /** Interval of the background refresh in hours. */
    val libraryUpdateIntervalHours: Int = 12,
    val komga: ServerConnection = ServerConnection(),
    val suwayomi: ServerConnection = ServerConnection(),
)
