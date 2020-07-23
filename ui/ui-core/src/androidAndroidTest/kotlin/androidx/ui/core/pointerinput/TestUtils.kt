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

import androidx.ui.core.InternalPointerEvent
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Uptime

/**
 * This class enables Mockito to spy.
 *
 * It also allows the setting of a [modifyBlock] which is also a [PointerInputHandler] and enables
 * the processing of incoming [PointerInputChange]s.
 */
open class StubPointerInputHandler(
    private var modifyBlock: PointerInputHandler? = null
) : PointerInputHandler {
    override fun invoke(
        p1: List<PointerInputChange>,
        p2: PointerEventPass,
        p3: IntSize
    ): List<PointerInputChange> {
        return modifyBlock?.invoke(p1, p2, p3) ?: p1
    }
}

internal fun PointerInputEventData(
    id: Int,
    uptime: Uptime,
    position: Offset?,
    down: Boolean
): PointerInputEventData {
    val pointerInputData = PointerInputData(uptime, position, down)
    return PointerInputEventData(PointerId(id.toLong()), pointerInputData)
}

internal fun PointerInputEvent(
    id: Int,
    uptime: Uptime,
    position: Offset?,
    down: Boolean
): PointerInputEvent {
    return PointerInputEvent(
        uptime,
        listOf(PointerInputEventData(id, uptime, position, down)),
        MotionEventDouble
    )
}

internal fun PointerInputEvent(
    uptime: Uptime,
    pointers: List<PointerInputEventData>
) = PointerInputEvent(
        uptime,
        pointers,
        MotionEventDouble
    )

internal fun catchThrowable(lambda: () -> Unit): Throwable? {
    var exception: Throwable? = null

    try {
        lambda()
    } catch (theException: Throwable) {
        exception = theException
    }

    return exception
}

internal fun internalPointerEventOf(vararg changes: PointerInputChange) =
    InternalPointerEvent(changes.toList().associateBy { it.id }.toMutableMap(), MotionEventDouble)

/**
 * To be used to construct types that require a MotionEvent but where no details of the MotionEvent
 * are actually needed.
 */
private val MotionEventDouble = android.view.MotionEvent.obtain(0L, 0L,
    android.view.MotionEvent.ACTION_DOWN, 0f, 0f, 0)