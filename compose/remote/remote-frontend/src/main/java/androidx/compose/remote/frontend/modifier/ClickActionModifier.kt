/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.modifier

import androidx.compose.foundation.clickable
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.action.Action
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

class ClickActionModifier(val actions: List<Action>) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return androidx.compose.remote.creation.modifiers.ClickActionModifier(
            actions.map { it.toRemoteAction() }
        )
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        val previewActions = actions.map { it.toComposeUiAction() }

        return clickable { previewActions.forEach { action -> action.invoke() } }
    }
}

// TODO provide an onClickLabel
fun RemoteModifier.onClick(vararg actions: Action, role: Role? = Role.Button): RemoteModifier =
    then(ClickActionModifier(actions.toList()))
        .then(if (role != null) RemoteModifier.semantics { this.role = role } else RemoteModifier)

// TODO provide an onClickLabel
fun RemoteModifier.onClick(actions: List<Action>, role: Role? = Role.Button): RemoteModifier =
    then(ClickActionModifier(actions))
        .then(if (role != null) RemoteModifier.semantics { this.role = role } else RemoteModifier)
