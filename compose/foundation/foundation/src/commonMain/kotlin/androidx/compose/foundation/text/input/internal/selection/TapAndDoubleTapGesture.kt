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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.PressGestureScopeImpl
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
import kotlinx.coroutines.launch

/**
 * Detect tap and double tap gestures. This is a special gesture detector that's copied from
 * [PointerInputScope.detectTapGestures] to always call [onTap] even when a double tap is detected.
 * In that case both [onTap] and [onDoubleTap] are called successively. However, if there is a long
 * tap, neither of the callbacks are called.
 */
internal suspend fun PointerInputScope.detectTapAndDoubleTap(
    onTap: TapOnPosition? = null,
    onDoubleTap: TapOnPosition? = null,
    onPress: suspend PressGestureScope.(Offset) -> Unit,
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectTapAndDoubleTap)

    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        launch {
            pressScope.reset()
            pressScope.onPress(down.position)
        }

        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        var upOrCancel: PointerInputChange? = null
        try {
            // wait for first tap up or long press
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpOrCancellation()
            }
            if (upOrCancel == null) {
                launch {
                    pressScope.cancel() // tap-up was canceled
                }
            } else {
                upOrCancel.consume()
                launch {
                    pressScope.release()
                }
            }
        } catch (_: PointerEventTimeoutCancellationException) {
            consumeUntilUp()
            launch {
                pressScope.release()
            }
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
                    launch {
                        pressScope.reset()
                    }
                    launch { pressScope.onPress(secondDown.position) }
                    try {
                        // Might have a long second press as the second tap
                        withTimeout(longPressTimeout) {
                            val secondUp = waitForUpOrCancellation()
                            if (secondUp != null) {
                                secondUp.consume()
                                launch {
                                    pressScope.release()
                                }
                                onDoubleTap.onEvent(secondUp.position)
                            } else {
                                launch {
                                    pressScope.cancel()
                                }
                            }
                        }
                    } catch (e: PointerEventTimeoutCancellationException) {
                        consumeUntilUp()
                        launch {
                            pressScope.release()
                        }
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
