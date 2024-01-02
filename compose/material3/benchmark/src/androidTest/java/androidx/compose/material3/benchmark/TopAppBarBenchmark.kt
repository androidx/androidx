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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class TopAppBarBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val topAppBarTestCaseFactory = { TopAppBarTestCase() }

    // Picking the LargeTopAppBar to benchmark a two-row variation.
    private val largeTopAppBarTestCaseFactory = { LargeTopAppBarTestCase() }

    @Ignore
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(topAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(topAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(topAppBarTestCaseFactory)
    }

    @Ignore
    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(topAppBarTestCaseFactory)
    }

    @Test
    fun topAppBar_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(topAppBarTestCaseFactory)
    }

    @Test
    fun largeTopAppBar_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(largeTopAppBarTestCaseFactory)
    }
}

internal class TopAppBarTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        // Keeping it to the minimum, with just the necessary title.
        TopAppBar(title = { Text("Hello") }, modifier = Modifier.fillMaxWidth())
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

internal class LargeTopAppBarTestCase : LayeredComposeTestCase() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun MeasuredContent() {
        // Keeping it to the minimum, with just the necessary title.
        LargeTopAppBar(title = { Text("Hello") }, modifier = Modifier.fillMaxWidth())
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
