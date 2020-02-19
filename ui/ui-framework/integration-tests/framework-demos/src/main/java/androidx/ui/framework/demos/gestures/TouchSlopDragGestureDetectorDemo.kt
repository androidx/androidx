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
import androidx.compose.state
import androidx.ui.core.Direction
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.px

/**
 * Simple demo that shows off TouchSlopDragGestureDetector.
 */
class TouchSlopDragGestureDetectorDemo : Activity() {

    val VerticalColor = Color(0xfff44336)
    val HorizontalColor = Color(0xff2196f3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val xOffset = state { 0.px }
            val yOffset = state { 0.px }
            val canStartVertically = state { true }

            val dragObserver =
                if (canStartVertically.value) {
                    object : DragObserver {
                        override fun onDrag(dragDistance: PxPosition): PxPosition {
                            yOffset.value += dragDistance.y
                            return dragDistance
                        }

                        override fun onStop(velocity: PxPosition) {
                            canStartVertically.value = !canStartVertically.value
                            super.onStop(velocity)
                        }
                    }
                } else {
                    object : DragObserver {
                        override fun onDrag(dragDistance: PxPosition): PxPosition {
                            xOffset.value += dragDistance.x
                            return dragDistance
                        }

                        override fun onStop(velocity: PxPosition) {
                            canStartVertically.value = !canStartVertically.value
                            super.onStop(velocity)
                        }
                    }
                }

            val canDrag =
                if (canStartVertically.value) {
                    { direction: Direction ->
                        when (direction) {
                            Direction.DOWN -> true
                            Direction.UP -> true
                            else -> false
                        }
                    }
                } else {
                    { direction: Direction ->
                        when (direction) {
                            Direction.LEFT -> true
                            Direction.RIGHT -> true
                            else -> false
                        }
                    }
                }

            val color =
                if (canStartVertically.value) {
                    VerticalColor
                } else {
                    HorizontalColor
                }

            TouchSlopDragGestureDetector(dragObserver, canDrag) {
                DrawingBox(xOffset.value, yOffset.value, 96.dp, 96.dp, color)
            }
        }
    }
}