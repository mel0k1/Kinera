package org.kinera.app.core.model

data class Manga(
    val id: String,
    val source: MangaSource,
    val title: String,
    val altTitle: String? = null,
    val coverUrl: String? = null,
    val description: String? = null,
    val tags: List<MangaTag> = emptyList(),
    val state: MangaState = MangaState.UNKNOWN,
    val author: String? = null,
    val chaptersCount: Int = 0,
)
