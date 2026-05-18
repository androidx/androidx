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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.layout.MultiClickModifier
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.CombinedAction
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.ClickActionModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.semantics.Role

internal class ClickableModifier(
    public val actions: List<Action>,
    public val clickType: Int = MultiClickModifier.CLICK_TYPE_SINGLE,
) : RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return ClickActionModifier(
            @Suppress("ListIterator")
            actions.mapNotNull { action ->
                if (action is RemoteAction) with(action) { toRemoteAction() } else null
            },
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
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * @param action will be called when user clicks on the element
 * @param enabled Controls the enabled state. When `false`, [action], and this modifier will appear
 *   disabled for accessibility services
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 */
public fun RemoteModifier.clickable(
    action: Action,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): RemoteModifier =
    then(
            if (enabled) {
                val actions =
                    when (action) {
                        is CombinedAction -> action.actions.toList()
                        else -> listOfNotNull(action)
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
                    this.enabled = enabled
                }
            else RemoteModifier
        )
