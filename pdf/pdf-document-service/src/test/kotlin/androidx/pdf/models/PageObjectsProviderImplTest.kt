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

import android.graphics.Path
import android.graphics.pdf.component.PdfPageImageObject
import android.graphics.pdf.component.PdfPageObject
import android.graphics.pdf.component.PdfPagePathObject
import android.os.Build
import android.util.Pair
import androidx.annotation.RequiresExtension
import androidx.pdf.PdfDocument
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.adapter.PdfPage
import androidx.pdf.annotation.models.KeyedPdfObject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PageObjectsProviderImplTest {

    @Test
    fun getPageObjects_filtersByType() {
        val mockRenderer = mock<PdfDocumentRenderer>()
        val mockPage = mock<PdfPage>()

        val imageObject = mock<PdfPageImageObject>()
        val pathObject = mock<PdfPagePathObject>()

        // Mock bitmap and matrix for imageObject as it's used in conversion
        whenever(imageObject.bitmap).thenReturn(mock())
        whenever(imageObject.matrix).thenReturn(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))

        // Mock toPath() for path object as it's used in conversion
        whenever(pathObject.toPath()).thenReturn(Path())
        whenever(pathObject.fillColor).thenReturn(0)
        whenever(pathObject.strokeWidth).thenReturn(1f)

        val allObjects =
            listOf(Pair(1, imageObject as PdfPageObject), Pair(2, pathObject as PdfPageObject))

        whenever(mockPage.getPageObjects()).thenReturn(allObjects)
        whenever(mockRenderer.withPage<List<KeyedPdfObject>>(any(), any())).thenAnswer {
            val block = it.getArgument<(PdfPage) -> List<KeyedPdfObject>>(1)
            block(mockPage)
        }

        val provider = PageObjectsProviderImpl(mockRenderer)

        // Filter for images only
        val imageResults = provider.getPageObjects(0, PdfDocument.INCLUDE_IMAGE_PAGE_OBJECT)
        assertThat(imageResults).hasSize(1)
        assertThat(imageResults[0].key).isEqualTo("1")

        // Filter for paths only
        val pathResults = provider.getPageObjects(0, PdfDocument.INCLUDE_PATH_PAGE_OBJECT)
        assertThat(pathResults).hasSize(1)
        assertThat(pathResults[0].key).isEqualTo("2")

        // Filter for both
        val bothResults = provider.getPageObjects(0, PdfDocument.PAGE_OBJECT_INCLUDE_ALL_TYPES)
        assertThat(bothResults).hasSize(2)
    }
}
