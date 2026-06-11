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

package androidx.compose.ui.platform

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher

class GlobalSnapshotManagerTest {

    @AfterTest
    fun clear() {
        GlobalSnapshotManager.clear()
    }

    @Test
    fun refcountKeepsPumpAliveUntilLastHandleClosed() {
        val dispatcher = StandardTestDispatcher()
        val scheduler = dispatcher.scheduler

        var applyCount = 0
        val applyObserver = Snapshot.registerApplyObserver { _, _ -> applyCount++ }
        val state = mutableStateOf(0)

        try {
            val handle1 = GlobalSnapshotManager.register(dispatcher)
            val handle2 = GlobalSnapshotManager.register(dispatcher)

            assertNotNull(handle1)
            assertNotNull(handle2)

            // Trigger a write and allow the pump to deliver apply notifications.
            state.value++
            scheduler.advanceUntilIdle()
            val countAfterBothOpen = applyCount
            assertTrue(countAfterBothOpen > 0, "Expected apply notification after first write")

            // Close handle1 — the second handle keeps the pump alive.
            handle1.close()
            state.value++
            scheduler.advanceUntilIdle()
            val countAfterOneClose = applyCount
            assertTrue(
                countAfterOneClose > countAfterBothOpen,
                "Expected apply notification after closing one of two handles (pump should still run)"
            )

            // Close handle2 — the pump is now released.
            handle2.close()
            state.value++
            scheduler.advanceUntilIdle()
            // No new apply notification should have been dispatched by the torn-down pump.
            assertEquals(
                countAfterOneClose, applyCount,
                "Expected no apply notification after closing the last handle (pump should be dead)"
            )
        } finally {
            applyObserver.dispose()
        }
    }

    @Test
    fun doubleStartAndCloseDoesNotThrow() {
        val dispatcher = StandardTestDispatcher()

        val handle1 = GlobalSnapshotManager.register(dispatcher)
        val handle2 = GlobalSnapshotManager.register(dispatcher)

        assertNotNull(handle1)
        assertNotNull(handle2)

        handle1.close()
        handle2.close()
    }

    @Test
    fun nullHandleForContextWithoutDispatcher() {
        // EmptyCoroutineContext has no ContinuationInterceptor, so the context overload returns null.
        assertNull(GlobalSnapshotManager.register(kotlin.coroutines.EmptyCoroutineContext))
    }

    @Test
    fun nullHandleForContextWithImmediateDispatcher() {
        assertNull(GlobalSnapshotManager.register(Dispatchers.Unconfined + CoroutineName("x")))
    }

    @Test
    fun nullHandleForImmediateDispatcher() {
        assertNull(GlobalSnapshotManager.register(Dispatchers.Unconfined))
    }

    @Test
    fun distinctContextsOnSameDispatcherShareOnePump() {
        val dispatcher = StandardTestDispatcher()
        val scheduler = dispatcher.scheduler

        var applyCount = 0
        val applyObserver = Snapshot.registerApplyObserver { _, _ -> applyCount++ }
        val state = mutableStateOf(0)

        try {
            val handle1 = GlobalSnapshotManager.register(dispatcher + CoroutineName("a"))
            val handle2 = GlobalSnapshotManager.register(dispatcher + CoroutineName("b"))

            assertNotNull(handle1)
            assertNotNull(handle2)

            state.value++
            scheduler.advanceUntilIdle()
            val countAfterBothOpen = applyCount
            assertTrue(countAfterBothOpen > 0, "Expected apply notification after first write")

            // Closing one handle keeps the shared pump alive for the other context.
            handle1.close()
            state.value++
            scheduler.advanceUntilIdle()
            assertTrue(
                applyCount > countAfterBothOpen,
                "Expected the shared pump to stay alive after closing one of two handles"
            )

            handle2.close()
        } finally {
            applyObserver.dispose()
        }
    }
}
