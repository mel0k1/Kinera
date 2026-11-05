package org.kinera.app.data.repository

import org.kinera.app.core.model.MangaSource

/** Outcome of one library update pass. */
data class LibraryUpdateSummary(
    val checked: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    /** Titles whose chapter count grew since the previous update. */
    val withNewChapters: Int = 0,
)

/**
 * Walks the library and refreshes chapter information for every favourite.
 * The first pass after adding a title only records a baseline — nothing is
 * flagged as new, otherwise every freshly added manga would light up.
 *
 * Local sources never change remotely and self-hosted ones can be offline,
 * so their failures are counted but never abort the run.
 */
class LibraryUpdater(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
) {

    suspend fun updateAll(): LibraryUpdateSummary {
        val favourites = library.favouritesSnapshot()
        var updated = 0
        var failed = 0
        var withNew = 0
        for (favourite in favourites) {
            if (favourite.manga.source == MangaSource.LOCAL) continue
            val chapters = runCatching { catalog.chapters(favourite.manga.id) }
                .onFailure { failed++ }
                .getOrNull()
            if (chapters == null) continue
            val newest = chapters.lastOrNull()
            val now = System.currentTimeMillis()
            val newCount = when {
                // Baseline: first update after adding to the library.
                favourite.lastChapterId == null && favourite.totalChapters == 0 -> 0
                // A dropped translation branch can shrink the list.
                chapters.size < favourite.totalChapters -> favourite.newChapters
                // Growth only counts when the newest chapter actually changed.
                chapters.size > favourite.totalChapters && newest?.id != favourite.lastChapterId ->
                    chapters.size - favourite.totalChapters
                else -> favourite.newChapters
            }
            library.updateChapterInfo(
                mangaId = favourite.manga.id,
                total = chapters.size,
                lastChapterId = newest?.id,
                newChapters = newCount,
                checkedAt = now,
            )
            if (newCount > favourite.newChapters) withNew++
            updated++
        }
        return LibraryUpdateSummary(
            checked = favourites.size,
            updated = updated,
            failed = failed,
            withNewChapters = withNew,
        )
    }
}
