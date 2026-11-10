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

import androidx.appstate.transform.testing.transformTest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.savedstate.SavedStateRegistry
import kotlin.test.Test
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@IgnoreAndroidHostTest
@OptIn(ExperimentalCoroutinesApi::class)
class TransformScopeTest {

    @Test
    fun testRunnableTransformScope() = transformTest {
        val runnableScope = RunnableTransformScope()
        val transformScope: TransformScope = runnableScope
        var sourceState by mutableStateOf(5)
        val state = transformScope.launchTransform(initialState = 0) { sourceState }
        // Before run() is called, the initial state is retained.
        assertThat(state.value).isEqualTo(0)

        val job =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                runnableScope.run()
            }
        assertThat(state.value).isEqualTo(5)

        sourceState = 10
        advance()
        assertThat(state.value).isEqualTo(10)
        job.cancel()
    }

    @Test
    fun testRunnableTransformScopeThrowsWhenRunCalledWhileActive() = transformTest {
        val runnableScope = RunnableTransformScope()
        var sourceState by mutableStateOf(1)
        val state = runnableScope.launchTransform(initialState = 0) { sourceState }

        val job1 =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                runnableScope.run()
            }
        assertThat(state.value).isEqualTo(1)

        assertThrows<IllegalStateException> { runnableScope.run() }
            .hasMessageThat()
            .contains("RunnableTransformScope is already running")

        // Once the first run() call completes, run() can be called again.
        job1.cancel()
        advance()

        sourceState = 2
        val job2 =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                runnableScope.run()
            }
        assertThat(state.value).isEqualTo(2)
        job2.cancel()
    }

    @Test
    fun testTransformScopeWithCoroutineContext() = transformTest {
        val scope = TransformScope()
        var sourceState by mutableStateOf(0)
        val state = scope.launchTransform(initialState = 0) { sourceState }
        val job =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                scope.run()
            }
        assertThat(state.value).isEqualTo(0)

        sourceState = 5
        advance()
        assertThat(state.value).isEqualTo(5)
        job.cancel()
    }

    @Test
    fun testJobCompletionStopsTransformScope() = transformTest {
        val scope = TransformScope()
        var sourceState by mutableStateOf(0)
        val state = scope.launchTransform(initialState = 0) { sourceState }
        val job =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                scope.run()
            }
        assertThat(state.value).isEqualTo(0)

        sourceState = 1
        advance()
        assertThat(state.value).isEqualTo(1)

        job.cancel()
        advance()

        sourceState = 2
        advance()
        assertThat(state.value).isEqualTo(1)
    }

    @Test
    fun testSharedTransformScopeIndividualDisposal() = transformTest {
        val transformScope = TransformScope()
        var sourceState1 by mutableStateOf(0)
        var sourceState2 by mutableStateOf(10)

        var state1Invocations = 0
        var state2Invocations = 0
        var state1Disposed = false

        val state1 =
            transformScope.launchTransform(initialState = -1) {
                state1Invocations++
                DisposableEffect(Unit) { onDispose { state1Disposed = true } }
                sourceState1
            }
        val state2 =
            transformScope.launchTransform(initialState = -1) {
                state2Invocations++
                val rememberedBase = remember { 100 }
                rememberedBase + sourceState2
            }
        val job =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                transformScope.run()
            }

        assertThat(state1.value).isEqualTo(0)
        assertThat(state2.value).isEqualTo(110)
        assertThat(state1Invocations).isEqualTo(1)
        assertThat(state2Invocations).isEqualTo(1)

        // Updating only sourceState1 recomposes only state1, skipping state2.
        sourceState1 = 1
        advance()
        assertThat(state1.value).isEqualTo(1)
        assertThat(state2.value).isEqualTo(110)
        assertThat(state1Invocations).isEqualTo(2)
        assertThat(state2Invocations).isEqualTo(1)

        // Disposing state1 removes it from the shared Composition without re-running state2.
        state1.dispose()
        advance()
        assertThat(state1Disposed).isTrue()
        assertThat(state2Invocations).isEqualTo(1)

        sourceState1 = 2
        sourceState2 = 30
        advance()
        assertThat(state1.value).isEqualTo(1)
        assertThat(state2.value).isEqualTo(130)
        assertThat(state1Invocations).isEqualTo(2)
        assertThat(state2Invocations).isEqualTo(2)

        // Cancelling the run() job disposes the shared Composition and stops recomposition.
        job.cancel()
        advance()

        sourceState2 = 40
        advance()
        assertThat(state2.value).isEqualTo(130)
    }

    // As long as we give the Transform the proper container, everything works.
    @Test
    fun testLaunchTransformWithSavedStateContainer() = transformTest {
        val registry1 = SavedStateRegistry().asContainer()
        var triggerUpdate by mutableStateOf(false)

        val contentA: @Composable (Int) -> Int = {
            var savedCount by rememberSaveable { mutableStateOf(10) }
            if (triggerUpdate) {
                savedCount = 25
            }
            savedCount
        }
        val contentB: @Composable (String) -> String = {
            var savedText by rememberSaveable { mutableStateOf("initial") }
            if (triggerUpdate) {
                savedText = "updated"
            }
            savedText
        }

        val scope1 = TransformScope()
        val state1a =
            scope1.launchTransform(
                initialState = 0,
                container = registry1.getOrCreateContainer("first"),
                onUpdate = contentA,
            )
        val state1b =
            scope1.launchTransform(
                initialState = "",
                container = registry1.getOrCreateContainer("second"),
                onUpdate = contentB,
            )

        val job1 =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                scope1.run()
            }

        assertThat(state1a.value).isEqualTo(10)
        assertThat(state1b.value).isEqualTo("initial")

        triggerUpdate = true
        advance()
        assertThat(state1a.value).isEqualTo(25)
        assertThat(state1b.value).isEqualTo("updated")

        val savedMap = registry1.saveState()
        job1.cancel()
        triggerUpdate = false

        val registry2 = SavedStateRegistry(savedMap)
        val scope2 = TransformScope()
        val state2a =
            scope2.launchTransform(
                initialState = 0,
                container = registry2.getOrCreateContainer("first"),
                onUpdate = contentA,
            )
        val state2b =
            scope2.launchTransform(
                initialState = "",
                container = registry2.getOrCreateContainer("second"),
                onUpdate = contentB,
            )

        val job2 =
            backgroundScope.launch(testDispatcher, start = CoroutineStart.UNDISPATCHED) {
                scope2.run()
            }

        assertThat(state2a.value).isEqualTo(25)
        assertThat(state2b.value).isEqualTo("updated")
        job2.cancel()
    }
}
