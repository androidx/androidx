/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.layout

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_NORMAL
import androidx.wear.protolayout.LayoutElementBuilders.FontSetting
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.TextAlignment
import androidx.wear.protolayout.LayoutElementBuilders.TextOverflow
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.modifiers.toProtoLayoutModifiers
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextTest {
    @Test
    fun fontStyle_inflates() {
        val fontStyle: FontStyle =
            fontStyle(
                size = FONT_SIZE,
                italic = true,
                underline = true,
                color = FONT_COLOR,
                weight = FONT_WEIGHT,
                letterSpacingEm = FONT_LETTER_SPACING,
                settings = listOf(FontSetting.weight(FONT_AXIS_VALUE)),
                preferredFontFamilies = listOf(FONT_FAMILY, FONT_FAMILY_FALLBACK),
            )

        assertThat(fontStyle.size!!.value).isEqualTo(FONT_SIZE)
        assertThat(fontStyle.italic!!.value).isTrue()
        assertThat(fontStyle.underline!!.value).isTrue()
        assertThat(fontStyle.color!!.argb).isEqualTo(FONT_COLOR.staticArgb)
        assertThat(fontStyle.weight!!.value).isEqualTo(FONT_WEIGHT)
        assertThat(fontStyle.letterSpacing!!.value).isEqualTo(FONT_LETTER_SPACING)
        assertThat(fontStyle.sizes.size).isEqualTo(1)
        assertThat(fontStyle.sizes[0].value).isEqualTo(FONT_SIZE)
        assertThat(fontStyle.settings.size).isEqualTo(1)
        assertThat(fontStyle.settings[0].toFontSettingProto())
            .isEqualTo(FontSetting.weight(FONT_AXIS_VALUE).toFontSettingProto())
        assertThat(fontStyle.preferredFontFamilies.size).isEqualTo(2)
        assertThat(fontStyle.preferredFontFamilies[0]).isEqualTo(FONT_FAMILY)
        assertThat(fontStyle.preferredFontFamilies[1]).isEqualTo(FONT_FAMILY_FALLBACK)
    }

    @Test
    fun basicText_inflates() {
        val fontStyle = fontStyle(size = FONT_SIZE)

        val text: Text =
            basicText(
                text = TEXT,
                fontStyle = fontStyle,
                modifier = MODIFIER,
                maxLines = MAX_LINES,
                alignment = MULTILINE_ALIGNMENT,
                overflow = OVERFLOW,
                lineHeight = LINE_HEIGHT,
            )

        assertThat(text.fontStyle!!.toProto()).isEqualTo(fontStyle.toProto())
        assertThat(text.text!!.value).isEqualTo(TEXT.staticValue)
        assertThat(text.modifiers!!.toProto())
            .isEqualTo(MODIFIER.toProtoLayoutModifiers().toProto())
        assertThat(text.maxLines!!.value).isEqualTo(MAX_LINES)
        assertThat(text.multilineAlignment!!.value).isEqualTo(MULTILINE_ALIGNMENT)
        assertThat(text.overflow!!.value).isEqualTo(OVERFLOW)
        assertThat(text.lineHeight!!.value).isEqualTo(LINE_HEIGHT)
    }

    private companion object {
        val TEXT = "Text test".layoutString
        const val COLOR = Color.YELLOW
        const val FONT_SIZE = 12f
        val FONT_COLOR = COLOR.argb
        val MODIFIER = LayoutModifier.contentDescription("description")
        const val MAX_LINES = 2
        @TextAlignment const val MULTILINE_ALIGNMENT = TEXT_ALIGN_START
        @TextOverflow const val OVERFLOW = TEXT_OVERFLOW_ELLIPSIZE
        const val LINE_HEIGHT = 16f
        const val FONT_WEIGHT = FONT_WEIGHT_NORMAL
        const val FONT_LETTER_SPACING = 0.5f
        const val FONT_FAMILY = "ABC"
        const val FONT_FAMILY_FALLBACK = "XYZ"
        const val FONT_AXIS_VALUE = 100
    }
}
