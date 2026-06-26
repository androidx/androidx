/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.tracing.benchmark.driver

import android.os.SystemClock
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Ignore
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ClockBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    @Ignore(value = "Not something we need to compare in CI")
    fun systemClock() {
        benchmarkRule.measureRepeated { SystemClock.elapsedRealtimeNanos() }
    }

    @Test
    @Ignore(value = "Not something we need to compare in CI")
    fun standardClock() {
        benchmarkRule.measureRepeated { System.nanoTime() }
    }
}
