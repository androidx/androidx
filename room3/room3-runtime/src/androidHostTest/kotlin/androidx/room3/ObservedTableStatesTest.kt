/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room3

import androidx.kruth.assertThat
import androidx.room3.ObservedTableStates.ObserveOp
import androidx.room3.concurrent.AtomicInt
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class ObservedTableStatesTest {
    private lateinit var tableStates: ObservedTableStates

    @BeforeTest
    fun setup() {
        tableStates = ObservedTableStates(TABLE_COUNT)
    }

    @Test
    fun basicAdd() = runTest {
        assertThat(tableStates.onObserverAdded(intArrayOf(2, 3))).isTrue()
        tableStates.onSync { ops ->
            assertThat(ops)
                .isEqualTo(createSyncResult(mapOf(2 to ObserveOp.ADD, 3 to ObserveOp.ADD)))
        }
    }

    @Test
    fun basicRemove() = runTest {
        tableStates.onObserverAdded(intArrayOf(2, 3))
        tableStates.onSync {}

        assertThat(tableStates.onObserverRemoved(intArrayOf(3))).isTrue()
        tableStates.onSync { ops ->
            assertThat(ops).isEqualTo(createSyncResult(mapOf(3 to ObserveOp.REMOVE)))
        }
    }

    @Test
    fun noChange() = runTest {
        tableStates.onObserverAdded(intArrayOf(1, 3))
        tableStates.onSync {}

        assertThat(tableStates.onObserverAdded(intArrayOf(3))).isFalse()
        tableStates.onSync { ops -> assertNull(ops) }
    }

    @Test
    fun addAndDeleteNetMoChange() = runTest {
        tableStates.onObserverAdded(intArrayOf(1, 3))
        tableStates.onSync {}

        assertThat(tableStates.onObserverRemoved(intArrayOf(1, 3))).isTrue()
        assertThat(tableStates.onObserverAdded(intArrayOf(1, 3))).isTrue()
        tableStates.onSync { ops -> assertNull(ops) }
    }

    @Test
    fun multipleAddPendingChange() = runTest {
        assertThat(tableStates.onObserverAdded(intArrayOf(2))).isTrue()
        assertThat(tableStates.onObserverAdded(intArrayOf(2))).isTrue()
        tableStates.onSync { ops ->
            assertThat(ops).isEqualTo(createSyncResult(mapOf(2 to ObserveOp.ADD)))
        }

        assertThat(tableStates.onObserverAdded(intArrayOf(2))).isFalse()
        tableStates.onSync { ops -> assertThat(ops).isNull() }
    }

    @Test
    fun multipleAdditionsDeletions() = runTest {
        tableStates.onObserverAdded(intArrayOf(2, 4))
        tableStates.onSync {}

        assertThat(tableStates.onObserverAdded(intArrayOf(2))).isFalse()
        tableStates.onSync { ops -> assertNull(ops) }

        assertThat(tableStates.onObserverAdded(intArrayOf(2, 4))).isFalse()
        tableStates.onSync { ops -> assertNull(ops) }

        assertThat(tableStates.onObserverRemoved(intArrayOf(2))).isFalse()
        tableStates.onSync { ops -> assertNull(ops) }

        assertThat(tableStates.onObserverRemoved(intArrayOf(2, 4))).isFalse()
        tableStates.onSync { ops -> assertNull(ops) }

        assertThat(tableStates.onObserverAdded(intArrayOf(1, 3))).isTrue()
        assertThat(tableStates.onObserverRemoved(intArrayOf(2, 4))).isTrue()
        tableStates.onSync { ops ->
            assertThat(ops)
                .isEqualTo(
                    createSyncResult(
                        mapOf(
                            1 to ObserveOp.ADD,
                            2 to ObserveOp.REMOVE,
                            3 to ObserveOp.ADD,
                            4 to ObserveOp.REMOVE,
                        )
                    )
                )
        }
    }

    // Validates internal locks are OK with resuming coroutine in different thread. b/553140228
    @Test
    fun syncSuspendingAction() = runTest {
        tableStates.onObserverAdded(intArrayOf(1))
        withContext(NewThreadDispatcher()) {
            val beforeThread = Thread.currentThread()
            tableStates.onSync { ops ->
                yield()
                val afterThread = Thread.currentThread()
                assertThat(beforeThread).isNotEqualTo(afterThread)
                assertThat(ops).isEqualTo(createSyncResult(mapOf(1 to ObserveOp.ADD)))
            }
        }
    }

    /** A CoroutineDispatcher that dispatches every block into a new thread */
    private class NewThreadDispatcher : CoroutineDispatcher() {
        private val idCounter = AtomicInt(0)

        @OptIn(InternalCoroutinesApi::class)
        override fun dispatchYield(context: CoroutineContext, block: Runnable) {
            super.dispatchYield(context, block)
        }

        override fun isDispatchNeeded(context: CoroutineContext) = true

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            thread(name = "NewThreadDispatcher-${idCounter.incrementAndGet()}") { block.run() }
        }
    }

    companion object {
        private const val TABLE_COUNT = 5

        private fun createSyncResult(tuples: Map<Int, ObserveOp>): Array<ObserveOp> {
            return Array(TABLE_COUNT) { i -> tuples[i] ?: ObserveOp.NO_OP }
        }
    }
}
