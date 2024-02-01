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

package androidx.wear.compose.material.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.TimeTextDefaults
import androidx.wear.compose.material.TimeTextDefaults.CurvedTextSeparator
import androidx.wear.compose.material.curvedText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for Wear Compose TimeText.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TimeTextBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val timeTextCaseFactory = { TimeTextTestCase() }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(timeTextCaseFactory)
    }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(timeTextCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(timeTextCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(timeTextCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(timeTextCaseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(timeTextCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(timeTextCaseFactory)
    }
}

internal class TimeTextTestCase : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        TimeText(
            startLinearContent = {
                Text(
                    text = "Leading content",
                )
            },
            textLinearSeparator = {
                TimeTextDefaults.TextSeparator()
            },
            endLinearContent = {
                Text(
                    text = "Trailing content",
                )
            },
            startCurvedContent = {
                curvedText(
                    text = "Leading content"
                )
            },
            textCurvedSeparator = {
                CurvedTextSeparator()
            },
            endCurvedContent = {
                curvedText(
                    text = "Trailing content"
                )
            },
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
}
