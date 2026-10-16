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

package androidx.compose.ui.text.android

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.text.Layout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.text.TextUtils.TruncateAt
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(InternalPlatformTextApi::class)
@SmallTest
class StringCharSequenceTest {

    private val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 30f }
    private val width = 400

    @Test
    fun delegatesToString() {
        val string = "Hello World"
        val charSequence = StringCharSequence(string)

        assertThat(charSequence.length).isEqualTo(string.length)
        assertThat(charSequence[4]).isEqualTo('o')
        assertThat(charSequence.subSequence(6, 11).toString()).isEqualTo("World")
        assertThat(charSequence.toString()).isSameInstanceAs(string)
    }

    @Test
    fun textUtilsGetChars_copiesOnlyRequestedRange() {
        val charSequence = StringCharSequence("Hello World")
        val dest = CharArray(7) { '-' }

        TextUtils.getChars(charSequence, 6, 11, dest, 1)

        assertThat(String(dest)).isEqualTo("-World-")
    }

    @Test
    fun textLayout_wrapsStringForStaticLayout() {
        val textLayout =
            TextLayout(charSequence = LatinText, width = width.toFloat(), textPaint = paint)

        assertThat(textLayout.layout.lineCount).isGreaterThan(1)
        assertThat(textLayout.layout.text).isInstanceOf(StringCharSequence::class.java)
        assertThat(textLayout.layout.text.toString()).isEqualTo(LatinText)
    }

    @Test
    fun textLayout_doesNotWrapStringForBoringLayout() {
        val textLayout =
            TextLayout(charSequence = "Hello", width = width.toFloat(), textPaint = paint)

        assertThat(textLayout.layout.text).isEqualTo("Hello")
    }

    @Test
    fun staticLayout_latin_sameAsString() {
        assertSameAsString(LatinText)
    }

    @Test
    fun staticLayout_rtl_sameAsString() {
        assertSameAsString(RtlText, textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR)
    }

    @Test
    fun staticLayout_bidi_sameAsString() {
        assertSameAsString(BidiText, textDir = TextDirectionHeuristics.FIRSTSTRONG_LTR)
    }

    @Test
    fun staticLayout_tabs_sameAsString() {
        assertSameAsString("one\ttwo\tthree\tfour\tfive\tsix\tseven\teight\n".repeat(10))
    }

    @Test
    fun staticLayout_endEllipsis_sameAsString() {
        assertSameAsString(LatinText, maxLines = 3, ellipsize = TruncateAt.END)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun staticLayout_justified_sameAsString() {
        assertSameAsString(LatinText, justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD)
    }

    private fun assertSameAsString(
        string: String,
        textDir: TextDirectionHeuristic = LayoutCompat.DEFAULT_TEXT_DIRECTION_HEURISTIC,
        maxLines: Int = LayoutCompat.DEFAULT_MAX_LINES,
        ellipsize: TruncateAt? = null,
        justificationMode: Int = LayoutCompat.DEFAULT_JUSTIFICATION_MODE,
    ) {
        fun create(text: CharSequence) =
            StaticLayoutFactory.create(
                text = text,
                paint = paint,
                width = width,
                textDir = textDir,
                maxLines = maxLines,
                ellipsize = ellipsize,
                justificationMode = justificationMode,
            )
        val stringLayout = create(string)
        val wrappedLayout = create(StringCharSequence(string))

        assertThat(wrappedLayout.lineCount).isGreaterThan(1)
        assertThat(wrappedLayout.lineCount).isEqualTo(stringLayout.lineCount)
        for (line in 0 until stringLayout.lineCount) {
            assertThat(wrappedLayout.getLineEnd(line)).isEqualTo(stringLayout.getLineEnd(line))
            assertThat(wrappedLayout.getLineWidth(line)).isEqualTo(stringLayout.getLineWidth(line))
            assertThat(wrappedLayout.getEllipsisCount(line))
                .isEqualTo(stringLayout.getEllipsisCount(line))
        }
        assertThat(wrappedLayout.draw().sameAs(stringLayout.draw())).isTrue()
    }

    private fun Layout.draw(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }

    private companion object {
        val LatinText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.\n".repeat(20)
        val RtlText =
            "\u05e9\u05dc\u05d5\u05dd \u05e2\u05d5\u05dc\u05dd \u05de\u05d4 \u05e9\u05dc\u05d5\u05de\u05da "
                .repeat(30)
        val BidiText =
            "Hello \u05e9\u05dc\u05d5\u05dd world \u0645\u0631\u062d\u0628\u0627 123 ".repeat(30)
    }
}
