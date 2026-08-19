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

package androidx.camera.common

import androidx.camera.common.CaptureResultWrappers.lensShadingMap
import androidx.camera.common.testing.FakeCaptureResult
import androidx.camera.common.testing.FakeLensShadingMap
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [LensShadingMapWrapper]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public final class LensShadingMapWrapperTest {

    @Test
    public fun fakeLensShadingMap_defaultConstructor() {
        val fake = FakeLensShadingMap(rowCount = 2, columnCount = 3)
        assertThat(fake.rowCount).isEqualTo(2)
        assertThat(fake.columnCount).isEqualTo(3)
        assertThat(fake.gainFactorCount).isEqualTo(2 * 3 * 4)

        // All gains should be 1.0f by default
        for (r in 0 until 2) {
            for (c in 0 until 3) {
                for (ch in 0..3) {
                    assertThat(fake.getGainFactor(ch, c, r)).isEqualTo(1.0f)
                }
                val vector = fake.getGainFactorVector(c, r)
                assertThat(vector.red).isEqualTo(1.0f)
                assertThat(vector.greenEven).isEqualTo(1.0f)
                assertThat(vector.greenOdd).isEqualTo(1.0f)
                assertThat(vector.blue).isEqualTo(1.0f)
            }
        }
    }

    @Test
    public fun fakeLensShadingMap_customGains() {
        val gains =
            floatArrayOf(
                1.0f,
                1.1f,
                1.2f,
                1.3f, // r=0, c=0
                2.0f,
                2.1f,
                2.2f,
                2.3f, // r=0, c=1
                3.0f,
                3.1f,
                3.2f,
                3.3f, // r=1, c=0
                4.0f,
                4.1f,
                4.2f,
                4.3f, // r=1, c=1
            )
        val fake = FakeLensShadingMap(rowCount = 2, columnCount = 2, gainFactors = gains)

        assertThat(fake.getGainFactor(LensShadingMapWrapper.COLOR_CHANNEL_RED, 0, 0))
            .isEqualTo(1.0f)
        assertThat(fake.getGainFactor(LensShadingMapWrapper.COLOR_CHANNEL_GREEN_RED, 1, 0))
            .isEqualTo(2.1f)
        assertThat(fake.getGainFactor(LensShadingMapWrapper.COLOR_CHANNEL_GREEN_BLUE, 0, 1))
            .isEqualTo(3.2f)
        assertThat(fake.getGainFactor(LensShadingMapWrapper.COLOR_CHANNEL_BLUE, 1, 1))
            .isEqualTo(4.3f)

        val vector = fake.getGainFactorVector(1, 0)
        assertThat(vector.red).isEqualTo(2.0f)
        assertThat(vector.greenEven).isEqualTo(2.1f)
        assertThat(vector.greenOdd).isEqualTo(2.2f)
        assertThat(vector.blue).isEqualTo(2.3f)
    }

    @Test
    public fun fakeLensShadingMap_copyGainFactors() {
        val gains = floatArrayOf(1.0f, 1.1f, 1.2f, 1.3f, 2.0f, 2.1f, 2.2f, 2.3f)
        val fake = FakeLensShadingMap(rowCount = 1, columnCount = 2, gainFactors = gains)
        val dest = FloatArray(8)
        fake.copyGainFactors(dest, 0)
        assertThat(dest).isEqualTo(gains)

        val destWithOffset = FloatArray(10)
        fake.copyGainFactors(destWithOffset, 2)
        assertThat(destWithOffset[0]).isEqualTo(0.0f)
        assertThat(destWithOffset[1]).isEqualTo(0.0f)
        assertThat(destWithOffset.sliceArray(2..9)).isEqualTo(gains)
    }

    @Test
    public fun captureResultMetadata_lensShadingMap_extensionProperty() {
        val fakeLensShadingMap = FakeLensShadingMap(rowCount = 2, columnCount = 2)
        val fakeResult =
            FakeCaptureResult(
                cameraId = CameraId("0"),
                frameNumber = CameraFrameNumber(1L),
                resultMetadata =
                    mapOf(CaptureResultWrapper.Keys.LENS_SHADING_MAP to fakeLensShadingMap),
            )

        // Access via extension property
        val retrievedMap = fakeResult.lensShadingMap
        assertThat(retrievedMap).isSameInstanceAs(fakeLensShadingMap)
    }

    @Test
    public fun fakeLensShadingMap_invalidInputs_throwExceptions() {
        // Invalid gainFactors size in constructor
        var exception =
            assertThrows(IllegalArgumentException::class.java) {
                FakeLensShadingMap(rowCount = 2, columnCount = 3, gainFactors = FloatArray(10))
            }
        assertThat(exception.message).contains("gainFactors.size (10) must be equal to 24")

        val fake = FakeLensShadingMap(rowCount = 2, columnCount = 3)

        // Invalid color channel
        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactor(colorChannel = -1, column = 0, row = 0)
            }
        assertThat(exception.message).contains("colorChannel (-1) must be between 0 and 3")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactor(colorChannel = 4, column = 0, row = 0)
            }
        assertThat(exception.message).contains("colorChannel (4) must be between 0 and 3")

        // Invalid column
        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactor(colorChannel = 0, column = -1, row = 0)
            }
        assertThat(exception.message).contains("column (-1) must be between 0 and 3")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactor(colorChannel = 0, column = 3, row = 0)
            }
        assertThat(exception.message).contains("column (3) must be between 0 and 3")

        // Invalid row
        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactor(colorChannel = 0, column = 0, row = -1)
            }
        assertThat(exception.message).contains("row (-1) must be between 0 and 2")

        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactor(colorChannel = 0, column = 0, row = 2)
            }
        assertThat(exception.message).contains("row (2) must be between 0 and 2")

        // Invalid column for Vector
        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactorVector(column = 3, row = 0)
            }
        assertThat(exception.message).contains("column (3) must be between 0 and 3")

        // Invalid row for Vector
        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.getGainFactorVector(column = 0, row = 2)
            }
        assertThat(exception.message).contains("row (2) must be between 0 and 2")

        // copyGainFactors with too small destination
        val dest = FloatArray(fake.gainFactorCount - 1)
        exception =
            assertThrows(IllegalArgumentException::class.java) {
                fake.copyGainFactors(dest, offset = 0)
            }
        assertThat(exception.message).contains("destination.size - offset (23) must be at least 24")
    }
}
