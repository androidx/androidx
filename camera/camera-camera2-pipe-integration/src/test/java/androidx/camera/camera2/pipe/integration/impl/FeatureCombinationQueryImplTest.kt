/*
 * Copyright 2025 The Android Open Source Project
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

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.hardware.camera2.CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import android.util.Range
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator.createCameraQuirks
import androidx.camera.camera2.pipe.testing.CameraPipeSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery.Companion.createSessionConfigBuilder
import androidx.camera.core.featuregroup.impl.UseCaseType
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
// TODO: b/417839748 - Decide on the appropriate API level for CameraX feature combo API
@Config(minSdk = 35)
class FeatureCombinationQueryImplTest {
    private val cameraMetadata =
        FakeCameraMetadata(
            characteristics =
                buildMap {
                    if (Build.VERSION.SDK_INT >= 33) {
                        put(
                            CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES,
                            DynamicRangeProfiles(
                                longArrayOf(
                                    DynamicRangeProfiles.HLG10,
                                    DynamicRangeProfiles.HLG10,
                                    0L,
                                )
                            ),
                        )
                    }
                }
        )

    private val cameraPipe =
        CameraPipeSimulator.create(TestScope(), ApplicationProvider.getApplicationContext())

    private val featureCombinationQueryImpl =
        FeatureCombinationQueryImpl(cameraMetadata, cameraPipe, createCameraQuirks(cameraMetadata))

    private val useCaseTypes =
        listOf(UseCaseType.PREVIEW, UseCaseType.IMAGE_CAPTURE, UseCaseType.VIDEO_CAPTURE)

    private val createdSurfaces = mutableListOf<DeferrableSurface>()

    @After
    fun tearDown() {
        createdSurfaces.forEach { it.close() }
    }

    @Test
    fun isSupported_convertedGraphConfigMatchesSessionConfig_withDefaultOptions() {
        // Arrange: Create a merged session config from the default use cases for testing
        val sessionConfig = useCaseTypes.createSessionConfig()

        // Act
        featureCombinationQueryImpl.isSupported(sessionConfig)

        // Assert
        val isConfigSupportedHistory = cameraPipe.isConfigSupportedHistory
        assertThat(isConfigSupportedHistory.size).isEqualTo(1)
        isConfigSupportedHistory.first().verify()
    }

    @Test
    fun isSupported_convertedGraphConfigMatchesSessionConfig_withHighQualityFeatureOptions() {
        // Arrange: Create a merged session config from the default use cases for testing, try to
        // use as many feature options as possible based on API level
        val dynamicRange =
            if (Build.VERSION.SDK_INT >= 33) DynamicRange.HLG_10_BIT else DynamicRange.UNSPECIFIED

        val hasPreviewStabilization = Build.VERSION.SDK_INT >= 33

        val hasJpegR = Build.VERSION.SDK_INT >= 34

        val sessionConfig =
            useCaseTypes.createSessionConfig(
                dynamicRange = dynamicRange,
                fpsRange = FPS_60,
                hasPreviewStabilization = hasPreviewStabilization,
                hasJpegR = hasJpegR,
            )

        // Act
        featureCombinationQueryImpl.isSupported(sessionConfig)

        // Assert
        val isConfigSupportedHistory = cameraPipe.isConfigSupportedHistory
        assertThat(isConfigSupportedHistory.size).isEqualTo(1)
        isConfigSupportedHistory
            .first()
            .verify(
                dynamicRange = dynamicRange,
                fpsRange = FPS_60,
                hasPreviewStabilization = hasPreviewStabilization,
                hasJpegR = hasJpegR,
            )
    }

    private fun CameraGraph.Config.verify(
        dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED,
        fpsRange: Range<Int>? = null,
        hasPreviewStabilization: Boolean = false,
        hasJpegR: Boolean = false,
    ) {
        verifyStreams(dynamicRange, hasJpegR)

        verifyFpsRange(fpsRange)
        verifyPreviewStabilization(hasPreviewStabilization)
    }

    private fun CameraGraph.Config.verifyStreams(dynamicRange: DynamicRange, hasJpegR: Boolean) {
        assertThat(streams.map { it.outputs.first().size })
            .containsExactly(PREVIEW_RESOLUTION, IMAGE_CAPTURE_RESOLUTION, VIDEO_CAPTURE_RESOLUTION)

        assertThat(streams.map { it.outputs.first().format })
            .containsExactly(
                StreamFormat.PRIVATE,
                if (hasJpegR) StreamFormat.JPEG_R else StreamFormat.JPEG,
                StreamFormat.PRIVATE,
            )

        // TODO: Verify the output type, but CameraPipe doesn't seem to expose it (or
        //  LazyOutputConfig) right now

        verifyDynamicRange(dynamicRange)
    }

    private fun CameraGraph.Config.verifyDynamicRange(dynamicRange: DynamicRange) {
        assertThat(streams.map { it.outputs.first().dynamicRangeProfile })
            .containsExactlyElementsIn(
                List(3) {
                    if (dynamicRange == DynamicRange.HLG_10_BIT) {
                        OutputStream.DynamicRangeProfile.HLG10
                    } else {
                        if (Build.VERSION.SDK_INT >= 33) {
                            OutputStream.DynamicRangeProfile.STANDARD
                        } else {
                            null
                        }
                    }
                }
            )
    }

    private fun CameraGraph.Config.verifyFpsRange(fpsRange: Range<Int>?) {
        assertThat(sessionParameters[CONTROL_AE_TARGET_FPS_RANGE]).isEqualTo(fpsRange)
    }

    private fun CameraGraph.Config.verifyPreviewStabilization(hasPreviewStabilization: Boolean) {
        assertThat(sessionParameters[CONTROL_VIDEO_STABILIZATION_MODE])
            .isEqualTo(
                if (hasPreviewStabilization) {
                    CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                } else {
                    null
                }
            )
    }

    private fun List<UseCaseType>.createSessionConfig(
        dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED,
        fpsRange: Range<Int> = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED,
        hasPreviewStabilization: Boolean = false,
        hasJpegR: Boolean = false,
    ): SessionConfig {
        return map {
                it.createUseCaseConfig(hasJpegR)
                    .createSessionConfigBuilder(
                        when (it) {
                            UseCaseType.PREVIEW -> PREVIEW_RESOLUTION
                            UseCaseType.IMAGE_CAPTURE -> IMAGE_CAPTURE_RESOLUTION
                            UseCaseType.VIDEO_CAPTURE -> VIDEO_CAPTURE_RESOLUTION
                            else -> PREVIEW_RESOLUTION
                        },
                        dynamicRange,
                    )
                    .setExpectedFrameRateRange(fpsRange)
                    .setPreviewStabilization(
                        if (hasPreviewStabilization) StabilizationMode.ON
                        else StabilizationMode.UNSPECIFIED
                    )
                    .build()
                    .apply { createdSurfaces.addAll(this.surfaces) }
            }
            .merge()
    }

    private fun List<SessionConfig>.merge(): SessionConfig {
        val validatingBuilder = SessionConfig.ValidatingBuilder()
        forEach { validatingBuilder.add(it) }
        return validatingBuilder.build()
    }

    private fun UseCaseType.createUseCaseConfig(hasJpegR: Boolean): UseCaseConfig<*> {
        val captureType =
            when (this) {
                UseCaseType.PREVIEW -> CaptureType.PREVIEW
                UseCaseType.IMAGE_CAPTURE -> CaptureType.IMAGE_CAPTURE
                UseCaseType.VIDEO_CAPTURE -> CaptureType.VIDEO_CAPTURE
                UseCaseType.STREAM_SHARING -> CaptureType.STREAM_SHARING
                UseCaseType.UNDEFINED -> CaptureType.PREVIEW
            }

        val inputType =
            when (this) {
                UseCaseType.PREVIEW -> ImageFormat.PRIVATE
                UseCaseType.IMAGE_CAPTURE -> if (hasJpegR) ImageFormat.JPEG_R else ImageFormat.JPEG
                UseCaseType.VIDEO_CAPTURE -> ImageFormat.PRIVATE
                UseCaseType.STREAM_SHARING -> ImageFormat.PRIVATE
                UseCaseType.UNDEFINED -> ImageFormat.PRIVATE
            }

        val builder =
            FakeUseCaseConfig.Builder(
                FakeUseCaseConfigFactory()
                    .getConfig(captureType, ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY),
                captureType,
                inputType,
            )

        return builder.useCaseConfig
    }

    private companion object {
        private val PREVIEW_RESOLUTION = SizeUtil.RESOLUTION_1080P
        private val IMAGE_CAPTURE_RESOLUTION = SizeUtil.RESOLUTION_1440P_16_9
        private val VIDEO_CAPTURE_RESOLUTION = SizeUtil.RESOLUTION_UHD

        private val FPS_60 = Range(60, 60)
    }
}
