/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.junit4

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
public class BenchmarkRuleTest {
    @get:Rule
    public val benchmarkRule: BenchmarkRule = BenchmarkRule(enableReport = false)

    @Test
    @Ignore("b/280041271")
    public fun runWithTimingDisabled() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled {
                Thread.sleep(5)
            }
        }
        val min = benchmarkRule.getState().getMinTimeNanos()
        Assert.assertTrue(
            "minimum $min should be less than 1ms",
            min < TimeUnit.MILLISECONDS.toNanos(1)
        )
    }
}
