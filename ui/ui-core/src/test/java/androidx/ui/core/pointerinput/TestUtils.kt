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

package androidx.ui.core.pointerinput

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.Uptime

/**
 * This class enables Mockito to spy.
 *
 * It also allows the setting of a [modifyBlock] which is also a [PointerInputHandler] and enables
 * the processing of incoming [PointerInputChange]s.
 */
open class StubPointerInputHandler(
    var modifyBlock: PointerInputHandler? = null
) : PointerInputHandler {
    override fun invoke(
        p1: List<PointerInputChange>,
        p2: PointerEventPass,
        p3: IntPxSize
    ): List<PointerInputChange> {
        return modifyBlock?.invoke(p1, p2, p3) ?: p1
    }
}

internal fun PointerInputEventData(
    id: Int,
    uptime: Uptime,
    position: PxPosition?,
    down: Boolean
): PointerInputEventData {
    val pointerInputData = PointerInputData(uptime, position, down)
    return PointerInputEventData(PointerId(id.toLong()), pointerInputData)
}

internal fun PointerInputEvent(
    id: Int,
    uptime: Uptime,
    position: PxPosition?,
    down: Boolean
): PointerInputEvent {
    return PointerInputEvent(
        uptime,
        listOf(PointerInputEventData(id, uptime, position, down))
    )
}

internal fun catchThrowable(lambda: () -> Unit): Throwable? {
    var exception: Throwable? = null

    try {
        lambda()
    } catch (theException: Throwable) {
        exception = theException
    }

    return exception
}