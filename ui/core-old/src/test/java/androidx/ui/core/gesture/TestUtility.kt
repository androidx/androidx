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

package androidx.ui.core.gesture

import androidx.ui.core.pointerinput.ConsumedData
import androidx.ui.core.pointerinput.PointerEventPass
import androidx.ui.core.pointerinput.PointerInputChange
import androidx.ui.core.pointerinput.PointerInputData
import androidx.ui.engine.geometry.Offset

val down = PointerInputChange(0f, 0f, false, 0f, 0f, true, 0f, 0f, false)
val downConsumed = PointerInputChange(0f, 0f, false, 0f, 0f, true, 0f, 0f, true)
val move = PointerInputChange(0f, 0f, true, 100f, 0f, true, 0f, 0f, false)
val moveConsumed = PointerInputChange(0f, 0f, true, 100f, 0f, true, 1f, 0f, false)
val up = PointerInputChange(0f, 0f, true, 0f, 0f, false, 0f, 0f, false)
val upConsumed = PointerInputChange(0f, 0f, true, 0f, 0f, false, 0f, 0f, true)
val upAfterMove = PointerInputChange(100f, 0f, true, 100f, 0f, false, 0f, 0f, false)

internal fun invokeHandler(
    pointerInputHandler: (PointerInputChange, PointerEventPass) -> PointerInputChange,
    pointerInputChange: PointerInputChange
): PointerInputChange {
    var localPointerInputChange = pointerInputChange
    PointerEventPass.values().forEach {
        localPointerInputChange = pointerInputHandler(localPointerInputChange, it)
    }
    return localPointerInputChange
}

internal fun PointerInputChange(
    previousX: Float = 0.0f,
    previousY: Float = 0.0f,
    previousDown: Boolean = false,
    currentX: Float = 0.0f,
    currentY: Float = 0.0f,
    currentDown: Boolean = false,
    consumedX: Float = 0.0f,
    consumedY: Float = 0.0f,
    consumedDown: Boolean = false
): PointerInputChange {
    return PointerInputChange(
        0,
        PointerInputData(Offset(currentX, currentY), currentDown),
        PointerInputData(Offset(previousX, previousY), previousDown),
        ConsumedData(Offset(consumedX, consumedY), consumedDown)
    )
}