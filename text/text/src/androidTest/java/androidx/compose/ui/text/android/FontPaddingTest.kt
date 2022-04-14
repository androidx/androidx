/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.text.android

import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.ui.text.font.test.R
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class FontPaddingTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val fontSize = 100f
    private val latinText = "a"
    private val latinTextMultiLine = "$latinText\n$latinText\n$latinText"

    private val tallText = "1"
    private val tallTextMultiLine = "$tallText\n$tallText\n$tallText"

    private val latinTypeface = ResourcesCompat.getFont(context, R.font.sample_font)!!
    private val tallTypeface = ResourcesCompat.getFont(context, R.font.tall_font)!!

    // latin font tests
    @Test
    fun latinIncludeFontPaddingDoesNotAffectLineMetricsSingleLine() {
        val withPadding = TextLayout(
            latinText,
            typeface = latinTypeface,
            includePadding = true
        )
        val withoutPadding = TextLayout(
            latinText,
            typeface = latinTypeface,
            includePadding = false
        )

        assertThat(withPadding.height).isEqualTo(withoutPadding.height)
        assertThat(withPadding.getLineTop(0)).isEqualTo(withoutPadding.getLineTop(0))
        assertThat(withPadding.getLineBaseline(0)).isEqualTo(withoutPadding.getLineBaseline(0))
        assertThat(withPadding.getLineBottom(0)).isEqualTo(withoutPadding.getLineBottom(0))
    }

    @Test
    fun latinIncludeFontPaddingDoesNotAffectLineMetricsMultiLine() {
        val withPadding = TextLayout(
            latinTextMultiLine,
            typeface = latinTypeface,
            includePadding = true
        )
        val withoutPadding = TextLayout(
            latinTextMultiLine,
            typeface = latinTypeface,
            includePadding = false
        )

        assertThat(withPadding.height).isEqualTo(withoutPadding.height)
        for (index in 0 until withPadding.lineCount) {
            assertThat(withPadding.getLineTop(index)).isEqualTo(withoutPadding.getLineTop(index))
            assertThat(withPadding.getLineBaseline(index))
                .isEqualTo(withoutPadding.getLineBaseline(index))
            assertThat(withPadding.getLineBottom(index))
                .isEqualTo(withoutPadding.getLineBottom(index))
        }
    }

    @Test
    fun latinIncludeFontPaddingDoesNotAffectGetLineForVertical() {
        val withPadding = TextLayout(
            latinTextMultiLine,
            typeface = latinTypeface,
            includePadding = true
        )
        val withoutPadding = TextLayout(
            latinTextMultiLine,
            typeface = latinTypeface,
            includePadding = false
        )

        assertThat(withPadding.height).isEqualTo(withoutPadding.height)

        val vertical = (withPadding.height / 2f).toInt()
        assertThat(withPadding.getLineForVertical(vertical))
            .isEqualTo(withoutPadding.getLineForVertical(vertical))
    }

    // tall font tests
    @Test
    fun tallIincludeFontPaddingDoesNotAffectLineMetricsSingleLine() {
        val withPadding = TextLayout(
            tallText,
            typeface = tallTypeface,
            includePadding = true
        )
        val withoutPadding = TextLayout(
            tallText,
            typeface = tallTypeface,
            includePadding = false
        )

        assertThat(withPadding.height).isEqualTo(withoutPadding.height)
        assertThat(withPadding.getLineTop(0)).isEqualTo(withoutPadding.getLineTop(0))
        assertThat(withPadding.getLineBaseline(0)).isEqualTo(withoutPadding.getLineBaseline(0))
        assertThat(withPadding.getLineBottom(0)).isEqualTo(withoutPadding.getLineBottom(0))
    }

    @Test
    fun tallIncludeFontPaddingDoesNotAffectLineMetricsMultiLine() {
        val withPadding = TextLayout(
            tallTextMultiLine,
            typeface = tallTypeface,
            includePadding = true
        )
        val withoutPadding = TextLayout(
            tallTextMultiLine,
            typeface = tallTypeface,
            includePadding = false
        )

        assertThat(withPadding.height).isEqualTo(withoutPadding.height)
        for (index in 0 until withPadding.lineCount) {
            assertThat(withPadding.getLineTop(index)).isEqualTo(withoutPadding.getLineTop(index))
            assertThat(withPadding.getLineBaseline(index))
                .isEqualTo(withoutPadding.getLineBaseline(index))
            assertThat(withPadding.getLineBottom(index))
                .isEqualTo(withoutPadding.getLineBottom(index))
        }
    }

    @Test
    fun tallIncludeFontPaddingDoesNotAffectGetLineForVertical() {
        val withPadding = TextLayout(
            tallTextMultiLine,
            typeface = latinTypeface,
            includePadding = true
        )
        val withoutPadding = TextLayout(
            tallTextMultiLine,
            typeface = latinTypeface,
            includePadding = false
        )

        assertThat(withPadding.height).isEqualTo(withoutPadding.height)

        val vertical = (withPadding.height / 2f).toInt()
        assertThat(withPadding.getLineForVertical(vertical))
            .isEqualTo(withoutPadding.getLineForVertical(vertical))
    }

    // tall vs latin font tests
    @Test
    fun tallTypefaceTextIsTwiceTheHeightOfLatinTypefaceTextSingleLine() {
        val latinLayout = TextLayout(latinText, typeface = latinTypeface)
        val tallLayout = TextLayout(tallText, typeface = tallTypeface)

        assertThat(tallLayout.height).isEqualTo(2 * latinLayout.height)
    }

    @Test
    fun tallTypefaceTextIsTwiceTheHeightOfLatinTypefaceTextMultiLine() {
        val latinLayout = TextLayout(latinTextMultiLine, typeface = latinTypeface)
        val tallLayout = TextLayout(tallTextMultiLine, typeface = tallTypeface)

        assertThat(tallLayout.height).isEqualTo(2 * latinLayout.height)
    }

    private fun TextLayout(
        text: CharSequence,
        includePadding: Boolean = false,
        typeface: Typeface = latinTypeface
    ): TextLayout {
        val textPaint = TextPaint().apply {
            this.typeface = typeface
            this.textSize = fontSize
        }

        /**
         * fallbackLineSpacing has to be false so that the real impact can be tested.
         * fallbackLineSpacing increases line height, and is enabled on starting API P StaticLayout,
         * Android T BoringLayout.
         */
        return TextLayout(
            charSequence = text,
            textPaint = textPaint,
            includePadding = includePadding,
            fallbackLineSpacing = false
        )
    }
}