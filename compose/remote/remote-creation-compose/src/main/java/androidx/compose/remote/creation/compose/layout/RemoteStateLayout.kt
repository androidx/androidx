/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not uses this file except in compliance with the License.
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

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.util.fastForEach

internal class RemoteStateMachine<T>
internal constructor(val currentState: RemoteInt, val states: List<T>) {

    fun size(): Int {
        return states.size
    }
}

internal class RemoteStateLayoutNode : RemoteComposeNode() {
    lateinit var currentState: RemoteInt

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val recordingModifier = creationState.toRecordingModifier(modifier)

        creationState.document.startStateLayout(
            recordingModifier,
            currentState.getIdForCreationState(creationState),
        )

        renderChildren(creationState, remoteCanvas)
        creationState.document.endStateLayout()
    }
}

@SuppressLint("PrimitiveInCollection")
@Composable
@RemoteComposable
internal fun <T> StateLayout(
    stateMachine: RemoteStateMachine<T>,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable @RemoteComposable (T) -> Unit,
) {
    RemoteComposeNode(
        factory = ::RemoteStateLayoutNode,
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

/**
 * A layout that displays content based on the current value of a [RemoteEnum] state.
 *
 * @param state The [RemoteEnum] state that determines which content to display.
 * @param modifier The [RemoteModifier] to be applied to the layout.
 * @param content The composable content to be displayed for each enum state.
 */
@RemoteComposable
@Composable
public fun <T : Enum<T>> RemoteStateLayout(
    state: RemoteEnum<T>,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (T) -> Unit,
) {
    val stateMachine = remember { RemoteStateMachine(state.intValue, state.enumEntries) }
    StateLayout(stateMachine, modifier, content)
}

/**
 * A layout that displays content based on the current value of a [RemoteBoolean] state.
 *
 * @param state The [RemoteBoolean] state that determines which content to display.
 * @param modifier The [RemoteModifier] to be applied to the layout.
 * @param content The composable content to be displayed for the boolean state.
 */
@RemoteComposable
@Composable
public fun RemoteStateLayout(
    state: RemoteBoolean,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (Boolean) -> Unit,
) {
    val stateMachine = remember { RemoteStateMachine(state.intValue, listOf(false, true)) }
    StateLayout(stateMachine, modifier, content)
}

/**
 * A layout that displays content based on the current value of a [RemoteInt] state.
 *
 * @param state The [RemoteInt] state that determines which content to display.
 * @param states The set of possible integer states.
 * @param modifier The [RemoteModifier] to be applied to the layout.
 * @param content The composable content to be displayed for each integer state.
 */
@RemoteComposable
@Composable
public fun RemoteStateLayout(
    state: RemoteInt,
    vararg states: Int,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (Int) -> Unit,
) {
    val stateMachine = remember { RemoteStateMachine(state, states.sorted()) }
    StateLayout(stateMachine, modifier, content)
}
