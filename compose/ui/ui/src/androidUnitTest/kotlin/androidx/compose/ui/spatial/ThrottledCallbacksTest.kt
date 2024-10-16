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
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutNode
import kotlinx.coroutines.DisposableHandle
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

        hb.dispose()
        fire(1)

        assertEquals(2, a)
        assertEquals(1, b)
        assertEquals(2, c)

        ha.dispose()
        fire(1)

        assertEquals(2, a)
        assertEquals(1, b)
        assertEquals(3, c)

        hc.dispose()
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

    var currentTime: Long = 1

    private fun ThrottledCallbacks.advanceBy(ms: Long) {
        currentTime += ms
        triggerDebounced(currentTime)
    }

    private fun ThrottledCallbacks.fire(id: Int) {
        fire(id, 0, 0, currentTime)
    }

    private fun ThrottledCallbacks.register(
        id: Int,
        throttleMs: Long,
        debounceMs: Long,
        callback: (RectInfo) -> Unit
    ): DisposableHandle {
        return register(id, throttleMs, debounceMs, fakeNode(), callback)
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
