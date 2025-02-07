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
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.material3.tokens.ColorTokens
import androidx.wear.protolayout.material3.tokens.ShapeTokens
import androidx.wear.protolayout.types.argb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class MaterialThemeTest {
    @Test
    fun defaultMaterialTheme_returnsTokenDefaults() {
        val defaultTheme = MaterialTheme()

        // Starts from 2 as that is the first value of Typography tokens
        // (Typography.BODY_EXTRA_SMALL).
        for (i in 2 until Typography.TOKEN_COUNT) {
            val fontStyle: LayoutElementBuilders.FontStyle =
                defaultTheme.getFontStyleBuilder(i).build()
            val textStyle = Typography.fromToken(i)
            assertThat(fontStyle.preferredFontFamilies).isEmpty()
            assertThat(fontStyle.size!!.value).isEqualTo(textStyle.size.value)
            assertThat(fontStyle.letterSpacing!!.value).isEqualTo(textStyle.letterSpacing.value)
            assertThat(fontStyle.settings).isEqualTo(textStyle.fontSettings)
        }

        assertThat(defaultTheme.colorScheme.primaryDim.staticArgb)
            .isEqualTo(ColorTokens.PRIMARY_DIM)
        assertThat(defaultTheme.shapes.medium.toProto())
            .isEqualTo(ShapeTokens.CORNER_MEDIUM.toProto())
    }

    @Test
    fun customMaterialTheme_overrideColor_returnsOverriddenValue() {
        assertThat(
                MaterialTheme(colorScheme = ColorScheme(error = Color.MAGENTA.argb))
                    .colorScheme
                    .error
                    .staticArgb
            )
            .isEqualTo(Color.MAGENTA)
    }

    @Test
    fun customMaterialTheme_colorNotOverridden_returnsDefaultValue() {
        // Provides a custom color scheme with an overridden color.
        assertThat(
                MaterialTheme(colorScheme = ColorScheme(secondary = Color.MAGENTA.argb))
                    .colorScheme
                    .onError
                    .staticArgb
            )
            .isEqualTo(ColorTokens.ON_ERROR)
    }
}
