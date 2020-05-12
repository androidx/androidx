/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import androidx.ui.core.CustomEvent
import androidx.ui.core.CustomEventDispatcher
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.round

/**
 * A [Modifier.Element] that can interact with pointer input.
 */
interface PointerInputModifier : Modifier.Element {
    val pointerInputFilter: PointerInputFilter
}

/**
 * A PointerInputFilter represents a single entity that receives [PointerInputChange]s),
 * interprets them, and consumes the aspects of the changes that it is react to such that other
 * PointerInputFilters don't also react to them.
 */
abstract class PointerInputFilter {

    /**
     * Invoked when pointers that previously hit this [PointerInputFilter] have changed.
     *
     * @param changes The list of [PointerInputChange]s with positions relative to this
     * [PointerInputFilter].
     * @param pass The [PointerEventPass] in which this function is being called.
     * @param bounds The width and height associated with this [PointerInputFilter].
     * @return The list of [PointerInputChange]s after any aspect of the changes have been consumed.
     *
     * @see PointerInputChange
     * @see PointerEventPass
     */
    abstract fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange>

    /**
     * Invoked to notify the handler that no more calls to [PointerInputFilter] will be made, until
     * at least new pointers exist.  This can occur for a few reasons:
     * 1. Android dispatches ACTION_CANCEL to Compose.
     * 2. This [PointerInputFilter] is no longer associated with a LayoutNode.
     * 3. This [PointerInputFilter]'s associated LayoutNode is no longer in the composition tree.
     */
    abstract fun onCancel()

    /**
     * Invoked right after this [PointerInputFilter] is hit by a pointer during hit testing.
     *
     * @param customEventDispatcher The [CustomEventDispatcher] that can be used to dispatch
     * [CustomEvent] across the tree of hit [PointerInputFilter]s.
     *
     * @See CustomEventDispatcher
     */
    open fun onInit(customEventDispatcher: CustomEventDispatcher) {}

    /**
     * Invoked when a [CustomEvent] is dispatched by a [PointerInputFilter].
     *
     * Dispatch occurs over all passes of [PointerEventPass].
     *
     * @param customEvent The [CustomEvent] is the event being dispatched.
     * @param pass The [PointerEventPass] in which this function is being called.
     *
     * @see CustomEvent
     * @see PointerEventPass
     */
    open fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {}

    internal lateinit var layoutCoordinates: LayoutCoordinates

    internal val size: IntPxSize
        get() = layoutCoordinates.size
    internal val position: IntPxPosition
        get() = layoutCoordinates.localToGlobal(PxPosition.Origin).round()
    internal val isAttached: Boolean
        get() = layoutCoordinates.isAttached
}
