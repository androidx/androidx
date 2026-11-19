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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.IntRange
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEnum
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.intValue
import androidx.compose.remote.creation.compose.util.fastForEach
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember

internal class RemoteStateMachine<T>
internal constructor(val currentState: RemoteInt, val states: List<T>) {

    fun size(): Int {
        return states.size
    }
}

internal class RemoteAllStatesLayoutNode : RemoteComposeNode() {
    lateinit var currentState: RemoteInt

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(remoteCanvas)
        val recordingModifier = scope.toRecordingModifier(modifier)

        creationState.document.startStateLayout(
            recordingModifier,
            creationState.getId(currentState),
        )

        renderChildren(creationState, remoteCanvas)
        creationState.document.endStateLayout()
    }
}

@Suppress("PrimitiveInCollection")
@Composable
@RemoteComposable
internal fun <T> StateLayout(
    stateMachine: RemoteStateMachine<T>,
    modifier: RemoteModifier = RemoteModifier,
    content: @Composable @RemoteComposable (T) -> Unit,
) {
    RemoteComposeNode(
        factory = ::RemoteAllStatesLayoutNode,
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
 * Displays the child layout matching [currentState] across all entries of [T].
 *
 * Interactive example cycling through enum states:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteAllStatesLayoutEnumSample
 *
 * Reducing a larger enum domain to fewer UI states:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteAllStatesLayoutReducedDomainSample
 * * Note: During document creation, [content] runs once for every entry in [T] in declaration order
 *   and emits a separate child subtree per entry. Grouping entries in a `when` branch does not
 *   deduplicate emitted subtrees; document size scales linearly with [T].
 * * Note: When [T] has many entries mapping to fewer distinct UI states, map [currentState] to a
 *   [RemoteBoolean] or a `0 until stateCount` [RemoteInt] using [RemoteEnum.toRemoteInt] or
 *   [RemoteEnum.ordinal].
 *
 * @param currentState [RemoteEnum] selecting the active child layout
 * @param modifier [RemoteModifier] applied to this layout
 * @param content composable invoked once per entry of [T] to build each state's layout
 */
@RemoteComposable
@Composable
public fun <T : Enum<T>> RemoteAllStatesLayout(
    currentState: RemoteEnum<T>,
    modifier: RemoteModifier = RemoteModifier,
    content: @RemoteComposable @Composable (T) -> Unit,
) {
    val stateMachine =
        remember(currentState) {
            RemoteStateMachine(currentState.ordinal, currentState.enumEntries)
        }
    StateLayout(stateMachine, modifier, content)
}

/**
 * Displays the child layout matching [currentState] for both boolean states.
 *
 * Interactive example toggling between boolean states:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteAllStatesLayoutBooleanSample
 * * Note: During document creation, [content] runs twice (`false`, then `true`), emitting one child
 *   subtree per value into the remote document.
 *
 * @param currentState [RemoteBoolean] selecting the active child layout
 * @param modifier [RemoteModifier] applied to this layout
 * @param content composable invoked once for `false` and once for `true`
 */
@RemoteComposable
@Composable
public fun RemoteAllStatesLayout(
    currentState: RemoteBoolean,
    modifier: RemoteModifier = RemoteModifier,
    content: @RemoteComposable @Composable (Boolean) -> Unit,
) {
    val stateMachine =
        remember(currentState) { RemoteStateMachine(currentState.intValue, listOf(false, true)) }
    StateLayout(stateMachine, modifier, content)
}

/**
 * Displays the child layout matching [currentState] across states `0` through `stateCount - 1`.
 *
 * Interactive example cycling through integer states:
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteAllStatesLayoutIntSample
 * * Note: During document creation, [content] runs once for every index in `0 until stateCount`,
 *   emitting a separate child subtree into the remote document for each state. If [currentState]
 *   evaluates outside `0 until stateCount` at runtime, the player falls back to displaying the
 *   first state (`0`).
 *
 * @param currentState [RemoteInt] selecting the active child layout by zero-based index (falls back
 *   to index `0` if out of range at runtime)
 * @param stateCount number of integer states (`0` through `stateCount - 1`), must be positive
 * @param modifier [RemoteModifier] applied to this layout
 * @param content composable invoked once per index in `0 until stateCount` to build each state's
 *   layout
 */
@RemoteComposable
@Composable
public fun RemoteAllStatesLayout(
    currentState: RemoteInt,
    @IntRange(from = 1) stateCount: Int,
    modifier: RemoteModifier = RemoteModifier,
    content: @RemoteComposable @Composable (Int) -> Unit,
) {
    require(stateCount > 0) { "stateCount must be positive, was $stateCount" }
    val stateMachine =
        remember(currentState, stateCount) {
            RemoteStateMachine(currentState, List(stateCount) { it })
        }
    StateLayout(stateMachine, modifier, content)
}
