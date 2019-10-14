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

/**
 * Describes a change that has occurred for a particular pointer, as well as how much of the change
 * has been consumed (meaning, used by a node in the UI)
 */
data class PointerInputChange(
    val id: Int,
    val current: PointerInputData,
    val previous: PointerInputData,
    val consumed: ConsumedData
)

/**
 * Data associated with a pointer
 */
data class PointerInputData(
    val timestamp: Timestamp? = null,
    val position: PxPosition? = null,
    val down: Boolean = false
)

/**
 * Describes what aspects of, and how much of, a change has been consumed.
 */
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

typealias PointerInputHandler =
            (List<PointerInputChange>, PointerEventPass) -> List<PointerInputChange>

// PointerInputChange extension functions

// Change querying functions

fun PointerInputChange.changedToDown() = !consumed.downChange && !previous.down && current.down

fun PointerInputChange.changedToDownIgnoreConsumed() = !previous.down && current.down

fun PointerInputChange.changedToUp() = !consumed.downChange && previous.down && !current.down

fun PointerInputChange.changedToUpIgnoreConsumed() = previous.down && !current.down

fun PointerInputChange.positionChange() = this.positionChangeInternal(false)

fun PointerInputChange.positionChangeIgnoreConsumed() = this.positionChangeInternal(true)

fun PointerInputChange.positionChanged() = this.positionChangeInternal(false) != PxPosition.Origin

fun PointerInputChange.positionChangedIgnoreConsumed() =
    this.positionChangeInternal(true) != PxPosition.Origin

private fun PointerInputChange.positionChangeInternal(ignoreConsumed: Boolean = false): PxPosition {
    val previousPosition = previous.position
    val currentPosition = current.position

    val offset =
        if (previousPosition == null || currentPosition == null) {
            PxPosition(0.px, 0.px)
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

fun PointerInputChange.anyPositionChangeConsumed() =
    consumed.positionChange.x.value != 0f || consumed.positionChange.y.value != 0f

// Consume functions

fun PointerInputChange.consumeDownChange() =
    copy(consumed = this.consumed.copy(downChange = true))

fun PointerInputChange.consumePositionChange(
    consumedDx: Px,
    consumedDy: Px
): PointerInputChange {
    val newConsumedDx = consumedDx + consumed.positionChange.x
    val newConsumedDy = consumedDy + consumed.positionChange.y
    // TODO(shepshapard): Handle case where consumption would make the consumption total to be
    // less than the total change.
    return copy(
        consumed = this.consumed.copy(
            positionChange = PxPosition(
                newConsumedDx,
                newConsumedDy
            )
        )
    )
}