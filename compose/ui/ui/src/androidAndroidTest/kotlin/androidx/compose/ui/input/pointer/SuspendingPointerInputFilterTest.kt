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

package androidx.compose.ui.input.pointer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Rule

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SuspendingPointerInputFilterTest {
    @get:Rule
    val rule = createComposeRule()

    @After
    fun after() {
        // some tests may set this
        isDebugInspectorInfoEnabled = false
    }

    @Test
    @MediumTest
    fun testAwaitSingleEvent() {
        val latch = CountDownLatch(1)
        val emitter = PointerInputChangeEmitter()
        val expectedChange = emitter.nextChange(Offset(5f, 5f))

        var returnedChange: PointerEvent? = null

        // Used to manually trigger a PointerEvent created from our PointerInputChange.
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                returnedChange = awaitPointerEvent()
                latch.countDown()
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        rule.runOnIdle {
            suspendingPointerInputModifierNode.onPointerEvent(
                expectedChange.toPointerEvent(),
                PointerEventPass.Main,
                IntSize(10, 10)
            )
        }

        rule.runOnIdle {
            assertTrue("Waiting for relaunch timed out", latch.await(200, TimeUnit.MILLISECONDS))
            assertEquals(expectedChange, returnedChange?.firstChange)
        }
    }

    @Test
    @MediumTest
    fun testAwaitSeveralEvents() {
        val latch = CountDownLatch(3)
        val results = Channel<PointerEvent>(Channel.UNLIMITED)

        // Used to manually trigger a PointerEvent(s) created from our PointerInputChange(s).
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                repeat(3) {
                    results.trySend(awaitPointerEvent())
                    latch.countDown()
                }
                results.close()
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        val emitter = PointerInputChangeEmitter()
        val expected = listOf(
            emitter.nextChange(Offset(5f, 5f)),
            emitter.nextChange(Offset(10f, 5f)),
            emitter.nextChange(Offset(10f, 10f))
        )

        val bounds = IntSize(20, 20)

        rule.runOnIdle {
            expected.forEach { pointerInputChange ->
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerInputChange.toPointerEvent(),
                    PointerEventPass.Main,
                    bounds
                )
            }
        }

        rule.runOnIdle {
            assertTrue("Waiting for relaunch timed out", latch.await(200, TimeUnit.MILLISECONDS))

            runTest {
                val received = withTimeout(200) {
                    results.receiveAsFlow()
                        .map { it.firstChange }
                        .toList()
                }
                assertEquals(expected, received)
            }
        }
    }

    @Test
    @MediumTest
    fun testSyntheticCancelEvent() {
        var currentEventAtEnd: PointerEvent? = null
        val latch = CountDownLatch(3)
        val results = Channel<PointerEvent>(Channel.UNLIMITED)

        // Used to manually trigger a PointerEvent(s) created from our PointerInputChange(s).
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                try {
                    repeat(3) {
                        results.trySend(awaitPointerEvent())
                        latch.countDown()
                    }
                    results.close()
                } finally {
                    currentEventAtEnd = currentEvent
                }
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        val bounds = IntSize(50, 50)
        val emitter1 = PointerInputChangeEmitter(0)
        val emitter2 = PointerInputChangeEmitter(1)
        val expectedEvents = listOf(
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(5f, 5f)),
                    emitter2.nextChange(Offset(10f, 10f))
                )
            ),
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(6f, 6f)),
                    emitter2.nextChange(Offset(10f, 10f), down = false)
                )
            ),
            // Synthetic cancel should look like this (Note: this specific event isn't ever
            // triggered directly, it's just for reference so you know what onCancelPointerInput()
            // triggers).
            // Both pointers are there, but only the with the pressed = true is changed to false,
            // and the down change is consumed.
            PointerEvent(
                listOf(
                    PointerInputChange(
                        PointerId(0),
                        0,
                        Offset(6f, 6f),
                        false,
                        0,
                        Offset(6f, 6f),
                        true,
                        isInitiallyConsumed = true
                    ),
                    PointerInputChange(
                        PointerId(1),
                        0,
                        Offset(10f, 10f),
                        false,
                        0,
                        Offset(10f, 10f),
                        false,
                        isInitiallyConsumed = false
                    )
                )
            )
        )

        rule.runOnIdle {
            expectedEvents.take(expectedEvents.size - 1).forEach { pointerEvent ->
                // Initial
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerEvent,
                    PointerEventPass.Initial,
                    bounds
                )

                // Main
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerEvent,
                    PointerEventPass.Main,
                    bounds
                )

                // Final
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerEvent,
                    PointerEventPass.Final,
                    bounds
                )
            }

            // Triggers cancel event
            suspendingPointerInputModifierNode.onCancelPointerInput()
        }

        // Checks events triggered are the correct ones
        rule.runOnIdle {
            assertTrue("Waiting for relaunch timed out", latch.await(200, TimeUnit.MILLISECONDS))

            runTest {
                val received = withTimeout(200) {
                    results.receiveAsFlow().toList()
                }

                assertThat(expectedEvents).hasSize(received.size)

                expectedEvents.forEachIndexed { index, expectedEvent ->
                    val actualEvent = received[index]
                    PointerEventSubject.assertThat(actualEvent).isStructurallyEqualTo(expectedEvent)
                }
                assertThat(currentEventAtEnd).isNotNull()
                PointerEventSubject.assertThat(currentEventAtEnd!!)
                    .isStructurallyEqualTo(expectedEvents.last())
            }
        }
    }

    @Test
    @LargeTest
    fun testNoSyntheticCancelEventWhenPressIsFalse() {
        var currentEventAtEnd: PointerEvent? = null
        val results = Channel<PointerEvent>(Channel.UNLIMITED)

        // Used to manually trigger a PointerEvent(s) created from our PointerInputChange(s).
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                try {
                    // NOTE: This will never trigger 3 times. There are only two events
                    // triggered followed by a onCancelPointerInput() call which doesn't trigger
                    // an event because the previous event has down (press) set to false, so we
                    // will always get an exception thrown with the last repeat's timeout
                    // (we expect this).
                    repeat(3) {
                        withTimeout(200) {
                            results.trySend(awaitPointerEvent())
                        }
                    }
                } finally {
                    currentEventAtEnd = currentEvent
                    results.close()
                }
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        val bounds = IntSize(50, 50)
        val emitter1 = PointerInputChangeEmitter(0)
        val emitter2 = PointerInputChangeEmitter(1)
        val twoExpectedEvents = listOf(
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(5f, 5f)),
                    emitter2.nextChange(Offset(10f, 10f))
                )
            ),
            // Pointer event changes don't have any pressed pointers!
            PointerEvent(
                listOf(
                    emitter1.nextChange(Offset(6f, 6f), down = false),
                    emitter2.nextChange(Offset(10f, 10f), down = false)
                )
            )
        )

        rule.runOnIdle {
            twoExpectedEvents.forEach { pointerEvent ->
                // Initial
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerEvent,
                    PointerEventPass.Initial,
                    bounds
                )

                // Main
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerEvent,
                    PointerEventPass.Main,
                    bounds
                )

                // Final
                suspendingPointerInputModifierNode.onPointerEvent(
                    pointerEvent,
                    PointerEventPass.Final,
                    bounds
                )
            }

            // Manually triggers cancel event.
            // Note: This will not trigger an event in the customPointerInput block because the
            // previous events don't have any pressed pointers.
            suspendingPointerInputModifierNode.onCancelPointerInput()
        }

        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            runTest {
                withTimeout(400) {
                    while (!results.isClosedForSend) {
                        yield()
                    }
                }

                val received = results.receiveAsFlow().toList()

                assertThat(received).hasSize(twoExpectedEvents.size)

                twoExpectedEvents.forEachIndexed { index, expectedEvent ->
                    val actualEvent = received[index]
                    PointerEventSubject.assertThat(actualEvent).isStructurallyEqualTo(expectedEvent)
                }
                assertThat(currentEventAtEnd).isNotNull()
                PointerEventSubject.assertThat(currentEventAtEnd!!)
                    .isStructurallyEqualTo(twoExpectedEvents.last())
            }
        }
    }

    @Test
    @MediumTest
    fun testCancelledHandlerBlock() {
        val counter = TestCounter()

        // Used to manually trigger a PointerEvent(s) created from our PointerInputChange(s).
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            try {
                awaitPointerEventScope {
                    try {
                        counter.expect(3, "about to call awaitPointerEvent")

                        // With only one event triggered, this will stay stuck in the repeat
                        // block until the Job is cancelled via
                        // SuspendPointerInputModifierNode.resetHandling()
                        repeat(2) {
                            awaitPointerEvent()
                            counter.expect(
                                4,
                                "One and only pointer event triggered to create Job."
                            )
                        }

                        fail("awaitPointerEvent returned; should have thrown for cancel")
                    } finally {
                        counter.expect(6, "inner finally block running")
                    }
                }
            } finally {
                counter.expect(7, "outer finally block running; inner " +
                    "finally should have run")
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        val emitter = PointerInputChangeEmitter()
        val singleEvent = emitter.nextChange(Offset(5f, 5f))
        val singleEventBounds = IntSize(20, 20)

        rule.runOnIdle {
            counter.expect(
                1,
                "Job to handle pointer input not created yet; awaitPointerEvent should " +
                    "be suspended"
            )

            counter.expect(
                2,
                "Trigger pointer input event to create Job for handing handle pointer" +
                    " input (done lazily in SuspendPointerInputModifierNode)."
            )

            suspendingPointerInputModifierNode.onPointerEvent(
                singleEvent.toPointerEvent(),
                PointerEventPass.Main,
                singleEventBounds
            )

            counter.expect(5, "before cancelling handler; awaitPointerEvent " +
                "should be suspended")

            // Cancels Job that manages pointer input events in SuspendPointerInputModifierNode.
            suspendingPointerInputModifierNode.resetPointerInputHandler()
            counter.expect(8, "after cancelling; finally blocks should have run")
        }
    }

    @Test
    @MediumTest
    fun testInspectorValue() {
        isDebugInspectorInfoEnabled = true

        rule.setContent {
            val pointerInputHandler: suspend PointerInputScope.() -> Unit = {}
            val modifier =
                Modifier.pointerInput(Unit, pointerInputHandler) as SuspendPointerInputElement

            assertThat(modifier.nameFallback).isEqualTo("pointerInput")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.asIterable()).containsExactly(
                ValueElement("key1", Unit),
                ValueElement("key2", null),
                ValueElement("keys", null),
                ValueElement("pointerInputHandler", pointerInputHandler)
            )
        }
    }

    @Test
    @MediumTest
    fun testRestartPointerInputWithTouchEvent() {
        val emitter = PointerInputChangeEmitter()
        val expectedChange = emitter.nextChange(Offset(5f, 5f))

        var forceRecompositionCount by mutableStateOf(0)
        var compositionCount = 0
        var pointerInputBlockExecutionCount = 0

        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            // pointerInput now lazily executes this block of code meaning it won't be
            // executed until an actual event happens.
            pointerInputBlockExecutionCount++
            suspendCancellableCoroutine<Unit> {}
        }

        rule.setContent {
            // Read the value in composition to change the lambda capture below
            val toCapture = forceRecompositionCount
            compositionCount++

            Box(
                modifier = elementFor(
                    key1 = toCapture,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        forceRecompositionCount = 1

        rule.runOnIdle {
            // Triggers first and only event (and launches coroutine).
            // Note: SuspendPointerInputModifierNode actually launches its coroutine lazily, so it
            // will not be launched until the first event is triggered which is what we do here.
            suspendingPointerInputModifierNode.onPointerEvent(
                expectedChange.toPointerEvent(),
                PointerEventPass.Main,
                IntSize(5, 5)
            )
        }

        rule.runOnIdle {
            assertEquals(compositionCount, 2)
            // One pointer input event, should have triggered one execution.
            assertEquals(pointerInputBlockExecutionCount, 1)
        }
    }

    @Test
    @MediumTest
    fun testRestartPointerInputWithNoTouchEvents() {
        var forceRecompositionCount by mutableStateOf(0)
        var compositionCount = 0
        var pointerInputBlockExecutionCount = 0

        rule.setContent {
            // Read the value in composition to change the lambda capture below
            val toCapture = forceRecompositionCount
            compositionCount++
            Box(
                Modifier.pointerInput(toCapture) {
                    // pointerInput now lazily executes this block of code meaning it won't be
                    // executed until an actual event happens.
                    pointerInputBlockExecutionCount++
                    suspendCancellableCoroutine<Unit> {}
                }
            )
        }

        forceRecompositionCount = 1

        rule.runOnIdle {
            assertEquals(compositionCount, 2)
            // No pointer input events, no block executions.
            assertEquals(pointerInputBlockExecutionCount, 0)
        }
    }

    @Test
    @LargeTest
    fun testWithTimeout() {
        val latch = CountDownLatch(1)
        val emitter = PointerInputChangeEmitter()
        val expectedChange = emitter.nextChange(Offset(5f, 5f))

        // Used to manually trigger a PointerEvent(s) created from our PointerInputChange(s).
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                try {
                    // Handles first event (needed to trigger the creation of the coroutine
                    // since it is lazily created).
                    awaitPointerEvent()

                    // Times out waiting for second event (no second event is triggered in this
                    // test).
                    withTimeout(10) {
                        awaitPointerEvent()
                    }
                } catch (exception: Exception) {
                    assertThat(exception)
                        .isInstanceOf(PointerEventTimeoutCancellationException::class.java)
                    latch.countDown()
                }
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        rule.runOnIdle {
            // Triggers first event (and launches coroutine).
            // Note: SuspendPointerInputModifierNode actually launches its coroutine lazily, so it
            // will not be launched until the first event is triggered which is what we do here.
            suspendingPointerInputModifierNode.onPointerEvent(
                expectedChange.toPointerEvent(),
                PointerEventPass.Main,
                IntSize(5, 5)
            )
        }

        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
        }
    }

    @Test
    @LargeTest
    fun testWithTimeoutOrNull() {
        val emitter = PointerInputChangeEmitter()
        val expectedChange = emitter.nextChange(Offset(5f, 5f))

        // Sets an empty default (if not updated to null after call (expected), it will fail).
        var resultOfTimeoutOrNull: PointerEvent? = PointerEvent(listOf())

        // Used to manually trigger a PointerEvent(s) created from our PointerInputChange(s).
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                try {
                    // Handles first event (needed to trigger the creation of the coroutine
                    // since it is lazily created).
                    awaitPointerEvent()

                    // Times out waiting for second event (no second event is triggered in this
                    // test).
                    resultOfTimeoutOrNull = withTimeoutOrNull(10) {
                        awaitPointerEvent()
                    }
                } catch (exception: Exception) {
                    // An exception should not be raised in this test, but, just in case one is,
                    // we want to verify it isn't the one withTimeout will usually raise.
                    assertThat(exception)
                        .isNotInstanceOf(PointerEventTimeoutCancellationException::class.java)
                }
            }
        }

        rule.setContent {
            Box(
                modifier = elementFor(
                    key1 = Unit,
                    instance = suspendingPointerInputModifierNode as Modifier.Node
                )
            )
        }

        rule.runOnIdle {
            // Triggers first event (and launches coroutine).
            // Note: SuspendPointerInputModifierNode actually launches its coroutine lazily, so it
            // will not be launched until the first event is triggered which is what we do here.
            suspendingPointerInputModifierNode.onPointerEvent(
                expectedChange.toPointerEvent(),
                PointerEventPass.Main,
                IntSize(5, 5)
            )
        }

        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(resultOfTimeoutOrNull).isNull()
        }
    }

    @Test
    @MediumTest
    fun testDelegatedPointerEvent() {
        val latch = CountDownLatch(1)
        val emitter = PointerInputChangeEmitter()
        val expectedChange = emitter.nextChange(Offset(5f, 5f))

        var returnedChange: PointerEvent? = null

        // Used to manually trigger a PointerEvent created from our PointerInputChange.
        val suspendingPointerInputModifierNode = SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                returnedChange = awaitPointerEvent()
                latch.countDown()
            }
        }
        val node = object : DelegatingNode() {
            @Suppress("unused")
            val pointer = delegate(suspendingPointerInputModifierNode)
        }

        rule.setContent {
            Box(Modifier.elementFor(node))
        }

        rule.runOnIdle {
            suspendingPointerInputModifierNode.onPointerEvent(
                expectedChange.toPointerEvent(),
                PointerEventPass.Main,
                IntSize(10, 10)
            )
        }

        rule.runOnIdle {
            assertTrue("Waiting for relaunch timed out", latch.await(200, TimeUnit.MILLISECONDS))
            assertEquals(expectedChange, returnedChange?.firstChange)
        }
    }

    @Test
    @MediumTest
    fun testMultipleDelegatedPointerEvents2() {
        val events = mutableListOf<PointerEvent>()
        val tag = "input rect"

        val node = object : DelegatingNode() {
            @Suppress("unused")
            val piNode1 = delegate(SuspendingPointerInputModifierNode {
                awaitPointerEventScope {
                    events += awaitPointerEvent()
                }
            })

            @Suppress("unused")
            val piNode2 = delegate(SuspendingPointerInputModifierNode {
                awaitPointerEventScope {
                    events += awaitPointerEvent()
                }
            })
        }

        rule.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag(tag)
                    .elementFor(node)
            )
        }

        rule.onNodeWithTag(tag).performTouchInput {
            down(Offset.Zero)
        }
        assertThat(events).hasSize(2)
    }
}

