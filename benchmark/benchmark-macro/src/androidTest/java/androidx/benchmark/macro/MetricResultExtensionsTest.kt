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
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@SmallTest
class MetricResultExtensionsTest {
    @Test
    fun mergeToMetricResults_trivial() {
        assertEquals(
            expected = listOf(
                // note, bar sorted first
                MetricResult("bar", listOf(1.0)),
                MetricResult("foo", listOf(0.0))
            ),
            actual = listOf(
                mapOf("foo" to 0.0, "bar" to 1.0)
            ).mergeToMetricResults(tracePaths = emptyList())
        )
    }

    @Test
    fun mergeToMetricResults_standard() {
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
            ).mergeToMetricResults(tracePaths = emptyList())
        )
    }

    @Test
    fun mergeToMetricResults_missingKey() {
        assertEquals(
            expected = listOf(
                MetricResult("bar", listOf(101.0, 201.0)),
                MetricResult("foo", listOf(100.0, 200.0))
            ),
            actual = listOf(
                mapOf("foo" to 100.0, "bar" to 101.0),
                mapOf("foo" to 300.0), // bar missing! Skip this iteration!
                mapOf("foo" to 200.0, "bar" to 201.0),
            ).mergeToMetricResults(
                tracePaths = listOf("trace1.trace", "trace2.trace", "trace3.trace")
            )
        )
    }
}
