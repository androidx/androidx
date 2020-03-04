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
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.consumeDownChange
import androidx.ui.core.gesture.util.anyPointersInBounds
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.temputils.delay
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
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
@Composable
fun DoubleTapGestureDetector(
    onDoubleTap: (PxPosition) -> Unit
): Modifier {
    val coroutineContext = CoroutineContextAmbient.current
    // TODO(shepshapard): coroutineContext should be a field
    val modifier = remember { DoubleTapGestureRecognizer(coroutineContext) }
    modifier.onDoubleTap = onDoubleTap
    return PointerInputModifier(modifier)
}

internal class DoubleTapGestureRecognizer(
    coroutineContext: CoroutineContext
) : PointerInputFilter() {
    lateinit var onDoubleTap: (PxPosition) -> Unit

    private enum class State {
        Idle, Down, Up, SecondDown
    }

    var doubleTapTimeout = DoubleTapTimeout
    private var state = State.Idle
    private var job: Job? = null

    override val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass, bounds: IntPxSize ->

            var changesToReturn = changes

            if (pass == PointerEventPass.PostUp) {
                if (state == State.Idle && changesToReturn.all { it.changedToDown() }) {
                    state = State.Down
                } else if (state == State.Down && changesToReturn.all { it.changedToUp() }) {
                    state = State.Up
                    job = delay(doubleTapTimeout, coroutineContext) {
                        state = State.Idle
                    }
                } else if (state == State.Up && changesToReturn.all { it.changedToDown() }) {
                    job?.cancel()
                    state = State.SecondDown
                } else if (state == State.SecondDown && changesToReturn.all { it.changedToUp() }) {
                    changesToReturn = changesToReturn.map { it.consumeDownChange() }
                    state = State.Idle
                    onDoubleTap.invoke(changes[0].previous.position!!)
                } else if ((state == State.Down || state == State.SecondDown) &&
                    !changesToReturn.anyPointersInBounds(bounds)
                ) {
                    // If we are in one of the down states, and none of pointers are in our bounds,
                    // then we should cancel and wait till we can be Idle again.
                    state = State.Idle
                }
            }

            if (pass == PointerEventPass.PostDown &&
                changesToReturn.any { it.anyPositionChangeConsumed() }
            ) {
                state = State.Idle
            }

            changesToReturn
        }

    override var cancelHandler = {
        job?.cancel()
        state = State.Idle
    }
}