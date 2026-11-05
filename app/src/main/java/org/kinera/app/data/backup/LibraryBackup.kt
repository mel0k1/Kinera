package org.kinera.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaSource
import org.kinera.app.data.local.LibraryDatabase
import org.kinera.app.data.local.entity.BookmarkEntity
import org.kinera.app.data.local.entity.CategoryEntity
import org.kinera.app.data.local.entity.FavouriteEntity
import org.kinera.app.data.local.entity.HistoryEntity

/**
 * Portable snapshot of the library: favourites with their categories,
 * reading history and page bookmarks. The file is a pretty-printed JSON
 * document the user exchanges through the system file picker, so it works
 * across reinstalls and devices.
 *
 * Categories are referenced by name, not by row id: ids are local to a
 * device, while names survive a restore and are recreated on demand.
 */
@Serializable
data class LibraryBackup(
    val version: Int = BACKUP_VERSION,
    val createdAt: Long,
    val app: String = APP_MARKER,
    val categories: List<BackupCategory> = emptyList(),
    val favourites: List<BackupFavourite> = emptyList(),
    val history: List<BackupHistory> = emptyList(),
    val bookmarks: List<BackupBookmark> = emptyList(),
) {
    companion object {
        const val BACKUP_VERSION = 1
        const val APP_MARKER = "Kinera"
    }
}

@Serializable
data class BackupCategory(
    val name: String,
    val position: Int = 0,
    val createdAt: Long = 0,
)

@Serializable
data class BackupFavourite(
    val mangaId: String,
    val sourceId: String,
    val title: String,
    val altTitle: String? = null,
    val coverUrl: String? = null,
    /** Name of the assigned category; empty / absent means none. */
    val category: String? = null,
    val createdAt: Long = 0,
    /** Full serialized Manga payload so covers and tags survive a restore. */
    val mangaJson: String? = null,
    val totalChapters: Int = 0,
    val lastChapterId: String? = null,
    val newChapters: Int = 0,
    val checkedAt: Long = 0,
)

