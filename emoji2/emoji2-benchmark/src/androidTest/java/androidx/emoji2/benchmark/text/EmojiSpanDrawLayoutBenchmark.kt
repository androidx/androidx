/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.benchmark.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
class EmojiSpanDrawLayoutBenchmark(private val size: Int) {

    @get:Rule val benchmarkRule = BenchmarkRule()

    companion object {
        @Parameterized.Parameters(name = "size={0}")
        @JvmStatic
        fun parameters() = listOf(1, 40, 80, 120, 160, 200)
    }

    @Test
    fun emojiSpansDraw() {
        initializeEmojiCompatWithBundledForTest()
        val text =
            EmojiCompat.get().process(POLARBEAR.repeat(size)) as? Spanned
                ?: throw IllegalStateException("Fail the test")
        // this assertion is just to validate we're actually benchmarking EmojiSpans
        assertEquals(size, text.getSpans(0, text.length, EmojiSpan::class.java).size)
        measureRepeatedDrawText(text)
    }

    @Test
    fun emojiDraw() {
        val emojiString = POLARBEAR.repeat(size)
        measureRepeatedDrawText(emojiString)
    }

    @Test
    fun latinDraw() {
        val string = "E".repeat(size)

        measureRepeatedDrawText(string)
    }

    private fun measureRepeatedDrawText(text: CharSequence) {
        val paint = TextPaint()
        val layout = StaticLayout.Builder.obtain(text, 0, size, paint, Int.MAX_VALUE).build()
        var bitmap: Bitmap? = null
        try {
            bitmap =
                Bitmap.createBitmap(
                    layout.getLineWidth(1).toInt() + 100,
                    100,
                    Bitmap.Config.ARGB_8888,
                )
            val canvas = Canvas(bitmap)
            benchmarkRule.measureRepeated { layout.draw(canvas) }
        } finally {
            bitmap?.recycle()
        }
    }
}
