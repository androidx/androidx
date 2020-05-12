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

import androidx.compose.remember
import androidx.ui.core.CoroutineContextAmbient
import androidx.ui.core.CustomEvent
import androidx.ui.core.CustomEventDispatcher
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.composed
import androidx.ui.core.consumeDownChange
import androidx.ui.core.gesture.customevents.LongPressFiredEvent
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.temputils.delay
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.util.fastAny
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
fun Modifier.longPressGestureFilter(
    onLongPress: (PxPosition) -> Unit
): Modifier = composed {
    @Suppress("DEPRECATION")
    val coroutineContext = CoroutineContextAmbient.current
    val filter = remember { LongPressGestureFilter(coroutineContext) }
    filter.onLongPress = onLongPress
    PointerInputModifierImpl(filter)
}

internal class LongPressGestureFilter(
    private val coroutineContext: CoroutineContext
) : PointerInputFilter() {
    lateinit var onLongPress: (PxPosition) -> Unit

    var longPressTimeout = LongPressTimeout

    private enum class State {
        Idle, Primed, Fired
    }

    private var state = State.Idle
    private val pointerPositions = linkedMapOf<PointerId, PxPosition>()
    private var job: Job? = null
    private lateinit var customEventDispatcher: CustomEventDispatcher

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        this.customEventDispatcher = customEventDispatcher
    }

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {

            var changesToReturn = changes

            if (pass == PointerEventPass.InitialDown && state == State.Fired) {
                // If we fired and have not reset, we should prevent other pointer input nodes from
                // responding to up, so consume it early on.
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
                    // If we are idle and all of the changes changed to down, we are prime to fire
                    // the event.
                    primeToFire()
                } else if (state != State.Idle && changes.all { it.changedToUpIgnoreConsumed() }) {
                    // If we have started and all of the changes changed to up, reset to idle.
                    resetToIdle()
                } else if (!changesToReturn.anyPointersInBounds(bounds)) {
                    // If all pointers have gone out of bounds, reset to idle.
                    resetToIdle()
                }

                if (state == State.Primed) {
                    // If we are primed, keep track of all down pointer positions so we can pass
                    // pointer position information to the event we will fire.
                    changes.forEach {
                        if (it.current.down) {
                            pointerPositions[it.id] = it.current.position!!
                        } else {
                            pointerPositions.remove(it.id)
                        }
                    }
                }
            }

            if (
                pass == PointerEventPass.PostDown &&
                state != State.Idle &&
                changes.fastAny { it.anyPositionChangeConsumed() }
            ) {
                // If we are anything but Idle and something consumed movement, reset.
                resetToIdle()
            }

            return changesToReturn
        }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        if (
            state == State.Primed &&
            customEvent is LongPressFiredEvent &&
            pass == PointerEventPass.InitialDown
        ) {
            // If we are primed but something else fired long press, we should reset.
            // Doesn't matter what pass we are on, just choosing one so we only reset once.
            resetToIdle()
        }
    }

    override fun onCancel() {
        resetToIdle()
    }

    private fun fireLongPress() {
        state = State.Fired
        onLongPress.invoke(pointerPositions.asIterable().first().value)
        customEventDispatcher.dispatchCustomEvent(LongPressFiredEvent)
    }

    private fun primeToFire() {
        state = State.Primed
        job = delay(longPressTimeout, coroutineContext) {
            fireLongPress()
        }
    }

    private fun resetToIdle() {
        state = State.Idle
        job?.cancel()
        pointerPositions.clear()
    }
}