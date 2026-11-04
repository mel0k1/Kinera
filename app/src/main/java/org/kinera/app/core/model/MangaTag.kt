package org.kinera.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaTag(
    val id: String,
    val title: String,
)
