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

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Test

@SmallTest
@OptIn(ExperimentalCoroutinesApi::class)
class FlowWithLifecycleTest {
    private val owner = FakeLifecycleOwner()

    @Test
    fun testFiniteFlowCompletes() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.CREATED)
        val result = flowOf(1, 2, 3)
            .flowWithLifecycle(owner.lifecycle, Lifecycle.State.CREATED)
            .take(3)
            .toList()
        assertThat(result).containsExactly(1, 2, 3).inOrder()
        owner.setState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testFlowStartsInSubsequentLifecycleState() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        val result = flowOf(1, 2, 3)
            .flowWithLifecycle(owner.lifecycle, Lifecycle.State.CREATED)
            .take(3)
            .toList()
        assertThat(result).containsExactly(1, 2, 3).inOrder()
        owner.setState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testFlowDoesNotCollectIfLifecycleIsDestroyed() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.DESTROYED)
        val result = flowOf(1, 2, 3)
            .flowWithLifecycle(owner.lifecycle, Lifecycle.State.RESUMED)
            .take(3)
            .toList()
        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun testCollectionRestartsWithFlowThatCompletes() = runBlocking(Dispatchers.Main) {
        assertFlowCollectsAgainOnRestart(
            flowOf(1, 2),
            expectedItemsBeforeRestarting = listOf(1, 2),
            expectedItemsAfterRestarting = listOf(1, 2, 1, 2)
        )
    }

    @Test
    fun testCollectionRestartsWithFlowThatDoesNotComplete() = runBlocking(Dispatchers.Main) {
        assertFlowCollectsAgainOnRestart(
            flow {
                emit(1)
                emit(2)
                delay(10000L)
            },
            expectedItemsBeforeRestarting = listOf(1, 2),
            expectedItemsAfterRestarting = listOf(1, 2, 1, 2)
        )
    }

    @Test
    fun testCollectionRestartsWithAHotFlow() = runBlocking(Dispatchers.Main) {
        val sharedFlow = MutableSharedFlow<Int>()
        assertFlowCollectsAgainOnRestart(
            sharedFlow,
            expectedItemsBeforeRestarting = listOf(1, 2),
            expectedItemsAfterRestarting = listOf(1, 2, 4),
            beforeRestart = {
                sharedFlow.emit(1)
                sharedFlow.emit(2)
            },
            onRestart = { sharedFlow.emit(3) },
            afterRestart = { sharedFlow.emit(4) }
        )
    }

    @Test
    fun testCancellingCoroutineDoesNotGetUpdates() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.STARTED)
        val sharedFlow = MutableSharedFlow<Int>()
        val resultList = mutableListOf<Int>()
        val job = launch(Dispatchers.Main.immediate) {
            sharedFlow
                .flowWithLifecycle(owner.lifecycle, Lifecycle.State.RESUMED)
                .collect { resultList.add(it) }
        }
        owner.setState(Lifecycle.State.RESUMED)
        sharedFlow.emit(1)
        sharedFlow.emit(2)
        yield()
        assertThat(resultList).containsExactly(1, 2).inOrder()
        // Lifecycle is cancelled
        job.cancel()
        yield()
        sharedFlow.emit(3)
        yield()
        // No more items are received
        assertThat(resultList).containsExactly(1, 2).inOrder()
        owner.setState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testDestroyedLifecycleDoesNotGetUpdates() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.STARTED)
        val sharedFlow = MutableSharedFlow<Int>()
        val resultList = mutableListOf<Int>()
        launch(Dispatchers.Main.immediate) {
            sharedFlow
                .flowWithLifecycle(owner.lifecycle, Lifecycle.State.RESUMED)
                .collect { resultList.add(it) }
        }
        owner.setState(Lifecycle.State.RESUMED)
        sharedFlow.emit(1)
        sharedFlow.emit(2)
        yield()
        assertThat(resultList).containsExactly(1, 2).inOrder()
        // Lifecycle is cancelled
        owner.setState(Lifecycle.State.DESTROYED)
        sharedFlow.emit(3)
        yield()
        // No more items are received
        assertThat(resultList).containsExactly(1, 2).inOrder()
    }

    @Test
    fun testWithLaunchIn() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.STARTED)
        val resultList = mutableListOf<Int>()
        flowOf(1, 2, 3)
            .flowWithLifecycle(owner.lifecycle)
            .onEach { resultList.add(it) }
            .launchIn(owner.lifecycleScope)
        assertThat(resultList).containsExactly(1, 2, 3).inOrder()
        // Lifecycle is cancelled
        owner.setState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testOnEachBeforeOperatorOnlyExecutesInTheRightState() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.RESUMED)
        val sharedFlow = MutableSharedFlow<Int>()
        val resultList = mutableListOf<Int>()

        sharedFlow
            .onEach { resultList.add(it) }
            .flowWithLifecycle(owner.lifecycle, Lifecycle.State.RESUMED)
            .launchIn(owner.lifecycleScope)

        sharedFlow.emit(1)
        sharedFlow.emit(2)
        yield()
        assertThat(resultList).containsExactly(1, 2).inOrder()

        // Lifecycle is started again, onEach shouldn't be called
        owner.setState(Lifecycle.State.STARTED)
        yield()
        sharedFlow.emit(3)
        yield()
        assertThat(resultList).containsExactly(1, 2).inOrder()

        // Lifecycle is resumed again, onEach should be called
        owner.setState(Lifecycle.State.RESUMED)
        yield()
        sharedFlow.emit(4)
        yield()
        assertThat(resultList).containsExactly(1, 2, 4).inOrder()

        owner.setState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun testExtensionFailsWithInitializedState() = runBlocking(Dispatchers.Main) {
        try {
            flowOf(1, 2, 3)
                .flowWithLifecycle(owner.lifecycle, Lifecycle.State.INITIALIZED)
                .take(3)
                .toList()
        } catch (e: Throwable) {
            assertThat(e is IllegalArgumentException).isTrue()
        }
        Unit // tries to return the result of the try expression, using Unit instead
    }

    @Test
    fun testExtensionDoesNotCollectInDestroyedState() = runBlocking(Dispatchers.Main) {
        owner.setState(Lifecycle.State.STARTED)
        val resultList = mutableListOf<Int>()
        launch(Dispatchers.Main.immediate) {
            flowOf(1, 2, 3)
                .flowWithLifecycle(owner.lifecycle, Lifecycle.State.DESTROYED)
                .collect { resultList.add(it) }
        }
        assertThat(resultList).isEmpty()
        // Lifecycle is cancelled
        owner.setState(Lifecycle.State.DESTROYED)
    }

    private suspend fun assertFlowCollectsAgainOnRestart(
        flowUnderTest: Flow<Int>,
        expectedItemsBeforeRestarting: List<Int>,
        expectedItemsAfterRestarting: List<Int>,
        beforeRestart: suspend () -> Unit = { },
        onRestart: suspend () -> Unit = { },
        afterRestart: suspend () -> Unit = { }
    ) = coroutineScope {
        owner.setState(Lifecycle.State.STARTED)

        val resultList = mutableListOf<Int>()
        launch(Dispatchers.Main.immediate) {
            flowUnderTest
                .flowWithLifecycle(owner.lifecycle, Lifecycle.State.RESUMED)
                .collect { resultList.add(it) }
        }
        assertThat(resultList.size).isEqualTo(0)
        owner.setState(Lifecycle.State.RESUMED)

        beforeRestart()
        yield()
        assertThat(resultList).containsExactlyElementsIn(expectedItemsBeforeRestarting).inOrder()
        // Flow collection cancels
        owner.setState(Lifecycle.State.STARTED)

        onRestart()
        yield()
        assertThat(resultList).containsExactlyElementsIn(expectedItemsBeforeRestarting).inOrder()
        // Flow collection resumes
        owner.setState(Lifecycle.State.RESUMED)

        afterRestart()
        yield()
        assertThat(resultList).containsExactlyElementsIn(expectedItemsAfterRestarting).inOrder()
        owner.setState(Lifecycle.State.DESTROYED)
    }
}
