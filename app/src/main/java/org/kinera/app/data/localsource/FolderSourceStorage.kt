package org.kinera.app.data.localsource

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaChapter
import org.kinera.app.core.model.MangaSource
import java.io.File

/**
 * "Folder of loose images" local source. The user picks a directory through
 * SAF, we persist the read permission and treat the folder as one manga:
 * images in the folder root form the first chapter and every subfolder with
 * images becomes its own chapter. Nothing is copied - pages are streamed
 * straight from the picked location via content URIs, so the folder must not
 * move or be deleted while the permission is granted.
 */
class FolderSourceStorage(private val context: Context) {

    @kotlinx.serialization.Serializable
    data class FolderEntry(
        val key: String,
        val name: String,
        val treeUri: String,
        val addedAt: Long,
    )

    sealed interface ImportResult {
        data object Added : ImportResult
        data object Exists : ImportResult
        data object Failed : ImportResult
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val refresh = MutableStateFlow(0)

    private val registryFile: File
        get() = File(context.filesDir, "folder_sources.json")

    private val coverDir: File
        get() = File(context.filesDir, "local_covers").also { it.mkdirs() }

    /** Folder library as a stream of [Manga] cards, rescanned after every change. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val library: Flow<List<Manga>> = refresh.flatMapLatest {
        flow { emit(withContext(Dispatchers.IO) { scanLibrary() }) }
    }.flowOn(Dispatchers.IO)

    fun entries(): List<FolderEntry> = runCatching {
        val file = registryFile
        if (!file.exists()) return emptyList()
        json.decodeFromString(ListSerializer(FolderEntry.serializer()), file.readText())
    }.getOrNull().orEmpty()

    fun entry(key: String): FolderEntry? = entries().firstOrNull { it.key == key }

    suspend fun add(treeUri: Uri): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val root = DocumentFile.fromTreeUri(context, treeUri) ?: error("Cannot open folder")
            val name = root.name?.trim().orEmpty().ifEmpty { "Folder" }
            val key = makeKey(name, treeUri)
            val current = entries()
            if (current.any { it.key == key }) return@runCatching ImportResult.Exists
            val entry = FolderEntry(key, name, treeUri.toString(), System.currentTimeMillis())
            if (buildManga(entry) == null) error("No readable pages in $name")
            saveEntries(current + entry)
            refresh.value += 1
            ImportResult.Added
        }.getOrDefault(ImportResult.Failed)
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        saveEntries(entries().filterNot { it.key == key })
        coverFile(key).delete()
        refresh.value += 1
        Unit
    }

    fun buildManga(entry: FolderEntry): Manga? = runCatching {
        val chapters = chapterDirs(entry)
        if (chapters.isEmpty()) return null
        Manga(
            id = mangaId(entry.key),
            source = MangaSource.LOCAL,
            title = entry.name,
            coverUrl = coverUrl(entry, chapters),
            description = null,
            tags = emptyList(),
            chaptersCount = chapters.size,
        )
    }.getOrNull()

    /** Chapters of one folder: root images first (if any), then subfolders by name. */
    fun chapters(entry: FolderEntry): List<MangaChapter> = runCatching {
        chapterDirs(entry).map { dir ->
            MangaChapter(
                id = chapterId(entry.key, dir.key),
                source = MangaSource.LOCAL,
                number = null,
                title = dir.name,
                volume = null,
                scanlator = null,
                uploadedAt = dir.uploadedAt,
                pagesCount = dir.pageCount,
            )
        }
    }.getOrNull().orEmpty()

    /** Page URIs (content://) of one chapter in reading order. */
    fun pages(entry: FolderEntry, chapterKey: String): List<String> = runCatching {
        val dir = chapterDocument(entry, chapterKey) ?: error("Chapter not found: $chapterKey")
        images(dir).map { it.uri.toString() }
    }.getOrNull().orEmpty()

    private class ChapterDir(
        val key: String,
        val name: String,
        val document: DocumentFile?,
        val pageCount: Int,
        val uploadedAt: Long,
    )

