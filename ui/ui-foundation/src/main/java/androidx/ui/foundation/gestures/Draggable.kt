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
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.composed
import androidx.ui.core.gesture.ScrollCallback
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.core.gesture.scrollGestureFilter
import androidx.ui.core.gesture.scrollorientationlocking.Orientation
import androidx.ui.foundation.Interaction
import androidx.ui.foundation.InteractionState
import androidx.ui.geometry.Offset
import kotlin.math.sign

/**
 * Configure touch dragging for the UI element in a single [DragDirection]. The drag distance is
 * reported to [onDragDeltaConsumptionRequested] as a single [Float] value in pixels.
 *
 * The common usecase for this component is when you need to be able to drag something
 * inside the component on the screen and represent this state via one float value
 *
 * If you need to control the whole dragging flow, consider using [dragGestureFilter] instead.
 *
 * If you are implementing scroll/fling behavior, consider using [scrollable].
 *
 * @sample androidx.ui.foundation.samples.DraggableSample
 *
 * [AnimatedFloat] offers a standard implementation of flinging behavior:
 *
 * @sample androidx.ui.foundation.samples.AnchoredDraggableSample
 *
 * @param dragDirection direction in which drag should be happening
 * @param onDragStarted callback that will be invoked when drag has been started after touch slop
 * has been passed, with starting position provided
 * @param onDragStopped callback that will be invoked when drag stops, with velocity provided
 * @param enabled whether or not drag is enabled
 * @param interactionState [InteractionState] that will be updated when this draggable is
 * being dragged, using [Interaction.Dragged].
 * @param startDragImmediately when set to true, draggable will start dragging immediately and
 * prevent other gesture detectors from reacting to "down" events (in order to block composed
 * press-based gestures).  This is intended to allow end users to "catch" an animating widget by
 * pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragDeltaConsumptionRequested callback to be invoked when drag occurs. Users must
 * update their state in this lambda and return amount of delta consumed
 */
fun Modifier.draggable(
    dragDirection: DragDirection,
    onDragStarted: (startedPosition: Offset) -> Unit = {},
    onDragStopped: (velocity: Float) -> Unit = {},
    enabled: Boolean = true,
    interactionState: InteractionState? = null,
    startDragImmediately: Boolean = false,
    onDragDeltaConsumptionRequested: (Float) -> Float
): Modifier = composed {
    val dragState = remember { DraggableState() }
    onDispose {
        interactionState?.removeInteraction(Interaction.Dragged)
    }

    val orientation =
        when (dragDirection) {
            DragDirection.Horizontal -> Orientation.Horizontal
            DragDirection.Vertical -> Orientation.Vertical
            DragDirection.ReversedHorizontal -> Orientation.Horizontal
        }

    val sign =
        when (dragDirection) {
            DragDirection.Horizontal -> 1
            DragDirection.Vertical -> 1
            DragDirection.ReversedHorizontal -> -1
        }

    scrollGestureFilter(
        scrollCallback = object : ScrollCallback {

            override fun onStart(downPosition: Offset) {
                if (enabled) {
                    interactionState?.addInteraction(Interaction.Dragged)
                    onDragStarted(downPosition)
                }
            }

            override fun onScroll(scrollDistance: Float): Float {
                if (!enabled) return scrollDistance
                val consumed = onDragDeltaConsumptionRequested(scrollDistance * sign)
                dragState.value = dragState.value + consumed
                // TODO (malkov): temporary negate reversed direction with sign
                //  remove when b/159618405 is fixed.
                return consumed * sign
            }

            override fun onCancel() {
                if (enabled) {
                    interactionState?.removeInteraction(Interaction.Dragged)
                    onDragStopped(0f)
                }
            }

            override fun onStop(velocity: Float) {
                if (enabled) {
                    interactionState?.removeInteraction(Interaction.Dragged)
                    onDragStopped(velocity * sign)
                }
            }
        },
        orientation = orientation,
        // TODO(shepshapard): canDrag is intended to prevent something from starting a scroll in
        //  a direction where scrolling is already at it's max.  This code is not yet doing that.
        canDrag = { enabled },
        startDragImmediately = startDragImmediately
    )
}

private class DraggableState {
    var value: Float = 0f
}