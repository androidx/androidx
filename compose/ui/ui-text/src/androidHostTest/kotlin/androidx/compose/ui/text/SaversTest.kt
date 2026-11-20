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

package androidx.compose.ui.text

import android.text.Spannable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.createCharSequence
import androidx.compose.ui.text.platform.extensions.correctBlurRadius
import androidx.compose.ui.text.platform.extensions.flattenFontStylesAndApply
import androidx.compose.ui.text.platform.extensions.hasFontAttributes
import androidx.compose.ui.text.platform.extensions.setBulletSpans
import androidx.compose.ui.text.platform.extensions.setSpanStyles
import androidx.compose.ui.text.platform.hasMetricAffectingSpanStyle
import androidx.compose.ui.text.platform.isIncludeFontPaddingEnabled
import androidx.compose.ui.text.platform.style.CustomBulletSpan
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
@Suppress("Deprecation")
class SaversTest {
    private val defaultSaverScope = SaverScope { true }

    @Test
    fun test_TextUnit() {
        val original = 2.sp
        val saved = save(original, TextUnit.Saver, defaultSaverScope)
        val restored: TextUnit? = restore(saved, TextUnit.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextUnit_unspecified() {
        val original = TextUnit.Unspecified
        val saved = save(original, TextUnit.Saver, defaultSaverScope)
        val restored: TextUnit? = restore(saved, TextUnit.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Offset() {
        val original = Offset(10f, 10f)
        val saved = save(original, Offset.Saver, defaultSaverScope)
        val restored: Offset? = restore(saved, Offset.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Offset_Unspecified() {
        val original = Offset.Unspecified
        val saved = save(original, Offset.Saver, defaultSaverScope)
        val restored: Offset? = restore(saved, Offset.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Offset_Infinite() {
        val original = Offset.Infinite
        val saved = save(original, Offset.Saver, defaultSaverScope)
        val restored: Offset? = restore(saved, Offset.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Color() {
        val original = Color.Yellow
        val saved = save(original, Color.Saver, defaultSaverScope)
        val restored: Color? = restore(saved, Color.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Color_Unspecified() {
        val original = Color.Unspecified
        val saved = save(original, Color.Saver, defaultSaverScope)
        val restored: Color? = restore(saved, Color.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Shadow() {
        val original = Shadow(color = Color.Blue, offset = Offset(5f, 5f), blurRadius = 2f)
        val saved = save(original, Shadow.Saver, defaultSaverScope)
        val restored: Shadow? = restore(saved, Shadow.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Shadow_None() {
        val original = Shadow.None
        val saved = save(original, Shadow.Saver, defaultSaverScope)
        val restored: Shadow? = restore(saved, Shadow.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_ParagraphStyle() {
        val original = ParagraphStyle()
        val saved = save(original, ParagraphStyleSaver, defaultSaverScope)
        val restored: ParagraphStyle? = restore(saved, ParagraphStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_ParagraphStyle_with_a_nonnull_value() {
        val original = ParagraphStyle(textDirection = TextDirection.Rtl)
        val saved = save(original, ParagraphStyleSaver, defaultSaverScope)
        val restored: ParagraphStyle? = restore(saved, ParagraphStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_ParagraphStyle_with_no_null_value() {
        val original =
            ParagraphStyle(
                textAlign = TextAlign.Justify,
                textDirection = TextDirection.Rtl,
                lineHeight = 10.sp,
                textIndent = TextIndent(firstLine = 2.sp, restLine = 3.sp),
                platformStyle = PlatformParagraphStyle.Default,
                lineHeightStyle = LineHeightStyle.Default,
                lineBreak = LineBreak.Paragraph,
                hyphens = Hyphens.Auto,
                textMotion = TextMotion.Animated,
            )
        val saved = save(original, ParagraphStyleSaver, defaultSaverScope)
        val restored: ParagraphStyle? = restore(saved, ParagraphStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun paragraphStyleSaver_restoresBoth5ElementAnd9ElementLists() {
        val customPlatform =
            PlatformParagraphStyle(
                includeFontPadding = true,
                emojiSupportMatch = EmojiSupportMatch.All,
            )
        val customMotion =
            TextMotion(
                linearity = TextMotion.Linearity.FontHinting,
                subpixelTextPositioning = true,
            )
        val samples =
            listOf(
                ParagraphStyle(),
                ParagraphStyle(
                    textAlign = TextAlign.Justify,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 10.sp,
                    textIndent = TextIndent.None,
                    platformStyle = PlatformParagraphStyle.Default,
                    lineHeightStyle = LineHeightStyle.Default,
                    lineBreak = LineBreak.Paragraph,
                    hyphens = Hyphens.Auto,
                    textMotion = TextMotion.Animated,
                ),
                ParagraphStyle(
                    textAlign = TextAlign.End,
                    textDirection = TextDirection.ContentOrLtr,
                    lineHeight = 1.5.em,
                    textIndent = TextIndent(firstLine = 2.sp, restLine = 3.sp),
                    platformStyle = customPlatform,
                    lineHeightStyle =
                        LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Top,
                            trim = LineHeightStyle.Trim.FirstLineTop,
                            mode = LineHeightStyle.Mode.Tight,
                        ),
                    lineBreak = LineBreak.Heading,
                    hyphens = Hyphens.None,
                    textMotion = customMotion,
                ),
            )

        for (original in samples) {
            val other =
                ParagraphStyle(
                    textAlign = TextAlign.Start,
                    lineHeight = if (original.lineHeight.isEm) 2.em else 20.sp,
                )

            // 9-element legacy list
            val legacyList =
                arrayListOf(
                    save(original.textAlign, TextAlign.Saver, defaultSaverScope),
                    save(original.textDirection, TextDirection.Saver, defaultSaverScope),
                    save(original.lineHeight, TextUnit.Saver, defaultSaverScope),
                    save(original.textIndent, TextIndent.Saver, defaultSaverScope),
                    save(original.platformStyle, PlatformParagraphStyle.Saver, defaultSaverScope),
                    save(original.lineHeightStyle, LineHeightStyle.Saver, defaultSaverScope),
                    save(original.lineBreak, LineBreak.Saver, defaultSaverScope),
                    save(original.hyphens, Hyphens.Saver, defaultSaverScope),
                    save(original.textMotion, TextMotion.Saver, defaultSaverScope),
                )

            // 5-element packed list
            val packedSource =
                ParagraphStyle(
                    bits =
                        packParagraphStyleBits(
                            original.textAlign,
                            original.textDirection,
                            original.lineHeight,
                            original.textIndent,
                            original.platformStyle,
                            original.lineBreak,
                            original.hyphens,
                            original.textMotion,
                        ),
                    lineHeightStyle = original.lineHeightStyle,
                    extras =
                        createParagraphStyleExtras(
                            original.textIndent,
                            original.platformStyle,
                            original.textMotion,
                        ),
                )
            val packedList =
                arrayListOf(
                    packedSource.bits,
                    save(packedSource.extras?.textIndent, TextIndent.Saver, defaultSaverScope),
                    save(
                        packedSource.extras?.platformStyle,
                        PlatformParagraphStyle.Saver,
                        defaultSaverScope,
                    ),
                    save(packedSource.lineHeightStyle, LineHeightStyle.Saver, defaultSaverScope),
                    save(packedSource.extras?.textMotion, TextMotion.Saver, defaultSaverScope),
                )

            for (payload in listOf(legacyList, packedList)) {
                val restored = ParagraphStyleSaver.restore(payload)!!
                assertThat(restored).isEqualTo(original)
                assertThat(original).isEqualTo(restored)
                assertThat(restored.hashCode()).isEqualTo(original.hashCode())
                assertThat(restored.legacyParagraphStyle != null)
                    .isEqualTo(!PackedTextStylesLatch.isEnabled)
                assertThat(other.merge(restored)).isEqualTo(other.merge(original))
                assertThat(lerp(other, restored, 0.5f)).isEqualTo(lerp(other, original, 0.5f))
            }
        }
    }

    @Test
    fun test_SpanStyle() {
        val original = SpanStyle()
        val saved = save(original, SpanStyleSaver, defaultSaverScope)
        val restored: SpanStyle? = restore(saved, SpanStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_SpanStyle_with_a_nonnull_value() {
        val original = SpanStyle(baselineShift = BaselineShift.Subscript)
        val saved = save(original, SpanStyleSaver, defaultSaverScope)
        val restored: SpanStyle? = restore(saved, SpanStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_SpanStyle_with_no_null_value() {
        val original =
            SpanStyle(
                color = Color.Red,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                // fontFamily =
                fontFeatureSettings = "feature settings",
                letterSpacing = 2.em,
                baselineShift = BaselineShift.Superscript,
                textGeometricTransform = TextGeometricTransform(2f, 3f),
                localeList = LocaleList(Locale("sr-Latn-SR"), Locale("sr-Cyrl-SR"), Locale("en")),
                background = Color.Blue,
                textDecoration = TextDecoration.LineThrough,
                shadow = Shadow(color = Color.Red, offset = Offset(2f, 2f), blurRadius = 4f),
            )
        val saved = save(original, SpanStyleSaver, defaultSaverScope)
        val restored: SpanStyle? = restore(saved, SpanStyleSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextLinkStyles() {
        val original = TextLinkStyles(null)
        val saved = save(original, TextLinkStylesSaver, defaultSaverScope)
        val restored: TextLinkStyles? = restore(saved, TextLinkStylesSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextLinkStyles_withNonNullValues() {
        val original =
            TextLinkStyles(
                SpanStyle(color = Color.Red),
                SpanStyle(color = Color.Green),
                SpanStyle(color = Color.Blue),
                SpanStyle(color = Color.Gray),
            )
        val saved = save(original, TextLinkStylesSaver, defaultSaverScope)
        val restored: TextLinkStyles? = restore(saved, TextLinkStylesSaver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_FontWeight() {
        val original = FontWeight(123)
        val saved = save(original, FontWeight.Saver, defaultSaverScope)
        val restored: FontWeight? = restore(saved, FontWeight.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_FontWeight_w100() {
        val original = FontWeight.W100
        val saved = save(original, FontWeight.Saver, defaultSaverScope)
        val restored: FontWeight? = restore(saved, FontWeight.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_BaselineShift() {
        val original = BaselineShift(2f)
        val saved = save(original, BaselineShift.Saver, defaultSaverScope)
        val restored: BaselineShift? = restore(saved, BaselineShift.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_BaselineShift_None() {
        val original = BaselineShift.None
        val saved = save(original, BaselineShift.Saver, defaultSaverScope)
        val restored: BaselineShift? = restore(saved, BaselineShift.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextDecoration() {
        val original =
            TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
        val saved = save(original, TextDecoration.Saver, defaultSaverScope)
        val restored: TextDecoration? = restore(saved, TextDecoration.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextDecoration_None() {
        val original = TextDecoration.None
        val saved = save(original, TextDecoration.Saver, defaultSaverScope)
        val restored: TextDecoration? = restore(saved, TextDecoration.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun testSaveRestore_lineThrough() {
        val original = TextDecoration.LineThrough
        val saved = save(original, TextDecoration.Saver, defaultSaverScope)
        val restored: TextDecoration? = restore(saved, TextDecoration.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun testSaveRestore_underline() {
        val original = TextDecoration.Underline
        val saved = save(original, TextDecoration.Saver, defaultSaverScope)
        val restored: TextDecoration? = restore(saved, TextDecoration.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextGeometricTransform() {
        val original = TextGeometricTransform(1f, 2f)
        val saved = save(original, TextGeometricTransform.Saver, defaultSaverScope)
        val restored: TextGeometricTransform? = restore(saved, TextGeometricTransform.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextGeometricTransform_None() {
        val original = TextGeometricTransform.None
        val saved = save(original, TextGeometricTransform.Saver, defaultSaverScope)
        val restored: TextGeometricTransform? = restore(saved, TextGeometricTransform.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextIndent() {
        val original = TextIndent(1.sp, 2.sp)
        val saved = save(original, TextIndent.Saver, defaultSaverScope)
        val restored: TextIndent? = restore(saved, TextIndent.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextIndent_None() {
        val original = TextIndent.None
        val saved = save(original, TextIndent.Saver, defaultSaverScope)
        val restored: TextIndent? = restore(saved, TextIndent.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotatedString() {
        val original = AnnotatedString("abc")
        val saved = with(AnnotatedStringSaver) { defaultSaverScope.save(original) }

        assertThat(AnnotatedStringSaver.restore(saved!!)).isEqualTo(original)
    }

    @Test
    fun test_AnnotatedString_withSpanStyles() {
        val original = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("1") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("2") }
        }

        val saved = with(AnnotatedStringSaver) { defaultSaverScope.save(original) }

        val restored: AnnotatedString = AnnotatedStringSaver.restore(saved!!)!!
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotatedString_withParagraphStyles() {
        val original = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) { append("1") }
            withStyle(ParagraphStyle(textDirection = TextDirection.Rtl)) { append("2") }
        }

        val saved = with(AnnotatedStringSaver) { defaultSaverScope.save(original) }

        val restored: AnnotatedString = AnnotatedStringSaver.restore(saved!!)!!
        assertThat(restored).isEqualTo(original)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_AnnotatedString_withAnnotations() {
        val original = buildAnnotatedString {
            withAnnotation(tag = "Tag1", annotation = "Annotation1") { append("1") }
            withAnnotation(VerbatimTtsAnnotation("verbatim1")) { append("2") }
            withAnnotation(tag = "Tag2", annotation = "Annotation2") { append("3") }
            withAnnotation(VerbatimTtsAnnotation("verbatim2")) { append("4") }
            withAnnotation(UrlAnnotation("url1")) { append("5") }
            withAnnotation(UrlAnnotation("url2")) { append("6") }
            withLink(
                LinkAnnotation.Url(
                    "url3",
                    TextLinkStyles(
                        SpanStyle(color = Color.Red),
                        SpanStyle(color = Color.Green),
                        SpanStyle(color = Color.Blue),
                        SpanStyle(color = Color.White),
                    ),
                )
            ) {
                append("7")
            }
            withLink(
                LinkAnnotation.Clickable(
                    "tag3",
                    TextLinkStyles(
                        SpanStyle(color = Color.Red),
                        SpanStyle(color = Color.Green),
                        SpanStyle(color = Color.Blue),
                        SpanStyle(background = Color.Gray),
                    ),
                    null,
                )
            ) {
                append("8")
            }
        }

        val saved = with(AnnotatedStringSaver) { defaultSaverScope.save(original) }

        val restored: AnnotatedString = AnnotatedStringSaver.restore(saved!!)!!

        assertThat(restored).isEqualTo(original)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_AnnotatedString_withSpanAndParagraphStylesAndAnnotations() {
        val original = buildAnnotatedString {
            withStyle(ParagraphStyle(textAlign = TextAlign.Justify)) { append("1") }
            withStyle(ParagraphStyle(textDirection = TextDirection.Rtl)) { append("2") }
            withStyle(SpanStyle(color = Color.Red)) { append("3") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("4") }
            withAnnotation(tag = "Tag1", annotation = "Annotation1") { append("5") }
            withAnnotation(VerbatimTtsAnnotation("verbatim1")) { append("6") }
            withAnnotation(tag = "Tag2", annotation = "Annotation2") { append("7") }
            withAnnotation(VerbatimTtsAnnotation("verbatim2")) { append("8") }
            withAnnotation(UrlAnnotation("url1")) { append("9") }
            withAnnotation(UrlAnnotation("url2")) { append("10") }
            withLink(
                LinkAnnotation.Url(
                    "url3",
                    TextLinkStyles(
                        SpanStyle(color = Color.Red),
                        SpanStyle(color = Color.Green),
                        SpanStyle(color = Color.Blue),
                        SpanStyle(color = Color.Yellow),
                    ),
                )
            ) {
                append("11")
            }
            withLink(
                LinkAnnotation.Clickable(
                    "tag3",
                    TextLinkStyles(
                        SpanStyle(color = Color.Red),
                        SpanStyle(color = Color.Green),
                        SpanStyle(color = Color.Blue),
                        SpanStyle(color = Color.Gray),
                    ),
                    null,
                )
            ) {
                append("12")
            }
        }

        val saved = with(AnnotatedStringSaver) { defaultSaverScope.save(original) }

        val restored: AnnotatedString = AnnotatedStringSaver.restore(saved!!)!!
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_Locale() {
        val original = Locale("sr-Latn-SR")
        val saved = with(Locale.Saver) { defaultSaverScope.save(original) }

        assertThat(Locale.Saver.restore(saved!!)).isEqualTo(original)
    }

    @Test
    fun test_LocaleList() {
        val original = LocaleList(Locale("sr-Latn-SR"), Locale("sr-Cyrl-SR"), Locale("en-US"))
        val saved = with(LocaleList.Saver) { defaultSaverScope.save(original) }

        assertThat(LocaleList.Saver.restore(saved!!)).isEqualTo(original)
    }

    @Test
    fun test_PlatformParagraphStyle() {
        val original = PlatformParagraphStyle.Default
        val saved = save(original, PlatformParagraphStyle.Saver, defaultSaverScope)
        val restored: PlatformParagraphStyle? = restore(saved, PlatformParagraphStyle.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_PlatformParagraphStyle_with_no_null_args() {
        val original = PlatformParagraphStyle(EmojiSupportMatch.All, true)
        val saved = save(original, PlatformParagraphStyle.Saver, defaultSaverScope)
        val restored: PlatformParagraphStyle? = restore(saved, PlatformParagraphStyle.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_LineHeightStyle() {
        val original =
            LineHeightStyle(
                LineHeightStyle.Alignment.Proportional,
                LineHeightStyle.Trim.Both,
                LineHeightStyle.Mode.Minimum,
            )
        val saved = save(original, LineHeightStyle.Saver, defaultSaverScope)
        val restored: LineHeightStyle? = restore(saved, LineHeightStyle.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_LineBreak() {
        val original = LineBreak.Paragraph
        val saved = save(original, LineBreak.Saver, defaultSaverScope)
        val restored: LineBreak? = restore(saved, LineBreak.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_TextMotion() {
        val original = TextMotion.Animated
        val saved = save(original, TextMotion.Saver, defaultSaverScope)
        val restored: TextMotion? = restore(saved, TextMotion.Saver)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotationSaver_ParagraphStyle() {
        val original = ParagraphStyle(textAlign = TextAlign.Center)
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: ParagraphStyle? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotationSaver_SpanStyle() {
        val original = SpanStyle(color = Color.Red)
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: SpanStyle? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotationSaver_VerbatimTtsAnnotation() {
        val original = VerbatimTtsAnnotation("verbatim")
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: VerbatimTtsAnnotation? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    @OptIn(ExperimentalTextApi::class)
    @Test
    fun test_AnnotationSaver_UrlAnnotation() {
        val original = UrlAnnotation("url")
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: UrlAnnotation? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotationSaver_LinkAnnotationUrl() {
        val original = LinkAnnotation.Url("url", TextLinkStyles(SpanStyle(color = Color.Red)))
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: LinkAnnotation.Url? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotationSaver_LinkAnnotationClickable() {
        val original =
            LinkAnnotation.Clickable("tag", TextLinkStyles(SpanStyle(color = Color.Red)), null)
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: LinkAnnotation.Clickable? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun test_AnnotationSaver_StringAnnotation() {
        val original = StringAnnotation("string")
        val saved = save(original, AnnotatedString.Annotation.Saver, defaultSaverScope)
        val restored: StringAnnotation? = restore(saved, AnnotatedString.Annotation.Saver)
        assertThat(restored).isEqualTo(original)
    }

    private fun <Original, Saveable : Any> assertSaverRoundTrip(
        saver: Saver<Original, Saveable>,
        vararg values: Original,
    ) {
        for (original in values) {
            val saved = with(saver) { defaultSaverScope.save(original) }!!
            val restored = saver.restore(saved)
            assertThat(restored).isEqualTo(original)
        }
    }

    @Test
    fun orphanedValueClassSavers_roundTripAllValues() {
        assertSaverRoundTrip(
            TextAlign.Saver,
            TextAlign.Left,
            TextAlign.Right,
            TextAlign.Center,
            TextAlign.Justify,
            TextAlign.Start,
            TextAlign.End,
            TextAlign.Unspecified,
        )
        assertSaverRoundTrip(
            TextDirection.Saver,
            TextDirection.Ltr,
            TextDirection.Rtl,
            TextDirection.Content,
            TextDirection.ContentOrLtr,
            TextDirection.ContentOrRtl,
            TextDirection.Unspecified,
        )
        assertSaverRoundTrip(Hyphens.Saver, Hyphens.None, Hyphens.Auto, Hyphens.Unspecified)
        assertSaverRoundTrip(FontStyle.Saver, FontStyle.Normal, FontStyle.Italic)
        assertSaverRoundTrip(
            FontSynthesis.Saver,
            FontSynthesis.None,
            FontSynthesis.Weight,
            FontSynthesis.Style,
            FontSynthesis.All,
        )
        assertSaverRoundTrip(
            TextRange.Saver,
            TextRange.Zero,
            TextRange(0, 10),
            TextRange(7, 7),
            TextRange(12, 3),
        )
        assertSaverRoundTrip(
            EmojiSupportMatch.Saver,
            EmojiSupportMatch.Default,
            EmojiSupportMatch.None,
            EmojiSupportMatch.All,
        )
        assertSaverRoundTrip(
            TextUnitType.Saver,
            TextUnitType.Unspecified,
            TextUnitType.Sp,
            TextUnitType.Em,
        )
        assertSaverRoundTrip(
            TextUnit.Saver,
            TextUnit.Unspecified,
            0.sp,
            14.5.sp,
            2.25.em,
            (-1.5).em,
        )
    }

    @Test
    fun colorLocaleListFontWeightBaselineShiftLineHeightStyleAndTextMotion_edgeCases() {
        val p3Color = Color(0.8f, 0.2f, 0.4f, 0.9f, ColorSpaces.DisplayP3)
        val savedP3 = with(Color.Saver) { defaultSaverScope.save(p3Color) }!!
        assertThat(savedP3).isEqualTo(p3Color.toArgb())
        assertThat(Color.Saver.restore(savedP3)).isEqualTo(Color(p3Color.toArgb()))
        assertSaverRoundTrip(
            Color.Saver,
            Color.Unspecified,
            Color(0xFF112233),
        )
        assertSaverRoundTrip(LocaleList.Saver, LocaleList.Empty)
        assertSaverRoundTrip(FontWeight.Saver, FontWeight(345))
        assertSaverRoundTrip(
            LineHeightStyle.Saver,
            LineHeightStyle(
                alignment = LineHeightStyle.Alignment(0.25f),
                trim = LineHeightStyle.Trim.FirstLineTop,
                mode = LineHeightStyle.Mode.Minimum,
            ),
        )

        for (shift in listOf(BaselineShift(-0.0f), BaselineShift(0.37f))) {
            val savedShift = with(BaselineShift.Saver) { defaultSaverScope.save(shift) }!!
            val restoredShift = BaselineShift.Saver.restore(savedShift)!!
            assertThat(restoredShift.multiplier.toRawBits()).isEqualTo(shift.multiplier.toRawBits())
        }

        val linearities =
            listOf(
                TextMotion.Linearity.Linear,
                TextMotion.Linearity.FontHinting,
                TextMotion.Linearity.None,
            )
        for (linearity in linearities) {
            for (subpixel in listOf(true, false)) {
                assertSaverRoundTrip(TextMotion.Saver, TextMotion(linearity, subpixel))
            }
        }
    }

    @Test
    fun paragraphStyleSaver_presetColdFields_keepExtrasNull() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val presetStyle =
            ParagraphStyle(
                textAlign = TextAlign.Justify,
                textDirection = TextDirection.ContentOrRtl,
                lineHeight = 20.sp,
                textIndent = TextIndent.None,
                platformStyle = PlatformParagraphStyle.Default,
                lineHeightStyle = LineHeightStyle.Default,
                lineBreak = LineBreak.Heading,
                hyphens = Hyphens.Auto,
                textMotion = TextMotion.Static,
            )
        assertThat(presetStyle.extras).isNull()

        val saved = with(ParagraphStyleSaver) { defaultSaverScope.save(presetStyle) }!!
        val restored = ParagraphStyleSaver.restore(saved)!!

        assertThat(restored).isEqualTo(presetStyle)
        assertThat(restored.extras).isNull()
        assertThat(restored.bits and 1L).isEqualTo(0L)
        assertThat(restored.textIndent).isEqualTo(TextIndent.None)
        assertThat(restored.platformStyle).isEqualTo(PlatformParagraphStyle.Default)
        assertThat(restored.textMotion).isEqualTo(TextMotion.Static)
    }

    @Test
    fun paragraphStyleSaver_nonPresetColdExtras_roundTripsEachColdField() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val coldPlatform =
            PlatformParagraphStyle(
                includeFontPadding = true,
                emojiSupportMatch = EmojiSupportMatch.All,
            )
        val coldMotion =
            TextMotion(
                linearity = TextMotion.Linearity.FontHinting,
                subpixelTextPositioning = true,
            )
        val coldIndent = TextIndent(firstLine = 12.sp, restLine = 4.em)

        val permutations =
            listOf(
                ParagraphStyle(textIndent = coldIndent),
                ParagraphStyle(platformStyle = coldPlatform),
                ParagraphStyle(textMotion = coldMotion),
                ParagraphStyle(
                    textAlign = TextAlign.End,
                    textDirection = TextDirection.Rtl,
                    lineHeight = 1.5.em,
                    textIndent = coldIndent,
                    platformStyle = coldPlatform,
                    lineHeightStyle =
                        LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Top,
                            trim = LineHeightStyle.Trim.FirstLineTop,
                            mode = LineHeightStyle.Mode.Tight,
                        ),
                    lineBreak = LineBreak.Paragraph,
                    hyphens = Hyphens.None,
                    textMotion = coldMotion,
                ),
            )

        for (style in permutations) {
            assertThat(style.extras).isNotNull()
            val saved = with(ParagraphStyleSaver) { defaultSaverScope.save(style) }!!
            val restored = ParagraphStyleSaver.restore(saved)!!
            assertThat(restored).isEqualTo(style)
            assertThat(restored.extras).isNotNull()
            assertThat(restored.bits and 1L).isEqualTo(1L)
            assertThat(restored.textIndent).isEqualTo(style.textIndent)
            assertThat(restored.platformStyle).isEqualTo(style.platformStyle)
            assertThat(restored.textMotion).isEqualTo(style.textMotion)
        }
    }

    @Test
    fun spanStyleSaver_unsavedSpanBits_clearedOnRestoreWhenNoSavedColdFieldsExist() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val customShaderBrush =
            object : ShaderBrush() {
                override fun createShader(size: Size): Shader =
                    throw UnsupportedOperationException("Not needed on host")
            }

        val unsavedOnlyStyles =
            listOf(
                SpanStyle(fontFamily = FontFamily.Monospace),
                SpanStyle(platformStyle = PlatformSpanStyle.Default),
                SpanStyle(drawStyle = Fill),
                SpanStyle(drawStyle = Stroke(width = 3f)),
                SpanStyle(brush = customShaderBrush, alpha = 0.75f),
                SpanStyle(
                    color = Color.Green,
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.Weight,
                    fontFamily = FontFamily.SansSerif,
                    platformStyle = PlatformSpanStyle.Default,
                    drawStyle = Stroke(width = 2f),
                ),
            )

        for (style in unsavedOnlyStyles) {
            val saved = with(SpanStyleSaver) { defaultSaverScope.save(style) }!!
            val restored = SpanStyleSaver.restore(saved)!!

            assertThat(restored.extras).isNull()
            assertThat(restored.bits and UNSAVED_SPAN_BITS).isEqualTo(0)
            assertThat(restored.fontFamily).isNull()
            assertThat(restored.platformStyle).isNull()
            assertThat(restored.drawStyle).isNull()
            assertThat(restored.brush).isNull()
            assertThat(restored.color).isEqualTo(style.color)
            assertThat(restored.fontSize).isEqualTo(style.fontSize)
            assertThat(restored.fontStyle).isEqualTo(style.fontStyle)
            assertThat(restored.fontSynthesis).isEqualTo(style.fontSynthesis)
        }
    }

    @Test
    fun spanStyleSaver_solidColorBrushWithAlpha_andPresetPackedColdFields() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val solidBrushStyle =
            SpanStyle(brush = SolidColor(Color.Red), alpha = 0.5f, fontSize = 14.sp)
        assertThat(solidBrushStyle.extras).isNull()
        assertSaverRoundTrip(SpanStyleSaver, solidBrushStyle)

        val presetSpanStyle =
            SpanStyle(
                color = Color.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.All,
                fontFeatureSettings = "",
                letterSpacing = 0.5.em,
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                background = Color.Transparent,
                textDecoration = TextDecoration.Underline + TextDecoration.LineThrough,
                shadow = Shadow.None,
            )
        assertThat(presetSpanStyle.extras).isNull()

        val saved = with(SpanStyleSaver) { defaultSaverScope.save(presetSpanStyle) }!!
        val restored = SpanStyleSaver.restore(saved)!!

        assertThat(restored).isEqualTo(presetSpanStyle)
        assertThat(restored.extras).isNull()
        assertThat(restored.bits and 1).isEqualTo(0)
        assertThat(restored.fontFeatureSettings).isEqualTo("")
        assertThat(restored.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(restored.textGeometricTransform).isEqualTo(TextGeometricTransform.None)
        assertThat(restored.background).isEqualTo(Color.Transparent)
        assertThat(restored.shadow).isEqualTo(Shadow.None)
    }

    @Test
    fun spanStyleSaver_extrasWithTransparentOrUnspecifiedBackground_andNegativeZeroBaselineShift() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val withTransparentBg =
            SpanStyle(
                fontFeatureSettings = "smcp",
                background = Color.Transparent,
            )
        val withUnspecifiedBg =
            SpanStyle(
                fontFeatureSettings = "smcp",
                background = Color.Unspecified,
            )
        assertSaverRoundTrip(SpanStyleSaver, withTransparentBg, withUnspecifiedBg)

        val negativeZeroShift = SpanStyle(baselineShift = BaselineShift(-0.0f))
        assertThat(negativeZeroShift.extras).isNotNull()
        val saved = with(SpanStyleSaver) { defaultSaverScope.save(negativeZeroShift) }!!
        val restored = SpanStyleSaver.restore(saved)!!
        assertThat(restored).isEqualTo(negativeZeroShift)
        assertThat(restored.baselineShift!!.multiplier.toRawBits()).isEqualTo((-0.0f).toRawBits())
    }

    @Test
    fun spanStyleSaver_shaderBrushInExtras_clearedByUnsavedSpanBitsAndFallsBackToUnspecifiedColor() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val style = SpanStyle(brush = shaderBrush, alpha = 0.8f, fontSize = 16.sp)
        assertThat(style.extras).isNotNull()
        assertThat(style.brush).isEqualTo(shaderBrush)

        val saved = with(SpanStyleSaver) { defaultSaverScope.save(style) }!!
        val restored = SpanStyleSaver.restore(saved)!!

        assertThat(restored.brush).isNull()
        assertThat(restored.color).isEqualTo(Color.Unspecified)
        assertThat(restored.fontSize).isEqualTo(16.sp)
        assertThat(restored.bits and UNSAVED_SPAN_BITS).isEqualTo(0)
        assertThat(restored.extras).isNull()
    }

    @Test
    fun spanStyleSaver_transparentBackgroundWithNullExtras_preservesTransparentViaBits() {
        assumeTrue(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val transparentStyle = SpanStyle(background = Color.Transparent, fontSize = 14.sp)
        assertThat(transparentStyle.extras).isNull()
        assertThat(transparentStyle.background).isEqualTo(Color.Transparent)

        val saved = with(SpanStyleSaver) { defaultSaverScope.save(transparentStyle) }!!
        @Suppress("UNCHECKED_CAST") val savedList = saved as List<Any?>
        // index 9 is false because extras == null
        assertThat(savedList[9]).isEqualTo(false)
        // index 1 preserves HAS_BACKGROUND
        assertThat((savedList[1] as Int) and (1 shl 12)).isNotEqualTo(0)

        val restored = SpanStyleSaver.restore(saved)!!
        assertThat(restored.extras).isNull()
        assertThat(restored.background).isEqualTo(Color.Transparent)
    }

    @Test
    fun spanStyleSaver_restoresBoth14And11ElementLists() {
        // 14-element legacy list format:
        val legacySaved =
            arrayListOf(
                with(Color.Saver) { defaultSaverScope.save(Color.Green) },
                with(TextUnit.Saver) { defaultSaverScope.save(18.sp) },
                null,
                null,
                null,
                -1,
                null,
                with(TextUnit.Saver) { defaultSaverScope.save(0.sp) },
                null,
                null,
                null,
                with(Color.Saver) { defaultSaverScope.save(Color.Unspecified) },
                null,
                null,
            )
        assertThat(legacySaved.size).isEqualTo(14)
        val restoredFromLegacy = SpanStyleSaver.restore(legacySaved)!!
        assertThat(restoredFromLegacy.color).isEqualTo(Color.Green)
        assertThat(restoredFromLegacy.fontSize).isEqualTo(18.sp)

        // 11-element packed list format:
        val packedExpected =
            SpanStyle(
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.Weight,
                fontFeatureSettings = "smcp",
                baselineShift = BaselineShift.None,
                background = Color.Transparent,
                textDecoration = TextDecoration.Underline,
            )
        val packedList =
            arrayListOf<Any?>(
                packSpanColorXor(Color.Red),
                packSpanStyleBits(
                    fontStyle = FontStyle.Italic,
                    fontSynthesis = FontSynthesis.Weight,
                    fontFeatureSettings = "smcp",
                    textGeometricTransform = null,
                    background = Color.Transparent,
                    textDecoration = TextDecoration.Underline,
                    shadow = null,
                    platformStyle = null,
                    drawStyle = null,
                    baselineShift = BaselineShift.None,
                ),
                save(14.sp, TextUnit.Saver, defaultSaverScope),
                save(TextUnit.Unspecified, TextUnit.Saver, defaultSaverScope),
                save(FontWeight.Bold, FontWeight.Saver, defaultSaverScope),
                "smcp",
                false,
                false,
                false,
                false,
                false,
            )
        assertThat(packedList.size).isEqualTo(11)
        val restoredFromPacked = SpanStyleSaver.restore(packedList)!!
        assertThat(restoredFromPacked).isEqualTo(packedExpected)
        assertThat(restoredFromPacked.legacySpanStyle != null)
            .isEqualTo(!PackedTextStylesLatch.isEnabled)
        assertThat(SpanStyle(fontSize = 20.sp).merge(restoredFromPacked).color).isEqualTo(Color.Red)
        assertThat(lerp(SpanStyle(color = Color.Blue), restoredFromPacked, 0.75f))
            .isEqualTo(lerp(SpanStyle(color = Color.Blue), packedExpected, 0.75f))

        if (PackedTextStylesLatch.isEnabled) {
            val savedPacked =
                with(SpanStyleSaver) {
                    defaultSaverScope.save(SpanStyle(color = Color.Red, fontSize = 14.sp))
                }!!
            @Suppress("UNCHECKED_CAST") val savedPackedList = savedPacked as List<Any?>
            assertThat(savedPackedList.size).isEqualTo(11)
            assertThat(savedPackedList[0]).isEqualTo(packSpanColorXor(Color.Red))
        }

        // Restore gracefully accepts legacy Int at index 0 from previous runs
        val savedWithInt = packedList.toMutableList().apply { set(0, Color.Red.toArgb()) }
        val restoredFromInt = SpanStyleSaver.restore(savedWithInt)!!
        assertThat(restoredFromInt.color).isEqualTo(Color.Red)
        assertThat(restoredFromInt.fontSize).isEqualTo(14.sp)

        // Restore gracefully accepts 0L and 0 Int for Unspecified color
        val savedWithZeroLong = packedList.toMutableList().apply { set(0, 0L) }
        assertThat(SpanStyleSaver.restore(savedWithZeroLong)!!.color).isEqualTo(Color.Unspecified)
        val savedWithZeroInt = packedList.toMutableList().apply { set(0, 0) }
        assertThat(SpanStyleSaver.restore(savedWithZeroInt)!!.color).isEqualTo(Color.Unspecified)
    }

    @Test
    fun spanStyleSaver_preservesTransparentAndWideGamutColors_andClearsBrush() {
        val transparentStyle = SpanStyle(color = Color.Transparent, fontSize = 12.sp)
        val savedTransparent = with(SpanStyleSaver) { defaultSaverScope.save(transparentStyle) }!!
        val restoredTransparent = SpanStyleSaver.restore(savedTransparent)!!
        assertThat(restoredTransparent.color).isEqualTo(Color.Transparent)
        assertThat(restoredTransparent).isEqualTo(transparentStyle)

        val unspecifiedStyle = SpanStyle(color = Color.Unspecified, fontSize = 12.sp)
        val savedUnspecified = with(SpanStyleSaver) { defaultSaverScope.save(unspecifiedStyle) }!!
        val restoredUnspecified = SpanStyleSaver.restore(savedUnspecified)!!
        assertThat(restoredUnspecified.color).isEqualTo(Color.Unspecified)
        assertThat(restoredUnspecified).isEqualTo(unspecifiedStyle)

        val brushStyle =
            SpanStyle(
                brush = Brush.linearGradient(listOf(Color.Red, Color.Blue)),
                alpha = 0.75f,
                fontSize = 16.sp,
            )
        val savedBrush = with(SpanStyleSaver) { defaultSaverScope.save(brushStyle) }!!
        val restoredBrush = SpanStyleSaver.restore(savedBrush)!!
        assertThat(restoredBrush.brush).isNull()
        assertThat(restoredBrush.color).isEqualTo(Color.Unspecified)
        assertThat(restoredBrush.fontSize).isEqualTo(16.sp)

        if (PackedTextStylesLatch.isEnabled) {
            @Suppress("UNCHECKED_CAST")
            assertThat((savedUnspecified as List<Any?>)[0]).isEqualTo(0L)
            @Suppress("UNCHECKED_CAST") assertThat((savedBrush as List<Any?>)[0]).isEqualTo(0L)

            val displayP3Color = Color(0.9f, 0.1f, 0.2f, 1f, ColorSpaces.DisplayP3)
            val wideGamutStyle = SpanStyle(color = displayP3Color, fontSize = 14.sp)
            val savedWideGamut = with(SpanStyleSaver) { defaultSaverScope.save(wideGamutStyle) }!!
            val restoredWideGamut = SpanStyleSaver.restore(savedWideGamut)!!
            assertThat(restoredWideGamut.color).isEqualTo(displayP3Color)
            assertThat(restoredWideGamut).isEqualTo(wideGamutStyle)
        }
    }

    @Test
    fun annotationSaver_and_annotatedStringSaver_edgeCases() {
        assertThrows(UnsupportedOperationException::class.java) {
            with(AnnotatedString.Annotation.Saver) {
                defaultSaverScope.save(Bullet.Default)
            }
        }

        val annotatedWithBullet =
            AnnotatedString(
                text = "bullet item",
                annotations = listOf(AnnotatedString.Range(Bullet.Default, 0, 6)),
            )
        val savedWithBullet =
            with(AnnotatedStringSaver) { defaultSaverScope.save(annotatedWithBullet) }!!
        assertThat(AnnotatedStringSaver.restore(savedWithBullet))
            .isEqualTo(AnnotatedString("bullet item"))

        val unsaveableDrawAnnotation = DrawAnnotation(drawBehind = {})
        val strictScope = androidx.compose.runtime.saveable.SaverScope { false }
        assertThrows(UnsupportedOperationException::class.java) {
            with(AnnotatedString.Annotation.Saver) { strictScope.save(unsaveableDrawAnnotation) }
        }

        val urlWithNullStyles =
            LinkAnnotation.Url(
                url = "https://developer.android.com",
                styles = null,
                linkInteractionListener = {},
            )
        val clickableWithNullStyles =
            LinkAnnotation.Clickable(
                tag = "action_tag",
                styles = null,
                linkInteractionListener = {},
            )

        val savedUrl =
            with(AnnotatedString.Annotation.Saver) { defaultSaverScope.save(urlWithNullStyles) }!!
        val restoredUrl = AnnotatedString.Annotation.Saver.restore(savedUrl) as LinkAnnotation.Url
        assertThat(restoredUrl.url).isEqualTo("https://developer.android.com")
        assertThat(restoredUrl.styles).isNull()
        assertThat(restoredUrl.linkInteractionListener).isNull()

        val savedClickable =
            with(AnnotatedString.Annotation.Saver) {
                defaultSaverScope.save(clickableWithNullStyles)
            }!!
        val restoredClickable =
            AnnotatedString.Annotation.Saver.restore(savedClickable) as LinkAnnotation.Clickable
        assertThat(restoredClickable.tag).isEqualTo("action_tag")
        assertThat(restoredClickable.styles).isNull()
        assertThat(restoredClickable.linkInteractionListener).isNull()

        assertSaverRoundTrip(
            TextLinkStylesSaver,
            TextLinkStyles(style = SpanStyle(color = Color.Red)),
            TextLinkStyles(focusedStyle = SpanStyle(color = Color.Green)),
            TextLinkStyles(hoveredStyle = SpanStyle(color = Color.Blue)),
            TextLinkStyles(pressedStyle = SpanStyle(color = Color.Yellow)),
        )
    }

    private class RecordingSpannable(private val text: String = "Hello World") :
        Spannable, CharSequence by text {
        data class RecordedSpan(val what: Any, val start: Int, val end: Int, val flags: Int)

        val spans = mutableListOf<RecordedSpan>()

        override fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
            if (what != null) {
                spans.add(RecordedSpan(what, start, end, flags))
            }
        }

        override fun removeSpan(what: Any?) {
            spans.removeAll { it.what === what }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> getSpans(start: Int, end: Int, type: Class<T>?): Array<T> =
            emptyArray<Any>() as Array<T>

        override fun getSpanStart(tag: Any?): Int = -1

        override fun getSpanEnd(tag: Any?): Int = -1

        override fun getSpanFlags(tag: Any?): Int = 0

        override fun nextSpanTransition(start: Int, limit: Int, type: Class<*>?): Int = limit
    }

    private class CountingDensity(
        override val density: Float = 2f,
        override val fontScale: Float = 1f,
    ) : Density {
        var toPxCallCount = 0

        override fun TextUnit.toPx(): Float {
            toPxCallCount++
            return value * density * fontScale
        }
    }

    @Test
    fun setBulletSpans_lazyTextIndentEvaluationAndLeadingMargin() {
        assumeTrue(PackedTextStylesLatch.isEnabled)
        // 1. Zero bullets: does not evaluate textIndent.firstLine.toPx()
        val noBulletsSpannable = RecordingSpannable("Paragraph without bullets")
        val density0 = CountingDensity()
        noBulletsSpannable.setBulletSpans(
            annotations =
                listOf(
                    AnnotatedString.Range(SpanStyle(color = Color.Red), 0, 5),
                    AnnotatedString.Range(StringAnnotation("tag"), 0, 5),
                ),
            contextFontSize = 16f,
            density = density0,
            textIndent = TextIndent(firstLine = 20.sp, restLine = 10.sp),
        )
        assertThat(noBulletsSpannable.spans).isEmpty()
        assertThat(density0.toPxCallCount).isEqualTo(0)

        // 2. Multiple bullets: evaluates textIndent.firstLine.toPx() lazily once
        val multiBulletSpannable = RecordingSpannable("Item 1\nItem 2\nItem 3")
        val density1 = CountingDensity(density = 2f, fontScale = 1f)
        val emBullet = Bullet.Default.copy(width = 1.em, height = 1.em, padding = 0.5.em)
        multiBulletSpannable.setBulletSpans(
            annotations =
                listOf(
                    AnnotatedString.Range(emBullet, 0, 6),
                    AnnotatedString.Range(SpanStyle(color = Color.Blue), 0, 6),
                    AnnotatedString.Range(emBullet, 7, 13),
                    AnnotatedString.Range(emBullet, 14, 20),
                ),
            contextFontSize = 20f,
            density = density1,
            textIndent = TextIndent(firstLine = 5.sp, restLine = 5.sp),
        )
        assertThat(multiBulletSpannable.spans).hasSize(3)
        assertThat(density1.toPxCallCount).isEqualTo(1)
        for (recorded in multiBulletSpannable.spans) {
            val bulletSpan = recorded.what as CustomBulletSpan
            // minimumRequiredIndent = (20f + 10f) = 30; textIndentPx = 5.sp * 2f = 10; diff = 10 -
            // 30 = -20 -> abs(diff) = 20
            assertThat(bulletSpan.getLeadingMargin(true)).isEqualTo(20)
        }

        // 3. Unspecified bullet width/padding falls back to contextFontSize (16f each -> 32)
        val unspecifiedSpannable = RecordingSpannable("Item 1")
        val unspecifiedBullet =
            Bullet.Default.copy(
                width = TextUnit.Unspecified,
                height = TextUnit.Unspecified,
                padding = TextUnit.Unspecified,
            )
        unspecifiedSpannable.setBulletSpans(
            annotations = listOf(AnnotatedString.Range(unspecifiedBullet, 0, 6)),
            contextFontSize = 16f,
            density = CountingDensity(),
            textIndent = TextIndent(firstLine = TextUnit.Unspecified),
        )
        assertThat(unspecifiedSpannable.spans).hasSize(1)
        val bulletSpan = unspecifiedSpannable.spans.single().what as CustomBulletSpan
        assertThat(bulletSpan.getLeadingMargin(true)).isEqualTo(32)
    }

    @Test
    fun setSpanStyles_and_flattenFontStylesAndApply_hostVerification() {
        val spannable = RecordingSpannable("Hello")
        val nonSpanAnnotations =
            listOf<AnnotatedString.Range<out AnnotatedString.Annotation>>(
                AnnotatedString.Range(ParagraphStyle(textAlign = TextAlign.Center), 0, 5),
                AnnotatedString.Range(Bullet.Default, 0, 5),
            )
        spannable.setSpanStyles(
            contextTextStyle = TextStyle.Default,
            annotations = nonSpanAnnotations,
            density = CountingDensity(),
            resolveTypeface = { _, _, _, _ ->
                throw AssertionError("resolveTypeface must not be called")
            },
        )
        assertThat(spannable.spans).isEmpty()

        val applied = mutableListOf<Triple<SpanStyle, Int, Int>>()
        flattenFontStylesAndApply(null, emptyList()) { style, s, e ->
            applied.add(Triple(style, s, e))
        }
        assertThat(applied).isEmpty()

        val s1 = SpanStyle(fontWeight = FontWeight.Bold)
        val s2 = SpanStyle(fontStyle = FontStyle.Italic)
        flattenFontStylesAndApply(
            null,
            listOf(
                AnnotatedString.Range(s1, 0, 10),
                AnnotatedString.Range(s2, 4, 6),
            ),
        ) { style, s, e ->
            applied.add(Triple(style, s, e))
        }
        assertThat(applied)
            .containsExactly(
                Triple(s1, 0, 4),
                Triple(s1.merge(s2), 4, 6),
                Triple(s1, 6, 10),
            )
            .inOrder()
    }

    @Test
    fun hasMetricAffectingSpanStyle_hasFontAttributes_and_correctBlurRadius_hostVerification() {
        assertThat(emptyList<AnnotatedString.Range<*>>().hasMetricAffectingSpanStyle()).isFalse()

        val nonSpanRanges =
            listOf<AnnotatedString.Range<*>>(
                AnnotatedString.Range(ParagraphStyle(textAlign = TextAlign.Center), 0, 4),
                AnnotatedString.Range(Bullet.Default, 0, 4),
                AnnotatedString.Range(StringAnnotation("meta"), 0, 4),
            )
        assertThat(nonSpanRanges.hasMetricAffectingSpanStyle()).isFalse()

        val nonMetricSpanRanges =
            listOf<AnnotatedString.Range<*>>(
                AnnotatedString.Range(
                    SpanStyle(
                        color = Color.Red,
                        background = Color.Yellow,
                        textDecoration = TextDecoration.Underline,
                        shadow = Shadow.None,
                        drawStyle = Fill,
                        baselineShift = BaselineShift.None,
                    ),
                    0,
                    4,
                )
            )
        assertThat(nonMetricSpanRanges.hasMetricAffectingSpanStyle()).isFalse()

        for (metricStyle in
            listOf(
                SpanStyle(fontSize = 18.sp),
                SpanStyle(letterSpacing = 1.sp),
                SpanStyle(fontFamily = FontFamily.Serif),
                SpanStyle(fontStyle = FontStyle.Italic),
                SpanStyle(fontWeight = FontWeight.Bold),
                SpanStyle(fontSynthesis = FontSynthesis.Weight),
                SpanStyle(fontFeatureSettings = "smcp"),
                SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX = 1.2f)),
                SpanStyle(localeList = LocaleList("en-US")),
                SpanStyle(baselineShift = BaselineShift.Superscript),
            )) {
            assertThat(
                    listOf(AnnotatedString.Range(metricStyle, 0, 4)).hasMetricAffectingSpanStyle()
                )
                .isTrue()
        }

        val linkWithMetricStyle =
            LinkAnnotation.Url(
                "https://example.com",
                TextLinkStyles(focusedStyle = SpanStyle(fontWeight = FontWeight.Bold)),
            )
        assertThat(
                listOf(AnnotatedString.Range(linkWithMetricStyle, 0, 4))
                    .hasMetricAffectingSpanStyle()
            )
            .isTrue()

        assertThat(SpanStyle().hasFontAttributes()).isFalse()
        assertThat(SpanStyle(fontSynthesis = FontSynthesis.All).hasFontAttributes()).isFalse()
        assertThat(SpanStyle(fontFamily = FontFamily.Cursive).hasFontAttributes()).isTrue()
        assertThat(SpanStyle(fontStyle = FontStyle.Normal).hasFontAttributes()).isTrue()
        assertThat(SpanStyle(fontWeight = FontWeight.W500).hasFontAttributes()).isTrue()

        assertThat(correctBlurRadius(0f)).isEqualTo(Float.MIN_VALUE)
        assertThat(correctBlurRadius(-0.0f)).isEqualTo(Float.MIN_VALUE)
        assertThat(correctBlurRadius(4.5f)).isEqualTo(4.5f)
    }

    @Test
    fun androidParagraphHelper_fastPath_and_includeFontPadding_and_textDirectionHeuristics() {
        assertThat(TextStyle.Default.isIncludeFontPaddingEnabled()).isFalse()
        assertThat(
                TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = true))
                    .isIncludeFontPaddingEnabled()
            )
            .isTrue()
        assertThat(
                TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    .isIncludeFontPaddingEnabled()
            )
            .isFalse()

        val rawText = "FastPathText"
        val seq =
            createCharSequence(
                text = rawText,
                contextFontSize = 16f,
                contextTextStyle = TextStyle(textIndent = TextIndent.None),
                annotations = emptyList(),
                userAnnotations = emptyList(),
                placeholders = emptyList(),
                density = CountingDensity(),
                resolveTypeface = { _, _, _, _ ->
                    throw AssertionError("Must not resolve typeface on fast path")
                },
                useEmojiCompat = false,
                softWrap = true,
                mayHaveNewLine = false,
                defaultLocaleList = LocaleList("en-US"),
            )
        assertThat(seq).isSameInstanceAs(rawText)

        val locale = Locale("en-US")
        assertThat(resolveTextDirectionHeuristics(TextDirection.ContentOrLtr, locale)).isEqualTo(2)
        assertThat(resolveTextDirectionHeuristics(TextDirection.ContentOrRtl, locale)).isEqualTo(3)
        assertThat(resolveTextDirectionHeuristics(TextDirection.Ltr, locale)).isEqualTo(0)
        assertThat(resolveTextDirectionHeuristics(TextDirection.Rtl, locale)).isEqualTo(1)
    }
}
