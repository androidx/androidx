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

package androidx.camera.camera2.compat.workaround

import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.impl.QuirkSettings
import androidx.camera.core.impl.QuirkSettingsHolder
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
public class DngCreatorSizeMismatchTest {

    private var originalBrand: String? = null
    private var originalModel: String? = null
    private var originalDevice: String? = null

    @Before
    public fun storeOriginalBuildFields() {
        originalBrand = Build.BRAND
        originalModel = Build.MODEL
        originalDevice = Build.DEVICE
        resetDeviceQuirks()
    }

    @After
    public fun restoreOriginalBuildFields() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", originalBrand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", originalModel)
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", originalDevice)
        resetDeviceQuirks()
    }

    private fun resetDeviceQuirks() {
        QuirkSettingsHolder.instance().set(QuirkSettings.withAllQuirksDisabled())
        QuirkSettingsHolder.instance().set(QuirkSettings.withDefaultBehavior())
    }

    @Test
    public fun filterRawSizes_filtersNonSensorSizesForAffectedDevice() {
        val affectedDevices = listOf("olivelite", "olive")
        val sensorSize = Size(4208, 3120)
        val rawFormats =
            listOf(
                ImageFormat.RAW_SENSOR,
                ImageFormat.RAW10,
                ImageFormat.RAW12,
                ImageFormat.RAW_PRIVATE,
            )

        val cameraMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to sensorSize)
            )

        for (device in affectedDevices) {
            ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", device)
            resetDeviceQuirks()

            val workaround = DngCreatorSizeMismatch(cameraMetadata)

            for (rawFormat in rawFormats) {
                val sizes =
                    mutableListOf(
                        Size(4208, 3120),
                        Size(4208, 2368),
                        Size(3200, 2400),
                        Size(4208, 1992),
                    )
                workaround.filterRawSizes(sizes, rawFormat)
                assertThat(sizes).containsExactly(Size(4208, 3120))
            }
        }
    }

    @Test
    public fun filterRawSizes_matchesPreCorrectionActiveArraySize() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "olivelite")
        resetDeviceQuirks()

        val pixelArraySize = Size(4208, 3120)
        val preCorrectionRect = Rect(0, 0, 4000, 3000)

        val cameraMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to pixelArraySize,
                        CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE to
                            preCorrectionRect,
                    )
            )

        val workaround = DngCreatorSizeMismatch(cameraMetadata)
        val sizes =
            mutableListOf(
                Size(4208, 3120),
                Size(4000, 3000),
                Size(4208, 2368),
                Size(3200, 2400),
                Size(4208, 1992),
            )

        workaround.filterRawSizes(sizes, ImageFormat.RAW_SENSOR)
        assertThat(sizes).containsExactly(Size(4208, 3120), Size(4000, 3000))
    }

    @Test
    public fun filterRawSizes_matchesSwappedSensorDimensions() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "olivelite")
        resetDeviceQuirks()

        val sensorSize = Size(4208, 3120)
        val cameraMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to sensorSize)
            )

        val workaround = DngCreatorSizeMismatch(cameraMetadata)
        val sizes = mutableListOf(Size(3120, 4208), Size(3200, 2400))

        workaround.filterRawSizes(sizes, ImageFormat.RAW_SENSOR)
        assertThat(sizes).containsExactly(Size(3120, 4208))
    }

    @Test
    public fun filterRawSizes_doesNotFilterNonRawFormats() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "olivelite")
        resetDeviceQuirks()

        val sensorSize = Size(4208, 3120)
        val cameraMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to sensorSize)
            )

        val workaround = DngCreatorSizeMismatch(cameraMetadata)
        val sizes =
            mutableListOf(Size(4208, 3120), Size(4208, 2368), Size(3200, 2400), Size(4208, 1992))

        workaround.filterRawSizes(sizes, ImageFormat.YUV_420_888)
        assertThat(sizes)
            .containsExactly(Size(4208, 3120), Size(4208, 2368), Size(3200, 2400), Size(4208, 1992))
            .inOrder()
    }

    @Test
    public fun filterRawSizes_doesNotFilterOnUnaffectedDevice() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "unaffected_device")
        resetDeviceQuirks()

        val sensorSize = Size(4208, 3120)
        val cameraMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to sensorSize)
            )

        val workaround = DngCreatorSizeMismatch(cameraMetadata)
        val sizes =
            mutableListOf(Size(4208, 3120), Size(4208, 2368), Size(3200, 2400), Size(4208, 1992))

        workaround.filterRawSizes(sizes, ImageFormat.RAW_SENSOR)
        assertThat(sizes)
            .containsExactly(Size(4208, 3120), Size(4208, 2368), Size(3200, 2400), Size(4208, 1992))
            .inOrder()
    }
}
