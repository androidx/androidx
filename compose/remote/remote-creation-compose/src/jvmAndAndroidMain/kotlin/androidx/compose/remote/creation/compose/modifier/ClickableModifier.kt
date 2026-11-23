/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.modifier

import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.util.fastMap
import androidx.compose.remote.creation.modifiers.ClickActionModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Stable

internal data class ClickableModifier(
    public val actions: List<RemoteAction>,
    public val clickType: Int = MultiClickModifier.CLICK_TYPE_SINGLE,
) : RemoteModifier.Element {
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return ClickActionModifier(
            actions.fastMap { action -> with(action) { toRemoteAction() } },
            clickType,
        )
    }
}

// TODO provide an onClickLabel
/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * Example of toggling remote state when an element is clicked:
 *
 * @sample androidx.compose.remote.creation.compose.samples.ClickableValueChangeSample
 * @param onClick will be called when user clicks on the element
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services. Non-constant values currently default to `true`.
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations. By default, no role is set (`null`).
 * @return a [RemoteModifier] that makes the component clickable
 */
@Stable
public fun RemoteModifier.clickable(
    onClick: RemoteAction,
    enabled: RemoteBoolean = true.rb,
    role: RemoteRole? = null,
): RemoteModifier =
    then(
            if (enabled.constantValueOrNull == null || enabled.constantValue) {
                val actions =
                    when (onClick) {
                        is CombinedAction -> onClick.actions.toList()
                        else -> listOfNotNull(onClick)
                    }
                if (actions.isNotEmpty()) ClickableModifier(actions) else RemoteModifier
            } else {
                RemoteModifier
            }
        )
        .then(
            if (role != null)
                RemoteModifier.semantics {
                    this.role = role
                    this.isEnabled = enabled
                }
            else RemoteModifier
        )
