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

package androidx.pdf

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfRectTest {

    @Test
    fun leftBottom_returnsCorrectPoint() {
        val rect = PdfRect(pageNum = 1, left = 10f, top = 20f, right = 50f, bottom = 40f)
        val leftBottom = rect.leftBottom

        assertThat(leftBottom.pageNum).isEqualTo(1)
        assertThat(leftBottom.x).isEqualTo(10f)
        assertThat(leftBottom.y).isEqualTo(40f)
    }

    @Test
    fun rightBottom_returnsCorrectPoint() {
        val rect = PdfRect(pageNum = 2, left = 10f, top = 20f, right = 50f, bottom = 40f)
        val rightBottom = rect.rightBottom

        assertThat(rightBottom.pageNum).isEqualTo(2)
        assertThat(rightBottom.x).isEqualTo(50f)
        assertThat(rightBottom.y).isEqualTo(40f)
    }

    @Test
    fun centerPoint_returnsCorrectPoint() {
        val rect = PdfRect(pageNum = 0, left = 0f, top = 0f, right = 100f, bottom = 100f)
        val center = rect.centerPoint

        assertThat(center.pageNum).isEqualTo(0)
        assertThat(center.x).isEqualTo(50f)
        assertThat(center.y).isEqualTo(50f)
    }

    @Test
    fun toPdfRect_returnsCorrectPdfRect() {
        val rect = Rect(50, 100, 150, 200)
        val imageRect = RectF(100f, 200f, 300f, 400f)
        val bitmapSize = Point(500, 1000)

        val pdfRect = rect.toPdfRect(pageNum = 0, imageRect = imageRect, bitmapSize = bitmapSize)

        assertThat(pdfRect.pageNum).isEqualTo(0)
        assertThat(pdfRect.left).isEqualTo(120f)
        assertThat(pdfRect.top).isEqualTo(220f)
        assertThat(pdfRect.right).isEqualTo(160f)
        assertThat(pdfRect.bottom).isEqualTo(240f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toPdfRect_withZeroBitmapWidth_throwsException() {
        val rect = Rect(50, 100, 150, 200)
        val imageRect = RectF(100f, 200f, 300f, 400f)
        val bitmapSize = Point(0, 1000)

        rect.toPdfRect(pageNum = 0, imageRect = imageRect, bitmapSize = bitmapSize)
    }
}
