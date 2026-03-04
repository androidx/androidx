/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.glimmer

import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastCoerceAtMost

/**
 * Applies a gradient scrim (a fade effect) to the edges of the content. This scrim is not visible
 * when the user reaches an edge and scales gradually as the user scrolls. In other words, the scrim
 * only appears when scrolling in that direction is possible.
 *
 * @param state The [androidx.compose.foundation.ScrollIndicatorState] associated with the layout
 *   receiving the scrim
 * @param maxScrimSize The maximum size of the scrim, in [Dp], from the edge in the specified
 *   orientation. The size of the scrim might be less at the beginning and end of the list, and the
 *   maximum size might be reduced if there's not enough space for the full size. If set to 0, no
 *   scrim will be applied. If this value is negative, an exception is thrown.
 * @param orientation The main axis in which this container scrolls
 * @throws IllegalArgumentException if [maxScrimSize] is negative.
 */
internal fun Modifier.edgeScrim(
    state: ScrollIndicatorState,
    maxScrimSize: Dp,
    orientation: Orientation,
): Modifier {
    require(maxScrimSize.value >= 0f) { "Scrim size can't be negative: $maxScrimSize" }
    if (maxScrimSize.value == 0f) {
        return this
    }

    // Offscreen composition strategy is used because below scrim uses DstOut blend mode,
    // which cuts out the content of the list and keeps only the background behind.
    val modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    return if (orientation == Orientation.Vertical) {
        modifier.drawVerticalScrim(
            state = state,
            scrimHeight = maxScrimSize,
            gradientStops = DefaultGradientStops,
        )
    } else {
        modifier.drawHorizontalScrim(
            state = state,
            scrimWidth = maxScrimSize,
            gradientStops = DefaultGradientStops,
        )
    }
}

/** Please see [edgeScrim] for documentation. */
private fun Modifier.drawVerticalScrim(
    state: ScrollIndicatorState,
    scrimHeight: Dp,
    gradientStops: Array<Pair<Float, Color>>,
): Modifier {
    return drawWithCache {
        val scrimHeightPx = minOf(scrimHeight.toPx(), size.height / 2)

        val topBrush =
            Brush.verticalGradient(colorStops = gradientStops, startY = 0f, endY = scrimHeightPx)
        val bottomBrush =
            Brush.verticalGradient(
                colorStops = gradientStops,
                startY = size.height,
                endY = size.height - scrimHeightPx,
            )
        val scrimSize = size.copy(height = scrimHeightPx)
        val bottomOffset = Offset(x = 0f, y = size.height - scrimHeightPx)
        val bottomPivot = Offset(x = 0f, y = size.height)

        onDrawWithContent {
            drawContent()
            drawScrim(
                state = state,
                orientation = Orientation.Vertical,
                scrimMaxSizePx = scrimHeightPx,
                scrimSize = scrimSize,
                startBrush = topBrush,
                endBrush = bottomBrush,
                endPivot = bottomPivot,
                endOffset = bottomOffset,
            )
        }
    }
}

/** Please see [edgeScrim] for documentation. */
private fun Modifier.drawHorizontalScrim(
    state: ScrollIndicatorState,
    scrimWidth: Dp,
    gradientStops: Array<Pair<Float, Color>>,
): Modifier {
    return drawWithCache {
        val scrimWidthPx = minOf(scrimWidth.toPx(), size.width / 2)

        val leftBrush =
            Brush.horizontalGradient(colorStops = gradientStops, startX = 0f, endX = scrimWidthPx)
        val rightBrush =
            Brush.horizontalGradient(
                colorStops = gradientStops,
                startX = size.width,
                endX = size.width - scrimWidthPx,
            )

        val scrimSize = size.copy(width = scrimWidthPx)
        val rightOffset = Offset(x = size.width - scrimWidthPx, y = 0f)
        val rightPivot = Offset(x = size.width, y = 0f)

        onDrawWithContent {
            drawContent()
            drawScrim(
                state = state,
                orientation = Orientation.Horizontal,
                scrimMaxSizePx = scrimWidthPx,
                scrimSize = scrimSize,
                startBrush = leftBrush,
                endBrush = rightBrush,
                endPivot = rightPivot,
                endOffset = rightOffset,
            )
        }
    }
}

private fun DrawScope.drawScrim(
    state: ScrollIndicatorState,
    orientation: Orientation,
    scrimMaxSizePx: Float,
    scrimSize: Size,
    startBrush: Brush,
    endBrush: Brush,
    endPivot: Offset,
    endOffset: Offset,
) {
    val startScrollOffsetPx = state.scrollOffset
    val endScrollOffsetPx = state.contentSize - state.viewportSize - startScrollOffsetPx

    val startScrimScale = (startScrollOffsetPx / scrimMaxSizePx).fastCoerceAtMost(1f)
    val endScrimScale = (endScrollOffsetPx / scrimMaxSizePx).fastCoerceAtMost(1f)

    if (startScrimScale > 0f) {
        scale(
            scaleX = if (orientation == Orientation.Vertical) 1f else startScrimScale,
            scaleY = if (orientation == Orientation.Vertical) startScrimScale else 1f,
            pivot = Offset.Zero,
        ) {
            drawRect(
                brush = startBrush,
                topLeft = Offset.Zero,
                size = scrimSize,
                blendMode = BlendMode.DstOut,
            )
        }
    }
    if (endScrimScale > 0f) {
        scale(
            scaleX = if (orientation == Orientation.Vertical) 1f else endScrimScale,
            scaleY = if (orientation == Orientation.Vertical) endScrimScale else 1f,
            pivot = endPivot,
        ) {
            drawRect(
                brush = endBrush,
                topLeft = endOffset,
                size = scrimSize,
                blendMode = BlendMode.DstOut,
            )
        }
    }
}

private val DefaultGradientStops: Array<Pair<Float, Color>> =
    arrayOf(0.00f to Color.Black, 1.00f to Color.Transparent)
