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

package androidx.compose.ui.spatial

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatableNode.RegistrationHandle
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ThrottledCallbacksTest {

    @Test
    fun testImmediateFire() = test {
        var called = 0
        register(1, throttleMs = 0, debounceMs = 0) { called++ }
        fire(1)
        assertEquals(1, called)
    }

    @Test
    fun testImmediateFireMultipleCallbacks() = test {
        var a = 0
        register(1, throttleMs = 0, debounceMs = 0) { a++ }
        var b = 0
        register(1, throttleMs = 0, debounceMs = 0) { b++ }
        fire(1)
        assertEquals(1, a)
        assertEquals(1, b)
    }

    @Test
    fun testImmediateFireMultipleCallbacksRemoval() = test {
        var a = 0
        val ha = register(1, throttleMs = 0, debounceMs = 0) { a++ }
        var b = 0
        val hb = register(1, throttleMs = 0, debounceMs = 0) { b++ }
        var c = 0
        val hc = register(1, throttleMs = 0, debounceMs = 0) { c++ }
        fire(1)
        assertEquals(1, a)
        assertEquals(1, b)
        assertEquals(1, c)

        hb.unregister()
        fire(1)

        assertEquals(2, a)
        assertEquals(1, b)
        assertEquals(2, c)

        ha.unregister()
        fire(1)

        assertEquals(2, a)
        assertEquals(1, b)
        assertEquals(3, c)

        hc.unregister()
        fire(1)

        assertEquals(2, a)
        assertEquals(1, b)
        assertEquals(3, c)
    }

    @Test
    fun testImmediateFireOnlyTargetsId() = test {
        var a = 0
        register(1, throttleMs = 0, debounceMs = 0) { a++ }
        var b = 0
        register(2, throttleMs = 0, debounceMs = 0) { b++ }
        fire(1)
        assertEquals(1, a)
        assertEquals(0, b)
        advanceBy(1)
        fire(2)
        assertEquals(1, a)
        assertEquals(1, b)
        advanceBy(1)
        fire(2)
        assertEquals(1, a)
        assertEquals(2, b)
    }

    @Test
    fun testDebounceSingleFire() = test {
        var called = 0
        register(1, throttleMs = 0, debounceMs = 10) { called++ }
        fire(1)
        assertEquals(0, called)
        advanceBy(8)
        assertEquals(0, called)
        advanceBy(8)
        assertEquals(1, called)
    }

    @Test
    fun testDebounceManyFire() = test {
        var called = 0
        register(1, throttleMs = 0, debounceMs = 10) { called++ }
        fire(1)
        assertEquals(0, called)
        advanceBy(8)
        assertEquals(0, called)
        fire(1)
        assertEquals(0, called)
        advanceBy(8)
        assertEquals(0, called)
        fire(1)
        assertEquals(0, called)
        advanceBy(8)
        assertEquals(0, called)
        advanceBy(8)
        assertEquals(1, called)
    }

    @Test
    fun testThrottleSingleFire() = test {
        var called = 0
        register(1, throttleMs = 10, debounceMs = 0) { called++ }
        fire(1)
        assertEquals(1, called)
        advanceBy(8)
        assertEquals(1, called)
        fire(1)
        assertEquals(1, called)
        advanceBy(8)
        assertEquals(1, called)
        fire(1)
        assertEquals(2, called)
        advanceBy(8)
        assertEquals(2, called)
        fire(1)
        assertEquals(2, called)
    }

    @Test
    fun testThrottleRapidFireCallsOncePerDuration() = test {
        var called = 0
        register(1, throttleMs = 10, debounceMs = 0) { called++ }
        repeat(10) {
            fire(1)
            advanceBy(1)
            assertEquals(1, called)
        }
        // it is at this point that we cross over the throttle deadline, so we get the second fire
        fire(1)
        advanceBy(1)
        assertEquals(2, called)

        // go ahead and fire once more. it won't invoke right now...
        fire(1)
        advanceBy(1)
        assertEquals(2, called)

        // but if we wait more than 10ms it will fire the state that it settled on
        advanceBy(12)
        assertEquals(3, called)
    }

    @Test
    fun testThrottleAndDebounceSlowFiring() = test {
        var called = 0
        register(1, throttleMs = 20, debounceMs = 10) { called++ }
        fire(1)
        assertEquals(1, called) // called right away
        advanceBy(30) // more than throttle and debounce time passes
        assertEquals(1, called)
        fire(1)
        assertEquals(2, called) // called right away
    }

    @Test
    fun testThrottleAndDebounceRapidFiring() = test {
        var called = 0
        register(1, throttleMs = 20, debounceMs = 10) { called++ }
        fire(1)
        assertEquals(1, called) // called right away
        advanceBy(16) // above debounce but below throttle
        assertEquals(1, called) // not called again
        fire(1) // this should get ignored since it is below throttle limit
        assertEquals(1, called)
        advanceBy(12) // advance above debounce limit
        // after debounce deadline passed, lambda gets called because the last position was lost
        assertEquals(2, called)
        advanceBy(30) // more than throttle and debounce time passes
        assertEquals(2, called)
        fire(1)
        assertEquals(3, called) // called right away
    }

    @Test
    fun testThrottleAndDebounceWithLargerDebounce() = test {
        var called = 0
        register(1, throttleMs = 20, debounceMs = 400) { called++ }
        fire(1)
        assertEquals(1, called) // called right away
        advanceBy(16) // above debounce but below throttle
        assertEquals(1, called) // not called again
        fire(1) // this should get ignored since it is below throttle limit
        assertEquals(1, called)
        advanceBy(16) // advance above throttle limit
        assertEquals(1, called) // still one since we haven't fired again
        fire(1)
        assertEquals(2, called) // triggered, since we are above throttle
        advanceBy(10)
        fire(1) // this shouldn't fire right away, since we just fired one
        assertEquals(2, called) // no new calls
        advanceBy(100) // advance past throttle limit
        assertEquals(2, called) // still not called
        advanceBy(500) // advance past debounce limit
        assertEquals(3, called) // now it gets called because of debouncs
    }

    @Test
    fun testFireOnRectChangedEntriesSameMillisecondZeroThrottle() = test {
        var called = 0
        var lastBounds: RelativeLayoutBounds? = null
        register(1, throttleMs = 0, debounceMs = 0) {
            called++
            lastBounds = it
        }
        fire(1)
        assertEquals(1, called)
        assertEquals(IntOffset.Zero, lastBounds?.positionInWindow)
        // Firing all rectChanged entries in the same millisecond (e.g. when viewToWindowMatrix or
        // windowOffset updates right after measureAndLayout) should still invoke unthrottled
        // callbacks with the updated offset.
        updateOffsets(IntOffset(10, 20), IntOffset.Zero, null, 100, 100)
        fireOnRectChangedEntries(currentTime)
        assertEquals(2, called)
        assertEquals(IntOffset(10, 20), lastBounds?.positionInWindow)
    }

    @Test
    fun testFireOnRectChangedEntriesWithThrottle() = test {
        var called = 0
        register(1, throttleMs = 10, debounceMs = 0) { called++ }
        fireOnRectChangedEntries(currentTime)
        assertEquals(1, called)
        advanceBy(5)
        fireOnRectChangedEntries(currentTime)
        assertEquals(1, called)
        advanceBy(10)
        assertEquals(2, called)
    }

    @Test
    fun testRectManagerDispatchCallbacksBothDirtyFiresOnce() {
        val noOpDelayed =
            object : ExecuteDelayed {
                override fun executeDelayed(delayMillis: Long, block: () -> Unit): Any = Any()

                override fun removeDelayedExecution(token: Any) {}
            }
        val rectManager = RectManager(executeDelayed = noOpDelayed)
        var called = 0
        var lastBounds: RelativeLayoutBounds? = null
        rectManager.rects.insert(
            value = 1,
            l = 0,
            t = 0,
            r = 20,
            b = 30,
            parentId = -1,
            parentIndex = NotFound,
            focusable = false,
            gesturable = false,
            hasCallbacks = true,
        )
        rectManager.throttledCallbacks.registerOnRectChanged(
            id = 1,
            throttleMillis = 0,
            debounceMillis = 0,
            node = fakeNode(),
        ) {
            called++
            lastBounds = it
        }
        rectManager.invalidate()
        rectManager.updateOffsets(
            screenOffset = IntOffset(5, 10),
            windowOffset = IntOffset.Zero,
            viewToWindowMatrix = Matrix(),
            windowWidth = 200,
            windowHeight = 200,
        )

        // Both isDirty and isScreenOrWindowDirty are true: should fire once with updated rect and
        // offsets
        rectManager.dispatchCallbacks()
        assertEquals(1, called)
        assertEquals(20, lastBounds?.width)
        assertEquals(30, lastBounds?.height)
        assertEquals(IntOffset(5, 10), lastBounds?.positionInWindow)
        assertEquals(IntRect(5, 10, 25, 40), lastBounds?.boundsInWindow)

        // Dispatching when neither flag is dirty should not fire
        rectManager.dispatchCallbacks()
        assertEquals(1, called)

        // Updating with a 90-degree rotation matrix and dispatching should fire with rotated bounds
        rectManager.updateOffsets(
            screenOffset = IntOffset(5, 10),
            windowOffset = IntOffset.Zero,
            viewToWindowMatrix = Matrix().apply { rotateZ(90f) },
            windowWidth = 200,
            windowHeight = 200,
        )
        rectManager.dispatchCallbacks()
        assertEquals(2, called)
        assertEquals(IntRect(-30, 0, 0, 20), lastBounds?.boundsInWindow)
    }

    var currentTime: Long = 1

    private fun ThrottledCallbacks.advanceBy(ms: Long) {
        currentTime += ms
        triggerDebounced(currentTime)
    }

    private fun ThrottledCallbacks.fire(id: Int) {
        fireOnUpdatedRect(id, 0, 0, currentTime)
    }

    private fun ThrottledCallbacks.register(
        id: Int,
        throttleMs: Long,
        debounceMs: Long,
        callback: (RelativeLayoutBounds) -> Unit,
    ): RegistrationHandle {
        return registerOnRectChanged(id, throttleMs, debounceMs, fakeNode(), callback)
    }

    private inline fun test(block: ThrottledCallbacks.() -> Unit) {
        val cbs = ThrottledCallbacks()
        currentTime = 1
        block(cbs)
    }

    private fun fakeNode(): DelegatableNode {
        val node = object : Modifier.Node() {}

        val layoutNode = LayoutNode(0, 0, 0, 0)
        node.updateCoordinator(layoutNode.outerCoordinator)
        layoutNode.measurePassDelegate.isPlaced = true
        return node
    }
}
