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

package androidx.benchmark.macro.test

import androidx.benchmark.Stats
import androidx.benchmark.macro.ideSummaryString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class IdeSummaryStringTest {
    @Test
    fun minimalSample() {
        val stats = Stats(longArrayOf(0, 1, 2), "Metric")
        assertEquals(
            """
                |foo
                |  Metric   min 0,   median 1,   max 2
                |
            """.trimMargin(),
            ideSummaryString("foo", listOf(stats))
        )
    }

    @Test
    fun complexSample() {
        val metric1 = Stats(longArrayOf(0, 1, 2), "Metric1")
        val metric2 = Stats(longArrayOf(0, 111, 222), "Metric2")
        assertEquals(
            """
                |foo
                |  Metric1   min   0,   median   1,   max   2
                |  Metric2   min   0,   median 111,   max 222
                |
            """.trimMargin(),
            ideSummaryString("foo", listOf(metric1, metric2))
        )
    }
}