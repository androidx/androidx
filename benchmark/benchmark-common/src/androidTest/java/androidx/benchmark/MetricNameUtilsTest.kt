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

package androidx.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MetricNameUtilsTest {
    @Test
    fun toSnakeCase() {
        assertEquals("noop", "noop".toSnakeCase())
        assertEquals("camel_case", "camelCase".toSnakeCase())
        assertEquals("time_ns", "timeNs".toSnakeCase())
        assertEquals("a_ba_ba_b", "aBaBaB".toSnakeCase())
        assertEquals("frame_time_90th_percentile_ms", "frameTime90thPercentileMs".toSnakeCase())
    }

    @Test
    fun toOutputMetricName() {
        assertEquals("noop", "noop".toOutputMetricName())
        assertEquals("camel_case", "camelCase".toOutputMetricName())
        assertEquals("time_nanos", "timeNs".toOutputMetricName())
        assertEquals("a_ba_ba_b", "aBaBaB".toOutputMetricName())
        assertEquals(
            "frame_time_90th_percentile_millis",
            "frameTime90thPercentileMs".toOutputMetricName()
        )
    }
}