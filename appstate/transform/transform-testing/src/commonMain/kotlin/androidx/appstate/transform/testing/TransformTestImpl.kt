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

package androidx.appstate.transform.testing

import androidx.appstate.transform.DisposableState
import androidx.appstate.transform.TransformScope
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import androidx.savedstate.SavedStateContainer
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

internal fun runTransformTest(
    context: CoroutineContext,
    block: suspend TransformTestScope.() -> Unit,
): TestResult =
    runTest(context) {
        TransformTestScopeImpl(this).block()
    }

@OptIn(ExperimentalCoroutinesApi::class)
internal class TransformTestScopeImpl(private val testScope: TestScope) :
    TransformTestScope, CoroutineScope by testScope {

    override val testCoroutineScheduler: TestCoroutineScheduler
        get() = testScope.testScheduler

    override val testDispatcher: TestDispatcher =
        testScope.coroutineContext[ContinuationInterceptor] as TestDispatcher

    override val backgroundScope: CoroutineScope
        get() = testScope.backgroundScope

    override fun TransformScope(): TransformScope = TestTransformScopeImpl()

    override suspend fun TransformScope.run() {
        (this as TestTransformScopeImpl).run()
    }

    override fun advance() {
        testCoroutineScheduler.runCurrent()
        Snapshot.sendApplyNotifications()
        while (Snapshot.isApplyObserverNotificationPending) {
            // TODO(b/583929056): Wait for any concurrent thread (e.g. GlobalSnapshotManager) that
            // claimed pending snapshot changes to finish running apply observers so that resulting
            // recomposition work is scheduled before runCurrent() below.
        }
        testCoroutineScheduler.runCurrent()
    }

    override fun advanceTimeBy(duration: Duration) = testCoroutineScheduler.advanceTimeBy(duration)

    override fun advanceUntilIdle() = testCoroutineScheduler.advanceUntilIdle()

    private inner class TestTransformScopeImpl : TransformScope {

        private val transformations = mutableStateListOf<Transformation<*>>()

        override fun <T> launchTransform(
            initialState: T,
            container: SavedStateContainer,
            onUpdate: @Composable ((T) -> T),
        ): DisposableState<T> {
            val transformation =
                Transformation(initialState, container, onUpdate) { transformations.remove(it) }
            transformations.add(transformation)
            return transformation.state
        }

        suspend fun run() {
            val context = currentCoroutineContext()
            lateinit var clock: BroadcastFrameClock
            clock = BroadcastFrameClock {
                CoroutineScope(context).launch {
                    clock.sendFrame(testCoroutineScheduler.currentTime * 1_000_000)
                }
            }
            val recomposer = Recomposer(context)
            val composition = Composition(UnitApplier, recomposer)
            try {
                withContext(clock) {
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
                clock.cancel()
                recomposer.close()
            }
        }
    }
}

@Stable
private class Transformation<T>(
    initialValue: T,
    container: SavedStateContainer = SavedStateContainer(),
    private val onUpdate: @Composable (T) -> T,
    onDispose: (Transformation<T>) -> Unit,
) {
    private val saveableStateRegistry = SaveableStateRegistry(container) { true }

    val key: Any = (saveableStateRegistry.consumeRestored(TRANSFORM_KEY) as? Int) ?: hashCode()

    private val keyEntry = saveableStateRegistry.registerProvider(TRANSFORM_KEY) { key }

    val state =
        TransformState(initialValue) {
            keyEntry.unregister()
            onDispose(this)
        }

    @Composable
    operator fun invoke() {
        CompositionLocalProvider(LocalSaveableStateRegistry provides saveableStateRegistry) {
            state.value = onUpdate(Snapshot.withoutReadObservation { state.value })
        }
    }
}

private const val TRANSFORM_KEY = "androidx.appstate.transform_TRANSFORMATION_KEY"

private class TransformState<T>
private constructor(
    private val delegate: MutableState<T>,
    private val dispose: () -> Unit,
) : DisposableState<T>, MutableState<T> by delegate, StateObject by delegate as StateObject {

    override var value: T
        get() = delegate.value
        set(value) {
            delegate.value = value
        }

    constructor(
        initialValue: T,
        dispose: () -> Unit,
    ) : this(mutableStateOf(initialValue), dispose)

    override fun dispose() {
        dispose.invoke()
    }
}

private object UnitApplier : AbstractApplier<Unit>(Unit) {
    override fun insertBottomUp(index: Int, instance: Unit) {}

    override fun insertTopDown(index: Int, instance: Unit) {}

    override fun move(from: Int, to: Int, count: Int) {}

    override fun remove(index: Int, count: Int) {}

    override fun onClear() {}
}
