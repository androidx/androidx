/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal.selection

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny

/**
 * Detects pointer down and up events. This detector does not require events to be unconsumed.
 */
internal suspend fun PointerInputScope.detectPressDownGesture(
    onDown: TapOnPosition,
    onUp: (() -> Unit)? = null
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDown.onEvent(down.position)

        if (onUp != null) {
            // Wait for that pointer to come up.
            do {
                val event = awaitPointerEvent()
            } while (event.changes.fastAny { it.id == down.id && it.pressed })
            onUp.invoke()
        }
    }
}
