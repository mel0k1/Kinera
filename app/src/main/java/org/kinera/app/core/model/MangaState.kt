package org.kinera.app.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MangaState {
    ONGOING,
    FINISHED,
    PAUSED,
    CANCELLED,
    UNKNOWN;
}
