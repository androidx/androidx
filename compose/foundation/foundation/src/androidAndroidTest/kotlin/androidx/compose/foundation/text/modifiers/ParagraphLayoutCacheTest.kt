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

import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.toIntPx
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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
            val textDelegate = ParagraphLayoutCache(
                text = text,
                style = TextStyle(fontSize = fontSize, fontFamily = fontFamily),
                fontFamilyResolver = fontFamilyResolver,
            ).also {
                it.density = this
            }

            assertThat(textDelegate.minIntrinsicWidth(LayoutDirection.Ltr))
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val textDelegate = ParagraphLayoutCache(
                text = text,
                style = TextStyle(fontSize = fontSize, fontFamily = fontFamily),
                fontFamilyResolver = fontFamilyResolver,
            ).also {
                it.density = this
            }

            assertThat(textDelegate.maxIntrinsicWidth(LayoutDirection.Ltr))
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() {
        val textDelegate = ParagraphLayoutCache(
            text = "Hello World",
            style = TextStyle.Default,
            fontFamilyResolver = fontFamilyResolver,
        ).also {
            it.density = density
        }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layoutSize

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(
            constraintsSecondLayout,
            LayoutDirection.Ltr
        )
        val resultSecondLayout = textDelegate.layoutSize

        assertThat(resultFirstLayout.height).isLessThan(resultSecondLayout.height)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() {
        val textDelegate = ParagraphLayoutCache(
            text = "Hello World",
            style = TextStyle.Default,
            fontFamilyResolver = fontFamilyResolver,
        ).also {
            it.density = density
        }
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        textDelegate.layoutWithConstraints(constraintsFirstLayout, LayoutDirection.Ltr)
        val resultFirstLayout = textDelegate.layoutSize
        assertThat(resultFirstLayout.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        textDelegate.layoutWithConstraints(
            constraintsSecondLayout,
            LayoutDirection.Ltr
        )
        val resultSecondLayout = textDelegate.layoutSize
        assertThat(resultSecondLayout.height).isEqualTo(heightSecondLayout)
    }

    @Test
    fun TextLayoutResult_layout_withEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val textDelegate = ParagraphLayoutCache(
            text = "Hello World! Hello World! Hello World! Hello World!",
            style = TextStyle(fontSize = fontSize.sp),
            fontFamilyResolver = fontFamilyResolver,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        ).also {
            it.density = density
        }

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

        val textDelegate = ParagraphLayoutCache(
            text = "Hello World! Hello World! Hello World! Hello World!",
            style = TextStyle(fontSize = fontSize.sp),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Ellipsis,
        ).also {
            it.density = density
        }
        textDelegate.layoutWithConstraints(Constraints.fixed(0, 0), LayoutDirection.Ltr)
        val constraints = Constraints(
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

        val textDelegate = ParagraphLayoutCache(
            text = "Hello World",
            style = TextStyle(fontSize = fontSize.sp, letterSpacing = 0.5.sp),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Ellipsis,
        ).also {
            it.density = density
        }

        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val layoutResultLtr = textDelegate.layoutSize
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Rtl)
        val layoutResultRtl = textDelegate.layoutSize

        assertThat(layoutResultLtr.width).isEqualTo(layoutResultRtl.width)
    }

    @Test
    fun maxHeight_hasSameHeight_asParagraph() {
        val text = "a\n".repeat(20)
        val textDelegate = ParagraphLayoutCache(
            text = text,
            style = TextStyle(fontSize = 1.sp),
            fontFamilyResolver = fontFamilyResolver,
            overflow = TextOverflow.Ellipsis,
            maxLines = 5
        ).also {
            it.density = density
        }
        textDelegate.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        val actual = textDelegate.paragraph!!

        val expected = Paragraph(
            text,
            TextStyle(fontSize = 1.sp),
            Constraints(),
            density,
            fontFamilyResolver,
            emptyList(),
            maxLines = 5,
            ellipsis = true
        )
        assertThat(actual.height).isEqualTo(expected.height)
    }

    @Test
    fun slowCreate_null_beforeLayout() {
        val text = "hello"
        val subject = ParagraphLayoutCache(
            text,
            TextStyle(fontSize = 1.sp),
            fontFamilyResolver
        ).also {
            it.density = density
        }

        assertThat(subject.slowCreateTextLayoutResultOrNull()).isNull()
    }

    @Test
    fun slowCreate_not_null_afterLayout() {
        val text = "hello"
        val subject = ParagraphLayoutCache(
            text,
            TextStyle(fontSize = 1.sp),
            fontFamilyResolver
        ).also {
            it.density = density
        }

        subject.layoutWithConstraints(Constraints(), LayoutDirection.Ltr)
        assertThat(subject.slowCreateTextLayoutResultOrNull()).isNotNull()
    }

    @Test
    fun slowCreate_not_null_afterLayout_minWidthMinHeight() {
        val text = "hello"
        val subject = ParagraphLayoutCache(
            text,
            TextStyle(fontSize = 1.sp),
            fontFamilyResolver
        ).also {
            it.density = density
        }

        subject.layoutWithConstraints(Constraints(minWidth = 5, minHeight = 5), LayoutDirection.Ltr)
        assertThat(subject.slowCreateTextLayoutResultOrNull()).isNotNull()
    }
}
