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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

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
    if (maxScrimSize.value == 0f) return this
    require(maxScrimSize.value > 0f) { "Scrim size can't be negative: $maxScrimSize" }

    val modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    return if (orientation == Orientation.Vertical) {
        modifier.drawWithCache {
            val maxScrimHeight = (size.height / 2f).coerceAtMost(maxScrimSize.toPx())
            onDrawWithContent {
                drawContent()
                drawScrims(
                    left = 0f,
                    top = state.scrollOffset.toFloat().coerceAtMost(maxScrimHeight),
                    right = 0f,
                    bottom = state.scrollEndOffset().toFloat().coerceAtMost(maxScrimHeight),
                )
            }
        }
    } else {
        modifier.drawWithCache {
            val maxScrimWidth = (size.width / 2f).coerceAtMost(maxScrimSize.toPx())
            onDrawWithContent {
                drawContent()
                val start = state.scrollOffset.toFloat().coerceAtMost(maxScrimWidth)
                val end = state.scrollEndOffset().toFloat().coerceAtMost(maxScrimWidth)
                drawScrims(
                    left = if (layoutDirection == LayoutDirection.Ltr) start else end,
                    top = 0f,
                    right = if (layoutDirection == LayoutDirection.Ltr) end else start,
                    bottom = 0f,
                )
            }
        }
    }
}

private fun ScrollIndicatorState.scrollEndOffset() = contentSize - scrollOffset - viewportSize

/**
 * Applies a gradient scrim (a fade effect) to the edges of the content.
 *
 * The returned [PaddingValues] defines how far inset into the content the scrim will be.
 */
internal fun Modifier.edgeScrim(scrims: Density.(Size) -> PaddingValues): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
        onDrawWithContent {
            val scrims = scrims(this, size)
            drawContent()
            if (scrims != PaddingValues.Zero) {
                drawScrims(
                    left = scrims.calculateLeftPadding(layoutDirection).toPx(),
                    top = scrims.calculateTopPadding().toPx(),
                    right = scrims.calculateRightPadding(layoutDirection).toPx(),
                    bottom = scrims.calculateBottomPadding().toPx(),
                )
            }
        }
    }

private fun DrawScope.drawScrims(left: Float, top: Float, right: Float, bottom: Float) {
    val contentSize = size
    if (left > 0f) {
        scale(scaleX = left, scaleY = contentSize.height, pivot = Offset.Zero) {
            drawScrimRect(ScrimUnitBrushes.Left)
        }
    }
    if (top > 0f) {
        scale(scaleX = contentSize.width, scaleY = top, pivot = Offset.Zero) {
            drawScrimRect(ScrimUnitBrushes.Top)
        }
    }
    if (right > 0f) {
        withTransform({
            translate(left = contentSize.width - right)
            scale(scaleX = right, scaleY = contentSize.height, pivot = Offset.Zero)
        }) {
            drawScrimRect(ScrimUnitBrushes.Right)
        }
    }
    if (bottom > 0f) {
        withTransform({
            translate(top = contentSize.height - bottom)
            scale(scaleX = contentSize.width, scaleY = bottom, pivot = Offset.Zero)
        }) {
            drawScrimRect(ScrimUnitBrushes.Bottom)
        }
    }
}

private fun DrawScope.drawScrimRect(brush: Brush) {
    // we are already scaled and translated before drawing this, so just draw a 1x1 rect
    drawRect(brush = brush, size = Size(1f, 1f), blendMode = BlendMode.DstOut)
}

/**
 * "Unit" brushes, which apply a scrim properly to a 1x1 square.
 *
 * Before using these, use [DrawScope.withTransform] to translate and scale appropriately.
 */
private object ScrimUnitBrushes {
    val Left =
        Brush.horizontalGradient(
            colors = listOf(Color.Black, Color.Transparent),
            startX = 0f,
            endX = 1f,
        )
    val Top =
        Brush.verticalGradient(
            colors = listOf(Color.Black, Color.Transparent),
            startY = 0f,
            endY = 1f,
        )
    val Right =
        Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = 0f,
            endX = 1f,
        )
    val Bottom =
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startY = 0f,
            endY = 1f,
        )
}
