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
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.composed
import androidx.ui.core.consumeDownChange
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition

/**
 * Reacts if the first pointer input change it sees is an unconsumed down change, and if it reacts,
 * consumes all further down changes.
 *
 * This GestureDetector is not generally intended to be used directly, but is instead intended to be
 * used as a building block to create more complex GestureDetectors.
 *
 * This GestureDetector is a bit more experimental then the other GestureDetectors (the number and
 * types of GestureDetectors is still very much a work in progress) and is intended to be a
 * generically useful building block for more complicated GestureDetectors.
 *
 * The theory is that this GestureDetector can be reused in PressIndicatorGestureDetector, and there
 * could be a corresponding RawPressReleasedGestureDetector.
 *
 * @param onPressStart Called when the first pointer "presses" on the GestureDetector.  [PxPosition]
 * is the position of that first pointer on press.
 * @param enabled If false, this GestureDetector will effectively act as if it is not in the
 * hierarchy.
 * @param executionPass The [PointerEventPass] during which this GestureDetector will attempt to
 * react to and consume down changes.  Defaults to [PointerEventPass.PostUp].
 */
fun Modifier.rawPressStartGestureFilter(
    onPressStart: (PxPosition) -> Unit,
    enabled: Boolean = false,
    executionPass: PointerEventPass = PointerEventPass.PostUp
): Modifier = composed {
    val filter = remember { RawPressStartGestureFilter() }
    filter.onPressStart = onPressStart
    filter.setEnabled(enabled = enabled)
    filter.setExecutionPass(executionPass)
    PointerInputModifierImpl(filter)
}

internal class RawPressStartGestureFilter : PointerInputFilter() {

    lateinit var onPressStart: (PxPosition) -> Unit
    private var enabled: Boolean = true
    private var executionPass = PointerEventPass.InitialDown

    private var active = false

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {

            var internalChanges = changes

            if (pass == executionPass) {
                if (enabled && internalChanges.all { it.changedToDown() }) {
                    // If we have not yet started and all of the changes changed to down, we are
                    // starting.
                    active = true
                    onPressStart(internalChanges.first().current.position!!)
                } else if (internalChanges.all { it.changedToUp() }) {
                    // If we have started and all of the changes changed to up, we are stopping.
                    active = false
                }

                if (active) {
                    // If we have started, we should consume the down change on all changes.
                    internalChanges = internalChanges.map { it.consumeDownChange() }
                }
            }

            return internalChanges
        }

    override fun onCancel() {
        active = false
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        // Whenever we are disabled, we can just go ahead and become inactive (which is the state we
        // should be in if we are to pretend that we aren't in the hierarchy.
        if (!enabled) {
            onCancel()
        }
    }

    fun setExecutionPass(executionPass: PointerEventPass) {
        this.executionPass = executionPass
    }
}