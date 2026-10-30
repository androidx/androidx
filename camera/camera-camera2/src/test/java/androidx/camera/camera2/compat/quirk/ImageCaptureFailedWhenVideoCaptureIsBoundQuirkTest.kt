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

package androidx.camera.camera2.compat.quirk

import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.core.impl.Quirks
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class ImageCaptureFailedWhenVideoCaptureIsBoundQuirkTest(
    private val brand: String,
    private val model: String,
    private val device: String,
    private val enabled: Boolean,
    private val captureIntentPreview: Boolean,
    private val surfaceProcessing: Boolean,
) {
    @After
    fun tearDown() {
        ShadowBuild.reset()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Brand: {0}, Model: {1}, Device: {2}, Enabled: {3}"
        )
        fun data() =
            listOf(
                arrayOf<Any>("BLU", "Studio X10", "studio_x10", true, true, true),
                arrayOf<Any>("Itel", "itel w6004", "w6004", true, true, true),
                arrayOf<Any>("Vivo", "vivo 1805", "1805", true, true, true),
                arrayOf<Any>("Positivo", "Twist 2 Pro", "twist_2_pro", true, true, true),
                arrayOf<Any>("Motorola", "moto e13", "e13", true, false, true),
                arrayOf<Any>("Samsung", "SM-X200", "gta8wifi", true, false, true),
                arrayOf<Any>("Samsung", "SM-A536U", "a53x", true, false, true),
                arrayOf<Any>("TECNO-MOBILE", "tecno mobile bf6", "TECNO-BF6", true, true, true),
                arrayOf<Any>("TECNO", "TECNO BF6", "TECNO-BF6", true, true, true),
                arrayOf<Any>("TECNO", "TECNO Spark 10", "TECNO-KI5q", false, false, false),
                arrayOf<Any>("Google", "Pixel 7", "panther", false, false, false),
            )
    }

    private fun getCameraQuirks(): Quirks {
        val cameraMetadata = FakeCameraMetadata.fromTemplate(template = HighEndDeviceTemplate)
        val map = StreamConfigurationMapBuilder.newBuilder().build()
        return CameraQuirks(cameraMetadata, StreamConfigurationMapCompat(map, cameraMetadata))
            .quirks
    }

    @Test
    fun canEnableQuirkAndWorkaroundsCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel(model)
        ShadowBuild.setDevice(device)
        val cameraQuirks = getCameraQuirks()
        val quirk = cameraQuirks.get(ImageCaptureFailedWhenVideoCaptureIsBoundQuirk::class.java)
        assertThat(quirk != null).isEqualTo(enabled)
        if (quirk != null) {
            assertThat(quirk.workaroundByCaptureIntentPreview()).isEqualTo(captureIntentPreview)
            assertThat(quirk.workaroundBySurfaceProcessing()).isEqualTo(surfaceProcessing)
        }
    }
}
