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

package androidx.pdf.annotation.drawer

import android.graphics.RectF
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfAnnotationDrawerFactoryImplTest {

    private lateinit var pdfObjectDrawerFactory: PdfObjectDrawerFactory
    private lateinit var pdfAnnotationDrawerFactory: PdfAnnotationDrawerFactoryImpl

    @Before
    fun setUp() {
        pdfObjectDrawerFactory = FakePdfObjectDrawerFactory()
        pdfAnnotationDrawerFactory = PdfAnnotationDrawerFactoryImpl(pdfObjectDrawerFactory)
    }

    @Test
    fun create_withStampAnnotation_returnsStampPdfAnnotationDrawer() {
        val stampAnnotation =
            StampAnnotation(pageNum = 0, bounds = RectF(0f, 0f, 10f, 10f), pdfObjects = emptyList())
        val drawer = pdfAnnotationDrawerFactory.create(stampAnnotation)
        assertThat(drawer).isInstanceOf(StampPdfAnnotationDrawer::class.java)
    }

    @Test
    fun create_withUnsupportedAnnotationType_throwsIllegalArgumentException() {
        val unsupportedAnnotation = object : PdfAnnotation(pageNum = 0) {}

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                pdfAnnotationDrawerFactory.create(unsupportedAnnotation)
            }
        assertThat(exception.message)
            .isEqualTo("Unsupported PdfAnnotation type: $unsupportedAnnotation")
    }
}
