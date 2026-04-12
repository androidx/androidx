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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Sampled
@Composable
fun Draggable2DSample() {
    // Draw a box that has a a grey background
    // with a red square that moves along 300.dp dragging in both directions
    val max = 200.dp
    val min = 0.dp
    val (minPx, maxPx) = with(LocalDensity.current) { min.toPx() to max.toPx() }
    // this is the offset we will update while dragging
    var offsetPositionX by remember { mutableStateOf(0f) }
    var offsetPositionY by remember { mutableStateOf(0f) }

    Box(
        modifier =
            Modifier.width(max)
                .height(max)
                .draggable2D(
                    state =
                        rememberDraggable2DState { delta ->
                            val newValueX = offsetPositionX + delta.x
                            val newValueY = offsetPositionY + delta.y
                            offsetPositionX = newValueX.coerceIn(minPx, maxPx)
                            offsetPositionY = newValueY.coerceIn(minPx, maxPx)
                        }
                )
                .background(Color.LightGray)
    ) {
        Box(
            Modifier.offset {
                    IntOffset(offsetPositionX.roundToInt(), offsetPositionY.roundToInt())
                }
                .size(50.dp)
                .background(Color.Red)
        )
    }
}
