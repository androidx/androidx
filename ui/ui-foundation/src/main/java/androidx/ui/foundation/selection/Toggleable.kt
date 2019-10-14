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
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.foundation.Strings
import androidx.ui.foundation.semantics.toggleableState
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick
import androidx.ui.semantics.accessibilityValue

/**
 * Combines [PressReleasedGestureDetector] and [Semantics] for the components that need to be
 * toggleable, like Switch.
 *
 * @sample androidx.ui.foundation.samples.ToggleableSample
 *
 * @see [TriStateToggleable] if you require support for an indeterminate state.
 *
 * @param checked whether Toggleable is checked or unchecked
 * @param onCheckedChange callback to be invoked when toggleable is being clicked,
 * therefore the change of checked state in requested.
 * If null, Toggleable will appears in the [checked] state and remains disabled
 */
@Composable
fun Toggleable(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    TriStateToggleable(
        value = ToggleableState(checked),
        onToggle = onCheckedChange?.let { { it(!checked) } },
        children = children
    )
}
/**
 * Combines [PressReleasedGestureDetector] and [Semantics] for the components with three states
 * like Checkbox.
 *
 * It supports three states: checked, unchecked and indeterminate.
 *
 * TriStateToggleable should be used when there are
 * dependent Toggleables associated to this component and those can have different values.
 *
 * @sample androidx.ui.foundation.samples.TriStateToggleableSample
 *
 * @see [Toggleable] if you want to support only two states: checked and unchecked
 *
 * @param value current value for the component
 * @param onToggle will be called when user toggles the toggleable. The children will not be
 *  toggleable when it is null.
 */
@Composable
fun TriStateToggleable(
    value: ToggleableState = ToggleableState.Checked,
    onToggle: (() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    PressReleasedGestureDetector(
        onRelease = onToggle,
        consumeDownOnStart = false
    ) {
        // TODO(pavlis): Handle multiple states for Semantics
        Semantics(properties = {
            this.accessibilityValue = when (value) {
                // TODO(ryanmentley): These should be set by Checkbox, Switch, etc.
                ToggleableState.Checked -> Strings.Checked
                ToggleableState.Unchecked -> Strings.Unchecked
                ToggleableState.Indeterminate -> Strings.Indeterminate
            }
            this.toggleableState = value
            this.enabled = onToggle != null

            if (onToggle != null) {
                onClick(action = onToggle, label = "Toggle")
            }
        }) {
            children()
        }
    }
}

// TODO: These shouldn't use checkbox-specific language
enum class ToggleableState {
    Checked,
    Unchecked,
    Indeterminate
}

fun ToggleableState(checked: Boolean) = when (checked) {
    true -> ToggleableState.Checked
    false -> ToggleableState.Unchecked
}