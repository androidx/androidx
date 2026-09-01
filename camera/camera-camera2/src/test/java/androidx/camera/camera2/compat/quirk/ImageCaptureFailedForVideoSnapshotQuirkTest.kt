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
class ImageCaptureFailedForVideoSnapshotQuirkTest(
    private val model: String,
    private val enabled: Boolean,
) {
    @After
    fun tearDown() {
        ShadowBuild.reset()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Model: {0}, Enabled: {1}")
        fun data() =
            listOf(
                arrayOf<Any>("itel l6006", true),
                arrayOf<Any>("itel w6004", true),
                arrayOf<Any>("moto g(20)", true),
                arrayOf<Any>("moto e13", true),
                arrayOf<Any>("moto e20", true),
                arrayOf<Any>("rmx3231", true),
                arrayOf<Any>("rmx3263", true),
                arrayOf<Any>("rmx3511", true),
                arrayOf<Any>("sm-a032f", true),
                arrayOf<Any>("sm-a035m", true),
                arrayOf<Any>("sm-f936u1", true),
                arrayOf<Any>("SM-F936U1", true),
                arrayOf<Any>("sm-f946u1", true),
                arrayOf<Any>("tecno mobile bf6", true),
                arrayOf<Any>("Pixel 7", false),
                arrayOf<Any>("sm-s928u1", false),
            )
    }

    private fun getCameraQuirks(): Quirks {
        val cameraMetadata = FakeCameraMetadata.fromTemplate(template = HighEndDeviceTemplate)
        val map = StreamConfigurationMapBuilder.newBuilder().build()
        return CameraQuirks(cameraMetadata, StreamConfigurationMapCompat(map, cameraMetadata))
            .quirks
    }

    @Test
    fun canEnableQuirkCorrectly() {
        ShadowBuild.setModel(model)
        val cameraQuirks = getCameraQuirks()
        assertThat(cameraQuirks.contains(ImageCaptureFailedForVideoSnapshotQuirk::class.java))
            .isEqualTo(enabled)
    }
}
