/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.measureStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * ./gradlew :privacysandbox:sdkruntime:integration-tests:macrobenchmark:connectedReleaseAndroidTest
 */
@LargeTest
@RunWith(Parameterized::class)
class SdkRuntimeBenchmark(
    @Suppress("unused") private val ciTestConfigType: String // Added to test name by Parameterized
) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() =
        benchmarkRule.measureStartup(
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.COLD,
            packageName = "androidx.privacysandbox.sdkruntime.integration.testapp",
        ) {
            action = "androidx.privacysandbox.sdkruntime.integration.testapp.BenchmarkActivity"
        }

    companion object {
        /** Add test config type (main or compat) to test name */
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun params(): List<String> =
            listOf(
                InstrumentationRegistry.getArguments()
                    .getString("androidx.testConfigType", "LOCAL_RUN")
            )
    }
}
