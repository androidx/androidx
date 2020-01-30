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
import androidx.ui.core.CoroutineContextAmbient
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.PointerInput
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.consumeDownChange
import androidx.ui.core.PointerId
import androidx.ui.core.gesture.util.anyPointersInBounds
import androidx.ui.temputils.delay
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

// TODO(b/137569202): This bug tracks the note below regarding the need to eventually
//  improve LongPressGestureDetector.
// TODO(b/139020678): Probably has shared functionality with other press based detectors.
/**
 * Responds to a pointer being "down" for an extended amount of time.
 *
 * Note: this is likely a temporary, naive, and flawed approach. It is not necessarily guaranteed
 * to interoperate well with forthcoming behavior related to disambiguation between multi-tap
 * (double tap, triple tap) and tap.
 */
@Composable
fun LongPressGestureDetector(
    onLongPress: (PxPosition) -> Unit,
    children: @Composable() () -> Unit
) {
    val coroutineContext = CoroutineContextAmbient.current
    val recognizer =
        remember { LongPressGestureRecognizer(coroutineContext) }
    recognizer.onLongPress = onLongPress

    PointerInput(
        pointerInputHandler = recognizer.pointerInputHandler,
        cancelHandler = recognizer.cancelHandler,
        children = children
    )
}

internal class LongPressGestureRecognizer(
    coroutineContext: CoroutineContext
) {
    lateinit var onLongPress: (PxPosition) -> Unit

    private enum class State {
        Idle, Primed, Fired
    }

    private var state = State.Idle
    private val pointerPositions = linkedMapOf<PointerId, PxPosition>()
    var longPressTimeout = LongPressTimeout
    var job: Job? = null

    val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass, bounds: IntPxSize ->

            var changesToReturn = changes

            if (pass == PointerEventPass.InitialDown && state == State.Fired) {
                // If we are in the Fired state, we dispatched the long press event and pointers are still down so we
                // should consume any up events to prevent other gesture detectors from responding to up.
                changesToReturn = changesToReturn.map {
                    if (it.changedToUp()) {
                        it.consumeDownChange()
                    } else {
                        it
                    }
                }
            }

            if (pass == PointerEventPass.PostUp) {
                if (state == State.Idle && changes.all { it.changedToDown() }) {
                    // If we have not yet started and all of the changes changed to down, we are
                    // starting.
                    job = delay(longPressTimeout, coroutineContext) {
                        onLongPress.invoke(pointerPositions.asIterable().first().value)
                        state = State.Fired
                    }
                    pointerPositions.clear()
                    state = State.Primed
                } else if (state != State.Idle && changes.all { it.changedToUpIgnoreConsumed() }) {
                    // If we have started and all of the changes changed to up, we are stopping.
                    cancelHandler()
                } else if (!changesToReturn.anyPointersInBounds(bounds)) {
                    // If none of the pointers are in bounds of our bounds, we should reset and wait
                    // till all pointers are changing to down to "prime" again.
                    cancelHandler()
                }

                if (state == State.Primed) {
                    // If we are primed, for all down pointers, keep track of their current
                    // positions, and for all other pointers, remove their tracked information.
                    changes.forEach {
                        if (it.current.down) {
                            pointerPositions[it.id] = it.current.position!!
                        } else {
                            pointerPositions.remove(it.id)
                        }
                    }
                }
            }

            if (pass == PointerEventPass.PostDown &&
                state != State.Idle &&
                changes.any { it.anyPositionChangeConsumed() }
            ) {
                // If we are primed, reset so we don't fire.
                // If we are fired, reset to idle so we don't block up events that still fire after
                // dragging (like flinging).
                cancelHandler()
            }

            changesToReturn
        }

    val cancelHandler = {
        job?.cancel()
        state = State.Idle
    }
}