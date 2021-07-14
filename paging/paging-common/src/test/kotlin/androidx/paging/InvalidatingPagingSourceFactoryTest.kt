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

package androidx.paging

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvalidatingPagingSourceFactoryTest {

    @Test
    fun getPagingSource() {
        val testFactory = InvalidatingPagingSourceFactory { TestPagingSource() }
        repeat(4) { testFactory() }
        assertEquals(4, testFactory.pagingSources.size)
    }

    @Test
    fun invalidateRemoveFromList() {
        val testFactory = InvalidatingPagingSourceFactory { TestPagingSource() }
        repeat(4) { testFactory() }
        assertEquals(4, testFactory.pagingSources.size)

        testFactory.invalidate()
        assertEquals(0, testFactory.pagingSources.size)
    }

    @Test
    fun invalidatePagingSource() {
        val invalidateCalls = Array(4) { false }
        val testFactory = InvalidatingPagingSourceFactory { TestPagingSource() }
        repeat(4) { testFactory() }
        testFactory.pagingSources.forEachIndexed { index, pagingSource ->
            pagingSource.registerInvalidatedCallback {
                invalidateCalls[index] = true
            }
        }
        testFactory.invalidate()
        assertTrue { invalidateCalls.all { it } }
    }

    @Test
    fun skipInvalidatedPagingSources() {
        val testFactory = InvalidatingPagingSourceFactory { TestPagingSource() }
        repeat(4) { testFactory() }

        val pagingSource = testFactory.pagingSources[0]
        pagingSource.invalidate()

        assertTrue(pagingSource.invalid)

        var invalidateCount = 0

        testFactory.pagingSources.forEach {
            it.registerInvalidatedCallback {
                invalidateCount++
            }
        }

        testFactory.invalidate()

        assertEquals(4, invalidateCount)
    }

    @Test
    fun invalidate_preventsInfiniteLoopsWithSynchronousInvalidation() {
        val factory = InvalidatingPagingSourceFactory { TestPagingSource() }

        fun startNewGeneration() {
            factory().registerInvalidatedCallback { startNewGeneration() }
        }

        startNewGeneration()

        // Ensure that .invalidate() does not iterate over new PagingSources that get created
        // after .invalidate() is called as it would result in an infinite loop.
        factory.invalidate()
    }
}
