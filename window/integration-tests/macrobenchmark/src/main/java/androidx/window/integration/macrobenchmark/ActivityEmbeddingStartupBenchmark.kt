/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.integration.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.measureStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance test for Activity Embedding startup.
 *
 * To run the test: ./gradlew window:integration-tests:macrobenchmark:connectedReleaseAndroidTest
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ActivityEmbeddingStartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureStartup(
        compilationMode = CompilationMode.DEFAULT,
        startupMode = StartupMode.COLD,
        packageName = "androidx.window.integration.macrobenchmark.target"
    ) {
        action = "androidx.window.integration.macrobenchmark.target.ACTIVITY1"
    }
}
