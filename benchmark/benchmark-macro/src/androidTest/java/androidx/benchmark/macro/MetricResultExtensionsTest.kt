/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.benchmark.MetricResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@SmallTest
class MetricResultExtensionsTest {
    @Test
    fun mergeToSingleMetricResults_trivial() {
        assertEquals(
            expected = listOf(
                // note, bar sorted first
                MetricResult("bar", listOf(1.0)),
                MetricResult("foo", listOf(0.0))
            ),
            actual = listOf(
                mapOf("foo" to 0.0, "bar" to 1.0)
            ).mergeToSingleMetricResults()
        )
    }

    @Test
    fun mergeToSingleMetricResults_standard() {
        assertEquals(
            expected = listOf(
                // note, bar sorted first
                MetricResult("bar", listOf(101.0, 301.0, 201.0)),
                MetricResult("foo", listOf(100.0, 300.0, 200.0))
            ),
            actual = listOf(
                mapOf("foo" to 100.0, "bar" to 101.0),
                mapOf("foo" to 300.0, "bar" to 301.0),
                mapOf("foo" to 200.0, "bar" to 201.0),
            ).mergeToSingleMetricResults()
        )
    }

    @Test
    fun mergeToSingleMetricResults_missingKey() {
        assertEquals(
            expected = listOf(
                MetricResult("bar", listOf(101.0, 201.0)),
                MetricResult("foo", listOf(100.0, 200.0))
            ),
            actual = listOf(
                mapOf("foo" to 100.0, "bar" to 101.0),
                mapOf("foo" to 300.0), // bar missing! Skip this iteration!
                mapOf("foo" to 200.0, "bar" to 201.0),
            ).mergeToSingleMetricResults()
        )
    }

    @Test
    fun mergeToSampledMetricResults_trivial() {
        assertEquals(
            expected = listOf(
                // note, bar sorted first
                MetricResult("bar", listOf(1.0)),
                MetricResult("foo", listOf(0.0))
            ),
            actual = listOf(
                mapOf("foo" to listOf(0.0), "bar" to listOf(1.0))
            ).mergeToSampledMetricResults()
        )
    }

    @Test
    fun mergeToSampledMetricResults_singleMeasurement() {
        assertEquals(
            expected = listOf(
                // note, bar sorted first
                MetricResult("bar", listOf(101.0, 301.0, 201.0)),
                MetricResult("foo", listOf(100.0, 300.0, 200.0))
            ),
            actual = listOf(
                mapOf("foo" to listOf(100.0), "bar" to listOf(101.0)),
                mapOf("foo" to listOf(300.0), "bar" to listOf(301.0)),
                mapOf("foo" to listOf(200.0), "bar" to listOf(201.0)),
            ).mergeToSampledMetricResults()
        )
    }

    @Test
    fun mergeToSampledMetricResults_standard() {
        assertEquals(
            expected = listOf(
                // note, bar sorted first
                MetricResult("bar", List(6) { it.toDouble() }),
                MetricResult("foo", List(6) { it.toDouble() })
            ),
            actual = listOf(
                mapOf("foo" to listOf(0.0, 1.0, 2.0), "bar" to listOf(0.0)),
                mapOf("foo" to listOf(3.0, 4.0, 5.0), "bar" to listOf(1.0, 2.0, 3.0, 4.0, 5.0)),
            ).mergeToSampledMetricResults()
        )
    }

    @Test
    fun mergeToSampledMetricResults_missingKey() {
        val exception = assertFailsWith<IllegalStateException> {
            listOf(
                mapOf("foo" to listOf(0.0), "bar" to listOf(0.0)),
                mapOf("foo" to listOf(1.0)),
            ).mergeToSampledMetricResults()
        }
        assertTrue(exception.message!!.contains("Iteration 1 didn't capture metric bar"))
    }
}
