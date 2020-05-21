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
import androidx.ui.core.CustomEventDispatcher
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.composed
import androidx.ui.core.consumeDownChange
import androidx.ui.core.gesture.customevents.DelayUpEvent
import androidx.ui.core.gesture.customevents.DelayUpMessage
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.temputils.delay
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.util.fastAny
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

// TODO(b/138605697): This bug tracks the note below: DoubleTapGestureDetector should use the
//  eventual api that will allow it to temporary block tap.
// TODO(b/138754591): The behavior of this gesture detector needs to be finalized.
// TODO(b/139020678): Probably has shared functionality with other press based detectors.
/**
 * Responds to pointers going down and up (tap) and then down and up again (another tap)
 * with minimal gap of time between the first up and the second down.
 *
 * Note: This is a temporary implementation to unblock dependents.  Once the underlying API that
 * allows double tap to temporarily block tap from firing is complete, this gesture detector will
 * not block tap when the first "up" occurs. It will however block the 2nd up from causing tap to
 * fire.
 *
 * Also, given that this gesture detector is so temporary, opting to not write substantial tests.
 */
fun Modifier.doubleTapGestureFilter(
    onDoubleTap: (PxPosition) -> Unit
): Modifier = composed {
    @Suppress("DEPRECATION")
    val coroutineContext = CoroutineContextAmbient.current
    // TODO(shepshapard): coroutineContext should be a field
    val filter = remember { DoubleTapGestureFilter(coroutineContext) }
    filter.onDoubleTap = onDoubleTap
    PointerInputModifierImpl(filter)
}

internal class DoubleTapGestureFilter(
    val coroutineContext: CoroutineContext
) : PointerInputFilter() {

    lateinit var onDoubleTap: (PxPosition) -> Unit

    private enum class State {
        Idle, Down, Up, SecondDown
    }

    var doubleTapTimeout = DoubleTapTimeout
    private var state = State.Idle
    private var job: Job? = null
    private lateinit var delayUpDispatcher: DelayUpDispatcher

    override fun onInit(customEventDispatcher: CustomEventDispatcher) {
        delayUpDispatcher = DelayUpDispatcher(customEventDispatcher)
    }

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {

        if (pass == PointerEventPass.PostUp) {
            if (state == State.Idle && changes.all { it.changedToDown() }) {
                state = State.Down
                return changes
            }

            if (state == State.Down && changes.all { it.changedToUp() }) {
                state = State.Up
                delayUpDispatcher.delayUp(changes)
                job = delay(doubleTapTimeout, coroutineContext) {
                    state = State.Idle
                    delayUpDispatcher.allowUp()
                }
                return changes
            }

            if (state == State.Up && changes.all { it.changedToDown() }) {
                state = State.SecondDown
                job?.cancel()
                delayUpDispatcher.disallowUp()
                return changes
            }

            if (state == State.SecondDown && changes.all { it.changedToUp() }) {
                state = State.Idle
                onDoubleTap.invoke(changes[0].previous.position!!)
                return changes.map { it.consumeDownChange() }
            }
        }

        if (pass == PointerEventPass.PostDown) {

            val noPointersAreInBoundsAndNotUpState =
                (state != State.Up && !changes.anyPointersInBounds(bounds))

            val anyPositionChangeConsumed = changes.fastAny { it.anyPositionChangeConsumed() }

            if (noPointersAreInBoundsAndNotUpState || anyPositionChangeConsumed) {
                // A pointers movement was consumed or all of our pointers are out of bounds, so
                // reset to idle.
                fullReset()
            }
        }

        return changes
    }

    override fun onCancel() {
        fullReset()
    }

    private fun fullReset() {
        delayUpDispatcher.disallowUp()
        job?.cancel()
        state = State.Idle
    }

    private class DelayUpDispatcher(val customEventDispatcher: CustomEventDispatcher) {

        // Non-writeable because we send this to customEventDispatcher and we don't want to ever
        // accidentally mutate what we have sent.
        private var blockedUpEvents: Set<PointerId>? = null

        fun delayUp(changes: List<PointerInputChange>) {
            blockedUpEvents =
                changes
                    .mapTo(mutableSetOf()) { it.id }
                    .also {
                        customEventDispatcher.retainHitPaths(it)
                        customEventDispatcher.dispatchCustomEvent(
                            DelayUpEvent(DelayUpMessage.DelayUp, it)
                        )
                    }
        }

        fun disallowUp() {
            unBlockUpEvents(true)
        }

        fun allowUp() {
            unBlockUpEvents(false)
        }

        private fun unBlockUpEvents(upIsConsumed: Boolean) {
            blockedUpEvents?.let {
                val message =
                    if (upIsConsumed) {
                        DelayUpMessage.DelayedUpConsumed
                    } else {
                        DelayUpMessage.DelayedUpNotConsumed
                    }
                customEventDispatcher.dispatchCustomEvent(
                    DelayUpEvent(message, it)
                )
                customEventDispatcher.releaseHitPaths(it)
            }
            blockedUpEvents = null
        }
    }
}