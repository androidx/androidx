/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.wear.compose.material3.benchmark

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material3.MaterialTheme
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Benchmark for Wear Compose Material 3 [androidx.wear.compose.foundation.AmbientModeManager]. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AmbientModeManagerBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()
    private val ambientCaseFactory = { AmbientModeManagerTestCase() }

    @Before
    fun setUp() {
        Assume.assumeTrue(isWearSDKInstalled())
    }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(ambientCaseFactory)
    }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(ambientCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(ambientCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(ambientCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(ambientCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(ambientCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(ambientCaseFactory)
    }

    private fun isWearSDKInstalled(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager: PackageManager = context.packageManager
        return packageManager.systemSharedLibraryNames?.contains("wear-sdk") ?: false
    }
}

internal class AmbientModeManagerTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val ambientModeManager = rememberAmbientModeManager()
        CompositionLocalProvider(LocalAmbientModeManager provides ambientModeManager) {}
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}
