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

import android.text.SpannableString
import android.text.SpannedString
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
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
class PaintBoundsTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val fontSize = 100f
    private val latinText = "a"
    private val latinTextMultiLine = "$latinText\n$latinText\n$latinText"
    private val latinTypeface = ResourcesCompat.getFont(context, R.font.sample_font)!!

    @Test
    fun emptyString() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }

        val text = ""
        assertThat(
            paint.getCharSequenceBounds(text, 0, text.length)
        ).isEqualTo(
            paint.getStringBounds(text, 0, text.length)
        )
    }

    @Test
    fun singleLineLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = "abc"

        assertThat(
            paint.getCharSequenceBounds(text, 0, text.length)
        ).isEqualTo(
            paint.getStringBounds(text, 0, text.length)
        )
    }

    @Test
    fun singleLineRtl() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = "\u05D0\u05D0\u05D0"

        assertThat(
            paint.getCharSequenceBounds(text, 0, text.length)
        ).isEqualTo(
            paint.getStringBounds(text, 0, text.length)
        )
    }

    @Test
    fun multiLineLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = "abc\ndef"

        assertThat(
            paint.getCharSequenceBounds(text, 0, "abc".length)
        ).isEqualTo(
            paint.getStringBounds(text, 0, "abc".length)
        )
    }

    @Test
    fun multiLineRtl() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = "\u05D0\u05D0\u05D0\n\u05D0\u05D0\u05D0"

        assertThat(
            paint.getCharSequenceBounds(text, 0, text.indexOf('\n'))
        ).isEqualTo(
            paint.getStringBounds(text, 0, text.indexOf('\n'))
        )
    }

    @Test
    fun spannedNoSpansLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannedString("abc")

        assertThat(
            paint.getCharSequenceBounds(text, 0, text.length)
        ).isEqualTo(
            paint.getStringBounds(text.toString(), 0, text.length)
        )
    }

    @Test
    fun singleSpanLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannableString("abc")
        text.setSpan(RelativeSizeSpan(2f), 1, 2, 0)

        val bounds = paint.getCharSequenceBounds(text, 0, text.length)

        assertThat(bounds.height()).isEqualTo((2 * fontSize).toInt())
        assertThat(bounds.width()).isEqualTo((4 * fontSize).toInt())
    }

    @Test
    fun singleSpanRtl() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannableString("\u05D0\u05D0\u05D0")
        text.setSpan(RelativeSizeSpan(2f), 1, 2, 0)

        val bounds = paint.getCharSequenceBounds(text, 0, text.length)

        assertThat(bounds.height()).isEqualTo((2 * fontSize).toInt())
        assertThat(bounds.width()).isEqualTo((4 * fontSize).toInt())
    }

    @Test
    fun zeroLengthSpanLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannableString("abc")
        text.setSpan(RelativeSizeSpan(2f), 1, 1, 0)

        assertThat(
            paint.getCharSequenceBounds(text, 0, text.length)
        ).isEqualTo(
            paint.getStringBounds(text.toString(), 0, text.length)
        )
    }

    @Test
    fun spanCoveringTextLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannableString("abc")
        text.setSpan(RelativeSizeSpan(2f), 0, text.length, 0)

        val bounds = paint.getCharSequenceBounds(text, 0, text.length)

        assertThat(bounds.height()).isEqualTo((2 * fontSize).toInt())
        assertThat(bounds.width()).isEqualTo((2 * text.length * fontSize).toInt())
    }

    @Test
    fun spanCoveringTextRtl() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannableString("\u05D0\u05D0\u05D0")
        text.setSpan(RelativeSizeSpan(2f), 0, text.length, 0)

        val bounds = paint.getCharSequenceBounds(text, 0, text.length)

        assertThat(bounds.height()).isEqualTo((2 * fontSize).toInt())
        assertThat(bounds.width()).isEqualTo((2 * text.length * fontSize).toInt())
    }

    @Test
    fun multipleSpansIntersectingLtr() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }
        val text = SpannableString("abaaba")
        text.setSpan(AbsoluteSizeSpan((fontSize * 2).toInt()), 1, 5, 0)
        text.setSpan(AbsoluteSizeSpan((fontSize * 3).toInt()), 2, 4, 0)

        val bounds = paint.getCharSequenceBounds(text, 0, text.length)

        assertThat(bounds.width()).isEqualTo((12 * fontSize).toInt())
    }

    @Test
    fun multipleSpansIntersectingRtl() {
        val paint = TextPaint().apply {
            textSize = fontSize
            typeface = latinTypeface
        }

        val text = SpannableString("\u05D0\u05D0\u05D0\u05D0\u05D0\u05D0")
        text.setSpan(AbsoluteSizeSpan((fontSize * 2).toInt()), 1, 5, 0)
        text.setSpan(AbsoluteSizeSpan((fontSize * 3).toInt()), 2, 4, 0)

        val bounds = paint.getCharSequenceBounds(text, 0, text.length)

        assertThat(bounds.width()).isEqualTo((12 * fontSize).toInt())
    }
}