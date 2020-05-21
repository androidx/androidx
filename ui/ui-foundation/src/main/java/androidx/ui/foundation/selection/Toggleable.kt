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
import androidx.ui.core.Modifier
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.foundation.Strings
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
 * @sample androidx.ui.foundation.samples.ToggleableSample
 *
 * @see [TriStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param onValueChange callback to be invoked when toggleable is being clicked,
 * therefore the change of the state in requested.
 * @param enabled enabled whether or not this [Toggleable] will handle input events and appear
 * enabled for semantics purposes
 * @param modifier allows to provide a modifier to be added before the gesture detector, for
 * example Ripple should be added at this point. this will be easier once we migrate this
 * function to a Modifier
 */
@Composable
fun Toggleable(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    TriStateToggleable(
        state = ToggleableState(value),
        onClick = { onValueChange(!value) },
        enabled = enabled,
        modifier = modifier,
        children = children
    )
}

/**
 * Combines [tapGestureFilter] and [Semantics] for the components with three states
 * like TriStateCheckbox.
 *
 * It supports three states: On, Off and Indeterminate.
 *
 * TriStateToggleable should be used when there are
 * dependent Toggleables associated to this component and those can have different values.
 *
 * @sample androidx.ui.foundation.samples.TriStateToggleableSample
 *
 * @see [Toggleable] if you want to support only two states: on and off
 *
 * @param state current value for the component
 * @param onClick will be called when user toggles the toggleable.
 * @param enabled enabled whether or not this [TriStateToggleable] will handle input events and
 * appear enabled for semantics purposes
 * @param modifier allows to provide a modifier to be added before the gesture detector, for
 * example Ripple should be added at this point. this will be easier once we migrate this
 * function to a Modifier
 */
@Composable
fun TriStateToggleable(
    state: ToggleableState = On,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit
) {
    // TODO(pavlis): Handle multiple states for Semantics
    Semantics(container = true, properties = {
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
    }) {
        // TODO(b/150706555): This layout is temporary and should be removed once Semantics
        //  is implemented with modifiers.
        @Suppress("DEPRECATION")
        PassThroughLayout(modifier.tapGestureFilter { onClick() }, children)
    }
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