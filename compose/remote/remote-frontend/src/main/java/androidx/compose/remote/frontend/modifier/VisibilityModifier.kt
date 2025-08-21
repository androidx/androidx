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

import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.state.RemoteInt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

class VisibilityModifier(val visible: RemoteInt) : RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        val id = visible.getIntId()
        return androidx.compose.remote.creation.modifiers.VisibilityModifier(id)
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        return layout { measurable, constraints ->
            if (visible.value == Component.Visibility.VISIBLE) {
                val placeable = measurable.measure(constraints = constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            } else {
                layout(0, 0) {}
            }
        }
    }
}

// TODO make RemoteInt a State internally so this is just RemoteInt
fun RemoteModifier.visibility(visible: RemoteInt): RemoteModifier =
    then(VisibilityModifier(visible))
