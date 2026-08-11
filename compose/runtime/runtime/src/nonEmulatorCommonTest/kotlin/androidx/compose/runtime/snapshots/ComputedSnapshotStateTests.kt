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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.openSnapshotCount
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComputedSnapshotStateTests {

    private var count = 0

    @BeforeTest
    fun recordOpenSnapshots() {
        count = openSnapshotCount()
    }

    // Validate that the tests do not change the number of open snapshots
    @AfterTest
    fun validateOpenSnapshots() {
        assertEquals(count, openSnapshotCount())
    }

    @Test
    fun aStateFreeCalculationCanBeUsed() {
        val a = computedStateOf { 10 }
        assertEquals(10, a.value)
    }

    @Test
    fun theCalculationIsRecomputedEachInvocation() {
        var runs = 0
        var i = 0
        val a = computedStateOf {
            runs++
            i
        }
        assertEquals(0, runs, "The calculation is run only when the value is first requested")
        i++
        assertEquals(1, a.value, "Calculation should return updated value")
        assertEquals(1, runs, "Calculation should be run once per read")
        i++
        assertEquals(2, a.value, "Calculation should return updated value")
        assertEquals(2, runs, "Calculation should be run once per read")
    }

    @Test
    fun statesCanBeUsedInGlobalSnapshot() {
        val a = mutableStateOf(1)
        val b = mutableStateOf(10)
        val c = computedStateOf { a.value + b.value }
        assertEquals(11, c.value)
        a.value += 1
        assertEquals(12, c.value)
        b.value += 10
        assertEquals(22, c.value)
    }

    @Test
    fun statesCanBeUsedInSnapshot() {
        val a = mutableStateOf(1)
        val b = mutableStateOf(10)
        val c = computedStateOf { a.value + b.value }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            assertEquals(11, c.value)
            a.value += 1
            assertEquals(12, c.value)
            b.value += 10
            assertEquals(22, c.value)
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun snapshotsAreIsolatedFromGlobalChanges() {
        var state by mutableStateOf(0)
        val computed by computedStateOf { state }
        val snapshot = Snapshot.takeSnapshot()
        try {
            state = 1
            assertEquals(1, state)
            assertEquals(1, computed)
            assertEquals(0, snapshot.enter { state })
            assertEquals(0, snapshot.enter { computed })
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun mutableSnapshotsCanBeApplied() {
        var state by mutableStateOf(0)
        val computed by computedStateOf { state }
        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                assertEquals(0, state)
                assertEquals(0, computed)
                state = 1
                assertEquals(1, state)
                assertEquals(1, computed)
            }
            assertEquals(0, state)
            assertEquals(0, computed)
            snapshot.apply().check()
            assertEquals(1, state)
            assertEquals(1, computed)
        } finally {
            snapshot.dispose()
        }

        // The same thing can be done with an atomic block
        atomic {
            assertEquals(1, state)
            assertEquals(1, computed)
            state = 2
            assertEquals(2, state)
            assertEquals(2, computed)
        }
        assertEquals(2, state)
        assertEquals(2, computed)
    }

    @Test
    fun multipleSnapshotsAreIsolatedAndCanBeApplied() {
        val count = 2
        val state = MutableList(count) { mutableIntStateOf(0) }
        val computed = state.map { computedStateOf { it.intValue } }

        // Create count snapshots
        val snapshots = MutableList(count) { Snapshot.takeMutableSnapshot() }
        try {
            repeat(count) {
                assertEquals(0, state[it].intValue)
                assertEquals(0, computed[it].value)
            }

            snapshots.forEachIndexed { index, snapshot ->
                snapshot.enter { state[index].intValue = index }
            }

            // Ensure the modifications in snapshots are not visible to global
            repeat(count) {
                assertEquals(0, state[it].intValue)
                assertEquals(0, computed[it].value)
            }

            // Ensure snapshots can see their own value but no other changes
            repeat(count) { index ->
                snapshots[index].enter {
                    repeat(count) {
                        if (it != index) assertEquals(0, state[it].intValue)
                        else assertEquals(it, state[it].intValue)
                        if (it != index) assertEquals(0, computed[it].value)
                        else assertEquals(it, computed[it].value)
                    }
                }
            }

            // Apply all the snapshots
            repeat(count) { snapshots[it].apply().check() }

            // Global should now be able to see all changes
            repeat(count) {
                assertEquals(it, state[it].intValue)
                assertEquals(it, computed[it].value)
            }
        } finally {
            // Dispose the snapshots
            snapshots.forEach { it.dispose() }
        }
    }

    @Test
    fun stateReadsCanBeObserved() {
        val state = mutableStateOf(0)
        val computed = computedStateOf { state.value }

        var readCount = 0
        val readStates = mutableSetOf<Any>()
        val snapshot =
            Snapshot.takeSnapshot {
                readCount++
                readStates.add(it)
            }
        try {
            val result = snapshot.enter { computed.value }

            assertEquals(0, result)
            // 1 for computed, 1 for state
            assertEquals(2, readStates.size)
            // 1 for computed, 1 for state
            assertEquals(2, readCount)
            assertEquals(true, readStates.contains(state))
            assertEquals(true, readStates.contains(computed))
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun nestedComputedStatesProduceAResult() {
        val n = 3
        val mutableState = mutableStateOf(1)
        val computedStates1 = List(n) { computedStateOf { mutableState.value } }
        val computedStates2 = List(n) { computedStateOf { computedStates1.sumOf { it.value } } }
        val computedState3 = computedStateOf { computedStates2.sumOf { it.value } }

        assertEquals(n * n, computedState3.value)

        mutableState.value = 2
        assertEquals(n * n * 2, computedState3.value)
    }

    @Test
    fun calculationIsExecutedOnEveryReadEvenIfUnchanged() {
        var runs = 0
        val state = mutableStateOf(0)
        val computed = computedStateOf {
            runs++
            state.value
        }
        assertEquals(0, runs)
        assertEquals(0, computed.value)
        assertEquals(1, runs)
        assertEquals(0, computed.value)
        assertEquals(2, runs)
        assertEquals(0, computed.value)
        assertEquals(3, runs)
    }

    @Test
    fun nullResultIsEvaluatedEveryTime() {
        var runs = 0
        val computed = computedStateOf {
            runs++
            null
        }
        kotlin.test.assertNull(computed.value)
        assertEquals(1, runs)
        kotlin.test.assertNull(computed.value)
        assertEquals(2, runs)
    }

    @Test
    fun snapshotReadOnlyDoesNotUpdateRecord() {
        val state = mutableStateOf(0)
        val computed = computedStateOf { state.value }

        val readOnlySnapshot = Snapshot.takeSnapshot()
        try {
            val result = readOnlySnapshot.enter { computed.value }
            assertEquals(0, result)
        } finally {
            readOnlySnapshot.dispose()
        }
    }

    @Test
    fun multipleReadsInSameSnapshotWithDifferentValuesSetMultipleUniqueValues() {
        var state by mutableStateOf(1)
        val computed = computedStateOf { state }

        val snapshot = Snapshot.takeMutableSnapshot()
        try {
            snapshot.enter {
                assertEquals(1, computed.value)
                state = 2
                assertEquals(2, computed.value)
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun customMutationPolicyStructuralEquality() {
        val state = mutableStateOf(listOf("a"))
        val computed = computedStateOf(policy = structuralEqualityPolicy()) { state.value }

        assertEquals(listOf("a"), computed.value)
        state.value = listOf("a")
        assertEquals(listOf("a"), computed.value)
    }

    @Test
    fun customMutationPolicyReferentialEquality() {
        class Item(val value: String) {
            override fun equals(other: Any?): Boolean = other is Item && other.value == value

            override fun hashCode(): Int = value.hashCode()
        }

        val item1 = Item("test")
        val item2 = Item("test")
        val state = mutableStateOf(item1)
        val computed = computedStateOf(policy = referentialEqualityPolicy()) { state.value }

        assertEquals(item1, computed.value)
        state.value = item2
        assertEquals(item2, computed.value)
    }

    @Test
    fun customMutationPolicyNeverEqual() {
        var state by mutableStateOf(10)
        val computed = computedStateOf(policy = neverEqualPolicy()) { state }

        assertEquals(10, computed.value)
        state = 10
        assertEquals(10, computed.value)
    }

    @Test
    fun derivedStateOfReadingComputedState() {
        var state by mutableStateOf(0)
        var computedRuns = 0
        var derivedRuns = 0

        val computed = computedStateOf {
            computedRuns++
            state
        }
        val derived = derivedStateOf {
            derivedRuns++
            computed.value * 2
        }

        assertEquals(0, computedRuns)
        assertEquals(0, derivedRuns)

        // First read of derivedStateOf calculates both
        assertEquals(0, derived.value)
        assertEquals(1, computedRuns)
        assertEquals(1, derivedRuns)

        // Subsequent reads of derivedStateOf hit derivedState cache, so computed is NOT re-run
        assertEquals(0, derived.value)
        assertEquals(1, computedRuns)
        assertEquals(1, derivedRuns)

        // Updating state invalidates derivedState cache, so both re-run
        state = 5
        assertEquals(10, derived.value)
        assertEquals(2, computedRuns)
        assertEquals(2, derivedRuns)
    }

    @Test
    fun computedStateOfReadingDerivedState() {
        var state by mutableStateOf(0)
        var derivedRuns = 0
        var computedRuns = 0

        val derived = derivedStateOf {
            derivedRuns++
            state + 10
        }
        val computed = computedStateOf {
            computedRuns++
            derived.value
        }

        assertEquals(10, computed.value)
        assertEquals(1, derivedRuns)
        assertEquals(1, computedRuns)

        // Reading computed again re-runs computed, but derived uses its cached value
        assertEquals(10, computed.value)
        assertEquals(1, derivedRuns)
        assertEquals(2, computedRuns)

        // Mutating state invalidates derived state cache
        state = 1
        assertEquals(11, computed.value)
        assertEquals(2, derivedRuns)
        assertEquals(3, computedRuns)
    }

    @Test
    fun computedStateOfReadingNestedDerivedState() {
        var a by mutableIntStateOf(1)
        var b by mutableIntStateOf(2)

        var derivedRuns = 0
        var innerComputedRuns = 0
        var outerComputedRuns = 0

        val derived = derivedStateOf {
            derivedRuns++
            a * 10
        }
        val innerComputed = computedStateOf {
            innerComputedRuns++
            b * 100
        }
        val outerComputed = computedStateOf {
            outerComputedRuns++
            derived.value + innerComputed.value
        }

        assertEquals(210, outerComputed.value)
        assertEquals(1, derivedRuns)
        assertEquals(1, innerComputedRuns)
        assertEquals(1, outerComputedRuns)

        // Second read of outerComputed re-runs outerComputed & innerComputed, but derived is cached
        assertEquals(210, outerComputed.value)
        assertEquals(1, derivedRuns)
        assertEquals(2, innerComputedRuns)
        assertEquals(2, outerComputedRuns)

        // Mutating a updates derived on the next read of outerComputed
        a = 2
        assertEquals(220, outerComputed.value)
        assertEquals(2, derivedRuns)
        assertEquals(3, innerComputedRuns)
        assertEquals(3, outerComputedRuns)
    }

    @Test
    fun derivedStateOfReadingNestedComputedState() {
        var a by mutableIntStateOf(1)
        var b by mutableIntStateOf(2)
        var c by mutableIntStateOf(3)

        var computed1Runs = 0
        var derived1Runs = 0
        var outerDerivedRuns = 0

        val computed1 = computedStateOf {
            computed1Runs++
            a * 10
        }
        val derived1 = derivedStateOf {
            derived1Runs++
            b * 100
        }
        val outerDerived = derivedStateOf {
            outerDerivedRuns++
            computed1.value + derived1.value + c
        }

        assertEquals(213, outerDerived.value)
        assertEquals(1, computed1Runs)
        assertEquals(1, derived1Runs)
        assertEquals(1, outerDerivedRuns)

        // Second read is cached by outerDerived
        assertEquals(213, outerDerived.value)
        assertEquals(1, computed1Runs)
        assertEquals(1, derived1Runs)
        assertEquals(1, outerDerivedRuns)

        // Mutating a recomputes computed1 and outerDerived, but derived1 remains cached
        a = 2
        assertEquals(223, outerDerived.value)
        assertEquals(2, computed1Runs)
        assertEquals(1, derived1Runs)
        assertEquals(2, outerDerivedRuns)

        // Mutating b recomputes derived1 and outerDerived, and computed1 runs during outerDerived
        b = 3
        assertEquals(323, outerDerived.value)
        assertEquals(3, computed1Runs)
        assertEquals(2, derived1Runs)
        assertEquals(3, outerDerivedRuns)
    }

    @Test
    fun toStringDoesNotObserveReads() {
        val state = mutableStateOf(42)
        val computed = computedStateOf { state.value }

        val readStates = mutableSetOf<Any>()
        val snapshot = Snapshot.takeSnapshot { readStates.add(it) }
        try {
            val str = snapshot.enter { computed.toString() }
            assertTrue(str.contains("42"))
            assertEquals(
                0,
                readStates.size,
                "toString() should not record snapshot read observations",
            )
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun exceptionInCalculationIsNotStateRecordMutated() {
        var shouldThrow = true
        var state by mutableStateOf(1)
        val computed = computedStateOf {
            if (shouldThrow) throw IllegalStateException("Calculation failed")
            state
        }

        assertFailsWith<IllegalStateException> { computed.value }

        shouldThrow = false
        assertEquals(1, computed.value)
    }
}
