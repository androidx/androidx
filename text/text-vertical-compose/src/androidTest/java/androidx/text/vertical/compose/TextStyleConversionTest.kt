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

package androidx.text.vertical.compose

import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.os.LocaleList as AndroidLocaleList
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextStyleConversionTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun setStyleToPaint_defaultFontSize() {
        val paint = TextPaint()
        setStyleToPaint(VerticalTextStyle.Default, Typeface.DEFAULT, density, paint)
        assertThat(paint.textSize).isEqualTo(32f) // 16.sp * density 2
    }

    @Test
    fun setStyleToPaint_fontSize_sp() {
        val paint = TextPaint()
        setStyleToPaint(VerticalTextStyle(fontSize = 20.sp), Typeface.DEFAULT, density, paint)
        assertThat(paint.textSize).isEqualTo(40f) // 20.sp * density 2
    }

    @Test
    fun setStyleToPaint_fontSize_em() {
        val paint = TextPaint()
        setStyleToPaint(VerticalTextStyle(fontSize = 1.5.em), Typeface.DEFAULT, density, paint)
        assertThat(paint.textSize).isEqualTo(48f) // 16.sp * 2 * 1.5
    }

    @Test
    fun setStyleToPaint_fontSize_respectsFontScale() {
        val scaledDensity = Density(density = 2f, fontScale = 1.5f)
        val expectedDefaultPx = with(scaledDensity) { DefaultFontSize.toPx() }
        val expected20SpPx = with(scaledDensity) { 20.sp.toPx() }
        assertThat(expectedDefaultPx).isGreaterThan(32f)

        val paint = TextPaint()

        setStyleToPaint(VerticalTextStyle.Default, Typeface.DEFAULT, scaledDensity, paint)
        assertThat(paint.textSize).isEqualTo(expectedDefaultPx)

        setStyleToPaint(VerticalTextStyle(fontSize = 20.sp), Typeface.DEFAULT, scaledDensity, paint)
        assertThat(paint.textSize).isEqualTo(expected20SpPx)

        setStyleToPaint(
            VerticalTextStyle(fontSize = 1.5.em),
            Typeface.DEFAULT,
            scaledDensity,
            paint,
        )
        assertThat(paint.textSize).isEqualTo(expectedDefaultPx * 1.5f)
    }

    @Test
    fun setStyleToPaint_color_specifiedAndUnspecified() {
        val paint = TextPaint()
        setStyleToPaint(VerticalTextStyle(color = Color.Red), Typeface.DEFAULT, density, paint)
        assertThat(paint.color).isEqualTo(AndroidColor.RED)

        val defaultPaint = TextPaint()
        setStyleToPaint(
            VerticalTextStyle(color = Color.Unspecified),
            Typeface.DEFAULT,
            density,
            defaultPaint,
        )
        assertThat(defaultPaint.color).isEqualTo(AndroidColor.BLACK)
    }

    @Test
    fun setStyleToPaint_background_specifiedAndUnspecified() {
        val paint = TextPaint()
        setStyleToPaint(
            VerticalTextStyle(background = Color.Yellow),
            Typeface.DEFAULT,
            density,
            paint,
        )
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)

        val defaultPaint = TextPaint()
        setStyleToPaint(
            VerticalTextStyle(background = Color.Unspecified),
            Typeface.DEFAULT,
            density,
            defaultPaint,
        )
        assertThat(defaultPaint.bgColor).isEqualTo(AndroidColor.TRANSPARENT)
    }

    @Test
    fun setStyleToPaint_typeface() {
        val paint = TextPaint()
        setStyleToPaint(VerticalTextStyle.Default, Typeface.SERIF, density, paint)
        assertThat(paint.typeface).isEqualTo(Typeface.SERIF)

        val boldItalic = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        setStyleToPaint(VerticalTextStyle.Default, boldItalic, density, paint)
        assertThat(paint.typeface).isEqualTo(boldItalic)
    }

    @Test
    fun setStyleToPaint_fontFeatureSettings() {
        val paint = TextPaint()
        setStyleToPaint(
            VerticalTextStyle(fontFeatureSettings = "smcp"),
            Typeface.DEFAULT,
            density,
            paint,
        )
        assertThat(paint.fontFeatureSettings).isEqualTo("smcp")

        val defaultPaint = TextPaint()
        setStyleToPaint(VerticalTextStyle.Default, Typeface.DEFAULT, density, defaultPaint)
        assertThat(defaultPaint.fontFeatureSettings).isNull()
    }

    @Test
    fun setStyleToPaint_localeList() {
        val paint = TextPaint()
        setStyleToPaint(
            VerticalTextStyle(localeList = LocaleList("ja-JP,zh-CN")),
            Typeface.DEFAULT,
            density,
            paint,
        )
        assertThat(paint.textLocales).isEqualTo(AndroidLocaleList.forLanguageTags("ja-JP,zh-CN"))
    }

    @Test
    fun setStyleToPaint_recyclesTextPaint_resetsUnspecifiedFields() {
        val paint = TextPaint()
        val populatedStyle =
            VerticalTextStyle(
                color = Color.Red,
                fontSize = 24.sp,
                fontFeatureSettings = "smcp",
                background = Color.Yellow,
                localeList = LocaleList("ja-JP"),
            )
        setStyleToPaint(populatedStyle, Typeface.SERIF, density, paint)
        assertThat(paint.color).isEqualTo(AndroidColor.RED)
        assertThat(paint.bgColor).isEqualTo(AndroidColor.YELLOW)
        assertThat(paint.textSize).isEqualTo(48f)
        assertThat(paint.typeface).isEqualTo(Typeface.SERIF)
        assertThat(paint.fontFeatureSettings).isEqualTo("smcp")

        setStyleToPaint(VerticalTextStyle.Default, Typeface.DEFAULT, density, paint)
        assertThat(paint.color).isEqualTo(AndroidColor.BLACK)
        assertThat(paint.bgColor).isEqualTo(AndroidColor.TRANSPARENT)
        assertThat(paint.textSize).isEqualTo(32f)
        assertThat(paint.typeface).isEqualTo(Typeface.DEFAULT)
        assertThat(paint.fontFeatureSettings).isNull()
        assertThat(paint.textLocales).isEqualTo(AndroidLocaleList.getAdjustedDefault())
    }

    @Test
    fun setStyleToPaint_recycle_resetsExternalState() {
        val paint =
            TextPaint().apply {
                textScaleX = 1.5f
                textSkewX = -0.25f
                isUnderlineText = true
            }
        setStyleToPaint(VerticalTextStyle.Default, Typeface.DEFAULT, density, paint)
        assertThat(paint.textScaleX).isEqualTo(1f)
        assertThat(paint.textSkewX).isEqualTo(0f)
        assertThat(paint.isUnderlineText).isFalse()
    }
}
