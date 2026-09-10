/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a scroll-aware vertical gradient mask (fading edge) to the top and bottom of a scrollable
 * container controlled by [scrollState].
 *
 * @param scrollState The scroll state controlling the container.
 * @param fadeHeight Height of the gradient fade region.
 * @param bottomOffset Extra offset from the bottom of the container (e.g., system navigation bar
 *   inset) where the bottom fade should terminate.
 */
internal fun Modifier.verticalFadingEdge(
    scrollState: ScrollState,
    fadeHeight: Dp = 32.dp,
    bottomOffset: Dp = 0.dp,
): Modifier =
    verticalFadingEdge(
        fadeHeight = fadeHeight,
        bottomOffset = bottomOffset,
        topAlpha = { fadePx -> (scrollState.value.toFloat() / fadePx).coerceIn(0f, 1f) },
        bottomAlpha = { fadePx ->
            val remainingScroll = (scrollState.maxValue - scrollState.value).toFloat()
            (remainingScroll / fadePx).coerceIn(0f, 1f)
        },
    )

/**
 * Applies a scroll-aware vertical gradient mask (fading edge) to the top and bottom of a
 * [LazyListState] container.
 *
 * @param lazyListState The lazy list state controlling the container.
 * @param fadeHeight Height of the gradient fade region.
 * @param bottomOffset Extra offset from the bottom of the container (e.g., system navigation bar
 *   inset) where the bottom fade should terminate.
 */
internal fun Modifier.verticalFadingEdge(
    lazyListState: LazyListState,
    fadeHeight: Dp = 32.dp,
    bottomOffset: Dp = 0.dp,
): Modifier =
    verticalFadingEdge(
        fadeHeight = fadeHeight,
        bottomOffset = bottomOffset,
        topAlpha = { fadePx ->
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (lazyListState.firstVisibleItemScrollOffset.toFloat() / fadePx).coerceIn(0f, 1f)
            }
        },
        bottomAlpha = { if (lazyListState.canScrollForward) 1f else 0f },
    )

private fun Modifier.verticalFadingEdge(
    fadeHeight: Dp,
    bottomOffset: Dp,
    topAlpha: (fadePx: Float) -> Float,
    bottomAlpha: (fadePx: Float) -> Float,
): Modifier =
    this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithCache {
        val fadePx = fadeHeight.toPx()
        val bottomOffsetPx = bottomOffset.toPx()
        if (fadePx <= 0f) return@drawWithCache onDrawWithContent { drawContent() }

        val topBrush =
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = fadePx,
            )
        val bottomBrush =
            Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = 0f,
                endY = fadePx,
            )

        onDrawWithContent {
            drawContent()

            if (size.isEmpty()) return@onDrawWithContent

            val currentTopAlpha = topAlpha(fadePx)
            if (currentTopAlpha > 0f) {
                translate(top = (currentTopAlpha - 1f) * fadePx) {
                    drawRect(
                        brush = topBrush,
                        size = Size(size.width, fadePx),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }

            val currentBottomAlpha = bottomAlpha(fadePx)
            if (currentBottomAlpha > 0f) {
                val endY = (size.height - bottomOffsetPx).coerceAtLeast(0f)
                val startY = (endY - fadePx).coerceAtLeast(0f)
                if (endY > startY) {
                    translate(top = endY - currentBottomAlpha * fadePx) {
                        drawRect(
                            brush = bottomBrush,
                            size = Size(size.width, currentBottomAlpha * fadePx),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }
                if (bottomOffsetPx > 0f) {
                    drawRect(
                        color = Color.Black.copy(alpha = 1f - currentBottomAlpha),
                        topLeft = Offset(0f, endY),
                        size = Size(size.width, bottomOffsetPx),
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
        }
    }
