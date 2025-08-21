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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.DragEvent.DragDelta
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.coroutineScope

/**
 * State of Draggable2D. Allows for granular control of how deltas are consumed by the user as well
 * as to write custom drag methods using [drag] suspend function.
 */
interface Draggable2DState {
    /**
     * Call this function to take control of drag logic.
     *
     * All actions that change the logical drag position must be performed within a [drag] block
     * (even if they don't call any other methods on this object) in order to guarantee that mutual
     * exclusion is enforced.
     *
     * If [drag] is called from elsewhere with the [dragPriority] higher or equal to ongoing drag,
     * ongoing drag will be canceled.
     *
     * @param dragPriority of the drag operation
     * @param block to perform drag in
     */
    suspend fun drag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend Drag2DScope.() -> Unit,
    )

    /**
     * Dispatch drag delta in pixels avoiding all drag related priority mechanisms.
     *
     * **Note:** unlike [drag], dispatching any delta with this method will bypass dragging of any
     * priority. This method will also ignore `reverseDirection` and other parameters set in
     * draggable2D.
     *
     * This method is used internally for low level operations, allowing implementers of
     * [Draggable2DState] influence the consumption as suits them. Manually dispatching delta via
     * this method will likely result in a bad user experience, you must prefer [drag] method over
     * this one.
     *
     * @param delta amount of drag dispatched in the nested drag process
     */
    fun dispatchRawDelta(delta: Offset)
}

/** Scope used for suspending drag blocks */
interface Drag2DScope {
    /** Attempts to drag by [pixels] px. */
    fun dragBy(pixels: Offset)
}

/**
 * Default implementation of [Draggable2DState] interface that allows to pass a simple action that
 * will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a draggable2D modifier. When constructing this
 * [Draggable2DState], you must provide a [onDelta] lambda, which will be invoked whenever drag
 * happens (by gesture input or a custom [Draggable2DState.drag] call) with the delta in pixels.
 *
 * If you are creating [Draggable2DState] in composition, consider using [rememberDraggable2DState].
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
fun Draggable2DState(onDelta: (Offset) -> Unit): Draggable2DState = DefaultDraggable2DState(onDelta)

/**
 * Create and remember default implementation of [Draggable2DState] interface that allows to pass a
 * simple action that will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable2D] modifier. When constructing this
 * [Draggable2DState], you must provide a [onDelta] lambda, which will be invoked whenever drag
 * happens (by gesture input or a custom [Draggable2DState.drag] call) with the delta in pixels.
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
@Composable
fun rememberDraggable2DState(onDelta: (Offset) -> Unit): Draggable2DState {
    val onDeltaState = rememberUpdatedState(onDelta)
    return remember { Draggable2DState { onDeltaState.value.invoke(it) } }
}

/**
 * Configure touch dragging for the UI element in both orientations. The drag distance reported to
 * [Draggable2DState], allowing users to react to the drag delta and update their state.
 *
 * The common common usecase for this component is when you need to be able to drag something inside
 * the component on the screen and represent this state via one float value
 *
 * If you are implementing dragging in a single orientation, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.Draggable2DSample
 * @param state [Draggable2DState] state of the draggable2D. Defines how drag events will be
 *   interpreted by the user land logic.
 * @param enabled whether or not drag is enabled
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [DragInteraction.Start] when this draggable is being dragged.
 * @param startDragImmediately when set to true, draggable2D will start dragging immediately and
 *   prevent other gesture detectors from reacting to "down" events (in order to block composed
 *   press-based gestures). This is intended to allow end users to "catch" an animating widget by
 *   pressing on it. It's useful to set it when value you're dragging is settling / animating.
 * @param onDragStarted callback that will be invoked when drag is about to start at the starting
 *   position, allowing user to perform preparation for drag.
 * @param onDragStopped callback that will be invoked when drag is finished, allowing the user to
 *   react on velocity and process it.
 * @param reverseDirection reverse the direction of the dragging, so top to bottom dragging will
 *   behave like bottom to top and left to right will behave like right to left.
 */
