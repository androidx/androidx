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
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.composed
import androidx.ui.core.gesture.anyPointersInBounds
import androidx.ui.core.gesture.doubleTapGestureFilter
import androidx.ui.core.gesture.longPressGestureFilter
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.semantics.semantics
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.util.fastAny

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
 * pressed, using [Interaction.Pressed]. Only initial (first) press will be recorded and added to
 * [InteractionState]
 *
 * @deprecated Use [clickable] modifier instead
 */
@Deprecated(
    "Clickable has been deprecated, use clickable modifier instead",
    ReplaceWith(
        "Box(modifier.clickable(onClick = onClick, enabled = enabled), children = children)",
        "androidx.foundation.clickable",
        "androidx.foundation.Box"
    )
)
@Composable
fun Clickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionState: InteractionState? = null,
    children: @Composable () -> Unit
) {
    @Suppress("DEPRECATION")
    PassThroughLayout(
        modifier.clickable(
            enabled,
            onClickLabel,
            tempFunToAvoidCreatingLambdaInsideClickable(interactionState),
            onClick = onClick
        ),
        children
    )
}

// when there is a lambda inside Clickable it is created as a file $Clickable\$2.class which
// conflicts with similar lambda from Modifier.clickable which stored in $clickable\$2.class
// on the case-insensitive FS. proper workaround would be to use different @JvmName on these
// functions but it is currently not supported for composables b/157075847
@Composable
private fun tempFunToAvoidCreatingLambdaInsideClickable(
    interactionState: InteractionState?
): InteractionState {
    return interactionState ?: remember { InteractionState() }
}

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * @sample androidx.ui.foundation.samples.ClickableSample
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param interactionState [InteractionState] that will be updated when this Clickable is
 * pressed, using [Interaction.Pressed]. Only initial (first) press will be recorded and added to
 * [InteractionState]
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [IndicationAmbient] will be used. Pass `null` to show no indication
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
@Composable
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = IndicationAmbient.current(),
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed {
    val semanticModifier = Modifier.semantics(
        properties = {
            this.enabled = enabled
            if (enabled) {
                // b/156468846:  add long click semantics and double click if needed
                onClick(action = { onClick(); return@onClick true }, label = onClickLabel)
            }
        }
    )
    val interactionUpdate =
        if (enabled) {
            Modifier.noConsumptionIndicatorGestureFilter(
                onStart = { interactionState.addInteraction(Interaction.Pressed, it) },
                onStop = { interactionState.removeInteraction(Interaction.Pressed) },
                onCancel = { interactionState.removeInteraction(Interaction.Pressed) }
            )
        } else {
            Modifier
        }
    val tap = if (enabled) tapGestureFilter(onTap = { onClick() }) else Modifier
    val longTap = if (enabled && onLongClick != null) {
        longPressGestureFilter(onLongPress = { onLongClick() })
    } else {
        Modifier
    }
    val doubleTap =
        if (enabled && onDoubleClick != null) {
            doubleTapGestureFilter(onDoubleTap = { onDoubleClick() })
        } else {
            Modifier
        }
    onCommit(interactionState) {
        onDispose {
            interactionState.removeInteraction(Interaction.Pressed)
        }
    }
    semanticModifier
        .plus(interactionUpdate)
        .indication(interactionState, indication)
        .plus(tap)
        .plus(longTap)
        .plus(doubleTap)
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
            changes.fastAny { it.anyPositionChangeConsumed() }
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
