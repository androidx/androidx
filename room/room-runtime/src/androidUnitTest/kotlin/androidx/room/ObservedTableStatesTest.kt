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
package androidx.room

import androidx.kruth.assertThat
import androidx.room.ObservedTableStates.ObserveOp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ObservedTableStatesTest {
    private lateinit var tableStates: ObservedTableStates

    @BeforeTest
    fun setup() {
        tableStates = ObservedTableStates(TABLE_COUNT)
    }

    @Test
    fun basicAdd() = runTest {
        tableStates.onObserverAdded(intArrayOf(2, 3))
        assertThat(tableStates.getTablesToSync())
            .isEqualTo(createSyncResult(mapOf(2 to ObserveOp.ADD, 3 to ObserveOp.ADD)))
    }

    @Test
    fun basicRemove() = runTest {
        tableStates.onObserverAdded(intArrayOf(2, 3))
        tableStates.getTablesToSync()

        tableStates.onObserverRemoved(intArrayOf(3))
        assertThat(tableStates.getTablesToSync())
            .isEqualTo(createSyncResult(mapOf(3 to ObserveOp.REMOVE)))
    }

    @Test
    fun noChange() = runTest {
        tableStates.onObserverAdded(intArrayOf(1, 3))
        tableStates.getTablesToSync()

        tableStates.onObserverAdded(intArrayOf(3))
        assertNull(tableStates.getTablesToSync())
    }

    @Test
    fun multipleAdditionsDeletions() = runTest {
        tableStates.onObserverAdded(intArrayOf(2, 4))
        tableStates.getTablesToSync()

        tableStates.onObserverAdded(intArrayOf(2))
        assertNull(tableStates.getTablesToSync())

        tableStates.onObserverAdded(intArrayOf(2, 4))
        assertNull(tableStates.getTablesToSync())

        tableStates.onObserverRemoved(intArrayOf(2))
        assertNull(tableStates.getTablesToSync())

        tableStates.onObserverRemoved(intArrayOf(2, 4))
        assertNull(tableStates.getTablesToSync())

        tableStates.onObserverAdded(intArrayOf(1, 3))
        tableStates.onObserverRemoved(intArrayOf(2, 4))
        assertThat(tableStates.getTablesToSync())
            .isEqualTo(
                createSyncResult(
                    mapOf(
                        1 to ObserveOp.ADD,
                        2 to ObserveOp.REMOVE,
                        3 to ObserveOp.ADD,
                        4 to ObserveOp.REMOVE
                    )
                )
            )
    }

    companion object {
        private const val TABLE_COUNT = 5

        private fun createSyncResult(tuples: Map<Int, ObserveOp>): Array<ObserveOp> {
            return Array(TABLE_COUNT) { i -> tuples[i] ?: ObserveOp.NO_OP }
        }
    }
}
