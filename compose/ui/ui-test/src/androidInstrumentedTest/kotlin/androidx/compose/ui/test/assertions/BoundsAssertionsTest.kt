/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.test.assertions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.WithMinimumTouchTargetSize
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.expectError
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getFirstLinkBounds
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkAnnotation.Url
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BoundsAssertionsTest {
    companion object {
        private const val tag = "box"
    }

    @get:Rule val rule = createComposeRule()

    private fun composeBox() {
        rule.setContent {
            Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
                Box(modifier = Modifier.padding(start = 50.dp, top = 100.dp)) {
                    Box(
                        modifier =
                            Modifier.testTag(tag)
                                .requiredSize(80.dp, 100.dp)
                                .background(color = Color.Black)
                    )
                }
            }
        }
    }

    @Composable
    private fun SmallBox(modifier: Modifier = Modifier, tag: String = BoundsAssertionsTest.tag) {
        Box(
            modifier =
                modifier.testTag(tag).requiredSize(10.dp, 10.dp).background(color = Color.Black)
        )
    }

    @Test
    fun assertSizeEquals() {
        composeBox()

        rule.onNodeWithTag(tag).assertWidthIsEqualTo(80.dp).assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun assertSizeAtLeast() {
        composeBox()

        rule
            .onNodeWithTag(tag)
            .assertWidthIsAtLeast(80.dp)
            .assertWidthIsAtLeast(79.dp)
            .assertHeightIsAtLeast(100.dp)
            .assertHeightIsAtLeast(99.dp)
    }

    @Test
    fun assertSizeEquals_fail() {
        composeBox()

        expectError<AssertionError> { rule.onNodeWithTag(tag).assertWidthIsEqualTo(70.dp) }

        expectError<AssertionError> { rule.onNodeWithTag(tag).assertHeightIsEqualTo(90.dp) }
    }

    @Test
    fun assertSizeAtLeast_fail() {
        composeBox()

        expectError<AssertionError> { rule.onNodeWithTag(tag).assertWidthIsAtLeast(81.dp) }

        expectError<AssertionError> { rule.onNodeWithTag(tag).assertHeightIsAtLeast(101.dp) }
    }

    @Test
    fun assertTouchSizeEquals() {
        rule.setContent {
            WithMinimumTouchTargetSize(DpSize(20.dp, 20.dp)) { SmallBox(Modifier.clickable {}) }
        }

        rule.onNodeWithTag(tag).assertTouchWidthIsEqualTo(20.dp).assertTouchHeightIsEqualTo(20.dp)
    }

    @Test
    fun assertTouchSizeEquals_fail() {
        rule.setContent {
            WithMinimumTouchTargetSize(DpSize(20.dp, 20.dp)) { SmallBox(Modifier.clickable {}) }
        }

        expectError<AssertionError> { rule.onNodeWithTag(tag).assertTouchWidthIsEqualTo(19.dp) }

        expectError<AssertionError> { rule.onNodeWithTag(tag).assertTouchHeightIsEqualTo(21.dp) }
    }

    @Test
    fun assertPosition() {
        composeBox()

        rule
            .onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = 50.dp, expectedTop = 100.dp)
            .assertLeftPositionInRootIsEqualTo(50.dp)
            .assertTopPositionInRootIsEqualTo(100.dp)
    }

    @Test
    fun assertPosition_fail() {
        composeBox()

        expectError<AssertionError> {
            rule
                .onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 51.dp, expectedTop = 101.dp)
        }

        expectError<AssertionError> {
            rule
                .onNodeWithTag(tag)
                .assertPositionInRootIsEqualTo(expectedLeft = 49.dp, expectedTop = 99.dp)
        }
    }

    private fun composeClippedBox() {
        rule.setContent {
            Box(
                modifier = Modifier.fillMaxSize().clipToBounds().wrapContentSize(Alignment.TopStart)
            ) {
                // Box is shifted 30dp to the left and 10dp to the top,
                // so it is clipped to a size of 50 x 90
                Box(
                    modifier =
                        Modifier.offset((-30).dp, (-10).dp)
                            .testTag(tag)
                            .requiredSize(80.dp, 100.dp)
                            .background(color = Color.Black)
                )
            }
        }
    }

    @Test
    fun assertClippedPosition() {
        composeClippedBox()

        // Node is clipped, but position should be unaffected
        rule
            .onNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(expectedLeft = (-30).dp, expectedTop = (-10).dp)
            .assertLeftPositionInRootIsEqualTo((-30).dp)
            .assertTopPositionInRootIsEqualTo((-10).dp)
    }

    @Test
    fun assertClippedSize() {
        composeClippedBox()

        // Node is clipped, but width and height should be unaffected
        rule.onNodeWithTag(tag).assertWidthIsEqualTo(80.dp).assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun getPosition_measuredNotPlaced() {
        // When we have a node that is measure but not placed
        getPositionTest { DoNotPlace { Box(Modifier.testTag(tag).requiredSize(10.dp)) } }
    }

    @Test
    fun assertSizeUsesLocalDensity() {
        rule.setContent {
            SmallBox(tag = "default-density")
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SmallBox(tag = "low-density")
            }
            CompositionLocalProvider(LocalDensity provides Density(4f, 1f)) {
                SmallBox(tag = "high-density")
            }
        }

        fun SemanticsNodeInteraction.verifySize() {
            assertWidthIsEqualTo(10.dp)
            assertHeightIsEqualTo(10.dp)
        }

        rule.onNodeWithTag("default-density").verifySize()
        rule.onNodeWithTag("low-density").verifySize()
        rule.onNodeWithTag("high-density").verifySize()
    }

    @Composable
    private fun TouchTargetTestContent(tag: String) {
        WithMinimumTouchTargetSize(DpSize(20.dp, 20.dp)) { SmallBox(Modifier.clickable {}, tag) }
    }

    @Test
    fun assertTouchSizeUsesLocalDensity() {
        rule.setContent {
            TouchTargetTestContent("default-density")
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                TouchTargetTestContent("low-density")
            }
            CompositionLocalProvider(LocalDensity provides Density(4f, 1f)) {
                TouchTargetTestContent("high-density")
            }
        }

        fun SemanticsNodeInteraction.verifyTouchSize() {
            assertTouchWidthIsEqualTo(20.dp)
            assertTouchHeightIsEqualTo(20.dp)
        }

        rule.onNodeWithTag("default-density").verifyTouchSize()
        rule.onNodeWithTag("low-density").verifyTouchSize()
        rule.onNodeWithTag("high-density").verifyTouchSize()
    }

    @Test
    fun assertBoundsInRootUsesLocalDensity() {
        rule.setContent {
            SmallBox(Modifier.padding(20.dp), "default-density")
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                SmallBox(Modifier.padding(20.dp), "low-density")
            }
            CompositionLocalProvider(LocalDensity provides Density(4f, 1f)) {
                SmallBox(Modifier.padding(20.dp), "high-density")
            }
        }

        fun SemanticsNodeInteraction.verifyBoundsInRoot() {
            getBoundsInRoot().let {
                it.left.assertIsEqualTo(20.dp)
                it.top.assertIsEqualTo(20.dp)
            }
        }

        rule.onNodeWithTag("default-density").verifyBoundsInRoot()
        rule.onNodeWithTag("low-density").verifyBoundsInRoot()
        rule.onNodeWithTag("high-density").verifyBoundsInRoot()
    }

    @Test
    fun assertAlignmentLinesUseLocalDensity() {
        rule.setContent {
            BoxWithAlignmentLine(Modifier.testTag("default-density"))
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                BoxWithAlignmentLine(Modifier.testTag("low-density"))
            }
            CompositionLocalProvider(LocalDensity provides Density(4f, 1f)) {
                BoxWithAlignmentLine(Modifier.testTag("high-density"))
            }
        }

        fun SemanticsNodeInteraction.verifyAlignmentLine() {
            getAlignmentLinePosition(TestLine).assertIsEqualTo(TestLinePosition)
        }

        rule.onNodeWithTag("default-density").verifyAlignmentLine()
        rule.onNodeWithTag("low-density").verifyAlignmentLine()
        rule.onNodeWithTag("high-density").verifyAlignmentLine()
    }

    @Test
    fun getPosition_notMeasuredNotPlaced() {
        // When we have a node that is not measure and not placed
        getPositionTest { DoNotMeasure { Box(Modifier.testTag(tag).requiredSize(10.dp)) } }
    }

    private fun getPositionTest(content: @Composable () -> Unit) {
        // When we have a node that is [not] measured and not placed
        rule.setContent(content)

        // Then querying for positional information gives unspecified values
        val node = rule.onNodeWithTag(tag)
        node.assertPositionInRootIsEqualTo(Dp.Unspecified, Dp.Unspecified)
        node.assertTopPositionInRootIsEqualTo(Dp.Unspecified)
        node.assertLeftPositionInRootIsEqualTo(Dp.Unspecified)
        node.getUnclippedBoundsInRoot().let {
            assertThat(it.left).isEqualTo(Dp.Unspecified)
            assertThat(it.top).isEqualTo(Dp.Unspecified)
            assertThat(it.right).isEqualTo(Dp.Unspecified)
            assertThat(it.bottom).isEqualTo(Dp.Unspecified)
        }

        fun notEqual(subject: String) = "Actual $subject is Dp.Unspecified, expected 1.0.dp \\(.*"

        expectError<AssertionError>(expectedMessage = notEqual("left")) {
            node.assertPositionInRootIsEqualTo(1.dp, 1.dp)
        }
        expectError<AssertionError>(expectedMessage = notEqual("top")) {
            node.assertTopPositionInRootIsEqualTo(1.dp)
        }
        expectError<AssertionError>(expectedMessage = notEqual("left")) {
            node.assertLeftPositionInRootIsEqualTo(1.dp)
        }
    }

    @Test
    fun getSize_measuredNotPlaced() {
        // When we have a node that is measure but not placed
        getSizeTest { DoNotPlace { Box(Modifier.testTag(tag).requiredSize(10.dp)) } }
    }

    @Test
    fun getSize_notMeasuredNotPlaced() {
        // When we have a node that is not measure and not placed
        getSizeTest { DoNotMeasure { Box(Modifier.testTag(tag).requiredSize(10.dp)) } }
    }

    private fun getSizeTest(content: @Composable () -> Unit) {
        // When we have a node that is [not] measured and not placed
        rule.setContent(content)

        // Then querying for size information gives real or unspecified values
        val node = rule.onNodeWithTag(tag)
        node.assertWidthIsEqualTo(Dp.Unspecified)
        node.assertHeightIsEqualTo(Dp.Unspecified)
        node.assertWidthIsAtLeast(Dp.Unspecified)
        node.assertHeightIsAtLeast(Dp.Unspecified)

        fun notEqual(subject: String) = "Actual $subject is Dp.Unspecified, expected 10.0.dp \\(.*"
        fun notAtLeast(subject: String) =
            "Actual $subject is Dp.Unspecified, expected at least 10.0.dp \\(.*"

        expectError<AssertionError>(expectedMessage = notEqual("width")) {
            node.assertWidthIsEqualTo(10.dp)
        }
        expectError<AssertionError>(expectedMessage = notEqual("height")) {
            node.assertHeightIsEqualTo(10.dp)
        }
        expectError<AssertionError>(expectedMessage = notAtLeast("width")) {
            node.assertWidthIsAtLeast(10.dp)
        }
        expectError<AssertionError>(expectedMessage = notAtLeast("height")) {
            node.assertHeightIsAtLeast(10.dp)
        }
    }

    @Test
    fun getAlignmentLine_measuredNotPlaced() {
        // When we have a node with an alignment line that is measured but not placed
        getAlignmentLineTest(expectedPosition = TestLinePosition) {
            DoNotPlace { BoxWithAlignmentLine(Modifier.testTag(tag)) }
        }
    }

    @Test
    fun getAlignmentLine_notMeasuredNotPlaced() {
        // When we have a node with an alignment line that is not measured and not placed
        getAlignmentLineTest(expectedPosition = Dp.Unspecified) {
            DoNotMeasure { BoxWithAlignmentLine(Modifier.testTag(tag)) }
        }
    }

    private fun getAlignmentLineTest(expectedPosition: Dp, content: @Composable () -> Unit) {
        // When we have a node with an alignment line that is [not] measured and not placed
        rule.setContent(content)

        // Then we can still query the alignment line
        rule.onNodeWithTag(tag).getAlignmentLinePosition(TestLine).assertIsEqualTo(expectedPosition)
    }

    @Composable
    private fun DoNotMeasure(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { _, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {}
        }
    }

    @Composable
    private fun DoNotPlace(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Layout(content, modifier) { measurables, constraints ->
            val placeable = measurables.map { it.measure(constraints) }
            layout(placeable.maxOf { it.width }, placeable.maxOf { it.height }) {}
        }
    }

    private val TestLinePosition = 10.dp
    private val TestLine = HorizontalAlignmentLine(::max)

    @Composable
    private fun BoxWithAlignmentLine(modifier: Modifier, linePosition: Dp = TestLinePosition) {
        val linePositionPx = with(LocalDensity.current) { linePosition.roundToPx() }
        Layout({}, modifier) { _, constraints ->
            layout(
                constraints.maxWidth,
                constraints.maxHeight,
                mapOf(TestLine to linePositionPx)
            ) {}
        }
    }

    @Test
    fun getFirstLinkBounds_multipleMatches_handleSecond() {
        lateinit var textLayoutResult: TextLayoutResult
        var count = -1
        rule.setContent {
            BasicText(
                buildAnnotatedString {
                    append("a")
                    withLink(LinkAnnotation.Url("url1")) { append("link1") }
                    withLink(LinkAnnotation.Url("url2")) { append("link2") }
                    withLink(LinkAnnotation.Clickable("tag1") {}) { append("link3") }
                },
                onTextLayout = { textLayoutResult = it },
            )
        }

        val textNode = rule.onNodeWithText("a", substring = true)
        assertThat(
                textNode.getFirstLinkBounds {
                    count++
                    count == 1
                }
            )
            .isEqualTo(
                Rect(
                    textLayoutResult.getBoundingBox(6).left,
                    textLayoutResult.getBoundingBox(6).top,
                    textLayoutResult.getBoundingBox(10).right,
                    textLayoutResult.getBoundingBox(10).bottom
                )
            )
    }

    @Test
    fun getFirstLinkBounds_forSpecificUrl_multipleMatches_firstReturn() {
        lateinit var textLayoutResult: TextLayoutResult
        rule.setContent {
            BasicText(
                buildAnnotatedString {
                    append("a")
                    withLink(LinkAnnotation.Url("url1")) { append("link1") }
                    withLink(LinkAnnotation.Url("url2")) { append("link2") }
                    withLink(LinkAnnotation.Url("url1") {}) { append("link3") }
                    withLink(LinkAnnotation.Clickable("tag2") {}) { append("link4") }
                },
                onTextLayout = { textLayoutResult = it },
            )
        }

        val textNode = rule.onNodeWithText("a", substring = true)
        assertThat(textNode.getFirstLinkBounds { (it.item as? LinkAnnotation.Url)?.url == "url1" })
            .isEqualTo(
                Rect(
                    textLayoutResult.getBoundingBox(1).left,
                    textLayoutResult.getBoundingBox(1).top,
                    textLayoutResult.getBoundingBox(5).right,
                    textLayoutResult.getBoundingBox(5).bottom
                )
            )
    }

    @Test
    fun getFirstLinkBounds_forAllClickable_multipleMatches_returnsFirst() {
        lateinit var textLayoutResult: TextLayoutResult
        rule.setContent {
            BasicText(
                buildAnnotatedString {
                    append("a")
                    withLink(LinkAnnotation.Clickable("tag1") {}) { append("link1") }
                    withLink(LinkAnnotation.Url("url2")) { append("link2") }
                    withLink(LinkAnnotation.Clickable("tag2") {}) { append("link3") }
                },
                onTextLayout = { textLayoutResult = it },
            )
        }

        val textNode = rule.onNodeWithText("a", substring = true)
        assertThat(textNode.getFirstLinkBounds { it.item is LinkAnnotation.Clickable })
            .isEqualTo(
                Rect(
                    textLayoutResult.getBoundingBox(1).left,
                    textLayoutResult.getBoundingBox(1).top,
                    textLayoutResult.getBoundingBox(5).right,
                    textLayoutResult.getBoundingBox(5).bottom
                )
            )
    }

    @Test
    fun getFirstLinkBounds_ZeroMatches() {
        rule.setContent { BasicText(AnnotatedString("a")) }
        assertThat(rule.onNodeWithText("a").getFirstLinkBounds { true }).isNull()
    }

    @Test
    fun getFirstLinkBounds_explicitZeroMatches() {
        rule.setContent {
            BasicText(buildAnnotatedString { withLink(LinkAnnotation.Url("url")) { append("a") } })
        }
        assertThat(rule.onNodeWithText("a").getFirstLinkBounds { false }).isNull()
    }

    @Test
    fun getFirstLinkBounds_relativeToTextNode() {
        lateinit var textLayoutResult: TextLayoutResult
        val offset = with(rule.density) { 17f.toDp() }
        rule.setContent {
            BasicText(
                buildAnnotatedString { withLink(Url("url")) { append("a") } },
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.offset(offset, offset)
            )
        }

        val textNode = rule.onNodeWithText("a")
        assertThat(textNode.getBoundsInRoot().topLeft).isEqualTo(DpOffset(offset, offset))
        assertThat(textLayoutResult.getBoundingBox(0).topLeft).isEqualTo(Offset(0f, 0f))
        assertThat(textNode.getFirstLinkBounds { true }!!.topLeft).isEqualTo(Offset(0f, 0f))
    }

    @Test(expected = AssertionError::class)
    fun getFirstLinkBounds_expectMatchingTextForTextLayoutResult() {
        val TAG = "text node"
        rule.setContent {
            BasicText(
                buildAnnotatedString { withLink(LinkAnnotation.Url("url")) { append("link") } },
                modifier = Modifier.testTag(TAG).semantics { text = AnnotatedString("other text") }
            )
        }
        rule.onNodeWithTag(TAG).getFirstLinkBounds { true }
    }

    @Test
    fun getFirstLinkBounds_mergedNode_expectMatchingTextForTextLayoutResult() {
        lateinit var textLayoutResult: TextLayoutResult
        val TAG = "box node"
        rule.setContent {
            Box(Modifier.semantics(true) {}.testTag(TAG)) {
                Box(Modifier.size(10.dp).semantics { text = AnnotatedString("no link text") })
                BasicText(
                    buildAnnotatedString { withLink(LinkAnnotation.Url("url")) { append("a") } },
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }

        val textNode = rule.onNodeWithTag(TAG)
        assertThat(textNode.getFirstLinkBounds { true })
            .isEqualTo(textLayoutResult.getBoundingBox(0))
    }

    @Test
    fun getFirstLinkBounds_link_inMiddle_spansMultipleLines_returnsFirstLineBounds() {
        lateinit var textLayoutResult: TextLayoutResult

        rule.setContent {
            BasicText(
                buildAnnotatedString {
                    append("a")
                    withLink(Url("url")) { append("bc") }
                },
                modifier = Modifier.width(20.dp),
                style = TextStyle(fontSize = with(rule.density) { 20.dp.toSp() }),
                onTextLayout = { textLayoutResult = it }
            )
        }

        val textNode = rule.onNodeWithText("abc")
        val expected =
            Rect(
                textLayoutResult.getBoundingBox(1).left,
                textLayoutResult.getBoundingBox(1).top,
                textLayoutResult.getBoundingBox(1).right,
                textLayoutResult.getBoundingBox(1).bottom
            )
        assertThat(textLayoutResult.lineCount).isEqualTo(3)
        assertThat(textNode.getFirstLinkBounds { true }).isEqualTo(expected)
    }

    @Test
    fun getFirstLinkBounds_linkSpansMultipleLines_returnsFirstLineBounds() {
        lateinit var textLayoutResult: TextLayoutResult
        rule.setContent {
            BasicText(
                buildAnnotatedString { withLink(Url("url")) { append("abc") } },
                modifier = Modifier.width(30.dp),
                style = TextStyle(fontSize = with(rule.density) { 20.dp.toSp() }),
                onTextLayout = { textLayoutResult = it }
            )
        }

        val textNode = rule.onNodeWithText("abc")
        val expected =
            Rect(
                textLayoutResult.getBoundingBox(0).left,
                textLayoutResult.getBoundingBox(0).top,
                textLayoutResult.getBoundingBox(1).right,
                textLayoutResult.getBoundingBox(1).bottom
            )
        assertThat(textLayoutResult.lineCount).isEqualTo(2)
        assertThat(textNode.getFirstLinkBounds { true }).isEqualTo(expected)
    }

    @Test
    fun getFirstLinkBounds_rtlText() {
        lateinit var textLayoutResult: TextLayoutResult
        rule.setContent {
            BasicText(
                buildAnnotatedString {
                    append("\u05D1\u05D2\u05D3\u05D0\u05D1")
                    addLink(Url("url"), 1, 4)
                },
                modifier = Modifier.testTag("tag"),
                onTextLayout = { textLayoutResult = it }
            )
        }

        val textNode = rule.onNodeWithTag("tag")
        val expectedBounds =
            textLayoutResult.run {
                val right = getBoundingBox(1).right
                val top = getBoundingBox(1).top
                val left = getBoundingBox(3).left
                val bottom = getBoundingBox(3).bottom
                Rect(left, top, right, bottom)
            }
        assertThat(textNode.getFirstLinkBounds { true }).isEqualTo(expectedBounds)
    }

    private val DpRect.topLeft: DpOffset
        get() = DpOffset(left, top)
}
