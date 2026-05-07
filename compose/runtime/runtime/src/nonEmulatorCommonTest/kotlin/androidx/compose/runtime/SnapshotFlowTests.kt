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

import androidx.collection.intListOf
import androidx.collection.mutableIntListOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
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

    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun sharingOneSnapshotFlowManager_watchingSameStateObject() = runTest {
        val state = mutableStateOf(false)

        val manager = SnapshotFlowManager()

        val result1 = mutableListOf<Boolean>()
        val collector1 =
            snapshotFlow(manager) { state.value }.onEach { result1.add(it) }.launchIn(this)

        val collector2Done = Latch().also { it.closeLatch() }
        val result2 = mutableListOf<Boolean>()
        val collector2 =
            snapshotFlow(manager) { state.value }
                .onEach {
                    result2.add(it)
                    if (result2.size == 2) {
                        collector2Done.openLatch()
                    }
                }
                .launchIn(this + Dispatchers.Unconfined)

        // This test uses the `runTest` single-threaded dispatcher, which means that changes aren't
        // flushed to observers until we `yield()` intentionally.
        yield()

        state.value = true

        Snapshot.sendApplyNotifications()
        yield()

        collector2Done.await()
        assertEquals(listOf(false, true), result1)
        assertEquals(listOf(false, true), result2)

        collector1.cancel()
        collector2.cancel()
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun sharingOneSnapshotFlowManager_watchingDifferentStateObjects() = runTest {
        var state1 by mutableIntStateOf(0)
        var state2 by mutableIntStateOf(0)

        val manager = SnapshotFlowManager()

        val result1 = mutableIntListOf()
        val collector1 = snapshotFlow(manager) { state1 }.onEach { result1.add(it) }.launchIn(this)

        val collector2Done = Latch().also { it.closeLatch() }
        val collector2 =
            snapshotFlow(manager) { state2 }
                .onEach {
                    if (it == 2) {
                        collector2Done.openLatch()
                    }
                }
                .launchIn(this + Dispatchers.Unconfined)

        // This test uses the `runTest` single-threaded dispatcher, which means that changes aren't
        // flushed to observers until we `yield()` intentionally.
        yield()

        state1++
        Snapshot.sendApplyNotifications()
        yield()

        state2++
        Snapshot.sendApplyNotifications()
        yield()

        state1++
        state2++
        Snapshot.sendApplyNotifications()
        yield()

        collector2Done.await()
        assertEquals(intListOf(0, 1, 2), result1)

        collector1.cancel()
        collector2.cancel()
    }

    /**
     * We previously encountered crashes in scenarios like the following:
     *
     * Setup: Two `snapshotFlow`s, `snapshotFlowA` and `snapshotFlowB`, are managed by the same
     * manager and watch the same state object
     *
     * Crash trace:
     * 1. The state object is changed and the manager's apply observer is run
     * 2. The manager's apply observer signals `snapshotFlowA` to rerun its block
     * 3. The block of `snapshotFlowA` modifies the state object again and runs the manager's apply
     *    observer again, which causes the blocks of `snapshotFlowA` and `snapshotFlowB` to be rerun
     * 4. The apply observer invocation started in step 1 still needs to signal `snapshotFlowB` to
     *    rerun its block, but when attempting to do so, encounters state that was corrupted by the
     *    other invocation started in step 3
     *
     * This test serves as a regression test against such crashes, e.g. b/508270881.
     */
    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun snapshotFlowManagerApplyObserverCorruption() = runTest {
        val state = mutableIntStateOf(0)

        val manager = SnapshotFlowManager()

        val collector1 = snapshotFlow(manager) { state.intValue }.launchIn(this)

        val collector2Done = Latch().also { it.closeLatch() }
        val collector2 =
            snapshotFlow(manager) { state.intValue }
                .onEach {
                    if (it == 1) {
                        state.intValue = 2
                        Snapshot.sendApplyNotifications()
                    }
                    if (it == 2) {
                        collector2Done.openLatch()
                    }
                }
                .launchIn(this + Dispatchers.Unconfined)

        // This test uses the `runTest` single-threaded dispatcher, which means that changes aren't
        // flushed to observers until we `yield()` intentionally.
        yield()

        state.intValue = 1
        Snapshot.sendApplyNotifications()

        collector2Done.await()

        collector1.cancel()
        collector2.cancel()
    }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    @Test
    fun cancelSoleManagedSnapshotFlowThenReuseManager() = runTest {
        val state = mutableStateOf(false)

        val manager = SnapshotFlowManager()

        val results = mutableListOf<Boolean>()

        val collector1 =
            snapshotFlow(manager) { state.value }.onEach { results.add(it) }.launchIn(this)

        // This test uses the `runTest` single-threaded dispatcher, which means that changes aren't
        // flushed to observers until we `yield()` intentionally.
        yield()

        state.value = true
        Snapshot.sendApplyNotifications()
        yield()

        collector1.cancel()
        val collector2 =
            snapshotFlow(manager) { state.value }.onEach { results.add(it) }.launchIn(this)
        val collector3 =
            snapshotFlow(manager) { state.value }.onEach { results.add(it) }.launchIn(this)

        yield()

        state.value = false
        Snapshot.sendApplyNotifications()
        yield()

        assertEquals(listOf(false, true, true, true, false, false), results)

        collector2.cancel()
        collector3.cancel()
    }

    @Test
    fun twoSnapshotFlowsWithDistinctManagers() = runTest {
        val state = mutableStateOf(false)

        val result1 = mutableListOf<Boolean>()
        val collector1 = snapshotFlow { state.value }.onEach { result1.add(it) }.launchIn(this)

        val collector2Done = Latch().also { it.closeLatch() }
        val result2 = mutableListOf<Boolean>()
        val collector2 =
            snapshotFlow { state.value }
                .onEach {
                    result2.add(it)
                    if (result2.size == 2) {
                        collector2Done.openLatch()
                    }
                }
                .launchIn(this + Dispatchers.Unconfined)

        // This test uses the `runTest` single-threaded dispatcher, which means that changes aren't
        // flushed to observers until we `yield()` intentionally.
        yield()

        state.value = true

        Snapshot.sendApplyNotifications()
        yield()

        collector2Done.await()
        assertEquals(listOf(false, true), result1)
        assertEquals(listOf(false, true), result2)

        collector1.cancel()
        collector2.cancel()
    }
}
