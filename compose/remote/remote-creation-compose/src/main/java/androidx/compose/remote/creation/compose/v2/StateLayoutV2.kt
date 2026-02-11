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

package androidx.compose.remote.creation.compose.v2

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.StateMachineSpec
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.util.fastForEach

@SuppressLint("PrimitiveInCollection")
@Composable
@RemoteComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun StateLayoutV2(
    stateMachine: StateMachineSpec,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable @RemoteComposable (Int) -> Unit,
) {
    RemoteComposeNode(
        factory = ::RemoteStateLayoutNodeV2,
        update = {
            set(modifier) { nodeModifier -> this.modifier = nodeModifier }
            set(stateMachine.currentState) { state -> this.currentState = state }
        },
        content = {
            val states = stateMachine.states
            states.fastForEach { state -> key(state) { content(state) } }
        },
    )
}
