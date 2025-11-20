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

package androidx.pdf.annotation.processor

import android.os.Parcel
import androidx.pdf.annotation.createPdfAnnotationData
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor.Companion.parcelSizeInBytes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfAnnotationDataParcelSizeTest {
    @Test
    fun parcelSizeInBytes_emptyPath_returnsCorrectSize() {
        // Arrange
        val annotationData = createPdfAnnotationData(pageNum = 0, pathLength = 0)

        // Act
        val actualSize = annotationData.parcelSizeInBytes()

        // Assert
        val parcel = Parcel.obtain()
        annotationData.writeToParcel(parcel, 0)
        val expectedSize = parcel.dataSize()
        parcel.recycle()

        assertThat(actualSize).isEqualTo(expectedSize)
    }

    @Test
    fun parcelSizeInBytes_withNonEmptyPath_returnsLargerSize() {
        // Arrange
        val annotationDataWithPath = createPdfAnnotationData(pageNum = 0, pathLength = 10)
        val annotationDataWithoutPath = createPdfAnnotationData(pageNum = 0, pathLength = 0)

        // Act
        val actualSizeWithPath = annotationDataWithPath.parcelSizeInBytes()

        // Assert
        var parcel = Parcel.obtain()
        annotationDataWithPath.writeToParcel(parcel, 0)
        val expectedSizeWithPath = parcel.dataSize()
        parcel.recycle()

        parcel = Parcel.obtain()
        annotationDataWithoutPath.writeToParcel(parcel, 0)
        val expectedSizeWithoutPath = parcel.dataSize()
        parcel.recycle()

        assertThat(actualSizeWithPath).isEqualTo(expectedSizeWithPath)
        assertThat(expectedSizeWithPath > expectedSizeWithoutPath).isTrue()
    }

    @Test
    fun parcelSizeInBytes_multipleAnnotations_sizesAreConsistent() {
        // Arrange
        val annotation1 = createPdfAnnotationData(pageNum = 0, pathLength = 10)
        val annotation2 = createPdfAnnotationData(pageNum = 0, pathLength = 10)

        // Act
        val actualSize1 = annotation1.parcelSizeInBytes()
        val actualSize2 = annotation2.parcelSizeInBytes()

        // Assert
        val parcel1 = Parcel.obtain()
        annotation1.writeToParcel(parcel1, 0)
        val expectedSize1 = parcel1.dataSize()
        parcel1.recycle()

        val parcel2 = Parcel.obtain()
        annotation2.writeToParcel(parcel2, 0)
        val expectedSize2 = parcel2.dataSize()
        parcel2.recycle()

        assertThat(actualSize1).isEqualTo(expectedSize1)
        assertThat(actualSize2).isEqualTo(expectedSize2)
    }
}
