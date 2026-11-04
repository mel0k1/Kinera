package org.kinera.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
)

class LibraryRepository(
    private val favouritesDao: FavouritesDao,
    private val categoriesDao: CategoriesDao,
    private val historyDao: HistoryDao,
    private val bookmarksDao: BookmarksDao,
    private val json: Json,
) {

    val favourites: Flow<List<FavouriteManga>> =
        favouritesDao.observeAll().mapList { entity ->
            val manga = runCatching { json.decodeFromString<Manga>(entity.mangaJson) }.getOrNull()
                ?: Manga(id = entity.mangaId, source = MangaSource.fromId(entity.sourceId), title = entity.title)
            FavouriteManga(manga, entity.categoryId, entity.createdAt)
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
        categoriesDao.insert(CategoryEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun deleteCategory(id: Long) = categoriesDao.delete(id)

    suspend fun assignCategory(mangaId: String, categoryId: Long?) {
        val existing = favouritesDao.get(mangaId) ?: return
        favouritesDao.insert(existing.copy(categoryId = categoryId))
    }

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

    private inline fun <T, R> Flow<List<T>>.mapList(crossinline transform: (T) -> R): Flow<List<R>> =
        map { list -> list.map(transform) }
}
