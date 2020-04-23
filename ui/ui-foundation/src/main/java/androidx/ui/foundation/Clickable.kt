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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.onDispose
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.gesture.anyPointersInBounds
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition

/**
 * Combines [tapGestureFilter] and [Semantics] for the clickable
 * components like Button.
 *
 * @sample androidx.ui.foundation.samples.ClickableSample
 *
 * @param onClick will be called when user clicked on the button
 * @param modifier allows to provide a modifier to be added before the gesture detector, for
 * example Ripple should be added at this point. this will be easier once we migrate this
 * function to a Modifier
 * @param enabled Controls the enabled state. When `false`, this component will not be
 * clickable
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param interactionState [InteractionState] that will be updated when this Clickable is
 * pressed, using [Interaction.Pressed].
 */
@Composable
fun Clickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionState: InteractionState? = null,
    children: @Composable() () -> Unit
) {
    Semantics(
        container = true,
        properties = {
            this.enabled = enabled
            if (enabled) {
                onClick(action = onClick, label = onClickLabel)
            }
        }
    ) {
        // TODO(b/150706555): This layout is temporary and should be removed once Semantics
        //  is implemented with modifiers.
        val tap = if (enabled) {
            (interactionState?.run {
                Modifier.noConsumptionIndicatorGestureFilter(
                    onStart = { addInteraction(Interaction.Pressed) },
                    onStop = { removeInteraction(Interaction.Pressed) },
                    onCancel = { removeInteraction(Interaction.Pressed) }
                )
            } ?: Modifier).tapGestureFilter(onClick)
        } else {
            Modifier
        }
        onCommit(interactionState) {
            onDispose {
                interactionState?.removeInteraction(Interaction.Pressed)
            }
        }
        @Suppress("DEPRECATION")
        PassThroughLayout(modifier + tap, children)
    }
}

/**
 * TODO: b/154589321 remove this
 * Temporary copy of pressIndicatorGestureFilter that does *not* consume down events.
 * This is needed so that Ripple can still see the events after clickable does, so that the
 * Ripple will still show.
 */
@Composable
private fun Modifier.noConsumptionIndicatorGestureFilter(
    onStart: (PxPosition) -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
): Modifier = this + remember { NoConsumptionIndicatorGestureFilter(onStart, onStop, onCancel) }

/**
 * Temporary, see [noConsumptionIndicatorGestureFilter]
 */
private class NoConsumptionIndicatorGestureFilter(
    val onStart: (PxPosition) -> Unit,
    val onStop: () -> Unit,
    // Rename to avoid clashing with onCancel() function
    val onCancelCallback: () -> Unit
) : PointerInputFilter(), PointerInputModifier {
    override val pointerInputFilter = this

    private var state = State.Idle

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {
        if (pass == PointerEventPass.PostUp) {
            if (state == State.Idle && changes.all { it.changedToDown() }) {
                // If we have not yet started and all of the changes changed to down, we are
                // starting.
                state = State.Started
                onStart(changes.first().current.position!!)
            } else if (state == State.Started) {
                if (changes.all { it.changedToUpIgnoreConsumed() }) {
                    // If we have started and all of the changes changed to up, we are stopping.
                    state = State.Idle
                    onStop()
                } else if (!changes.anyPointersInBounds(bounds)) {
                    // If all of the down pointers are currently out of bounds, we should cancel
                    // as this indicates that the user does not which to trigger a press based
                    // event.
                    state = State.Idle
                    onCancelCallback()
                }
            }
        }

        if (
            pass == PointerEventPass.PostDown &&
            state == State.Started &&
            changes.any { it.anyPositionChangeConsumed() }
        ) {
            // On the final pass, if we have started and any of the changes had consumed
            // position changes, we cancel.
            state = State.Idle
            onCancelCallback()
        }

        return changes
    }

    override fun onCancel() {
        if (state == State.Started) {
            state = State.Idle
            onCancelCallback()
        }
    }

    private enum class State {
        Idle, Started
    }
}
