package org.kinera.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MangaSource(val id: String, val title: String) {
    MANGADEX("mangadex", "MangaDex"),
    MANGALIB("mangalib", "MangaLib"),
    REMANGA("remanga", "Remanga"),
    DESUME("desume", "Desu.me"),
    KOMGA("komga", "Komga"),
    SUWAYOMI("suwayomi", "Suwayomi"),
    LOCAL("local", "Local");

    companion object {
        fun fromId(id: String): MangaSource = entries.firstOrNull { it.id == id } ?: MANGADEX
    }
}
