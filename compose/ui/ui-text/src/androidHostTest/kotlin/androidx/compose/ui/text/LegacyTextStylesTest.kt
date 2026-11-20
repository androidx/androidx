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

package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextForegroundStyle
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalTextApi::class)
@RunWith(JUnit4::class)
class LegacyTextStylesTest {

    @Test
    fun composeUiTextFlags_isPackedTextStylesEnabled_defaultsToTrueAndLatches() {
        assertThat(PackedTextStylesLatch.isEnabled)
            .isEqualTo(ComposeUiTextFlags.isPackedTextStylesEnabled)
        val paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center, lineHeight = 20.sp)
        val spanStyle = SpanStyle(color = Color.Red, fontSize = 16.sp)
        assertThat(paragraphStyle.legacyParagraphStyle != null)
            .isEqualTo(!PackedTextStylesLatch.isEnabled)
        assertThat(spanStyle.legacySpanStyle != null).isEqualTo(!PackedTextStylesLatch.isEnabled)
    }

    @Test
    fun legacyParagraphStyle_merge_lerp_andResolveDefaults_matchPackedParagraphStyle() {
        val legacyBase =
            LegacyParagraphStyle(
                textAlign = TextAlign.Start,
                textDirection = TextDirection.Ltr,
                lineHeight = 16.sp,
                textIndent = TextIndent(4.sp, 8.sp),
                platformStyle = PlatformParagraphStyle.Default,
                lineHeightStyle = LineHeightStyle.Default,
                lineBreak = LineBreak.Simple,
                hyphens = Hyphens.None,
                textMotion = TextMotion.Static,
            )
        val legacyOverride =
            LegacyParagraphStyle(
                textAlign = TextAlign.Justify,
                textDirection = TextDirection.Unspecified,
                lineHeight = 24.sp,
                textIndent = null,
                platformStyle = PlatformParagraphStyle(includeFontPadding = true),
                lineHeightStyle = null,
                lineBreak = LineBreak.Heading,
                hyphens = Hyphens.Auto,
                textMotion = TextMotion.Animated,
            )

        val merged =
            legacyBase.fastMerge(
                textAlign = legacyOverride.textAlign,
                textDirection = legacyOverride.textDirection,
                lineHeight = legacyOverride.lineHeight,
                textIndent = legacyOverride.textIndent,
                platformStyle = legacyOverride.platformStyle,
                lineHeightStyle = legacyOverride.lineHeightStyle,
                lineBreak = legacyOverride.lineBreak,
                hyphens = legacyOverride.hyphens,
                textMotion = legacyOverride.textMotion,
            )
        assertThat(merged.textAlign).isEqualTo(TextAlign.Justify)
        assertThat(merged.textDirection).isEqualTo(TextDirection.Ltr)
        assertThat(merged.lineHeight).isEqualTo(24.sp)
        assertThat(merged.textIndent).isEqualTo(TextIndent(4.sp, 8.sp))
        assertThat(merged.platformStyle)
            .isEqualTo(PlatformParagraphStyle(includeFontPadding = true))
        assertThat(merged.lineHeightStyle).isEqualTo(LineHeightStyle.Default)
        assertThat(merged.lineBreak).isEqualTo(LineBreak.Heading)
        assertThat(merged.hyphens).isEqualTo(Hyphens.Auto)
        assertThat(merged.textMotion).isEqualTo(TextMotion.Animated)

        val resolvedDefault =
            resolveLegacyParagraphStyleDefaults(
                LegacyParagraphStyle(
                    textAlign = TextAlign.Unspecified,
                    textDirection = TextDirection.Unspecified,
                    lineHeight = TextUnit.Unspecified,
                    textIndent = null,
                    platformStyle = null,
                    lineHeightStyle = null,
                    lineBreak = LineBreak.Unspecified,
                    hyphens = Hyphens.Unspecified,
                    textMotion = null,
                ),
                LayoutDirection.Rtl,
            )
        assertThat(resolvedDefault.textAlign).isEqualTo(TextAlign.Start)
        assertThat(resolvedDefault.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(resolvedDefault.textIndent).isEqualTo(TextIndent.None)
        assertThat(resolvedDefault.lineBreak).isEqualTo(LineBreak.Simple)
        assertThat(resolvedDefault.hyphens).isEqualTo(Hyphens.None)
        assertThat(resolvedDefault.textMotion).isEqualTo(TextMotion.Static)

        val lerped = lerp(legacyBase, legacyOverride, 0.25f)
        assertThat(lerped.textAlign).isEqualTo(TextAlign.Start)
        assertThat(lerped.lineHeight).isEqualTo(18.sp)
    }

    @Test
    fun legacySpanStyle_merge_lerp_andResolveDefaults_matchPackedSpanStyle() {
        val shaderBrush = Brush.linearGradient(listOf(Color.Red, Color.Blue))
        val legacyBase =
            LegacySpanStyle(
                textForegroundStyle = TextForegroundStyle.from(Color.Red),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.None,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "smcp",
                letterSpacing = 0.1.em,
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                localeList = LocaleList("en-US"),
                background = Color.Yellow,
                textDecoration = TextDecoration.Underline,
                shadow = Shadow.None,
                platformStyle = PlatformSpanStyle.Default,
                drawStyle = Fill,
            )
        val stroke = Stroke(width = 2f)
        val customShadow = Shadow(Color.Black, Offset(2f, 4f), 6f)
        val merged =
            legacyBase.fastMerge(
                color = Color.Unspecified,
                brush = shaderBrush,
                alpha = 0.6f,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSynthesis = FontSynthesis.All,
                fontFamily = null,
                fontFeatureSettings = null,
                letterSpacing = TextUnit.Unspecified,
                baselineShift = BaselineShift.Superscript,
                textGeometricTransform = null,
                localeList = null,
                background = Color.Unspecified,
                textDecoration = null,
                shadow = customShadow,
                platformStyle = null,
                drawStyle = stroke,
            )
        assertThat(merged.brush).isSameInstanceAs(shaderBrush)
        assertThat(merged.alpha).isEqualTo(0.6f)
        assertThat(merged.color).isEqualTo(Color.Unspecified)
        assertThat(merged.fontSize).isEqualTo(20.sp)
        assertThat(merged.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(merged.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(merged.fontSynthesis).isEqualTo(FontSynthesis.All)
        assertThat(merged.fontFamily).isEqualTo(FontFamily.SansSerif)
        assertThat(merged.fontFeatureSettings).isEqualTo("smcp")
        assertThat(merged.letterSpacing).isEqualTo(0.1.em)
        assertThat(merged.baselineShift).isEqualTo(BaselineShift.Superscript)
        assertThat(merged.localeList).isEqualTo(LocaleList("en-US"))
        assertThat(merged.background).isEqualTo(Color.Yellow)
        assertThat(merged.textDecoration).isEqualTo(TextDecoration.Underline)
        assertThat(merged.shadow).isEqualTo(customShadow)
        assertThat(merged.platformStyle).isEqualTo(PlatformSpanStyle.Default)
        assertThat(merged.drawStyle).isEqualTo(stroke)

        assertThat(legacyBase.hasSameLayoutAffectingAttributes(merged)).isFalse()
        assertThat(legacyBase.hasSameNonLayoutAttributes(merged)).isFalse()
        assertThat(
                legacyBase.hasSameLayoutAffectingAttributes(
                    legacyBase.fastMerge(
                        color = Color.Blue,
                        brush = null,
                        alpha = Float.NaN,
                        fontSize = TextUnit.Unspecified,
                        fontWeight = null,
                        fontStyle = null,
                        fontSynthesis = null,
                        fontFamily = null,
                        fontFeatureSettings = null,
                        letterSpacing = TextUnit.Unspecified,
                        baselineShift = null,
                        textGeometricTransform = null,
                        localeList = null,
                        background = Color.Unspecified,
                        textDecoration = TextDecoration.LineThrough,
                        shadow = null,
                        platformStyle = null,
                        drawStyle = null,
                    )
                )
            )
            .isTrue()

        val resolvedEmpty =
            resolveLegacySpanStyleDefaults(
                LegacySpanStyle(
                    textForegroundStyle = TextForegroundStyle.Unspecified,
                    fontSize = TextUnit.Unspecified,
                    fontWeight = null,
                    fontStyle = null,
                    fontSynthesis = null,
                    fontFamily = null,
                    fontFeatureSettings = null,
                    letterSpacing = TextUnit.Unspecified,
                    baselineShift = null,
                    textGeometricTransform = null,
                    localeList = null,
                    background = Color.Unspecified,
                    textDecoration = null,
                    shadow = null,
                    platformStyle = null,
                    drawStyle = null,
                )
            )
        assertThat(resolvedEmpty.color).isEqualTo(Color.Black)
        assertThat(resolvedEmpty.fontSize).isEqualTo(14.sp)
        assertThat(resolvedEmpty.fontWeight).isEqualTo(FontWeight.Normal)
        assertThat(resolvedEmpty.fontStyle).isEqualTo(FontStyle.Normal)
        assertThat(resolvedEmpty.fontSynthesis).isEqualTo(FontSynthesis.All)
        assertThat(resolvedEmpty.fontFamily).isEqualTo(FontFamily.Default)
        assertThat(resolvedEmpty.fontFeatureSettings).isEqualTo("")
        assertThat(resolvedEmpty.letterSpacing).isEqualTo(0.sp)
        assertThat(resolvedEmpty.baselineShift).isEqualTo(BaselineShift.None)
        assertThat(resolvedEmpty.textGeometricTransform).isEqualTo(TextGeometricTransform.None)
        assertThat(resolvedEmpty.background).isEqualTo(Color.Transparent)
        assertThat(resolvedEmpty.textDecoration).isEqualTo(TextDecoration.None)
        assertThat(resolvedEmpty.shadow).isEqualTo(Shadow.None)
        assertThat(resolvedEmpty.drawStyle).isEqualTo(Fill)

        val lerped = lerp(legacyBase, merged, 0.5f)
        assertThat(lerped.fontSize).isEqualTo(17.sp)
        assertThat(lerped.brush).isSameInstanceAs(shaderBrush)
    }

    @Test
    fun legacyStyles_preserveExactLegacyPolynomialHashCode() {
        val legacySpan =
            LegacySpanStyle(
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.None,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "kern",
                letterSpacing = 0.5.sp,
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                localeList = LocaleList("en-US"),
                background = Color.Transparent,
                textDecoration = TextDecoration.None,
                shadow = Shadow.None,
                platformStyle = PlatformSpanStyle.Default,
                drawStyle = Fill,
            )
        val packedSpan =
            SpanStyle(
                color = Color.Red,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Normal,
                fontSynthesis = FontSynthesis.None,
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "kern",
                letterSpacing = 0.5.sp,
                baselineShift = BaselineShift.None,
                textGeometricTransform = TextGeometricTransform.None,
                localeList = LocaleList("en-US"),
                background = Color.Transparent,
                textDecoration = TextDecoration.None,
                shadow = Shadow.None,
                platformStyle = PlatformSpanStyle.Default,
                drawStyle = Fill,
            )

        var expectedLegacyLayoutHash = 16.sp.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + FontWeight.Medium.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + FontStyle.Normal.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + FontSynthesis.None.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + FontFamily.SansSerif.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + "kern".hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + 0.5.sp.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + BaselineShift.None.hashCode()
        expectedLegacyLayoutHash =
            31 * expectedLegacyLayoutHash + TextGeometricTransform.None.hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + LocaleList("en-US").hashCode()
        expectedLegacyLayoutHash = 31 * expectedLegacyLayoutHash + Color.Transparent.hashCode()
        expectedLegacyLayoutHash =
            31 * expectedLegacyLayoutHash + PlatformSpanStyle.Default.hashCode()

        assertThat(legacySpan.hashCodeLayoutAffectingAttributes())
            .isEqualTo(expectedLegacyLayoutHash)
        var expectedLegacySpanHash = Color.Red.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + 0
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + legacySpan.alpha.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + 16.sp.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + FontWeight.Medium.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + FontStyle.Normal.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + FontSynthesis.None.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + FontFamily.SansSerif.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + "kern".hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + 0.5.sp.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + BaselineShift.None.hashCode()
        expectedLegacySpanHash =
            31 * expectedLegacySpanHash + TextGeometricTransform.None.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + LocaleList("en-US").hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + Color.Transparent.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + TextDecoration.None.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + Shadow.None.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + PlatformSpanStyle.Default.hashCode()
        expectedLegacySpanHash = 31 * expectedLegacySpanHash + Fill.hashCode()
        assertThat(legacySpan.hashCode()).isEqualTo(expectedLegacySpanHash)
        if (PackedTextStylesLatch.isEnabled) {
            assertThat(legacySpan.hashCodeLayoutAffectingAttributes())
                .isNotEqualTo(packedSpan.hashCodeLayoutAffectingAttributes())
            assertThat(legacySpan.hashCode()).isNotEqualTo(packedSpan.hashCode())
        } else {
            assertThat(legacySpan.hashCodeLayoutAffectingAttributes())
                .isEqualTo(packedSpan.hashCodeLayoutAffectingAttributes())
            assertThat(legacySpan.hashCode()).isEqualTo(packedSpan.hashCode())
        }

        val legacyPara =
            LegacyParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Ltr,
                lineHeight = 24.sp,
                textIndent = TextIndent.None,
                platformStyle = PlatformParagraphStyle.Default,
                lineHeightStyle = LineHeightStyle.Default,
                lineBreak = LineBreak.Simple,
                hyphens = Hyphens.None,
                textMotion = TextMotion.Static,
            )
        val packedPara =
            ParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Ltr,
                lineHeight = 24.sp,
                textIndent = TextIndent.None,
                platformStyle = PlatformParagraphStyle.Default,
                lineHeightStyle = LineHeightStyle.Default,
                lineBreak = LineBreak.Simple,
                hyphens = Hyphens.None,
                textMotion = TextMotion.Static,
            )

        var expectedLegacyParaHash = TextAlign.Center.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + TextDirection.Ltr.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + 24.sp.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + TextIndent.None.hashCode()
        expectedLegacyParaHash =
            31 * expectedLegacyParaHash + PlatformParagraphStyle.Default.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + LineHeightStyle.Default.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + LineBreak.Simple.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + Hyphens.None.hashCode()
        expectedLegacyParaHash = 31 * expectedLegacyParaHash + TextMotion.Static.hashCode()

        assertThat(legacyPara.hashCode()).isEqualTo(expectedLegacyParaHash)
        if (PackedTextStylesLatch.isEnabled) {
            assertThat(legacyPara.hashCode()).isNotEqualTo(packedPara.hashCode())
        } else {
            assertThat(legacyPara.hashCode()).isEqualTo(packedPara.hashCode())
        }
    }
}
