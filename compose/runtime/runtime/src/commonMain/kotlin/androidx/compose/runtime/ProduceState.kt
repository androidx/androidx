/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

/** Receiver scope for use with [produceState]. */
public interface ProduceStateScope<T> : MutableState<T>, CoroutineScope {
    /**
     * Await the disposal of this producer whether it left the composition, the source changed, or
     * an error occurred. Always runs [onDispose] before resuming.
     *
     * This method is useful when configuring callback-based state producers that do not suspend,
     * for example:
     *
     * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
     */
    public suspend fun awaitDispose(onDispose: () -> Unit): Nothing
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

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time without a defined data source.
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. [producer] should use [ProduceStateScope.value] to set new
 * values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession. This can be changed by providing
 * a [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(Unit) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time without a defined data source.
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. [producer] should use [ProduceStateScope.value] to set new
 * values on the returned [State].
 *
 * The given [mutationPolicy] is used to control how changes are reported and merged in the returned
 * state. If not specified, the default behavior is [structuralEqualityPolicy], which conflates
 * values that are [equal][Any.equals] to each other. Note observers may only see the latest value
 * if several values are set in rapid succession. This is especially true when reading the returned
 * state in composition, as composition executes with the latest values from a snapshot rather than
 * composing once with each intermediate value of a state.
 *
 * The [mutationPolicy] is only used when initializing the underlying state object. If the
 * [mutationPolicy] changes after creating the state, the state and the producer are unaffected and
 * will continue collecting in the previously returned state with the original
 * [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param mutationPolicy A policy used to control how changes are handled in the returned state
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    mutationPolicy: SnapshotMutationPolicy<T>,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue, mutationPolicy) }
    LaunchedEffect(Unit) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1] changes, a running [producer] will be cancelled
 * and re-launched for the new source. [producer] should use [ProduceStateScope.value] to set new
 * values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession. This can be changed by providing
 * a [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param key1 A key that, when changed, will restart the [producer] lambda
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1] changes, a running [producer] will be cancelled
 * and re-launched for the new source. [producer] should use [ProduceStateScope.value] to set new
 * values on the returned [State].
 *
 * The given [mutationPolicy] is used to control how changes are reported and merged in the returned
 * state. If not specified, the default behavior is [structuralEqualityPolicy], which conflates
 * values that are [equal][Any.equals] to each other. Note observers may only see the latest value
 * if several values are set in rapid succession. This is especially true when reading the returned
 * state in composition, as composition executes with the latest values from a snapshot rather than
 * composing once with each intermediate value of a state.
 *
 * Changes to the [mutationPolicy] and [initialValue] are ignored.
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param key1 A key that, when changed, will restart the [producer] lambda
 * @param mutationPolicy A policy used to control how changes are handled in the returned state
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    mutationPolicy: SnapshotMutationPolicy<T>,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue, mutationPolicy) }
    LaunchedEffect(key1) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1] and [key2].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1] or [key2] change, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession. This can be changed by providing
 * a [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param key1 A key that, when changed, will restart the [producer] lambda
 * @param key2 A key that, when changed, will restart the [producer] lambda
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1, key2) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1] and [key2].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1] or [key2] change, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * The given [mutationPolicy] is used to control how changes are reported and merged in the returned
 * state. If not specified, the default behavior is [structuralEqualityPolicy], which conflates
 * values that are [equal][Any.equals] to each other. Note observers may only see the latest value
 * if several values are set in rapid succession. This is especially true when reading the returned
 * state in composition, as composition executes with the latest values from a snapshot rather than
 * composing once with each intermediate value of a state.
 *
 * The [mutationPolicy] is only used when initializing the underlying state object. If the
 * [mutationPolicy] changes after creating the state, the state and the producer are unaffected and
 * will continue collecting in the previously returned state with the original
 * [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param key1 A key that, when changed, will restart the [producer] lambda
 * @param key2 A key that, when changed, will restart the [producer] lambda
 * @param mutationPolicy A policy used to control how changes are handled in the returned state
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    mutationPolicy: SnapshotMutationPolicy<T>,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue, mutationPolicy) }
    LaunchedEffect(key1, key2) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1], [key2] and [key3].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1], [key2] or [key3] change, a running [producer]
 * will be cancelled and re-launched for the new source. [producer] should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession. This can be changed by providing
 * a [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param key1 A key that, when changed, will restart the [producer] lambda
 * @param key2 A key that, when changed, will restart the [producer] lambda
 * @param key3 A key that, when changed, will restart the [producer] lambda
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    key3: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1, key2, key3) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1], [key2] and [key3].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1], [key2] or [key3] change, a running [producer]
 * will be cancelled and re-launched for the new source. [producer] should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The given [mutationPolicy] is used to control how changes are reported and merged in the returned
 * state. If not specified, the default behavior is [structuralEqualityPolicy], which conflates
 * values that are [equal][Any.equals] to each other. Note observers may only see the latest value
 * if several values are set in rapid succession. This is especially true when reading the returned
 * state in composition, as composition executes with the latest values from a snapshot rather than
 * composing once with each intermediate value of a state.
 *
 * The [mutationPolicy] is only used when initializing the underlying state object. If the
 * [mutationPolicy] changes after creating the state, the state and the producer are unaffected and
 * will continue collecting in the previously returned state with the original
 * [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param key1 A key that, when changed, will restart the [producer] lambda
 * @param key2 A key that, when changed, will restart the [producer] lambda
 * @param key3 A key that, when changed, will restart the [producer] lambda
 * @param mutationPolicy A policy used to control how changes are handled in the returned state
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    key3: Any?,
    mutationPolicy: SnapshotMutationPolicy<T>,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue, mutationPolicy) }
    LaunchedEffect(key1, key2, key3) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [keys].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [keys] change, a running [producer] will be cancelled
 * and re-launched for the new source. [producer] should use [ProduceStateScope.value] to set new
 * values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession. This can be changed by providing
 * a [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param keys A list of keys that, when changed, will restart the [producer] lambda
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    vararg keys: Any?,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
    LaunchedEffect(keys = keys) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [keys].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [keys] change, a running [producer] will be cancelled
 * and re-launched for the new source. [producer] should use [ProduceStateScope.value] to set new
 * values on the returned [State].
 *
 * The given [mutationPolicy] is used to control how changes are reported and merged in the returned
 * state. If not specified, the default behavior is [structuralEqualityPolicy], which conflates
 * values that are [equal][Any.equals] to each other. Note observers may only see the latest value
 * if several values are set in rapid succession. This is especially true when reading the returned
 * state in composition, as composition executes with the latest values from a snapshot rather than
 * composing once with each intermediate value of a state.
 *
 * The [mutationPolicy] is only used when initializing the underlying state object. If the
 * [mutationPolicy] changes after creating the state, the state and the producer are unaffected and
 * will continue collecting in the previously returned state with the original
 * [SnapshotMutationPolicy].
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.ProduceState
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 * @param initialValue The value that the returned state will initially contain
 * @param keys A list of keys that, when changed, will restart the [producer] lambda
 * @param mutationPolicy A policy used to control how changes are handled in the returned state
 * @param producer A suspending lambda that defines what values are emitted to the returned state
 */
@Composable
public fun <T> produceState(
    initialValue: T,
    vararg keys: Any?,
    mutationPolicy: SnapshotMutationPolicy<T>,
    producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
    val result = remember { mutableStateOf(initialValue, mutationPolicy) }
    @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
    LaunchedEffect(keys = keys) { ProduceStateScopeImpl(result, coroutineContext).producer() }
    return result
}