private fun PointerInputChange.toPointerEvent() = PointerEvent(listOf(this))

private val PointerEvent.firstChange get() = changes.first()

private class PointerInputChangeEmitter(id: Int = 0) {
    val pointerId = PointerId(id.toLong())
    var previousTime = 0L
    var previousPosition = Offset.Zero
    var previousPressed = false

    fun nextChange(
        position: Offset = Offset.Zero,
        down: Boolean = true,
        time: Long = 0
    ): PointerInputChange {
        return PointerInputChange(
            id = pointerId,
            time,
            position,
            down,
            previousTime,
            previousPosition,
            previousPressed,
            isInitiallyConsumed = false
        ).also {
            previousTime = time
            previousPosition = position
            previousPressed = down
        }
    }
}

private class TestCounter {
    private var count = 0

    fun expect(checkpoint: Int, message: String = "(no message)") {
        val expected = count + 1
        if (checkpoint != expected) {
            fail("out of order event $checkpoint, expected $expected, $message")
        }
        count = expected
    }
}

private fun elementFor(
    key1: Any? = null,
    instance: Modifier.Node
) = object : ModifierNodeElement<Modifier.Node>() {
    override fun InspectorInfo.inspectableProperties() {
        debugInspectorInfo {
            name = "pointerInput"
            properties["key1"] = key1
            properties["instance"] = instance
        }
    }

    override fun create() = instance
    override fun update(node: Modifier.Node) {}
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuspendPointerInputElement) return false
        if (key1 != other.key1) return false
        return true
    }
    override fun hashCode(): Int {
        return key1?.hashCode() ?: 0
    }
}
