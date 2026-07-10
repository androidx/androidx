/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.graphics

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

@SmallTest
class CanvasTest {
    private val values = FloatArray(9)
    private val canvas = Canvas(createBitmap(1, 1))

    @Suppress("DEPRECATION")
    @Test
    fun withSave() {
        val beforeCount = canvas.saveCount

        canvas.matrix.getValues(values)
        val x = values[Matrix.MTRANS_X]
        val y = values[Matrix.MTRANS_Y]

        canvas.withSave {
            assertThat(beforeCount).isLessThan(saveCount)
            translate(10.0f, 10.0f)
        }

        canvas.matrix.getValues(values)
        assertEquals(x, values[Matrix.MTRANS_X])
        assertEquals(y, values[Matrix.MTRANS_Y])

        assertEquals(beforeCount, canvas.saveCount)
    }

    @Test
    fun withTranslation() {
        val beforeCount = canvas.saveCount
        canvas.withTranslation(x = 16.0f, y = 32.0f) {
            assertThat(beforeCount).isLessThan(saveCount)

            @Suppress("DEPRECATION") matrix.getValues(values) // will work for a software canvas

            assertEquals(16.0f, values[Matrix.MTRANS_X])
            assertEquals(32.0f, values[Matrix.MTRANS_Y])
        }
        assertEquals(beforeCount, canvas.saveCount)
    }

    @Test
    fun withRotation() {
        val beforeCount = canvas.saveCount
        canvas.withRotation(degrees = 90.0f, pivotX = 16.0f, pivotY = 32.0f) {
            assertThat(beforeCount).isLessThan(saveCount)

            @Suppress("DEPRECATION") matrix.getValues(values) // will work for a software canvas

            assertEquals(48.0f, values[Matrix.MTRANS_X])
            assertEquals(16.0f, values[Matrix.MTRANS_Y])
            assertEquals(-1.0f, values[Matrix.MSKEW_X])
            assertEquals(1.0f, values[Matrix.MSKEW_Y])
        }
        assertEquals(beforeCount, canvas.saveCount)
    }

    @Test
    fun withScale() {
        val beforeCount = canvas.saveCount
        canvas.withScale(x = 2.0f, y = 4.0f, pivotX = 16.0f, pivotY = 32.0f) {
            assertThat(beforeCount).isLessThan(saveCount)

            @Suppress("DEPRECATION") matrix.getValues(values) // will work for a software canvas

            assertEquals(-16.0f, values[Matrix.MTRANS_X])
            assertEquals(-96.0f, values[Matrix.MTRANS_Y])
            assertEquals(2.0f, values[Matrix.MSCALE_X])
            assertEquals(4.0f, values[Matrix.MSCALE_Y])
        }
        assertEquals(beforeCount, canvas.saveCount)
    }

    @Test
    fun withSkew() {
        val beforeCount = canvas.saveCount
        canvas.withSkew(x = 2.0f, y = 4.0f) {
            assertThat(beforeCount).isLessThan(saveCount)

            @Suppress("DEPRECATION") matrix.getValues(values) // will work for a software canvas

            assertEquals(2.0f, values[Matrix.MSKEW_X])
            assertEquals(4.0f, values[Matrix.MSKEW_Y])
        }
        assertEquals(beforeCount, canvas.saveCount)
    }

    @Suppress("DEPRECATION")
    @Test
    fun withMatrix() {
        val originMatrix = canvas.matrix

        val inputMatrix = Matrix()
        inputMatrix.postTranslate(16.0f, 32.0f)
        inputMatrix.postRotate(90.0f, 16.0f, 32.0f)
        inputMatrix.postScale(2.0f, 4.0f, 16.0f, 32.0f)

        val beforeCount = canvas.saveCount
        canvas.withMatrix(inputMatrix) {
            assertThat(beforeCount).isLessThan(saveCount)
            assertEquals(inputMatrix, matrix)
        }

        assertEquals(originMatrix, canvas.matrix)
        assertEquals(beforeCount, canvas.saveCount)
    }

    @Test
    fun withClipRect() {
        val b = createBitmap(4, 4)

        // clipRect(Int...)
        // Use white and red
        b.applyCanvas {
            drawARGB(255, 255, 255, 255)
            withClip(0, 0, 2, 2) { drawARGB(255, 255, 0, 0) }
        }
        assertEquals(0xff_ff_00_00.toInt(), b[1, 1])
        assertEquals(0xff_ff_ff_ff.toInt(), b[3, 3])

        // clipRect(Float...)
        // Use black and green
        b.applyCanvas {
            drawARGB(255, 0, 0, 0)
            withClip(0.0f, 0.0f, 2.0f, 2.0f) { drawARGB(255, 0, 255, 0) }
        }
        assertEquals(0xff_00_ff_00.toInt(), b[1, 1])
        assertEquals(0xff_00_00_00.toInt(), b[3, 3])

        // clipRect(Rect)
        // Use white and red
        b.applyCanvas {
            drawARGB(255, 255, 255, 255)
            withClip(Rect(0, 0, 2, 2)) { drawARGB(255, 255, 0, 0) }
        }
        assertEquals(0xff_ff_00_00.toInt(), b[1, 1])
        assertEquals(0xff_ff_ff_ff.toInt(), b[3, 3])

        // clipRect(RectF)
        // Use black and green
        b.applyCanvas {
            drawARGB(255, 0, 0, 0)
            withClip(RectF(0.0f, 0.0f, 2.0f, 2.0f)) { drawARGB(255, 0, 255, 0) }
        }
        assertEquals(0xff_00_ff_00.toInt(), b[1, 1])
        assertEquals(0xff_00_00_00.toInt(), b[3, 3])
    }

    @Test
    fun withClipPath() {
        val b = createBitmap(4, 4)

        b.applyCanvas {
            drawARGB(255, 255, 255, 255)
            withClip(Path().apply { addRect(0.0f, 0.0f, 2.0f, 2.0f, Path.Direction.CW) }) {
                drawARGB(255, 255, 0, 0)
            }
        }
        assertEquals(0xff_ff_00_00.toInt(), b[1, 1])
        assertEquals(0xff_ff_ff_ff.toInt(), b[3, 3])
    }
}
