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
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.animation.animatedFloat
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.animation.AnchorsFlingConfig
import androidx.ui.foundation.animation.fling
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.draggable
import androidx.ui.graphics.Color
import androidx.ui.layout.offset
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.dp

@Sampled
@Composable
fun DraggableSample() {
    // Draw a seekbar-like composable that has a black background
    // with a red square that moves along the 300.dp drag distance
    val squareSize = 50.dp
    val max = 300.dp
    val min = 0.dp
    val (minPx, maxPx) = with(DensityAmbient.current) {
        min.toPx() to max.toPx()
    }
    // this is the  state we will update while dragging
    var position by state { 0f }

    // seekbar itself
    Box(
        modifier = Modifier
            .preferredWidth(max + squareSize)
            .draggable(dragDirection = DragDirection.Horizontal) { delta ->
                // consume only delta that needed if we hit bounds
                val old = position
                position = (position + delta).coerceIn(minPx, maxPx)
                position - old
            },
        backgroundColor = Color.Black
    ) {
        val xOffset = with(DensityAmbient.current) { position.toDp() }
        Box(
            Modifier.offset(x = xOffset, y = 0.dp).preferredSize(squareSize),
            backgroundColor = Color.Red
        )
    }
}

@Sampled
@Composable
fun AnchoredDraggableSample() {
    // Draw a seekbar-like composable that has a black background
    // with a red square that moves along the 300.dp drag distance
    // and only can take 3 positions:  min, max and in the middle
    val squareSize = 50.dp
    val max = 300.dp
    val min = 0.dp
    val (minPx, maxPx) = with(DensityAmbient.current) {
        min.toPx() to max.toPx()
    }
    // define anchors (final position) and fling behavior to go to these anchors after drag
    val anchors = listOf(minPx, maxPx, maxPx / 2)
    val flingConfig = AnchorsFlingConfig(anchors)
    // define and set up animatedFloat as our dragging state
    val position = animatedFloat(0f)
    position.setBounds(minPx, maxPx)

    // seekbar itself
    Box(
        modifier = Modifier.preferredWidth(max + squareSize).draggable(
            startDragImmediately = position.isRunning,
            dragDirection = DragDirection.Horizontal,
            // launch fling with velocity to animate to the closes anchor
            onDragStopped = { position.fling(flingConfig, it) }
        ) { delta ->
            position.snapTo(position.value + delta)
            delta // consume all delta no matter the bounds to avoid nested dragging (as example)
        },
        backgroundColor = Color.Black
    ) {
        val xOffset = with(DensityAmbient.current) { position.value.toDp() }
        Box(
            Modifier.offset(x = xOffset, y = 0.dp).preferredSize(squareSize),
            backgroundColor = Color.Red
        )
    }
}
