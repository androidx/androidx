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

package androidx.camera.camera2.pipe

import android.graphics.SurfaceTexture
import android.hardware.camera2.params.OutputConfiguration
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
internal class StreamTest {
    private val streamConfig1 =
        CameraStream.Config.create(size = Size(640, 480), format = StreamFormat.YUV_420_888)

    private val streamConfig2 =
        CameraStream.Config.create(size = Size(640, 480), format = StreamFormat.YUV_420_888)

    private val streamConfig3 =
        CameraStream.Config.create(size = Size(640, 480), format = StreamFormat.JPEG)

    @Test
    fun differentStreamConfigsAreNotEqual() {
        assertThat(streamConfig1).isNotEqualTo(streamConfig3)
        assertThat(streamConfig2).isNotEqualTo(streamConfig3)
    }

    @Test
    fun equivalentStreamConfigsAreNotEqual() {
        assertThat(streamConfig1).isNotEqualTo(streamConfig2)
        assertThat(streamConfig1).isNotSameInstanceAs(streamConfig2)
    }

    @Test
    fun equivalentOutputsAreNotEqual() {
        assertThat(streamConfig1.outputs.single()).isNotEqualTo(streamConfig2.outputs.single())
        assertThat(streamConfig1.outputs.single())
            .isNotSameInstanceAs(streamConfig2.outputs.single())
    }

    @Test
    fun sharedOutputsAreShared() {
        val outputConfig =
            OutputStream.Config.create(size = Size(640, 480), format = StreamFormat.YUV_420_888)
        val sharedConfig1 = CameraStream.Config.create(outputConfig)
        val sharedConfig2 = CameraStream.Config.create(outputConfig)
        assertThat(sharedConfig1).isNotEqualTo(sharedConfig2)
        assertThat(sharedConfig1.outputs.single()).isEqualTo(sharedConfig2.outputs.single())
        assertThat(sharedConfig1.outputs.single()).isSameInstanceAs(sharedConfig2.outputs.single())
    }

    @Test
    @Config(minSdk = 34)
    fun outputConfig_useReadoutTimestampDefaultsToNull() {
        val outputConfig =
            OutputStream.Config.create(size = Size(640, 480), format = StreamFormat.YUV_420_888)
        assertThat(outputConfig.useReadoutTimestamp).isNull()

        val outputConfigExplicitFalse =
            OutputStream.Config.create(
                size = Size(640, 480),
                format = StreamFormat.YUV_420_888,
                useReadoutTimestamp = false,
            )
        assertThat(outputConfigExplicitFalse.useReadoutTimestamp).isFalse()
    }

    @Test
    @Config(minSdk = 34)
    fun externalOutputConfig_useReadoutTimestampMirrorsOutputConfiguration() {
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        val externalOutputConfigEnabled =
            OutputConfiguration(surface).apply { setReadoutTimestampEnabled(true) }

        val externalConfigEnabled =
            OutputStream.Config.external(
                size = Size(640, 480),
                format = StreamFormat.YUV_420_888,
                externalOutputConfig = externalOutputConfigEnabled,
                streamUseHint = null,
            )
        assertThat(externalConfigEnabled.useReadoutTimestamp).isTrue()

        val externalOutputConfigDefault = OutputConfiguration(surface)
        val externalConfigDefault =
            OutputStream.Config.external(
                size = Size(640, 480),
                format = StreamFormat.YUV_420_888,
                externalOutputConfig = externalOutputConfigDefault,
                streamUseHint = null,
            )
        assertThat(externalConfigDefault.useReadoutTimestamp).isFalse()

        surface.release()
        surfaceTexture.release()
    }

    @Test
    @Config(sdk = [33])
    fun externalOutputConfig_api33_useReadoutTimestampIsNull() {
        val surfaceTexture = SurfaceTexture(0)
        val surface = Surface(surfaceTexture)
        val externalOutputConfig = OutputConfiguration(surface)

        val externalConfig =
            OutputStream.Config.external(
                size = Size(640, 480),
                format = StreamFormat.YUV_420_888,
                externalOutputConfig = externalOutputConfig,
                streamUseHint = null,
            )
        assertThat(externalConfig.useReadoutTimestamp).isNull()

        surface.release()
        surfaceTexture.release()
    }
}
