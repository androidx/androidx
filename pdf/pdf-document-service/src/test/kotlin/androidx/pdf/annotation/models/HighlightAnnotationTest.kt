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

package androidx.pdf.annotation.models

import android.graphics.Color
import android.graphics.RectF
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class HighlightAnnotationTest {

    @Test
    fun equals_sameValues_returnsTrue() {
        val bounds = listOf(RectF(10f, 10f, 20f, 20f), RectF(30f, 30f, 40f, 40f))
        val highlight1 = HighlightAnnotation(pageNum = 1, bounds = bounds, color = Color.YELLOW)
        val highlight2 = HighlightAnnotation(pageNum = 1, bounds = bounds, color = Color.YELLOW)

        assertThat(highlight1).isEqualTo(highlight2)
        assertThat(highlight1.hashCode()).isEqualTo(highlight2.hashCode())
    }

    @Test
    fun equals_differentValues_returnsFalse() {
        val bounds1 = listOf(RectF(10f, 10f, 20f, 20f))
        val bounds2 = listOf(RectF(0f, 0f, 5f, 5f))
        val highlight = HighlightAnnotation(pageNum = 1, bounds = bounds1, color = Color.YELLOW)

        // Different pageNum
        assertThat(highlight).isNotEqualTo(HighlightAnnotation(2, bounds1, Color.YELLOW))
        // Different bounds
        assertThat(highlight).isNotEqualTo(HighlightAnnotation(1, bounds2, Color.YELLOW))
        // Different color
        assertThat(highlight).isNotEqualTo(HighlightAnnotation(1, bounds1, Color.BLUE))
    }

    @Test
    fun parcelable_writeRead_preservesData() {
        val bounds = listOf(RectF(10.5f, 20.5f, 30.5f, 40.5f), RectF(50f, 60f, 70f, 80f))
        val original = HighlightAnnotation(pageNum = 5, bounds = bounds, color = Color.CYAN)

        val parcel = Parcel.obtain()
        try {
            // Write to parcel
            original.writeToParcel(parcel, 0)

            // Reset parcel for reading
            parcel.setDataPosition(0)

            // Read from parcel using CREATOR
            val createdFromParcel = HighlightAnnotation.CREATOR.createFromParcel(parcel)

            assertThat(createdFromParcel.pageNum).isEqualTo(original.pageNum)
            assertThat(createdFromParcel.color).isEqualTo(original.color)
            assertThat(createdFromParcel.bounds).hasSize(original.bounds.size)

            for (i in original.bounds.indices) {
                val originalRect = original.bounds[i]
                val parcelledRect = createdFromParcel.bounds[i]
                assertThat(parcelledRect.left).isEqualTo(originalRect.left)
                assertThat(parcelledRect.top).isEqualTo(originalRect.top)
                assertThat(parcelledRect.right).isEqualTo(originalRect.right)
                assertThat(parcelledRect.bottom).isEqualTo(originalRect.bottom)
            }

            // Final equality check
            assertThat(createdFromParcel).isEqualTo(original)
        } finally {
            parcel.recycle()
        }
    }
}
