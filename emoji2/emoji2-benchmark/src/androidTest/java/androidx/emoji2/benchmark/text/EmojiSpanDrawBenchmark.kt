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
import android.text.TextPaint
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 19)
class EmojiSpanDrawBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun draw() {
        initializeEmojiCompatWithBundledForTest(true)
        val ec = EmojiCompat.get()
        val polarSpanned = ec.process(POLARBEAR) as Spanned
        val span = polarSpanned.getSpans(0, polarSpanned.length, EmojiSpan::class.java).first()

        val paint = TextPaint()
        var bitmap: Bitmap? = null
        try {
            bitmap = Bitmap.createBitmap(
                100,
                100,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            benchmarkRule.measureRepeated {
                span.draw(
                    canvas,
                    polarSpanned,
                    /* start */ 0,
                    /* end */ polarSpanned.length,
                    /* x */0f,
                    /* top */ 0,
                    /* y */ 0,
                    /* bottom */ 0,
                    paint
                )
            }
        } finally {
            bitmap?.recycle()
        }
    }
}
