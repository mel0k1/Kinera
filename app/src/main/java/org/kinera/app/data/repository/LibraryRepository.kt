package org.kinera.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaSource
import org.kinera.app.data.local.dao.BookmarksDao
import org.kinera.app.data.local.dao.CategoriesDao
import org.kinera.app.data.local.dao.FavouritesDao
import org.kinera.app.data.local.dao.HistoryDao
import org.kinera.app.data.local.entity.BookmarkEntity
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.local.entity.FavouriteEntity
import org.kinera.app.data.local.entity.HistoryEntity

data class FavouriteManga(
    val manga: Manga,
    val categoryId: Long?,
    val createdAt: Long,
    /** Chapters that appeared since the user last opened the title. */
    val newChapters: Int = 0,
    /** Chapter count at the last library update. */
    val totalChapters: Int = 0,
    /** Id of the newest chapter at the last update; null before the first run. */
    val lastChapterId: String? = null,
)

class LibraryRepository(
    private val favouritesDao: FavouritesDao,
    private val categoriesDao: CategoriesDao,
    private val historyDao: HistoryDao,
    private val bookmarksDao: BookmarksDao,
    private val json: Json,
) {

    private val reorderMutex = Mutex()

    val favourites: Flow<List<FavouriteManga>> =
        favouritesDao.observeAll().mapList { entity ->
            entity.toFavouriteManga()
        }

    val categories: Flow<List<CategoryEntity>> = categoriesDao.observeAll()

    val history: Flow<List<HistoryEntity>> = historyDao.observeAll()

    fun observeFavorite(mangaId: String): Flow<Boolean> = favouritesDao.observeContains(mangaId)

    suspend fun toggleFavorite(manga: Manga): Boolean {
        val existing = favouritesDao.get(manga.id)
        return if (existing == null) {
            favouritesDao.insert(
                FavouriteEntity(
                    mangaId = manga.id,
                    sourceId = manga.source.id,
                    title = manga.title,
                    altTitle = manga.altTitle,
                    coverUrl = manga.coverUrl,
                    categoryId = null,
                    createdAt = System.currentTimeMillis(),
                    mangaJson = json.encodeToString(manga),
                ),
            )
            true
        } else {
            favouritesDao.delete(existing)
            false
        }
    }

    suspend fun addCategory(name: String): Long =
        categoriesDao.insert(
            CategoryEntity(
                name = name.trim(),
                createdAt = System.currentTimeMillis(),
                position = categoriesDao.getAll().maxOfOrNull { it.position + 1 } ?: 0,
            ),
        )

    suspend fun deleteCategory(id: Long) = categoriesDao.delete(id)

    /**
     * Moves one category to another index of the chips row and renumbers
     * every position so the order stays dense after future moves. A mutex
     * serializes rapid swaps fired while the user drags a chip.
     */
    suspend fun moveCategory(categoryId: Long, toIndex: Int) = reorderMutex.withLock {
        val ordered = categoriesDao.getAll()
        val fromIndex = ordered.indexOfFirst { it.id == categoryId }
        if (fromIndex < 0 || toIndex !in ordered.indices || fromIndex == toIndex) return@withLock
        val mutable = ordered.toMutableList()
        val category = mutable.removeAt(fromIndex)
        mutable.add(toIndex, category)
        mutable.forEachIndexed { index, item ->
            if (item.position != index) categoriesDao.updatePosition(item.id, index)
        }
    }

    suspend fun assignCategory(mangaId: String, categoryId: Long?) {
        val existing = favouritesDao.get(mangaId) ?: return
        favouritesDao.insert(existing.copy(categoryId = categoryId))
    }

    /** Snapshot for the library updater; safe on remote sources only. */
    suspend fun favouritesSnapshot(): List<FavouriteManga> =
        favouritesDao.getAll().map { it.toFavouriteManga() }

    /**
     * Stores the result of one library update. A smaller chapter count
     * never resets the badge: sources may drop removed branches, but the
     * user should still see that something changed.
     */
    suspend fun updateChapterInfo(
        mangaId: String,
        total: Int,
        lastChapterId: String?,
        newChapters: Int,
        checkedAt: Long,
    ) {
        val existing = favouritesDao.get(mangaId) ?: return
        val keptNew = if (total < existing.totalChapters && existing.totalChapters > 0) {
            maxOf(existing.newChapters, newChapters)
        } else {
            newChapters
        }
        favouritesDao.updateChapterInfo(
            mangaId = mangaId,
            total = total,
            lastChapterId = lastChapterId,
            newChapters = keptNew,
            checkedAt = checkedAt,
        )
    }

    /** Clears the "new chapters" badge once the user opens the title. */
    suspend fun markOpened(mangaId: String) = favouritesDao.clearNewChapters(mangaId)

    suspend fun saveProgress(
        manga: Manga,
        chapterId: String,
        chapterLabel: String,
        page: Int,
        total: Int,
    ) {
        historyDao.upsert(
            HistoryEntity(
                mangaId = manga.id,
                sourceId = manga.source.id,
                title = manga.title,
                coverUrl = manga.coverUrl,
                chapterId = chapterId,
                chapterLabel = chapterLabel,
                page = page,
                total = total,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getProgress(mangaId: String): HistoryEntity? = historyDao.get(mangaId)

    suspend fun removeHistory(mangaId: String) = historyDao.delete(mangaId)

    suspend fun clearHistory() = historyDao.clear()

    fun bookmarks(mangaId: String): Flow<List<BookmarkEntity>> = bookmarksDao.observeForManga(mangaId)

    fun observeBookmark(mangaId: String, chapterId: String, page: Int): Flow<Boolean> =
        bookmarksDao.observeContains(mangaId, chapterId, page)

    /** Adds a bookmark for the page; toggling an existing one removes it. */
    suspend fun toggleBookmark(
        manga: Manga,
        chapter: MangaChapter,
        page: Int,
        pageUrl: String?,
    ): Boolean {
        val existing = bookmarksDao.get(manga.id, chapter.id, page)
        return if (existing == null) {
            bookmarksDao.insert(
                BookmarkEntity(
                    mangaId = manga.id,
                    chapterId = chapter.id,
                    page = page,
                    sourceId = manga.source.id,
                    title = manga.title,
                    coverUrl = manga.coverUrl,
                    chapterLabel = chapter.label,
                    pageUrl = pageUrl,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            true
        } else {
            bookmarksDao.delete(existing.mangaId, existing.chapterId, existing.page)
            false
        }
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) =
        bookmarksDao.delete(bookmark.mangaId, bookmark.chapterId, bookmark.page)

    private fun FavouriteEntity.toFavouriteManga(): FavouriteManga {
        val manga = runCatching { json.decodeFromString<Manga>(mangaJson) }.getOrNull()
            ?: Manga(id = mangaId, source = MangaSource.fromId(sourceId), title = title)
        return FavouriteManga(
            manga = manga,
            categoryId = categoryId,
            createdAt = createdAt,
            newChapters = newChapters,
            totalChapters = totalChapters,
            lastChapterId = lastChapterId,
        )
    }

    private inline fun <T, R> Flow<List<T>>.mapList(crossinline transform: (T) -> R): Flow<List<R>> =
        map { list -> list.map(transform) }
}
