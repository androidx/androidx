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
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.semantics.Role

internal class ClickableModifier(
    public val actions: List<Action>,
    public val clickType: Int = MultiClickModifier.CLICK_TYPE_SINGLE,
) : RemoteModifier.Element {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun RemoteStateScope.toRecordingModifierElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.ClickActionModifier(
            @Suppress("ListIterator") actions.map { action -> with(action) { toRemoteAction() } },
            clickType,
        )
    }
}

// TODO provide an onClickLabel
public fun RemoteModifier.clickable(
    action: Action? = null,
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
