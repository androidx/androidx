/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.framework.demos.gestures

import android.app.Activity
import android.os.Bundle
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Layout
import androidx.ui.core.gesture.RawScaleGestureDetector
import androidx.ui.core.gesture.RawScaleObserver
import androidx.ui.core.setContent
import androidx.ui.foundation.DrawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAlign
import androidx.ui.unit.IntPx

/**
 * Demo app created to study some complex interactions of multiple DragGestureDetectors.
 */
class NestedScalingDemo : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Layout(
                children = {
                    Scalable(.66666666f, Color(0xFFffeb3b.toInt())) {
                        Scalable(.5f, Color(0xFF4caf50.toInt())) {}
                    }
                },
                measureBlock = { measurables, constraints ->

                    val placeable = measurables.first().measure(constraints)

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            (constraints.maxWidth - placeable.width) / 2,
                            (constraints.maxHeight - placeable.height) / 2
                        )
                    }
                })
        }
    }
}

@Composable
fun Scalable(
    minPercent: Float,
    color: Color,
    children: @Composable() () -> Unit
) {

    val currentPercent = state { 1f }

    val outerScaleObserver = object : RawScaleObserver {
        override fun onScale(scaleFactor: Float): Float {
            val oldSize = currentPercent.value
            currentPercent.value = oldSize * scaleFactor
            if (currentPercent.value < minPercent) {
                currentPercent.value = minPercent
            } else if (currentPercent.value > 1f) {
                currentPercent.value = 1f
            }
            return currentPercent.value / oldSize
        }
    }

    RawScaleGestureDetector(outerScaleObserver) {
        Layout(
            children = children,
            modifier = LayoutAlign.Center + DrawBackground(color = color),
            measureBlock = { measurables, constraints ->
                val newConstraints =
                    constraints.copy(
                        maxWidth = constraints.maxWidth * currentPercent.value,
                        maxHeight = constraints.maxHeight * currentPercent.value,
                        minWidth = IntPx.Zero,
                        minHeight = IntPx.Zero
                    )

                val placeable = if (measurables.isNotEmpty()) {
                    measurables.first().measure(newConstraints)
                } else {
                    null
                }

                layout(newConstraints.maxWidth, newConstraints.maxHeight) {
                    placeable?.place(
                        (newConstraints.maxWidth - placeable.width) / 2,
                        (newConstraints.maxHeight - placeable.height) / 2
                    )
                }
            })
    }
}