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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class CameraCrashWhenHlg10UhdWithVideoStabilizationQuirkTest(
    private val brand: String,
    private val device: String,
    private val enabled: Boolean,
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "Brand: {0}, device: {1}, enabled: {2}"
        )
        fun data() =
            listOf(
                // Galaxy S26, S26+ and S26 Ultra.
                arrayOf<Any>("samsung", "m1q", true),
                arrayOf<Any>("samsung", "m2q", true),
                arrayOf<Any>("samsung", "m3q", true),
                arrayOf<Any>("Samsung", "M1Q", true),
                // Galaxy Z Fold8 and Galaxy S25, not affected.
                arrayOf<Any>("samsung", "h8q", false),
                arrayOf<Any>("samsung", "pa1q", false),
                // Other brand.
                arrayOf<Any>("google", "m1q", false),
            )
    }

    @Test
    fun canEnableQuirkCorrectly() {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(brand)
        ShadowBuild.setDevice(device)

        val cameraMetadata = FakeCameraMetadata()
        val map = StreamConfigurationMapBuilder.newBuilder().build()
        val quirks =
            CameraQuirks(cameraMetadata, StreamConfigurationMapCompat(map, cameraMetadata)).quirks

        assertThat(quirks.contains(CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk::class.java))
            .isEqualTo(enabled)
    }
}
