/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import android.graphics.Typeface
import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.internal.AsyncFauxFont
import androidx.compose.foundation.text.input.internal.AsyncTestTypefaceLoader
import androidx.compose.foundation.text.toIntPx
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MultiParagraphLayoutCacheTest {

    private val fontFamily = TEST_FONT_FAMILY
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun minIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate =
                MultiParagraphLayoutCache(
                        text = annotatedString,
                        style = TextStyle.Default,
                        fontFamilyResolver = fontFamilyResolver,
                    )
                    .also { it.density = this }

            textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)

            assertThat(textDelegate.minIntrinsicWidth(LayoutDirection.Ltr))
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun intrinsicHeight_invalidates() {
        val fontSize = 20.sp
        val text = "Hello"
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        val textDelegate =
            MultiParagraphLayoutCache(
                    text = annotatedString,
                    style = TextStyle.Default,
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }

        val original = textDelegate.intrinsicHeight(20, LayoutDirection.Ltr)
        textDelegate.update(
            AnnotatedString("Longer\ntext\ngoes\nhere\n\n\n."),
            TextStyle.Default,
            fontFamilyResolver,
            TextOverflow.Visible,
            true,
            Int.MAX_VALUE,
            -1,
            null,
            null,
        )
        val after = textDelegate.intrinsicHeight(20, LayoutDirection.Ltr)
        assertThat(original).isLessThan(after)
    }

    @Test
    fun minIntrinsicsHeight_respectsMinLines() {
        with(density) {
            val fontSize = 20.sp
            val text = AnnotatedString("A")
            val singleLineLayout =
                MultiParagraphLayoutCache(
                        text = text,
                        style = TextStyle.Default.copy(fontSize = fontSize),
                        fontFamilyResolver = fontFamilyResolver,
                        minLines = 1,
                    )
                    .also { it.density = this }
            val withMinLinesLayout =
                MultiParagraphLayoutCache(
                        text = text,
                        style = TextStyle.Default.copy(fontSize = fontSize),
                        fontFamilyResolver = fontFamilyResolver,
                        minLines = 3,
                    )
                    .also { it.density = this }

            assertThat(withMinLinesLayout.intrinsicHeight(200, LayoutDirection.Ltr))
                .isEqualTo(singleLineLayout.intrinsicHeight(200, LayoutDirection.Ltr) * 3)
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate =
                MultiParagraphLayoutCache(
                        text = annotatedString,
                        style = TextStyle.Default,
                        fontFamilyResolver = fontFamilyResolver,
                    )
                    .also { it.density = this }

            textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)

            assertThat(textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr))
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() {
        val textDelegate =
            MultiParagraphLayoutCache(
                    text = AnnotatedString("Hello World"),
                    style = TextStyle.Default,
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.textLayoutResult
        assertThat(resultFirstLayout.layoutInput.constraints).isEqualTo(constraintsFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(constraintsSecondLayout, LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.textLayoutResult
        assertThat(resultSecondLayout.layoutInput.constraints).isEqualTo(constraintsSecondLayout)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() {
        val textDelegate =
            MultiParagraphLayoutCache(
                    text = AnnotatedString("Hello World"),
                    style = TextStyle.Default,
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.textLayoutResult
        assertThat(resultFirstLayout.size.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(constraintsSecondLayout, LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.textLayoutResult
        assertThat(resultSecondLayout.size.height).isEqualTo(heightSecondLayout)
    }

    @Test
    fun TextLayoutResult_layout_withEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")
        val textDelegate =
            MultiParagraphLayoutCache(
                    text = text,
                    style = TextStyle(fontSize = fontSize.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        // Makes width smaller than needed.
        val width = textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr) / 2
        val constraints = Constraints(maxWidth = width)
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.textLayoutResult

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }

    @Test
    fun TextLayoutResult_layoutWithLimitedHeight_withEllipsis() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")

        val textDelegate =
            MultiParagraphLayoutCache(
                    text = text,
                    style = TextStyle(fontSize = fontSize.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                )
                .also { it.density = density }
        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        val constraints =
            Constraints(
                maxWidth = textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr) / 4,
                maxHeight = (fontSize * 2.7).roundToInt(), // fully fits at most 2 lines
            )
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.textLayoutResult

        assertThat(layoutResult.lineCount).isEqualTo(2)
        assertThat(layoutResult.isLineEllipsized(1)).isTrue()
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentDensity() {
        var backingDensity = 1f
        val density =
            object : Density {
                override val density: Float
                    get() = backingDensity

                override val fontScale: Float
                    get() = 1f
            }
        val textDelegate =
            MultiParagraphLayoutCache(
                    text = AnnotatedString("Hello World"),
                    style = TextStyle.Default,
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.textLayoutResult.size

        backingDensity = 2f
        // Compose makes sure to notify us that density has changed but using the same object
        textDelegate.density = density

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.textLayoutResult.size

        assertThat(resultFirstLayout.width).isLessThan(resultSecondLayout.width)
        assertThat(resultFirstLayout.height).isLessThan(resultSecondLayout.height)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentFontScale() {
        var backingFontScale = 1f
        val density =
            object : Density {
                override val density: Float
                    get() = 1f

                override val fontScale: Float
                    get() = backingFontScale
            }
        val textDelegate =
            MultiParagraphLayoutCache(
                    text = AnnotatedString("Hello World"),
                    style = TextStyle.Default,
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.textLayoutResult.size

        backingFontScale = 2f
        // Compose makes sure to notify us that density has changed but using the same object
        textDelegate.density = density

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.textLayoutResult.size

        assertThat(resultFirstLayout.width).isLessThan(resultSecondLayout.width)
        assertThat(resultFirstLayout.height).isLessThan(resultSecondLayout.height)
    }

    @Test
    fun TextLayoutResult_sameWidth_inRtlAndLtr_withLetterSpacing() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World")

        val textDelegate =
            MultiParagraphLayoutCache(
                    text = text,
                    style = TextStyle(fontSize = fontSize.sp, letterSpacing = 0.5.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val layoutResultLtr = textDelegate.textLayoutResult
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Rtl)
        val layoutResultRtl = textDelegate.textLayoutResult

        assertThat(layoutResultLtr.size.width).isEqualTo(layoutResultRtl.size.width)
    }

    private fun testAutoSize_fontSizeFittingConstraints_doesntOverflow(
        autoSize: TextAutoSize,
        text: AnnotatedString,
        constraints: Constraints,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
    ) {
        val layoutCache =
            MultiParagraphLayoutCache(
                    text = text,
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = 1,
                    autoSize = autoSize,
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.hasVisualOverflow).isFalse()
        assertThat(layoutResult.size.width).isAtMost(constraints.maxWidth)
        assertThat(layoutResult.size.height).isAtMost(constraints.maxHeight)
        assertThat(layoutResult.multiParagraph.height).isAtMost(constraints.maxHeight)
        assertThat(layoutResult.multiParagraph.width).isAtMost(constraints.maxWidth)
        assertThat(layoutResult.lineCount).isEqualTo(1)
    }

    private fun testAutoSize_fontSizeNotFittingConstraints_overflows(
        autoSize: TextAutoSize,
        text: AnnotatedString,
        constraints: Constraints,
    ) {
        val layoutCache =
            MultiParagraphLayoutCache(
                    text = text,
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    softWrap = false,
                    maxLines = 1,
                    autoSize = autoSize,
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.hasVisualOverflow).isTrue()
        assertThat(layoutResult.size.height).isLessThan(constraints.maxHeight)
        assertThat(layoutResult.size.width).isEqualTo(constraints.maxWidth)
        assertThat(layoutResult.multiParagraph.height).isLessThan(constraints.maxHeight)
        assertThat(layoutResult.multiParagraph.width).isGreaterThan(constraints.maxWidth)
        assertThat(layoutResult.lineCount).isEqualTo(1)
    }

    @Test
    fun autoSize_singlePresetFontSize_fittingConstraints_doesntOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }
        val fittingConstraintsFontSize = widthPerCharacter * 0.8
        val autoSize = AutoSizePreset(arrayOf(fittingConstraintsFontSize))
        testAutoSize_fontSizeFittingConstraints_doesntOverflow(
            autoSize = autoSize,
            text = AnnotatedString(text),
            constraints = constraints,
        )
    }

    @Test
    fun autoSize_singlePresetFontSize_notFittingConstraints_overflows() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }
        val overflowingConstraintsFontSize = widthPerCharacter * 1.2
        val autoSize = AutoSizePreset(arrayOf(overflowingConstraintsFontSize))
        testAutoSize_fontSizeNotFittingConstraints_overflows(
            autoSize = autoSize,
            text = AnnotatedString(text),
            constraints = constraints,
        )
    }

    @Test
    fun autoSize_multipleFontSizes_fittingConstraints_doesntOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }
        val autoSize =
            AutoSizePreset(
                arrayOf(widthPerCharacter * 0.3, widthPerCharacter * 0.5, widthPerCharacter * 0.7)
            )
        testAutoSize_fontSizeFittingConstraints_doesntOverflow(
            autoSize = autoSize,
            text = AnnotatedString(text),
            constraints = constraints,
        )
    }

    @Test
    fun autoSize_multipleFontSizes_notFittingConstraints_overflows() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }
        val autoSize =
            AutoSizePreset(
                arrayOf(widthPerCharacter * 1.2, widthPerCharacter * 1.4, widthPerCharacter * 1.6)
            )
        testAutoSize_fontSizeNotFittingConstraints_overflows(
            autoSize = autoSize,
            text = AnnotatedString(text),
            constraints = constraints,
        )
    }

    @Test
    fun TextLayoutResult_autoSize_textLongerThan30Characters_doesOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val layoutCache =
            MultiParagraphLayoutCache(
                    text =
                        AnnotatedString(
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas " +
                                "sollicitudin arcu, sed mattis orci gravida vel. Donec luctus turpis."
                        ),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = TextAutoSize.StepBased(20.sp, 51.sp, 1.sp),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        // this should overflow - 20.sp is too large of a font size to use for the longer text
        assertThat(layoutResult.hasVisualOverflow).isTrue()
        assertThat(layoutResult.multiParagraph.height).isEqualTo(600)
    }

    @Test
    fun TextLayoutResult_autoSize_startEllipsis_fittingText_doesNotEllipsize() {
        autoSizeEllipsizeFittingTextDoesNotEllipsize(TextOverflow.StartEllipsis)
    }

    @Test
    fun TextLayoutResult_autoSize_middleEllipsis_fittingText_doesNotEllipsize() {
        autoSizeEllipsizeFittingTextDoesNotEllipsize(TextOverflow.MiddleEllipsis)
    }

    @Test
    fun TextLayoutResult_autoSize_ellipsis_fittingText_doesNotEllipsize() {
        autoSizeEllipsizeFittingTextDoesNotEllipsize(TextOverflow.Ellipsis)
    }

    private fun autoSizeEllipsizeFittingTextDoesNotEllipsize(textOverflow: TextOverflow) {
        check(
            textOverflow == TextOverflow.StartEllipsis ||
                textOverflow == TextOverflow.MiddleEllipsis ||
                textOverflow == TextOverflow.Ellipsis
        ) {
            "textOverflow should be an Ellipsis mode, but was $textOverflow"
        }
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = textOverflow,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = widthPerCharacter * 0.5,
                            maxFontSize = widthPerCharacter * 0.9,
                            stepSize = (0.5).sp,
                        ),
                    // Use maxLines to ensure that the text would be ellipsized if it was too big
                    maxLines = 1,
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.layoutInput.style.fontSize).isEqualTo(widthPerCharacter * 0.9)
        assertThat(layoutResult.didOverflowWidth).isFalse()
        assertThat(layoutResult.didOverflowHeight).isFalse()
        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isFalse()
    }

    @Test
    fun TextLayoutResult_autoSize_startEllipsis_overflowingText_firstLineIsEllipsized() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }

        // The min font size overflows
        val minFontSize = widthPerCharacter * 1.2
        val maxFontSize = widthPerCharacter * 3

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.StartEllipsis,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = minFontSize,
                            maxFontSize = maxFontSize,
                            stepSize = 1.sp,
                        ),
                    // StartEllipsis only happens for single-line text
                    maxLines = 1,
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.layoutInput.style.fontSize).isEqualTo(minFontSize)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
        assertThat(layoutResult.didOverflowWidth).isFalse()
        assertThat(layoutResult.lineCount).isEqualTo(1)
    }

    @Test
    fun TextLayoutResult_autoSize_startEllipsis_overflowingText_multiLine_isClipped() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaa"
        val linesToOverflow =
            ceil(calculateLinesNeededToOverflowVertically(text.length)).roundToInt()
        val overflowingCharacterSize = (constraints.maxHeight / linesToOverflow).sp

        val minFontSize = overflowingCharacterSize
        val maxFontSize = overflowingCharacterSize * 2

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.StartEllipsis,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = minFontSize,
                            maxFontSize = maxFontSize,
                            stepSize = 1.sp,
                        ),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.layoutInput.style.fontSize).isEqualTo(minFontSize)
        assertThat(layoutResult.hasVisualOverflow).isTrue()
        assertThat(layoutResult.lineCount).isGreaterThan(1)

        val ellipsizedLines = mutableListOf<Int>()
        for (line in 0 until layoutResult.lineCount) {
            if (layoutResult.isLineEllipsized(line)) {
                ellipsizedLines.add(line)
            }
        }
        assertWithMessage("No lines should be ellipsized").that(ellipsizedLines).isEmpty()
    }

    @Test
    fun TextLayoutResult_autoSize_middleEllipsis_overflowingText_singleLine_hasEllipsizedLine() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaa"
        val widthPerCharacter =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }

        // The min font size overflows
        val minFontSize = widthPerCharacter * 1.2
        val maxFontSize = widthPerCharacter * 3

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.MiddleEllipsis,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = minFontSize,
                            maxFontSize = maxFontSize,
                            stepSize = 1.sp,
                        ),
                    // MiddleEllipsis only happens for single-line text
                    maxLines = 1,
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.layoutInput.style.fontSize).isEqualTo(minFontSize)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
        assertThat(layoutResult.didOverflowWidth).isFalse()
        assertThat(layoutResult.lineCount).isEqualTo(1)
    }

    @Test
    fun TextLayoutResult_autoSize_middleEllipsis_overflowingText_multiLine_isClipped() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "aaaaaaaaaa"
        val linesToOverflow =
            ceil(calculateLinesNeededToOverflowVertically(text.length)).roundToInt()
        val overflowingCharacterSize = (constraints.maxHeight / linesToOverflow).sp

        val minFontSize = overflowingCharacterSize
        val maxFontSize = overflowingCharacterSize * 2

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.MiddleEllipsis,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = minFontSize,
                            maxFontSize = maxFontSize,
                            stepSize = 1.sp,
                        ),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.layoutInput.style.fontSize).isEqualTo(minFontSize)
        assertThat(layoutResult.hasVisualOverflow).isTrue()
        assertThat(layoutResult.lineCount).isGreaterThan(1)

        val ellipsizedLines = mutableListOf<Int>()
        for (line in 0 until layoutResult.lineCount) {
            if (layoutResult.isLineEllipsized(line)) {
                ellipsizedLines.add(line)
            }
        }
        assertWithMessage("No lines should be ellipsized").that(ellipsizedLines).isEmpty()
    }

    /**
     * This test isn't aligned with the StartEllipsis and MiddleEllipsis tests due to a bug on FTL's
     * Nexus 4 API 21 target. TODO: Align this test with the other tests (b/401074904)
     */
    @Test
    fun TextLayoutResult_autoSize_ellipsis_overflowingText_lastLineIsEllipsized() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas " +
                "sollicitudin arcu, sed mattis orci gravida vel. Donec luctus turpis."
        val minFontSize = 20.sp
        val maxFontSize = 51.sp

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = minFontSize,
                            maxFontSize = maxFontSize,
                            stepSize = 1.sp,
                        ),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.layoutInput.style.fontSize).isEqualTo(minFontSize)
        assertThat(layoutResult.didOverflowWidth).isFalse()
        assertThat(layoutResult.multiParagraph.height).isEqualTo(100)
        assertThat(layoutResult.lineCount).isEqualTo(5)
        for (line in 0..3) {
            assertWithMessage("Line $line should not be ellipsized")
                .that(layoutResult.isLineEllipsized(line))
                .isFalse()
        }
        assertWithMessage("Last line should be ellipsized")
            .that(layoutResult.isLineEllipsized(4))
            .isTrue()
    }

    /**
     * Calculate the number of lines needed for a text of length [textLength] to overflow in a grid
     * with 1:1 aspect ratio (width == height). This can be used to calculate e.g. the size uniform
     * sized characters need to take up in order to overflow.
     *
     * @param textLength The length of the text
     * @return The number of lines needed to overflow, as a double. Consider using [ceil] on the
     *   result.
     */
    private fun calculateLinesNeededToOverflowVertically(textLength: Int): Double {
        val charCount = textLength.toDouble()
        // In a grid with uniformly sized rows and columns, the total grid capacity can be expressed
        //  as: charCount = x^2. Solving for x gets us: x = √(charCount).
        val gridCapacity = sqrt(charCount)
        // In order to calculate how many lines we need to overflow, we're looking for x + 1:
        //  charCount = (x + 1) * x. Applying the quadratic formula gets us the following equation:
        val linesNeededToOverflow = sqrt(charCount + 0.25) - 0.5
        // Finally, rounding up the upper bound (linesNeededToOverflow) could leave us with too high
        //  of a line count, so we take the mean of the lower and upper bound.
        return (gridCapacity + linesNeededToOverflow) / 2
    }

    @Test
    fun TextLayoutResult_autoSize_visibleOverflow_doesOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val layoutCache =
            MultiParagraphLayoutCache(
                    text =
                        AnnotatedString(
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas " +
                                "sollicitudin arcu, sed mattis orci gravida vel. Donec luctus turpis."
                        ),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Visible,
                    autoSize = TextAutoSize.StepBased(20.sp, 51.sp, 1.sp),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        // this should overflow
        assertThat(layoutResult.hasVisualOverflow).isTrue()
        assertThat(layoutResult.multiParagraph.height).isEqualTo(600)
    }

    @Test
    fun TextLayoutResult_autoSize_em_checkOverflowAndHeight() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontSize = 5.sp, fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizePreset(arrayOf(5.12.em)), // = 25.6sp
                )
                .also { it.density = density }

        // 5.12.em / 25.6.sp shouldn't overflow
        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        var layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.hasVisualOverflow).isFalse()
        assertThat(layoutResult.multiParagraph.height).isEqualTo(100)

        layoutCache.updateAutoSize(
            text = text,
            fontSize = 5.sp,
            autoSize = AutoSizePreset(arrayOf(), fallbackFontSize = 5.14.em),
        )

        // 5.14 .em / 25.7.sp should overflow
        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.hasVisualOverflow).isTrue()
        assertThat(layoutResult.multiParagraph.height).isGreaterThan(100)
    }

    @Test(expected = IllegalStateException::class)
    fun autoSize_toPx_em_style_fontSize_is_em_throws() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString("Hello World"),
                    style = TextStyle(fontSize = 0.01.em, fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizePreset(arrayOf(2.em)),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
    }

    @Test
    fun TextLayoutResult_autoSize_em_style_fontSize_is_unspecified_checkOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        // For a density of 1, our grid capacity can be calculated as:
        // (height / fontSize) * (width / fontSize)
        // This test uses TextUnit.Unspecified, which resolves to 16.sp during layout. With 16sp and
        // a 100x100 rect, we have a character capacity of 39. At 2em, we have a character capacity
        // of ~9.7, so we use a string with 10 chars to test.
        val text = "aaaaaaaaaa"

        fun fakeAutoSize(returnFontSize: TextUnit) =
            object : TextAutoSize {
                override fun TextAutoSizeLayoutScope.getFontSize(
                    constraints: Constraints,
                    text: AnnotatedString,
                ) = returnFontSize

                override fun equals(other: Any?) = this === other

                override fun hashCode() = System.identityHashCode(this)
            }

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = fakeAutoSize(1.em),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        var layoutResult = layoutCache.textLayoutResult
        // doesn't overflow
        assertThat(layoutResult.hasVisualOverflow).isFalse()

        layoutCache.updateAutoSize(text, TextUnit.Unspecified, fakeAutoSize(2.em))

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        layoutResult = layoutCache.textLayoutResult
        // does overflow
        assertThat(layoutResult.hasVisualOverflow).isTrue()
    }

    @Test
    fun TextLayoutResult_autoSize_minLines_greaterThan_1_checkOverflowAndHeight() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val layoutCache =
            MultiParagraphLayoutCache(
                    text = AnnotatedString("H"),
                    style = TextStyle(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                    minLines = 2,
                    autoSize = TextAutoSize.StepBased(20.sp, 51.sp, 1.sp),
                )
                .also { it.density = density }

        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertThat(layoutResult.hasVisualOverflow).isFalse()
        assertThat(layoutResult.multiParagraph.height)
            .isAtMost(55) // this value is different between
        // different API levels. Either 51 or 52. Using isAtMost to anticipate future permutations.
    }

    // Regression test for b/376834366
    @Test
    fun autoSize_maxLines_1_fittingConstraints_overflowBiasedWindow_doesntOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = AnnotatedString("aaaaaaaaaaaa")
        // Assuming uniform character sizing (like in our test), this is the character size that
        // technically fits the constraints for the given text.
        val optimalCharacterSizeForText =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }

        // In this test, we want a left-hand biased search window so that the first auto size
        // layout should not overflow. With a left-hand biased window, the midpoint of the
        // search will start further on the left and then move right, highlighting any potential
        // caching issues.
        val min = optimalCharacterSizeForText * 0.3
        val max = optimalCharacterSizeForText * 1.3
        val autoSize = TextAutoSize.StepBased(minFontSize = min, maxFontSize = max)

        val autoSizeLayoutCache = createLayoutCache(text, autoSize, maxLines = 1)
        autoSizeLayoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val autoSizeLayoutResult = autoSizeLayoutCache.textLayoutResult
        assertWithMessage("Layout Result (Auto Size) overflow")
            .that(autoSizeLayoutResult.hasVisualOverflow)
            .isFalse()
        assertWithMessage("Font size used for auto size")
            .that(autoSizeLayoutResult.layoutInput.style.fontSize.value)
            .isAtMost(optimalCharacterSizeForText.value * 1.1f) // Use a slight tolerance
        assertThat(autoSizeLayoutResult.layoutInput.constraints).isEqualTo(constraints)

        val layoutCache =
            createLayoutCache(text, style = autoSizeLayoutResult.layoutInput.style, maxLines = 1)
        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertWithMessage("Layout Result (No Auto Size) overflow")
            .that(autoSizeLayoutResult.hasVisualOverflow)
            .isFalse()
        assertThat(layoutResult.layoutInput.constraints).isEqualTo(constraints)
    }

    // Regression test for b/376834366
    @Test
    fun autoSize_maxLines_1_fittingConstraints_underflowBiasedWindow_doesntOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = AnnotatedString("aaaaaaaaaaaa")
        // Assuming uniform character sizing (like in our test), this is the character size that
        // that technically fits the constraints for the given text.
        val optimalCharacterSizeForText =
            with(density) { (constraints.maxWidth / text.length.toFloat()).toSp() }

        // In this test, we want a right-hand biased search window so that the first auto size
        // layout should overflow. With a right-hand biased window, the midpoint of the
        // search will start further on the right and then move left, highlighting any potential
        // caching issues.
        val min = optimalCharacterSizeForText * 0.9
        val max = optimalCharacterSizeForText * 2
        val autoSize = TextAutoSize.StepBased(minFontSize = min, maxFontSize = max)

        val autoSizeLayoutCache = createLayoutCache(text, autoSize, maxLines = 1)
        autoSizeLayoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val autoSizeLayoutResult = autoSizeLayoutCache.textLayoutResult
        assertWithMessage("Layout Result (Auto Size) overflow")
            .that(autoSizeLayoutResult.hasVisualOverflow)
            .isFalse()
        assertWithMessage("Font size used for auto size")
            .that(autoSizeLayoutResult.layoutInput.style.fontSize.value)
            .isAtMost(optimalCharacterSizeForText.value * 1.1f) // Use a slight tolerance
        assertThat(autoSizeLayoutResult.layoutInput.constraints).isEqualTo(constraints)

        val layoutCache =
            createLayoutCache(text, style = autoSizeLayoutResult.layoutInput.style, maxLines = 1)
        layoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = layoutCache.textLayoutResult
        assertWithMessage("Layout Result (No Auto Size) overflow")
            .that(autoSizeLayoutResult.hasVisualOverflow)
            .isFalse()
        assertThat(layoutResult.layoutInput.constraints).isEqualTo(constraints)
    }

    /**
     * Regression test for b/392070664. Tests the scenario where we have successfully performed auto
     * size before the font was resolved, requiring us to re-layout without the cache getting
     * invalidated entirely.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun autoSize_layoutAfterFontResolutionCompletesProducesSameResult() = runTest {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        // Use an auto size that only performs two passes and returns the size of the last pass as
        // the most optimal size. This simulates using TextAutoSize.StepBased with a string that can
        // be fitted using auto size's max font size.
        val autoSize =
            object : TextAutoSize {
                override fun TextAutoSizeLayoutScope.getFontSize(
                    constraints: Constraints,
                    text: AnnotatedString,
                ): TextUnit {
                    performLayout(constraints, text, 110.sp)
                    performLayout(constraints, text, 112.sp)
                    return 112.sp
                }

                override fun equals(other: Any?) = this === other

                override fun hashCode() = System.identityHashCode(this)
            }

        // Font family resolution is executed on the main thread by default, but we need to control
        // the dispatcher in this test. FontListFontFamilyTypefaceAdapter creates a SupervisorJob
        // that will then be active in this coroutine context, so we pass a Job along to make that
        // SupervisorJob a child of our job and control its lifespan.
        val fontFamilyResolutionJob = Job()
        val testSchedulerControlledFontFamilyResolver =
            createFontFamilyResolver(
                context = context,
                coroutineContext = this.coroutineContext + fontFamilyResolutionJob,
            )

        val typefaceLoader = AsyncTestTypefaceLoader()
        val fauxFont = AsyncFauxFont(typefaceLoader)
        val autoSizeLayoutCache =
            createLayoutCache(
                AnnotatedString("aaaaaaaaa"),
                autoSize,
                style = TextStyle(fontFamily = fauxFont.toFontFamily()),
                fontFamilyResolver = testSchedulerControlledFontFamilyResolver,
            )

        autoSizeLayoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val firstPassAutoSizeLayoutResult = autoSizeLayoutCache.textLayoutResult

        // By default, the system will fall back to Typeface.DEFAULT. We return something different
        //  here to make sure the fonts actually become stale.
        typefaceLoader.completeOne(fauxFont, Typeface.DEFAULT_BOLD)
        advanceUntilIdle()

        autoSizeLayoutCache.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val secondPassAutoSizeLayoutResult = autoSizeLayoutCache.textLayoutResult

        assertWithMessage("First pass LayoutInput's fontSize is equal to second pass")
            .that(firstPassAutoSizeLayoutResult.layoutInput.style.fontSize)
            .isEqualTo(secondPassAutoSizeLayoutResult.layoutInput.style.fontSize)
        assertWithMessage("First pass MultiParagraph width is equal to second pass")
            .that(firstPassAutoSizeLayoutResult.multiParagraph.width)
            .isEqualTo(secondPassAutoSizeLayoutResult.multiParagraph.width)
        assertWithMessage("First pass MultiParagraph height is equal to second pass")
            .that(firstPassAutoSizeLayoutResult.multiParagraph.height)
            .isEqualTo(secondPassAutoSizeLayoutResult.multiParagraph.height)
    }

    @Test
    fun maxHeight_hasSameHeight_asParagraph() {
        val text = buildAnnotatedString {
            for (i in 1..100 step 10) {
                pushStyle(SpanStyle(fontSize = i.sp))
                append("$i.sp\n")
                pop()
            }
        }

        val textDelegate =
            MultiParagraphLayoutCache(
                    text = text,
                    style = TextStyle(fontSize = 1.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5,
                )
                .also { it.density = density }
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val actual = textDelegate.textLayoutResult.multiParagraph

        val expected =
            Paragraph(
                text.text,
                TextStyle(fontSize = 1.sp),
                Constraints(),
                density,
                fontFamilyResolver,
                text.spanStyles,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
        assertThat(actual.height).isEqualTo(expected.height)
    }

    @Test
    fun hugeString_doesntCrash() {
        val text = "A".repeat(100_000)
        val subject =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontSize = 100.sp),
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }
        subject.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
    }

    @Test
    fun history_isRecorded() {
        val text = "Hello, World"
        val subject =
            MultiParagraphLayoutCache(
                    text = AnnotatedString(text),
                    style = TextStyle(fontSize = 100.sp),
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }

        subject.layoutWithConstraints(Constraints.fixed(100, 100), LayoutDirection.Ltr)
        subject.update(
            text = AnnotatedString("Hello again, World"),
            style = TextStyle(fontSize = 100.sp),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            minLines = DefaultMinLines,
            placeholders = null,
            autoSize = null,
        )
        subject.layoutWithConstraints(Constraints.fixed(100, 100), LayoutDirection.Ltr)
        subject.density = Density(2f, 3f)

        assertThat(subject.historyFlag).isEqualTo(0b011101101)
    }

    private fun MultiParagraphLayoutCache.updateAutoSize(
        text: String,
        fontSize: TextUnit,
        autoSize: TextAutoSize,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
    ) =
        update(
            text = AnnotatedString(text),
            style = TextStyle(fontSize = fontSize, fontFamily = fontFamily),
            fontFamilyResolver = fontFamilyResolver,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = Int.MAX_VALUE,
            minLines = DefaultMinLines,
            placeholders = null,
            autoSize = autoSize,
        )

    private fun createLayoutCache(
        text: AnnotatedString,
        autoSize: TextAutoSize? = null,
        style: TextStyle = TextStyle(fontFamily = fontFamily),
        maxLines: Int = Int.MAX_VALUE,
        fontFamilyResolver: FontFamily.Resolver = this.fontFamilyResolver,
    ): MultiParagraphLayoutCache {
        return MultiParagraphLayoutCache(
                text = text,
                style = style,
                fontFamilyResolver = fontFamilyResolver,
                autoSize = autoSize,
                maxLines = maxLines,
            )
            .also { it.density = density }
    }
}
