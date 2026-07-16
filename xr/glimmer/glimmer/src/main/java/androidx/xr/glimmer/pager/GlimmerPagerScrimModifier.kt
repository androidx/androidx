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

                val continuousPosition = state.continuousPosition
                val pageCount = state.pageCount
                val transitionProgress =
                    getTransitionProgress(
                        continuousPosition = continuousPosition,
                        closestPageFromStart =
                            getClosestPageFromStart(continuousPosition, pageCount),
                        pageCount = pageCount,
                    )

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

@Suppress("PrimitiveInCollection")
private val StartScrimColors = listOf(Color.Black, Color.Transparent)

@Suppress("PrimitiveInCollection")
private val EndScrimColors = listOf(Color.Transparent, Color.Black)

private val ScrimSize = 50.dp
