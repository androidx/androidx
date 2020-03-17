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
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.gesture.ScaleGestureDetector
import androidx.ui.core.gesture.ScaleObserver
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.graphics.Color
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.px

/**
 * Simple demo that shows off how DragGestureDetector and ScaleGestureDetector automatically
 * interoperate.
 */
@Composable
fun DragScaleGestureDetectorDemo() {
    val width = state { 200.dp }
    val height = state { 200.dp }
    val xOffset = state { 0.px }
    val yOffset = state { 0.px }
    val dragInScale = state { false }

    val scaleObserver = object : ScaleObserver {
        override fun onScale(scaleFactor: Float) {
            width.value *= scaleFactor
            height.value *= scaleFactor
        }
    }

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: PxPosition): PxPosition {
            xOffset.value += dragDistance.x
            yOffset.value += dragDistance.y
            return dragDistance
        }
    }

    val onRelease = {
        dragInScale.value = !dragInScale.value
    }

    if (dragInScale.value) {
        ScaleGestureDetector(scaleObserver) {
            TouchSlopDragGestureDetector(dragObserver = dragObserver) {
                PressReleasedGestureDetector(onRelease) {
                    DrawingBox(
                        xOffset.value,
                        yOffset.value,
                        width.value,
                        height.value,
                        Color(0xFFf44336.toInt())
                    )
                }
            }
        }
    } else {
        TouchSlopDragGestureDetector(dragObserver = dragObserver) {
            ScaleGestureDetector(scaleObserver) {
                PressReleasedGestureDetector(onRelease) {
                    DrawingBox(
                        xOffset.value,
                        yOffset.value,
                        width.value,
                        height.value,
                        Color(0xFF2196f3.toInt())
                    )
                }
            }
        }
    }
}