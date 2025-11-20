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

import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.createStampAnnotationWithPath
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
class StampAnnotationConverterTest {
    private lateinit var converter: StampAnnotationConverter

    @Before
    fun setUp() {
        converter = StampAnnotationConverter()
    }

    @Test
    fun convert_emptyPdfObjects_returnsAospStampAnnotationWithNoObjects() {
        if (!isRequiredSdkExtensionAvailable()) return

        val stampAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 0)
        val expectedBounds = RectF(0f, 0f, 0f, 0f)

        val result = converter.convert(stampAnnotation)

        assertThat(result.bounds).isEqualTo(expectedBounds)
        assertThat(result.objects.isEmpty()).isTrue()
    }

    @Test
    fun convert_withPdfObjects_returnsAospStampAnnotationWithObjects() {
        if (!isRequiredSdkExtensionAvailable()) return

        val stampAnnotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 1)

        val result = converter.convert(stampAnnotation)

        assertThat(result.bounds).isNotNull()
        assertThat(result.objects).isNotNull()
    }
}
