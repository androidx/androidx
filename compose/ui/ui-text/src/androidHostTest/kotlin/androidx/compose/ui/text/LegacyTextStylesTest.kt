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

import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
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
        assertThat(paragraphStyle.legacyParagraphStyle != null)
            .isEqualTo(!PackedTextStylesLatch.isEnabled)
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
}
