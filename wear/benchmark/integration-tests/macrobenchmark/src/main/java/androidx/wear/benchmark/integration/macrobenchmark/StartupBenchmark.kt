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

package androidx.wear.benchmark.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.testutils.createStartupCompilationParams
import androidx.testutils.measureStartup
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class StartupBenchmark(
    private val startupMode: StartupMode,
    private val compilationMode: CompilationMode
) {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Before
    fun setUp() {
        disableChargingExperience()
    }

    @After
    fun destroy() {
        enableChargingExperience()
    }

    @Test
    fun startup() = benchmarkRule.measureStartup(
        compilationMode = compilationMode,
        startupMode = startupMode,
        packageName = "androidx.wear.benchmark.integration.macrobenchmark.target"
    ) {
        action = "androidx.wear.benchmark.integration.macrobenchmark.target" + ".STARTUP_ACTIVITY"
    }

    companion object {
        @Parameterized.Parameters(name = "startup={0},compilation={1}")
        @JvmStatic
        fun parameters() = createStartupCompilationParams()
    }
}
