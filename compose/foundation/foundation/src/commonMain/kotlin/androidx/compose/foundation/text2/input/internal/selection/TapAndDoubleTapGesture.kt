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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.coroutineScope

/**
 * Detect tap and double tap gestures. This is a special gesture detector that's copied from
 * [PointerInputScope.detectTapGestures] to always call [onTap] even when a double tap is detected.
 * In that case both [onTap] and [onDoubleTap] are called successively. However, if there is a long
 * tap, neither of the callbacks are called.
 */
internal suspend fun PointerInputScope.detectTapAndDoubleTap(
    onTap: TapOnPosition? = null,
    onDoubleTap: TapOnPosition? = null,
) = coroutineScope {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        var upOrCancel: PointerInputChange? = null
        try {
            // wait for first tap up or long press
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpOrCancellation()
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            consumeUntilUp()
        }

        if (upOrCancel != null) {
            upOrCancel.consume()
            // Tap was successful. There was no long click. Now evaluate whether it's gonna be
            // a double tap.
            onTap?.onEvent(upOrCancel.position)
            if (onDoubleTap != null) {
                // check for second tap
                val secondDown = awaitSecondDown(upOrCancel)

                if (secondDown != null) {
                    try {
                        // Might have a long second press as the second tap
                        withTimeout(longPressTimeout) {
                            val secondUp = waitForUpOrCancellation()
                            if (secondUp != null) {
                                secondUp.consume()
                                onDoubleTap.onEvent(secondUp.position)
                            }
                        }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        consumeUntilUp()
                    }
                }
            }
        }
    }
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a
 * second press event is received before the time out, it is returned or `null` is returned
 * if no second press is received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange
): PointerInputChange? = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
    val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
    var change: PointerInputChange
    // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
    do {
        change = awaitFirstDown()
    } while (change.uptimeMillis < minUptime)
    change
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

internal fun interface TapOnPosition {
    fun onEvent(offset: Offset)
}
