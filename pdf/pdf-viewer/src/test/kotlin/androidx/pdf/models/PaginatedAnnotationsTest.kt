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

package androidx.pdf.models

import android.os.Parcel
import androidx.pdf.annotation.AnnotationHandleIdGenerator.composeAnnotationId
import androidx.pdf.annotation.models.PaginatedAnnotations
import com.google.common.truth.Truth.assertThat
import createDummyKeyedPdfAnnotation
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PaginatedAnnotationsTest {
    @Test
    fun test_createFromParcel_nonEmptyList_returnsNonEmptyAnnotations() {
        // Arrange
        val mockIds = listOf("first", "second").map { composeAnnotationId(0, it) }
        val mockPageAnnotationDataList =
            mockIds.map { createDummyKeyedPdfAnnotation(pageNum = 0, id = it) }
        val original =
            PaginatedAnnotations(
                mockPageAnnotationDataList,
                currentBatchIndex = 0,
                totalBatchCount = 10,
            )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        // Act
        val recreated = PaginatedAnnotations.CREATOR.createFromParcel(parcel)

        // Assert
        assertThat(recreated.annotations).isEqualTo(original.annotations)
        assertThat(recreated.currentBatchIndex).isEqualTo(original.currentBatchIndex)
        assertThat(recreated.totalBatchCount).isEqualTo(original.totalBatchCount)

        parcel.recycle()
    }

    @Test
    fun test_createFromParcel_emptyList_returnsEmptyAnnotations() {
        // Arrange
        val original =
            PaginatedAnnotations(
                annotations = listOf(),
                currentBatchIndex = 0,
                totalBatchCount = 10,
            )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        // Act
        val recreated = PaginatedAnnotations.CREATOR.createFromParcel(parcel)

        // Assert
        assertThat(recreated.annotations).isEqualTo(original.annotations)
        assertThat(recreated.currentBatchIndex).isEqualTo(original.currentBatchIndex)
        assertThat(recreated.totalBatchCount).isEqualTo(original.totalBatchCount)

        // Clean up the Parcel
        parcel.recycle()
    }
}
