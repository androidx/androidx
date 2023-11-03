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

package androidx.compose.material3.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FloatingActionButtonBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val fabTestCaseFactory = { FloatingActionButtonTestCase() }
    private val extendedFabTestCaseFactory = { ExtendedFloatingActionButtonTestCase() }

    @Ignore
    @Test
    fun fab_first_compose() {
        benchmarkRule.benchmarkFirstCompose(fabTestCaseFactory)
    }

    @Ignore
    @Test
    fun extendedFab_first_compose() {
        benchmarkRule.benchmarkFirstCompose(extendedFabTestCaseFactory)
    }

    @Ignore
    @Test
    fun fab_measure() {
        benchmarkRule.benchmarkFirstMeasure(fabTestCaseFactory)
    }

    @Ignore
    @Test
    fun extendedFab_measure() {
        benchmarkRule.benchmarkFirstMeasure(extendedFabTestCaseFactory)
    }

    @Ignore
    @Test
    fun fab_layout() {
        benchmarkRule.benchmarkFirstLayout(fabTestCaseFactory)
    }

    @Ignore
    @Test
    fun extendedFab_layout() {
        benchmarkRule.benchmarkFirstLayout(extendedFabTestCaseFactory)
    }

    @Ignore
    @Test
    fun fab_draw() {
        benchmarkRule.benchmarkFirstDraw(fabTestCaseFactory)
    }

    @Ignore
    @Test
    fun extendedFab_draw() {
        benchmarkRule.benchmarkFirstDraw(extendedFabTestCaseFactory)
    }

    @Test
    fun fab_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(fabTestCaseFactory)
    }

    @Test
    fun extendedFab_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(extendedFabTestCaseFactory)
    }
}

internal class FloatingActionButtonTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        FloatingActionButton(onClick = { /*TODO*/ }) {
            Box(modifier = Modifier.size(24.dp))
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

internal class ExtendedFloatingActionButtonTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        ExtendedFloatingActionButton(
            text = { Text(text = "Extended FAB") },
            icon = {
                Box(modifier = Modifier.size(24.dp))
            },
            onClick = { /*TODO*/ })
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
