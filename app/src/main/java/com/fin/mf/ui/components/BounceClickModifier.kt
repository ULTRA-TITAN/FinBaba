package com.fin.mf.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * High-performance allocation-free scale feedback modifier on touch down/up.
 * Runs on the RenderThread via [graphicsLayer] to maintain a solid 120 FPS frame budget
 * without blocking underlying touch gestures or scroll handling.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.97f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Convenience bounceClick extension that attaches an [onClick] listener.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.97f,
    onClick: () -> Unit
): Modifier = this
    .bounceClick(scaleDown = scaleDown)
    .clickable(onClick = onClick)