@Stable
fun Modifier.draggable2D(
    state: Draggable2DState,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    startDragImmediately: Boolean = false,
    onDragStarted: (startedPosition: Offset) -> Unit = NoOpOnDragStart,
    onDragStopped: (velocity: Velocity) -> Unit = NoOpOnDragStop,
    reverseDirection: Boolean = false,
): Modifier =
    this then
        Draggable2DElement(
            state = state,
            enabled = enabled,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = onDragStarted,
            onDragStopped = onDragStopped,
            reverseDirection = reverseDirection,
        )

internal class Draggable2DElement(
    private val state: Draggable2DState,
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource?,
    private val startDragImmediately: Boolean,
    private val onDragStarted: (startedPosition: Offset) -> Unit,
    private val onDragStopped: (velocity: Velocity) -> Unit,
    private val reverseDirection: Boolean,
) : ModifierNodeElement<Draggable2DNode>() {
    override fun create(): Draggable2DNode =
        Draggable2DNode(
            state,
            CanDrag,
            enabled,
            interactionSource,
            startDragImmediately,
            reverseDirection,
            onDragStarted,
            onDragStopped,
        )

    override fun update(node: Draggable2DNode) {
        node.update(
            state,
            CanDrag,
            enabled,
            interactionSource,
            startDragImmediately,
            reverseDirection,
            onDragStarted,
            onDragStopped,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as Draggable2DElement

        if (state != other.state) return false
        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (startDragImmediately != other.startDragImmediately) return false
        if (onDragStarted !== other.onDragStarted) return false
        if (onDragStopped !== other.onDragStopped) return false
        if (reverseDirection != other.reverseDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        result = 31 * result + startDragImmediately.hashCode()
        result = 31 * result + onDragStarted.hashCode()
        result = 31 * result + onDragStopped.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "draggable2D"
        properties["enabled"] = enabled
        properties["interactionSource"] = interactionSource
        properties["startDragImmediately"] = startDragImmediately
        properties["onDragStarted"] = onDragStarted
        properties["onDragStopped"] = onDragStopped
        properties["reverseDirection"] = reverseDirection
        properties["state"] = state
    }

    companion object {
        val CanDrag: (PointerInputChange) -> Boolean = { true }
    }
}

internal class Draggable2DNode(
    private var state: Draggable2DState,
    canDrag: (PointerInputChange) -> Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    private var startDragImmediately: Boolean,
    private var reverseDirection: Boolean,
    private var onDragStarted: (startedPosition: Offset) -> Unit,
    private var onDragStopped: (velocity: Velocity) -> Unit,
) :
    DragGestureNode(
        canDrag = canDrag,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = null,
    ) {

    override suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit) {
        state.drag(MutatePriority.UserInput) {
            forEachDelta { dragDelta -> dragBy(dragDelta.delta.reverseIfNeeded()) }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {
        onDragStarted.invoke(startedPosition)
    }

    override fun onDragStopped(event: DragEvent.DragStopped) {
        onDragStopped.invoke(event.velocity)
    }

    override fun startDragImmediately(): Boolean = startDragImmediately

    fun update(
        state: Draggable2DState,
        canDrag: (PointerInputChange) -> Boolean,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
        startDragImmediately: Boolean,
        reverseDirection: Boolean,
        onDragStarted: (startedPosition: Offset) -> Unit,
        onDragStopped: (velocity: Velocity) -> Unit,
    ) {
        var resetPointerInputHandling = false
        if (this.state != state) {
            this.state = state
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }

        this.onDragStarted = onDragStarted
        this.onDragStopped = onDragStopped

        this.startDragImmediately = startDragImmediately

        update(
            canDrag = canDrag,
            enabled = enabled,
            interactionSource = interactionSource,
            orientationLock = null,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )
    }

    @Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")
    private inline fun Offset.reverseIfNeeded() = if (reverseDirection) -this else this
}

private class DefaultDraggable2DState(val onDelta: (Offset) -> Unit) : Draggable2DState {
    private val drag2DScope: Drag2DScope =
        object : Drag2DScope {
            override fun dragBy(pixels: Offset) = onDelta(pixels)
        }

    private val drag2DMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend Drag2DScope.() -> Unit,
    ): Unit = coroutineScope { drag2DMutex.mutateWith(drag2DScope, dragPriority, block) }

    override fun dispatchRawDelta(delta: Offset) {
        return onDelta(delta)
    }
}

private val NoOpOnDragStart: (startedPosition: Offset) -> Unit = {}
private val NoOpOnDragStop: (velocity: Velocity) -> Unit = {}
