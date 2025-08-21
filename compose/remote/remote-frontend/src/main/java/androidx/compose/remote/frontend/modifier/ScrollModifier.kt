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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.ScrollModifier as CoreScrollModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

class RemoteScrollState(val position: Float, val notches: Int) {

    fun toComposeUi(): ScrollState {
        return ScrollState(0)
    }
}

@Composable
fun rememberRemoteScrollState(evenNotches: Int = 0): RemoteScrollState {
    val state = LocalRemoteComposeCreationState.current
    val scrollState = remember {
        var positionId = 0f
        if (state !is NoRemoteCompose) {
            positionId = Utils.asNan(state.document.nextId())
        }
        RemoteScrollState(positionId, evenNotches)
    }
    return scrollState
}

data class ScrollModifier(val direction: Int, val state: RemoteScrollState) :
    RemoteModifier.Element {
    override fun toRemoteComposeElement(): RecordingModifier.Element {
        return CoreScrollModifier(direction, state.position, state.notches)
    }

    @Composable
    override fun Modifier.toComposeUi(): Modifier {
        if (direction == CoreScrollModifier.VERTICAL) {
            return verticalScroll(state.toComposeUi())
        } else {
            return horizontalScroll(state.toComposeUi())
        }
    }
}

@Composable
fun RemoteModifier.verticalScroll(state: RemoteScrollState): RemoteModifier {
    return this.then(ScrollModifier(CoreScrollModifier.VERTICAL, state))
}

fun RemoteModifier.horizontalScroll(state: RemoteScrollState): RemoteModifier =
    this.then(ScrollModifier(CoreScrollModifier.HORIZONTAL, state))
