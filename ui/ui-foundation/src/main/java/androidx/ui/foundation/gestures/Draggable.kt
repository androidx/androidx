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
import androidx.compose.Composable
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.gesture.DragGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px

// TODO(b/145766300): Consider folding "isAnimating" into dragValue.
/**
 * Component that provides high-level drag functionality reflected in one value
 *
 * The common usecase for this component is when you need to be able to drag/scroll something
 * on the screen and represent it as one value via [AnimatedFloat].
 *
 * If you need to control the whole dragging flow,
 * consider using [DragGestureDetector] instead.
 *
 * @sample androidx.ui.foundation.samples.DraggableSample
 *
 * By using [AnimatedFloat] as dragValue you can achieve
 * fling behaviour by calling fling on it
 *
 * @sample androidx.ui.foundation.samples.AnchoredDraggableSample
 *
 * @param dragDirection direction in which drag should be happening
 * @param dragValue value holder for value that needs to be dragged
 * @param onDragValueChangeRequested callback to be invoked when drag happened and
 * change on dragValue is requested. The value should be updated synchronously
 * in order to provide smooth dragging experience
 * @param onDragStarted callback that will be invoked when drag has been started after touch slop
 * has been passed, with starting position provided
 * @param onDragStopped callback that will be invoked when drag stops, with velocity provided
 * @param enabled whether or not drag is enabled
 * @param isValueAnimating Set to true when dragValue is being animated. Setting to true will
 * inform this Draggable that it should start dragging and prevent other gesture detectors from
 * reacting to "down" events (in order to block composed press-based gestures).  This is intended to
 * allow end users to "catch" an animating widget by pressing on it.
 */
@Composable
fun Draggable(
    dragDirection: DragDirection,
    dragValue: AnimatedFloat,
    onDragValueChangeRequested: (Float) -> Unit,
    onDragStarted: (startedPosition: PxPosition) -> Unit = {},
    onDragStopped: (velocity: Float) -> Unit = {},
    enabled: Boolean = true,
    isValueAnimating: Boolean = false,
    children: @Composable() () -> Unit
) {
    val dragModifier = DragGestureDetector(
        dragObserver = object : DragObserver {

            override fun onStart(downPosition: PxPosition) {
                if (enabled) onDragStarted(downPosition)
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                if (!enabled) return PxPosition.Origin
                val oldValue = dragValue.value
                val projected = dragDirection.project(dragDistance)
                onDragValueChangeRequested(oldValue + projected)
                val consumed = dragValue.value - oldValue
                val fractionConsumed = if (projected == 0f) 0f else consumed / projected
                return PxPosition(
                    dragDirection.xProjection(dragDistance.x).px * fractionConsumed,
                    dragDirection.yProjection(dragDistance.y).px * fractionConsumed
                )
            }

            override fun onStop(velocity: PxPosition) {
                if (enabled) onDragStopped(dragDirection.project(velocity))
            }
        },
        canDrag = { direction ->
            enabled && dragDirection
                .isDraggableInDirection(
                    direction,
                    dragValue.value
                )
        },
        startDragImmediately = isValueAnimating
    )

    // TODO(b/150706555): This layout is temporary and should be removed once Semantics
    //  is implemented with modifiers.
    @Suppress("DEPRECATION")
    PassThroughLayout(dragModifier, children)
}