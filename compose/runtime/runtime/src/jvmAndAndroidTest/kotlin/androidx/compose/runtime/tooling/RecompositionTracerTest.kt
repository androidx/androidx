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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class Ref<T : Any> {
    lateinit var value: T
}

private fun Any.asString() = Snapshot.withoutReadObservation { toString() }

@OptIn(InternalComposeTracingApi::class, ExperimentalComposeRuntimeApi::class)
class RecompositionTracerTest {

    sealed interface TraceEvent {
        data class BeginSection(val scope: RecomposeScope, val flowIds: List<Long> = emptyList()) :
            TraceEvent {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is BeginSection) return false
                return scope == other.scope
            }

            override fun hashCode(): Int = scope.hashCode()
        }

        data class EndSection(val scope: RecomposeScope) : TraceEvent

        data class StateRead(
            val scope: RecomposeScope,
            val value: String,
            val flowIds: List<Long> = emptyList(),
        ) : TraceEvent {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is StateRead) return false
                return scope == other.scope && value == other.value
            }

            override fun hashCode(): Int = 31 * scope.hashCode() + value.hashCode()
        }

        data class StateWrite(val value: String, val flowIds: List<Long> = emptyList()) :
            TraceEvent {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is StateWrite) return false
                return value == other.value
            }

            override fun hashCode(): Int = value.hashCode()
        }

        data class DirectInvalidation(val scope: RecomposeScope, val flowId: Long = 0) :
            TraceEvent {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is DirectInvalidation) return false
                return scope == other.scope
            }

            override fun hashCode(): Int = scope.hashCode()
        }
    }

    class MockTraceEventListener : RecompositionTracer.TraceEventListener {
        val events = mutableListOf<TraceEvent>()
        var enabled = true

        override fun onStateRead(
            scope: RecomposeScope,
            value: Any,
            flowIds: List<Long>,
            stackTrace: List<StackTraceElement>,
        ) {
            events.add(TraceEvent.StateRead(scope, value.asString(), flowIds))
        }

        override fun onStateWrite(
            value: Any,
            flowIds: List<Long>,
            stackTrace: List<StackTraceElement>,
        ) {
            events.add(TraceEvent.StateWrite(value.asString(), flowIds))
        }

        override fun onBeginRecomposeGroup(scope: RecomposeScope, flowIds: List<Long>) {
            events.add(TraceEvent.BeginSection(scope, flowIds))
        }

        override fun onEndRecomposeGroup(scope: RecomposeScope) {
            events.add(TraceEvent.EndSection(scope))
        }

        override fun onDirectInvalidation(
            scope: RecomposeScope,
            flowId: Long,
            stackTrace: List<StackTraceElement>,
        ) {
            events.add(TraceEvent.DirectInvalidation(scope, flowId))
        }

        override fun isEnabled(): Boolean = enabled

        fun clear() {
            events.clear()
        }

        inline fun <reified T : TraceEvent> findEvents(): List<T> = events.filterIsInstance<T>()
    }

    private fun assertEvents(actual: List<TraceEvent>, expected: List<TraceEvent>) {
        assertEquals(expected, actual)

        // Verify flow IDs mapping
        val accumulatedWriteFlows = mutableSetOf<Long>()
        for (act in actual) {
            when (act) {
                is TraceEvent.StateWrite -> {
                    assertTrue("Write event should have flow IDs", act.flowIds.isNotEmpty())
                    accumulatedWriteFlows.addAll(act.flowIds)
                }
                is TraceEvent.BeginSection -> {
                    assertTrue("Recompose group should have flow IDs", act.flowIds.isNotEmpty())
                    if (accumulatedWriteFlows.isNotEmpty()) {
                        assertTrue(
                            "Recompose flows (${act.flowIds}) should intersect with accumulated write flows ($accumulatedWriteFlows)",
                            act.flowIds.any { it in accumulatedWriteFlows },
                        )
                    }
                    accumulatedWriteFlows.clear()
                }
                else -> {}
            }
        }
    }

    private fun runRecompositionTracingTest(
        block: suspend CompositionTestScope.(MockTraceEventListener) -> Unit
    ) = compositionTest {
        val listener = MockTraceEventListener()
        val tracer = RecompositionTracer(listener)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { tracer.runTracing() }
        try {
            block(listener)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun testStateReadsAndWritesEmitTraces() = runRecompositionTracingTest { listener ->
        val dataState = mutableStateOf(0)
        var data by dataState
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            Text("$data")
        }

        listener.clear()

        // Now perform a write that should be linked to the recorded read
        data++
        expectChanges()

        val targetScope = scope!!
        assertEvents(
            listener.events,
            listOf(
                TraceEvent.StateWrite(dataState.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, dataState.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )
    }

    @Test
    fun testCleanupOnScopeDisposed() = runRecompositionTracingTest { listener ->
        var show by mutableStateOf(true)
        val dataState = mutableStateOf(0)
        var data by dataState

        val myContent = @Composable { Text("$data") }
        compose {
            if (show) {
                myContent()
            }
        }

        listener.clear()

        // Remove the scope from composition
        show = false
        expectChanges() // This should dispose the scope

        listener.clear()

        // Write to state. The scope is disposed, so this should NOT emit flowIds for that scope.
        data++

        assertEvents(listener.events, emptyList())
    }

    @Test
    fun testMultipleStateReadsInSameScope() = runRecompositionTracingTest { listener ->
        val dataState1 = mutableStateOf(0)
        var data1 by dataState1
        val dataState2 = mutableStateOf(0)
        var data2 by dataState2
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            Text("$data1 $data2")
        }

        listener.clear()

        // Write to data1
        data1++
        expectChanges()

        val targetScope = scope!!
        val events1 = listener.events
        assertEvents(
            events1,
            listOf(
                TraceEvent.StateWrite(dataState1.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, dataState1.asString()),
                TraceEvent.StateRead(targetScope, dataState2.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )

        listener.clear()

        // Write to data2
        data2++
        expectChanges()

        val events2 = listener.events
        assertEvents(
            events2,
            listOf(
                TraceEvent.StateWrite(dataState2.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, dataState1.asString()),
                TraceEvent.StateRead(targetScope, dataState2.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )
    }

    @Test
    fun testStateReadsAndWritesInDifferentScopes() = runRecompositionTracingTest { listener ->
        val parentDataState = mutableStateOf(0)
        var parentData by parentDataState
        val childDataState = mutableStateOf(0)
        var childData by childDataState
        var parentScope: RecomposeScope? = null
        val childScopeHolder = Ref<RecomposeScope>()

        compose {
            parentScope = currentRecomposeScope
            Text("$parentData")
            Child(childScopeHolder) { childData }
        }

        listener.clear()

        // Write parentData only -> should invalidate parent recompose group
        parentData++
        expectChanges()

        val targetParentScope = parentScope!!
        val parentEvents = listener.events
        assertEvents(
            parentEvents,
            listOf(
                TraceEvent.StateWrite(parentDataState.asString()),
                TraceEvent.BeginSection(targetParentScope),
                TraceEvent.StateRead(targetParentScope, parentDataState.asString()),
                TraceEvent.EndSection(targetParentScope),
            ),
        )

        listener.clear()

        // Write childData only -> should invalidate child recompose group
        childData++
        expectChanges()

        val childEvents = listener.events
        val childScope = childScopeHolder.value
        assertEvents(
            childEvents,
            listOf(
                TraceEvent.StateWrite(childDataState.asString()),
                TraceEvent.BeginSection(childScope),
                TraceEvent.StateRead(childScope, childDataState.asString()),
                TraceEvent.EndSection(childScope),
            ),
        )
    }

    @Test
    fun testConditionalReadsCleanUpStaleReads() = runRecompositionTracingTest { listener ->
        val readAState = mutableStateOf(true)
        var readA by readAState
        val stateAState = mutableStateOf(0)
        var stateA by stateAState
        val stateBState = mutableStateOf(0)
        var stateB by stateBState
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            if (readA) {
                Text("A: $stateA")
            } else {
                Text("B: $stateB")
            }
        }

        listener.clear()

        // Change condition to read B instead of A
        readA = false
        expectChanges() // Recomposes, should clear stateA from scopedReads, and read stateB
        listener.clear()

        // Now write to stateA. It is no longer read, so it should NOT trace any flows.
        stateA++
        val eventsA = listener.events
        assertEvents(eventsA, emptyList())

        listener.clear()

        // Write to stateB. It is currently read, so it should trace flows.
        stateB++
        expectChanges()

        val targetScope = scope!!
        val eventsB = listener.events
        assertEvents(
            eventsB,
            listOf(
                TraceEvent.StateWrite(stateBState.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, readAState.asString()),
                TraceEvent.StateRead(targetScope, stateBState.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )
    }

    @Test
    fun testObservationDisabled() = runRecompositionTracingTest { listener ->
        listener.enabled = false
        var data by mutableStateOf(0)

        compose { Text("$data") }

        data++
        expectChanges()

        // Tracer is disabled, so there should be no events recorded
        assertEvents(listener.events, emptyList())
    }

    @Test
    fun testEnabledDisabledToggling() = runRecompositionTracingTest { listener ->
        val dataState = mutableStateOf(0)
        var data by dataState
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            Text("$data")
        }

        listener.clear()

        // 1. Write when enabled
        data++
        expectChanges()

        val targetScope = scope!!
        assertEvents(
            listener.events,
            listOf(
                TraceEvent.StateWrite(dataState.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, dataState.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )

        listener.clear()

        // 2. Disable listener and write
        listener.enabled = false
        data++
        expectChanges()

        // No events should be recorded when disabled
        assertEvents(listener.events, emptyList())

        listener.clear()

        // 3. Re-enable listener and write
        listener.enabled = true
        data++
        expectChanges()

        assertEvents(
            listener.events,
            listOf(
                TraceEvent.StateWrite(dataState.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, dataState.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )
    }

    @Test
    fun testDerivedStateOfInvalidation() = runRecompositionTracingTest { listener ->
        var underlyingState by mutableStateOf(0)
        val derivedState = androidx.compose.runtime.derivedStateOf { underlyingState > 0 }

        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Text("${derivedState.value}")
        }

        listener.clear()

        // Write to underlyingState which changes derivedState value (false -> true)
        underlyingState++
        expectChanges()

        val targetScope = scope!!
        assertEvents(
            listener.events,
            listOf(
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, derivedState.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )
    }

    @Test
    fun testMultipleStateWritesInvalidateSameScope() = runRecompositionTracingTest { listener ->
        val stateA = mutableStateOf(0)
        var dataA by stateA
        val stateB = mutableStateOf(0)
        var dataB by stateB
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            Text("$dataA $dataB")
        }

        listener.clear()

        // Perform multiple writes sequentially in the global snapshot
        stateA.value++
        stateB.value++
        expectChanges()

        val targetScope = scope!!
        assertEvents(
            listener.events,
            listOf(
                TraceEvent.StateWrite(stateA.asString()),
                TraceEvent.StateWrite(stateB.asString()),
                TraceEvent.BeginSection(targetScope),
                TraceEvent.StateRead(targetScope, stateA.asString()),
                TraceEvent.StateRead(targetScope, stateB.asString()),
                TraceEvent.EndSection(targetScope),
            ),
        )
    }

    @Test
    fun testReadsAndWritesHaveDifferentFlowIdsButBothConnectToRecompose() =
        runRecompositionTracingTest { listener ->
            val dataState = mutableStateOf(0)
            var data by dataState

            compose { Text("$data") }

            // Capture the read flow ID from the initial composition
            val initialReadEvents = listener.findEvents<TraceEvent.StateRead>()
            assertTrue("Expected initial read event", initialReadEvents.isNotEmpty())
            val initialReadFlowId = initialReadEvents[0].flowIds.single()

            listener.clear()

            // Write to state to trigger recomposition
            data++
            expectChanges()

            // Events should be: StateWrite, BeginSection, StateRead, EndSection
            val writeEvents = listener.findEvents<TraceEvent.StateWrite>()
            val beginEvents = listener.findEvents<TraceEvent.BeginSection>()

            assertEquals(1, writeEvents.size)
            assertEquals(1, beginEvents.size)

            val writeFlowIds = writeEvents[0].flowIds
            val beginFlowIds = beginEvents[0].flowIds

            // Read flow ID should be in beginSection flow IDs
            assertTrue(
                "Recompose group should connect to read: read=$initialReadFlowId, begin=$beginFlowIds",
                initialReadFlowId in beginFlowIds,
            )
            // Write flow IDs should intersect with beginSection flow IDs
            assertTrue(
                "Recompose group should connect to write",
                writeFlowIds.any { it in beginFlowIds },
            )
            // Read flow ID should NOT be in write flow IDs!
            assertTrue(
                "Read flow ID should not be in write flow IDs",
                writeFlowIds.none { it == initialReadFlowId },
            )
        }

    @Test
    fun testScopeSkippedBetweenReadAndInvalidatingWrite() =
        runRecompositionTracingTest { listener ->
            val dataState = mutableStateOf(0)
            var trigger by mutableStateOf(0)
            val scopeHolder = Ref<RecomposeScope>()

            compose {
                Text("$trigger")
                Child(scopeHolder) { dataState.value }
            }

            val targetScope = scopeHolder.value
            listener.clear()

            // 1. Force parent to recompose. Child will be skipped because its args haven't changed.
            trigger++
            expectChanges()

            // Verify Child was NOT recomposed
            val childRecomposed =
                listener.findEvents<TraceEvent.BeginSection>().any { it.scope == targetScope }
            assertTrue("Child should have been skipped", !childRecomposed)

            listener.clear()

            // 2. Now write to dataState. Child should recompose, and the event should be traced
            // properly.
            dataState.value++
            expectChanges()

            assertEvents(
                listener.events,
                listOf(
                    TraceEvent.StateWrite(dataState.asString()),
                    TraceEvent.BeginSection(targetScope),
                    TraceEvent.StateRead(targetScope, dataState.asString()),
                    TraceEvent.EndSection(targetScope),
                ),
            )
        }
}

@Composable
private fun Child(scopeHolder: Ref<RecomposeScope>, dataProducer: () -> Int) {
    scopeHolder.value = currentRecomposeScope
    Text("${dataProducer()}")
}
