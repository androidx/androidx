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

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.models.KeyedPdfObject
import androidx.pdf.annotation.models.PathPdfObject
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfDocumentPageObjectsTest {

    @Test
    fun getPageObjects_returnsAllObjects() = runTest {
        val imageObject =
            KeyedPdfObject(
                "image1",
                ImagePdfObject(
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    RectF(0f, 0f, 1f, 1f),
                ),
            )
        val pathObject = KeyedPdfObject("path1", PathPdfObject(0, 0f, listOf()))

        val fakeDoc =
            FakePdfDocument(pageObjectsPerPage = mapOf(0 to listOf(imageObject, pathObject)))

        val result = fakeDoc.getPageObjects(0, PdfDocument.PAGE_OBJECT_INCLUDE_ALL_TYPES)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.key }).containsExactly("image1", "path1")
    }

    @Test
    fun getPageObjects_filtersByImage() = runTest {
        val imageObject =
            KeyedPdfObject(
                "image1",
                ImagePdfObject(
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    RectF(0f, 0f, 1f, 1f),
                ),
            )
        val pathObject = KeyedPdfObject("path1", PathPdfObject(0, 0f, listOf()))

        val fakeDoc =
            FakePdfDocument(pageObjectsPerPage = mapOf(0 to listOf(imageObject, pathObject)))

        val result = fakeDoc.getPageObjects(0, PdfDocument.INCLUDE_IMAGE_PAGE_OBJECT)

        assertThat(result).hasSize(1)
        assertThat(result[0].key).isEqualTo("image1")
        assertThat(result[0].pdfObject).isInstanceOf(ImagePdfObject::class.java)
    }

    @Test
    fun getPageObjects_filtersByPath() = runTest {
        val imageObject =
            KeyedPdfObject(
                "image1",
                ImagePdfObject(
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                    RectF(0f, 0f, 1f, 1f),
                ),
            )
        val pathObject = KeyedPdfObject("path1", PathPdfObject(0, 0f, listOf()))

        val fakeDoc =
            FakePdfDocument(pageObjectsPerPage = mapOf(0 to listOf(imageObject, pathObject)))

        val result = fakeDoc.getPageObjects(0, PdfDocument.INCLUDE_PATH_PAGE_OBJECT)

        assertThat(result).hasSize(1)
        assertThat(result[0].key).isEqualTo("path1")
        assertThat(result[0].pdfObject).isInstanceOf(PathPdfObject::class.java)
    }

    @Test
    fun getPageObjects_emptyPage_returnsEmptyList() = runTest {
        val fakeDoc = FakePdfDocument(pageObjectsPerPage = emptyMap())

        val result = fakeDoc.getPageObjects(0, PdfDocument.PAGE_OBJECT_INCLUDE_ALL_TYPES)

        assertThat(result).isEmpty()
    }
}
