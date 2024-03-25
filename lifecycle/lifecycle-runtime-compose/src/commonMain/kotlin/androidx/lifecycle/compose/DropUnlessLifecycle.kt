/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.compose

import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner

/**
 * Returns a new decorated function that will invoke the given [block] if the [lifecycleOwner]'s
 * lifecycle state is at least [state]. Otherwise, [block] is not invoked.
 *
 * Note this function should **not** be used to target the [State.DESTROYED] because
 * Compose stops recomposing after receiving a [Lifecycle.Event.ON_STOP] and will never be
 * aware of an [State.DESTROYED] state.
 *
 * @param state The target [Lifecycle.State] to match.
 * @param lifecycleOwner The [LifecycleOwner] whose lifecycle will be observed. Defaults to the
 *  current [LocalLifecycleOwner].
 * @param block The callback to be executed when the observed lifecycle state is at least [state].
 *
 * @return A decorated function that invoke [block] only if the lifecycle state is at least [state].
 *
 * @throws IllegalArgumentException if attempting to target state [State.DESTROYED].
 *
 * @see dropUnlessStarted
 * @see dropUnlessResumed
 */
@CheckResult
@Composable
private fun dropUnlessStateIsAtLeast(
    state: State,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: () -> Unit,
): () -> Unit {
    require(state != State.DESTROYED) {
        "Target state is not allowed to be `Lifecycle.State.DESTROYED` because Compose disposes " +
            "of the composition before `Lifecycle.Event.ON_DESTROY` observers are invoked."
    }

    return {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(state)) {
            block()
        }
    }
}

/**
 * Returns a new decorated function that will invoke the given [block] if the [lifecycleOwner]'s
 * lifecycle state is at least [State.STARTED]. Otherwise, [block] is not invoked.
 *
 * @param lifecycleOwner The [LifecycleOwner] whose lifecycle will be observed. Defaults to the
 *  current [LocalLifecycleOwner].
 * @param block The callback to be executed when the observed lifecycle state is at least
 *  [State.STARTED].
 *
 * @return A decorated function that invoke [block] only if the lifecycle state is at least
 *  [State.STARTED].
 *
 * @sample androidx.lifecycle.compose.samples.DropUnlessStarted
 */
@CheckResult
@Composable
fun dropUnlessStarted(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: () -> Unit,
): () -> Unit = dropUnlessStateIsAtLeast(State.STARTED, lifecycleOwner, block)

/**
 * Returns a new decorated function that will invoke the given [block] if the [lifecycleOwner]'s
 * lifecycle state is at least [State.RESUMED]. Otherwise, [block] is not invoked.
 *
 * For Navigation users, it's recommended to safeguard navigate methods when using them while a
 * composable is in transition as a result of navigation.
 *
 * @param lifecycleOwner The [LifecycleOwner] whose lifecycle will be observed. Defaults to the
 *  current [LocalLifecycleOwner].
 * @param block The callback to be executed when the observed lifecycle state is at least
 *  [State.RESUMED].
 *
 * @return A decorated function that invoke [block] only if the lifecycle state is at least
 *  [State.RESUMED].
 *
 * @sample androidx.lifecycle.compose.samples.DropUnlessResumed
 */
@CheckResult
@Composable
fun dropUnlessResumed(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    block: () -> Unit,
): () -> Unit = dropUnlessStateIsAtLeast(State.RESUMED, lifecycleOwner, block)
