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

import androidx.ui.engine.geometry.Offset

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
    val position: Offset? = null,
    val down: Boolean = false
)

/**
 * Describes what aspects of, and how much of, a change has been consumed.
 */
data class ConsumedData(
    val positionChange: Offset = Offset.zero,
    val downChange: Boolean = false
)
/**
 * The enumeration of passes where [PointerInputChange] traverses up and down the UI tree.
 */
enum class PointerEventPass {
    InitialDown, PreUp, PreDown, PostUp, PostDown
}

typealias PointerInputHandler = (PointerInputChange, PointerEventPass) -> PointerInputChange

// PointerInputChange extension functions

// Change querying functions

fun PointerInputChange.changedToDown() = !consumed.downChange && !previous.down && current.down

fun PointerInputChange.changedToDownIgnoreConsumed() = !previous.down && current.down

fun PointerInputChange.changedToUp() = !consumed.downChange && previous.down && !current.down

fun PointerInputChange.changedToUpIgnoreConsumed() = previous.down && !current.down

fun PointerInputChange.positionChange() = this.positionChangeInternal(false)

fun PointerInputChange.positionChangeIgnoreConsumed() = this.positionChangeInternal(true)

fun PointerInputChange.positionChanged() = this.positionChangeInternal(false) != Offset.zero

fun PointerInputChange.positionChangedIgnoreConsumed() =
    this.positionChangeInternal(true) != Offset.zero

private fun PointerInputChange.positionChangeInternal(ignoreConsumed: Boolean = false): Offset {
    val previousPosition = previous.position
    val currentPosition = current.position

    val offset =
        if (previousPosition == null || currentPosition == null) {
            Offset(0f, 0f)
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
    consumed.positionChange.dx != 0f || consumed.positionChange.dy != 0f

// Consume functions

fun PointerInputChange.consumeDownChange() =
    copy(consumed = this.consumed.copy(downChange = true))

fun PointerInputChange.consumePositionChange(
    consumedDx: Float,
    consumedDy: Float
): PointerInputChange {
    val newConsumedDx = consumedDx + consumed.positionChange.dx
    val newConsumedDy = consumedDy + consumed.positionChange.dy
    // TODO(shepshapard): Handle case where consumption would make the consumption total to be
    // less than the total change.
    return copy(
        consumed = this.consumed.copy(
            positionChange = Offset(
                newConsumedDx,
                newConsumedDy
            )
        )
    )
}

// Offset modification functions

fun PointerInputChange.subtractOffset(offset: Offset): PointerInputChange {
    return if (offset == Offset.zero) {
        this
    } else {
        this.copy(
            current = current.copy(position = this.current.position?.minus(offset)),
            previous = previous.copy(position = this.previous.position?.minus(offset))
        )
    }
}