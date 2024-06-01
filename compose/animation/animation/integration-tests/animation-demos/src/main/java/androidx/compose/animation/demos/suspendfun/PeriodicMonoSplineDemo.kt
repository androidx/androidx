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

@file:OptIn(ExperimentalAnimationSpecApi::class)
@file:Suppress("InfiniteTransitionLabel", "InfinitePropertiesLabel")

package androidx.compose.animation.demos.suspendfun

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.KeyframesWithSplineSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.keyframesWithSpline
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Preview
@Composable
fun PeriodicMonoSplineDemo() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    ) {
        MonoSplineCurve(
            periodicBias = Float.NaN,
            modifier = Modifier.height(300.dp).background(Color.LightGray)
        )
        MonoSplineCurve(
            periodicBias = 0f,
            modifier = Modifier.height(300.dp).background(Color.LightGray)
        )
        MonoSplineCurve(
            periodicBias = 0.5f,
            modifier = Modifier.height(300.dp).background(Color.LightGray)
        )
        MonoSplineCurve(
            periodicBias = 1f,
            modifier = Modifier.height(300.dp).background(Color.LightGray)
        )
    }
}

private fun periodicKeyframes(periodicBias: Float): KeyframesWithSplineSpec<Offset> =
    keyframesWithSpline(periodicBias) {
        durationMillis = 2000

        Offset(0.5f, 1f) atFraction 0.5f
    }

private val pathWidth = 2.dp
private val pathColor = Color(0xFFFFC107)
private val padding = 40.dp
private val indicatorSize = 4.dp

@Composable
private fun MonoSplineCurve(periodicBias: Float, modifier: Modifier = Modifier) {
    val sampleSize = 1_000
    val density = LocalDensity.current
    val keyframesSpec = remember(periodicBias) { periodicKeyframes(periodicBias) }
    val points = remember { FloatArray(sampleSize) }
    val pointsPath = remember { Path() }
    var isDraw by remember { mutableStateOf(false) }

    val infiniteAnimation = rememberInfiniteTransition()
    val animatedValue =
        infiniteAnimation.animateValue(
            initialValue = Offset.Zero,
            targetValue = Offset(1f, 0f),
            typeConverter = Offset.VectorConverter,
            animationSpec =
                InfiniteRepeatableSpec(animation = keyframesSpec, repeatMode = RepeatMode.Restart)
        )

    val text =
        remember(periodicBias) {
            if (periodicBias.isNaN()) {
                "Normal Monotonic Spline"
            } else {
                "Periodic with bias: $periodicBias"
            }
        }

    val indicatorSizePx = with(density) { indicatorSize.toPx() }
    val indicatorPath =
        remember(indicatorSizePx) {
            Path().apply {
                moveTo(0f, -indicatorSizePx)
                lineTo(indicatorSizePx, 0f)
                lineTo(0f, indicatorSizePx)
                lineTo(-indicatorSizePx, 0f)
                close()
            }
        }

    Row(modifier) {
        Box(Modifier.fillMaxHeight().weight(1f, true)) {
            Text(text = text)
            Box(
                Modifier.fillMaxSize()
                    .padding(padding)
                    .drawWithCache {
                        if (isDraw) {
                            val pathWidthPx = pathWidth.toPx()
                            pointsPath.reset()
                            val arraySize = points.size
                            val width = size.width
                            val height = size.height
                            points.forEachIndexed { index, yFactor ->
                                val xi = (index.toFloat() / arraySize) * width
                                pointsPath.lineTo(xi, yFactor * height)
                            }
                            onDrawBehind {
                                scale(1f, -1f) {
                                    drawPath(
                                        path = pointsPath,
                                        color = pathColor,
                                        style = Stroke(width = pathWidthPx)
                                    )
                                }
                            }
                        } else {
                            onDrawBehind {}
                        }
                    }
                    .drawBehind {
                        scale(1f, -1f) {
                            val currValue = animatedValue.value
                            translate(size.width * currValue.x, size.height * currValue.y) {
                                drawPath(path = indicatorPath, color = Color.Red, style = Fill)
                            }
                        }
                    }
            )
        }
        Box(
            Modifier.fillMaxHeight().width(40.dp).padding(vertical = padding).drawBehind {
                scale(1f, -1f) {
                    drawCircle(
                        color = Color.Red,
                        radius = size.width * 0.5f,
                        center =
                            Offset(x = size.width * 0.5f, y = size.height * animatedValue.value.y)
                    )
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val zeroVector = AnimationVector2D(0f, 0f)
        val vectorized = keyframesSpec.vectorize(Offset.VectorConverter)
        var timeMillis = 0f
        val step = vectorized.durationMillis.toFloat() / sampleSize
        var count = 0
        while (count < sampleSize) {
            val vectorValue =
                vectorized.getValueFromNanos(
                    playTimeNanos = timeMillis.roundToLong() * 1_000_000,
                    initialValue = zeroVector,
                    targetValue = zeroVector,
                    initialVelocity = zeroVector
                )
            points[count] = vectorValue.v2
            timeMillis += step
            count++
        }
        isDraw = true
    }
}
