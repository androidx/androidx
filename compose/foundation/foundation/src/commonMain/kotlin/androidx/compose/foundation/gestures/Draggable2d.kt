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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope

/**
 * State of Draggable2d. Allows for granular control of how deltas are consumed by the user as well
 * as to write custom drag methods using [drag] suspend function.
 */
internal interface Draggable2dState {
    /**
     * Call this function to take control of drag logic.
     *
     * All actions that change the logical drag position must be performed within a [drag]
     * block (even if they don't call any other methods on this object) in order to guarantee
     * that mutual exclusion is enforced.
     *
     * If [drag] is called from elsewhere with the [dragPriority] higher or equal to ongoing
     * drag, ongoing drag will be canceled.
     *
     * @param dragPriority of the drag operation
     * @param block to perform drag in
     */
    suspend fun drag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend Drag2dScope.() -> Unit
    )

    /**
     * Dispatch drag delta in pixels avoiding all drag related priority mechanisms.
     *
     * **Note:** unlike [drag], dispatching any delta with this method will bypass scrolling of
     * any priority. This method will also ignore `reverseDirection` and other parameters set in
     * draggable2d.
     *
     * This method is used internally for low level operations, allowing implementers of
     * [Draggable2dState] influence the consumption as suits them.
     * Manually dispatching delta via this method will likely result in a bad user experience,
     * you must prefer [drag] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested drag process
     */
    fun dispatchRawDelta(delta: Offset)
}

/**
 * Scope used for suspending drag blocks
 */
internal interface Drag2dScope {
    /**
     * Attempts to drag by [pixels] px.
     */
    fun dragBy(pixels: Offset)
}

/**
 * Default implementation of [Draggable2dState] interface that allows to pass a simple action that
 * will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a draggable2d modifier. When constructing this
 * [Draggable2dState], you must provide a [onDelta] lambda, which will be invoked whenever
 * drag happens (by gesture input or a custom [Draggable2dState.drag] call) with the delta in
 * pixels.
 *
 * If you are creating [Draggable2dState] in composition, consider using [rememberDraggable2dState].
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
@Suppress("PrimitiveInLambda")
internal fun Draggable2dState(onDelta: (Offset) -> Unit): Draggable2dState =
    DefaultDraggable2dState(onDelta)

/**
 * Create and remember default implementation of [Draggable2dState] interface that allows to pass a
 * simple action that will be invoked when the drag occurs.
 *
 * This is the simplest way to set up a [draggable] modifier. When constructing this
 * [Draggable2dState], you must provide a [onDelta] lambda, which will be invoked whenever
 * drag happens (by gesture input or a custom [Draggable2dState.drag] call) with the delta in
 * pixels.
 *
 * @param onDelta callback invoked when drag occurs. The callback receives the delta in pixels.
 */
@Suppress("PrimitiveInLambda")
@Composable
internal fun rememberDraggable2dState(onDelta: (Offset) -> Unit): Draggable2dState {
    val onDeltaState = rememberUpdatedState(onDelta)
    return remember { Draggable2dState { onDeltaState.value.invoke(it) } }
}

@Suppress("PrimitiveInLambda")
private class DefaultDraggable2dState(val onDelta: (Offset) -> Unit) : Draggable2dState {
    private val drag2dScope: Drag2dScope = object : Drag2dScope {
        override fun dragBy(pixels: Offset) = onDelta(pixels)
    }

    private val drag2dMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend Drag2dScope.() -> Unit
    ): Unit = coroutineScope {
        drag2dMutex.mutateWith(drag2dScope, dragPriority, block)
    }

    override fun dispatchRawDelta(delta: Offset) {
        return onDelta(delta)
    }
}
