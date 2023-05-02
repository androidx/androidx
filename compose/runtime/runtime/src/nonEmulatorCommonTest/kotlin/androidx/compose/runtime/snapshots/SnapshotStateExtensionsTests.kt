/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.runtime.asDoubleState
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.asLongState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SnapshotStateExtensionsTests {

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun stateAsIntStateDispatchesSnapshotUpdates() = runTest {
        val snapshotObservationJob = Job()
        val state = mutableIntStateOf(512)
        val intState = state.asIntState()
        val intSnapshotHistory = getSnapshotHistory(snapshotObservationJob) { intState.intValue }
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asIntState() returned a State that dispatched unexpected values when its " +
                "corresponding state had been initialized and not modified.",
            expected = listOf(512),
            actual = intSnapshotHistory.value
        )

        state.intValue = 1024
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asIntState() returned a State that dispatched unexpected values when its " +
                "corresponding state was reassigned and changed.",
            expected = listOf(512, 1024),
            actual = intSnapshotHistory.value
        )

        state.intValue = 2048
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asIntState() returned a State that dispatched unexpected values when its " +
                "corresponding state was reassigned and changed.",
            expected = listOf(512, 1024, 2048),
            actual = intSnapshotHistory.value
        )
        snapshotObservationJob.cancel()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun stateAsLongStateDispatchesSnapshotUpdates() = runTest {
        val snapshotObservationJob = Job()
        val state = mutableLongStateOf(1000L)
        val longState = state.asLongState()
        val longSnapshotHistory = getSnapshotHistory(snapshotObservationJob) { longState.longValue }
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asLongState() returned a State that dispatched unexpected values when its " +
                "corresponding state had been initialized and not modified.",
            expected = listOf(1000L),
            actual = longSnapshotHistory.value
        )

        state.longValue = 2000L
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asLongState() returned a State that dispatched unexpected values when its " +
                "corresponding state was reassigned and changed.",
            expected = listOf(1000L, 2000L),
            actual = longSnapshotHistory.value
        )

        state.longValue = 3000L
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asLongState() returned a State that dispatched unexpected values when its " +
                "corresponding state was reassigned and changed.",
            expected = listOf(1000L, 2000L, 3000L),
            actual = longSnapshotHistory.value
        )
        snapshotObservationJob.cancel()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun stateAsFloatStateDispatchesSnapshotUpdates() = runTest {
        val snapshotObservationJob = Job()
        val state = mutableFloatStateOf(0.0f)
        val floatState = state.asFloatState()
        val floatSnapshotHistory = getSnapshotHistory(snapshotObservationJob) {
            floatState.floatValue
        }
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asFloatState() returned a State that dispatched unexpected values when " +
                "its corresponding state had been initialized and not modified.",
            expected = listOf(0f),
            actual = floatSnapshotHistory.value
        )

        state.floatValue = 1f
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asFloatState() returned a State that dispatched unexpected values when " +
                "its corresponding state was reassigned and changed.",
            expected = listOf(0f, 1f),
            actual = floatSnapshotHistory.value
        )

        state.floatValue = 2f
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asFloatState() returned a State that dispatched unexpected values when " +
                "its corresponding state was reassigned and changed.",
            expected = listOf(0f, 1f, 2f),
            actual = floatSnapshotHistory.value
        )
        snapshotObservationJob.cancel()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun stateAsDoubleStateDispatchesSnapshotUpdates() = runTest {
        val snapshotObservationJob = Job()
        val state = mutableDoubleStateOf(1.0)
        val doubleState = state.asDoubleState()
        val doubleSnapshotHistory = getSnapshotHistory(snapshotObservationJob) {
            doubleState.doubleValue
        }
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asDoubleState() returned a State that dispatched unexpected values when " +
                "its corresponding state had been initialized and not modified.",
            expected = listOf(1.0),
            actual = doubleSnapshotHistory.value
        )

        state.doubleValue = 2.5
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asDoubleState() returned a State that dispatched unexpected values when " +
                "its corresponding state was reassigned and changed.",
            expected = listOf(1.0, 2.5),
            actual = doubleSnapshotHistory.value
        )

        state.doubleValue = 5.0
        advanceGlobalSnapshotAndSettle()

        assertEquals(
            message = "asDoubleState() returned a State that dispatched unexpected values when " +
                "its corresponding state was reassigned and changed.",
            expected = listOf(1.0, 2.5, 5.0),
            actual = doubleSnapshotHistory.value
        )
        snapshotObservationJob.cancel()
    }

    @ExperimentalCoroutinesApi
    private fun TestScope.advanceGlobalSnapshotAndSettle() {
        Snapshot.sendApplyNotifications()
        testScheduler.advanceUntilIdle()
    }

    private suspend fun <T> getSnapshotHistory(
        coroutineContext: CoroutineContext = EmptyCoroutineContext,
        block: () -> T
    ): StateFlow<List<T>> {
        // Build manually rather than use `snapshotFlow {}`, because snapshotFlow implicitly
        // has the behavior of `distinctUntilChanged()`, which hides behavior we want to test.
        val snapshotFlow = callbackFlow<T> {
            val readSet = mutableSetOf<Any>()
            val readObserver: (Any) -> Unit = { readSet.add(it) }

            fun emitLatestValue() = channel.trySendBlocking(
                with(Snapshot.takeSnapshot(readObserver)) {
                    try {
                        enter { block() }
                    } finally {
                        dispose()
                    }
                }
            )

            emitLatestValue()
            val handle = Snapshot.registerApplyObserver { changed, _ ->
                for (changedObjects in changed) {
                    if (readSet.any { it in changed }) {
                        emitLatestValue()
                    }
                }
            }

            awaitClose { handle.dispose() }
        }

        return snapshotFlow
            .runningFold(emptyList<T>()) { acc, value -> acc + value }
            .stateIn(CoroutineScope(currentCoroutineContext() + coroutineContext))
    }
}