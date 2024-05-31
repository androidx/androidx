/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.demos.suspendfun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.round
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Suppress("PrimitiveInCollection")
@OptIn(ExperimentalAnimationSpecApi::class)
@Preview
@Composable
fun OffsetKeyframeWithSplineDemo() {
    val points = remember { mutableStateListOf<Offset>() }
    val offsetAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val density = LocalDensity.current

    BoxWithConstraints(
        Modifier.fillMaxSize().drawBehind {
            drawPoints(
                points = points,
                pointMode = PointMode.Lines,
                color = Color.LightGray,
                strokeWidth = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f))
            )
        }
    ) {
        val minDimension = minOf(maxWidth, maxHeight)
        val size = minDimension / 4

        val sizePx = with(density) { size.toPx() }
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val maxXOff = (widthPx - sizePx) / 2f
        val maxYOff = heightPx - (sizePx / 2f)

        Box(
            Modifier.align(Alignment.TopCenter)
                .offset { offsetAnim.value.round() }
                .size(size)
                .background(Color.Red, RoundedCornerShape(50))
                .onPlaced { points.add(it.boundsInParent().center) }
        )

        LaunchedEffect(Unit) {
            delay(1000)
            while (isActive) {
                offsetAnim.animateTo(
                    targetValue = Offset.Zero,
                    animationSpec =
                        keyframesWithSpline {
                            durationMillis = 2400

                            // Increasingly approach the halfway point moving from side to side
                            repeat(4) {
                                val i = it + 1
                                val sign = if (i % 2 == 0) 1 else -1
                                Offset(
                                    x = maxXOff * (i.toFloat() / 5f) * sign,
                                    y = (maxYOff) * (i.toFloat() / 5f)
                                ) atFraction (0.1f * i)
                            }

                            // Halfway point (at bottom of the screen)
                            Offset(0f, maxYOff) atFraction 0.5f

                            // Return with mirrored movement
                            repeat(4) {
                                val i = it + 1
                                val sign = if (i % 2 == 0) 1 else -1
                                Offset(
                                    x = maxXOff * (1f - i.toFloat() / 5f) * sign,
                                    y = (maxYOff) * (1f - i.toFloat() / 5f)
                                ) atFraction ((0.1f * i) + 0.5f)
                            }
                        }
                )
                points.clear()
            }
        }
    }
}
