/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appstate.transform

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.savedstate.SavedStateContainer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * A scope for launching state transformations via [launchTransform].
 *
 * Pass a [TransformScope] to components that only need to launch transformations without managing
 * the underlying [Composition] lifecycle. To create a scope and run its transformations, use
 * [RunnableTransformScope] and call [RunnableTransformScope.run].
 */
public interface TransformScope {

    /**
     * Launches a Composable [onUpdate] transformation in this [TransformScope] and returns its
     * result as a [DisposableState].
     *
     * This function bridges reactive Compose state (or Flows collected as state) into a
     * continuously updated [State] object outside a traditional Compose UI hierarchy. The
     * [transform] block is automatically recomposed whenever any Compose state it reads is updated,
     * and the newly returned value is pushed into the resulting [State].
     *
     * By default, this function uses an unattached [SavedStateContainer]. In order for
     * [androidx.compose.runtime.saveable.rememberSaveable] inside [onUpdate] to work properly
     * (saving and restoring state across configuration changes or process death), developers must
     * provide their own [SavedStateContainer], such as one obtained from
     * `SavedStateHandle.getOrCreateContainer`:
     * ```kotlin
     * class MyViewModel(
     *     transformScope: TransformScope,
     *     savedStateHandle: SavedStateHandle,
     * ) : ViewModel() {
     *     val uiState: DisposableState<Int> = transformScope.launchTransform(
     *         initialState = 0,
     *         container = savedStateHandle.getOrCreateContainer("myTransform"),
     *     ) {
     *         var count by rememberSaveable { mutableStateOf(0) }
     *         count
     *     }
     * }
     * ```
     *
     * The same is also possible with a [androidx.savedstate.SavedStateRegistry] via
     * [androidx.savedstate.SavedStateRegistry.getOrCreateContainer].
     *
     * Calling [DisposableState.dispose] removes this transformation from the [TransformScope] so
     * that it is no longer recomposed in the shared [Composition].
     *
     * @param initialState The initial value to populate the returned [DisposableState] before the
     *   first composition completes.
     * @param container The [SavedStateContainer] used to save and restore
     *   [androidx.compose.runtime.saveable.rememberSaveable] state. Developers must provide their
     *   own [SavedStateContainer] for `rememberSaveable` to persist and restore state properly.
     * @param onUpdate The [Composable] block that computes the updated value of type [T].
     * @return A [DisposableState] containing the latest result of the [onUpdate] block.
     */
    public fun <T> launchTransform(
        initialState: T,
        container: SavedStateContainer = SavedStateContainer(),
        onUpdate: @Composable (T) -> T,
    ): DisposableState<T>
}

/**
 * A [TransformScope] that manages a shared headless [Composition] and requires calling [run] to
 * execute state transformations created via [launchTransform].
 *
 * Create a [RunnableTransformScope], launch [run] in a coroutine, and pass it down as a
 * [TransformScope] to components that only need to call [launchTransform]:
 * ```kotlin
 * val transformScope = RunnableTransformScope()
 * scope.launch { transformScope.run() }
 * val viewModel = MyViewModel(transformScope = transformScope)
 * ```
 */
public class RunnableTransformScope : TransformScope {

    private val transformations = mutableStateListOf<Transformation<*>>()
    private val mutex = Mutex()

    override fun <T> launchTransform(
        initialState: T,
        container: SavedStateContainer,
        onUpdate: @Composable (T) -> T,
    ): DisposableState<T> {
        val transformation = Transformation(initialState, container, onUpdate)
        transformation.installIn(this)
        transformations.add(transformation)
        return transformation.state
    }

    internal fun <T> remove(transformation: Transformation<T>) {
        transformations.remove(transformation)
    }

    /**
     * Starts the [TransformScope] and begins the process of updating the present state
     * transformations.
     *
     * @throws IllegalStateException if [run] is already active on this [RunnableTransformScope].
     */
    public suspend fun run() {
        check(mutex.tryLock()) { "RunnableTransformScope is already running" }
        try {
            val context = currentCoroutineContext()
            val clock = context[MonotonicFrameClock] ?: HeadlessTransformClock()
            GlobalSnapshotManager.ensureStarted(context.minusKey(Job))
            val recomposer = Recomposer(context)
            val composition = Composition(UnitApplier, recomposer)
            try {
                withContext(clock) {
                    if (clock is HeadlessTransformClock) {
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            clock.runClock()
                        }
                    }
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        recomposer.runRecomposeAndApplyChanges()
                    }
                    composition.setContent {
                        // order matters here so doing a loop that preserves it
                        for (i in transformations.indices) {
                            val transform = transformations[i]
                            key(transform.key) { transform() }
                        }
                    }
                }
            } finally {
                composition.dispose()
                if (clock is HeadlessTransformClock) {
                    clock.cancel()
                }
                recomposer.close()
            }
        } finally {
            mutex.unlock()
        }
    }
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun remove(index: Int, count: Int) {}

    override fun onClear() {}
}
