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
 * Benchmark for [BasicText] with a layout-affecting style (such as [TextStyleScope.setBackground] /
 * [TextStyle.background] or [TextStyleScope.setFontSize] / [TextStyle.fontSize]), comparing the
 * initial and state-toggling performance of the `styleProvider` overload against the [TextStyle]
 * parameter overload.
 */
@LargeTest
@RunWith(Parameterized::class)
class BasicTextLayoutStyleBenchmark(private val textLength: Int) {
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
            BasicTextStyleProviderLayoutStyleTestCase(
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
            BasicTextTextStyleLayoutStyleTestCase(
                texts = texts,
                width = width,
                fontSize = fontSize,
            )
        }
    }

    private val fontSizeStyleProviderCaseFactory = {
        textBenchmarkRule.generator { textGenerator ->
            val texts =
                List(textBenchmarkRule.repeatTimes) { textGenerator.nextParagraph(textLength) }
            BasicTextStyleProviderFontSizeTestCase(
                texts = texts,
                width = width,
            )
        }
    }

    private val fontSizeTextStyleCaseFactory = {
        textBenchmarkRule.generator { textGenerator ->
            val texts =
                List(textBenchmarkRule.repeatTimes) { textGenerator.nextParagraph(textLength) }
            BasicTextTextStyleFontSizeTestCase(
                texts = texts,
                width = width,
            )
        }
    }

    // --- Initial Background (styleProvider overload) ---

    /** Measure initial compose time with background using styleProvider overload. */
    @Test
    fun firstBackground_styleProvider_compose() {
        benchmarkRule.benchmarkFirstCompose(styleProviderCaseFactory)
    }

    /** Measure initial measure time with background using styleProvider overload. */
    @Test
    fun firstBackground_styleProvider_measure() {
        benchmarkRule.benchmarkFirstMeasure(styleProviderCaseFactory)
    }

    /** Measure initial layout time with background using styleProvider overload. */
    @Test
    fun firstBackground_styleProvider_layout() {
        benchmarkRule.benchmarkFirstLayout(styleProviderCaseFactory)
    }

    /** Measure initial draw time with background using styleProvider overload. */
    @Test
    fun firstBackground_styleProvider_draw() {
        benchmarkRule.benchmarkFirstDraw(styleProviderCaseFactory)
    }

    /**
     * Measure initial compose + measure + layout + draw time with background using styleProvider
     * overload.
     */
    @Test
    fun firstBackground_styleProvider_composeMeasureLayoutDraw() {
        benchmarkRule.benchmarkToFirstPixel(styleProviderCaseFactory)
    }

    // --- Initial Background (TextStyle parameter overload) ---

    /** Measure initial compose time with background using TextStyle parameter overload. */
    @Test
    fun firstBackground_textStyle_compose() {
        benchmarkRule.benchmarkFirstCompose(textStyleCaseFactory)
    }

    /** Measure initial measure time with background using TextStyle parameter overload. */
    @Test
    fun firstBackground_textStyle_measure() {
        benchmarkRule.benchmarkFirstMeasure(textStyleCaseFactory)
    }

    /** Measure initial layout time with background using TextStyle parameter overload. */
    @Test
    fun firstBackground_textStyle_layout() {
        benchmarkRule.benchmarkFirstLayout(textStyleCaseFactory)
    }

    /** Measure initial draw time with background using TextStyle parameter overload. */
    @Test
    fun firstBackground_textStyle_draw() {
        benchmarkRule.benchmarkFirstDraw(textStyleCaseFactory)
    }

    /**
     * Measure initial compose + measure + layout + draw time with background using TextStyle
     * parameter overload.
     */
    @Test
    fun firstBackground_textStyle_composeMeasureLayoutDraw() {
        benchmarkRule.benchmarkToFirstPixel(textStyleCaseFactory)
    }

    // --- Toggle Background (styleProvider overload) ---

    /**
     * Measure recompose time when toggling background using styleProvider overload. Recomposition
     * should be skipped because the state is read in the layout/draw node scope.
     */
    @Test
    fun toggleBackground_styleProvider_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            styleProviderCaseFactory,
            assertOneRecomposition = false,
            requireRecomposition = false,
        )
    }

    /** Measure measure time when toggling background using styleProvider overload. */
    @Test
    fun toggleBackground_styleProvider_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            styleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /** Measure layout time when toggling background using styleProvider overload. */
    @Test
    fun toggleBackground_styleProvider_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            styleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /** Measure draw time when toggling background using styleProvider overload. */
    @Test
    fun toggleBackground_styleProvider_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            styleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measure recompose + measure + layout + draw time when toggling background using styleProvider
     * overload.
     */
    @Test
    fun toggleBackground_styleProvider_composeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            styleProviderCaseFactory,
            assertOneRecomposition = false,
            requireRecomposition = false,
        )
    }

    // --- Toggle Background (TextStyle parameter overload) ---

    /**
     * Measure recompose time when toggling background using TextStyle parameter overload.
     * Recomposition occurs because the state is read during the composition phase.
     */
    @Test
    fun toggleBackground_textStyle_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            textStyleCaseFactory,
            assertOneRecomposition = true,
            requireRecomposition = true,
        )
    }

    /** Measure measure time when toggling background using TextStyle parameter overload. */
    @Test
    fun toggleBackground_textStyle_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            textStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /** Measure layout time when toggling background using TextStyle parameter overload. */
    @Test
    fun toggleBackground_textStyle_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            textStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /** Measure draw time when toggling background using TextStyle parameter overload. */
    @Test
    fun toggleBackground_textStyle_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            textStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /**
     * Measure recompose + measure + layout + draw time when toggling background using TextStyle
     * parameter overload.
     */
    @Test
    fun toggleBackground_textStyle_composeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            textStyleCaseFactory,
            assertOneRecomposition = true,
            requireRecomposition = true,
        )
    }

    // --- Initial FontSize (styleProvider overload) ---

    /** Measure initial compose time with fontSize using styleProvider overload. */
    @Test
    fun firstFontSize_styleProvider_compose() {
        benchmarkRule.benchmarkFirstCompose(fontSizeStyleProviderCaseFactory)
    }

    /** Measure initial measure time with fontSize using styleProvider overload. */
    @Test
    fun firstFontSize_styleProvider_measure() {
        benchmarkRule.benchmarkFirstMeasure(fontSizeStyleProviderCaseFactory)
    }

    /** Measure initial layout time with fontSize using styleProvider overload. */
    @Test
    fun firstFontSize_styleProvider_layout() {
        benchmarkRule.benchmarkFirstLayout(fontSizeStyleProviderCaseFactory)
    }

    /** Measure initial draw time with fontSize using styleProvider overload. */
    @Test
    fun firstFontSize_styleProvider_draw() {
        benchmarkRule.benchmarkFirstDraw(fontSizeStyleProviderCaseFactory)
    }

    /**
     * Measure initial compose + measure + layout + draw time with fontSize using styleProvider
     * overload.
     */
    @Test
    fun firstFontSize_styleProvider_composeMeasureLayoutDraw() {
        benchmarkRule.benchmarkToFirstPixel(fontSizeStyleProviderCaseFactory)
    }

    // --- Initial FontSize (TextStyle parameter overload) ---

    /** Measure initial compose time with fontSize using TextStyle parameter overload. */
    @Test
    fun firstFontSize_textStyle_compose() {
        benchmarkRule.benchmarkFirstCompose(fontSizeTextStyleCaseFactory)
    }

    /** Measure initial measure time with fontSize using TextStyle parameter overload. */
    @Test
    fun firstFontSize_textStyle_measure() {
        benchmarkRule.benchmarkFirstMeasure(fontSizeTextStyleCaseFactory)
    }

    /** Measure initial layout time with fontSize using TextStyle parameter overload. */
    @Test
    fun firstFontSize_textStyle_layout() {
        benchmarkRule.benchmarkFirstLayout(fontSizeTextStyleCaseFactory)
    }

    /** Measure initial draw time with fontSize using TextStyle parameter overload. */
    @Test
    fun firstFontSize_textStyle_draw() {
        benchmarkRule.benchmarkFirstDraw(fontSizeTextStyleCaseFactory)
    }

    /**
     * Measure initial compose + measure + layout + draw time with fontSize using TextStyle
     * parameter overload.
     */
    @Test
    fun firstFontSize_textStyle_composeMeasureLayoutDraw() {
        benchmarkRule.benchmarkToFirstPixel(fontSizeTextStyleCaseFactory)
    }

    // --- Toggle FontSize (styleProvider overload) ---

    /**
     * Measure recompose time when toggling fontSize using styleProvider overload. Recomposition
     * should be skipped because the state is read in the layout/draw node scope.
     */
    @Test
    fun toggleFontSize_styleProvider_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            fontSizeStyleProviderCaseFactory,
            assertOneRecomposition = false,
            requireRecomposition = false,
        )
    }

    /** Measure measure time when toggling fontSize using styleProvider overload. */
    @Test
    fun toggleFontSize_styleProvider_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            fontSizeStyleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /** Measure layout time when toggling fontSize using styleProvider overload. */
    @Test
    fun toggleFontSize_styleProvider_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            fontSizeStyleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /** Measure draw time when toggling fontSize using styleProvider overload. */
    @Test
    fun toggleFontSize_styleProvider_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            fontSizeStyleProviderCaseFactory,
            toggleCausesRecompose = false,
            assertOneRecomposition = false,
        )
    }

    /**
     * Measure recompose + measure + layout + draw time when toggling fontSize using styleProvider
     * overload.
     */
    @Test
    fun toggleFontSize_styleProvider_composeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            fontSizeStyleProviderCaseFactory,
            assertOneRecomposition = false,
            requireRecomposition = false,
        )
    }

    // --- Toggle FontSize (TextStyle parameter overload) ---

    /**
     * Measure recompose time when toggling fontSize using TextStyle parameter overload.
     * Recomposition occurs because the state is read during the composition phase.
     */
    @Test
    fun toggleFontSize_textStyle_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            fontSizeTextStyleCaseFactory,
            assertOneRecomposition = true,
            requireRecomposition = true,
        )
    }

    /** Measure measure time when toggling fontSize using TextStyle parameter overload. */
    @Test
    fun toggleFontSize_textStyle_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            fontSizeTextStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /** Measure layout time when toggling fontSize using TextStyle parameter overload. */
    @Test
    fun toggleFontSize_textStyle_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            fontSizeTextStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /** Measure draw time when toggling fontSize using TextStyle parameter overload. */
    @Test
    fun toggleFontSize_textStyle_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            fontSizeTextStyleCaseFactory,
            toggleCausesRecompose = true,
            assertOneRecomposition = true,
        )
    }

    /**
     * Measure recompose + measure + layout + draw time when toggling fontSize using TextStyle
     * parameter overload.
     */
    @Test
    fun toggleFontSize_textStyle_composeMeasureLayoutDraw() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayoutDraw(
            fontSizeTextStyleCaseFactory,
            assertOneRecomposition = true,
            requireRecomposition = true,
        )
    }
}

