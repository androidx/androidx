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

package androidx.compose.foundation.benchmark.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayoutDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextStyleScope
import androidx.compose.ui.text.benchmark.TextBenchmarkTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark for [BasicText] with a draw-only style (such as [TextStyleScope.setBrush] / [Brush]),
 * comparing the initial and state-toggling performance of the `styleProvider` overload against the
 * [TextStyle] parameter overload.
 */
@LargeTest
@RunWith(Parameterized::class)
class BasicTextDrawStyleBenchmark(private val textLength: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0}")
        fun initParameters(): Array<Any> = arrayOf(32, 512).filterForCi()
    }

    @get:Rule val textBenchmarkRule = TextBenchmarkTestRule()

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val width = textBenchmarkRule.widthDp.dp
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    private val styleProviderCaseFactory = {
        textBenchmarkRule.generator { textGenerator ->
            val texts =
                List(textBenchmarkRule.repeatTimes) { textGenerator.nextParagraph(textLength) }
            BasicTextStyleProviderDrawStyleTestCase(
                texts = texts,
                width = width,
                fontSize = fontSize,
            )
        }
    }

    private val textStyleCaseFactory = {
        textBenchmarkRule.generator { textGenerator ->
            val texts =
                List(textBenchmarkRule.repeatTimes) { textGenerator.nextParagraph(textLength) }
            BasicTextTextStyleDrawStyleTestCase(texts = texts, width = width, fontSize = fontSize)
        }
    }

    // --- Initial Brush (styleProvider overload) ---

    /** Measure initial compose time with brush using styleProvider overload. */
    @Test
    fun firstBrush_styleProvider_compose() {
        benchmarkRule.benchmarkFirstCompose(styleProviderCaseFactory)
    }

    /** Measure initial measure time with brush using styleProvider overload. */
    @Test
    fun firstBrush_styleProvider_measure() {
        benchmarkRule.benchmarkFirstMeasure(styleProviderCaseFactory)
    }

    /** Measure initial layout time with brush using styleProvider overload. */
    @Test
    fun firstBrush_styleProvider_layout() {
        benchmarkRule.benchmarkFirstLayout(styleProviderCaseFactory)
    }

    /** Measure initial draw time with brush using styleProvider overload. */
    @Test
    fun firstBrush_styleProvider_draw() {
        benchmarkRule.benchmarkFirstDraw(styleProviderCaseFactory)
    }

    /**
     * Measure initial compose + measure + layout + draw time with brush using styleProvider
     * overload.
     */
    @Test
    fun firstBrush_styleProvider_composeMeasureLayoutDraw() {
        benchmarkRule.benchmarkToFirstPixel(styleProviderCaseFactory)
    }

    // --- Initial Brush (TextStyle parameter overload) ---

    /** Measure initial compose time with brush using TextStyle parameter overload. */
    @Test
    fun firstBrush_textStyle_compose() {
        benchmarkRule.benchmarkFirstCompose(textStyleCaseFactory)
    }

    /** Measure initial measure time with brush using TextStyle parameter overload. */
    @Test
    fun firstBrush_textStyle_measure() {
        benchmarkRule.benchmarkFirstMeasure(textStyleCaseFactory)
    }

    /** Measure initial layout time with brush using TextStyle parameter overload. */
    @Test
    fun firstBrush_textStyle_layout() {
        benchmarkRule.benchmarkFirstLayout(textStyleCaseFactory)
    }

    /** Measure initial draw time with brush using TextStyle parameter overload. */
    @Test
    fun firstBrush_textStyle_draw() {
        benchmarkRule.benchmarkFirstDraw(textStyleCaseFactory)
    }

    /**
     * Measure initial compose + measure + layout + draw time with brush using TextStyle parameter
     * overload.
     */
    @Test
    fun firstBrush_textStyle_composeMeasureLayoutDraw() {
        benchmarkRule.benchmarkToFirstPixel(textStyleCaseFactory)
    }

    // --- Toggle Brush (styleProvider overload) ---

    /**
     * Measure recompose time when toggling brush using styleProvider overload. Recomposition should
     * be skipped because the state is read in the layout/draw node scope.
     */
    @Test
    fun toggleBrush_styleProvider_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            styleProviderCaseFactory,
            assertOneRecomposition = false,
            requireRecomposition = false,
        )
    }

    /** Measure measure time when toggling brush using styleProvider overload. */
    @Test
    fun toggleBrush_styleProvider_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            styleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /** Measure layout time when toggling brush using styleProvider overload. */
    @Test
    fun toggleBrush_styleProvider_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            styleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /** Measure draw time when toggling brush using styleProvider overload. */
    @Test
    fun toggleBrush_styleProvider_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            styleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measure recompose + measure + layout + draw time when toggling brush using styleProvider
     * overload.
     */
    @Test
    fun toggleBrush_styleProvider_composeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            styleProviderCaseFactory,
            assertOneRecomposition = false,
            requireRecomposition = false,
        )
    }

    // --- TextStyle parameter overload ---

    /**
     * Measure recompose time when toggling brush using TextStyle parameter overload. Recomposition
     * occurs because the state is read during the composition phase.
     */
    @Test
    fun toggleBrush_textStyle_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            textStyleCaseFactory,
            assertOneRecomposition = true,
            requireRecomposition = true,
        )
    }

    /** Measure measure time when toggling brush using TextStyle parameter overload. */
    @Test
    fun toggleBrush_textStyle_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            textStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /** Measure layout time when toggling brush using TextStyle parameter overload. */
    @Test
    fun toggleBrush_textStyle_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            textStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /** Measure draw time when toggling brush using TextStyle parameter overload. */
    @Test
    fun toggleBrush_textStyle_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            textStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /**
     * Measure recompose + measure + layout + draw time when toggling brush using TextStyle
     * parameter overload.
     */
    @Test
    fun toggleBrush_textStyle_composeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            textStyleCaseFactory,
            assertOneRecomposition = true,
            requireRecomposition = true,
        )
    }
}