    private fun chapterDirs(entry: FolderEntry): List<ChapterDir> {
        val root = rootDocument(entry) ?: return emptyList()
        val result = mutableListOf<ChapterDir>()
        val rootImages = images(root)
        if (rootImages.isNotEmpty()) {
            result += ChapterDir(
                key = ROOT_CHAPTER_KEY,
                name = entry.name,
                document = root,
                pageCount = rootImages.size,
                uploadedAt = root.lastModified().takeIf { it > 0 } ?: entry.addedAt,
            )
        }
        root.listFiles()
            .filter { it.isDirectory }
            .sortedWith { a, b -> LocalSourceStorage.naturalCompare(a.name.orEmpty(), b.name.orEmpty()) }
            .forEach { dir ->
                val count = images(dir).size
                if (count > 0) {
                    result += ChapterDir(
                        key = dir.name.orEmpty().ifEmpty { dir.uri.toString() },
                        name = dir.name.orEmpty(),
                        document = dir,
                        pageCount = count,
                        uploadedAt = dir.lastModified().takeIf { it > 0 } ?: entry.addedAt,
                    )
                }
            }
        return result
    }

    private fun chapterDocument(entry: FolderEntry, chapterKey: String): DocumentFile? {
        val root = rootDocument(entry) ?: return null
        if (chapterKey == ROOT_CHAPTER_KEY) {
            return root.takeIf { images(it).isNotEmpty() }
        }
        return root.listFiles()
            .firstOrNull { it.isDirectory && it.name == chapterKey }
            ?.takeIf { images(it).isNotEmpty() }
    }

    private fun images(dir: DocumentFile): List<DocumentFile> =
        dir.listFiles()
            .filter { it.isFile && it.name?.let(::isImageName) == true }
            .sortedWith { a, b ->
                LocalSourceStorage.naturalCompare(a.name.orEmpty(), b.name.orEmpty())
            }

    private fun rootDocument(entry: FolderEntry): DocumentFile? = runCatching {
        DocumentFile.fromTreeUri(context, Uri.parse(entry.treeUri))
            ?.takeIf { it.exists() }
    }.getOrNull()

    private fun coverUrl(entry: FolderEntry, chapters: List<ChapterDir>): String? {
        val target = coverFile(entry.key)
        if (target.exists() && target.length() > 0L) return "file://${target.absolutePath}"
        val first = chapters.firstOrNull()?.document ?: return null
        val firstImage = images(first).firstOrNull() ?: return null
        return runCatching {
            context.contentResolver.openInputStream(firstImage.uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            "file://${target.absolutePath}"
        }.onFailure { target.delete() }.getOrNull()
    }

    private fun scanLibrary(): List<Manga> =
        entries()
            .mapNotNull { entry ->
                runCatching { buildManga(entry) }.getOrNull()
            }
            .sortedBy { it.title.lowercase() }

    private fun saveEntries(list: List<FolderEntry>) {
        runCatching {
            registryFile.writeText(
                json.encodeToString(ListSerializer(FolderEntry.serializer()), list),
            )
        }
    }

    private fun coverFile(key: String): File = File(coverDir, "$key.jpg")

    companion object {
        const val MANGA_ID_PREFIX = "lfold:"
        const val CHAPTER_ID_PREFIX = "lfoldch:"
        const val ROOT_CHAPTER_KEY = "_"

        fun mangaId(key: String) = "$MANGA_ID_PREFIX$key"
        fun chapterId(key: String, chapterKey: String) = "$CHAPTER_ID_PREFIX$key/$chapterKey"

        /** Splits "lfoldch:key/chapterKey" back into (key, chapterKey). */
        fun parseChapterId(chapterId: String): Pair<String, String>? {
            val rest = chapterId.removePrefix(CHAPTER_ID_PREFIX)
            val slash = rest.indexOf('/')
            if (slash <= 0 || slash == rest.length - 1) return null
            return rest.substring(0, slash) to rest.substring(slash + 1)
        }

        private fun isImageName(name: String): Boolean {
            if (name.startsWith(".")) return false
            return name.substringAfterLast('.', "").lowercase() in LocalSourceStorage.IMAGE_EXTENSIONS
        }

        private fun makeKey(name: String, uri: Uri): String {
            val safe = name.map { c -> if (c.isLetterOrDigit()) c else '_' }
                .joinToString("")
                .trim('_')
                .ifEmpty { "folder" }
            val hash = (uri.toString().hashCode().toLong() and 0xFFFFFFFFL).toString(16)
            return "${safe.take(40)}-$hash"
        }
    }
}
