package org.kinera.app.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ReaderMode { WEBTOON, PAGED }

data class KineraSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = true,
    val readerMode: ReaderMode = ReaderMode.WEBTOON,
    val keepScreenOn: Boolean = true,
    val webtoonZoom: Boolean = true,
)
