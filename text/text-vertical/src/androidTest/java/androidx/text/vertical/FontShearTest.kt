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
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FontShearTest {

    private val TEXT = "Hello"
    private val SHEARED =
        SpannableString(TEXT).apply {
            setSpan(FontShearSpan(), 0, TEXT.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    private val ONE_EM = 10f // make 1em = 10px
    private val PAINT = TextPaint().apply { textSize = ONE_EM }

    private class MockCanvas(val skewCallback: (x: Float, y: Float) -> Unit) : Canvas() {
        override fun skew(x: Float, y: Float) {
            super.skew(x, y)
            skewCallback(x, y)
        }
    }

    @Test
    fun `FontShearRunTest Upright Case`() {
        UprightLayoutRun(TEXT, 0, TEXT.length, PAINT).also {
            it.draw(
                MockCanvas { x, y ->
                    assertThat(x).isEqualTo(0f)
                    assertThat(y).isEqualTo(0f)
                },
                0f,
                0f,
                PAINT,
            )
        }

        UprightLayoutRun(SHEARED, 0, SHEARED.length, PAINT).also {
            it.draw(
                MockCanvas { x, y ->
                    assertThat(x).isEqualTo(0f)
                    assertThat(y).isEqualTo(FontShearSpan.DEFAULT_FONT_SHEAR)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun `FontShearRunTest Rotate Case`() {
        RotateLayoutRun(TEXT, 0, TEXT.length, PAINT).also {
            it.draw(
                MockCanvas { x, y ->
                    assertThat(x).isEqualTo(0f)
                    assertThat(y).isEqualTo(0f)
                },
                0f,
                0f,
                PAINT,
            )
        }

        RotateLayoutRun(SHEARED, 0, SHEARED.length, PAINT).also {
            it.draw(
                MockCanvas { x, y ->
                    assertThat(x).isEqualTo(FontShearSpan.DEFAULT_FONT_SHEAR)
                    assertThat(y).isEqualTo(0f)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun `FontShearRunTest TateChuYoko Case`() {
        TateChuYokoLayoutRun(TEXT, 0, TEXT.length, PAINT).also {
            it.draw(
                MockCanvas { x, y ->
                    assertThat(x).isEqualTo(0f)
                    assertThat(y).isEqualTo(0f)
                },
                0f,
                0f,
                PAINT,
            )
        }

        TateChuYokoLayoutRun(SHEARED, 0, SHEARED.length, PAINT).also {
            it.draw(
                MockCanvas { x, y ->
                    assertThat(x).isEqualTo(FontShearSpan.DEFAULT_FONT_SHEAR)
                    assertThat(y).isEqualTo(0f)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }
}
