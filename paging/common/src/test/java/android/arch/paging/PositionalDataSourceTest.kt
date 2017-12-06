/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.paging

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PositionalDataSourceTest {
    @Test
    fun computeFirstLoadPositionZero() {
        assertEquals(0, PositionalDataSource.computeFirstLoadPosition(0, 30, 10, 100))
    }

    @Test
    fun computeFirstLoadPositionRequestedPositionIncluded() {
        assertEquals(10, PositionalDataSource.computeFirstLoadPosition(10, 10, 10, 100))
    }

    @Test
    fun computeFirstLoadPositionRound() {
        assertEquals(10, PositionalDataSource.computeFirstLoadPosition(13, 30, 10, 100))
    }

    @Test
    fun computeFirstLoadPositionEndAdjusted() {
        assertEquals(70, PositionalDataSource.computeFirstLoadPosition(99, 30, 10, 100))
    }

    @Test
    fun computeFirstLoadPositionEndAdjustedAndAligned() {
        assertEquals(70, PositionalDataSource.computeFirstLoadPosition(99, 35, 10, 100))
    }
}
