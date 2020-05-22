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

package androidx.ui.core.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.rawScaleGestureFilter
import androidx.ui.core.gesture.RawScaleObserver
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.IntPx

/**
 * Demo app created to study some complex interactions of multiple DragGestureDetectors.
 */
@Composable
fun NestedScalingDemo() {
    Column {
        Text("Demonstrates nested scaling.")
        Text("As of now, this works the same way that nested scrolling does.  There is a scaling " +
                "region inside another scaling region. If you scale the inner region far " +
                "enough, it will actually stop scaling and the outer region will scale instead. " +
                "Or you can just scale the outer region (Scale out to get started)")
        Layout(
            children = {
                Scalable(.66666666f, Color(0xFFffeb3b.toInt())) {
                    Scalable(.5f, Color(0xFF4caf50.toInt())) {}
                }
            }) { measurables, constraints, _ ->
            val placeable = measurables.first().measure(constraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(
                    (constraints.maxWidth - placeable.width) / 2,
                    (constraints.maxHeight - placeable.height) / 2
                )
            }
        }
    }
}

@Composable
private fun Scalable(
    minPercent: Float,
    color: Color,
    children: @Composable () -> Unit
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

    Layout(
        children = children,
        modifier = Modifier.wrapContentSize(Alignment.Center)
            .rawScaleGestureFilter(outerScaleObserver)
            .drawBackground(color = color),
        measureBlock = { measurables, constraints, _ ->
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