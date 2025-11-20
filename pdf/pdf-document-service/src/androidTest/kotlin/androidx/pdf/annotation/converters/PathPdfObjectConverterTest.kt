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

package androidx.pdf.annotation.converters

import android.graphics.pdf.component.PdfPagePathObject
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.randomizePathPdfObject
import androidx.pdf.utils.AnnotationUtilsTest.Companion.isRequiredSdkExtensionAvailable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(AndroidJUnit4::class)
class PathPdfObjectConverterTest {
    private lateinit var converter: PathPdfObjectConverter

    @Before
    fun setUp() {
        converter = PathPdfObjectConverter()
    }

    @Test
    fun convert_emptyPathPdfObject_returnsEmptyPath() {
        if (!isRequiredSdkExtensionAvailable()) return

        val pathPdfObject = PathPdfObject(brushColor = 0, brushWidth = 0f, inputs = emptyList())

        val result = converter.convert(pathPdfObject)

        assertThat(result.toPath().isEmpty).isTrue()
    }

    @Test
    fun convert_nonEmptyPathPdfObject_returnsNonEmptyPdfPathObject() {
        if (!isRequiredSdkExtensionAvailable()) return

        val pathPdfObject = randomizePathPdfObject(pathLength = 1)
        val expectedStrokeColor = 0
        val expectedStrokeWidth = 0f
        val expectedRenderMode = PdfPagePathObject.RENDER_MODE_FILL

        val result = converter.convert(pathPdfObject)

        assertThat(result.toPath().isEmpty).isFalse()
        assertThat(result.strokeColor).isEqualTo(expectedStrokeColor)
        assertThat(result.strokeWidth).isEqualTo(expectedStrokeWidth)
        assertThat(result.renderMode).isEqualTo(expectedRenderMode)
    }
}
