/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.PerceivedResolutionResult
import androidx.xr.runtime.internal.PixelDimensions
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakePanelEntityTest {
    private lateinit var underTest: FakePanelEntity

    @Before
    fun setUp() {
        underTest = FakePanelEntity()
    }

    @Test
    fun getSizeInPixels_returnsDefaultValue() {
        check(underTest.sizeInPixels == PixelDimensions(640, 480))
    }

    @Test
    fun getCornerRadius_setNegativeCornerRadius_returnLastValidCornerRadius() {
        // Default value
        check(underTest.cornerRadius == 32.0f)

        underTest.cornerRadius = -64.0f

        assertThat(underTest.cornerRadius).isEqualTo(32.0f)
    }

    @Test
    fun getSize_setNegativeSize_returnLastValidSize() {
        // Default value
        val defaultSize = Dimensions(1.0f, 1.0f, 0.0f)
        check(underTest.size == defaultSize)

        // Set a negative width value.
        underTest.size = Dimensions(-1280.0f, 720.0f, 1.0f)

        assertThat(underTest.size).isEqualTo(defaultSize)

        // Set a negative height value.
        underTest.size = Dimensions(1280.0f, -720.0f, 1.0f)

        assertThat(underTest.size).isEqualTo(defaultSize)

        // Set a negative depth value.
        underTest.size = Dimensions(1280.0f, 720.0f, -1.0f)

        assertThat(underTest.size).isEqualTo(defaultSize)
    }

    @Test
    fun getSize_setNonZeroPositiveDepthSize_returnsZeroDepthSize() {
        // Default value
        val defaultSize = Dimensions(1.0f, 1.0f, 0.0f)
        check(underTest.size == defaultSize)

        // Set a non-zero positive depth value.
        underTest.size = Dimensions(2.0f, 3.0f, 1.0f)

        // Value set successfully but returned with depth set to 0.
        assertThat(underTest.size).isEqualTo(Dimensions(2.0f, 3.0f, 0.0f))
    }

    @Test
    fun getPerceivedResolutionResult_withDifferentResults() {
        // Default value
        assertThat(underTest.getPerceivedResolution())
            .isInstanceOf(PerceivedResolutionResult.InvalidCameraView::class.java)

        // Set the EntityTooClose result.
        underTest.setPerceivedResolution(PerceivedResolutionResult.EntityTooClose())

        assertThat(underTest.getPerceivedResolution())
            .isInstanceOf(PerceivedResolutionResult.EntityTooClose::class.java)

        // Set the Success result.
        underTest.setPerceivedResolution(
            PerceivedResolutionResult.Success(PixelDimensions(640, 480))
        )
        val successResult = underTest.getPerceivedResolution() as PerceivedResolutionResult.Success

        assertThat(successResult).isInstanceOf(PerceivedResolutionResult.Success::class.java)
        assertThat(successResult.perceivedResolution).isEqualTo(PixelDimensions(640, 480))
    }
}
