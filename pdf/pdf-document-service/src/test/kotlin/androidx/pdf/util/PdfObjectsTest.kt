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

package androidx.pdf.utils

import android.graphics.Bitmap
import android.graphics.RectF
import android.graphics.pdf.component.PdfPageImageObject
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.models.ImagePdfObject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfObjectsTest {

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    @Test
    fun toPdfObject_withImageObject_returnsImagePdfObject() {

        // mocking bitmap so we don't have to create one
        val expectedBitmap = mock<Bitmap>()

        // Identity matrix translated by (10, 20)
        val matrixValues = floatArrayOf(1f, 0f, 10f, 0f, 1f, 20f, 0f, 0f, 1f)

        val mockImageObject = mock<PdfPageImageObject>()
        whenever(mockImageObject.matrix).thenReturn(matrixValues)
        whenever(mockImageObject.bitmap).thenReturn(expectedBitmap)

        val result = mockImageObject.toPdfObject()

        assertThat(result is ImagePdfObject).isTrue()
        (result as ImagePdfObject).let { imagePdfObject ->

            // assert that bitmap is same
            assertThat(imagePdfObject.bitmap).isEqualTo(expectedBitmap)

            // assert bounds are converted correctly
            // Unit rect (0,0,1,1) translated by (10, 20) -> (10, 20, 11, 21)
            assertThat(imagePdfObject.bounds).isEqualTo(RectF(10f, 20f, 11f, 21f))
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    @Test
    fun toImagePdfObject_transformsBoundsCorrectly() {
        // Scaling matrix: 100x200
        val matrixValues = floatArrayOf(100f, 0f, 0f, 0f, 200f, 0f, 0f, 0f, 1f)
        val expectedBitmap = mock<Bitmap>()
        val expectedImageObject = mock<PdfPageImageObject>()
        whenever(expectedImageObject.matrix).thenReturn(matrixValues)
        whenever(expectedImageObject.bitmap).thenReturn(expectedBitmap)

        val result = expectedImageObject.toImagePdfObject()

        assertThat(result.bounds).isEqualTo(RectF(0f, 0f, 100f, 200f))
        assertThat(result.bitmap).isEqualTo(expectedBitmap)
    }
}
