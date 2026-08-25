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

package androidx.camera.core.internal.compat.quirk

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW_JPEG
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@Config(sdk = [Config.ALL_SDKS])
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class SimultaneousRawJpegNotSupportedQuirkTest {

    @Test
    fun load_returnsTrue_whenDeviceIsOp56dbl1() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "op56dbl1")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "other")
        assertThat(SimultaneousRawJpegNotSupportedQuirk.load()).isTrue()
    }

    @Test
    fun load_returnsTrue_whenModelIsCph2525() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "other")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "cph2525")
        assertThat(SimultaneousRawJpegNotSupportedQuirk.load()).isTrue()
    }

    @Test
    fun load_returnsFalse_forOtherDevices() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "pixel7")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Pixel 7")
        assertThat(SimultaneousRawJpegNotSupportedQuirk.load()).isFalse()
    }

    @Test
    fun supportedOutputFormats_excludesRawJpeg_whenQuirkLoaded() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "op56dbl1")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "cph2525")

        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setAvailableCapabilities(
                    setOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
                )
                setSupportedResolutions(ImageFormat.RAW_SENSOR, listOf(Size(4096, 3072)))
                setSupportedResolutions(ImageFormat.JPEG, listOf(Size(4096, 3072)))
            }

        val capabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo)
        assertThat(capabilities.supportedOutputFormats)
            .containsExactly(OUTPUT_FORMAT_JPEG, OUTPUT_FORMAT_RAW)
    }

    @Test
    fun supportedOutputFormats_includesRawJpeg_whenQuirkNotLoaded() {
        ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", "pixel7")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Pixel 7")

        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setAvailableCapabilities(
                    setOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
                )
                setSupportedResolutions(ImageFormat.RAW_SENSOR, listOf(Size(4096, 3072)))
                setSupportedResolutions(ImageFormat.JPEG, listOf(Size(4096, 3072)))
            }

        val capabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo)
        assertThat(capabilities.supportedOutputFormats)
            .containsExactly(OUTPUT_FORMAT_JPEG, OUTPUT_FORMAT_RAW, OUTPUT_FORMAT_RAW_JPEG)
    }
}
