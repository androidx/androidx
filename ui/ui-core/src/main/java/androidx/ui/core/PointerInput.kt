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

package androidx.ui.core

import androidx.compose.Immutable
import androidx.compose.Stable
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.Uptime

/**
 * Describes a change that has occurred for a particular pointer, as well as how much of the change
 * has been consumed (meaning, used by a node in the UI).
 *
 * The [current] data always represents the position of the pointer relative to the element that
 * this [PointerInputChange] is being dispatched to.
 *
 * The [previous] data, however, represents the position of the pointer offset to the current
 * position of the pointer relative to the screen.
 *
 * This means that [current] and [previous] can always be used to understand how much a pointer
 * has moved relative to an element, even if that element is moving along with the changes to the
 * pointer.  For example, if a pointer touches a 1x1 pixel box in the middle, [current] will
 * report a position of (0, 0) when dispatched to it.  If the next event moves x position 5
 * pixels, [current] will report (5, 0) and [previous] will report (0, 0).  If the box moves all 5
 * pixels, and the next event represents the pointer moving along the x axis for 5 more pixels,
 * [current] will again report (5, 0) and [previous] will report (0, 0).
 *
 * @param id The unique id of the pointer associated with this [PointerInputChange].
 * @param current The [PointerInputData] that represents the current state of this pointer.
 * @param previous The [PointerInputData] that represents the previous state of this pointer.
 * @param consumed Which aspects of this change have been consumed.
 */
@Immutable
data class PointerInputChange(
    val id: PointerId,
    val current: PointerInputData,
    val previous: PointerInputData,
    val consumed: ConsumedData
)

/**
 * An ID for a given pointer.
 *
 * @param value The actual value of the id.
 */
inline class PointerId(val value: Long)

// TODO(shepshapard): Uptime will be an Inline Class, so it should not be nullable.
/**
 * Data associated with a pointer.
 *
 * @param uptime The time associated with this particular [PointerInputData]
 * @param position The position of the pointer at [uptime] relative to element that
 * the owning [PointerInputChange] is being dispatched to.
 * @param down True if the at [uptime] the pointer was contacting the screen.
 */
@Immutable
data class PointerInputData(
    @Stable
    val uptime: Uptime? = null,
    @Stable
    val position: PxPosition? = null,
    @Stable
    val down: Boolean = false
)

/**
 * Describes what aspects of, and how much of, a change has been consumed.
 *
 * @param positionChange The amount of change to the position that has been consumed.
 * @param downChange True if a change to down or up has been consumed.
 */
@Immutable
data class ConsumedData(
    val positionChange: PxPosition = PxPosition.Origin,
    val downChange: Boolean = false
)

/**
 * The enumeration of passes where [PointerInputChange] traverses up and down the UI tree.
 */
enum class PointerEventPass {
    InitialDown, PreUp, PreDown, PostUp, PostDown
}

/**
 * A function used to react to and modify [PointerInputChange]s.
 */
typealias PointerInputHandler =
            (List<PointerInputChange>, PointerEventPass, IntPxSize) -> List<PointerInputChange>

// This CustomEvent interface primarily exists exists to provide a base type other than Any.  If it
// were Any, then Unit would be sufficient, which is not a valid type, or value, to send as a
// custom event.
/**
 * The base type for all custom events.
 */
interface CustomEvent

// TODO(b/149030989): Provide sample for usage of CustomEventDispatcher.
/**
 * Defines the interface that is used to dispatch CustomEvents to pointer input nodes across the
 * compose tree.
 */
interface CustomEventDispatcher {

    /**
     * Dispatches the [event] to all other pointer input nodes that share associated [PointerId]s
     * with the pointer input node doing the dispatching.
     *
     * @param event The [CustomEvent] to dispatch.
     */
    // TODO(shepshapard): Come back and consider any issues with: This effectively allows
    //  individual pointer input nodes to gain a reference back to the internal HitPathTracker.
    //  But I think that is ok since pointer input nodes should  never be able to live for longer
    //  than the HitPathTracker that would be responsible for tracking them.
    fun dispatchCustomEvent(event: CustomEvent)

    /**
     * Arranges to retain the hit paths associated with the provided [pointerIds] such that if
     * they are requested to be removed for any reason, they are retained.
     *
     * For example, this is useful when a pointer input filter wants to be able to send future
     * custom messages to a another after the pointer has actually be released from the screen
     * (such as in the case where a Double Tap gesture detector may want to delay a Single Tap
     * gesture detector from firing but later may allow it to do so even after the pointer
     * associated with the Single Tap Gesture detector no longer exists.
     */
    fun retainHitPaths(pointerIds: Set<PointerId>)

