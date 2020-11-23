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

package androidx.benchmark.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkConfig
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MacrobenchmarkRuleTest {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    @Ignore("Not running the test in CI")
    fun basicTest() = benchmarkRule.measureRepeated(
        MacrobenchmarkConfig(
            packageName = "androidx.benchmark.integration.macrobenchmark.target",
            listOf(StartupTimingMetric()),
            CompilationMode.Speed,
            killProcessEachIteration = true,
            iterations = 4
        )
    ) {
        pressHome()
        launchPackageAndWait()
    }
}
