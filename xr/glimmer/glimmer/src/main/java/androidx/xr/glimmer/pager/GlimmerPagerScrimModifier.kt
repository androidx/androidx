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

package androidx.xr.glimmer.pager

import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost

internal fun Modifier.horizontalPagerScrim(state: GlimmerPagerState): Modifier =
    this.graphicsLayer {
            // Offscreen composition strategy is used because the scrim below uses DstOut blend
            // mode, which effectively cuts out a portion of this layer's content to reveal what is
            // behind it.
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithCache {
            val scrimSizePx = ScrimSize.toPx().fastCoerceAtMost(size.width / 2)

            val startBrush =
                Brush.horizontalGradient(colors = StartScrimColors, startX = 0f, endX = scrimSizePx)
            val endBrush =
                Brush.horizontalGradient(
                    colors = EndScrimColors,
                    startX = size.width - scrimSizePx,
                    endX = size.width,
                )
            val scrimSize = size.copy(width = scrimSizePx)
            val endTopLeft = Offset(size.width - scrimSizePx, 0f)

            onDrawWithContent {
                drawContent()

                val transitionProgress = state.transitionProgress

                // To maintain a consistent visual effect whether transitioning forward or backward,
                // the alpha progression must be symmetrical across the first and last halves of the
                // progress.
                val alpha =
                    when {
                        transitionProgress <= 0.05f -> 0f
                        transitionProgress <= 0.35f -> {
                            // Fade-in: 5% to 35%, alpha 0 -> 1
                            (transitionProgress - 0.05f) / 0.3f
                        }
                        transitionProgress <= 0.65f -> 1f // Hold: 35% to 65%, alpha 1
                        transitionProgress <= 0.95f -> {
                            // Fade-out: 65% to 95%, alpha 1 -> 0
                            1 - ((transitionProgress - 0.65f) / 0.3f)
                        }
                        else -> 0f
                    }

                if (alpha > 0f) {
                    // Start scrim (Left)
                    drawRect(
                        brush = startBrush,
                        topLeft = Offset.Zero,
                        size = scrimSize,
                        blendMode = BlendMode.DstOut,
                        alpha = alpha,
                    )
                    // End scrim (Right)
                    drawRect(
                        brush = endBrush,
                        topLeft = endTopLeft,
                        size = scrimSize,
                        blendMode = BlendMode.DstOut,
                        alpha = alpha,
                    )
                }
            }
        }

/**
 * Calculates the continuous fractional progress between pages during a transition, in the range
 * `[0.0, 1.0]`.
 *
 * This property normalizes the [GlimmerPagerState.currentPageOffsetFraction] to provide a
 * continuous progression relative to the anchored page of the transition.
 *
 * For example:
 * - During a transition from page `n` to `n+1`, `continuousOffsetFraction` progresses from `0.0` to
 *   `1.0`.
 * - Conversely, transitioning from page `n+1` to `n`, `continuousOffsetFraction` progresses from
 *   `1.0` to `0.0`.
 */
private val GlimmerPagerState.transitionProgress: Float
    @FrequentlyChangingValue
    get() {
        if (pageCount <= 1) return 0f

        // Combine the current page and offset to get a continuous fractional position.
        val continuousPosition = currentPage + currentPageOffsetFraction

        // Calculate the lower index of the two pages currently involved in the transition. The
        // maximum valid base page index is (pageCount - 2).
        val anchoredPage = continuousPosition.toInt().coerceIn(0, (pageCount - 2).coerceAtLeast(0))

        return continuousPosition - anchoredPage
    }

@Suppress("PrimitiveInCollection")
private val StartScrimColors = listOf(Color.Black, Color.Transparent)

@Suppress("PrimitiveInCollection")
private val EndScrimColors = listOf(Color.Transparent, Color.Black)

private val ScrimSize = 50.dp
