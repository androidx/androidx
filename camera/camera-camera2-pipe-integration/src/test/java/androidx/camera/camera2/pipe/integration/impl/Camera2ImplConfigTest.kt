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
package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Range
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.testing.fakes.FakeConfig
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val INVALID_TEMPLATE_TYPE = -1
private const val INVALID_COLOR_CORRECTION_MODE = -1

@ExperimentalCamera2Interop
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Camera2ImplConfigTest {
    @Test
    fun emptyConfigurationDoesNotContainTemplateType() {
        // Arrange
        val builder = FakeConfig.Builder()
        // Act
        val config = Camera2ImplConfig(builder.build())
        // Assert
        Truth.assertThat(config.getCaptureRequestTemplate(INVALID_TEMPLATE_TYPE))
            .isEqualTo(INVALID_TEMPLATE_TYPE)
    }

    @Test
    fun canSetAndRetrieveCaptureRequestKeys_byBuilder() {
        // Arrange
        val fakeRange = Range(0, 30)
        val builder = Camera2ImplConfig.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange
            ).setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CameraMetadata.COLOR_CORRECTION_MODE_FAST
            )
        // Act
        val config = Camera2ImplConfig(builder.build())
        // Assert
        Truth.assertThat(
            config.getCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                valueIfMissing = null
            )
        ).isEqualTo(fakeRange)

        Truth.assertThat(
            config.getCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                INVALID_COLOR_CORRECTION_MODE
            )
        ).isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST)
    }

    @Test
    fun canSetCaptureRequestOptionWithPriority() {
        // Arrange
        val builder = Camera2ImplConfig.Builder()
            .setCaptureRequestOptionWithPriority(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF,
                androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE
            )
        // Act
        val config = builder.build()
        // Assert
        config.findOptions(CAPTURE_REQUEST_ID_STEM) { option ->
            Truth.assertThat(option.token).isEqualTo(CaptureRequest.CONTROL_AF_MODE)
            Truth.assertThat(config.retrieveOption(option)).isEqualTo(
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            Truth.assertThat(config.getOptionPriority(option))
                .isEqualTo(androidx.camera.core.impl.Config.OptionPriority.ALWAYS_OVERRIDE)
            true
        }
    }

    @Test
    fun canInsertAllOptions_byBuilder() {
        // Arrange
        val fakeRange = Range(0, 30)
        val builder = Camera2ImplConfig.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fakeRange
            ).setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CameraMetadata.COLOR_CORRECTION_MODE_FAST
            )
        val config1 = Camera2ImplConfig(builder.build())
        val builder2 = Camera2ImplConfig.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON
            ).setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            ).insertAllOptions(config1)
        // Act
        val config2 = Camera2ImplConfig(builder2.build())
        // Assert
        Truth.assertThat(
            config2.getCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                valueIfMissing = null
            )
        ).isEqualTo(fakeRange)
        Truth.assertThat(
            config2.getCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                INVALID_COLOR_CORRECTION_MODE
            )
        ).isEqualTo(CameraMetadata.COLOR_CORRECTION_MODE_FAST)
        Truth.assertThat(
            config2.getCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE, valueIfMissing = 0
            )
        ).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
        Truth.assertThat(
            config2.getCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE, 0
            )
        ).isEqualTo(CaptureRequest.CONTROL_AWB_MODE_AUTO)
    }

    @Test
    fun captureRequestOptionPriorityIsOPTIONAL() {
        // Arrange
        val range = Range(0, 30)
        val builder = Camera2ImplConfig.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
        // Act
        val config: androidx.camera.core.impl.Config = builder.build()
        // Assert
        config.findOptions(
            CAPTURE_REQUEST_ID_STEM
        ) { option: androidx.camera.core.impl.Config.Option<*>? ->
            Truth.assertThat(
                config.getOptionPriority(option!!)
            ).isEqualTo(androidx.camera.core.impl.Config.OptionPriority.OPTIONAL)
            true
        }
    }

    // TODO: After porting CameraEventCallback (used for extension) to CameraUseCaseAdapter,
    //  also porting canExtendWithCameraEventCallback
}