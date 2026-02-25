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

package androidx.pdf.models

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.models.toImageSelection
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ImagePdfObjectTest {

    @Test
    fun toImageSelection_mapsPropertiesCorrectly() {

        val expectedBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        val expectedBounds = RectF(10f, 20f, 100f, 150f)
        val pageNum = 1
        val imagePdfObject = ImagePdfObject(expectedBitmap, expectedBounds)

        val imageSelection = imagePdfObject.toImageSelection(pageNum)

        assertThat(imageSelection.bitmap).isEqualTo(expectedBitmap)
        assertThat(imageSelection.bounds).hasSize(1)
        val actualBounds = imageSelection.bounds.first()
        assertThat(actualBounds.pageNum).isEqualTo(pageNum)

        // Compare the RectF coordinates inside the PdfRect
        val actualRect = actualBounds
        assertThat(actualRect.left).isEqualTo(expectedBounds.left)
        assertThat(actualRect.top).isEqualTo(expectedBounds.top)
        assertThat(actualRect.right).isEqualTo(expectedBounds.right)
        assertThat(actualRect.bottom).isEqualTo(expectedBounds.bottom)
    }

    @Test
    fun toImageSelection_randomizedProperties_mapsCorrectly() {
        val random = java.util.Random()

        repeat(5) {
            val pageNum = random.nextInt(100)

            // Generate random bounds
            val left = random.nextFloat() * 100
            val top = random.nextFloat() * 100
            val right = left + random.nextFloat() * 100 // Ensure right > left
            val bottom = top + random.nextFloat() * 100 // Ensure bottom > top
            val expectedBounds = RectF(left, top, right, bottom)

            val expectedBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
            val imagePdfObject = ImagePdfObject(expectedBitmap, expectedBounds)

            val imageSelection = imagePdfObject.toImageSelection(pageNum)

            assertThat(imageSelection.bitmap).isEqualTo(expectedBitmap)
            assertThat(imageSelection.bounds).hasSize(1)

            val actualPdfRect = imageSelection.bounds.first()
            assertThat(actualPdfRect.pageNum).isEqualTo(pageNum)

            // Compare the RectF coordinates inside the PdfRect
            val actualRect = actualPdfRect
            assertThat(actualRect.left).isEqualTo(expectedBounds.left)
            assertThat(actualRect.top).isEqualTo(expectedBounds.top)
            assertThat(actualRect.right).isEqualTo(expectedBounds.right)
            assertThat(actualRect.bottom).isEqualTo(expectedBounds.bottom)
        }
    }
}
