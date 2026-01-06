/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.compose.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A [produceState] that is lifecycle-aware.
 *
 * [producer] is launched when the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [produceStateWithLifecycle] is recomposed with a different [lifecycle]
 * or [minActiveState].
 *
 * @see androidx.compose.runtime.produceState
 */
@Composable
fun <T> produceStateWithLifecycle(
    initialValue: T,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffectWithLifecycle(lifecycle = lifecycle, minActiveState = minActiveState) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * A [produceState] that is lifecycle-aware.
 *
 * [producer] is launched when the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [produceStateWithLifecycle] is recomposed with a different [key1],
 * [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.produceState
 */
@Composable
fun <T> produceStateWithLifecycle(
    initialValue: T,
    key1: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffectWithLifecycle(key1, lifecycle = lifecycle, minActiveState = minActiveState) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * A [produceState] that is lifecycle-aware.
 *
 * [producer] is launched when the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [produceStateWithLifecycle] is recomposed with a different [key1],
 * [key2], [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.produceState
 */
@Composable
fun <T> produceStateWithLifecycle(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffectWithLifecycle(
        key1,
        key2,
        lifecycle = lifecycle,
        minActiveState = minActiveState,
    ) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * A [produceState] that is lifecycle-aware.
 *
 * [producer] is launched when the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [produceStateWithLifecycle] is recomposed with a different [key1],
 * [key2], [key3], [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.produceState
 */
@Composable
fun <T> produceStateWithLifecycle(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    key3: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffectWithLifecycle(
        key1,
        key2,
        key3,
        lifecycle = lifecycle,
        minActiveState = minActiveState,
    ) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * A [produceState] that is lifecycle-aware.
 *
 * [producer] is launched when the [lifecycle] reaches the [minActiveState], and will be
 * **re-launched** whenever [produceStateWithLifecycle] is recomposed with a different [keys],
 * [lifecycle] or [minActiveState].
 *
 * @see androidx.compose.runtime.produceState
 */
@Composable
fun <T> produceStateWithLifecycle(
    initialValue: T,
    vararg keys: Any?,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffectWithLifecycle(
        keys = keys,
        lifecycle = lifecycle,
        minActiveState = minActiveState,
    ) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

private class ProduceStateScopeImpl<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext,
) : ProduceStateScope<T>, MutableState<T> by state {
    override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
        try {
            suspendCancellableCoroutine<Nothing> {}
        } finally {
            onDispose()
        }
    }
}
