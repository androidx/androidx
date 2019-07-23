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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.gestures.AnchorsFlingConfig
import androidx.ui.foundation.gestures.AnimatedDraggable
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Container
import androidx.ui.layout.Padding

@Sampled
@Composable
fun AnimatedDraggableSample() {
    // Composable that users can drag over 300 dp. There are 3 anchors
    // and the value will gravitate to 0, 150 or 300 dp
    val max = 300.dp
    val min = 0.dp
    val (minPx, maxPx) = +withDensity {
        min.toPx().value to max.toPx().value
    }

    AnimatedDraggable(
        dragDirection = DragDirection.Horizontal,
        startValue = minPx,
        minValue = minPx,
        maxValue = maxPx,
        // Specify an anchored behavior for the fling with anchors at max, min and center.
        flingConfig = AnchorsFlingConfig(listOf(minPx, maxPx / 2, maxPx))
    ) { dragValue ->

        // dragValue is the AnimatedFloat with current value in progress
        // of dragging or animating
        val draggedDp = +withDensity {
            dragValue.value.toDp()
        }
        val squareSize = 50.dp

        // Draw a seekbar-like widget that has a black background
        // with a red square that moves along the drag
        Container(width = max + squareSize, alignment = Alignment.CenterLeft) {
            DrawShape(RectangleShape, Color.Black)
            Padding(left = draggedDp) {
                ColoredRect(Color.Red, width = squareSize, height = squareSize)
            }
        }
    }
}