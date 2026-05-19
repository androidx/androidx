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

import android.graphics.RectF
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfPointTest {

    @Test
    fun toImagePoint_returnsCorrectPixelCoordinates() {
        // PDF image at (10, 10) with size 100x100
        val imageRect = RectF(10f, 10f, 110f, 110f)
        // Image bitmap is 1000x1000 pixels
        val bitmapSize = Dimension(1000, 1000)

        // Point at (20, 30) in PDF space
        // Relative to image: X = ((20-10)/100)*1000 = 100, Y = ((30-10)/100)*1000 = 200
        val pdfPoint = PdfPoint(pageNum = 0, x = 20f, y = 30f)

        val imagePoint = pdfPoint.toImagePoint(imageRect, bitmapSize)

        assertThat(imagePoint.x).isEqualTo(100)
        assertThat(imagePoint.y).isEqualTo(200)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toImagePoint_withZeroWidth_throwsException() {
        val imageRect = RectF(10f, 10f, 10f, 110f)
        val bitmapSize = Dimension(1000, 1000)
        val pdfPoint = PdfPoint(pageNum = 0, x = 20f, y = 30f)

        pdfPoint.toImagePoint(imageRect, bitmapSize)
    }
}
