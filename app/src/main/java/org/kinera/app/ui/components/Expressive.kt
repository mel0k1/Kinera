package org.kinera.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.MaterialTheme

/**
 * Signature M3 Expressive press feedback: the element springs down while
 * pressed and bounces back on release, driven by the theme motion scheme.
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        // A crisp spring instead of the bouncy default: the press should
        // feel immediate, not floaty.
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
    }
}

/** Convenience factory matching [pressScale]'s default source. */
@Composable
fun rememberPressInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }
