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

package androidx.compose.runtime

import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("RemoveExplicitTypeArguments")
class SnapshotFlowTests {
    @Test
    fun observeBasicChanges() = runTest {
        var state by mutableStateOf(1)
        var result = 0

        // Use Dispatchers.Unconfined to cause the observer to run immediately for this test,
        // both here and when we apply a change.
        val collector =
            snapshotFlow { state * 2 }
                .onEach { result = it }
                .launchIn(this + Dispatchers.Unconfined)

        assertEquals(2, result, "value after initial run")

        Snapshot.withMutableSnapshot { state = 5 }

        assertEquals(10, result, "value after snapshot update")

        collector.cancel()
    }

    @Test
    fun coalesceChanges() = runTest {
        var state by mutableStateOf(1)
        var runCount = 0

        // This test uses the runTest single-threaded dispatcher for observation, which means
        // we don't flush changes to the observer until we yield() intentionally.
        val collector = snapshotFlow { state }.onEach { runCount++ }.launchIn(this)

        assertEquals(0, runCount, "initial value - snapshot collector hasn't run yet")
        yield()
        assertEquals(1, runCount, "snapshot collector initial run")

        Snapshot.withMutableSnapshot { state++ }
        yield()

        assertEquals(2, runCount, "made one change")

        Snapshot.withMutableSnapshot { state++ }
        Snapshot.withMutableSnapshot { state++ }
        yield()

        assertEquals(3, runCount, "coalesced two changes")

        collector.cancel()
    }

    @Test
    fun ignoreUnrelatedChanges() = runTest {
        val state by mutableStateOf(1)
        var unrelatedState by mutableStateOf(1)
        var runCount = 0

        // This test uses the runTest single-threaded dispatcher for observation, which means
        // we don't flush changes to the observer until we yield() intentionally.
        val collector = snapshotFlow { state }.onEach { runCount++ }.launchIn(this)
        yield()

        assertEquals(1, runCount, "initial run")

        Snapshot.withMutableSnapshot { unrelatedState++ }
        yield()

        assertEquals(1, runCount, "after changing unrelated state")

        collector.cancel()
    }

    @Test
    fun nestedDerivedStateWorks() = runTest {
        val truth = mutableStateOf(true)
        val derived1 = derivedStateOf { truth.value }
        val derived2 = derivedStateOf { derived1.value }

        val results = mutableListOf<Int>()

        val collector1 = snapshotFlow { derived2.value }.onEach { results += 1 }.launchIn(this)

        val collector2 = snapshotFlow { derived2.value }.onEach { results += 2 }.launchIn(this)

        yield()

        truth.value = false

        Snapshot.sendApplyNotifications()
        yield()

        assertEquals(listOf(1, 2, 1, 2), results)

        collector1.cancel()
        collector2.cancel()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun collectAsState_schedulesOnLocalContext() = compositionTest {
        val stateFlow = MutableStateFlow(1)
        val flow =
            stateFlow.map { value ->
                val dispatcher = currentCoroutineContext()[CoroutineDispatcher.Key]
                "$value on ${if (dispatcher is TestDispatcher) "Test" else "$dispatcher"}"
            }

        var lastOuterSeen: String? = null
        var lastInnerSeen: String? = null
        var lastNestedSeen: String? = null
        var lastExplicitSeen: String? = null

        compose {
            lastOuterSeen = flow.collectAsState("").value
            CompositionLocalProvider(
                LocalCollectAsStateCoroutineContext provides rememberFakeDispatcher("Outer")
            ) {
                lastInnerSeen = flow.collectAsState("").value
                CompositionLocalProvider(
                    LocalCollectAsStateCoroutineContext provides rememberFakeDispatcher("Inner")
                ) {
                    lastNestedSeen = flow.collectAsState("").value
                    lastExplicitSeen =
                        flow.collectAsState("", rememberFakeDispatcher("Explicit")).value
                }
            }
        }

        advanceTimeBy(1)
        expectNoChanges()

        assertEquals("1 on Test", lastOuterSeen)
        assertEquals("1 on Outer", lastInnerSeen)
        assertEquals("1 on Inner", lastNestedSeen)
        assertEquals("1 on Explicit", lastExplicitSeen)

        stateFlow.value++
        advanceTimeBy(1)
        expectNoChanges()

        assertEquals("2 on Test", lastOuterSeen)
        assertEquals("2 on Outer", lastInnerSeen)
        assertEquals("2 on Inner", lastNestedSeen)
        assertEquals("2 on Explicit", lastExplicitSeen)
    }

    @Composable
    private fun rememberFakeDispatcher(name: String) = remember { NamedUnconfinedDispatcher(name) }

    class NamedUnconfinedDispatcher(val name: String) : CoroutineDispatcher() {
        override fun isDispatchNeeded(context: CoroutineContext) = false

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            block.run()
        }

        override fun toString() = name
    }
}
