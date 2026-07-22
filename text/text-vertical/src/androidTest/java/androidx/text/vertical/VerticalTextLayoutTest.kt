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

package androidx.text.vertical

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.text.TextPaint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VerticalTextLayoutTest {
    // The detailed behavior tests are written in LineBreakerTests and underlying LayoutRunTests.
    // In this test case, just check the set in builder and get in instance.

    val PAINT =
        TextPaint().apply {
            textSize = 10f // make 1em = 10px
        }

    val JP_TEXT = "吾輩は猫である。\n1904年(明治39年)生まれである。\n英名はI Am a Catである。"

    @Test
    fun constructor() {
        val layout = createVerticalTextLayout()
        assertThat(layout.width).isGreaterThan(0f)
        layout.run {
            assertThat(text).isEqualTo(JP_TEXT)
            assertThat(start).isEqualTo(0)
            assertThat(end).isEqualTo(JP_TEXT.length)
            assertThat(paint).isSameInstanceAs(PAINT)
            assertThat(height).isEqualTo(100f)
            assertThat(orientation).isEqualTo(TextOrientation.Mixed)
        }
    }

    @Test
    fun isVerticalTextSupported() {
        assertThat(createVerticalTextLayout().isVerticalTextSupported()).isTrue()
    }

    @Test
    fun lineCount_singleColumn() {
        val layout = VerticalTextLayout("あ", 0, 1, PAINT, 100f)
        assertThat(layout.lineCount).isEqualTo(1)
    }

    @Test
    fun lineCount_multipleColumns() {
        val text = "吾輩は猫である。名前はまだ無い。"
        val layout = VerticalTextLayout(text, 0, text.length, PAINT, 30f)
        assertThat(layout.lineCount).isGreaterThan(1)
    }

    @Test
    fun lineCount_isConsistentWithWidth() {
        val text = "吾輩は猫である。名前はまだ無い。"
        val layout = VerticalTextLayout(text, 0, text.length, PAINT, 30f)
        assertThat(layout.lineCount).isGreaterThan(0)
        assertThat(layout.width).isGreaterThan(0f)
    }

    @Test
    fun draw_smokeTest() {
        val layout = createVerticalTextLayout()
        val canvas = Canvas()
        layout.draw(canvas, 0f, 0f)
        // ensure no crash
    }

    @Test
    fun draw_positionsCharacters() {
        val text = "あい"
        val layout = VerticalTextLayout(text, 0, text.length, PAINT, 100f)
        val draws = mutableListOf<Triple<String, Float, Float>>()
        val canvas =
            object : Canvas() {
                var currentY = 0f

                override fun translate(dx: Float, dy: Float) {
                    currentY += dy
                }

                override fun drawText(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    y: Float,
                    paint: Paint,
                ) {
                    draws.add(Triple(text.subSequence(start, end).toString(), x, currentY + y))
                }

                override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                    draws.add(Triple(text, x, currentY + y))
                }
            }

        layout.draw(canvas, 0f, 0f)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            // In backport, it should draw character by character (or cluster by cluster)
            assertThat(draws.size).isAtLeast(2)

            // Check if Y is increasing.
            for (i in 0 until draws.size - 1) {
                assertThat(draws[i + 1].third).isGreaterThan(draws[i].third)
            }
        } else {
            // In native, it might be a single drawText call.
            assertThat(draws.size).isAtLeast(1)
        }
    }

    private fun createVerticalTextLayout() =
        VerticalTextLayout(JP_TEXT, 0, JP_TEXT.length, PAINT, 100f)
}
