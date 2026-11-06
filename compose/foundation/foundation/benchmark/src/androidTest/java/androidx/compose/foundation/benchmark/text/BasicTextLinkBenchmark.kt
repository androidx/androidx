/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasureLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.benchmark.TextBenchmarkTestRule
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class BasicTextLinkBenchmark(private val scenario: LinkScenario) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "scenario={0}")
        fun initParameters() =
            arrayOf(
                LinkScenario.SingleLineLink,
                LinkScenario.MultiLineLink,
                LinkScenario.HundredLinks,
                LinkScenario.StatelessLink,
            )
    }

    @get:Rule val textBenchmarkRule = TextBenchmarkTestRule()

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val width = textBenchmarkRule.widthDp.dp
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    private val caseFactory = {
        val texts =
            List(textBenchmarkRule.repeatTimes) {
                createAnnotatedString(scenario)
            }
        BasicTextLinkTestCase(texts, width, fontSize)
    }

    private val toggleCaseFactory = {
        val defaultTexts =
            List(textBenchmarkRule.repeatTimes) {
                createAnnotatedString(scenario)
            }
        val alternateTexts =
            List(textBenchmarkRule.repeatTimes) {
                createAnnotatedString(scenario, alternate = true)
            }
        BasicTextLinkToggleTestCase(defaultTexts, alternateTexts, width, fontSize)
    }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel(caseFactory)
    }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(caseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(caseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(caseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(caseFactory)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(caseFactory)
    }

    @Test
    fun toggleText_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            toggleCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun toggleText_measureLayout() {
        benchmarkRule.toggleStateBenchmarkMeasureLayout(
            toggleCaseFactory,
            assertOneRecomposition = false,
        )
    }

    @Test
    fun toggleText_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            toggleCaseFactory,
            assertOneRecomposition = false,
        )
    }
}

enum class LinkScenario {
    SingleLineLink,
    MultiLineLink,
    HundredLinks,
    StatelessLink,
}

private fun createAnnotatedString(
    scenario: LinkScenario,
    alternate: Boolean = false,
): AnnotatedString {
    val linkStyles =
        if (scenario == LinkScenario.StatelessLink) {
            TextLinkStyles(style = SpanStyle(color = if (alternate) Color.Red else Color.Blue))
        } else {
            TextLinkStyles(
                style = SpanStyle(color = if (alternate) Color.Red else Color.Blue),
                focusedStyle = SpanStyle(color = Color.Red),
                hoveredStyle = SpanStyle(color = Color.Green),
                pressedStyle = SpanStyle(color = Color.Magenta),
            )
        }
    val url = LinkAnnotation.Url("https://example.com", linkStyles)
    // The suffix doesn't contain spaces so that single-line links stay a single word and don't wrap
    val suffix = if (alternate) "_updated" else ""

    return buildAnnotatedString {
        when (scenario) {
            LinkScenario.SingleLineLink,
            LinkScenario.StatelessLink -> {
                append("Here is a short paragraph with a ")
                withLink(url) {
                    append("link$suffix")
                }
                append(" that fits on one line.")
            }
            LinkScenario.MultiLineLink -> {
                append("Here is a constrained-width paragraph containing ")
                withLink(url) {
                    append(
                        "one very long link$suffix that will wrap across multiple lines to test how the text layout and link measurement handle multi-line wrapping in compose text."
                    )
                }
                append(" And this is the end of the text.")
            }
            LinkScenario.HundredLinks -> {
                append("This text block reproduces the performance issue by having many links. ")
                for (i in 1..100) {
                    withLink(url) {
                        append("link$i$suffix")
                    }
                    append(" ")
                }
                append("End of text.")
            }
        }
    }
}

internal class BasicTextLinkTestCase(
    private val texts: List<AnnotatedString>,
    private val width: Dp,
    private val fontSize: TextUnit,
) : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        for (text in texts) {
            BasicText(
                text = text,
                style = TextStyle(fontSize = fontSize),
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        Column(modifier = Modifier.wrapContentSize(Alignment.Center).width(width)) {
            content()
        }
    }
}

internal class BasicTextLinkToggleTestCase(
    private val defaultTexts: List<AnnotatedString>,
    private val alternateTexts: List<AnnotatedString>,
    private val width: Dp,
    private val fontSize: TextUnit,
) : ComposeTestCase, ToggleableTestCase {

    private var isAlternate by mutableStateOf(false)

    @Composable
    override fun Content() {
        Column(modifier = Modifier.wrapContentSize(Alignment.Center).width(width)) {
            val texts = if (isAlternate) alternateTexts else defaultTexts
            for (text in texts) {
                BasicText(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                )
            }
        }
    }

    override fun toggleState() {
        isAlternate = !isAlternate
    }
}
