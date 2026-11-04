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
    val komga: ServerConnection = ServerConnection(),
    val suwayomi: ServerConnection = ServerConnection(),
)