    /**
     * Arranges to release any hit paths associated with the provided [pointerIds] such that if
     * they will be requested to be removed in the future, they will be removed upon request.
     *
     * If they were already requested to be removed while they were retained, they will be
     * removed immediately upon release.
     */
    fun releaseHitPaths(pointerIds: Set<PointerId>)
}

// PointerInputChange extension functions

// Change querying functions

/**
 * True if this [PointerInputChange] represents a pointer coming in contact with the screen and
 * that change has not been consumed.
 */
fun PointerInputChange.changedToDown() = !consumed.downChange && !previous.down && current.down

/**
 * True if this [PointerInputChange] represents a pointer coming in contact with the screen, whether
 * or not that change has been consumed.
 */
fun PointerInputChange.changedToDownIgnoreConsumed() = !previous.down && current.down

/**
 * True if this [PointerInputChange] represents a pointer breaking contact with the screen and
 * that change has not been consumed.
 */
fun PointerInputChange.changedToUp() = !consumed.downChange && previous.down && !current.down

/**
 * True if this [PointerInputChange] represents a pointer breaking contact with the screen, whether
 * or not that change has been consumed.
 */
fun PointerInputChange.changedToUpIgnoreConsumed() = previous.down && !current.down

/**
 * True if this [PointerInputChange] represents a pointer moving on the screen and some of that
 * movement has not been consumed.
 */
fun PointerInputChange.positionChanged() = this.positionChangeInternal(false) != PxPosition.Origin

/**
 * True if this [PointerInputChange] represents a pointer moving on the screen ignoring how much
 * of that movement may have been consumed.
 */
fun PointerInputChange.positionChangedIgnoreConsumed() =
    this.positionChangeInternal(true) != PxPosition.Origin

/**
 * The distance that the pointer has moved on the screen minus any distance that has been consumed.
 */
fun PointerInputChange.positionChange() = this.positionChangeInternal(false)

/**
 * The distance that the pointer has moved on the screen, ignoring any distance that may have been
 * consumed.
 */
fun PointerInputChange.positionChangeIgnoreConsumed() = this.positionChangeInternal(true)

private fun PointerInputChange.positionChangeInternal(ignoreConsumed: Boolean = false): PxPosition {
    val previousPosition = previous.position
    val currentPosition = current.position

    val offset =
        if (previousPosition == null || currentPosition == null) {
            PxPosition(0.0f, 0.0f)
        } else {
            currentPosition - previousPosition
        }

    return if (!ignoreConsumed) {
        offset - consumed.positionChange
    } else {
        offset
    }
}

// Consumption querying functions

/**
 * True if any of this [PointerInputChange]'s movement has been consumed.
 */
fun PointerInputChange.anyPositionChangeConsumed() =
    consumed.positionChange.x != 0f || consumed.positionChange.y != 0f

/**
 * True if any aspect of this [PointerInputChange] has been consumed.
 */
fun PointerInputChange.anyChangeConsumed() = anyPositionChangeConsumed() || consumed.downChange

// Consume functions

/**
 * Consume the up or down change of this [PointerInputChange] if there is an up or down change to
 * consume.
 *
 * Note: This function creates a modified copy of this [PointerInputChange].
 */
fun PointerInputChange.consumeDownChange() =
    if (current.down != previous.down) {
        copy(consumed = consumed.copy(downChange = true))
    } else {
        this
    }

/**
 * Consumes some portion of the position change of this [PointerInputChange].
 *
 * Note: This function creates a modified copy of this [PointerInputChange]
 *
 * @param consumedDx The amount of position change on the x axis to consume.
 * @param consumedDy The amount of position change on the y axis to consume.
 */
fun PointerInputChange.consumePositionChange(
    consumedDx: Float,
    consumedDy: Float
): PointerInputChange {
    val newConsumedDx = consumedDx + consumed.positionChange.x
    val newConsumedDy = consumedDy + consumed.positionChange.y
    // TODO(shepshapard): Handle case where consumption would make the consumption total to be
    //  less than the total change.
    return copy(
        consumed = this.consumed.copy(
            positionChange = PxPosition(
                newConsumedDx,
                newConsumedDy
            )
        )
    )
}

/**
 * Consumes all changes associated with the [PointerInputChange]
 *
 * Note: This function creates a modified copy of this [PointerInputChange]
 */
fun PointerInputChange.consumeAllChanges(): PointerInputChange {
    val remainingPositionChange = this.positionChange()
    return this
        .consumeDownChange()
        .consumePositionChange(remainingPositionChange.x, remainingPositionChange.y)
}