/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures2

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerEvent

data class PointerEvent2(
    val id: Int,
    val current: PointerData,
    val previous: PointerData,
    val consumed: ConsumedData
)

@Suppress("FunctionName")
fun PointerEvent2(pointerEvent: PointerEvent) =
    PointerEvent2(pointerEvent.pointer, PointerData(pointerEvent), PointerData(), ConsumedData())

// Change querying functions

fun PointerEvent2.changedToUp(ignoreConsumed: Boolean = false) =
    (ignoreConsumed || !consumed.downChange) && previous.down && !current.down

fun PointerEvent2.changedToDown(ignoreConsumed: Boolean = false) =
    (ignoreConsumed || !consumed.downChange) && !previous.down && current.down

fun PointerEvent2.positionChange(ignoreConsumed: Boolean = false): Offset {
    val previousPosition = previous.position
    val currentPosition = current.position

    val offset =
        if (previousPosition == null || currentPosition == null) {
            Offset(0.0, 0.0)
        } else {
            previousPosition - currentPosition
        }

    return if (!ignoreConsumed) {
        offset - consumed.positionChange
    } else {
        offset
    }
}

fun PointerEvent2.positionChanged(ignoreConsumed: Boolean = false): Boolean {
    return positionChange(ignoreConsumed) != Offset.zero
}

// Consumption querying functions

fun PointerEvent2.anyPositionChangeConsumed() =
    consumed.positionChange.dx != 0.0 || consumed.positionChange.dy != 0.0

// Consume functions

fun PointerEvent2.consumeDownChange() =
    copy(consumed = this.consumed.copy(downChange = true))

fun PointerEvent2.consumePositionChange(consumedDx: Double, consumedDy: Double): PointerEvent2 {
    val newConsumedDx = consumedDx + consumed.positionChange.dx
    val newConsumedDy = consumedDy + consumed.positionChange.dy
    // TODO(shepshapard): Handle error if over consumed
    return copy(
        consumed = this.consumed.copy(
            positionChange = Offset(
                newConsumedDx,
                newConsumedDy
            )
        )
    )
}

// Should only be called by GestureBinder2

internal fun PointerEvent2.update(pointerEvent: PointerEvent): PointerEvent2 =
    copy(current = PointerData(pointerEvent), previous = current, consumed = ConsumedData())

data class PointerData(
    val timeStamp: Duration? = null,
    val position: Offset? = null,
    val down: Boolean = false,
    val cancelled: Boolean = false
)

@Suppress("FunctionName")
private fun PointerData(pointerEvent: PointerEvent): PointerData {
    return PointerData(
        pointerEvent.timeStamp,
        pointerEvent.position,
        pointerEvent.down,
        pointerEvent is PointerCancelEvent
    )
}

data class ConsumedData(
    val positionChange: Offset = Offset(0.0, 0.0),
    val downChange: Boolean = false
)