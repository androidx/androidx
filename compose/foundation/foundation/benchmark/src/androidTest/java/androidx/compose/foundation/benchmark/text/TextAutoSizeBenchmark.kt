/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.benchmark.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.benchmark.TextBenchmarkTestRule
import androidx.compose.ui.text.benchmark.cartesian
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** The benchmark for [Text] composable with the input being a plain string. */
@LargeTest
@RunWith(Parameterized::class)
class TextAutoSizeBenchmark(private val textLength: Int, private val autoSize: TextAutoSize) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0},autoSize={1}")
        fun initParameters() =
            cartesian(
                // Text Length
                arrayOf(32, 512),
                // AutoSize
                arrayOf(TextAutoSize.StepBased())
            )
    }

    @get:Rule val textBenchmarkRule = TextBenchmarkTestRule()

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val width = textBenchmarkRule.widthDp.dp

    private val caseFactory = {
        textBenchmarkRule.generator { textGenerator ->
            /**
             * Text render has a word cache in the underlying system. To get a proper metric of its
             * performance, the cache needs to be disabled, which unfortunately is not doable via
             * public API. Here is a workaround which generates a new string when a new test case is
             * created.
             */
            val texts =
                List(textBenchmarkRule.repeatTimes) {
                    AnnotatedString(textGenerator.nextParagraph(textLength))
                }
            AutoSizeTextInColumnTestCase(texts = texts, width = width, autoSize = autoSize)
        }
    }

    /**
     * Measure the time taken to compose a [Text] composable from scratch with the given input. This
     * is the time taken to call the [Text] composable function.
     */
    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(caseFactory)
    }

    /**
     * Measure the time taken by the first time measure the [Text] composable with the given input.
     * This is mainly the time used to measure all the [Measurable]s in the [Text] composable.
     */
    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    /**
     * Measure the time taken by the first time layout the [Text] composable with the given input.
     * This is mainly the time used to place [Placeable]s in [Text] composable.
     */
    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }

    /**
     * Measure the time taken by layout the [Text] composable after the layout constrains changed.
     * This is mainly the time used to re-measure and re-layout the composable.
     */
    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(caseFactory)
    }
}

/** The benchmark test case for [Text], where the input is a plain string. */
private class AutoSizeTextInColumnTestCase(
    private val texts: List<AnnotatedString>,
    private val width: Dp,
    private val autoSize: TextAutoSize
) : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        for (text in texts) {
            BasicText(text = text, autoSize = autoSize, modifier = Modifier.height(30.dp))
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        Column(
            modifier =
                Modifier.wrapContentSize(Alignment.Center)
                    .width(width)
                    .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}
