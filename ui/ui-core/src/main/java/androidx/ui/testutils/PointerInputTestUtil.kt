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
import androidx.ui.core.CustomEvent
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.ui.unit.Duration
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.Uptime
import androidx.ui.unit.ipx

// TODO(shepshapard): Document.

fun down(
    id: Long,
    duration: Duration = Duration.Zero,
    x: Float = 0f,
    y: Float = 0f
): PointerInputChange =
    PointerInputChange(
        PointerId(id),
        PointerInputData(Uptime.Boot + duration, PxPosition(x, y), true),
        PointerInputData(null, null, false),
        ConsumedData(PxPosition.Origin, false)
    )

fun PointerInputChange.moveTo(duration: Duration, x: Float = 0f, y: Float = 0f) =
    copy(
        previous = current,
        current = PointerInputData(Uptime.Boot + duration, PxPosition(x, y), true),
        consumed = ConsumedData()
    )

fun PointerInputChange.moveBy(duration: Duration, dx: Float = 0f, dy: Float = 0f) =
    copy(
        previous = current,
        current = PointerInputData(
            current.uptime!! + duration,
            PxPosition(current.position!!.x + dx, current.position.y + dy),
            true
        ),
        consumed = ConsumedData()
    )

fun PointerInputChange.up(duration: Duration) =
    copy(
        previous = current,
        current = PointerInputData(Uptime.Boot + duration, null, false),
        consumed = ConsumedData()
    )

fun PointerInputChange.consume(dx: Float = 0f, dy: Float = 0f, downChange: Boolean = false) =
    copy(
        consumed = consumed.copy(
            positionChange = PxPosition(
                consumed.positionChange.x + dx,
                consumed.positionChange.y + dy
            ), downChange = consumed.downChange || downChange
        )
    )

fun PointerInputHandler.invokeOverAllPasses(
    pointerInputChanges: PointerInputChange,
    size: IntPxSize = IntPxSize(Int.MAX_VALUE.ipx, Int.MAX_VALUE.ipx)
) = invokeOverPasses(
    listOf(pointerInputChanges),
    listOf(
        PointerEventPass.InitialDown,
        PointerEventPass.PreUp,
        PointerEventPass.PreDown,
        PointerEventPass.PostUp,
        PointerEventPass.PostDown
    ),
    size = size
).first()

fun PointerInputHandler.invokeOverAllPasses(
    vararg pointerInputChanges: PointerInputChange,
    size: IntPxSize = IntPxSize(Int.MAX_VALUE.ipx, Int.MAX_VALUE.ipx)
) = invokeOverPasses(
    pointerInputChanges.toList(),
    listOf(
        PointerEventPass.InitialDown,
        PointerEventPass.PreUp,
        PointerEventPass.PreDown,
        PointerEventPass.PostUp,
        PointerEventPass.PostDown
    ),
    size = size
)

// TODO(shepshapard): Rename to invokeOverPass
fun PointerInputHandler.invokeOverPasses(
    pointerInputChange: PointerInputChange,
    pointerEventPass: PointerEventPass,
    size: IntPxSize = IntPxSize(Int.MAX_VALUE.ipx, Int.MAX_VALUE.ipx)
) = invokeOverPasses(listOf(pointerInputChange), listOf(pointerEventPass), size).first()

// TODO(shepshapard): Rename to invokeOverPass
fun PointerInputHandler.invokeOverPasses(
    vararg pointerInputChanges: PointerInputChange,
    pointerEventPass: PointerEventPass,
    size: IntPxSize = IntPxSize(Int.MAX_VALUE.ipx, Int.MAX_VALUE.ipx)
) = invokeOverPasses(pointerInputChanges.toList(), listOf(pointerEventPass), size)

fun PointerInputHandler.invokeOverPasses(
    pointerInputChange: PointerInputChange,
    vararg pointerEventPasses: PointerEventPass,
    size: IntPxSize = IntPxSize(Int.MAX_VALUE.ipx, Int.MAX_VALUE.ipx)
) = invokeOverPasses(listOf(pointerInputChange), pointerEventPasses.toList(), size).first()

fun PointerInputHandler.invokeOverPasses(
    pointerInputChanges: List<PointerInputChange>,
    pointerEventPasses: List<PointerEventPass>,
    size: IntPxSize = IntPxSize(Int.MAX_VALUE.ipx, Int.MAX_VALUE.ipx)
): List<PointerInputChange> {
    require(pointerInputChanges.isNotEmpty())
    require(pointerEventPasses.isNotEmpty())
    var localPointerInputChanges = pointerInputChanges
    pointerEventPasses.forEach {
        localPointerInputChanges = this.invoke(localPointerInputChanges, it, size)
    }
    return localPointerInputChanges
}

/**
 * Simulates the dispatching of [event] to [this] on all [PointerEventPass]es in their standard
 * order.
 *
 * @param event The event to dispatch.
 */
fun ((CustomEvent, PointerEventPass) -> Unit).invokeOverAllPasses(
    event: CustomEvent
) {
    invokeOverPasses(
        event,
        listOf(
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown,
            PointerEventPass.PostUp,
            PointerEventPass.PostDown
        )
    )
}

/**
 * Simulates the dispatching of [event] to [this] on all [PointerEventPass]es in their standard
 * order.
 *
 * @param event The event to dispatch.
 * @param pointerEventPasses The [PointerEventPass]es to pass to each call to [this].
 */
fun ((CustomEvent, PointerEventPass) -> Unit).invokeOverPasses(
    event: CustomEvent,
    pointerEventPasses: List<PointerEventPass>
) {
    pointerEventPasses.forEach { pass ->
        this.invoke(event, pass)
    }
}
