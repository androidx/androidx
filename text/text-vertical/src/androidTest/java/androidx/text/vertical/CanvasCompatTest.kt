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

package androidx.text.vertical

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
@SmallTest
class CanvasCompatTest {

    @Test
    @SdkSuppress(
        maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM
    ) // Test fallback path (API < 36 / Baklava)
    fun drawTextVertical_calculatesCorrectBaselineOffset() {
        val canvas = mock<Canvas>()
        val paint = TextPaint().apply { textSize = 100f }

        // Calculate expected ratio manually for verification (mimicking createCJKMetrics)
        val metrics = Paint.FontMetricsInt()
        val oldSize = paint.textSize
        paint.textSize = 1000f
        paint.getFontMetricsIntCompat("あ", 0, 1, 0, 1, false, metrics)
        paint.textSize = oldSize
        val ascent = metrics.ascent / 1000f
        val descent = metrics.descent / 1000f
        val expectedRatio = ascent / (ascent - descent)

        val startY = 50f
        val expectedYOffset = startY + expectedRatio * paint.textSize

        val text = "あ"
        canvas.drawTextVertical(text, 0, text.length, 10f, startY, paint)

        val yCaptor = argumentCaptor<Float>()

        verify(canvas, times(1))
            .drawText(
                any<CharSequence>(), // text
                any<Int>(), // start
                any<Int>(), // end
                any<Float>(), // x
                yCaptor.capture(), // y
                any<Paint>(), // paint
            )

        // The captured Y coordinate should match our calculated expectedYOffset
        val capturedY = yCaptor.firstValue
        assert(Math.abs(capturedY - expectedYOffset) < 0.01f) {
            "Expected Y offset: $expectedYOffset, but got: $capturedY"
        }
    }
}