private val Background1 = Color.Red
private val Background2 = Color.Blue

private val FontSize1 = 14.sp
private val FontSize2 = 20.sp

private class BasicTextStyleProviderLayoutStyleTestCase(
    private val texts: List<String>,
    private val width: Dp,
    private val fontSize: TextUnit,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val currentBackground = mutableStateOf(Background1)

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
                setFontSize(this@BasicTextStyleProviderLayoutStyleTestCase.fontSize)
                setBackground(currentBackground.value)
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
        currentBackground.value =
            if (currentBackground.value == Background1) Background2 else Background1
    }
}

private class BasicTextTextStyleLayoutStyleTestCase(
    private val texts: List<String>,
    private val width: Dp,
    private val fontSize: TextUnit,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val currentBackground = mutableStateOf(Background1)

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
                    background = currentBackground.value,
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
        currentBackground.value =
            if (currentBackground.value == Background1) Background2 else Background1
    }
}

private class BasicTextStyleProviderFontSizeTestCase(
    private val texts: List<String>,
    private val width: Dp,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val currentFontSize = mutableStateOf(FontSize1)

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
                setFontSize(currentFontSize.value)
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
        currentFontSize.value = if (currentFontSize.value == FontSize1) FontSize2 else FontSize1
    }
}

private class BasicTextTextStyleFontSizeTestCase(
    private val texts: List<String>,
    private val width: Dp,
) : LayeredComposeTestCase(), ToggleableTestCase {
    private val currentFontSize = mutableStateOf(FontSize1)

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
            style = TextStyle(fontSize = currentFontSize.value),
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
        currentFontSize.value = if (currentFontSize.value == FontSize1) FontSize2 else FontSize1
    }
}
