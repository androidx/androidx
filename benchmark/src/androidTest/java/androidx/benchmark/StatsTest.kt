/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.benchmark

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class StatsTest {
    @Test
    fun simple() {
        val stats = Stats(listOf(10, 10, 10, 10))
        assertEquals(10.0, stats.mean, 0.0)
        assertEquals(10, stats.median)
        assertEquals(10, stats.max)
        assertEquals(10, stats.min)
        assertEquals(0.0, stats.standardDeviation, 0.0)
        assertEquals(10, stats.percentile90)
        assertEquals(10, stats.percentile95)
    }

    @Test
    fun one() {
        val stats = Stats(listOf(10))
        assertEquals(10.0, stats.mean, 0.0)
        assertEquals(10, stats.median)
        assertEquals(10, stats.max)
        assertEquals(10, stats.min)
        assertEquals(Double.NaN, stats.standardDeviation, 0.0)
        assertEquals(10, stats.percentile90)
        assertEquals(10, stats.percentile95)
    }

    @Test
    fun percentile() {
        val stats = Stats((1L..100L).toList())
        assertEquals(50.5, stats.mean, 0.0)
        assertTrue(stats.median == 50L || stats.median == 51L)
        assertEquals(100, stats.max)
        assertEquals(1, stats.min)
        assertEquals(29.01, stats.standardDeviation, 0.05)
        assertEquals(90, stats.percentile90)
        assertEquals(95, stats.percentile95)
    }
}
