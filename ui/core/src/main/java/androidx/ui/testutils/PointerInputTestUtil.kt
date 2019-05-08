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

package androidx.ui.testutils

import androidx.ui.core.ConsumedData
import androidx.ui.core.Duration
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.ui.core.PxPosition
import androidx.ui.core.Timestamp
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px

fun down(
    id: Int = 0,
    timestamp: Timestamp = 0L.millisecondsToTimestamp(),
    x: Float = 0f,
    y: Float = 0f
): PointerInputChange =
    PointerInputChange(
        id,
        PointerInputData(timestamp, PxPosition(x.px, y.px), true),
        PointerInputData(null, null, false),
        ConsumedData(PxPosition.Origin, false)
    )

fun PointerInputChange.moveTo(timestamp: Timestamp, x: Float = 0f, y: Float = 0f) =
    copy(
        previous = current,
        current = PointerInputData(timestamp, PxPosition(x.px, y.px), true),
        consumed = ConsumedData()
    )

fun PointerInputChange.moveBy(duration: Duration, dx: Float = 0f, dy: Float = 0f) =
    copy(
        previous = current,
        current = PointerInputData(
            current.timestamp!! + duration,
            PxPosition(current.position!!.x + dx.px, current.position.y + dy.px),
            true
        ),
        consumed = ConsumedData()
    )

fun PointerInputChange.up(timestamp: Timestamp) =
    copy(
        previous = current,
        current = PointerInputData(timestamp, null, false),
        consumed = ConsumedData()
    )

fun PointerInputChange.consume(dx: Float = 0f, dy: Float = 0f, downChange: Boolean = false) =
    copy(
        consumed = consumed.copy(
            positionChange = PxPosition(
                consumed.positionChange.x + dx.px,
                consumed.positionChange.y + dy.px
            ), downChange = consumed.downChange || downChange
        )
    )

fun PointerInputHandler.invokeOverPasses(
    pointerInputChanges: List<PointerInputChange>,
    vararg pointerEventPasses: PointerEventPass
): List<PointerInputChange> {
    var localPointerInputChanges = pointerInputChanges
    pointerEventPasses.forEach {
        localPointerInputChanges = this.invoke(localPointerInputChanges, it)
    }
    return localPointerInputChanges
}

fun PointerInputHandler.invokeOverAllPasses(
    pointerInputChanges: List<PointerInputChange>
) = invokeOverPasses(
    pointerInputChanges,
    PointerEventPass.InitialDown,
    PointerEventPass.PreUp,
    PointerEventPass.PreDown,
    PointerEventPass.PostUp,
    PointerEventPass.PostDown
)
