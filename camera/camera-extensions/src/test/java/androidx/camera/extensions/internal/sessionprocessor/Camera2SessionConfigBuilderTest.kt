/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.internal.sessionprocessor

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Camera2SessionConfigBuilderTest {
    @Test
    fun canAddOutputConfig() {
        // Arrange
        val builder = Camera2SessionConfigBuilder()
        val outputConfig1 = mock(Camera2OutputConfig::class.java)
        val outputConfig2 = mock(Camera2OutputConfig::class.java)

        // Act
        builder.addOutputConfig(outputConfig1)
        builder.addOutputConfig(outputConfig2)

        // Assert
        val sessionConfig = builder.build()
        assertThat(sessionConfig.outputConfigs).containsExactly(outputConfig1, outputConfig2)
    }

    @Test
    fun canSetTemplateId() {
        // Arrange
        val builder = Camera2SessionConfigBuilder()

        // Act
        builder.sessionTemplateId = CameraDevice.TEMPLATE_VIDEO_SNAPSHOT

        // Assert
        val sessionConfig = builder.build()
        assertThat(sessionConfig.sessionTemplateId).isEqualTo(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT)
    }

    @Test
    fun canSetSessionParameters() {
        // Arrange
        val builder = Camera2SessionConfigBuilder()

        // Act
        builder.addSessionParameter(
            CaptureRequest.CONTROL_AWB_MODE,
            CaptureRequest.CONTROL_AWB_MODE_OFF
        )
        builder.addSessionParameter(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        )

        // Assert
        val sessionConfig = builder.build()
        assertThat(sessionConfig.sessionParameters[CaptureRequest.CONTROL_AWB_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AWB_MODE_OFF)
        assertThat(sessionConfig.sessionParameters[CaptureRequest.CONTROL_AF_MODE])
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
    }
}
