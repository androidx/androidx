/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
@Sampled
fun WindowInsetsRulersSample() {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(Modifier.background(Color.Blue)) // top area (e.g. status bar)
            Box(Modifier.background(Color.Yellow)) // bottom area (e.g. navigation bar)
            Box(Modifier.background(Color.White)) // content between top and bottom
        },
        measurePolicy = { measurables, constraints ->
            if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                layout(width, height) {
                    val top =
                        maxOf(0, WindowInsetsRulers.SystemBars.current.top.current(0f).roundToInt())
                    val topArea = measurables[0].measure(Constraints.fixed(width, top))
                    topArea.place(0, 0)

                    val bottom =
                        minOf(
                            height,
                            WindowInsetsRulers.SystemBars.current.bottom.current(0f).roundToInt(),
                        )
                    val bottomArea =
                        measurables[1].measure(Constraints.fixed(width, height - bottom))
                    bottomArea.place(0, bottom)

                    val contentArea = measurables[2].measure(Constraints.fixed(width, bottom - top))
                    contentArea.place(0, top)
                }
            } else {
                // It should only get here if inside scrollable content or trying to align
                // to an alignment line. Only place the content.
                val placeable = measurables[2].measure(constraints) // content
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        },
    )
}

@Composable
@Sampled
fun SourceAndTargetInsetsSample() {
    Column(Modifier.fillMaxSize()) {
        // TextField will show the IME when it is focused.
        TextField("HelloWorld", {}, Modifier.fillMaxWidth())
        // When the IME shows, animate the content to align with the top of the IME.
        // When the IME hides, animate the content to the top of the Box.
        val verticalPosition = remember { Animatable(0f) }
        val coroutineScope = rememberCoroutineScope()
        Box(
            Modifier.fillMaxSize().background(Color.Yellow).layout { measurable, constraints ->
                if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val placeable =
                            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        val ime = WindowInsetsRulers.Ime
                        val animationProperties = ime.getAnimation(this)
                        val height = constraints.maxHeight.toFloat()
                        val sourceBottom = animationProperties.source.bottom.current(height)
                        val targetBottom = animationProperties.target.bottom.current(height)
                        val targetPosition =
                            if (!animationProperties.isVisible || sourceBottom < targetBottom) {
                                // IME is either not visible or animating away
                                0f
                            } else if (animationProperties.isAnimating) {
                                // IME is visible and animating
                                targetBottom - placeable.height
                            } else {
                                // IME is visible and not animating, so use the IME poosition
                                ime.current.bottom.current(height) - placeable.height
                            }
                        if (targetPosition != verticalPosition.targetValue) {
                            coroutineScope.launch { verticalPosition.animateTo(targetPosition) }
                        }
                        placeable.place(0, verticalPosition.value.roundToInt())
                    }
                } else {
                    // It should only get here if inside scrollable content or aligning to an
                    // alignment
                    // line.
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        ) {
            // content
            Box(Modifier.size(100.dp).background(Color.Blue))
        }
    }
}

@Composable
@Sampled
fun InsetsRulersAlphaSample() {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(Modifier.background(Color.Blue)) // status bar background
            Box(Modifier.background(Color.Yellow)) // navigation bar background on bottom
            Box(Modifier.background(Color.White)) // content between top and bottom
        },
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            layout(width, height) {
                val top = WindowInsetsRulers.StatusBars.current.top.current(0f).roundToInt()
                val bottom =
                    WindowInsetsRulers.NavigationBars.current.bottom.current(0f).roundToInt()
                measurables[0].measure(Constraints.fixed(width, top)).placeWithLayer(0, 0) {
                    alpha = WindowInsetsRulers.StatusBars.getAnimation(this@layout).alpha
                }
                measurables[2].measure(Constraints.fixed(width, height - bottom)).place(0, bottom)
                measurables[1].measure(Constraints.fixed(width, bottom - top)).placeWithLayer(
                    0,
                    top,
                ) {
                    alpha = WindowInsetsRulers.NavigationBars.getAnimation(this@layout).alpha
                }
            }
        },
    )
}

@Composable
@Sampled
fun MaximumSample() {
    // When the status bar is visible, don't show the content that would be in the status area.
    // When the status bar is hidden, show content that would be in the status area.
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(Modifier.background(Color.Blue)) // status bar area content
            Box(Modifier.background(Color.Yellow)) // normal content
        },
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            layout(width, height) {
                val top = WindowInsetsRulers.StatusBars.maximum.top.current(0f).roundToInt()
                val statusBarAnimationProperties = WindowInsetsRulers.StatusBars.getAnimation(this)
                if (!statusBarAnimationProperties.isVisible) {
                    // Only place the status bar content when the status bar isn't visible. We don't
                    // want it cluttering the status bar area when the status bar is shown.
                    measurables[0].measure(Constraints.fixed(width, top)).place(0, 0)
                }
                // Place the normal content below where the status bar would be.
                measurables[1].measure(Constraints.fixed(width, height - top)).place(0, top)
            }
        },
    )
}
