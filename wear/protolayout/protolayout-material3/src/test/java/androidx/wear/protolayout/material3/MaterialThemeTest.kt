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
package androidx.wear.protolayout.material3

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class MaterialThemeTest {
    @Test
    fun defaultMaterialTheme_returnsTokenDefaults() {
        for (i in 0 until Typography.TOKEN_COUNT) {
            val fontStyle: LayoutElementBuilders.FontStyle =
                DEFAULT_MATERIAL_THEME.getFontStyleBuilder(i).build()
            val textStyle = Typography.fromToken(i)
            assertThat(fontStyle.preferredFontFamilies).isEmpty()
            assertThat(fontStyle.size!!.value).isEqualTo(textStyle.size.value)
            assertThat(fontStyle.letterSpacing!!.value).isEqualTo(textStyle.letterSpacing.value)
            assertThat(fontStyle.settings).isEqualTo(textStyle.fontSettings)
        }

        for (i in 0 until ColorTokens.TOKEN_COUNT) {
            assertThat(DEFAULT_MATERIAL_THEME.getColor(i).getArgb())
                .isEqualTo(ColorTokens.fromToken(i).argb)
        }

        for (i in 0 until Shape.TOKEN_COUNT) {
            assertThat(DEFAULT_MATERIAL_THEME.getCornerShape(i).getRadius()!!.getValue())
                .isEqualTo(Shape.fromToken(i).radius!!.value)
        }
    }

    @Test
    fun customMaterialTheme_overrideColor_returnsOverriddenValue() {
        assertThat(
                MaterialTheme(
                        customColorScheme =
                            mapOf(ColorTokens.ERROR to ColorBuilders.argb(Color.MAGENTA))
                    )
                    .getColor(ColorTokens.ERROR)
                    .argb
            )
            .isEqualTo(Color.MAGENTA)
    }

    @Test
    fun customMaterialTheme_colorNotOverridden_returnsDefaultValue() {
        // Provides a custom color scheme with an overridden color.
        assertThat(
                MaterialTheme(
                        customColorScheme =
                            mapOf(ColorTokens.SECONDARY to ColorBuilders.argb(Color.MAGENTA))
                    )
                    .getColor(ColorTokens.ON_ERROR)
                    .argb
            )
            .isEqualTo(ColorTokens.fromToken(ColorTokens.ON_ERROR).argb)
    }
}
