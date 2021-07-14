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
public class InstrumentationResultsTest {
    @Test
    public fun ideSummary_alignment() {
        val summary1 = InstrumentationResults.ideSummaryLine("foo", 1000, 100)
        val summary2 = InstrumentationResults.ideSummaryLine("fooBarLongerKey", 10000, 0)

        assertEquals(
            summary1.indexOf("foo"),
            summary2.indexOf("foo")
        )
    }

    @Test
    public fun ideSummary_allocs() {
        assertEquals(
            "        1,000 ns    foo",
            InstrumentationResults.ideSummaryLine("foo", 1000, null)
        )
        assertEquals(
            "        1,000 ns          10 allocs    foo",
            InstrumentationResults.ideSummaryLine("foo", 1000, 10)
        )
    }
}
