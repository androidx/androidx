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
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Direction
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutOffset
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * Simple demo that shows off TouchSlopDragGestureDetector.
 */
@Composable
fun TouchSlopDragGestureDetectorDemo() {

    val verticalColor = Color(0xfff44336)
    val horizontalColor = Color(0xff2196f3)

    val offset = state { PxPosition.Origin }
    val canStartVertically = state { true }

    val dragObserver =
        if (canStartVertically.value) {
            object : DragObserver {
                override fun onDrag(dragDistance: PxPosition): PxPosition {
                    offset.value =
                        PxPosition(x = offset.value.x, y = offset.value.y + dragDistance.y)
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
                    offset.value =
                        PxPosition(x = offset.value.x + dragDistance.x, y = offset.value.y)
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
            verticalColor
        } else {
            horizontalColor
        }

    val (offsetX, offsetY) =
        with(DensityAmbient.current) { offset.value.x.toDp() to offset.value.y.toDp() }

    Box(
        LayoutOffset(offsetX, offsetY) +
                LayoutSize.Fill + LayoutAlign.Center +
                DragGestureDetector(dragObserver, canDrag) +
                LayoutSize(96.dp),
        backgroundColor = color
    )
}
