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

package androidx.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

/**
 * Launches and runs the given [block] in a coroutine when `this` [LifecycleOwner]'s [Lifecycle]
 * is at least at [state]. The launched coroutine will be cancelled when the lifecycle state falls
 * below [state].
 *
 * The [block] will cancel and re-launch as the lifecycle moves in and out of the target state.
 * To permanently remove the work from the lifecycle, [Job.cancel] the returned [Job].
 *
 * ```
 * // Runs the block of code in a coroutine when the lifecycleOwner is at least STARTED.
 * // The coroutine will be cancelled when the ON_STOP event happens and will restart executing
 * // if the lifecycleOwner's lifecycle receives the ON_START event again.
 * lifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
 *     uiStateFlow.collect { uiState ->
 *         updateUi(uiState)
 *     }
 * }
 * ```
 *
 * The best practice is to call this function when the lifecycleOwner is initialized. For
 * example, `onCreate` in an Activity, or `onViewCreated` in a Fragment. Otherwise, multiple
 * repeating jobs doing the same could be registered and be executed at the same time.
 *
 * Warning: [Lifecycle.State.INITIALIZED] is not allowed in this API. Passing it as a
 * parameter will throw an [IllegalArgumentException].
 *
 * @see Lifecycle.repeatOnLifecycle for details
 *
 * @param state [Lifecycle.State] in which the coroutine running [block] starts. That coroutine
 * will cancel if the lifecycle falls below that state, and will restart if it's in that state
 * again.
 * @param coroutineContext [CoroutineContext] used to execute [block].
 * @param block The block to run when the lifecycle is at least in [state] state.
 * @return [Job] to manage the repeating work.
 */
public fun LifecycleOwner.addRepeatingJob(
    state: Lifecycle.State,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job = lifecycleScope.launch(coroutineContext) {
    lifecycle.repeatOnLifecycle(state, block)
}

/**
 * Runs the given [block] in a new coroutine when `this` [Lifecycle] is at least at [state] and
 * suspends the execution until `this` [Lifecycle] is [Lifecycle.State.DESTROYED].
 *
 * The [block] will cancel and re-launch as the lifecycle moves in and out of the target state.
 *
 * Warning: [Lifecycle.State.INITIALIZED] is not allowed in this API. Passing it as a
 * parameter will throw an [IllegalArgumentException].
 *
 * @param state [Lifecycle.State] in which `block` runs in a new coroutine. That coroutine
 * will cancel if the lifecycle falls below that state, and will restart if it's in that state
 * again.
 * @param block The block to run when the lifecycle is at least in [state] state.
 */
public suspend fun Lifecycle.repeatOnLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    require(state !== Lifecycle.State.INITIALIZED) {
        "repeatOnLifecycle cannot start work with the INITIALIZED lifecycle state."
    }

    if (currentState === Lifecycle.State.DESTROYED) {
        return
    }

    coroutineScope {
        withContext(Dispatchers.Main.immediate) {
            // Check the current state of the lifecycle as the previous check is not guaranteed
            // to be done on the main thread.
            if (currentState === Lifecycle.State.DESTROYED) return@withContext

            // Instance of the running repeating coroutine
            var launchedJob: Job? = null

            // Registered observer
            var observer: LifecycleEventObserver? = null

            try {
                // Suspend the coroutine until the lifecycle is destroyed or
                // the coroutine is cancelled
                suspendCancellableCoroutine<Unit> { cont ->
                    // Lifecycle observers that executes `block` when the lifecycle reaches certain state, and
                    // cancels when it moves falls below that state.
                    val startWorkEvent = Lifecycle.Event.upTo(state)
                    val cancelWorkEvent = Lifecycle.Event.downFrom(state)
                    observer = LifecycleEventObserver { _, event ->
                        if (event == startWorkEvent) {
                            // Launch the repeating work preserving the calling context
                            launchedJob = this@coroutineScope.launch(block = block)
                            return@LifecycleEventObserver
                        }
                        if (event == cancelWorkEvent) {
                            launchedJob?.cancel()
                            launchedJob = null
                        }
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            cont.resume(Unit)
                        }
                    }
                    this@repeatOnLifecycle.addObserver(observer as LifecycleEventObserver)
                }
            } finally {
                launchedJob?.cancel()
                observer?.let {
                    this@repeatOnLifecycle.removeObserver(it)
                }
            }
        }
    }
}
