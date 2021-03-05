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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launches and runs the given [block] in a coroutine that executes the [block] on the
 * main thread when this [LifecycleOwner]'s [Lifecycle] is at least at [state].
 * The launched coroutine will be cancelled when the lifecycle state falls below [state].
 *
 * The [block] will cancel and re-launch as the lifecycle moves in and out of the target state.
 * To permanently remove the work from the lifecycle, [Job.cancel] the returned [Job].
 *
 * [Lifecycle] is bound to the Android **main thread** and the lifecycle state is only
 * guaranteed to be valid and consistent on that main thread. [block] always runs on
 * [Dispatchers.Main] and launches when the lifecycle [state] is first reached.
 * **This overrides any [CoroutineDispatcher] specified in [coroutineContext].**
 *
 * An active coroutine is a child [Job] of any job present in [coroutineContext]. This returned
 * [Job] is the parent of any currently running [block] and will **complete** when the [Lifecycle]
 * is [destroyed][Lifecycle.Event.ON_DESTROY]. To perform an action when a [RepeatingWorkObserver]
 * is completed or cancelled, see [Job.join] or [Job.invokeOnCompletion].
 *
 * ```
 * // Runs the block of code in a coroutine when the lifecycleOwner is at least STARTED.
 * // The coroutine will be cancelled when the ON_STOP event happens and will restart executing
 * // if the lifecycleOwner's lifecycle receives the ON_START event again.
 * lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
 * Warning: [Lifecycle.State.DESTROYED] and [Lifecycle.State.INITIALIZED] are not allowed in this
 * API. Passing those as a parameter will throw an [IllegalArgumentException].
 *
 * @param state [Lifecycle.State] in which the coroutine starts. That coroutine will cancel
 * if the lifecycle falls below that state, and will restart if it's in that state again.
 * @param coroutineContext [CoroutineContext] used to execute [block]. Note that its
 * [CoroutineDispatcher] will be replaced by [Dispatchers.Main].
 * @param block The block to run when the lifecycle is at least in [state] state.
 * @return [Job] to manage the repeating work.
 */
public fun LifecycleOwner.repeatOnLifecycle(
    state: Lifecycle.State,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job = lifecycle.repeatOnLifecycle(state, coroutineContext, block)

/**
 * Launches and runs the given [block] in a coroutine that executes the [block] on the
 * main thread when this [Lifecycle] is at least at [state].
 * The launched coroutine will be cancelled when the lifecycle state falls below [state].
 *
 * The [block] will cancel and re-launch as the lifecycle moves in and out of the target state.
 * To permanently remove the work from the lifecycle, [Job.cancel] the returned [Job].
 *
 * [Lifecycle] is bound to the Android **main thread** and the lifecycle state is only
 * guaranteed to be valid and consistent on that main thread. [block] always runs on
 * [Dispatchers.Main] and launches when the lifecycle [state] is first reached.
 * **This overrides any [CoroutineDispatcher] specified in [coroutineContext].**
 *
 * An active coroutine is a child [Job] of any job present in [coroutineContext]. This returned
 * [Job] is the parent of any currently running [block] and will **complete** when the [Lifecycle]
 * is [destroyed][Lifecycle.Event.ON_DESTROY]. To perform an action when a [RepeatingWorkObserver]
 * is completed or cancelled, see [Job.join] or [Job.invokeOnCompletion].
 *
 * ```
 * class MyActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         /* ... */
 *         // Runs the block of code in a coroutine when the lifecycle is at least STARTED.
 *         // The coroutine will be cancelled when the ON_STOP event happens and will restart
 *         // executing if the lifecycle receives the ON_START event again.
 *         lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
 *             uiStateFlow.collect { uiState ->
 *                 updateUi(uiState)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * The best practice is to call this function in the lifecycle callback when the View gets
 * created. For example, `onCreate` in an Activity, or `onViewCreated` in a Fragment. Otherwise,
 * multiple repeating jobs doing the same could be registered and be executed at the same time.
 *
 * Warning: [Lifecycle.State.DESTROYED] and [Lifecycle.State.INITIALIZED] are not allowed in this
 * API. Passing those as a parameter will throw an [IllegalArgumentException].
 *
 * @param state [Lifecycle.State] in which the coroutine starts. That coroutine will cancel
 * if the lifecycle falls below that state, and will restart if it's in that state again.
 * @param coroutineContext [CoroutineContext] used to execute [block]. Note that its
 * [CoroutineDispatcher] will be replaced by [Dispatchers.Main].
 * @param block The block to run when the lifecycle is at least in [state].
 * @return [Job] to manage the repeating work.
 */
public fun Lifecycle.repeatOnLifecycle(
    state: Lifecycle.State,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job {
    if (currentState === Lifecycle.State.DESTROYED) {
        // Fast-path! As the work would immediately complete, return a completed Job
        // to avoid extra allocations and adding/removing observers
        return Job().apply { complete() }
    }

    return RepeatingWorkObserver(this, state, coroutineContext + Dispatchers.Main, block)
        .also { observer ->
            // Immediately add the LifecycleEventObserver to ensure that RepeatingWorkObserver Job's
            // `invokeOnCompletion` that removes the observer doesn't happen before this in the case
            // of a parent job cancelled early or an ON_DESTROY completing the observer
            observer.launch(Dispatchers.Main.immediate) {
                addObserver(observer)
            }
        }.job
}

/**
 * [LifecycleEventObserver] that executes work periodically when the Lifecycle reaches
 * certain State.
 */
private class RepeatingWorkObserver(
    private val lifecycle: Lifecycle,
    state: Lifecycle.State,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    private val block: suspend CoroutineScope.() -> Unit,
) : LifecycleEventObserver, CoroutineScope {

    init {
        if (state === Lifecycle.State.INITIALIZED || state === Lifecycle.State.DESTROYED) {
            throw IllegalArgumentException(
                "RepeatingWorkObserver cannot start work with lifecycle states that are " +
                    "at least INITIALIZED or DESTROYED."
            )
        }
    }

    private val startWorkEvent = Lifecycle.Event.upTo(state)
    private val cancelWorkEvent = Lifecycle.Event.downFrom(state)

    // Exposing this job to enable cancelling RepeatingWorkObserver from the outside
    val job = Job(coroutineContext[Job]).apply { invokeOnCompletion { removeSelf() } }
    override val coroutineContext = coroutineContext + job

    private var launchedJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == startWorkEvent) {
            launchedJob = launch(start = CoroutineStart.UNDISPATCHED, block = block)
            return
        }
        if (event == cancelWorkEvent) {
            launchedJob?.cancel()
            launchedJob = null
        }
        if (event == Lifecycle.Event.ON_DESTROY) {
            job.complete()
        }
    }

    private fun removeSelf() {
        if (Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext)) {
            GlobalScope.launch(Dispatchers.Main.immediate) {
                lifecycle.removeObserver(this@RepeatingWorkObserver)
            }
        } else lifecycle.removeObserver(this@RepeatingWorkObserver)
    }
}
