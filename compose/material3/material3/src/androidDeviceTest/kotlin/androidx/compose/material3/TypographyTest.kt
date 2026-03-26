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

package androidx.compose.material3

import androidx.compose.material3.tokens.TypeScaleTokens
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TypographyTest {
    @Test
    fun typography_defaultFontFamily() {
        val typ = Typography()
        assertThat(typ.displayLarge.fontFamily).isEqualTo(TypeScaleTokens.DisplayLargeFont)
        assertThat(typ.displayMedium.fontFamily).isEqualTo(TypeScaleTokens.DisplayMediumFont)
        assertThat(typ.displaySmall.fontFamily).isEqualTo(TypeScaleTokens.DisplaySmallFont)
        assertThat(typ.headlineLarge.fontFamily).isEqualTo(TypeScaleTokens.HeadlineLargeFont)
        assertThat(typ.headlineMedium.fontFamily).isEqualTo(TypeScaleTokens.HeadlineMediumFont)
        assertThat(typ.headlineSmall.fontFamily).isEqualTo(TypeScaleTokens.HeadlineSmallFont)
        assertThat(typ.titleLarge.fontFamily).isEqualTo(TypeScaleTokens.TitleLargeFont)
        assertThat(typ.titleMedium.fontFamily).isEqualTo(TypeScaleTokens.TitleMediumFont)
        assertThat(typ.titleSmall.fontFamily).isEqualTo(TypeScaleTokens.TitleSmallFont)
        assertThat(typ.bodyLarge.fontFamily).isEqualTo(TypeScaleTokens.BodyLargeFont)
        assertThat(typ.bodyMedium.fontFamily).isEqualTo(TypeScaleTokens.BodyMediumFont)
        assertThat(typ.bodySmall.fontFamily).isEqualTo(TypeScaleTokens.BodySmallFont)
        assertThat(typ.labelLarge.fontFamily).isEqualTo(TypeScaleTokens.LabelLargeFont)
        assertThat(typ.labelMedium.fontFamily).isEqualTo(TypeScaleTokens.LabelMediumFont)
        assertThat(typ.labelSmall.fontFamily).isEqualTo(TypeScaleTokens.LabelSmallFont)
    }

    @Test
    fun typography_customFontFamily() {
        val customFontFamily = FontFamily.Cursive
        val typ = Typography(fontFamily = customFontFamily)
        assertThat(typ.displayLarge.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.displayMedium.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.displaySmall.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.headlineLarge.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.headlineMedium.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.headlineSmall.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.titleLarge.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.titleMedium.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.titleSmall.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.bodyLarge.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.bodyMedium.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.bodySmall.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.labelLarge.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.labelMedium.fontFamily).isEqualTo(customFontFamily)
        assertThat(typ.labelSmall.fontFamily).isEqualTo(customFontFamily)
    }

    @Test
    fun typography_customFontFamily_overrideDefault() {
        val customFontFamily = FontFamily.Cursive
        val typ =
            Typography(
                fontFamily = customFontFamily,
                labelMedium = TextStyle(fontFamily = FontFamily.SansSerif),
            )
        assertThat(typ.labelMedium.fontFamily).isEqualTo(FontFamily.SansSerif)
    }
}
