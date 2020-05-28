/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.foundation.selection

import androidx.compose.Composable
import androidx.compose.onCommit
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.composed
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Indication
import androidx.ui.foundation.IndicationAmbient
import androidx.ui.foundation.Interaction
import androidx.ui.foundation.InteractionState
import androidx.ui.foundation.Strings
import androidx.ui.foundation.indication
import androidx.ui.foundation.selection.ToggleableState.Indeterminate
import androidx.ui.foundation.selection.ToggleableState.Off
import androidx.ui.foundation.selection.ToggleableState.On
import androidx.ui.foundation.semantics.toggleableState
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.accessibilityValue
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick

/**
 * Combines [tapGestureFilter] and [Semantics] for the components that need to be
 * toggleable, like Switch.
 *
 * @see [TriStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 * @param enabled whether or not this [Toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param modifier allows to provide a modifier to be added before the gesture detector, for
 * example Ripple should be added at this point. this will be easier once we migrate this
 * function to a Modifier
 * @Deprecated Use [Modifier.toggleable] instead.
 */
@Deprecated(
    "This component has been deprecated. Use [Modifier.toggleable] instead.",
    ReplaceWith(
        "Box(modifier.toggleable(" +
                "value = value," +
                " onValueChange = onValueChange," +
                " enabled = enabled" +
                "), children = children)",
        "androidx.foundation.toggleable",
        "androidx.foundation.Box"
    )
)
@Composable
fun Toggleable(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    @Suppress("DEPRECATION")
    PassThroughLayout(
        modifier.toggleable(value = value, onValueChange = onValueChange, enabled = enabled),
        children
    )
}

/**
 * Configure component to make it toggleable via input and accessibility events
 *
 * @sample androidx.ui.foundation.samples.ToggleableSample
 *
 * @see [Modifier.triStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param onValueChange callback to be invoked when toggleable is clicked,
 * therefore the change of the state in requested.
 * @param enabled whether or not this [toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param interactionState [InteractionState] that will be updated when this toggleable is
 * pressed, using [Interaction.Pressed]
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [IndicationAmbient] will be used. Pass `null` to show no indication
 */
@Composable
fun Modifier.toggleable(
    value: Boolean,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = IndicationAmbient.current(),
    onValueChange: (Boolean) -> Unit
) = triStateToggleable(
    state = ToggleableState(value),
    onClick = { onValueChange(!value) },
    enabled = enabled,
    interactionState = interactionState,
    indication = indication
)

/**
 * Combines [tapGestureFilter] and [Semantics] for the components with three states
 * like TriStateCheckbox.
 *
 * It supports three states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are
 * dependent Toggleables associated to this component and those can have different values.
 *
 * @see [Toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param onClick will be called when user toggles the toggleable.
 * @param enabled whether or not this [TriStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param modifier allows to provide a modifier to be added before the gesture detector, for
 * example Ripple should be added at this point. this will be easier once we migrate this
 * function to a Modifier
 *
 * @Deprecated Use [Modifier.triStateToggleable] instead.
 */
@Deprecated(
    "This component has been deprecated. Use [Modifier.triStateToggleable] instead.",
    ReplaceWith(
        "Box(modifier.triStateToggleable(" +
                "state = state," +
                " onClick = onClick," +
                " enabled = enabled" +
                "), children = children)",
        "androidx.foundation.triStateToggleable",
        "androidx.foundation.Box"
    )
)
@Composable
fun TriStateToggleable(
    state: ToggleableState = On,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    @Suppress("DEPRECATION")
    PassThroughLayout(
        modifier.triStateToggleable(state = state, onClick = onClick, enabled = enabled),
        children
    )
}

/**
 * Configure component to make it toggleable via input and accessibility events with three
 * states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are dependent Toggleables associated to this
 * component and those can have different values.
 *
 * @sample androidx.ui.foundation.samples.TriStateToggleableSample
 *
 * @see [Modifier.toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param onClick will be called when user clicks the toggleable.
 * @param enabled whether or not this [triStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param interactionState [InteractionState] that will be updated when this toggleable is
 * pressed, using [Interaction.Pressed]
 * @param indication indication to be shown when modified element is pressed. Be default,
 * indication from [IndicationAmbient] will be used. Pass `null` to show no indication
 */
@Composable
fun Modifier.triStateToggleable(
    state: ToggleableState,
    enabled: Boolean = true,
    interactionState: InteractionState = remember { InteractionState() },
    indication: Indication? = IndicationAmbient.current(),
    onClick: () -> Unit
) = composed {
    // TODO(pavlis): Handle multiple states for Semantics
    val semantics = Modifier.semantics(
        properties = {
            this.accessibilityValue = when (state) {
                // TODO(ryanmentley): These should be set by Checkbox, Switch, etc.
                On -> Strings.Checked
                Off -> Strings.Unchecked
                Indeterminate -> Strings.Indeterminate
            }
            this.toggleableState = state
            this.enabled = enabled

            if (enabled) {
                onClick(action = { onClick(); return@onClick true }, label = "Toggle")
            }
        }
    )
    val interactionUpdate =
        if (enabled) {
            Modifier.pressIndicatorGestureFilter(
                onStart = { interactionState.addInteraction(Interaction.Pressed, it) },
                onStop = { interactionState.removeInteraction(Interaction.Pressed) },
                onCancel = { interactionState.removeInteraction(Interaction.Pressed) }
            )
        } else {
            Modifier
        }
    val click = if (enabled) Modifier.tapGestureFilter { onClick() } else Modifier

    onCommit(interactionState) {
        onDispose {
            interactionState.removeInteraction(Interaction.Pressed)
        }
    }
    semantics
        .indication(interactionState, indication)
        .plus(interactionUpdate)
        .plus(click)
}

/**
 * Enum that represents possible toggleable states.
 */
enum class ToggleableState {
    /**
     * State that means a component is on
     */
    On,
    /**
     * State that means a component is off
     */
    Off,
    /**
     * State that means that on/off value of a component cannot be determined
     */
    Indeterminate
}

/**
 * Return corresponding ToggleableState based on a Boolean representation
 *
 * @param value whether the ToggleableState is on or off
 */
fun ToggleableState(value: Boolean) = if (value) On else Off