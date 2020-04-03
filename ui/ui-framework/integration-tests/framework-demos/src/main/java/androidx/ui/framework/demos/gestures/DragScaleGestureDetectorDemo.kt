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

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.scaleGestureFilter
import androidx.ui.core.gesture.ScaleObserver
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.offset
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * Simple demo that shows off how DragGestureDetector and ScaleGestureDetector automatically
 * interoperate.
 */
@Composable
fun DragScaleGestureDetectorDemo() {
    val size = state { 200.dp }
    val offset = state { PxPosition.Origin }
    val dragInScale = state { false }

    val scaleObserver = object : ScaleObserver {
        override fun onScale(scaleFactor: Float) {
            size.value *= scaleFactor
        }
    }

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: PxPosition): PxPosition {
            offset.value += dragDistance
            return dragDistance
        }
    }

    val onRelease = {
        dragInScale.value = !dragInScale.value
    }

    val gestures =
        if (dragInScale.value) {
            Modifier
                .scaleGestureFilter(scaleObserver)
                .dragGestureFilter(dragObserver)
                .tapGestureFilter(onRelease)
        } else {
            Modifier
                .dragGestureFilter(dragObserver)
                .scaleGestureFilter(scaleObserver)
                .tapGestureFilter(onRelease)
        }

    val (offsetX, offsetY) =
        with(DensityAmbient.current) { offset.value.x.toDp() to offset.value.y.toDp() }

    Box(
        Modifier.offset(offsetX, offsetY)
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .plus(gestures)
            .preferredSize(size.value),
        backgroundColor = Color(0xFFf44336.toInt())
    )
}
