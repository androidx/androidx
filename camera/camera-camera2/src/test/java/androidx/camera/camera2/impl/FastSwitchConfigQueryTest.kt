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

package androidx.camera.camera2.impl

import android.graphics.ImageFormat
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE
import android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE
import android.util.Range
import android.util.Size
import androidx.camera.camera2.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.internal.StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION
import androidx.camera.camera2.interop.setCamera2CaptureRequestConfigurator
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.CameraPipeSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraInfoAdapterCreator.createCameraQuirks
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.core.CameraXConfig
import androidx.camera.core.CompositionSettings
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.impl.Config.OptionPriority
import androidx.camera.core.impl.HardwareConfigQueryResult
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.ResolvedStreamInfo
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = 35)
class FastSwitchConfigQueryTest {
    private val cameraMetadata = FakeCameraMetadata.fromTemplate(HighEndDeviceTemplate)
    private val cameraPipe =
        CameraPipeSimulator.create(TestScope(), ApplicationProvider.getApplicationContext())
    private val query =
        FastSwitchConfigQuery(
            cameraPipe,
            CameraConfig(cameraMetadata.camera),
            FakeCameraProperties(cameraMetadata),
            createCameraQuirks(cameraMetadata),
        )

    private val config =
        SessionConfig.Builder(
                Preview.Builder().build(),
                ImageCapture.Builder().build(),
                ImageAnalysis.Builder().build(),
            )
            .build()

    @Test
    fun isConfigSupported_queriesTheCameraWithAllStreams() {
        val streams =
            listOf(
                createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE),
                createStream(CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG, SMALL_SIZE),
                createStream(CaptureType.IMAGE_ANALYSIS, ImageFormat.YUV_420_888, SMALL_SIZE),
            )

        val result = query.isConfigSupported(config, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        assertThat(graphConfig.camera).isEqualTo(cameraMetadata.camera)
        assertThat(graphConfig.streams.map { it.outputs.single().size })
            .containsExactly(SMALL_SIZE, SMALL_SIZE, SMALL_SIZE)
        assertThat(graphConfig.streams.map { it.outputs.single().format })
            .containsExactly(StreamFormat.PRIVATE, StreamFormat.JPEG, StreamFormat.YUV_420_888)
    }

    @Test
    fun isConfigSupported_setsTheExpectedFrameRateRange() {
        val fps = Range(30, 30)
        val streams =
            listOf(
                createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE, fps),
                createStream(CaptureType.IMAGE_ANALYSIS, ImageFormat.YUV_420_888, SMALL_SIZE, fps),
            )

