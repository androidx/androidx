/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.camera2.pipe

import androidx.camera.integration.camera2.pipe.CameraMetadataKey.CONTROL_AE_MODE
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.LENS_FOCAL_LENGTH
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.LENS_FOCUS_DISTANCE
import androidx.camera.integration.camera2.pipe.CameraMetadataKey.STATISTICS_FACES
import androidx.camera.integration.camera2.pipe.transformations.DataTransformations1D
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DataTransformations1DTests {

    /**
     * LENS_FOCUS_DISTANCE tests
     */
    @Test
    fun lensFocusDistance_nullTest() {
        assertThat(DataTransformations1D.convert(LENS_FOCUS_DISTANCE, null)).isNull()
    }

    @Test
    fun lensFocusDistance_floatTest() {
        val nums: MutableList<Float> = mutableListOf(1f, 32f, 64f, 1024f)
        val transformedNums: MutableList<Float> = mutableListOf(1 / 1f, 1 / 32f, 1 / 64f, 1 / 1024f)

        (0 until nums.size).forEach {
            val transformedData = DataTransformations1D.convert(LENS_FOCUS_DISTANCE, nums[it])
            assertThat(transformedData is Float).isTrue()
            assertThat(transformedData as Float)
                .isWithin(0.0001f)
                .of(transformedNums[it])
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun lensFocusDistance_intTest() {
        DataTransformations1D.convert(LENS_FOCUS_DISTANCE, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun lensFocusDistance_booleanTest() {
        DataTransformations1D.convert(LENS_FOCUS_DISTANCE, true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun lensFocusDistance_arrayTest() {
        DataTransformations1D.convert(LENS_FOCUS_DISTANCE, arrayListOf(1, 2))
    }

    /**
     * CONTROL_AE_MODE tests
     */
    @Test
    fun controlAEMode_nullTest() {
        assertThat(DataTransformations1D.convert(CONTROL_AE_MODE, null)).isNull()
    }

    @Test
    fun controlAEMode_arrayOfIntTest() {
        val keyData = 4
        val transformedData = DataTransformations1D.convert(CONTROL_AE_MODE, keyData)
        assertThat(transformedData).isEqualTo(keyData)
    }

    @Test(expected = IllegalArgumentException::class)
    fun controlAEMode_floatTest() {
        DataTransformations1D.convert(CONTROL_AE_MODE, 18F)
    }

    /**
     * STATISTICS_FACES tests
     */
    @Test
    fun statisticsFaces_nullTest() {
        assertThat(DataTransformations1D.convert(STATISTICS_FACES, null)).isNull()
    }

    @Test(expected = IllegalArgumentException::class)
    fun statisticsFaces_arrayOfIntTest() {
        val faces: Array<Int> = arrayOf()
        DataTransformations1D.convert(STATISTICS_FACES, faces)
    }

    @Test(expected = IllegalArgumentException::class)
    fun statisticsFaces_nonArrayTest() {
        DataTransformations1D.convert(STATISTICS_FACES, 18L)
    }

    /**
     * Tests for keys with no specific transformation e.g. LENS_FOCAL_LENGTH
     */
    @Test
    fun lensFocalLength_nullTest() {
        assertThat(DataTransformations1D.convert(LENS_FOCAL_LENGTH, null)).isNull()
    }

    @Test
    fun lensFocalLength_numberTest() {
        val keyDataFloat = 29.9f
        val transformedDataFloat = DataTransformations1D.convert(LENS_FOCAL_LENGTH, keyDataFloat)
        assertThat(transformedDataFloat is Float).isTrue()
        assertThat(keyDataFloat)
            .isWithin(0.0001f)
            .of(transformedDataFloat as Float)

        val keyDataInt = 29
        val transformedDataInt = DataTransformations1D.convert(LENS_FOCAL_LENGTH, keyDataInt)
        assertThat(transformedDataInt is Int).isTrue()
        assertThat(keyDataInt).isEqualTo(transformedDataInt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun lensFocalLength_booleanTest() {
        DataTransformations1D.convert(LENS_FOCAL_LENGTH, true)
    }
}
