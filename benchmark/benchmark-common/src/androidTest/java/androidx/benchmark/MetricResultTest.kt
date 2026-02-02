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

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MetricResultTest {
    @Test
    fun constructorThrowsIfEmpty() {
        val exception =
            assertFailsWith<IllegalArgumentException> { MetricResult("test", emptyList()) }

        assertEquals("At least one result is necessary, 0 found for test.", exception.message!!)
    }

    @Test
    fun zeros() {
        val metricResult = MetricResult("test", listOf(0.0, 0.0))
        assertEquals(0.0, metricResult.min, 0.0)
        assertEquals(0.0, metricResult.max, 0.0)
        assertEquals(0.0, metricResult.median, 0.0)
        assertEquals(0.0, metricResult.standardDeviation, 0.0)
        assertEquals(0.0, metricResult.coefficientOfVariation, 0.0)

        assertEquals(0, metricResult.minIndex)
        assertEquals(0, metricResult.maxIndex)
        assertEquals(1, metricResult.medianIndex)
    }

    @Test
    fun repeat() {
        val metricResult = MetricResult("test", listOf(10.0, 10.0, 10.0, 10.0))
        assertEquals(10.0, metricResult.min, 0.0)
        assertEquals(10.0, metricResult.max, 0.0)
        assertEquals(10.0, metricResult.median, 0.0)
        assertEquals(0.0, metricResult.standardDeviation, 0.0)
        assertEquals(0.0, metricResult.coefficientOfVariation, 0.0)

        assertEquals(0, metricResult.minIndex)
        assertEquals(0, metricResult.maxIndex)
        assertEquals(2, metricResult.medianIndex)
    }

    @Test
    fun one() {
        val metricResult = MetricResult("test", listOf(10.0))
        assertEquals(10.0, metricResult.min, 0.0)
        assertEquals(10.0, metricResult.max, 0.0)
        assertEquals(10.0, metricResult.median, 0.0)
        assertEquals(0.0, metricResult.standardDeviation, 0.0)
        assertEquals(0.0, metricResult.coefficientOfVariation, 0.0)

        assertEquals(0, metricResult.minIndex)
        assertEquals(0, metricResult.maxIndex)
        assertEquals(0, metricResult.medianIndex)
    }

    @Test
    fun simple() {
        val metricResult = MetricResult("test", (0..100).map { it.toDouble() })
        assertEquals(50.0, metricResult.median, 0.0)
        assertEquals(100.0, metricResult.max, 0.0)
        assertEquals(0.0, metricResult.min, 0.0)
        assertEquals(29.3, metricResult.standardDeviation, 0.05)
        assertEquals(0.586, metricResult.coefficientOfVariation, 0.0005)

        assertEquals(0, metricResult.minIndex)
        assertEquals(100, metricResult.maxIndex)
        assertEquals(50, metricResult.medianIndex)
    }

    @Test
    fun lerp() {
        assertEquals(MetricResult.lerp(0.0, 1000.0, 0.5), 500.0, 0.0)
        assertEquals(MetricResult.lerp(0.0, 1000.0, 0.75), 750.0, 0.0)
        assertEquals(MetricResult.lerp(0.0, 1000.0, 0.25), 250.0, 0.0)
        assertEquals(MetricResult.lerp(500.0, 1000.0, 0.25), 625.0, 0.0)
    }

    @Test
    fun putInBundle() {
        val metricResult = MetricResult("test", (0..1).map { it.toDouble() })
        Bundle().apply {
            metricResult.putInBundle(this, prefix = "prefix_")

            // can't use equality check, since Bundle doesn't implement equals(), and
            // toString() comparison doesn't seem safe
            assertEquals(0.5, getDouble("prefix_test_median"), 0.001)
            assertEquals(0.71, getDouble("prefix_test_stddev"), 0.01)
            assertEquals(1.0, getDouble("prefix_test_max"), 0.001)
            assertEquals(0.0, getDouble("prefix_test_min"), 0.001)
        }
    }

    @Test
    fun putPercentilesInBundle() {
        val metricResult = MetricResult("test", (0..100).map { it.toDouble() })
        Bundle().apply {
            metricResult.putPercentilesInBundle(this, prefix = "prefix_")

            // can't use equality check, since Bundle doesn't implement equals(), and
            // toString() comparison doesn't seem safe
            assertEquals(50.0, getDouble("prefix_test_p50"), 0.1)
            assertEquals(90.0, getDouble("prefix_test_p90"), 0.1)
            assertEquals(95.0, getDouble("prefix_test_p95"), 0.1)
            assertEquals(99.0, getDouble("prefix_test_p99"), 0.1)
        }
    }

    @Test
    fun getPercentile() {
        (0..100).forEach {
            assertEquals(
                it.toDouble(),
                MetricResult.getPercentile(listOf(0.0, 25.0, 50.0, 75.0, 100.0), it),
                0.01,
            )
        }
    }
}
