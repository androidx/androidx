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
import androidx.ui.core.CustomEvent
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
import androidx.ui.core.gesture.customevents.DelayUpEvent
import androidx.ui.core.gesture.customevents.DelayUpMessage
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.util.fastAny

/**
 * This gesture detector fires a callback when a traditional press is being released.  This is
 * generally the same thing as "onTap" or "onClick".
 *
 * [onTap] is called with the position of the last pointer to go "up".
 *
 * More specifically, it will call [onTap] if:
 * - All of the first [PointerInputChange]s it receives during the [PointerEventPass.PostUp] pass
 *   have unconsumed down changes, thus representing new set of pointers, none of which have had
 *   their down events consumed.
 * - The last [PointerInputChange] it receives during the [PointerEventPass.PostUp] pass has
 *   an unconsumed up change.
 * - While it has at least one pointer touching it, no [PointerInputChange] has had any
 *   movement consumed (as that would indicate that something in the heirarchy moved and this a
 *   press should be cancelled.
 * - It also fully cooperates with [DelayUpEvent] [CustomEvent]s it receives such that it will delay
 *   calling [onTap] if all of it's up events are being blocked.  If it was being blocked and later
 *   is allowed to fire it's up event (which is [onTap]) it will do so and consume the delayed up
 *   custom event such that no other gesture filters will also respond to the delayed up.
 *
 *   @param onTap Called when a tap has occurred.
 */
// TODO(b/139020678): Probably has shared functionality with other press based detectors.

fun Modifier.tapGestureFilter(
    onTap: (PxPosition) -> Unit
): Modifier = composed {
    val filter = remember { TapGestureFilter() }
    filter.onTap = onTap
    PointerInputModifierImpl(filter)
}

internal class TapGestureFilter : PointerInputFilter() {
    /**
     * Called to indicate that a press gesture has successfully completed.
     *
     * This should be used to fire a state changing event as if a button was pressed.
     */
    lateinit var onTap: (PxPosition) -> Unit

    /**
     * True when we are primed to call [onTap] and may be consuming all down changes.
     */
    private var primed = false

    private var downPointers: MutableSet<PointerId> = mutableSetOf()
    private var upBlockedPointers: MutableSet<PointerId> = mutableSetOf()
    private var lastPxPosition: PxPosition? = null

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {

        if (pass == PointerEventPass.PostUp) {

            if (primed &&
                changes.all { it.changedToUp() }
            ) {
                val pointerPxPosition: PxPosition = changes[0].previous.position!!
                if (changes.fastAny { !upBlockedPointers.contains(it.id) }) {
                    // If we are primed, all pointers went up, and at least one of the pointers is
                    // not blocked, we can fire, reset, and consume all of the up events.
                    reset()
                    onTap.invoke(pointerPxPosition)
                    return changes.map { it.consumeDownChange() }
                } else {
                    lastPxPosition = pointerPxPosition
                }
            }

            if (changes.all { it.changedToDown() }) {
                // Reset in case we were incorrectly left waiting on a delayUp message.
                reset()
                // If all of the changes are down, can become primed.
                primed = true
            }

            if (primed) {
                changes.forEach {
                    if (it.changedToDown()) {
                        downPointers.add(it.id)
                    }
                    if (it.changedToUpIgnoreConsumed()) {
                        downPointers.remove(it.id)
                    }
                }
            }
        }

        if (pass == PointerEventPass.PostDown && primed) {

            val anyPositionChangeConsumed = changes.fastAny { it.anyPositionChangeConsumed() }

            val noPointersInBounds =
                upBlockedPointers.isEmpty() && !changes.anyPointersInBounds(bounds)

            if (anyPositionChangeConsumed || noPointersInBounds) {
                // If we are on the final pass, we are primed, and either we aren't blocked and
                // all pointers are out of bounds.
                reset()
            }
        }

        return changes
    }

    override fun onCancel() {
        reset()
    }

    override fun onCustomEvent(customEvent: CustomEvent, pass: PointerEventPass) {
        if (!primed || pass != PointerEventPass.PostUp || customEvent !is DelayUpEvent) {
            return
        }

        if (customEvent.message == DelayUpMessage.DelayUp) {
            // If the message is to DelayUp, track all currently down pointers that are also ones
            // we are supposed to block the up event for.
            customEvent.pointers.forEach {
                if (downPointers.contains(it)) {
                    upBlockedPointers.add(it)
                }
            }
            return
        }

        upBlockedPointers.removeAll(customEvent.pointers)
        if (upBlockedPointers.isEmpty() && downPointers.isEmpty()) {
            if (customEvent.message == DelayUpMessage.DelayedUpNotConsumed) {
                // If the up was not consumed, then we can fire our callback and consume it.
                onTap.invoke(lastPxPosition!!)
                customEvent.message = DelayUpMessage.DelayedUpConsumed
            }
            // At this point, we were primed, no pointers were down, and we are unblocked, so we
            // are at least resetting.
            reset()
        }
    }

    private fun reset() {
        primed = false
        upBlockedPointers.clear()
        downPointers.clear()
        lastPxPosition = null
    }
}