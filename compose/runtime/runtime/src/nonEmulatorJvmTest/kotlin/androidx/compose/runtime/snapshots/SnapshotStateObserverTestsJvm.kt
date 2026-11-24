/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.collection.ObjectList
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SnapshotStateObserverTestsJvm {

    @Test // regression test for 192677711
    fun tryToReproduceRaceCondition() {
        var running = true
        var threadException: Exception? = null
        try {
            thread {
                try {
                    while (running) {
                        Snapshot.sendApplyNotifications()
                    }
                } catch (e: Exception) {
                    threadException = e
                }
            }

            for (i in 1..1000) {
                val state1 by mutableStateOf(0)
                var state2 by mutableStateOf(true)
                val observer = SnapshotStateObserver({}).apply { start() }
                repeat(1000) {
                    observer.observeReads(Unit, {}) {
                        @Suppress("UNUSED_EXPRESSION") state1
                        if (state2) {
                            state2 = false
                        }
                    }
                }
                assertNull(threadException)
            }
        } finally {
            running = false
        }
        assertNull(threadException)
    }

    @Test // regression test for 192677711, second case
    fun tryToReproduceSecondRaceCondtion() {
        var running = true
        var threadException: Exception? = null
        try {
            thread {
                try {
                    while (running) {
                        Snapshot.sendApplyNotifications()
                    }
                } catch (e: Exception) {
                    threadException = e
                }
            }

            for (i in 1..1000) {
                val state1 by mutableStateOf(0)
                var state2 by mutableStateOf(true)
                val observer = SnapshotStateObserver({}).apply { start() }
                observer.observeReads(Unit, {}) {
                    repeat(1000) {
                        @Suppress("UNUSED_EXPRESSION") state1
                        if (state2) {
                            state2 = false
                        }
                    }
                }
                assertNull(threadException)
            }
        } finally {
            running = false
        }
        assertNull(threadException)
    }

    @Test
    fun stateChangeTriggersUpdateWhenDerivedStateIsUsedRightAfter() {
        val data = "data"
        var changes = 0
        val derivedState = derivedStateOf { 0 }

        runSimpleTest { stateObserver, state ->
            stateObserver.observeReads(data, { _ -> changes++ }) {
                if (state.value == 0) {
                    state.value++
                }
                // derived state read internally calls notifyObjectsInitialized() which triggers
                // the first onValueChanged invocation.
                derivedState.value
            }
        }

        assertEquals(2, changes)
    }

    @Test
    fun nestedObservationIsClearingThePreviousScopesBeforeReexecuting() {
        val data1 = "data1"
        val data2 = "data2"
        var changes = 0

        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableStateOf(true)
        val state2 = mutableStateOf(0)
        val onChanged1: (String) -> Unit = {}
        val onChanged2: (String) -> Unit = { _ -> changes++ }

        fun runObservedBlocks() {
            // we have nested observations
            stateObserver.observeReads(data1, onChanged1) {
                stateObserver.observeReads(data2, onChanged2) {
                    if (state1.value) {
                        state2.value++
                    }
                }
            }
        }

        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            runObservedBlocks()
            assertEquals(0, changes)

            state1.value = false
            Snapshot.sendApplyNotifications()
            runObservedBlocks()
            assertEquals(1, changes)

            state2.value++
            Snapshot.sendApplyNotifications()
            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun readingNestedDerivedStateFromAnImmediatelyRerunningObserver() {
        var changes = 0

        val state = mutableStateOf(0)
        val derivedState = derivedStateOf { state.value }
        val nestedDerivedState = derivedStateOf { derivedState.value }

        val stateObserver = SnapshotStateObserver { it() }
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            val observer =
                object : (Any) -> Unit {
                    override fun invoke(affected: Any) {
                        assertEquals(this, affected)
                        assertEquals(0, changes)
                        changes++
                        readWithObservation()
                    }

                    fun readWithObservation() {
                        stateObserver.observeReads(this, this) {
                            // read the value
                            nestedDerivedState.value
                        }
                    }
                }
            // read with 0
            observer.readWithObservation()
            // increase to 1
            state.value++
            Snapshot.sendApplyNotifications()

            assertEquals(1, changes)
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun rereadingDerivedState_whenDependenciesChanged() {
        var changes = 0
        val changeBlock: (Any) -> Unit = { changes++ }

        val states = List(2) { mutableStateOf(true) }.sortedBy { System.identityHashCode(it) }

        val derivedStates =
            List(10) {
                derivedStateOf {
                    if (states[1].value) {
                        states[0].value
                    }
                }
            }

        runSimpleTest { stateObserver, _ ->
            // record observation for a draw scope
            stateObserver.observeReads("draw", changeBlock) { derivedStates.forEach { it.value } }

            Snapshot.sendApplyNotifications()

            // flip the states to force re-recording value
            states[0].value = false
            states[1].value = false

            Snapshot.sendApplyNotifications()
        }
        assertEquals(0, changes)
    }

    @Test
    fun rereadingComputedState_whenDependenciesChanged() {
        var changes = 0
        val changeBlock: (Any) -> Unit = { changes++ }

        val states = List(2) { mutableStateOf(true) }.sortedBy { System.identityHashCode(it) }

        val computedStates =
            List(10) {
                computedStateOf {
                    if (states[1].value) {
                        states[0].value
                    }
                }
            }

        runSimpleTest { stateObserver, _ ->
            // record observation for a draw scope
            stateObserver.observeReads("draw", changeBlock) { computedStates.forEach { it.value } }

            Snapshot.sendApplyNotifications()

            // flip the states to force re-recording value
            states[0].value = false
            states[1].value = false

            Snapshot.sendApplyNotifications()
        }
        assertEquals(0, changes)
    }

    @Test
    fun observedScopeMap_lazilyAllocatesInternalMaps() {
        val stateObserver = SnapshotStateObserver { it() }
        val state1 = mutableStateOf(0)
        val state2 = mutableStateOf(0)
        val unrelatedState = mutableStateOf(0)
        var changes1 = 0
        var changes2 = 0
        val onChanged1: (Any) -> Unit = { changes1++ }
        val onChanged2: (Any) -> Unit = { changes2++ }

        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            // Observe a scope with no state reads: none of the backing maps should be allocated.
            stateObserver.observeReads("emptyScope", onChanged1) {}
            val map1 = getObservedScopeMap(stateObserver, index = 0)
            assertNull(readBackingField(map1, "_valueToScopes"))
            assertNull(readBackingField(map1, "_scopeToValues"))
            assertNull(readBackingField(map1, "_invalidated"))
            assertNull(readBackingField(map1, "_statesToReread"))
            assertNull(readBackingField(map1, "_dependencyToIndirectStates"))
            assertNull(readBackingField(map1, "_recordedIndirectStateValues"))

            // Apply notifications and clear operations on an empty scope map must not allocate.
            unrelatedState.value++
            Snapshot.sendApplyNotifications()
            stateObserver.clear()
            assertNull(readBackingField(map1, "_valueToScopes"))
            assertNull(readBackingField(map1, "_scopeToValues"))
            assertNull(readBackingField(map1, "_invalidated"))
            assertNull(readBackingField(map1, "_statesToReread"))
            assertNull(readBackingField(map1, "_dependencyToIndirectStates"))
            assertNull(readBackingField(map1, "_recordedIndirectStateValues"))

            // Observe a regular state read (no DerivedState/ComputedState) on two scope maps.
            stateObserver.observeReads("scope1", onChanged1) { state1.value }
            stateObserver.observeReads("scope2", onChanged2) { state2.value }
            val map2 = getObservedScopeMap(stateObserver, index = 1)

            assertNotNull(readBackingField(map1, "_valueToScopes"))
            assertNotNull(readBackingField(map1, "_scopeToValues"))
            assertNull(readBackingField(map1, "_invalidated"))
            assertNull(readBackingField(map1, "_statesToReread"))
            assertNull(readBackingField(map1, "_dependencyToIndirectStates"))
            assertNull(readBackingField(map1, "_recordedIndirectStateValues"))

            // Unrelated change must not allocate invalidated/indirect/reread maps.
            unrelatedState.value++
            Snapshot.sendApplyNotifications()
            assertEquals(0, changes1)
            assertEquals(0, changes2)
            assertNull(readBackingField(map1, "_invalidated"))
            assertNull(readBackingField(map1, "_statesToReread"))
            assertNull(readBackingField(map1, "_dependencyToIndirectStates"))
            assertNull(readBackingField(map1, "_recordedIndirectStateValues"))
            assertNull(readBackingField(map2, "_invalidated"))

            // Invalidating map1 must NOT allocate map2's _invalidated or either map's indirect
            // state maps.
            state1.value++
            Snapshot.sendApplyNotifications()
            assertEquals(1, changes1)
            assertEquals(0, changes2)
            assertNotNull(readBackingField(map1, "_invalidated"))
            assertNull(readBackingField(map1, "_statesToReread"))
            assertNull(readBackingField(map1, "_dependencyToIndirectStates"))
            assertNull(readBackingField(map1, "_recordedIndirectStateValues"))
            assertNull(readBackingField(map2, "_invalidated"))
            assertNull(readBackingField(map2, "_statesToReread"))
            assertNull(readBackingField(map2, "_dependencyToIndirectStates"))
            assertNull(readBackingField(map2, "_recordedIndirectStateValues"))
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun recordInvalidation_clearedScopeMap_shortCircuitsWithoutAllocating() {
        val stateObserver = SnapshotStateObserver { it() }
        val state = mutableStateOf(0)
        var changes = 0
        val onChanged: (Any) -> Unit = { changes++ }

        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            stateObserver.observeReads("scope", onChanged) { state.value }
            val scopeMap = getObservedScopeMap(stateObserver, index = 0)
            stateObserver.clear()

            state.value++
            Snapshot.sendApplyNotifications()

            assertEquals(0, changes)
            assertNull(readBackingField(scopeMap, "_invalidated"))
            assertNull(readBackingField(scopeMap, "_statesToReread"))
            assertNull(readBackingField(scopeMap, "_dependencyToIndirectStates"))
            assertNull(readBackingField(scopeMap, "_recordedIndirectStateValues"))
        } finally {
            stateObserver.stop()
        }
    }

    @Test
    fun indirectStates_lazilyAllocateIndirectMapsAndInvalidateCorrectly() {
        val stateObserver = SnapshotStateObserver { it() }
        val state = mutableStateOf(0)
        val derived = derivedStateOf { state.value > 0 }
        val computed = computedStateOf { state.value * 2 }
        var derivedChanges = 0
        var computedChanges = 0
        val onDerivedChanged: (Any) -> Unit = { derivedChanges++ }
        val onComputedChanged: (Any) -> Unit = { computedChanges++ }

        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()

            stateObserver.observeReads("derivedScope", onDerivedChanged) { derived.value }
            stateObserver.observeReads("computedScope", onComputedChanged) { computed.value }

            val derivedMap = getObservedScopeMap(stateObserver, index = 0)
            val computedMap = getObservedScopeMap(stateObserver, index = 1)
            assertNotNull(readBackingField(derivedMap, "_dependencyToIndirectStates"))
            assertNotNull(readBackingField(derivedMap, "_recordedIndirectStateValues"))
            assertNotNull(readBackingField(computedMap, "_dependencyToIndirectStates"))
            assertNotNull(readBackingField(computedMap, "_recordedIndirectStateValues"))

            // Change state without changing derived result (0 -> -1 keeps state.value > 0 false),
            // while computed changes (0 -> -2).
            state.value = -1
            Snapshot.sendApplyNotifications()
            assertEquals(0, derivedChanges)
            assertEquals(1, computedChanges)
            assertNull(readBackingField(derivedMap, "_invalidated"))
            assertNotNull(readBackingField(derivedMap, "_statesToReread"))

            // Now flip derived result (-1 -> 1).
            state.value = 1
            Snapshot.sendApplyNotifications()
            assertEquals(1, derivedChanges)
            assertEquals(2, computedChanges)
        } finally {
            stateObserver.stop()
        }
    }

    private fun getObservedScopeMap(observer: SnapshotStateObserver, index: Int): Any {
        val field = SnapshotStateObserver::class.java.getDeclaredField("observedScopeMaps")
        field.isAccessible = true
        val list = field.get(observer) as ObjectList<*>
        return list[index]!!
    }

    private fun readBackingField(scopeMap: Any, fieldName: String): Any? {
        val field = scopeMap.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(scopeMap)
    }

    private fun runSimpleTest(
        block: (modelObserver: SnapshotStateObserver, data: MutableState<Int>) -> Unit
    ) {
        val stateObserver = SnapshotStateObserver { it() }
        val state = mutableStateOf(0)
        try {
            stateObserver.start()
            Snapshot.notifyObjectsInitialized()
            block(stateObserver, state)
            state.value++
            Snapshot.sendApplyNotifications()
        } finally {
            stateObserver.stop()
        }
    }
}
