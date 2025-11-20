/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeRuntimeApi::class)

package androidx.compose.runtime.snapshots.tooling

import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.Snapshot.Companion.openSnapshotCount
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.test.IgnoreJsTarget

class SnapshotObserverTests {
    @Test
    fun canAddAndRemoveObserver() {
        observeSnapshots(object : SnapshotObserver {}) {}
    }

    @Test
    fun canObserverReadonlySnapshotCreation() {
        val observed = mutableScatterSetOf<Snapshot>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onCreated(
                    snapshot: Snapshot,
                    parent: Snapshot?,
                    observers: SnapshotInstanceObservers?,
                ) {
                    observed.add(snapshot)
                }
            }
        ) {
            val created = Snapshot.takeSnapshot()
            assertTrue(created in observed)
            created.dispose()
        }
    }

    @Test
    fun canObserverMutableSnapshotCreation() {
        val observed = mutableScatterSetOf<Snapshot>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onCreated(
                    snapshot: Snapshot,
                    parent: Snapshot?,
                    observers: SnapshotInstanceObservers?,
                ) {
                    observed.add(snapshot)
                }
            }
        ) {
            val created = Snapshot.takeMutableSnapshot()
            assertTrue(created in observed)
            created.dispose()
        }
    }

    @Test
    fun canObserveApply() {
        val applied = mutableListOf<Pair<Snapshot, Set<Any>>>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onApplied(snapshot: Snapshot, changed: Set<Any>) {
                    applied.add(snapshot to changed)
                }
            }
        ) {
            val state = mutableIntStateOf(10)
            val snapshot = Snapshot.takeMutableSnapshot()
            snapshot.enter { state.intValue = 12 }
            snapshot.apply().check()
            val apply = applied.first()
            assertEquals(snapshot, apply.first)
            assertTrue(apply.second.contains(state))
            snapshot.dispose()
        }
    }

    @Test
    fun canObserverDisposeOfReadonlySnapshot() {
        val disposed = mutableScatterSetOf<Snapshot>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreDispose(snapshot: Snapshot) {
                    disposed.add(snapshot)
                }
            }
        ) {
            val snapshot = Snapshot.takeSnapshot()
            snapshot.dispose()
            assertTrue(disposed.contains(snapshot))
        }
    }

    @Test
    fun canObserverDisposeOfMutableSnapshot_NotApplied() {
        val disposed = mutableScatterSetOf<Snapshot>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreDispose(snapshot: Snapshot) {
                    disposed.add(snapshot)
                }
            }
        ) {
            val snapshot = Snapshot.takeMutableSnapshot()
            snapshot.dispose()
            assertTrue(disposed.contains(snapshot))
        }
    }

    @Test
    fun canObserverDisposeOfMutableSnapshot_Applied() {
        val disposed = mutableScatterSetOf<Snapshot>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreDispose(snapshot: Snapshot) {
                    disposed.add(snapshot)
                }
            }
        ) {
            val snapshot = Snapshot.takeMutableSnapshot()
            snapshot.apply().check()
            snapshot.dispose()
            assertTrue(disposed.contains(snapshot))
        }
    }

    @Test
    fun canObserveApplyOfNestedSnapshot() {
        val applied = mutableListOf<Pair<Snapshot, Set<Any>>>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onApplied(snapshot: Snapshot, changed: Set<Any>) {
                    applied.add(snapshot to changed)
                }
            }
        ) {
            val state = mutableIntStateOf(10)
            val snapshot = Snapshot.takeMutableSnapshot()
            val nestedSnapshot = snapshot.takeNestedMutableSnapshot()
            nestedSnapshot.enter { state.intValue = 12 }
            nestedSnapshot.apply().check()
            snapshot.apply().check()

            val nestedApply = applied.first()
            assertEquals(nestedSnapshot, nestedApply.first)
            assertTrue(nestedApply.second.contains(state))
            nestedSnapshot.dispose()
            val snapshotApply = applied.last()
            assertEquals(snapshot, snapshotApply.first)
            assertTrue(snapshotApply.second.contains(state))
            snapshot.dispose()
        }
    }

    @Test
    fun canObserveReadsInReadonlySnapshot() {
        val state = mutableIntStateOf(10)
        val read = mutableListOf<Pair<Any, Boolean>>()

        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreCreate(
                    parent: Snapshot?,
                    readonly: Boolean,
                ): SnapshotInstanceObservers {
                    return SnapshotInstanceObservers(readObserver = { read.add(it to true) })
                }
            }
        ) {
            val snapshot = Snapshot.takeSnapshot(readObserver = { read.add(it to false) })
            try {
                val result = snapshot.enter { state.intValue }
                assertEquals(10, result)
                assertEquals(mutableListOf<Pair<Any, Boolean>>(state to true, state to false), read)
            } finally {
                snapshot.dispose()
            }
        }
    }

    @Test
    fun canObserveReadsInMutableSnapshot() {
        val state = mutableIntStateOf(10)
        val read = mutableListOf<Pair<Any, Boolean>>()

        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreCreate(
                    parent: Snapshot?,
                    readonly: Boolean,
                ): SnapshotInstanceObservers {
                    return SnapshotInstanceObservers(readObserver = { read.add(it to true) })
                }
            }
        ) {
            val snapshot = Snapshot.takeMutableSnapshot(readObserver = { read.add(it to false) })
            try {
                val result = snapshot.enter { state.intValue }
                assertEquals(10, result)
                assertEquals(mutableListOf<Pair<Any, Boolean>>(state to true, state to false), read)
            } finally {
                snapshot.dispose()
            }
        }
    }

    @Test
    fun canObserveWrites() {
        val state = mutableIntStateOf(10)
        val writes = mutableListOf<Pair<Any, Boolean>>()
        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreCreate(
                    parent: Snapshot?,
                    readonly: Boolean,
                ): SnapshotInstanceObservers {
                    return SnapshotInstanceObservers(writeObserver = { writes.add(it to true) })
                }
            }
        ) {
            val snapshot = Snapshot.takeMutableSnapshot(writeObserver = { writes.add(it to false) })
            try {
                val result =
                    snapshot.enter {
                        state.intValue = 20
                        state.intValue
                    }
                assertEquals(20, result)
                assertEquals(
                    expected = mutableListOf<Pair<Any, Boolean>>(state to true, state to false),
                    actual = writes,
                )
            } finally {
                snapshot.dispose()
            }
        }
    }

    @Test
    @IgnoreJsTarget // b/409727050
    fun canHaveMultipleObservers() {
        val events = mutableListOf<Pair<Any?, String>>()
        fun observer(prefix: String) =
            object : SnapshotObserver {
                override fun onPreCreate(
                    parent: Snapshot?,
                    readonly: Boolean,
                ): SnapshotInstanceObservers {
                    record(parent, "creating, readonly = $readonly")
                    return SnapshotInstanceObservers(
                        readObserver = { record(it, "reading") },
                        writeObserver = { record(it, "writing") },
                    )
                }

                override fun onCreated(
                    snapshot: Snapshot,
                    parent: Snapshot?,
                    observers: SnapshotInstanceObservers?,
                ) {
                    record(snapshot to parent, "created")
                }

                override fun onPreDispose(snapshot: Snapshot) {
                    record(snapshot, "disposing")
                }

                override fun onApplied(snapshot: Snapshot, changed: Set<Any>) {
                    record(snapshot to changed, "applied")
                }

                fun record(value: Any?, msg: String) {
                    events.add(value to "$prefix: $msg")
                }
            }
        val state1 = mutableIntStateOf(1)
        val state2 = mutableIntStateOf(2)

        observeSnapshots(observer("Outer")) {
            observeSnapshots(observer("Inner")) {
                val ros1 = Snapshot.takeSnapshot()
                try {
                    ros1.enter {
                        state1.intValue
                        state2.intValue
                    }
                } finally {
                    ros1.dispose()
                }

                val ms1 = Snapshot.takeMutableSnapshot()
                try {
                    ms1.enter { state1.intValue = 11 }
                    ms1.apply().check()
                } finally {
                    ms1.dispose()
                }
                assertContentEquals(
                    listOf(
                        null to "Outer: creating, readonly = true",
                        null to "Inner: creating, readonly = true",
                        (ros1 to null) to "Outer: created",
                        (ros1 to null) to "Inner: created",
                        state1 to "Inner: reading",
                        state1 to "Outer: reading",
                        state2 to "Inner: reading",
                        state2 to "Outer: reading",
                        ros1 to "Outer: disposing",
                        ros1 to "Inner: disposing",
                        null to "Outer: creating, readonly = false",
                        null to "Inner: creating, readonly = false",
                        (ms1 to null) to "Outer: created",
                        (ms1 to null) to "Inner: created",
                        state1 to "Inner: writing",
                        state1 to "Outer: writing",
                        (ms1 to setOf(state1)) to "Outer: applied",
                        (ms1 to setOf(state1)) to "Inner: applied",
                        ms1 to "Outer: disposing",
                        ms1 to "Inner: disposing",
                    ),
                    events as List<*>,
                )
            }
        }
    }

    @Test
    fun receivesTheCorrectParent() {
        val events = mutableListOf<Pair<Any?, String>>()
        fun observer() =
            object : SnapshotObserver {
                override fun onPreCreate(
                    parent: Snapshot?,
                    readonly: Boolean,
                ): SnapshotInstanceObservers {
                    record(parent, "creating, readonly = $readonly")
                    return SnapshotInstanceObservers(
                        readObserver = { record(it, "reading") },
                        writeObserver = { record(it, "writing") },
                    )
                }

                override fun onCreated(
                    snapshot: Snapshot,
                    parent: Snapshot?,
                    observers: SnapshotInstanceObservers?,
                ) {
                    record(snapshot to parent, "created")
                }

                override fun onPreDispose(snapshot: Snapshot) {
                    record(snapshot, "disposing")
                }

                override fun onApplied(snapshot: Snapshot, changed: Set<Any>) {
                    record(snapshot to changed, "applied")
                }

                fun record(value: Any?, msg: String) {
                    events.add(value to msg)
                }
            }

        observeSnapshots(observer()) {
            val ro1 = Snapshot.takeSnapshot()
            val ro2 = ro1.takeNestedSnapshot()
            ro2.dispose()
            ro1.dispose()

            val ms1 = Snapshot.takeMutableSnapshot()
            val ms2 = ms1.takeNestedMutableSnapshot()
            ms1.dispose()
            ms2.dispose()

            assertEquals(
                listOf(
                    null to "creating, readonly = true",
                    (ro1 to null) to "created",
                    ro1 to "creating, readonly = true",
                    (ro2 to ro1) to "created",
                    ro2 to "disposing",
                    ro1 to "disposing",
                    null to "creating, readonly = false",
                    (ms1 to null) to "created",
                    ms1 to "creating, readonly = false",
                    (ms2 to ms1) to "created",
                    ms1 to "disposing",
                    ms2 to "disposing",
                ),
                events,
            )
        }
    }

    @Test
    fun canCorrelateCreatingAndCreating() {
        var key: SnapshotInstanceObservers? = null
        observeSnapshots(
            object : SnapshotObserver {
                override fun onPreCreate(
                    parent: Snapshot?,
                    readonly: Boolean,
                ): SnapshotInstanceObservers {
                    val result = SnapshotInstanceObservers()
                    key = result
                    return result
                }

                override fun onCreated(
                    snapshot: Snapshot,
                    parent: Snapshot?,
                    observers: SnapshotInstanceObservers?,
                ) {
                    assertEquals(observers, key)
                }
            }
        ) {
            val snapshot = Snapshot.takeMutableSnapshot()
            snapshot.dispose()
        }
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
        assertEquals(count, openSnapshotCount(), "Snapshot not disposed?")
    }
}

@ExperimentalComposeRuntimeApi
private inline fun <R> observeSnapshots(observer: SnapshotObserver, block: () -> R): R {
    val handle = Snapshot.observeSnapshots(observer)
    try {
        return block()
    } finally {
        handle.dispose()
    }
}
