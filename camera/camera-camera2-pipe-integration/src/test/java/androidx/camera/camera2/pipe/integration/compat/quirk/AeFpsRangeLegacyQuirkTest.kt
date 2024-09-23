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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Range
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.internal.compat.quirk.AeFpsRangeQuirk
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AeFpsRangeLegacyQuirkTest {
    @Test
    fun validEntryExists_correctRangeIsSelected() {
        val availableFpsRanges: Array<Range<Int>> =
            arrayOf(Range(25, 30), Range(7, 33), Range(15, 30), Range(11, 22), Range(30, 30))
        val aeFpsRangeQuirk =
            createAeFpsRangeQuirk(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                availableFpsRanges
            )
        assertThat(aeFpsRangeQuirk!!.targetAeFpsRange).isEqualTo(Range(15, 30))
    }

    @Test
    fun noValidEntry_doesNotSetFpsRange() {
        val availableFpsRanges: Array<Range<Int>> =
            arrayOf(Range(25, 25), Range(7, 33), Range(15, 24), Range(11, 22))
        val aeFpsRangeQuirk =
            createAeFpsRangeQuirk(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                availableFpsRanges
            )
        assertThat(aeFpsRangeQuirk!!.targetAeFpsRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun availableArrayIsNull_doesNotSetFpsRange() {
        val aeFpsRangeQuirk =
            createAeFpsRangeQuirk(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, null)
        assertThat(aeFpsRangeQuirk!!.targetAeFpsRange)
            .isEqualTo(StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun limitedDevices_doesNotSetFpsRange() {
        val availableFpsRanges: Array<Range<Int>> = arrayOf(Range(15, 30))
        val aeFpsRangeQuirk =
            createAeFpsRangeQuirk(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                availableFpsRanges
            )
        assertThat(aeFpsRangeQuirk).isNull()
    }

    @Test
    fun fullDevices_doesNotSetFpsRange() {
        val availableFpsRanges: Array<Range<Int>> = arrayOf(Range(15, 30))
        val aeFpsRangeQuirk =
            createAeFpsRangeQuirk(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                availableFpsRanges
            )
        assertThat(aeFpsRangeQuirk).isNull()
    }

    @Test
    fun level3Devices_doesNotSetFpsRange() {
        val availableFpsRanges: Array<Range<Int>> = arrayOf(Range(15, 30))
        val aeFpsRangeQuirk =
            createAeFpsRangeQuirk(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
                availableFpsRanges
            )
        assertThat(aeFpsRangeQuirk).isNull()
    }

    private fun createAeFpsRangeQuirk(
        hardwareLevel: Int,
        availableFpsRanges: Array<Range<Int>>?
    ): AeFpsRangeQuirk? {
        val streamConfigurationMap = StreamConfigurationMapBuilder.newBuilder().build()

        val metadata =
            FakeCameraMetadata(
                mapOf(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to
                        availableFpsRanges,
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to streamConfigurationMap
                )
            )
        return CameraQuirks(
                metadata,
                StreamConfigurationMapCompat(
                    streamConfigurationMap,
                    OutputSizesCorrector(metadata, streamConfigurationMap)
                )
            )
            .quirks
            .getAll(AeFpsRangeQuirk::class.java)
            .firstOrNull()
    }
}