private val Brush1 = Brush.linearGradient(listOf(Color.Red, Color.Blue))
private val Brush2 = Brush.linearGradient(listOf(Color.Green, Color.Yellow))

private class BasicTextStyleProviderDrawStyleTestCase(
    private val texts: List<String>,
    private val width: Dp,
    private val fontSize: TextUnit,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val currentBrush = mutableStateOf(Brush1)

    @Composable
    override fun MeasuredContent() {
        for (text in texts) {
            Text(text)
        }
    }

    /**
     * Separate restartable composable wrapper per item (similar to Material `Text` or an item
     * composable) so that each `BasicText` has its own recompose scope rather than sharing a single
     * parent `MeasuredContent` recompose scope across the loop.
     */
    @Composable
    private fun Text(text: String) {
        BasicText(
            text = text,
            styleProvider = {
                setFontSize(this@BasicTextStyleProviderDrawStyleTestCase.fontSize)
                setBrush(currentBrush.value)
            },
        )
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

    override fun toggleState() {
        currentBrush.value = if (currentBrush.value == Brush1) Brush2 else Brush1
    }
}

private class BasicTextTextStyleDrawStyleTestCase(
    private val texts: List<String>,
    private val width: Dp,
    private val fontSize: TextUnit,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val currentBrush = mutableStateOf(Brush1)

    @Composable
    override fun MeasuredContent() {
        for (text in texts) {
            Text(text)
        }
    }

    /**
     * Separate restartable composable wrapper per item (similar to Material `Text` or an item
     * composable) so that each `BasicText` has its own recompose scope rather than sharing a single
     * parent `MeasuredContent` recompose scope across the loop.
     */
    @Composable
    private fun Text(text: String) {
        BasicText(
            text = text,
            style =
                TextStyle(
                    fontSize = fontSize,
                    brush = currentBrush.value,
                ),
        )
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

    override fun toggleState() {
        currentBrush.value = if (currentBrush.value == Brush1) Brush2 else Brush1
    }
}
