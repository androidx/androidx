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

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
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
        val scope = overriddenScope(creationState)
        val recordingModifier = scope.toRecordingModifier(modifier)

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
 * A layout that manages and displays multiple states defined by a [RemoteEnum].
 *
 * This component ensures that all possible states defined in the [RemoteEnum] are composed, while
 * the underlying remote rendering system handles the visibility and transitions between them based
 * on the [currentState].
 *
 * @param currentState The state machine governing the available states and the current active
 *   state.
 * @param modifier The [RemoteModifier] to be applied to this layout.
 * @param content A composable lambda that defines the UI for each state [T].
 */
@RemoteComposable
@Composable
public fun <T : Enum<T>> RemoteStateLayout(
    currentState: RemoteEnum<T>,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (T) -> Unit,
) {
    val stateMachine = remember {
        RemoteStateMachine(currentState.intValue, currentState.enumEntries)
    }
    StateLayout(stateMachine, modifier, content)
}

/**
 * A layout that manages and displays two states defined by a [RemoteBoolean].
 *
 * This component ensures that both possible states are composed, while the underlying remote
 * rendering system handles the visibility and transitions between them based on the [currentState].
 *
 * @param currentState The state machine governing the available states and the current active
 *   state.
 * @param modifier The [RemoteModifier] to be applied to this layout.
 * @param content A composable lambda that defines the UI for each state [Boolean].
 */
@RemoteComposable
@Composable
public fun RemoteStateLayout(
    currentState: RemoteBoolean,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (Boolean) -> Unit,
) {
    val stateMachine = remember { RemoteStateMachine(currentState.intValue, listOf(false, true)) }
    StateLayout(stateMachine, modifier, content)
}

/**
 * A layout that manages and displays multiple states defined by a [RemoteInt].
 *
 * This component ensures that all possible states defined in [states] are composed, while the
 * underlying remote rendering system handles the visibility and transitions between them based on
 * the [RemoteStateMachine.currentState].
 *
 * @param currentState The state machine governing the available states and the current active
 *   state.
 * @param states The list of integer states that this layout can display.
 * @param modifier The [RemoteModifier] to be applied to this layout.
 * @param content A composable lambda that defines the UI for each state [Int].
 */
@RemoteComposable
@Composable
public fun RemoteStateLayout(
    currentState: RemoteInt,
    vararg states: Int,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable (Int) -> Unit,
) {
    val stateMachine = remember { RemoteStateMachine(currentState, states.sorted()) }
    StateLayout(stateMachine, modifier, content)
}
