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
import androidx.compose.animation.core.ArcMode.Companion.ArcAbove
import androidx.compose.animation.core.ArcMode.Companion.ArcBelow
import androidx.compose.animation.core.ArcMode.Companion.ArcLinear
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

@SuppressWarnings("PrimitiveInCollection")
@OptIn(ExperimentalAnimationSpecApi::class)
@Preview
@Composable
fun ArcOffsetDemo() {
    val animOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val points = remember { mutableStateListOf<Offset>() }
    val target = remember { MutableStateFlow(Offset.Unspecified) }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                val halfSize = size.toSize() * 0.5f
                detectTapGestures {
                    target.value = Offset(x = it.x - halfSize.width, y = it.y - halfSize.height)
                }
            }
            .drawBehind {
                val halfSize = size * 0.5f
                translate(halfSize.width, halfSize.height) {
                    drawPoints(
                        points = points,
                        pointMode = PointMode.Lines,
                        color = Color(0xFFFFC107),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 2f)),
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {
        Text("Tap anywhere to animate")
        Box(
            Modifier.size(50.dp)
                .align(Alignment.Center)
                .offset { animOffset.value.round() }
                .background(Color.Red, RoundedCornerShape(50))
        )
    }

    LaunchedEffect(Unit) {
        target.collectLatest { target ->
            if (target != Offset.Unspecified) {
                points.clear()
                val current = animOffset.value
                val diffOff = target - current
                val halfDiff = diffOff * 0.5f
                val midOffset = current + halfDiff
                val mode = if (diffOff.y > 0f) ArcBelow else ArcAbove
                animOffset.animateTo(
                    targetValue = target,
                    animationSpec =
                        keyframes {
                            durationMillis = 1400

                            current atFraction 0f using LinearEasing using ArcLinear
                            midOffset atFraction 0.5f using FastOutSlowInEasing using mode
                        }
                ) {
                    points.add(value)
                }
            }
        }
    }
}
