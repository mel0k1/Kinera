package org.kinera.app.core.model

data class Paged<T>(
    val items: List<T>,
    val offset: Int = 0,
    val total: Int = items.size,
    val hasNext: Boolean = false,
)
