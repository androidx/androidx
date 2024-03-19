/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.handwriting

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastFirstOrNull

/**
 * A utility function that detects stylus movements and calls the [onHandwritingSlopExceeded] when
 * it detects that stylus movement has exceeds the handwriting slop.
 * If [onHandwritingSlopExceeded] returns true, this method will consume the events and consider
 * that the handwriting has successfully started. Otherwise, it'll stop monitoring the current
 * gesture.
 */
internal suspend inline fun PointerInputScope.detectStylusHandwriting(
    crossinline onHandwritingSlopExceeded: () -> Boolean
) {
    awaitEachGesture {
        val firstDown =
            awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Initial)

        val isStylus =
            firstDown.type == PointerType.Stylus || firstDown.type == PointerType.Eraser
        if (!isStylus) {
            return@awaitEachGesture
        }
        // Await the touch slop before long press timeout.
        var exceedsTouchSlop: PointerInputChange? = null
        // The stylus move must exceeds touch slop before long press timeout.
        while (true) {
            val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Main)
            // The tracked pointer is consumed or lifted, stop tracking.
            val change = pointerEvent.changes.fastFirstOrNull {
                !it.isConsumed && it.id == firstDown.id && it.pressed
            }
            if (change == null) {
                break
            }

            val time = change.uptimeMillis - firstDown.uptimeMillis
            if (time >= viewConfiguration.longPressTimeoutMillis) {
                break
            }

            val offset = change.position - firstDown.position
            if (offset.getDistance() > viewConfiguration.handwritingSlop) {
                exceedsTouchSlop = change
                break
            }
        }

        if (exceedsTouchSlop == null || !onHandwritingSlopExceeded.invoke()) {
            return@awaitEachGesture
        }
        exceedsTouchSlop.consume()

        // Consume the remaining changes of this pointer.
        while (true) {
            val pointerEvent = awaitPointerEvent(pass = PointerEventPass.Initial)
            val pointerChange = pointerEvent.changes.fastFirstOrNull {
                !it.isConsumed && it.id == firstDown.id && it.pressed
            } ?: return@awaitEachGesture
            pointerChange.consume()
        }
    }
}

/**
 *  Whether the platform supports the stylus handwriting or not. This is for platform level support
 *  and NOT for checking whether the IME supports handwriting.
 */
internal expect val isStylusHandwritingSupported: Boolean
