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

package androidx.compose.ui.window

import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteropTransactionQueueTest {

    // Ordered record of which transactions were performed (by name).
    private val performed = mutableListOf<String>()

    // Non-empty transaction: recorded via its action, takes the normal scheduling path.
    private fun transaction(name: String) = object : UIKitInteropTransaction {
        override val actions: List<UIKitInteropAction> = listOf { performed.add(name) }
        override val isInteropActive: Boolean = false
    }

    // Empty transaction: no actions, triggers the actions.isEmpty() fast-path in scheduleTransaction.
    private fun emptyTransaction() = object : UIKitInteropTransaction {
        override val actions: List<UIKitInteropAction> = emptyList()
        override val isInteropActive: Boolean = false
    }

    @Test
    fun `single transaction is performed when its frame completion fires`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val index = queue.scheduleTransaction(transaction("t0"))

        assertTrue(performed.isEmpty())

        queue.performScheduledTransactions(index)

        assertEquals(listOf("t0"), performed)
    }

    @Test
    fun `transaction is not performed when an earlier index is requested`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        queue.scheduleTransaction(transaction("t0"))
        val i1 = queue.scheduleTransaction(transaction("t1"))
        val i2 = queue.scheduleTransaction(transaction("t2"))

        // Perform only up to and including t1 — t2 must remain pending
        queue.performScheduledTransactions(i1)
        assertEquals(listOf("t0", "t1"), performed)

        // Now complete t2
        queue.performScheduledTransactions(i2)
        assertEquals(listOf("t0", "t1", "t2"), performed)
    }

    @Test
    fun `no transactions scheduled perform is a no-op`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        // -1 is always before any scheduled index (which start at 0)
        queue.performScheduledTransactions(0)
        assertTrue(performed.isEmpty())
    }

    @Test
    fun `each frame completion performs exactly its own transaction`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val i0 = queue.scheduleTransaction(transaction("t0"))
        val i1 = queue.scheduleTransaction(transaction("t1"))
        val i2 = queue.scheduleTransaction(transaction("t2"))

        queue.performScheduledTransactions(i0)
        assertEquals(listOf("t0"), performed)

        queue.performScheduledTransactions(i1)
        assertEquals(listOf("t0", "t1"), performed)

        queue.performScheduledTransactions(i2)
        assertEquals(listOf("t0", "t1", "t2"), performed)
    }

    @Test
    fun `dropped frame later completion performs all pending transactions in order`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        queue.scheduleTransaction(transaction("t0"))
        queue.scheduleTransaction(transaction("t1"))
        val i2 = queue.scheduleTransaction(transaction("t2"))

        // Frame 0 and 1 were dropped; only frame 2 is presented
        queue.performScheduledTransactions(i2)

        assertEquals(listOf("t0", "t1", "t2"), performed)
    }

    @Test
    fun `interleaved schedule and completion with dropped frames`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()

        queue.scheduleTransaction(transaction("t0"))
        val i1 = queue.scheduleTransaction(transaction("t1"))
        queue.scheduleTransaction(transaction("t2"))
        val i3 = queue.scheduleTransaction(transaction("t3"))

        // Frame 1 completes (frame 0 dropped)
        queue.performScheduledTransactions(i1)
        assertEquals(listOf("t0", "t1"), performed)

        queue.scheduleTransaction(transaction("t4"))
        val i5 = queue.scheduleTransaction(transaction("t5"))

        // Frame 3 completes (frame 2 dropped)
        queue.performScheduledTransactions(i3)
        assertEquals(listOf("t0", "t1", "t2", "t3"), performed)

        // Frame 5 completes (frame 4 dropped)
        queue.performScheduledTransactions(i5)
        assertEquals(listOf("t0", "t1", "t2", "t3", "t4", "t5"), performed)
    }

    @Test
    fun `newer frame presented first performs all pending transactions exactly once`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val i0 = queue.scheduleTransaction(transaction("t0"))
        val i1 = queue.scheduleTransaction(transaction("t1"))

        // Frame 1 presented before frame 0 (GPU reordering / fast path)
        queue.performScheduledTransactions(i1)
        assertEquals(listOf("t0", "t1"), performed)

        // Frame 0 completion arrives late — must be a no-op, no double execution
        queue.performScheduledTransactions(i0)
        assertEquals(listOf("t0", "t1"), performed)
    }

    @Test
    fun `stale completion that covers already-performed range is a no-op`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val i0 = queue.scheduleTransaction(transaction("t0"))
        val i1 = queue.scheduleTransaction(transaction("t1"))
        val i2 = queue.scheduleTransaction(transaction("t2"))

        queue.performScheduledTransactions(i2)
        assertEquals(listOf("t0", "t1", "t2"), performed)

        // All stale completions for earlier frames must not repeat any transactions
        queue.performScheduledTransactions(i0)
        queue.performScheduledTransactions(i1)
        queue.performScheduledTransactions(i2)
        assertEquals(listOf("t0", "t1", "t2"), performed)
    }

    @Test
    fun `filling buffer exactly does not trigger overflow`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val bufferSize = 16

        repeat(bufferSize) { i -> queue.scheduleTransaction(transaction("t$i")) }

        // No completions fired yet, no overflow → nothing performed
        assertTrue(performed.isEmpty())
    }

    @Test
    fun `overflow by one forces the oldest transaction to execute immediately`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val bufferSize = 16

        repeat(bufferSize) { i -> queue.scheduleTransaction(transaction("t$i")) }
        assertTrue(performed.isEmpty())

        // One more causes overflow — oldest (t0) is force-performed to free a slot
        queue.scheduleTransaction(transaction("t$bufferSize"))

        assertEquals(listOf("t0"), performed)
    }

    @Test
    fun `each successive overflow evicts exactly one oldest transaction in order`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val bufferSize = 16
        val overflowCount = 5

        repeat(bufferSize + overflowCount) { i ->
            queue.scheduleTransaction(transaction("t$i"))
        }

        // Each overflow evicts exactly 1 transaction, in schedule order
        assertEquals(listOf("t0", "t1", "t2", "t3", "t4"), performed)
    }

    @Test
    fun `ring buffer wraps around correctly over many frames`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val totalCount = 64 // 4x buffer size

        for (i in 0 until totalCount) {
            val index = queue.scheduleTransaction(transaction("t$i"))
            queue.performScheduledTransactions(index)
        }

        assertEquals((0 until totalCount).map { "t$it" }, performed)
    }

    @Test
    fun `empty transaction completion drains all pending real transactions`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        queue.scheduleTransaction(transaction("t0"))
        val i1 = queue.scheduleTransaction(transaction("t1"))

        val emptyIndex = queue.scheduleTransaction(emptyTransaction())

        queue.performScheduledTransactions(emptyIndex)
        assertEquals(listOf("t0", "t1"), performed)
    }

    @Test
    fun `empty transaction does not consume a buffer slot so full buffer does not overflow`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()
        val bufferSize = 16

        repeat(bufferSize) { i -> queue.scheduleTransaction(transaction("t$i")) }
        assertTrue(performed.isEmpty()) // buffer full, no overflow yet

        // Scheduling an empty transaction must NOT trigger overflow
        queue.scheduleTransaction(emptyTransaction())
        assertTrue(performed.isEmpty()) // still nothing performed
    }

    @Test
    fun `empty transaction interleaved with real transactions preserves order`() {
        val queue = SurfaceMetalRedrawer.InteropTransactionQueue()

        queue.scheduleTransaction(transaction("t0"))
        val e1 = queue.scheduleTransaction(emptyTransaction())
        val i1 = queue.scheduleTransaction(transaction("t1"))
        val e2 = queue.scheduleTransaction(emptyTransaction())
        val i2 = queue.scheduleTransaction(transaction("t2"))

        queue.performScheduledTransactions(e1)
        assertEquals(listOf("t0"), performed)

        queue.performScheduledTransactions(i1)
        assertEquals(listOf("t0", "t1"), performed)

        queue.performScheduledTransactions(e2)
        assertEquals(listOf("t0", "t1"), performed)

        queue.performScheduledTransactions(i2)
        assertEquals(listOf("t0", "t1", "t2"), performed)
    }
}
