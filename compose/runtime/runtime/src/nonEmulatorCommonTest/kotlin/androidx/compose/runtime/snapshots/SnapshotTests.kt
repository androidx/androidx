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

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.current
import androidx.compose.runtime.snapshots.Snapshot.Companion.openSnapshotCount
import androidx.compose.runtime.snapshots.Snapshot.Companion.takeMutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot.Companion.takeSnapshot
import androidx.compose.runtime.structuralEqualityPolicy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

class SnapshotTests {
    @Test
    fun aSnapshotCanBeCreated() {
        val snapshot = takeSnapshot()
        snapshot.dispose()
    }

    @Test
    fun aMutableStateCanBeCreated() {
        mutableStateOf("")
    }

    @Test
    fun aMutableStateCanBeReadOutsideASnapshot() {
        val state by mutableStateOf("")
        assertEquals("", state)
    }

    @Test
    fun aMutableStateCanBeWrittenToOutsideASnapshot() {
        var state by mutableStateOf("0")
        assertEquals("0", state)
        state = "1"
        assertEquals("1", state)
    }

    @Test
    fun snapshotsAreIsolatedFromGlobalChanges() {
        var state by mutableStateOf("0")
        val snapshot = takeSnapshot()
        try {
            state = "1"
            assertEquals("1", state)
            assertEquals("0", snapshot.enter { state })
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun mutableSnapshotsCanBeApplied() {
        var state by mutableIntStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter {
                assertEquals(0, state)
                state = 1
                assertEquals(1, state)
            }
            assertEquals(0, state)
            snapshot.apply().check()
            assertEquals(1, state)
        } finally {
            snapshot.dispose()
        }

        // The same thing can be done with an atomic block
        atomic {
            assertEquals(1, state)
            state = 2
            assertEquals(2, state)
        }
        assertEquals(2, state)
    }

    @Test
    fun multipleSnapshotsAreIsolatedAndCanBeApplied() {
        val count = 2
        val state = MutableList(count) { mutableIntStateOf(0) }

        // Create count snapshots
        val snapshots = MutableList(count) { takeMutableSnapshot() }
        try {
            snapshots.forEachIndexed { index, snapshot ->
                snapshot.enter { state[index].intValue = index }
            }

            // Ensure the modifications in snapshots are not visible to global
            repeat(count) { assertEquals(0, state[it].intValue) }

            // Ensure snapshots can see their own value but no other changes
            repeat(count) { index ->
                snapshots[index].enter {
                    repeat(count) {
                        if (it != index) assertEquals(0, state[it].intValue)
                        else assertEquals(it, state[it].intValue)
                    }
                }
            }

            // Apply all the snapshots
            repeat(count) { snapshots[it].apply().check() }

            // Global should now be able to see all changes
            repeat(count) { assertEquals(it, state[it].intValue) }
        } finally {
            // Dispose the snapshots
            snapshots.forEach { it.dispose() }
        }
    }

    @Test
    fun applyingASnapshotThatCollidesWithAGlobalChangeWillFail() {
        var state by mutableIntStateOf(0)

        val snapshot = snapshot { state = 1 }
        try {
            state = 2
            assertTrue(snapshot.apply() is SnapshotApplyResult.Failure)
            assertEquals(2, state)
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun applyingCollidingSnapshotsWillFail() {
        var state by mutableIntStateOf(0)
        val snapshot1 = snapshot { state = 1 }
        val snapshot2 = snapshot { state = 2 }
        try {
            assertEquals(0, state)
            snapshot1.apply().check()
            assertEquals(1, state)
            assertTrue(snapshot2.apply() is SnapshotApplyResult.Failure)
            assertEquals(1, state)
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun stateReadsCanBeObserved() {
        val state = mutableIntStateOf(0)

        val readStates = mutableListOf<Any>()
        val snapshot = takeSnapshot { readStates.add(it) }
        try {

            val result = snapshot.enter { state.intValue }

            assertEquals(0, result)
            assertEquals(1, readStates.size)
            assertEquals(state, readStates[0])
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun stateWritesCanBeObserved() {
        val state = mutableIntStateOf(0)
        val writtenStates = mutableListOf<Any>()
        val snapshot = takeMutableSnapshot { write -> writtenStates.add(write) }
        try {
            snapshot.enter {
                assertEquals(0, writtenStates.size)
                state.intValue = 2
                assertEquals(1, writtenStates.size)
            }
        } finally {
            snapshot.dispose()
        }
        assertEquals(1, writtenStates.size)
        assertEquals(state, writtenStates[0])
    }

    @Test
    fun appliesCanBeObserved() {
        val state = mutableIntStateOf(0)
        var observedSnapshot: Snapshot? = null
        val unregister =
            Snapshot.registerApplyObserver { changed, snapshot ->
                assertTrue(state in changed)
                observedSnapshot = snapshot
            }
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.intValue = 2 }
            assertEquals(null, observedSnapshot)
            snapshot.apply().check()
            assertEquals(snapshot, observedSnapshot)
        } finally {
            snapshot.dispose()
            unregister.dispose()
        }
    }

    @Test
    fun globalChangesCanBeObserved() {
        val state = mutableIntStateOf(0)

        Snapshot.notifyObjectsInitialized()

        var applyObserved = false
        val unregister =
            Snapshot.registerApplyObserver { changed, _ ->
                assertTrue(state in changed)
                applyObserved = true
            }
        try {
            state.intValue = 2

            // Nothing should have been observed yet.
            assertFalse(applyObserved)

            // Advance the global snapshot to send apply notifications
            Snapshot.sendApplyNotifications()

            assertTrue(applyObserved)
        } finally {
            unregister.dispose()
        }
    }

    @Test
    fun applyObserverNotificationIsPendingWhileSendingApplyNotifications() {
        val state = mutableIntStateOf(0)

        var notificationsPendingWhileObserving = false
        val unregister =
            Snapshot.registerApplyObserver { _, _ ->
                notificationsPendingWhileObserving = Snapshot.isApplyObserverNotificationPending
            }

        try {
            // Normally not pending
            assertFalse(Snapshot.isApplyObserverNotificationPending)

            state.intValue = 1

            Snapshot.sendApplyNotifications()

            // Was pending while sending apply notifications
            assertTrue(notificationsPendingWhileObserving)

            // Not pending afterwards
            assertFalse(Snapshot.isApplyObserverNotificationPending)
        } finally {
            unregister.dispose()
        }
    }

    @Test
    fun aNestedSnapshotCanBeTaken() {
        val state = mutableIntStateOf(0)

        val snapshot = takeSnapshot()
        try {
            val nested = snapshot.takeNestedSnapshot()
            try {
                state.intValue = 1

                assertEquals(0, nested.enter { state.intValue })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun aNestedMutableSnapshotCanBeTaken() {
        val state = mutableIntStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.intValue = 1 }
            val nested = snapshot.takeNestedMutableSnapshot()
            try {
                nested.enter { state.intValue = 2 }

                assertEquals(0, state.intValue)
                assertEquals(1, snapshot.enter { state.intValue })
                assertEquals(2, nested.enter { state.intValue })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun aNestedSnapshotOfAMutableSnapshotCanBeTaken() {
        val state = mutableIntStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.intValue = 1 }
            val nested = snapshot.takeNestedSnapshot()
            try {
                snapshot.enter { state.intValue = 2 }

                assertEquals(0, state.intValue)
                assertEquals(2, snapshot.enter { state.intValue })
                assertEquals(1, nested.enter { state.intValue })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun aNestedMutableSnapshotCanBeAppliedToItsParent() {
        val state = mutableIntStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.intValue = 1 }
            val nested = snapshot.takeNestedMutableSnapshot()
            try {

                nested.enter { state.intValue = 2 }
                assertEquals(0, state.intValue)
                assertEquals(1, snapshot.enter { state.intValue })
                assertEquals(2, nested.enter { state.intValue })

                nested.apply().check()
            } finally {
                nested.dispose()
            }
            assertEquals(0, state.intValue)
            assertEquals(2, snapshot.enter { state.intValue })

            snapshot.apply().check()
        } finally {
            snapshot.dispose()
        }

        assertEquals(2, state.intValue)
    }

    @Test
    fun aParentSnapshotCanAccessAStatObjectedCreateByANestedSnapshot() {
        val snapshot = takeMutableSnapshot()
        val state =
            try {
                val nested = snapshot.takeNestedMutableSnapshot()
                val state =
                    try {
                        nested.notifyObjectsInitialized()
                        val state = nested.enter { mutableIntStateOf(1) }
                        assertEquals(1, nested.enter { state.intValue })
                        nested.apply().check()
                        state
                    } finally {
                        nested.dispose()
                    }
                assertEquals(1, snapshot.enter { state.intValue })
                snapshot.apply().check()
                state
            } finally {
                snapshot.dispose()
            }
        assertEquals(1, state.intValue)
    }

    @Test
    fun atomicChangesNest() {
        val state = mutableIntStateOf(0)
        atomic {
            state.intValue = 1
            atomic {
                state.intValue = 2

                assertEquals(0, Snapshot.global { state.intValue })
            }
            assertEquals(2, state.intValue)
            assertEquals(0, Snapshot.global { state.intValue })
        }
        assertEquals(2, state.intValue)
    }

    @Test
    fun siblingNestedMutableSnapshotsAreIsolatedFromEachOther() {
        val state = mutableIntStateOf(0)
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state.intValue = 10 }

            val nested1 = snapshot.takeNestedMutableSnapshot()
            try {
                nested1.enter { state.intValue = 1 }
                val nested2 = snapshot.takeNestedMutableSnapshot()
                try {
                    nested2.enter { state.intValue = 2 }

                    assertEquals(0, state.intValue)
                    assertEquals(10, snapshot.enter { state.intValue })
                    assertEquals(1, nested1.enter { state.intValue })
                    assertEquals(2, nested2.enter { state.intValue })
                } finally {
                    nested2.dispose()
                }
            } finally {
                nested1.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertEquals(0, state.intValue)
    }

    @Test
    fun readingInANestedSnapshotNotifiesTheParent() {
        val state = mutableIntStateOf(0)
        val read = HashSet<Any>()
        val snapshot = takeSnapshot { read.add(it) }
        try {
            val nested = snapshot.takeNestedSnapshot()
            try {
                assertEquals(0, nested.enter { state.intValue })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(read.contains(state))
    }

    @Test
    fun readingInANestedSnapshotNotifiesNestedAndItsParent() {
        val state = mutableIntStateOf(0)
        val parentRead = HashSet<Any>()
        val nestedRead = HashSet<Any>()
        val snapshot = takeSnapshot { parentRead.add(it) }
        try {
            val nested = snapshot.takeNestedSnapshot { nestedRead.add(it) }
            try {
                assertEquals(0, nested.enter { state.intValue })
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(parentRead.contains(state))
        assertTrue(nestedRead.contains(state))
    }

    @Test
    fun writingToANestedSnapshotNotifiesTheParent() {
        val state = mutableIntStateOf(0)
        val written = HashSet<Any>()
        val snapshot = takeMutableSnapshot { written.add(it) }
        try {
            val nested = snapshot.takeNestedMutableSnapshot()
            try {
                nested.enter { state.intValue = 2 }
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(written.contains(state))
    }

    @Test
    fun writingToANestedSnapshotNotifiesNestedAndItsParent() {
        val state = mutableIntStateOf(0)
        val parentWritten = HashSet<Any>()
        val nestedWritten = HashSet<Any>()
        val snapshot = takeMutableSnapshot { parentWritten.add(it) }
        try {
            val nested = snapshot.takeNestedMutableSnapshot { nestedWritten.add(it) }
            try {
                nested.enter { state.intValue = 2 }
            } finally {
                nested.dispose()
            }
        } finally {
            snapshot.dispose()
        }
        assertTrue(parentWritten.contains(state))
        assertTrue(nestedWritten.contains(state))
    }

    @Test
    fun creatingAStateInANestedSnapshotAndMutatingInParentApplies() {
        val states = mutableListOf<MutableState<Int>>()
        val snapshot = takeMutableSnapshot()
        try {
            val nested = snapshot.takeNestedMutableSnapshot()
            try {
                nested.enter {
                    val state = mutableIntStateOf(0)
                    states.add(state)
                }
                nested.apply()
            } finally {
                nested.dispose()
            }
            snapshot.enter {
                for (state in states) {
                    state.value++
                }
            }
            snapshot.apply()
        } finally {
            snapshot.dispose()
        }
        for (state in states) {
            assertEquals(1, state.value)
        }
    }

    @Test
    fun snapshotsChangesCanMerge() {
        val state = mutableIntStateOf(0)
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            // Change the state to the same value in both snapshots
            snapshot1.enter { state.intValue = 1 }
            snapshot2.enter { state.intValue = 1 }

            // Still 0 until one of the snapshots is applied
            assertEquals(0, state.intValue)

            // Apply snapshot 1 should change the value to 1
            snapshot1.apply().check()
            assertEquals(1, state.intValue)

            // Applying snapshot 2 should succeed because it changed the value to the same value.
            snapshot2.apply().check()
            assertEquals(1, state.intValue)
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun mergedSnapshotsDoNotRepeatChangeNotifications() {
        val state = mutableIntStateOf(0)
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            val changes =
                changesOf(state) {
                    snapshot1.enter { state.intValue = 1 }
                    snapshot2.enter { state.intValue = 1 }
                    snapshot1.apply().check()
                    snapshot2.apply().check()
                }
            assertEquals(1, changes)
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun statesWithStructuralEqualityPolicyMerge() {
        data class Value(val v1: Int, val v2: Int)
        val state = mutableStateOf(Value(1, 2), structuralEqualityPolicy())
        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            snapshot1.enter { state.value = Value(3, 4) }
            snapshot2.enter { state.value = Value(3, 4) }
            snapshot1.apply().check()
            snapshot2.apply().check()
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    // Boxes a primitive Int into an object to facilitate testing on all platforms.
    // In common case we can't rely on a default pool of boxed Integers (-128..127).
    // For example, in K/Wasm each boxed Int is a new instance.
    private fun boxInt(i: Int): Any = i

    @Test
    fun stateUsingNeverEqualPolicyCannotBeMerged() {
        val value = boxInt(0)
        val value2 = boxInt(1)
        assertFailsWith(SnapshotApplyConflictException::class) {
            val state = mutableStateOf(value, neverEqualPolicy())
            val snapshot1 = takeMutableSnapshot()
            val snapshot2 = takeMutableSnapshot()
            try {
                snapshot1.enter { state.value = value2 }
                snapshot2.enter { state.value = value2 }
                snapshot1.apply().check()
                snapshot2.apply().check()
            } finally {
                snapshot1.dispose()
                snapshot2.dispose()
            }
        }
    }

    @Test
    fun changingAnEqualityPolicyStateToItsCurrentValueIsNotConsideredAChange() {
        val value = boxInt(0)
        val state = mutableStateOf(value, referentialEqualityPolicy())
        val changes = changesOf(state) { state.value = value }
        assertEquals(0, changes)
    }

    @Test
    fun changingANeverEqualPolicyStateToItsCurrentValueIsConsideredAChange() {
        val value = boxInt(0)
        val state = mutableStateOf(value, neverEqualPolicy())
        val changes = changesOf(state) { state.value = value }
        assertEquals(1, changes)
    }

    @Test
    fun toStringOfMutableStateDoesNotTriggerReadObserver() {
        val state = mutableIntStateOf(0)
        val normalReads = readsOf { state.intValue }
        assertEquals(1, normalReads)
        val toStringReads = readsOf { state.toString() }
        assertEquals(0, toStringReads)
    }

    @Test
    fun toStringOfDerivedStateDoesNotTriggerReadObservers() {
        val state = mutableIntStateOf(0)
        val derived = derivedStateOf { state.intValue + 1 }
        val toStringReads = readsOf { derived.toString() }
        assertEquals(0, toStringReads)
    }

    @Suppress("AutoboxingStateCreation") // Testing this case
    @Test
    fun toStringValueOfMutableState() {
        val state = mutableStateOf(10)
        assertEquals("MutableState(value=10)@${state.hashCode()}", state.toString())
        state.value = 20
        assertEquals("MutableState(value=20)@${state.hashCode()}", state.toString())
    }

    @Test
    fun toStringValueOfDerivedState() {
        val state = mutableIntStateOf(10)
        val derivedState = derivedStateOf { state.intValue + 10 }
        val hash = derivedState.hashCode()
        assertEquals("DerivedState(value=<Not calculated>)@$hash", derivedState.toString())
        assertEquals(20, derivedState.value)
        assertEquals("DerivedState(value=20)@$hash", derivedState.toString())
        state.intValue = 20
        assertEquals("DerivedState(value=<Not calculated>)@$hash", derivedState.toString())
        assertEquals(30, derivedState.value)
        assertEquals("DerivedState(value=30)@$hash", derivedState.toString())
    }

    @Test // Regression test for b/181162478
    fun nestedSnapshotsAreIsolated() {
        var state1 by mutableIntStateOf(0)
        var state2 by mutableIntStateOf(0)
        val parent = takeMutableSnapshot()
        parent.enter { state1 = 1 }
        Snapshot.withMutableSnapshot { state2 = 2 }
        val snapshot = parent.takeNestedSnapshot()
        parent.apply().check()
        parent.dispose()
        snapshot.enter {
            // Should see the change of state1
            assertEquals(1, state1)

            // But not the state change of state2
            assertEquals(0, state2)
        }
        snapshot.dispose()
    }

    @Test // Regression test for b/181159260
    fun readOnlySnapshotValidAfterParentDisposed() {
        var state by mutableIntStateOf(0)
        val parent = takeMutableSnapshot()
        parent.enter { state = 1 }
        val child = parent.takeNestedSnapshot()
        parent.apply().check()
        parent.dispose()
        child.enter { assertEquals(1, state) }
        val reads = mutableListOf<Any>()
        val nestedChild = child.takeNestedSnapshot { reads.add(it) }
        nestedChild.enter { assertEquals(1, state) }
        child.dispose()
        nestedChild.dispose()
    }

    @Test // Regression test for b/193006595
    fun transparentSnapshotAdvancesCorrectly() {
        val state =
            Snapshot.observe({}) {
                // In a transparent snapshot, advance the global snapshot
                Snapshot.notifyObjectsInitialized()

                // Create an apply an object in a snapshot
                val state = atomic { mutableIntStateOf(0) }

                // Ensure that the object can be accessed in the observer
                assertEquals(0, state.intValue)

                state
            }

        // Ensure that the object can be accessed globally.
        assertEquals(0, state.intValue)
    }

    // Regression test for b/199921314
    // This test lifted directly from the bug reported by chrnie@foxmail.com, modified and formatted
    // to avoid lint warnings.
    @Test
    fun testTakeSnapshotNested() {
        assertFailsWith<IllegalStateException> {
            Snapshot.withMutableSnapshot {
                val expectReadonlySnapshot = takeSnapshot()
                try {
                    expectReadonlySnapshot.enter {
                        var state by mutableIntStateOf(0)

                        // expect throw IllegalStateException:Cannot modify a state object in a
                        // read-only snapshot
                        state = 1

                        assertEquals(1, state)
                    }
                } finally {
                    expectReadonlySnapshot.dispose()
                }
            }
        }
    }

    @Test // Regression test for b/200575924
    // Test copied from b/200575924 bu chrnie@foxmail.com
    fun nestedMutableSnapshotCanNotSeeOtherSnapshotChange() {
        var state by mutableIntStateOf(0)

        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            snapshot2.enter { state = 1 }

            snapshot1.enter { Snapshot.withMutableSnapshot { assertEquals(0, state) } }
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test // Regression test for b/200575924
    // Test copied from b/200575924 by chrnie@foxmail.com
    fun nestedSnapshotCanNotSeeOtherSnapshotChange() {
        var state by mutableIntStateOf(0)

        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        try {
            snapshot2.enter { state = 1 }

            snapshot1.enter {
                val nestedSnapshot = takeSnapshot()
                try {
                    nestedSnapshot.enter { assertEquals(0, state) }
                } finally {
                    nestedSnapshot.dispose()
                }
            }
        } finally {
            snapshot1.dispose()
            snapshot2.dispose()
        }
    }

    @Test
    fun canTakeNestedSnapshotsFromApplyObserver() {
        var takenSnapshot: Snapshot? = null
        val observer =
            Snapshot.registerApplyObserver { _, snapshot ->
                if (takenSnapshot != null) error("already took a nested snapshot")
                takenSnapshot = snapshot.takeNestedSnapshot()
            }

        try {
            var state by mutableStateOf("initial")
            Snapshot.withMutableSnapshot { state = "before observer snapshot" }

            state = "after observer snapshot"

            val observerSnapshot = takenSnapshot ?: fail("snapshot was not taken by observer")

            observerSnapshot.enter { assertEquals("before observer snapshot", state) }
        } finally {
            observer.dispose()
            takenSnapshot?.dispose()
        }
    }

    @Test
    fun canTakeNestedMutableSnapshotsFromApplyObserver() {
        var takenSnapshot: MutableSnapshot? = null
        val observer =
            Snapshot.registerApplyObserver { _, snapshot ->
                if (takenSnapshot != null) error("already took a nested snapshot")
                takenSnapshot =
                    (snapshot as? MutableSnapshot)?.takeNestedMutableSnapshot()
                        ?: error("Applied snapshot was not mutable")
            }

        try {
            var state by mutableStateOf("initial")
            Snapshot.withMutableSnapshot { state = "before observer snapshot" }

            state = "after observer snapshot"

            val observerSnapshot = takenSnapshot ?: fail("snapshot was not taken by observer")

            observerSnapshot.enter {
                assertEquals("before observer snapshot", state)
                state = "change made by observer snapshot"
            }

            assertFalse(
                observerSnapshot.apply().succeeded,
                "applying observer snapshot with conflicting change",
            )
        } finally {
            observer.dispose()
            takenSnapshot?.dispose()
        }
    }

    @Test
    fun cannotTakeSnapshotOfClosedSnapshotAfterApplyReturns() {
        val snapshot = takeMutableSnapshot()
        var state by mutableStateOf("initial")

        try {
            snapshot.enter { state = "mutated" }
            snapshot.apply().check()
            snapshot.takeNestedSnapshot().dispose()
            fail("taking a nested snapshot of an applied snapshot did not throw")
        } catch (ise: IllegalStateException) {
            // expected
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun cannotApplyASnapshotTwice() {
        var state by mutableStateOf("initial")
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state = "mutated" }
            snapshot.apply().check()
            snapshot.apply().check()
            fail("An exception should have been thrown by second apply()")
        } catch (ise: IllegalStateException) {
            // Expected exception
            assertTrue(
                ise.message?.let {
                    it.contains("Snapshot is not open") && it.contains("applied=")
                } == true,
                "Incorrect message: ${ise.message}",
            )
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun cannotApplyAfterADispose() {
        var state by mutableStateOf("initial")
        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter { state = "mutated" }
            snapshot.dispose()
            snapshot.apply().check()
            fail("An exception should have been thrown by the apply()")
        } catch (ise: IllegalStateException) {
            // Expected exception
            assertTrue(
                ise.message?.let {
                    it.contains("Snapshot is not open") && it.contains("applied=")
                } == true,
                "Incorrect message: ${ise.message}",
            )
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun testRecordsAreReusedCorrectly() {
        val value = mutableIntStateOf(0)
        Snapshot.withMutableSnapshot { value.intValue++ }
        val mutable1 = takeMutableSnapshot()
        val readable1 = takeSnapshot()
        mutable1.enter { value.intValue++ }
        mutable1.apply().check()
        Snapshot.withMutableSnapshot { value.intValue++ }
        val v = readable1.enter { value.intValue }
        assertEquals(v, 1)
        readable1.dispose()
        mutable1.dispose()
    }

    @Test
    fun testUnsafeSnapshotEnterAndLeave() {
        val snapshot = takeSnapshot()
        try {
            val oldSnapshot = snapshot.unsafeEnter()
            try {
                assertSame(snapshot, current, "expected taken snapshot to be current")
            } finally {
                snapshot.unsafeLeave(oldSnapshot)
            }
            assertNotSame(snapshot, current, "expected taken snapshot not to be current")
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun testUnsafeSnapshotLeaveThrowsIfNotCurrent() {
        val snapshot = takeSnapshot()
        try {
            try {
                snapshot.unsafeLeave(null)
                fail("unsafeLeave should have thrown")
            } catch (_: IllegalStateException) {
                // expected
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun testNestedWithinTransparentSnapshotDisposedCorrectly() {
        val outerSnapshot =
            TransparentObserverSnapshot(
                parentSnapshot = currentSnapshot(),
                specifiedReadObserver = null,
                mergeParentObservers = false,
                ownsParentSnapshot = false,
            )

        try {
            outerSnapshot.enter {
                val innerSnapshot = outerSnapshot.takeNestedSnapshot()

                try {
                    innerSnapshot.enter {}
                } finally {
                    innerSnapshot.dispose()
                }
            }
        } finally {
            outerSnapshot.dispose()
        }
    }

    @Test
    fun testNestedWithinTransparentMutableSnapshotDisposedCorrectly() {
        val outerSnapshot =
            TransparentObserverMutableSnapshot(
                parentSnapshot = currentSnapshot() as? MutableSnapshot,
                specifiedReadObserver = null,
                specifiedWriteObserver = null,
                mergeParentObservers = false,
                ownsParentSnapshot = false,
            )

        try {
            outerSnapshot.enter {
                val innerSnapshot = outerSnapshot.takeNestedSnapshot()

                try {
                    innerSnapshot.enter {}
                } finally {
                    innerSnapshot.dispose()
                }
            }
        } finally {
            outerSnapshot.dispose()
        }
    }

    @Test
    fun testTransparentSnapshotMergedWithNestedReadObserver() {
        var outerChanges = 0
        var innerChanges = 0
        val state by mutableIntStateOf(0)

        val outerSnapshot =
            TransparentObserverSnapshot(
                parentSnapshot = currentSnapshot(),
                specifiedReadObserver = { outerChanges++ },
                mergeParentObservers = false,
                ownsParentSnapshot = false,
            )

        try {
            outerSnapshot.enter {
                val innerSnapshot =
                    outerSnapshot.takeNestedSnapshot(readObserver = { innerChanges++ })

                try {
                    innerSnapshot.enter {
                        state // read
                    }
                } finally {
                    innerSnapshot.dispose()
                }
            }
        } finally {
            outerSnapshot.dispose()
        }

        assertEquals(1, outerChanges)
        assertEquals(1, innerChanges)
    }

    @Test
    fun testTransparentMutableSnapshotMergedWithNestedReadObserver() {
        var outerChanges = 0
        var innerChanges = 0
        val state by mutableIntStateOf(0)

        val outerSnapshot =
            TransparentObserverMutableSnapshot(
                parentSnapshot = currentSnapshot() as? MutableSnapshot,
                specifiedReadObserver = { outerChanges++ },
                specifiedWriteObserver = null,
                mergeParentObservers = false,
                ownsParentSnapshot = false,
            )

        try {
            outerSnapshot.enter {
                val innerSnapshot =
                    outerSnapshot.takeNestedSnapshot(readObserver = { innerChanges++ })

                try {
                    innerSnapshot.enter {
                        state // read
                    }
                } finally {
                    innerSnapshot.dispose()
                }
            }
        } finally {
            outerSnapshot.dispose()
        }

        assertEquals(1, outerChanges)
        assertEquals(1, innerChanges)
    }

    @Test
    fun testSimpleReclaimingState() {
        val state = mutableIntStateOf(0)
        assertEquals(1, usedRecords(state as StateObject))
        Snapshot.withMutableSnapshot {
            state.intValue = 1
            assertEquals(2, usedRecords(state as StateObject))
        }
        assertEquals(1, usedRecords(state as StateObject))
    }

    @Test
    fun testDeferredReclaimingState_Merged() {
        val state = mutableIntStateOf(0)

        assertEquals(1, usedRecords(state as StateObject))

        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        val snapshot3 = takeMutableSnapshot()

        snapshot1.enter { state.intValue = 1 }

        snapshot2.enter { state.intValue = 1 }

        snapshot3.enter { state.intValue = 1 }

        snapshot1.apply()
        snapshot2.apply()
        snapshot3.apply()

        assertEquals(1, usedRecords(state as StateObject))
    }

    @Test
    fun testDeferredReclaimingState_MultipleObjects() {
        val state1 = mutableIntStateOf(0)
        val state2 = mutableIntStateOf(0)
        val state3 = mutableIntStateOf(0)

        // Take a read-only snapshot to force states to be preserved that would otherwise
        // be overwritten immediately.
        val readonlySnapshot = takeSnapshot()

        val snapshot1 = takeMutableSnapshot()
        val snapshot2 = takeMutableSnapshot()
        val snapshot3 = takeMutableSnapshot()

        snapshot1.enter { state1.intValue = 1 }

        snapshot2.enter { state2.intValue = 2 }

        snapshot3.enter { state3.intValue = 3 }

        snapshot1.apply()
        snapshot2.apply()

        readonlySnapshot.enter {
            assertEquals(0, state1.intValue)
            assertEquals(0, state2.intValue)
            assertEquals(0, state3.intValue)
        }

        // Allow the state to be collected again.
        readonlySnapshot.dispose()

        snapshot3.apply()

        assertEquals(1, usedRecords(state1 as StateObject))
        assertEquals(1, usedRecords(state2 as StateObject))
        assertEquals(1, usedRecords(state3 as StateObject))
    }

    @Test
    fun testWriteCount() {
        val state = mutableIntStateOf(0)
        val writtenStates = mutableListOf<Any>()
        val snapshot = takeMutableSnapshot { write -> writtenStates.add(write) }
        try {
            snapshot.enter {
                assertEquals(0, writtenStates.size)
                assertEquals(0, snapshot.writeCount)
                state.intValue = 2
                assertEquals(1, writtenStates.size)
                assertEquals(1, snapshot.writeCount)
            }
        } finally {
            snapshot.dispose()
        }
        assertEquals(1, writtenStates.size)
        assertEquals(state, writtenStates[0])
        assertEquals(0, current.writeCount)
    }

    @Test
    fun testTransparentSnapshotWriteCount() {
        val state = mutableIntStateOf(0)
        val transparentSnapshot =
            TransparentObserverMutableSnapshot(
                parentSnapshot = currentSnapshot() as? MutableSnapshot,
                specifiedReadObserver = null,
                specifiedWriteObserver = null,
                mergeParentObservers = false,
                ownsParentSnapshot = false,
            )
        try {
            transparentSnapshot.enter {
                assertEquals(0, transparentSnapshot.writeCount)
                state.intValue = 2
                assertEquals(1, transparentSnapshot.writeCount)
            }
        } finally {
            transparentSnapshot.dispose()
        }
        assertEquals(1, current.writeCount)
    }

    @Suppress("AutoboxingStateValueProperty") // The point of this test
    @Test
    fun testSnapshotStateIsBornAccessible() {
        fun <T, V> test(create: () -> T, read: (T) -> V, update: (T) -> V) {
            val snapshot = takeMutableSnapshot()
            val created: Any? = null
            val modified =
                observeChanges(snapshot) {
                    val (state, initial) =
                        snapshot.enter {
                            val state = create()
                            state to read(state)
                        }

                    // Ensure the value is accessible and has its initial state
                    assertEquals(initial, read(state))
                    val newValue = snapshot.enter { update(state) }

                    // Ensure the test actually modified it
                    assertNotEquals(initial, newValue)

                    // Ensure the value still has its initial state
                    assertEquals(initial, read(state))
                    snapshot.apply().check()

                    // Ensure the value now has the modified state
                    assertEquals(newValue, read(state))
                }

            // The object is not considered modified
            assertFalse(created in modified)
        }

        test({ mutableStateOf("A") }, { it.value }) {
            it.value = "B"
            "B"
        }
        test(
            { mutableIntStateOf(1) },
            { it.value },
            {
                it.intValue = 2
                2
            },
        )
        test(
            { mutableLongStateOf(1L) },
            { it.value },
            {
                it.longValue = 2L
                2L
            },
        )
        test(
            { mutableFloatStateOf(1f) },
            { it.value },
            {
                it.floatValue = 2f
                2f
            },
        )
        test(
            { mutableDoubleStateOf(1.0) },
            { it.value },
            {
                it.doubleValue = 2.0
                2.0
            },
        )
        test(
            { mutableStateListOf<Int>() },
            { it.isEmpty() },
            {
                it.add(1)
                it.isEmpty()
            },
        )
        test(
            { mutableStateMapOf<Int, Int>() },
            { it.isEmpty() },
            {
                it[23] = 42
                it.isEmpty()
            },
        )
    }

    @Test
    fun readObserverIsMergedOnNestedReadonlySnapshot() {
        val result = mutableListOf<Int>()

        val readObserver1: (Any) -> Unit = { result += 1 }
        val readObserver2: (Any) -> Unit = { result += 2 }
        val readObserver3: (Any) -> Unit = { result += 3 }

        val state = mutableStateOf("")

        val snapshot = takeSnapshot(readObserver1)
        try {
            snapshot.enter {
                Snapshot.observe(readObserver2) { Snapshot.observe(readObserver3) { state.value } }
            }
        } finally {
            snapshot.dispose()
        }

        assertEquals(listOf(3, 2, 1), result)
    }

    @Test
    fun earlyReturnWithMutableSnapshot() {
        val state = mutableIntStateOf(0)
        fun test() {
            Snapshot.withMutableSnapshot {
                state.intValue = 1
                return
            }
        }

        test()

        assertEquals(state.intValue, 1)
    }

    @Test
    fun earlyReturnGlobal() {
        val state = mutableIntStateOf(0)
        fun test() {
            Snapshot.global {
                state.intValue = 1
                return
            }
        }

        val snapshot = takeMutableSnapshot()
        try {
            snapshot.enter {
                test()
                @Suppress("RemoveRedundantQualifierName") assertEquals(snapshot, Snapshot.current)
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun throwInWithMutableSnapshot() {
        assertFailsWith<IllegalStateException> {
            Snapshot.withMutableSnapshot { error("Test error") }
        }
    }

    @Test
    fun throwInApplyWithMutableSnapshot() {
        assertFailsWith<SnapshotApplyConflictException> {
            val state = mutableIntStateOf(0)
            Snapshot.withMutableSnapshot {
                Snapshot.global { state.intValue = 1 }
                state.intValue = 2
            }
        }
    }

    private fun usedRecords(state: StateObject): Int {
        var used = 0
        var current: StateRecord? = state.firstStateRecord
        while (current != null) {
            if (current.snapshotId > 0) used++
            current = current.next
        }
        return used
    }

    private var count = 0

    @OptIn(InternalComposeApi::class)
    @BeforeTest
    fun recordOpenSnapshots() {
        count = openSnapshotCount()
    }

    // Validate that the tests do not change the number of open snapshots
    @OptIn(InternalComposeApi::class)
    @AfterTest
    fun validateOpenSnapshots() {
        assertEquals(count, openSnapshotCount(), "A snapshot was not disposed correctly")
    }
}

internal fun <T> changesOf(state: State<T>, block: () -> Unit): Int {
    var changes = 0
    val removeObserver =
        Snapshot.registerApplyObserver { states, _ -> if (states.contains(state)) changes++ }
    try {
        block()
        Snapshot.sendApplyNotifications()
    } finally {
        removeObserver.dispose()
    }
    return changes
}

internal fun observeChanges(snapshot: Snapshot, block: () -> Unit): Set<Any> {
    var changes = setOf<Any>()
    val removeObserver =
        Snapshot.registerApplyObserver { states, changedSnapshot ->
            if (changedSnapshot == snapshot) changes = states
        }
    try {
        block()
        Snapshot.sendApplyNotifications()
    } finally {
        removeObserver.dispose()
    }
    return changes
}

internal fun readsOf(block: () -> Unit): Int {
    var reads = 0
    val snapshot = takeSnapshot(readObserver = { reads++ })
    try {
        snapshot.enter(block)
    } finally {
        snapshot.dispose()
    }
    return reads
}

internal inline fun <T> atomic(block: () -> T): T {
    val snapshot = takeMutableSnapshot()
    val result: T
    try {
        result = snapshot.enter { block() }
        snapshot.apply().check()
    } finally {
        snapshot.dispose()
    }
    return result
}

internal inline fun snapshot(block: () -> Unit): MutableSnapshot {
    val snapshot = takeMutableSnapshot()
    snapshot.enter(block)
    return snapshot
}
