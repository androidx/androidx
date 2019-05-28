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

import androidx.ui.core.IntPxPosition
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputHandler
import androidx.ui.core.PxPosition
import androidx.ui.core.Timestamp
import androidx.ui.core.ipx

/**
 * This class enables Mockito to spy.
 *
 * It also allows the setting of a [modifyBlock] which is also a [PointerInputHandler] and enables
 * the processing of incoming [PointerInputChange]s.
 */
open class MyPointerInputHandler : PointerInputHandler {
    var modifyBlock: PointerInputHandler? = null
    override fun invoke(
        p1: List<PointerInputChange>,
        p2: PointerEventPass
    ): List<PointerInputChange> {
        return modifyBlock?.invoke(p1, p2) ?: p1
    }
}

internal fun LayoutNode(x: Int, y: Int, x2: Int, y2: Int) =
    androidx.ui.core.LayoutNode().apply {
        moveTo(x.ipx, y.ipx)
        resize(x2.ipx - x.ipx, y2.ipx - y.ipx)
    }

internal fun LayoutNode(position: IntPxPosition) =
    androidx.ui.core.LayoutNode().apply {
        moveTo(position.x, position.y)
    }

internal fun PointerInputEventData(
    id: Int,
    timestamp: Timestamp,
    position: PxPosition?,
    down: Boolean
): PointerInputEventData {
    val pointerInputData = androidx.ui.core.PointerInputData(timestamp, position, down)
    return PointerInputEventData(id, pointerInputData)
}

internal fun PointerInputEvent(
    id: Int,
    timestamp: Timestamp,
    position: PxPosition?,
    down: Boolean
): PointerInputEvent {
    return PointerInputEvent(
        timestamp,
        listOf(PointerInputEventData(id, timestamp, position, down))
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