@Serializable
data class BackupHistory(
    val mangaId: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String? = null,
    val chapterId: String,
    val chapterLabel: String,
    val page: Int = 0,
    val total: Int = 0,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupBookmark(
    val mangaId: String,
    val chapterId: String,
    val page: Int,
    val sourceId: String,
    val title: String,
    val coverUrl: String? = null,
    val chapterLabel: String,
    val pageUrl: String? = null,
    val createdAt: Long = 0,
)

/** Counters reported to the UI after a successful restore. */
data class RestoreSummary(
    val categories: Int,
    val favourites: Int,
    val history: Int,
    val bookmarks: Int,
)

class BackupManager(
    private val context: Context,
    private val database: LibraryDatabase,
    private val json: Json,
) {

    /** Writes the whole library into the document picked by the user. */
    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val categories = database.categoriesDao().getAll()
            val categoryNameById = categories.associate { it.id to it.name }
            val backup = LibraryBackup(
                createdAt = System.currentTimeMillis(),
                categories = categories.map {
                    BackupCategory(name = it.name, position = it.position, createdAt = it.createdAt)
                },
                favourites = database.favouritesDao().getAll().map {
                    it.toBackup(categoryNameById)
                },
                history = database.historyDao().getAll().map { it.toBackup() },
                bookmarks = database.bookmarksDao().getAll().map { it.toBackup() },
            )
            val payload = json.encodeToString(LibraryBackup.serializer(), backup)
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            } ?: error("Cannot open the destination document")
            backup.favourites.size
        }
    }

    /**
     * Merges the backup into the current library inside one transaction:
     * categories are matched by name (missing ones are created), every
     * favourite / history entry / bookmark is replaced with the stored
     * version. Existing data that is not present in the backup stays.
     */
    suspend fun import(uri: Uri): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: error("Cannot open the selected file")
            val backup = json.decodeFromString(LibraryBackup.serializer(), payload)
            require(backup.app == LibraryBackup.APP_MARKER) { "Not a Kinera backup" }

            database.withTransaction {
                val categoriesDao = database.categoriesDao()
                // Name → row id, over the union of stored and imported categories.
                val idByName = categoriesDao.getAll().associate { it.name to it.id }.toMutableMap()
                backup.categories
                    .sortedBy { it.position }
                    .filter { it.name.isNotBlank() && it.name !in idByName }
                    .forEach { category ->
                        val id = categoriesDao.insert(
                            CategoryEntity(
                                name = category.name,
                                createdAt = if (category.createdAt > 0) {
                                    category.createdAt
                                } else {
                                    System.currentTimeMillis()
                                },
                                position = category.position,
                            ),
                        )
                        idByName[category.name] = id
                    }

                backup.favourites.forEach { entry ->
                    database.favouritesDao().insert(entry.toEntity(idByName))
                }
                backup.history.forEach { entry ->
                    database.historyDao().upsert(entry.toEntity())
                }
                backup.bookmarks.forEach { entry ->
                    database.bookmarksDao().insert(entry.toEntity())
                }
            }
            RestoreSummary(
                categories = backup.categories.size,
                favourites = backup.favourites.size,
                history = backup.history.size,
                bookmarks = backup.bookmarks.size,
            )
        }
    }

    /* ----- mapping ----- */

    private fun FavouriteEntity.toBackup(categoryNameById: Map<Long, String>) = BackupFavourite(
        mangaId = mangaId,
        sourceId = sourceId,
        title = title,
        altTitle = altTitle,
        coverUrl = coverUrl,
        category = categoryId?.let(categoryNameById::get),
        createdAt = createdAt,
        mangaJson = mangaJson,
        totalChapters = totalChapters,
        lastChapterId = lastChapterId,
        newChapters = newChapters,
        checkedAt = checkedAt,
    )

    private fun BackupFavourite.toEntity(idByName: Map<String, Long>) = FavouriteEntity(
        mangaId = mangaId,
        sourceId = sourceId,
        title = title,
        altTitle = altTitle,
        coverUrl = coverUrl,
        categoryId = category?.let { idByName[it] },
        createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
        mangaJson = mangaJson ?: fallbackMangaJson(),
        totalChapters = totalChapters,
        lastChapterId = lastChapterId,
        newChapters = newChapters,
        checkedAt = checkedAt,
    )

    /** Minimal Manga payload for backups taken before mangaJson existed. */
    private fun BackupFavourite.fallbackMangaJson(): String {
        val fallback = Manga(
            id = mangaId,
            source = MangaSource.fromId(sourceId),
            title = title,
            altTitle = altTitle,
            coverUrl = coverUrl,
        )
        return json.encodeToString(Manga.serializer(), fallback)
    }

    private fun HistoryEntity.toBackup() = BackupHistory(
        mangaId = mangaId,
        sourceId = sourceId,
        title = title,
        coverUrl = coverUrl,
        chapterId = chapterId,
        chapterLabel = chapterLabel,
        page = page,
        total = total,
        updatedAt = updatedAt,
    )

    private fun BackupHistory.toEntity() = HistoryEntity(
        mangaId = mangaId,
        sourceId = sourceId,
        title = title,
        coverUrl = coverUrl,
        chapterId = chapterId,
        chapterLabel = chapterLabel,
        page = page,
        total = total,
        updatedAt = updatedAt,
    )

    private fun BookmarkEntity.toBackup() = BackupBookmark(
        mangaId = mangaId,
        chapterId = chapterId,
        page = page,
        sourceId = sourceId,
        title = title,
        coverUrl = coverUrl,
        chapterLabel = chapterLabel,
        pageUrl = pageUrl,
        createdAt = createdAt,
    )

    private fun BackupBookmark.toEntity() = BookmarkEntity(
        mangaId = mangaId,
        chapterId = chapterId,
        page = page,
        sourceId = sourceId,
        title = title,
        coverUrl = coverUrl,
        chapterLabel = chapterLabel,
        pageUrl = pageUrl,
        createdAt = createdAt,
    )
}
