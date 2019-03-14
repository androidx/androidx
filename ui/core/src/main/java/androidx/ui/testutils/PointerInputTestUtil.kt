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
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInputData
import androidx.ui.core.PointerInputHandler
import androidx.ui.engine.geometry.Offset

fun down(id: Int = 0, x: Float = 0f, y: Float = 0f): PointerInputChange =
    PointerInputChange(
        id,
        PointerInputData(Offset(x, y), true),
        PointerInputData(null, false),
        ConsumedData(Offset(0f, 0f), false)
    )

fun PointerInputChange.moveTo(x: Float = 0f, y: Float = 0f) =
    copy(previous = current, current = PointerInputData(Offset(x, y), true))

fun PointerInputChange.up() =
    copy(previous = current, current = PointerInputData(null, false))

fun PointerInputChange.consume(dx: Float = 0f, dy: Float = 0f, downChange: Boolean = false) =
    copy(
        consumed = consumed.copy(
            positionChange = Offset(
                consumed.positionChange.dx + dx,
                consumed.positionChange.dy + dy
            ), downChange = consumed.downChange || downChange
        )
    )

fun PointerInputHandler.invokeOverPasses(
    pointerInputChange: PointerInputChange,
    vararg pointerEventPasses: PointerEventPass
): PointerInputChange {
    var localPointerInputChange = pointerInputChange
    pointerEventPasses.forEach {
        localPointerInputChange = this.invoke(localPointerInputChange, it)
    }
    return localPointerInputChange
}

fun PointerInputHandler.invokeOverAllPasses(
    pointerInputChange: PointerInputChange
): PointerInputChange {
    return invokeOverPasses(
        pointerInputChange,
        PointerEventPass.InitialDown,
        PointerEventPass.PreUp,
        PointerEventPass.PreDown,
        PointerEventPass.PostUp,
        PointerEventPass.PostDown
    )
}