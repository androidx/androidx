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

import androidx.compose.foundation.text.AutoSize
import androidx.compose.foundation.text.DefaultMinLines
import androidx.compose.foundation.text.FontSizeSearchScope
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.toIntPx
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
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
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ParagraphLayoutCacheTest {

    private val fontFamily = TEST_FONT_FAMILY
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontFamilyResolver = createFontFamilyResolver(context)

    @Test
    fun minIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val textDelegate =
                ParagraphLayoutCache(
                        text = text,
                        style = createTextStyle(fontSize = fontSize),
                        fontFamilyResolver = fontFamilyResolver,
                    )
                    .also { it.density = this }

            assertThat(textDelegate.minIntrinsicWidth(LayoutDirection.Ltr))
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val textDelegate =
                ParagraphLayoutCache(
                        text = text,
                        style = createTextStyle(fontSize = fontSize),
                        fontFamilyResolver = fontFamilyResolver,
                    )
                    .also { it.density = this }

            assertThat(textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr))
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() {
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = TextStyle.Default.copy(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layoutSize

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(constraintsSecondLayout, LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.layoutSize

        assertThat(resultFirstLayout.height).isLessThan(resultSecondLayout.height)
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentDensity() {
        var backingDensity = 1f
        val density =
            object : Density {
                override val density: Float
                    get() = backingDensity

                override val fontScale: Float
                    get() = 1f
            }
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = TextStyle.Default.copy(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layoutSize

        backingDensity = 2f
        // Compose makes sure to notify us that density has changed but using the same object
        textDelegate.density = density

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.layoutSize

        assertThat(resultFirstLayout.width).isLessThan(resultSecondLayout.width)
        assertThat(resultFirstLayout.height).isLessThan(resultSecondLayout.height)
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentFontScale() {
        var backingFontScale = 1f
        val density =
            object : Density {
                override val density: Float
                    get() = 1f

                override val fontScale: Float
                    get() = backingFontScale
            }
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = TextStyle.Default.copy(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layoutSize

        backingFontScale = 2f
        // Compose makes sure to notify us that density has changed but using the same object
        textDelegate.density = density

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.layoutSize

        assertThat(resultFirstLayout.width).isLessThan(resultSecondLayout.width)
        assertThat(resultFirstLayout.height).isLessThan(resultSecondLayout.height)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() {
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = TextStyle.Default.copy(fontFamily = fontFamily),
                    fontFamilyResolver = fontFamilyResolver,
                )
                .also { it.density = density }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layoutSize
        assertThat(resultFirstLayout.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(constraintsSecondLayout, LayoutDirection.Ltr)
        val resultSecondLayout = textDelegate.layoutSize
        assertThat(resultSecondLayout.height).isEqualTo(heightSecondLayout)
    }

    @Test
    fun TextLayoutResult_layout_withEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World! Hello World! Hello World! Hello World!",
                    style = createTextStyle(fontSize = fontSize.sp),
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
        val layoutResult = textDelegate.paragraph!!

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }

    @Test
    fun TextLayoutResult_layout_withStartEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World! Hello World! Hello World! Hello World!",
                    style = createTextStyle(fontSize = fontSize.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    softWrap = false,
                    overflow = TextOverflow.StartEllipsis,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        // Makes width smaller than needed.
        val width = textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr) / 2
        val constraints = Constraints(maxWidth = width)
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }

    @Test
    fun TextLayoutResult_layout_withMiddleEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World! Hello World! Hello World! Hello World!",
                    style = createTextStyle(fontSize = fontSize.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    softWrap = false,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        // Makes width smaller than needed.
        val width = textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr) / 2
        val constraints = Constraints(maxWidth = width)
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }

    @Test
    fun TextLayoutResult_layoutWithLimitedHeight_withEllipsis() {
        val fontSize = 20f

        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World! Hello World! Hello World! Hello World!",
                    style = createTextStyle(fontSize = fontSize.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                )
                .also { it.density = density }
        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        val constraints =
            Constraints(
                maxWidth = textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr) / 4,
                maxHeight = (fontSize * 2.7).roundToInt() // fully fits at most 2 lines
            )
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!

        assertThat(layoutResult.lineCount).isEqualTo(2)
        assertThat(layoutResult.isLineEllipsized(1)).isTrue()
    }

    @Test
    fun TextLayoutResult_sameWidth_inRtlAndLtr_withLetterSpacing() {
        val fontSize = 20f

        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = createTextStyle(fontSize = fontSize.sp, letterSpacing = 0.5.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val layoutResultLtr = textDelegate.layoutSize
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Rtl)
        val layoutResultRtl = textDelegate.layoutSize

        assertThat(layoutResultLtr.width).isEqualTo(layoutResultRtl.width)
    }

    @Test
    fun TextLayoutResult_autoSize_oneSize_checkOverflowAndHeight() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizePreset(arrayOf(25.sp))
                )
                .also { it.density = density }

        // 25.6.sp doesn't overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        var layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isEqualTo(100)

        textDelegate.updateAutoSize(
            text = "Hello World",
            fontSize = TextUnit.Unspecified,
            autoSize = AutoSizePreset(arrayOf(25.7.sp))
        )

        // 25.7.sp does overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isTrue()
        assertThat(layoutResult.height).isEqualTo(1000)
    }

    @Test
    fun TextLayoutResult_autoSize_multipleSizes_checkOverflowAndHeight() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizePreset(arrayOf(23.5.sp, 22.sp, 25.6.sp))
                )
                .also { it.density = density }

        // All font sizes shouldn't overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        var layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isEqualTo(100)

        textDelegate.updateAutoSize(
            text = text,
            fontSize = TextUnit.Unspecified,
            autoSize = AutoSizePreset(arrayOf(25.7.sp, 25.6.sp, 50.sp))
        )

        // Only 25.6.sp shouldn't overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isEqualTo(100)

        textDelegate.updateAutoSize(
            text = text,
            fontSize = TextUnit.Unspecified,
            autoSize = AutoSizePreset(arrayOf(25.9.sp, 25.7.sp, 50.sp))
        )

        // All font sizes should overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isTrue()
        assertThat(layoutResult.height).isEqualTo(1000)
    }

    @Test
    fun TextLayoutResult_autoSize_differentConstraints_doesOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 50, minHeight = 0, maxHeight = 50)

        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = createTextStyle(20.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSize.StepBased(20.sp, 51.sp, 1.sp)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!
        // this should overflow - 20.sp is too large a font size to use for the smaller constraints
        assertThat(textDelegate.didOverflow).isTrue()
        assertThat(layoutResult.height).isEqualTo(120)
    }

    @Test
    fun TextLayoutResult_autoSize_differentText_doesOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val textDelegate =
            ParagraphLayoutCache(
                    text =
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas " +
                            "sollicitudin arcu, sed mattis orci gravida vel. Donec luctus turpis.",
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSize.StepBased(20.sp, 51.sp, 1.sp)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!
        // this should overflow - 20.sp is too large of a font size to use for the longer text
        assertThat(textDelegate.didOverflow).isTrue()
        assertThat(layoutResult.height).isEqualTo(600)
    }

    @Test
    fun TextLayoutResult_autoSize_ellipsized_isLineEllipsized() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas " +
                "sollicitudin arcu, sed mattis orci gravida vel. Donec luctus turpis."

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = AutoSize.StepBased(20.sp, 51.sp, 1.sp)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!
        // Without ellipsis logic, the text would overflow with a height of 600.
        // This shouldn't overflow due to the ellipsis logic.
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isEqualTo(100)
        assertThat(layoutResult.isLineEllipsized(4)).isTrue()
    }

    @Test
    fun TextLayoutResult_autoSize_visibleOverflow_doesOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val textDelegate =
            ParagraphLayoutCache(
                    text =
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec egestas " +
                            "sollicitudin arcu, sed mattis orci gravida vel. Donec luctus turpis.",
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Visible,
                    autoSize = AutoSize.StepBased(20.sp, 51.sp, 1.sp)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!
        // this should overflow
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isEqualTo(600)
    }

    @Test
    fun TextLayoutResult_autoSize_em_checkOverflowAndHeight() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(5.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = AutoSizePreset(arrayOf(5.12.em)) // = 25.6sp
                )
                .also { it.density = density }

        // 5.12.em / 25.6.sp shouldn't overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        var layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isEqualTo(100)

        textDelegate.updateAutoSize(
            text = text,
            fontSize = 5.sp,
            autoSize = AutoSizePreset(arrayOf(5.14.em))
        )

        // 5.14 .em / 25.7.sp should overflow
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isTrue()
        assertThat(layoutResult.height).isEqualTo(1000)
    }

    @Test(expected = IllegalStateException::class)
    fun toPx_em_style_fontSize_is_em_throws() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val textDelegate =
            ParagraphLayoutCache(
                    text = "Hello World",
                    style = TextStyle(fontSize = 0.01.em),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizePreset(arrayOf(2.em))
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
    }

    @Test
    fun TextLayoutResult_autoSize_em_style_fontSize_is_unspecified_checkOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizePreset(arrayOf(1.em))
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        // doesn't overflow
        assertThat(textDelegate.didOverflow).isFalse()

        textDelegate.updateAutoSize(
            text = text,
            fontSize = TextUnit.Unspecified,
            AutoSizePreset(arrayOf(2.em))
        )

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        // does overflow
        assertThat(textDelegate.didOverflow).isTrue()
    }

    @Test
    fun TextLayoutResult_autoSize_em_withoutToPx_checkOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(1.em),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizeWithoutToPx(2.em)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        // this shouldn't overflow
        assertThat(textDelegate.didOverflow).isFalse()

        textDelegate.updateAutoSize(
            text = text,
            fontSize = 1.em,
            autoSize = AutoSizeWithoutToPx(3.em)
        )
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        // this should overflow
        assertThat(textDelegate.didOverflow).isTrue()
    }

    @Test
    fun TextLayoutResult_autoSize_em_withoutToPx_unspecifiedStyleFontSize_checkOverflow() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)
        val text = "Hello World"

        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Clip,
                    autoSize = AutoSizeWithoutToPx(1.em)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        // this shouldn't overflow
        assertThat(textDelegate.didOverflow).isFalse()

        textDelegate.updateAutoSize(
            text = text,
            fontSize = TextUnit.Unspecified,
            autoSize = AutoSizeWithoutToPx(2.em)
        )
        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        // this should overflow
        assertThat(textDelegate.didOverflow).isTrue()
    }

    @Test
    fun TextLayoutResult_autoSize_minLines_greaterThan_1_checkOverflowAndHeight() {
        val constraints = Constraints(minWidth = 0, maxWidth = 100, minHeight = 0, maxHeight = 100)

        val textDelegate =
            ParagraphLayoutCache(
                    text = "H",
                    style = createTextStyle(TextUnit.Unspecified),
                    fontFamilyResolver = fontFamilyResolver,
                    minLines = 2,
                    autoSize = AutoSize.StepBased(20.sp, 51.sp, 1.sp)
                )
                .also { it.density = density }

        textDelegate.layoutWithConstraints(constraints, LayoutDirection.Ltr)
        val layoutResult = textDelegate.paragraph!!
        assertThat(textDelegate.didOverflow).isFalse()
        assertThat(layoutResult.height).isAtMost(55) // this value is different between
        // different API levels. Either 51 or 52. Using isAtMost to anticipate future permutations.
    }

    @Test
    fun maxHeight_hasSameHeight_asParagraph() {
        val text = "a\n".repeat(20)
        val textDelegate =
            ParagraphLayoutCache(
                    text = text,
                    style = createTextStyle(fontSize = 1.sp),
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 5
                )
                .also { it.density = density }
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val actual = textDelegate.paragraph!!

        val expected =
            Paragraph(
                text,
                createTextStyle(fontSize = 1.sp),
                Constraints(),
                density,
                fontFamilyResolver,
                emptyList(),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        assertThat(actual.height).isEqualTo(expected.height)
    }

    @Test
    fun slowCreate_null_beforeLayout() {
        val text = "hello"
        val style = createTextStyle(fontSize = 1.sp)
        val subject =
            ParagraphLayoutCache(text, style, fontFamilyResolver).also { it.density = density }

        assertThat(subject.slowCreateTextLayoutResultOrNull(style = style)).isNull()
    }

    @Test
    fun slowCreate_not_null_afterLayout() {
        val text = "hello"
        val style = createTextStyle(fontSize = 1.sp)
        val subject =
            ParagraphLayoutCache(text, style, fontFamilyResolver).also { it.density = density }

        subject.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        assertThat(subject.slowCreateTextLayoutResultOrNull(style = style)).isNotNull()
    }

    @Test
    fun slowCreate_not_null_afterLayout_minWidthMinHeight() {
        val text = "hello"
        val style = createTextStyle(fontSize = 1.sp)
        val subject =
            ParagraphLayoutCache(text, style, fontFamilyResolver).also { it.density = density }

        subject.layoutWithConstraints(Constraints(minWidth = 5, minHeight = 5), LayoutDirection.Ltr)
        assertThat(subject.slowCreateTextLayoutResultOrNull(style = style)).isNotNull()
    }

    @Test
    fun hugeString_doesntCrash() {
        val text = "A".repeat(100_000)
        val style = createTextStyle(fontSize = 100.sp)
        val subject =
            ParagraphLayoutCache(text, style, fontFamilyResolver).also { it.density = density }
        subject.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
    }

    private fun createTextStyle(
        fontSize: TextUnit,
        letterSpacing: TextUnit = TextUnit.Unspecified
    ): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing
        )
    }

    private fun ParagraphLayoutCache.updateAutoSize(
        text: String,
        fontSize: TextUnit,
        autoSize: AutoSize
    ) =
        update(
            text = text,
            style = createTextStyle(fontSize),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = Int.MAX_VALUE,
            minLines = DefaultMinLines,
            autoSize = autoSize
        )

    private class AutoSizePreset(private val presets: Array<TextUnit>) : AutoSize {
        override fun FontSizeSearchScope.getFontSize(): TextUnit {
            var optimalFontSize = 0.sp
            for (size in presets) {
                if (
                    size.toPx() > optimalFontSize.toPx() &&
                        !performLayoutAndGetOverflow(size.toPx().toSp())
                ) {
                    optimalFontSize = size
                }
            }
            return if (optimalFontSize != 0.sp) optimalFontSize else 100.sp
            // 100.sp is the font size returned when all sizes in the presets array overflow
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AutoSizePreset

            return presets.contentEquals(other.presets)
        }

        override fun hashCode(): Int {
            return presets.contentHashCode()
        }
    }
}

private class AutoSizeWithoutToPx(private val fontSize: TextUnit) : AutoSize {
    override fun FontSizeSearchScope.getFontSize(): TextUnit {
        // if there is overflow then 100.sp is returned. Otherwise 0.sp is returned
        if (performLayoutAndGetOverflow(fontSize)) return 100.sp
        return 0.sp
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AutoSizeWithoutToPx

        return fontSize == other.fontSize
    }

    override fun hashCode(): Int {
        return fontSize.hashCode()
    }
}
