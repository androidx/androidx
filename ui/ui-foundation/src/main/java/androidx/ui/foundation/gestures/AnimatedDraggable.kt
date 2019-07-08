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

package androidx.ui.foundation.gestures

import androidx.animation.AnimatedFloat
import androidx.animation.BaseAnimatedValue
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.animation.animatedFloat
import androidx.ui.core.PxPosition
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.px

/**
 * Component that provides drag, fling and animation logic for one [Float] value.
 *
 * The common usecase for this component is when you need to be able to drag/scroll something
 * on the screen and also one or more of the following:
 * 1. Fling support when scrolling/dragging ends with velocity.
 * 2. Stable anchors points support for dragging value,
 * e.g. be able to drag between only predefined set of values.
 * 3. Automatic animation of draggable value, e.g emulate drag by click.
 *
 * @see [FlingConfig] to control anchors or fling intensity.
 *
 * This component provides high-level API and ownership for [AnimatedFloat]
 * and returns it as a parameter for its children.
 *
 * If you need only drag support without animations, consider using [DragGestureDetector] instead.
 *
 * If you need only animations without gesture support, consider using [animatedFloat] instead.
 *
 * //TODO: Add sample
 *
 * @param dragDirection direction in which drag should be happening
 * @param startValue value to set as initial for draggable/animating value in this component
 * @param minValue lower bound for draggable/animating value in this component
 * @param maxValue upper bound for draggable/animating value in this component
 * Either [DragDirection.Vertical] or [DragDirection.Horizontal]
 * @param flingConfig sets behavior of the fling after drag has ended.
 * Default is null, which means no drag will occur no matter the velocity
 */
@Composable
fun AnimatedDraggable(
    dragDirection: DragDirection,
    startValue: Float,
    minValue: Float = Float.MIN_VALUE,
    maxValue: Float = Float.MAX_VALUE,
    flingConfig: FlingConfig? = null,
    children: @Composable() (BaseAnimatedValue<Float>) -> Unit
) {
    val animFloat = (+animatedFloat(startValue)).apply {
        setBounds(minValue, maxValue)
    }
    DragGestureDetector(
        canDrag = { direction ->
            dragDirection.isDraggableInDirection(direction, minValue, animFloat.value, maxValue)
        },
        dragObserver = object : DragObserver {

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                val projected = dragDirection.project(dragDistance)
                val newValue = (animFloat.value + projected).coerceIn(minValue, maxValue)
                val consumed = newValue - animFloat.value
                animFloat.snapTo(newValue)
                val fractionConsumed = if (projected == 0f) 0f else consumed / projected
                return PxPosition(
                    dragDirection.xProjection(dragDistance.x).px * fractionConsumed,
                    dragDirection.yProjection(dragDistance.y).px * fractionConsumed
                )
            }

            override fun onStop(velocity: PxPosition) {
                val projected = dragDirection.project(velocity)
                flingConfig?.fling(animFloat, projected)
            }
        }
    ) {
        children(animFloat)
    }
}

