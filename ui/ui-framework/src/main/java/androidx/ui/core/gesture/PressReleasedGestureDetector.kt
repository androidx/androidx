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

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUp
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.consumeDownChange
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.PointerInputWrapper

/**
 * This gesture detector has a callback for when a press gesture being released for the purposes of
 * firing an event in response to something like a button being pressed.
 *
 * More specifically, it will call [onRelease] if:
 * - The first [PointerInputChange] it receives during the [PointerEventPass.PostUp] pass has
 *   an unconsumed down change.
 * - The last [PointerInputChange]  it receives during the [PointerEventPass.PostUp] pass has
 *   an unconsumed up change.
 * - And while it has at least one pointer touching it, no [PointerInputChange] has had any
 *   movement consumed (as that would indicate that something in the heirarchy moved and this a
 *   press should be cancelled.
 *
 * By default, this gesture detector also consumes the down change during the
 * [PointerEventPass.PostUp] pass if it has not already been consumed. That behavior can be changed
 * via [consumeDownOnStart].
 */
@Composable
fun PressReleasedGestureDetector(
    onRelease: (() -> Unit)? = null,
    consumeDownOnStart: Boolean = true,
    @Children children: @Composable() () -> Unit
) {
    val recognizer = +memo { PressReleaseGestureRecognizer() }
    recognizer.onRelease = onRelease
    recognizer.consumeDownOnStart = consumeDownOnStart
    PointerInputWrapper(pointerInputHandler = recognizer.pointerInputHandler) {
        children()
    }
}

internal class PressReleaseGestureRecognizer {
    /**
     * Called to indicate that a press gesture has successfully completed.
     *
     * This should be used to fire a state changing event as if a button was pressed.
     */
    var onRelease: (() -> Unit)? = null
    /**
     * True if down change should be consumed when start is called.  The default is true.
     */
    var consumeDownOnStart = true

    private var pointerCount = 0
    private var shouldRespondToUp = false

    val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass ->
            changes.map { processChange(it, pass) }
        }

    private fun processChange(
        pointerInputChange: PointerInputChange,
        pass: PointerEventPass
    ): PointerInputChange {
        var change = pointerInputChange

        if (pass == PointerEventPass.InitialDown && change.changedToDownIgnoreConsumed()) {
            pointerCount++
        }

        if (pass == PointerEventPass.PostUp && pointerCount == 1) {
            if (change.changedToDown()) {
                shouldRespondToUp = true
                if (consumeDownOnStart) {
                    change = change.consumeDownChange()
                }
            }
            if (shouldRespondToUp && change.changedToUp()) {
                onRelease?.invoke()
                change = change.consumeDownChange()
            }
        }

        if (pass == PointerEventPass.PostDown && change.anyPositionChangeConsumed()) {
            shouldRespondToUp = false
        }

        if (pass == PointerEventPass.PostDown && change.changedToUpIgnoreConsumed()) {
            pointerCount--
            if (pointerCount == 0) {
                shouldRespondToUp = false
            }
        }

        return change
    }
}