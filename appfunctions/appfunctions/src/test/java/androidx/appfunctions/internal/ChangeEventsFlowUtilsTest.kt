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

package androidx.appfunctions.internal

import androidx.appfunctions.ObserveAppFunctionsEvent
import androidx.appfunctions.internal.ChangeEventsFlowUtils.debounceAndMerge
import androidx.appfunctions.metadata.AppFunctionName
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeEventsFlowUtilsTest {

    @Test
    fun testDebounceAndMerge_singleEventEmitted_emitsEventAfterDebounce() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob = collectEvents(debouncedFlow, results)
        runCurrent()

        val event = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1))
        sourceFlow.emit(event)

        // Event should not be emitted yet (before debounce time)
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2)
        assertThat(results).isEmpty()

        // Event should be emitted after debounce time
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2 + 10.milliseconds)
        assertThat(results).containsExactly(listOf(event))

        collectJob.cancel()
    }

    @Test
    fun testDebounceAndMerge_burstOfMixedEvents_emitsConsolidatedEventsPerType() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob = collectEvents(debouncedFlow, results)
        runCurrent()

        sourceFlow.emit(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1)))
        sourceFlow.emit(ObserveAppFunctionsEvent.StatesChanged(setOf(testFunction1)))
        sourceFlow.emit(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage2)))
        sourceFlow.emit(ObserveAppFunctionsEvent.StatesChanged(setOf(testFunction2)))

        // Event should not be emitted yet (before debounce time)
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2)
        assertThat(results).isEmpty()

        // Event should be emitted after debounce time
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2 + 10.milliseconds)
        assertThat(results)
            .containsExactly(
                listOf(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1, testPackage2))),
                listOf(ObserveAppFunctionsEvent.StatesChanged(setOf(testFunction1, testFunction2))),
            )
            .inOrder()

        collectJob.cancel()
    }

    @Test
    fun testDebounceAndMerge_newEventBeforeDebounceExpires_restartsDebounceTimer() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob = collectEvents(debouncedFlow, results)
        runCurrent()

        val event1 = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1))
        sourceFlow.emit(event1)

        // Advance half of debounce time
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2)
        assertThat(results).isEmpty()

        // Emit another event before debounce expires
        val event2 = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage2))
        sourceFlow.emit(event2)

        // Advance another half of debounce; the timer should have reset and not emitted yet
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2)
        assertThat(results).isEmpty()

        // Wait remaining half of debounce time for event 2
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2 + 10.milliseconds)
        assertThat(results)
            .containsExactly(
                listOf(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1, testPackage2)))
            )

        collectJob.cancel()
    }

    @Test
    fun testDebounceAndMerge_spacedEventsEmitted_emitsEventsSeparatelyWithoutMerging() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob = collectEvents(debouncedFlow, results)
        runCurrent()

        val event1 = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1))
        sourceFlow.emit(event1)
        advanceTimeBy(TEST_DEBOUNCE_MILLIS + 50.milliseconds)

        assertThat(results).containsExactly(listOf(event1))

        val event2 = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage2))
        sourceFlow.emit(event2)
        advanceTimeBy(TEST_DEBOUNCE_MILLIS + 50.milliseconds)

        assertThat(results).containsExactly(listOf(event1), listOf(event2)).inOrder()

        collectJob.cancel()
    }

    @Test
    fun testDebounceAndMerge_flowCancelledBeforeDebounce_doesNotEmit() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob = collectEvents(debouncedFlow, results)
        runCurrent()

        sourceFlow.emit(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1)))
        advanceTimeBy(TEST_DEBOUNCE_MILLIS / 2)

        // Cancel flow before debounce expires
        collectJob.cancel()
        advanceTimeBy(TEST_DEBOUNCE_MILLIS)

        assertThat(results).isEmpty()
    }

    @Test
    fun testDebounceAndMerge_multipleCollectors_bothReceiveFullConsolidatedEvents() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results1 = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob1 = collectEvents(debouncedFlow, results1)

        val results2 = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob2 = collectEvents(debouncedFlow, results2)
        runCurrent()

        sourceFlow.emit(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1)))
        advanceTimeBy(30.milliseconds)
        sourceFlow.emit(ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage2)))

        advanceTimeBy(TEST_DEBOUNCE_MILLIS + 50.milliseconds)

        val expectedEvent =
            ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1, testPackage2))

        assertThat(results1).containsExactly(listOf(expectedEvent))
        assertThat(results2).containsExactly(listOf(expectedEvent))

        collectJob1.cancel()
        collectJob2.cancel()
    }

    @Test
    fun testDebounceAndMerge_lateJoinerCollects_doesNotReceivePriorEvents() = runTest {
        val sourceFlow = MutableSharedFlow<ObserveAppFunctionsEvent>(extraBufferCapacity = 64)
        val debouncedFlow = sourceFlow.debounceAndMerge(TEST_DEBOUNCE_MILLIS)

        val results1 = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob1 = collectEvents(debouncedFlow, results1)
        runCurrent()

        val eventA = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage1))
        sourceFlow.emit(eventA)
        advanceTimeBy(TEST_DEBOUNCE_MILLIS + 50.milliseconds)

        // Second collector starts after debounce expired
        val results2 = mutableListOf<List<ObserveAppFunctionsEvent>>()
        val collectJob2 = collectEvents(debouncedFlow, results2)
        runCurrent()

        val eventB = ObserveAppFunctionsEvent.MetadataChanged(setOf(testPackage2))
        sourceFlow.emit(eventB)
        advanceTimeBy(TEST_DEBOUNCE_MILLIS + 50.milliseconds)

        assertThat(results1).containsExactly(listOf(eventA), listOf(eventB)).inOrder()
        assertThat(results2).containsExactly(listOf(eventB))

        collectJob1.cancel()
        collectJob2.cancel()
    }

    private fun TestScope.collectEvents(
        flow: Flow<ObserveAppFunctionsEvent>,
        results: MutableList<List<ObserveAppFunctionsEvent>>,
    ): Job {
        return launch { flow.collect { event -> results.add(listOf(event)) } }
    }

    private companion object {
        private val TEST_DEBOUNCE_MILLIS = 100.milliseconds
        private const val testPackage1 = "com.example.pkg1"
        private const val testPackage2 = "com.example.pkg2"
        private val testFunction1 = AppFunctionName("com.example.pkg1", "func1")
        private val testFunction2 = AppFunctionName("com.example.pkg1", "func2")
    }
}
