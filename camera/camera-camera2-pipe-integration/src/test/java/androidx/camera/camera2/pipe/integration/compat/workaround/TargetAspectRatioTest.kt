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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Range
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TargetAspectRatioTest(val config: TestConfig) {

    companion object {
        private const val BACK_CAMERA_ID: String = "0"
        private val ALL_API_LEVELS: Range<Int> = Range(0, Int.MAX_VALUE)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "config={0}")
        fun data() =
            arrayListOf<Array<Any>>().apply {
                add(
                    arrayOf(
                        TestConfig(
                            "Google",
                            "Nexus 4",
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 22)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            "Google",
                            "Nexus 4",
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 22)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            "Google",
                            "Nexus 4",
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 22)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            "Google",
                            "Nexus 4",
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 22)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                            TargetAspectRatio.RATIO_ORIGINAL,
                            ALL_API_LEVELS
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                            TargetAspectRatio.RATIO_ORIGINAL,
                            ALL_API_LEVELS
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                            TargetAspectRatio.RATIO_ORIGINAL,
                            ALL_API_LEVELS
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                            TargetAspectRatio.RATIO_ORIGINAL,
                            ALL_API_LEVELS
                        )
                    )
                )
                // Test the legacy camera/Android 5.0 quirk.
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 21)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 21)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 21)
                        )
                    )
                )
                add(
                    arrayOf(
                        TestConfig(
                            null,
                            null,
                            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                            TargetAspectRatio.RATIO_MAX_JPEG,
                            Range<Int>(21, 21)
                        )
                    )
                )
            }
    }

    @Test
    fun getCorrectedRatio() {
        // Set up device properties
        if (config.brand != null) {
            ReflectionHelpers.setStaticField(Build::class.java, "BRAND", config.brand)
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", config.model)
        }

        val map = StreamConfigurationMapBuilder.newBuilder().build()

        val cameraMetadata =
            FakeCameraMetadata(
                cameraId = CameraId(BACK_CAMERA_ID),
                characteristics =
                    mapOf(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to config.hardwareLevel,
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to map
                    )
            )

        val outputSizesCorrector = OutputSizesCorrector(cameraMetadata, map)

        val streamConfigurationMapCompat = StreamConfigurationMapCompat(map, outputSizesCorrector)

        val aspectRatio: Int = TargetAspectRatio().get(cameraMetadata, streamConfigurationMapCompat)
        Truth.assertThat(aspectRatio).isEqualTo(getExpectedAspectRatio())
    }

    @TargetAspectRatio.Ratio
    private fun getExpectedAspectRatio(): Int {
        return if (config.affectedApiLevels.contains(Build.VERSION.SDK_INT))
            config.expectedAspectRatio
        else TargetAspectRatio.RATIO_ORIGINAL
    }

    data class TestConfig
    internal constructor(
        val brand: String?,
        val model: String?,
        val hardwareLevel: Int,
        @field:TargetAspectRatio.Ratio @param:TargetAspectRatio.Ratio val expectedAspectRatio: Int,
        val affectedApiLevels: Range<Int>
    )
}
