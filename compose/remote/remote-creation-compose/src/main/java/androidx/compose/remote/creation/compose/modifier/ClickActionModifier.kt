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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.semantics.Role

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ClickActionModifier(public val actions: List<Action>) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.ClickActionModifier(
            actions.map { it.toRemoteAction() }
        )
    }
}

// TODO provide an onClickLabel
public fun RemoteModifier.clickable(
    vararg actions: Action,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): RemoteModifier = clickable(actions.toList(), enabled, role)

// TODO provide an onClickLabel
public fun RemoteModifier.clickable(
    actions: List<Action>,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): RemoteModifier =
    then(if (enabled) ClickActionModifier(actions) else RemoteModifier)
        .then(
            if (role != null)
                RemoteModifier.semantics {
                    this.role = role
                    this.enabled = enabled
                }
            else RemoteModifier
        )
