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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInput
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.consumeDownChange
import androidx.ui.core.gesture.util.anyPointersInBounds
import androidx.ui.unit.IntPxSize

/**
 * This gesture detector fires a callback when a traditional press is being released.  This is
 * generally the same thing as "onTap" or "onClick".
 *
 * More specifically, it will call [onRelease] if:
 * - All of the first [PointerInputChange]s it receives during the [PointerEventPass.PostUp] pass
 *   have unconsumed down changes, thus representing new set of pointers, none of which have had
 *   their down events consumed.
 * - The last [PointerInputChange] it receives during the [PointerEventPass.PostUp] pass has
 *   an unconsumed up change.
 * - While it has at least one pointer touching it, no [PointerInputChange] has had any
 *   movement consumed (as that would indicate that something in the heirarchy moved and this a
 *   press should be cancelled.
 *
 * By default, this gesture detector also consumes the down change during the
 * [PointerEventPass.PostUp] pass if it has not already been consumed. That behavior can be changed
 * via [consumeDownOnStart].
 */
// TODO(b/139020678): Probably has shared functionality with other press based detectors.
// TODO(b/145238703): consumeDownOnStart should very likely go away.
@Composable
fun PressReleasedGestureDetector(
    onRelease: (() -> Unit)? = null,
    consumeDownOnStart: Boolean = true,
    enabled: Boolean = true,
    children: @Composable() () -> Unit
) {
    val recognizer = remember { PressReleaseGestureRecognizer() }
    recognizer.onRelease = onRelease
    recognizer.consumeDownOnStart = consumeDownOnStart
    recognizer.setEnabled(enabled)

    PointerInput(
        pointerInputHandler = recognizer.pointerInputHandler,
        cancelHandler = recognizer.cancelHandler,
        children = children
    )
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

    /**
     * True when we are primed to call [onRelease] and may be consuming all down changes.
     */
    private var active = false

    private var enabled = true

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!enabled) {
            active = false
        }
    }

    val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass, bounds: IntPxSize ->

            var internalChanges = changes

            if (pass == PointerEventPass.PostUp) {

                if (enabled && internalChanges.all { it.changedToDown() }) {
                    // If we have not yet started and all of the changes changed to down, we are
                    // starting.
                    active = true
                } else if (active && internalChanges.all { it.changedToUp() }) {
                    // If we have started and all of the changes changed to up, we are stopping.
                    active = false
                    internalChanges = internalChanges.map { it.consumeDownChange() }
                    onRelease?.invoke()
                } else if (!internalChanges.anyPointersInBounds(bounds)) {
                    // If none of the pointers are in bounds of our bounds, we should reset and wait
                    // till all pointers are changing to down.
                    cancelHandler()
                }

                if (active && consumeDownOnStart) {
                    // If we have started, we should consume the down change on all changes.
                    internalChanges = internalChanges.map { it.consumeDownChange() }
                }
            }

            if (pass == PointerEventPass.PostDown && active &&
                internalChanges.any { it.anyPositionChangeConsumed() }
            ) {
                // On the final pass, if we have started and any of the changes had consumed
                // position changes, we cancel.
                cancelHandler()
            }

            internalChanges
        }

    val cancelHandler = {
        active = false
    }
}