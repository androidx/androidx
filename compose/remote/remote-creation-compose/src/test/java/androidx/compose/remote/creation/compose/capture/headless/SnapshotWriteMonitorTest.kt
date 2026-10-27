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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.compose.remote.creation.compose.capture.headless

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SnapshotWriteMonitorTest {

    @Test
    fun acquire_refCountsConcurrentHandles_andStopsWhenLastCloses() {
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(0)
        assertThat(SnapshotWriteMonitor.isRunning).isFalse()

        val first = SnapshotWriteMonitor.acquire()
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(1)
        assertThat(SnapshotWriteMonitor.isRunning).isTrue()

        val second = SnapshotWriteMonitor.acquire()
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(2)
        assertThat(SnapshotWriteMonitor.isRunning).isTrue()

        first.close()
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(1)
        assertThat(SnapshotWriteMonitor.isRunning).isTrue()

        second.close()
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(0)
        assertThat(SnapshotWriteMonitor.isRunning).isFalse()
    }

    @Test
    fun close_isIdempotent() {
        val first = SnapshotWriteMonitor.acquire()
        val second = SnapshotWriteMonitor.acquire()

        first.close()
        first.close()

        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(1)
        assertThat(SnapshotWriteMonitor.isRunning).isTrue()

        second.close()
        second.close()

        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(0)
        assertThat(SnapshotWriteMonitor.isRunning).isFalse()
    }

    @Test
    fun acquire_withExplicitContext_deliversApplyNotificationsAndCoalescesBursts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val stateA = mutableStateOf(0)
        val stateB = mutableStateOf(0)
        val batches = mutableListOf<Set<Any>>()
        val applyObserver = Snapshot.registerApplyObserver { changed, _ ->
            val relevant = changed.intersect(setOf(stateA, stateB))
            if (relevant.isNotEmpty()) batches += relevant
        }
        val handle = SnapshotWriteMonitor.acquire(dispatcher)
        try {
            stateA.value = 1
            stateB.value = 2
            assertThat(batches).isEmpty()

            runCurrent()
            assertThat(batches).containsExactly(setOf<Any>(stateA, stateB))
        } finally {
            handle.close()
        }

        // Once closed, global writes are no longer auto-dispatched on the dispatcher.
        try {
            stateA.value = 3
            runCurrent()
            assertThat(batches).hasSize(1)
            Snapshot.sendApplyNotifications()
            assertThat(batches).hasSize(2)
        } finally {
            applyObserver.dispose()
        }
    }

    @Test
    fun acquire_independentContexts_refCountedSeparately() = runTest {
        val dispatcherA = StandardTestDispatcher(testScheduler, name = "A")
        val dispatcherB = StandardTestDispatcher(testScheduler, name = "B")
        val state = mutableStateOf(0)
        var delivered = 0
        val applyObserver = Snapshot.registerApplyObserver { changed, _ ->
            if (state in changed) delivered++
        }
        val handleA = SnapshotWriteMonitor.acquire(dispatcherA)
        val handleB = SnapshotWriteMonitor.acquire(dispatcherB)
        try {
            assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(2)
            assertThat(SnapshotWriteMonitor.isRunning).isTrue()

            handleA.close()
            assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(1)
            assertThat(SnapshotWriteMonitor.isRunning).isTrue()

            // Context B is still active and continues delivering apply notifications.
            state.value = 1
            runCurrent()
            assertThat(delivered).isEqualTo(1)
        } finally {
            handleA.close()
            handleB.close()
            applyObserver.dispose()
        }

        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(0)
        assertThat(SnapshotWriteMonitor.isRunning).isFalse()
    }

    @Test
    fun acquireForSession_coalescesDefaultAndEmptyContexts_andUsesCustomInterceptor() = runTest {
        val defaultHandle = SnapshotWriteMonitor.acquireForSession(Dispatchers.Default + Job())
        val emptyHandle = SnapshotWriteMonitor.acquireForSession(EmptyCoroutineContext)
        try {
            assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(2)
            assertThat(SnapshotWriteMonitor.isRunning).isTrue()

            defaultHandle.close()
            assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(1)
            assertThat(SnapshotWriteMonitor.isRunning).isTrue()
        } finally {
            defaultHandle.close()
            emptyHandle.close()
        }
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(0)
        assertThat(SnapshotWriteMonitor.isRunning).isFalse()

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val state = mutableStateOf(0)
        var delivered = 0
        val applyObserver = Snapshot.registerApplyObserver { changed, _ ->
            if (state in changed) delivered++
        }
        val sessionA = SnapshotWriteMonitor.acquireForSession(testDispatcher + Job())
        val sessionB = SnapshotWriteMonitor.acquireForSession(testDispatcher + Job())
        try {
            assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(2)
            state.value = 1
            runCurrent()
            assertThat(delivered).isEqualTo(1)
        } finally {
            sessionA.close()
            sessionB.close()
            applyObserver.dispose()
        }
        assertThat(SnapshotWriteMonitor.activeSessionCount).isEqualTo(0)
        assertThat(SnapshotWriteMonitor.isRunning).isFalse()
    }
}
