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
import java.util.Arrays
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ObservedTableTrackerTest {
    private lateinit var mTracker: InvalidationTracker.ObservedTableTracker
    @Before
    fun setup() {
        mTracker = InvalidationTracker.ObservedTableTracker(TABLE_COUNT)
    }

    @Test
    fun basicAdd() {
        mTracker.onAdded(2, 3)
        assertThat(
            mTracker.getTablesToSync()
        ).isEqualTo(
            createResponse(
                2,
                InvalidationTracker.ObservedTableTracker.ADD,
                3,
                InvalidationTracker.ObservedTableTracker.ADD
            )
        )
    }

    @Test
    fun basicRemove() {
        initState(2, 3)
        mTracker.onRemoved(3)
        assertThat(
            mTracker.getTablesToSync()
        ).isEqualTo(
            createResponse(3, InvalidationTracker.ObservedTableTracker.REMOVE)
        )
    }

    @Test
    fun noChange() {
        initState(1, 3)
        mTracker.onAdded(3)
        assertNull(
            mTracker.getTablesToSync()
        )
    }

    @Test
    fun multipleAdditionsDeletions() {
        initState(2, 4)
        mTracker.onAdded(2)
        assertNull(
            mTracker.getTablesToSync()
        )
        mTracker.onAdded(2, 4)
        assertNull(
            mTracker.getTablesToSync()
        )
        mTracker.onRemoved(2)
        assertNull(
            mTracker.getTablesToSync()
        )
        mTracker.onRemoved(2, 4)
        assertNull(
            mTracker.getTablesToSync()
        )
        mTracker.onAdded(1, 3)
        mTracker.onRemoved(2, 4)
        assertThat(
            mTracker.getTablesToSync()
        ).isEqualTo(
            createResponse(
                1,
                InvalidationTracker.ObservedTableTracker.ADD,
                2,
                InvalidationTracker.ObservedTableTracker.REMOVE,
                3,
                InvalidationTracker.ObservedTableTracker.ADD,
                4,
                InvalidationTracker.ObservedTableTracker.REMOVE
            )
        )
    }

    private fun initState(vararg tableIds: Int) {
        mTracker.onAdded(*tableIds)
        mTracker.getTablesToSync()
    }

    companion object {
        private const val TABLE_COUNT = 5
        private fun createResponse(vararg tuples: Int): IntArray {
            val result = IntArray(TABLE_COUNT)
            Arrays.fill(result, InvalidationTracker.ObservedTableTracker.NO_OP)
            var i = 0
            while (i < tuples.size) {
                result[tuples[i]] = tuples[i + 1]
                i += 2
            }
            return result
        }
    }
}