        query.isConfigSupported(config, streams)

        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        assertThat(graphConfig.sessionParameters[CONTROL_AE_TARGET_FPS_RANGE]).isEqualTo(fps)
    }

    @Test
    fun isConfigSupported_returnsUnsupported_whenTheCameraRejectsTheStreams() {
        val streams =
            listOf(
                createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, LARGE_SIZE),
                createStream(CaptureType.IMAGE_ANALYSIS, ImageFormat.YUV_420_888, LARGE_SIZE),
            )

        assertThat(query.isConfigSupported(config, streams))
            .isEqualTo(HardwareConfigQueryResult.UNSUPPORTED)
    }

    @Test
    fun isConfigSupported_returnsUnknown_whenThereAreNoStreams() {
        assertThat(query.isConfigSupported(config, emptyList()))
            .isEqualTo(HardwareConfigQueryResult.UNKNOWN)
        assertThat(cameraPipe.isConfigSupportedHistory).isEmpty()
    }

    @Test
    @Config(minSdk = 34, maxSdk = 34)
    fun isConfigSupported_returnsUnknown_belowApi35() {
        val streams = listOf(createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE))

        assertThat(query.isConfigSupported(config, streams))
            .isEqualTo(HardwareConfigQueryResult.UNKNOWN)
        assertThat(cameraPipe.isConfigSupportedHistory).isEmpty()
    }

    @Test
    fun isConfigSupported_withMeteringRepeatingAndPreview_configuresSurfaceTextureAndDefaultStreamUseCases() {
        val meteringRepeating =
            MeteringRepeating.Builder(
                    FakeCameraProperties(cameraMetadata),
                    DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext()),
                )
                .build()
        val previewOptions =
            MutableOptionsBundle.create().apply {
                insertOption(
                    STREAM_USE_CASE_STREAM_SPEC_OPTION,
                    SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                )
            }
        val stillOptions =
            MutableOptionsBundle.create().apply {
                insertOption(
                    STREAM_USE_CASE_STREAM_SPEC_OPTION,
                    SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong(),
                )
            }
        val streams =
            listOf(
                ResolvedStreamInfo(
                    createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE)
                        .useCaseConfig,
                    StreamSpec.builder(SMALL_SIZE)
                        .setImplementationOptions(Camera2ImplConfig(previewOptions))
                        .build(),
                ),
                ResolvedStreamInfo(
                    createStream(CaptureType.IMAGE_CAPTURE, ImageFormat.JPEG, SMALL_SIZE)
                        .useCaseConfig,
                    StreamSpec.builder(SMALL_SIZE)
                        .setImplementationOptions(Camera2ImplConfig(stillOptions))
                        .build(),
                ),
                ResolvedStreamInfo(
                    meteringRepeating.currentConfig,
                    StreamSpec.builder(SMALL_SIZE).build(),
                ),
            )

        val result = query.isConfigSupported(config, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        assertThat(graphConfig.defaultTemplate)
            .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_PREVIEW))
        val outputs = graphConfig.streams.map { it.outputs.single() }
        assertThat(outputs).hasSize(3)
        assertThat(outputs.last().format).isEqualTo(StreamFormat.PRIVATE)
        assertThat(outputs.last().size).isEqualTo(SMALL_SIZE)
        // Because MeteringRepeating is co-attached with Preview(PREVIEW), all streams fall back to
        // DEFAULT stream use case (or null) instead of stamping duplicate PREVIEW stream use cases.
        assertThat(outputs.map { it.streamUseCase })
            .containsNoneOf(
                OutputStream.StreamUseCase(SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()),
                OutputStream.StreamUseCase(
                    SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
                ),
            )
    }

    @Test
    fun isConfigSupported_withSecondaryInputFormat_includesSecondaryOutputStream() {
        val rawJpegConfig =
            FakeUseCaseConfig.Builder(
                    FakeUseCaseConfigFactory()
                        .getConfig(
                            CaptureType.IMAGE_CAPTURE,
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                        ),
                    CaptureType.IMAGE_CAPTURE,
                    ImageFormat.RAW_SENSOR,
                )
                .apply {
                    mutableConfig.insertOption(
                        ImageInputConfig.OPTION_SECONDARY_INPUT_FORMAT,
                        ImageFormat.JPEG,
                    )
                }
                .useCaseConfig
        val streams =
            listOf(
                createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE),
                ResolvedStreamInfo(rawJpegConfig, StreamSpec.builder(SMALL_SIZE).build()),
            )

        val result = query.isConfigSupported(config, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        val formats = graphConfig.streams.map { it.outputs.single().format }
        assertThat(formats)
            .containsExactly(StreamFormat.PRIVATE, StreamFormat.RAW_SENSOR, StreamFormat.JPEG)
    }

    @Test
    @Config(minSdk = 34, maxSdk = 34)
    fun isConfigSupported_returnsUnsupported_whenOptionsConflictEvenBelowApi35() {
        val option =
            androidx.camera.core.impl.Config.Option.create<Int>(
                "camera2.captureRequest.option.conflictKey",
                Int::class.javaObjectType,
                CONTROL_AE_TARGET_FPS_RANGE,
            )
        val stream1Config =
            FakeUseCaseConfig.Builder(
                    FakeUseCaseConfigFactory()
                        .getConfig(CaptureType.PREVIEW, ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY),
                    CaptureType.PREVIEW,
                    ImageFormat.PRIVATE,
                )
                .useCaseConfig
        val stream2Config =
            FakeUseCaseConfig.Builder(
                    FakeUseCaseConfigFactory()
                        .getConfig(
                            CaptureType.IMAGE_ANALYSIS,
                            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                        ),
                    CaptureType.IMAGE_ANALYSIS,
                    ImageFormat.YUV_420_888,
                )
                .useCaseConfig
        val options1 =
            MutableOptionsBundle.create().apply {
                insertOption(
                    option,
                    OptionPriority.REQUIRED,
                    1,
                )
            }
        val options2 =
            MutableOptionsBundle.create().apply {
                insertOption(
                    option,
                    OptionPriority.REQUIRED,
                    2,
                )
            }
        val streams =
            listOf(
                ResolvedStreamInfo(
                    stream1Config,
                    StreamSpec.builder(SMALL_SIZE).setImplementationOptions(options1).build(),
                ),
                ResolvedStreamInfo(
                    stream2Config,
                    StreamSpec.builder(SMALL_SIZE).setImplementationOptions(options2).build(),
                ),
            )

        assertThat(query.isConfigSupported(config, streams))
            .isEqualTo(HardwareConfigQueryResult.UNSUPPORTED)
    }

    @Test
    fun isConfigSupported_returnsUnsupported_whenSessionInteropSetsNonRegularSessionType() {
        val nonRegularConfig =
            SessionConfig.Builder(Preview.Builder().build())
                .setInterop { builder ->
                    builder.interopMutableConfig.insertOption(
                        Camera2ImplConfig.SESSION_TYPE_OPTION,
                        1,
                    )
                }
                .build()
        val streams = listOf(createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE))

        assertThat(query.isConfigSupported(nonRegularConfig, streams))
            .isEqualTo(HardwareConfigQueryResult.UNSUPPORTED)
        assertThat(cameraPipe.isConfigSupportedHistory).isEmpty()
    }

    @Test
    fun isConfigSupported_propagatesSurfaceConfigOptionsAndMirrorMode() {
        val useCaseConfigFactory = CameraUseCaseAdapter(ApplicationProvider.getApplicationContext())
        val previewUseCase =
            Preview.Builder()
                .setMirrorMode(MirrorMode.MIRROR_MODE_ON)
                .also { builder ->
                    builder.mutableConfig.insertOption(
                        Camera2ImplConfig.TIMESTAMP_BASE_OPTION,
                        android.hardware.camera2.params.OutputConfiguration
                            .TIMESTAMP_BASE_MONOTONIC,
                    )
                    builder.mutableConfig.insertOption(
                        Camera2ImplConfig.DYNAMIC_RANGE_PROFILE_OPTION,
                        android.hardware.camera2.params.DynamicRangeProfiles.HLG10,
                    )
                }
                .build()
        val fakeCameraInfo = FakeCameraInfoInternal()
        val mergedPreviewConfig =
            previewUseCase.mergeConfigs(
                fakeCameraInfo,
                null,
                previewUseCase.getDefaultConfig(true, useCaseConfigFactory),
            )
        val streams =
            listOf(
                ResolvedStreamInfo(
                    mergedPreviewConfig,
                    StreamSpec.builder(SMALL_SIZE).build(),
                    "physical-1",
                )
            )

        val result = query.isConfigSupported(config, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        val output = cameraPipe.isConfigSupportedHistory.single().streams.single().outputs.single()
        assertThat(output.camera?.value).isEqualTo("physical-1")
        assertThat(output.mirrorMode)
            .isEqualTo(
                OutputStream.MirrorMode(
                    android.hardware.camera2.params.OutputConfiguration.MIRROR_MODE_H
                )
            )
        assertThat(output.timestampBase)
            .isEqualTo(
                OutputStream.TimestampBase(
                    android.hardware.camera2.params.OutputConfiguration.TIMESTAMP_BASE_MONOTONIC
                )
            )
        assertThat(output.dynamicRangeProfile)
            .isEqualTo(
                OutputStream.DynamicRangeProfile(
                    android.hardware.camera2.params.DynamicRangeProfiles.HLG10
                )
            )
    }

    @Test
    fun isConfigSupported_withAeFpsRangeQuirk_appliesQuirkFpsRangeToNonMeteringStream() {
        val legacyMetadata =
            FakeCameraMetadata(
                characteristics =
                    mapOf(
                        android.hardware.camera2.CameraCharacteristics
                            .INFO_SUPPORTED_HARDWARE_LEVEL to
                            android.hardware.camera2.CameraCharacteristics
                                .INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                        android.hardware.camera2.CameraCharacteristics
                            .CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to
                            arrayOf(Range(15, 30), Range(30, 30)),
                    )
            )
        val legacyQuery =
            FastSwitchConfigQuery(
                cameraPipe,
                CameraConfig(legacyMetadata.camera),
                FakeCameraProperties(legacyMetadata),
                createCameraQuirks(legacyMetadata),
            )
        val streams = listOf(createStream(CaptureType.PREVIEW, ImageFormat.PRIVATE, SMALL_SIZE))

        legacyQuery.isConfigSupported(config, streams)

        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        assertThat(graphConfig.sessionParameters[CONTROL_AE_TARGET_FPS_RANGE])
            .isEqualTo(Range(15, 30))
    }

    @Test
    fun isConfigSupported_withStreamSharing_propagatesChildrenTemplateAndOptions() {
        val expectedFps = Range(24, 24)
        val useCaseConfigFactory = CameraUseCaseAdapter(ApplicationProvider.getApplicationContext())
        val childPreview =
            Preview.Builder()
                .setDefaultSessionConfig(
                    androidx.camera.core.impl.SessionConfig.Builder()
                        .apply { setTemplateType(CameraDevice.TEMPLATE_RECORD) }
                        .build()
                )
                .also { builder ->
                    builder.mutableConfig.insertOption(
                        CONTROL_AE_TARGET_FPS_RANGE.createCaptureRequestOption(),
                        expectedFps,
                    )
                }
                .build()
        val fakeCamera =
            FakeCamera(
                FakeCameraControl(),
                FakeCameraInfoInternal().apply {
                    setSupportedResolutions(ImageFormat.PRIVATE, listOf(SMALL_SIZE))
                },
            )
        val streamSharing =
            StreamSharing(
                fakeCamera,
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(
                    childPreview,
                    ImageCapture.Builder().build(),
                ),
                emptyList(),
                useCaseConfigFactory,
            )
        val mergedStreamSharingConfig =
            streamSharing.mergeConfigs(
                fakeCamera.cameraInfoInternal,
                null,
                streamSharing.getDefaultConfig(true, useCaseConfigFactory),
            )
        val streams =
            listOf(
                ResolvedStreamInfo(
                    mergedStreamSharingConfig,
                    StreamSpec.builder(SMALL_SIZE).build(),
                )
            )

        val result = query.isConfigSupported(config, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        assertThat(graphConfig.sessionTemplate)
            .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_RECORD))
        assertThat(graphConfig.sessionParameters[CONTROL_AE_TARGET_FPS_RANGE])
            .isEqualTo(expectedFps)
    }

    @Test
    fun isConfigSupported_withSessionInteropTemplateAndSessionParameters_propagatesToGraphConfig() {
        val useCaseConfigFactory = CameraUseCaseAdapter(ApplicationProvider.getApplicationContext())
        val childWithRecordTemplate =
            Preview.Builder()
                .setDefaultSessionConfig(
                    androidx.camera.core.impl.SessionConfig.Builder()
                        .apply { setTemplateType(CameraDevice.TEMPLATE_RECORD) }
                        .build()
                )
                .build()
        val fakeCamera =
            FakeCamera(
                FakeCameraControl(),
                FakeCameraInfoInternal().apply {
                    setSupportedResolutions(ImageFormat.PRIVATE, listOf(SMALL_SIZE))
                },
            )
        val streamSharing =
            StreamSharing(
                fakeCamera,
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                setOf(
                    childWithRecordTemplate,
                    ImageCapture.Builder().build(),
                ),
                emptyList(),
                useCaseConfigFactory,
            )
        val mergedStreamSharingConfig =
            streamSharing.mergeConfigs(
                fakeCamera.cameraInfoInternal,
                null,
                streamSharing.getDefaultConfig(true, useCaseConfigFactory),
            )
        val streams =
            listOf(
                ResolvedStreamInfo(
                    mergedStreamSharingConfig,
                    StreamSpec.builder(SMALL_SIZE).build(),
                )
            )
        val sessionInteropConfig =
            SessionConfig.Builder(childWithRecordTemplate)
                .setInterop { builder ->
                    builder.interopMutableConfig.insertOption(
                        Camera2ImplConfig.TEMPLATE_TYPE_OPTION,
                        CameraDevice.TEMPLATE_MANUAL,
                    )
                    builder.interopMutableConfig.insertOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE
                            .createSessionParameterOption(),
                        OptionPriority.ALWAYS_OVERRIDE,
                        android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE_MONO,
                    )
                }
                .build()

        val result = query.isConfigSupported(sessionInteropConfig, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        val graphConfig = cameraPipe.isConfigSupportedHistory.single()
        assertThat(graphConfig.sessionTemplate)
            .isEqualTo(RequestTemplate(CameraDevice.TEMPLATE_MANUAL))
        assertThat(
                graphConfig.sessionParameters[
                        android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE]
            )
            .isEqualTo(android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE_MONO)
    }

    @Test
    fun isConfigSupported_withCameraXConfigConfigurator_forwardsSessionParametersToConfigurator() {
        var capturedFps: Range<Int>? = null
        val cameraXConfig =
            CameraXConfig.Builder()
                .setCamera2CaptureRequestConfigurator { params ->
                    @Suppress("UNCHECKED_CAST")
                    capturedFps = params[CONTROL_AE_TARGET_FPS_RANGE] as? Range<Int>
                }
                .build()
        val queryWithCameraXConfig =
            FastSwitchConfigQuery(
                cameraPipe,
                CameraConfig(cameraMetadata.camera),
                FakeCameraProperties(cameraMetadata),
                createCameraQuirks(cameraMetadata),
                cameraXConfig,
            )
        val streams =
            listOf(
                createStream(
                    CaptureType.PREVIEW,
                    ImageFormat.PRIVATE,
                    SMALL_SIZE,
                    fps = Range(30, 30),
                )
            )

        val result = queryWithCameraXConfig.isConfigSupported(config, streams)

        assertThat(result).isEqualTo(HardwareConfigQueryResult.SUPPORTED)
        assertThat(capturedFps).isEqualTo(Range(30, 30))
    }

    private fun createStream(
        captureType: CaptureType,
        format: Int,
        size: Size,
        fps: Range<Int> = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED,
    ): ResolvedStreamInfo {
        val useCaseConfig: UseCaseConfig<*> =
            FakeUseCaseConfig.Builder(
                    FakeUseCaseConfigFactory()
                        .getConfig(captureType, ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY),
                    captureType,
                    format,
                )
                .useCaseConfig
        return ResolvedStreamInfo(
            useCaseConfig,
            StreamSpec.builder(size).setExpectedFrameRateRange(fps).build(),
        )
    }

    private companion object {
        // The fake camera backend supports sizes up to 1280x720.
        private val SMALL_SIZE = Size(640, 480)
        private val LARGE_SIZE = Size(1920, 1080)
    }
}
