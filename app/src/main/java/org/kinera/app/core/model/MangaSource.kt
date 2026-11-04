package org.kinera.app.core.model

enum class MangaSource(val id: String, val title: String) {
    MANGADEX("mangadex", "MangaDex");

    companion object {
        fun fromId(id: String): MangaSource = entries.firstOrNull { it.id == id } ?: MANGADEX
    }
}
