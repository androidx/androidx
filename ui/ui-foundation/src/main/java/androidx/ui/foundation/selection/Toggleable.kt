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
import androidx.compose.composer
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.foundation.Strings
import androidx.ui.foundation.semantics.toggleableState
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.enabled
import androidx.ui.semantics.onClick
import androidx.ui.semantics.accessibilityValue

@Composable
fun Toggleable(
    value: ToggleableState = ToggleableState.Checked,
    onToggle: (() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    PressReleasedGestureDetector(
        onRelease = onToggle,
        consumeDownOnStart = false
    ) {
        // TODO: enabled should not be hardcoded
        // TODO(pavlis): Handle multiple states for Semantics
        Semantics(properties = {
            this.accessibilityValue = when (value) {
                // TODO(ryanmentley): These should be set by Checkbox, Switch, etc.
                ToggleableState.Checked -> Strings.Checked
                ToggleableState.Unchecked -> Strings.Unchecked
                ToggleableState.Indeterminate -> Strings.Indeterminate
            }
            this.toggleableState = value
            this.enabled = true

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