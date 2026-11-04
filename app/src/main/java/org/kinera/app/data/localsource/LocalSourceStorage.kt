package org.kinera.app.data.localsource

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.kinera.app.core.model.Manga
import org.kinera.app.core.model.MangaSource
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Filesystem storage for imported CBZ archives. One archive is one local manga
 * with a single chapter. Files live in app-private storage, so no runtime
 * permissions are involved: the user picks archives through SAF and we copy
 * them into our own directory. Pages are unpacked into the cache on demand.
 */
class LocalSourceStorage(private val context: Context) {

    private val refresh = MutableStateFlow(0)

    val root: File
        get() = File(context.filesDir, "local_source").also { it.mkdirs() }

    private val coverDir: File
        get() = File(context.filesDir, "local_covers").also { it.mkdirs() }

    /** Local library as a stream of [Manga] cards, rescanned after every import. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val library: Flow<List<Manga>> = refresh.flatMapLatest {
        flow { emit(withContext(Dispatchers.IO) { scanLibrary() }) }
    }.flowOn(Dispatchers.IO)

    suspend fun import(archives: List<Uri>): ImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var skipped = 0
        var failed = 0
        for (uri in archives) {
            runCatching {
                val name = displayName(uri) ?: error("Unreadable file name")
                val base = stripExtension(name)
                val target = File(root, "$base.cbz")
                if (target.exists()) {
                    skipped++
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("Cannot open selected file")
                    imported++
                }
            }.onFailure { failed++ }
        }
        if (imported > 0) refresh.value += 1
        ImportResult(imported, skipped, failed)
    }

    suspend fun delete(base: String) = withContext(Dispatchers.IO) {
        resolveFile(base)?.delete()
        coverFile(base).delete()
        refresh.value += 1
        Unit
    }

    fun resolveFile(base: String): File? =
        root.listFiles { file -> file.isFile && file.nameWithoutExtension == base }
            ?.firstOrNull { it.extension.equals("cbz", ignoreCase = true) || it.extension.equals("zip", ignoreCase = true) }

    /**
     * Unpacks every page of the archive into the cache directory (skipping
     * work when the entries are already there) and returns file:// URLs in
     * reading order.
     */
    suspend fun extractPages(base: String): List<String> = withContext(Dispatchers.IO) {
        val file = resolveFile(base) ?: error("Archive not found: $base")
        val outDir = File(context.cacheDir, "cbz_pages/$base")
        ZipFile(file).use { zip ->
            val entries = imageEntries(zip)
            if (entries.isEmpty()) error("No readable pages in $base.cbz")
            val existing = outDir.listFiles()?.size ?: 0
            if (existing != entries.size) {
                outDir.deleteRecursively()
                outDir.mkdirs()
                entries.forEachIndexed { index, entry ->
                    File(outDir, "%04d.jpg".format(index)).outputStream().use { output ->
                        zip.getInputStream(entry).copyTo(output)
                    }
                }
            }
            outDir.listFiles { f -> f.isFile }
                ?.sortedBy { it.name }
                ?.map { "file://${it.absolutePath}" }
                .orEmpty()
        }
    }

    fun buildManga(file: File): Manga? = runCatching {
        ZipFile(file).use { zip ->
            val entries = imageEntries(zip)
            if (entries.isEmpty()) return null
            Manga(
                id = mangaId(file.nameWithoutExtension),
                source = MangaSource.LOCAL,
                title = prettyTitle(file.nameWithoutExtension),
                coverUrl = coverUrl(file, entries.first()),
                description = null,
                tags = emptyList(),
                chaptersCount = 1,
            )
        }
    }.getOrNull()

    private fun imageEntries(zip: ZipFile): List<ZipEntry> =
        zip.entries().asSequence()
            .filter { isImageEntry(it.name) }
            .sortedWith { a, b -> naturalCompare(a.name, b.name) }
            .toList()

    private fun coverUrl(file: File, firstEntry: ZipEntry): String? {
        val target = coverFile(file.nameWithoutExtension)
        if (!target.exists() || target.length() == 0L) {
            runCatching {
                ZipFile(file).use { zip ->
                    zip.getInputStream(firstEntry).use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }.onFailure { target.delete() } ?: return null
        }
        return if (target.exists()) "file://${target.absolutePath}" else null
    }

    private fun scanLibrary(): List<Manga> =
        root.listFiles { file -> file.isFile && file.extension.equals("cbz", ignoreCase = true) }
            ?.mapNotNull { buildManga(it) }
            ?.sortedBy { it.title.lowercase() }
            ?: emptyList()

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: uri.lastPathSegment

    private fun coverFile(base: String): File = File(coverDir, "$base.jpg")

    companion object {
        const val MANGA_ID_PREFIX = "local:"
        const val CHAPTER_ID_PREFIX = "lch:"

        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

        fun isImageEntry(name: String): Boolean {
            if (name.endsWith("/")) return false
            if (name.startsWith("__MACOSX") || name.contains("/__MACOSX/")) return false
            if (name.substringAfterLast('/').startsWith(".")) return false
            return name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
        }

        fun mangaId(base: String) = "$MANGA_ID_PREFIX$base"
        fun chapterId(base: String) = "$CHAPTER_ID_PREFIX$base"

        fun prettyTitle(base: String): String = base.replace('_', ' ').replace('.', ' ').trim()

        fun naturalCompare(a: String, b: String): Int {
            var i = 0
            var j = 0
            while (i < a.length && j < b.length) {
                val ca = a[i]
                val cb = b[j]
                if (ca.isDigit() && cb.isDigit()) {
                    var ei = i
                    while (ei < a.length && a[ei].isDigit()) ei++
                    var ej = j
                    while (ej < b.length && b[ej].isDigit()) ej++
                    val cmp = a.substring(i, ei).toLong().compareTo(b.substring(j, ej).toLong())
                    if (cmp != 0) return cmp
                    i = ei
                    j = ej
                } else {
                    val cmp = ca.compareTo(cb)
                    if (cmp != 0) return cmp
                    i++
                    j++
                }
            }
            return (a.length - i).compareTo(b.length - j)
        }

        private fun stripExtension(name: String): String =
            if (name.endsWith(".cbz", true) || name.endsWith(".zip", true)) name.dropLast(4) else name
    }
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val failed: Int,
)
