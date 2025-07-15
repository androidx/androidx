/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.selection

import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
fun Modifier.toggleable(
    value: Boolean,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onValueChange: (Boolean) -> Unit
): Modifier = toggleable(
    value = value,
    interactionSource = interactionSource as MutableInteractionSource?,
    indication = indication,
    enabled = enabled,
    role = role,
    onValueChange = onValueChange
)

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
fun Modifier.triStateToggleable(
    state: ToggleableState,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier = triStateToggleable(
    state = state,
    interactionSource = interactionSource as MutableInteractionSource?,
    indication = indication,
    enabled = enabled,
    role = role,
    onClick = onClick
)
