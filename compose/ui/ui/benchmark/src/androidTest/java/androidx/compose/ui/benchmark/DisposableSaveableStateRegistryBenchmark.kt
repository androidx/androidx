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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.ui.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.platform.DisposableSaveableStateRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DisposableSaveableStateRegistryBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun benchmarkPerformSave() {
        with(benchmarkRule) {
            runBenchmarkFor({ RegistryTestCase() }) {
                runOnUiThread { doFramesUntilNoChangesPending() }
                var registry: SaveableStateRegistry? = null
                runOnUiThread { registry = getTestCase().registry }

                // Assert type to ensure we benchmark the production Android implementation.
                assertTrue(registry is DisposableSaveableStateRegistry)

                // Measure performSave because it runs canBeSaved checks for rememberSaveable.
                measureRepeatedOnUiThread { registry!!.performSave() }
            }
        }
    }

    private class RegistryTestCase : ComposeTestCase {
        var registry: SaveableStateRegistry? = null

        @Composable
        override fun Content() {
            registry = LocalSaveableStateRegistry.current

            // Register multiple values for stable performSave workload.
            repeat(10_000) { index ->
                // UNUSED_VARIABLE: prevent compiler optimization to Unit.
                // DEPRECATION: rememberSaveable(key) deprecated. Needed to populate keys.
                @Suppress("UNUSED_VARIABLE", "DEPRECATION")
                val unused = rememberSaveable(key = index.toString()) { "value_$index" }
            }
        }
    }
}
