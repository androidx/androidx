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

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DividerBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val horizontalDividerTestCaseFactory = { HorizontalDividerTestCase() }
    private val verticalDividerTestCaseFactory = { VerticalDividerTestCase() }

    @Ignore
    @Test
    fun horizontalDivider_first_compose() {
        benchmarkRule.benchmarkFirstCompose(horizontalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalDivider_first_compose() {
        benchmarkRule.benchmarkFirstCompose(verticalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun horizontalDivider_measure() {
        benchmarkRule.benchmarkFirstMeasure(horizontalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalDivider_measure() {
        benchmarkRule.benchmarkFirstMeasure(verticalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun horizontalDivider_layout() {
        benchmarkRule.benchmarkFirstLayout(horizontalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalDivider_layout() {
        benchmarkRule.benchmarkFirstLayout(verticalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun horizontalDivider_draw() {
        benchmarkRule.benchmarkFirstDraw(horizontalDividerTestCaseFactory)
    }

    @Ignore
    @Test
    fun verticalDivider_draw() {
        benchmarkRule.benchmarkFirstDraw(verticalDividerTestCaseFactory)
    }

    @Test
    fun horizontalDivider_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(horizontalDividerTestCaseFactory)
    }

    @Test
    fun verticalDivider_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(verticalDividerTestCaseFactory)
    }
}

internal class HorizontalDividerTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        HorizontalDivider()
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}

internal class VerticalDividerTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        VerticalDivider()
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
