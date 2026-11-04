package org.kinera.app.core.model

data class MangaChapter(
    val id: String,
    val source: MangaSource,
    val number: String?,
    val title: String?,
    val volume: String?,
    val scanlator: String? = null,
    val uploadedAt: Long = 0L,
    val pagesCount: Int = 0,
) {
    val label: String
        get() = when {
            !title.isNullOrBlank() && !number.isNullOrBlank() -> "Ch. $number · $title"
            !title.isNullOrBlank() -> title
            !number.isNullOrBlank() -> "Ch. $number"
            else -> "—"
        }
}
