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

package androidx.text.vertical.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.text.SpannableString
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.TextOrientation
import androidx.text.vertical.VerticalTextLayout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.ceil
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class VerticalTextTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun exposesSemantics_correctly() {
        val text = SpannableString("Hello Vertical")
        val style = VerticalTextStyle(fontSize = 30.sp)

        rule.setContent { VerticalText(text = text, style = style) }

        // Modern Compose testing relies on finding nodes by their semantic text
        rule.onNodeWithText("Hello Vertical").assertExists().assertIsDisplayed()
    }

    @Test
    fun annotatedString_preservesSpansAndAnnotationsInSemantics() {
        val annotated = buildAnnotatedString {
            pushStringAnnotation(tag = "ruby", annotation = "かんじ")
            withStyle(SpanStyle(color = Color.Red)) { append("漢字") }
            pop()
        }

        rule.setContent {
            VerticalText(text = annotated, style = VerticalTextStyle(fontSize = 30.sp))
        }

        val node = rule.onNodeWithText("漢字").assertExists().fetchSemanticsNode()
        val semanticsText = node.config[SemanticsProperties.Text].single()
        assertThat(semanticsText).isEqualTo(annotated)
        assertThat(semanticsText.spanStyles).isEqualTo(annotated.spanStyles)
        assertThat(semanticsText.getStringAnnotations(0, semanticsText.length))
            .isEqualTo(annotated.getStringAnnotations(0, annotated.length))
    }

    @Test
    fun heightInMax_wrapsToContentHeight() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        var measuredSize = IntSize.Zero
        var maxHeightPx = 0
        var expectedContentHeight = 0f

        rule.setContent {
            val density = LocalDensity.current
            maxHeightPx = with(density) { 400.dp.roundToPx() }
            expectedContentHeight =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = "あいう",
                        height = maxHeightPx,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
                    .contentHeight
            VerticalText(
                text = "あいう",
                modifier = Modifier.maxHeight(maxHeightPx).onSizeChanged { measuredSize = it },
                style = style,
            )
        }

        rule.runOnIdle {
            assertThat(measuredSize.height).isGreaterThan(0)
            assertThat(measuredSize.height).isEqualTo(ceil(expectedContentHeight).toInt())
            assertThat(measuredSize.height).isLessThan(maxHeightPx)
        }
    }

    @Test
    fun heightInMin_coercesToMinHeight() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        var measuredSize = IntSize.Zero
        var minHeightPx = 0
        var expectedContentHeight = 0f

        rule.setContent {
            val density = LocalDensity.current
            minHeightPx = with(density) { 200.dp.roundToPx() }
            val maxHeightPx = with(density) { 400.dp.roundToPx() }
            expectedContentHeight =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = "あ",
                        height = maxHeightPx,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
                    .contentHeight
            VerticalText(
                text = "あ",
                modifier =
                    Modifier.minMaxHeight(minHeightPx, maxHeightPx).onSizeChanged {
                        measuredSize = it
                    },
                style = style,
            )
        }

        rule.runOnIdle {
            assertThat(ceil(expectedContentHeight).toInt()).isLessThan(minHeightPx)
            assertThat(measuredSize.height).isEqualTo(minHeightPx)
        }
    }

    @Test
    fun unboundedHeight_doesNotCrash() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        var childSize = IntSize.Zero
        var expectedHeight = 0

        rule.setContent {
            val density = LocalDensity.current
            expectedHeight =
                ceil(
                        VerticalTextLayoutCache()
                            .getLayout(
                                text = "あいう",
                                height = Constraints.Infinity,
                                orientation = TextOrientation.Mixed,
                                style = style,
                                typeface = Typeface.DEFAULT,
                                density = density,
                            )
                            .contentHeight
                    )
                    .toInt()
            Layout(content = { VerticalText(text = "あいう", style = style) }) {
                measurables,
                constraints ->
                val placeable =
                    measurables
                        .single()
                        .measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                childSize = IntSize(placeable.width, placeable.height)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(childSize.height).isGreaterThan(0)
            assertThat(childSize.height).isEqualTo(expectedHeight)
        }
    }

    @Test
    fun fixedHeight_keepsMaxHeight() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        var measuredSize = IntSize.Zero
        var fixedHeightPx = 0

        rule.setContent {
            val density = LocalDensity.current
            fixedHeightPx = with(density) { 200.dp.roundToPx() }
            VerticalText(
                text = "あいう",
                modifier = Modifier.fixedHeight(fixedHeightPx).onSizeChanged { measuredSize = it },
                style = style,
            )
        }

        rule.runOnIdle { assertThat(measuredSize.height).isEqualTo(fixedHeightPx) }
    }

    @Test
    fun clip_doesNotCutLastGlyph() {
        val style = VerticalTextStyle(color = Color.Black, fontSize = 20.sp)
        var measuredSize = IntSize.Zero
        var maxHeightPx = 0
        var expectedLayout: VerticalTextLayout? = null

        rule.setContent {
            val density = LocalDensity.current
            maxHeightPx = with(density) { 400.dp.roundToPx() }
            expectedLayout =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = "あいう",
                        height = maxHeightPx,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
            VerticalText(
                text = "あいう",
                modifier = Modifier.maxHeight(maxHeightPx).onSizeChanged { measuredSize = it },
                style = style,
                overflow = TextOverflow.Clip,
            )
        }

        rule.runOnIdle {
            val layout = checkNotNull(expectedLayout)
            val expectedContentHeight = layout.contentHeight
            assertThat(measuredSize.height).isEqualTo(ceil(expectedContentHeight).toInt())
            assertThat(measuredSize.height.toFloat()).isAtLeast(expectedContentHeight)
            assertThat(measuredSize.height).isLessThan(maxHeightPx)
            val fullPixels = layout.countColorPixels(Color.Black.toArgb(), maxHeightPx)
            val wrappedPixels = layout.countColorPixels(Color.Black.toArgb(), measuredSize.height)
            assertThat(wrappedPixels).isGreaterThan(0)
            assertThat(wrappedPixels).isEqualTo(fullPixels)
        }
    }

    @Test
    fun multiColumnMixedWidth_wrapsToTotalLayoutWidth() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        val text =
            SpannableString("あ\nい").apply {
                setSpan(
                    EmphasisSpan(EmphasisStyle.Dot, isFilled = true, AnnotationPosition.Before),
                    0,
                    1,
                    SpannableString.SPAN_INCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    EmphasisSpan(EmphasisStyle.Dot, isFilled = true, AnnotationPosition.After),
                    2,
                    3,
                    SpannableString.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }
        var measuredSize = IntSize.Zero
        var expectedWidthPx = 0
        var uniformWidthPx = 0

        rule.setContent {
            val density = LocalDensity.current
            val maxHeightPx = with(density) { 400.dp.roundToPx() }
            val vtl =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = text,
                        height = maxHeightPx,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
            // Column 0 (Before): [-0.5em, +1.0em] = 1.5em; Column 1 (After): [-1.0em, +0.5em] =
            // 1.5em; sum = 3.0em, whereas uniform columnWidth * lineCount = 2.0em * 2 = 4.0em.
            expectedWidthPx = ceil(3f * with(density) { 20.sp.toPx() }).toInt()
            uniformWidthPx = ceil(vtl.columnWidth * vtl.lineCount).toInt()
            VerticalText(
                text = text,
                modifier = Modifier.maxHeight(maxHeightPx).onSizeChanged { measuredSize = it },
                style = style,
            )
        }

        rule.runOnIdle {
            assertThat(measuredSize.width).isEqualTo(expectedWidthPx)
            assertThat(measuredSize.width).isLessThan(uniformWidthPx)
        }
    }

    @Test
    fun maxColumns_mixedWidth_sizesToIncludedColumnsOnly() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        val text =
            SpannableString("あ\nい").apply {
                setSpan(
                    EmphasisSpan(EmphasisStyle.Dot, isFilled = true, AnnotationPosition.Before),
                    0,
                    1,
                    SpannableString.SPAN_INCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    EmphasisSpan(EmphasisStyle.Dot, isFilled = true, AnnotationPosition.After),
                    2,
                    3,
                    SpannableString.SPAN_INCLUSIVE_EXCLUSIVE,
                )
            }
        var measuredSize = IntSize.Zero
        var expectedSingleColumnWidthPx = 0
        var uniformColumnWidthPx = 0

        rule.setContent {
            val density = LocalDensity.current
            val maxHeightPx = with(density) { 400.dp.roundToPx() }
            val vtl =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = text,
                        height = maxHeightPx,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
            // Column 0 (Before) is 1.5em wide, whereas bounding columnWidth across both columns is
            // 2.0em. With maxColumns = 1, desiredWidth must equal column 0's 1.5em width so column
            // 1 does not leak into the clipped bounds.
            expectedSingleColumnWidthPx = ceil(1.5f * with(density) { 20.sp.toPx() }).toInt()
            uniformColumnWidthPx = ceil(vtl.columnWidth).toInt()
            VerticalText(
                text = text,
                modifier = Modifier.maxHeight(maxHeightPx).onSizeChanged { measuredSize = it },
                style = style,
                overflow = TextOverflow.Clip,
                maxColumns = 1,
            )
        }

        rule.runOnIdle {
            assertThat(measuredSize.width).isEqualTo(expectedSingleColumnWidthPx)
            assertThat(measuredSize.width).isLessThan(uniformColumnWidthPx)
        }
    }

    @Test
    fun maxColumns_tallerHiddenColumn_measuresDrawnColumnHeight() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        val text = "あ\nあいうえおかきくけこ"
        var measuredSize = IntSize.Zero
        var firstColumnHeightPx = 0
        var fullContentHeightPx = 0

        rule.setContent {
            val density = LocalDensity.current
            val vtl =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = text,
                        height = Constraints.Infinity,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
            firstColumnHeightPx = ceil(vtl.getLineHeight(0)).toInt()
            fullContentHeightPx = ceil(vtl.contentHeight).toInt()
            VerticalText(
                text = text,
                modifier = Modifier.onSizeChanged { measuredSize = it },
                style = style,
                maxColumns = 1,
            )
        }

        rule.runOnIdle {
            assertThat(measuredSize.height).isEqualTo(firstColumnHeightPx)
            assertThat(measuredSize.height).isLessThan(fullContentHeightPx)
        }
    }

    @Test
    fun zeroColumns_measuresZeroSize() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        var measuredSize = IntSize(-1, -1)

        rule.setContent {
            VerticalText(
                text = "あいうえお",
                modifier = Modifier.onSizeChanged { measuredSize = it },
                style = style,
                maxColumns = 0,
                minColumns = 0,
            )
        }

        rule.runOnIdle { assertThat(measuredSize).isEqualTo(IntSize.Zero) }
    }

    @Test
    fun emptyText_reservesMinColumnsWidth() {
        val style = VerticalTextStyle(fontSize = 20.sp)
        var oneColumnSize = IntSize(-1, -1)
        var threeColumnSize = IntSize(-1, -1)
        var zeroColumnSize = IntSize(-1, -1)
        var annotatedOneColumnSize = IntSize(-1, -1)
        var textSizePx = 0f

        rule.setContent {
            textSizePx = with(LocalDensity.current) { 20.sp.toPx() }
            VerticalText(
                text = "",
                modifier = Modifier.onSizeChanged { oneColumnSize = it },
                style = style,
                minColumns = 1,
            )
            VerticalText(
                text = "",
                modifier = Modifier.onSizeChanged { threeColumnSize = it },
                style = style,
                minColumns = 3,
            )
            VerticalText(
                text = "",
                modifier = Modifier.onSizeChanged { zeroColumnSize = it },
                style = style,
                maxColumns = 0,
                minColumns = 0,
            )
            VerticalText(
                text = AnnotatedString(""),
                modifier = Modifier.onSizeChanged { annotatedOneColumnSize = it },
                style = style,
                minColumns = 1,
            )
        }

        rule.runOnIdle {
            assertThat(oneColumnSize.width).isEqualTo(ceil(textSizePx).toInt())
            assertThat(threeColumnSize.width).isEqualTo(ceil(3 * textSizePx).toInt())
            assertThat(zeroColumnSize.width).isEqualTo(0)
            assertThat(annotatedOneColumnSize.width).isEqualTo(ceil(textSizePx).toInt())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun maxColumns_withOverflowVisible_doesNotDrawExtraColumns() {
        val style = VerticalTextStyle(color = Color.Black, fontSize = 20.sp)
        val text = "あ\nい"
        var columnWidthPx = 0
        var outerSizePx = 0

        rule.setContent {
            columnWidthPx = columnWidthPx(text, style, LocalDensity.current)
            outerSizePx = 8 * columnWidthPx
            WhiteOuterBox(outerSizePx) {
                VerticalText(
                    text = text,
                    style = style,
                    overflow = TextOverflow.Visible,
                    maxColumns = 1,
                )
            }
        }

        // The node is one column wide and sits at the right edge of the outer box. Column 1 would
        // land in [outer - 2 * columnWidth, outer - columnWidth), left of the node.
        val bitmap = rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap()
        assertThat(bitmap.countNonWhite(outerSizePx - columnWidthPx until outerSizePx))
            .isGreaterThan(0)
        assertThat(bitmap.countNonWhite(0 until outerSizePx - columnWidthPx - 2)).isEqualTo(0)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun maxColumns_withWideMinWidthConstraint_doesNotDrawExtraColumns() {
        val style = VerticalTextStyle(color = Color.Black, fontSize = 20.sp)
        val text = "あ\nい"
        var columnWidthPx = 0
        var outerSizePx = 0

        rule.setContent {
            columnWidthPx = columnWidthPx(text, style, LocalDensity.current)
            outerSizePx = 8 * columnWidthPx
            WhiteOuterBox(outerSizePx) {
                VerticalText(
                    text = text,
                    modifier = Modifier.fixedSize(4 * columnWidthPx, 3 * columnWidthPx),
                    style = style,
                    overflow = TextOverflow.Clip,
                    maxColumns = 1,
                )
            }
        }

        // The node is four columns wide, so column 1 would land inside its bounds where clipping
        // cannot hide it.
        val bitmap = rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap()
        assertThat(bitmap.countNonWhite(outerSizePx - columnWidthPx until outerSizePx))
            .isGreaterThan(0)
        assertThat(
                bitmap.countNonWhite(
                    outerSizePx - 4 * columnWidthPx until outerSizePx - columnWidthPx - 2
                )
            )
            .isEqualTo(0)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun overflowEllipsis_clipsContent() {
        val style = VerticalTextStyle(color = Color.Black, fontSize = 20.sp)
        val text = "あいうえおかきくけこ"
        var columnWidthPx = 0
        var outerSizePx = 0
        var lineCount = 0

        rule.setContent {
            val density = LocalDensity.current
            columnWidthPx = columnWidthPx(text, style, density)
            val nodeHeightPx = ceil(2.5f * columnWidthPx).toInt()
            lineCount =
                VerticalTextLayoutCache()
                    .getLayout(
                        text = text,
                        height = nodeHeightPx,
                        orientation = TextOrientation.Mixed,
                        style = style,
                        typeface = Typeface.DEFAULT,
                        density = density,
                    )
                    .lineCount
            outerSizePx = 8 * columnWidthPx
            WhiteOuterBox(outerSizePx) {
                VerticalText(
                    text = text,
                    modifier = Modifier.fixedSize(2 * columnWidthPx, nodeHeightPx),
                    style = style,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Two glyphs fit per column, so the text needs five columns but the node holds two.
        assertThat(lineCount).isGreaterThan(2)
        val bitmap = rule.onNodeWithTag("outer").captureToImage().asAndroidBitmap()
        assertThat(bitmap.countNonWhite(outerSizePx - 2 * columnWidthPx until outerSizePx))
            .isGreaterThan(0)
        assertThat(bitmap.countNonWhite(0 until outerSizePx - 2 * columnWidthPx - 2)).isEqualTo(0)
    }
}

/** Returns the bounding column width in whole pixels for [text] laid out with [style]. */
private fun columnWidthPx(text: CharSequence, style: VerticalTextStyle, density: Density): Int =
    ceil(
            VerticalTextLayoutCache()
                .getLayout(
                    text = text,
                    height = Constraints.Infinity,
                    orientation = TextOrientation.Mixed,
                    style = style,
                    typeface = Typeface.DEFAULT,
                    density = density,
                )
                .columnWidth
        )
        .toInt()

/**
 * Composes [content] as the single child of a white box tagged `"outer"`.
 *
 * Centers the box in the window so system bars do not overlap the captured node, and places the
 * child against the right edge of the box so leftward overflow stays inside the captured bounds.
 */
@Composable
private fun WhiteOuterBox(outerSizePx: Int, content: @Composable () -> Unit) {
    Layout(
        content = {
            Layout(
                modifier = Modifier.drawBehind { drawRect(Color.White) }.testTag("outer"),
                content = content,
            ) { measurables, _ ->
                val placeable =
                    measurables
                        .single()
                        .measure(Constraints(maxWidth = outerSizePx, maxHeight = outerSizePx))
                layout(outerSizePx, outerSizePx) {
                    placeable.place(outerSizePx - placeable.width, 0)
                }
            }
        }
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(Constraints())
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(
                (constraints.maxWidth - placeable.width) / 2,
                (constraints.maxHeight - placeable.height) / 2,
            )
        }
    }
}

/** Counts the pixels in [xRange] across the full height that are not white. */
private fun Bitmap.countNonWhite(xRange: IntRange): Int {
    val white = Color.White.toArgb()
    var count = 0
    for (y in 0 until height) {
        for (x in xRange) {
            if (getPixel(x, y) != white) count++
        }
    }
    return count
}

private fun Modifier.fixedSize(widthPx: Int, heightPx: Int): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun Modifier.maxHeight(maxHeightPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = maxHeightPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

private fun Modifier.minMaxHeight(minHeightPx: Int, maxHeightPx: Int): Modifier =
    layout { measurable, constraints ->
        val placeable =
            measurable.measure(constraints.copy(minHeight = minHeightPx, maxHeight = maxHeightPx))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

private fun Modifier.fixedHeight(heightPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = heightPx, maxHeight = heightPx))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

@MediumTest
@RunWith(AndroidJUnit4::class)
class VerticalTextLayoutCacheTest {

    @Test
    fun cache_returnsSameInstance_whenInputsAreSame() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)

        assertSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenDensityChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT

        val layout1 =
            cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, Density(1f))
        val layout2 =
            cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, Density(2f))

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenStyleChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style1 = VerticalTextStyle(fontSize = 12.sp)
        val style2 = VerticalTextStyle(fontSize = 14.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style1, typeface, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Mixed, style2, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenTextChanges() {
        val cache = VerticalTextLayoutCache()
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout("Hello", 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout("World", 100, TextOrientation.Mixed, style, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenHeightChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout(text, 200, TextOrientation.Mixed, style, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenOrientationChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Upright, style, typeface, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_returnsNewInstance_whenTypefaceChanges() {
        val cache = VerticalTextLayoutCache()
        val text = "Hello"
        val style = VerticalTextStyle(fontSize = 12.sp)
        val typeface1 = Typeface.DEFAULT
        val typeface2 = Typeface.SERIF
        val density = Density(1f)

        val layout1 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface1, density)
        val layout2 = cache.getLayout(text, 100, TextOrientation.Mixed, style, typeface2, density)

        assertNotSame(layout1, layout2)
    }

    @Test
    fun cache_clearsBackground_whenStyleBackgroundBecomesUnspecified() {
        val cache = VerticalTextLayoutCache()
        val text = SpannableString("Hello")
        val withBg = VerticalTextStyle(fontSize = 24.sp, background = Color.Yellow)
        val withoutBg = VerticalTextStyle(fontSize = 24.sp, background = Color.Unspecified)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)
        val layoutHeight = 200

        val layout1 =
            cache.getLayout(text, layoutHeight, TextOrientation.Mixed, withBg, typeface, density)
        assertWithMessage("layout1 must draw the yellow background")
            .that(layout1.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isGreaterThan(0)

        val layout2 =
            cache.getLayout(text, layoutHeight, TextOrientation.Mixed, withoutBg, typeface, density)
        assertWithMessage("layout2 must not draw a yellow background")
            .that(layout2.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isEqualTo(0)
        assertWithMessage("layout2 must draw black text")
            .that(layout2.countColorPixels(Color.Black.toArgb(), layoutHeight))
            .isGreaterThan(0)
    }

    @Test
    fun cache_doesNotMutatePreviousLayoutPaintOnMiss() {
        val cache = VerticalTextLayoutCache()
        val text = SpannableString("Hello")
        val withBg = VerticalTextStyle(fontSize = 24.sp, background = Color.Yellow)
        val withoutBg = VerticalTextStyle(fontSize = 24.sp, background = Color.Unspecified)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)
        val layoutHeight = 200

        val layout1 =
            cache.getLayout(text, layoutHeight, TextOrientation.Mixed, withBg, typeface, density)
        val layout2 =
            cache.getLayout(text, layoutHeight, TextOrientation.Mixed, withoutBg, typeface, density)

        assertNotSame(layout1, layout2)
        assertWithMessage("layout1 must still draw the yellow background after a cache miss")
            .that(layout1.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isGreaterThan(0)
        assertWithMessage("layout2 must not draw a yellow background")
            .that(layout2.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isEqualTo(0)
    }

    @Test
    fun cache_convertsAnnotatedStringToSpanned_andInvalidatesWhenSpanStylesChange() {
        val cache = VerticalTextLayoutCache()
        val styledText = buildAnnotatedString {
            withStyle(SpanStyle(background = Color.Yellow)) { append("Hello") }
        }
        val plainText = AnnotatedString("Hello")
        val style = VerticalTextStyle(fontSize = 24.sp)
        val typeface = Typeface.DEFAULT
        val density = Density(1f)
        val layoutHeight = 200

        val layout1 =
            cache.getLayout(
                styledText,
                layoutHeight,
                TextOrientation.Mixed,
                style,
                typeface,
                density,
            )
        val layout1Cached =
            cache.getLayout(
                styledText,
                layoutHeight,
                TextOrientation.Mixed,
                style,
                typeface,
                density,
            )
        assertSame(layout1, layout1Cached)
        assertWithMessage("AnnotatedString SpanStyle background must be rendered")
            .that(layout1.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isGreaterThan(0)

        val layout2 =
            cache.getLayout(
                plainText,
                layoutHeight,
                TextOrientation.Mixed,
                style,
                typeface,
                density,
            )
        assertNotSame(layout1, layout2)
        assertWithMessage("Plain AnnotatedString must not draw yellow background")
            .that(layout2.countColorPixels(Color.Yellow.toArgb(), layoutHeight))
            .isEqualTo(0)
    }
}

/** Draws this layout into a new bitmap and returns the number of pixels that are [targetColor]. */
private fun VerticalTextLayout.countColorPixels(targetColor: Int, height: Int): Int {
    val bitmapWidth = ceil(width).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
    try {
        draw(Canvas(bitmap), bitmapWidth.toFloat(), 0f)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { it == targetColor }
    } finally {
        bitmap.recycle()
    }
}
