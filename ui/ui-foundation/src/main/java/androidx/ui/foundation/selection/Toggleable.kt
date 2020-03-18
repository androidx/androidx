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
import androidx.ui.core.PassThroughLayout
import androidx.ui.core.gesture.TapGestureDetector
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
 * Combines [TapGestureDetector] and [Semantics] for the components that need to be
 * toggleable, like Switch.
 *
 * @sample androidx.ui.foundation.samples.ToggleableSample
 *
 * @see [TriStateToggleable] if you require support for an indeterminate state.
 *
 * @param value whether Toggleable is on or off
 * @param onValueChange callback to be invoked when toggleable is being clicked,
 * therefore the change of the state in requested.
 * If null, Toggleable will appear in the [value] state and remains disabled
 */
@Composable
fun Toggleable(
    value: Boolean,
    onValueChange: ((Boolean) -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    TriStateToggleable(
        value = ToggleableState(value),
        onToggle = onValueChange?.let { { it(!value) } },
        children = children
    )
}

/**
 * Combines [TapGestureDetector] and [Semantics] for the components with three states
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
 * @param value current value for the component
 * @param onToggle will be called when user toggles the toggleable. The children will not be
 *  toggleable when it is null.
 */
@Composable
fun TriStateToggleable(
    value: ToggleableState = ToggleableState.On,
    onToggle: (() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
        // TODO(pavlis): Handle multiple states for Semantics
        Semantics(container = true, properties = {
            this.accessibilityValue = when (value) {
                // TODO(ryanmentley): These should be set by Checkbox, Switch, etc.
                ToggleableState.On -> Strings.Checked
                ToggleableState.Off -> Strings.Unchecked
                ToggleableState.Indeterminate -> Strings.Indeterminate
            }
            this.toggleableState = value
            this.enabled = onToggle != null

            if (onToggle != null) {
                onClick(action = onToggle, label = "Toggle")
            }
        }) {
            // TODO(b/150706555): This layout is temporary and should be removed once Semantics
            //  is implemented with modifiers.
            @Suppress("DEPRECATION")
            PassThroughLayout(TapGestureDetector(onToggle), children)
        }
}

/**
 * Enum that represents possible toggleable states.
 * @property On components is on
 * @property Off components is off
 * @property Indeterminate means that on/off value cannot be determined
 */
enum class ToggleableState {
    On,
    Off,
    Indeterminate
}

/**
 * Return corresponding ToggleableState based on a Boolean representation
 *
 * @param value whether the ToggleableState is on or off
 */
fun ToggleableState(value: Boolean) =
    if (value) ToggleableState.On else ToggleableState.Off