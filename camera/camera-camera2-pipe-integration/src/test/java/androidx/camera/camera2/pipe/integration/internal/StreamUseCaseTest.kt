/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.internal

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.SupportedSurfaceCombination
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CompositionSettings
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.UseCase
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import junit.framework.TestCase
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@Config(minSdk = 33)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class StreamUseCaseTest() {
    private val streamUseCaseOption: androidx.camera.core.impl.Config.Option<Long> =
        androidx.camera.core.impl.Config.Option.create(
            "camera2.cameraCaptureSession.streamUseCase",
            Long::class.javaPrimitiveType!!
        )

    private var mMockSurface1: DeferrableSurface =
        object : DeferrableSurface() {
            private val mSurfaceFuture: ListenableFuture<Surface> = ResolvableFuture.create()

            override fun provideSurface(): ListenableFuture<Surface> {
                // Return a never complete future.
                return mSurfaceFuture
            }
        }
    private var mMockSurface2: DeferrableSurface =
        object : DeferrableSurface() {
            private val mSurfaceFuture: ListenableFuture<Surface> = ResolvableFuture.create()

            override fun provideSurface(): ListenableFuture<Surface> {
                // Return a never complete future.
                return mSurfaceFuture
            }
        }

    @After
    fun tearDown() {
        mMockSurface1.close()
        mMockSurface2.close()
    }

    @Test
    fun populateSurfaceToStreamUseCaseMapping_singlePreview() {
        val streamUseCaseMap: MutableMap<DeferrableSurface, Long> = mutableMapOf()
        val optionsBundle = MutableOptionsBundle.create()
        optionsBundle.insertOption(
            StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION,
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
        )
        val sessionConfig =
            SessionConfig.Builder()
                .addSurface(mMockSurface1)
                .addImplementationOptions(Camera2ImplConfig(optionsBundle))
                .build()
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        val sessionConfigs = mutableListOf(sessionConfig)
        val useCaseConfigs = mutableListOf(useCaseConfig)
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
            sessionConfigs,
            useCaseConfigs,
            streamUseCaseMap
        )
        TestCase.assertTrue(
            streamUseCaseMap[mMockSurface1] ==
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
        )
    }

    @Test
    fun populateSurfaceToStreamUseCaseMapping_imageCaptureAndMeteringRepeat() {
        val streamUseCaseMap: MutableMap<DeferrableSurface, Long> = mutableMapOf()
        val optionsBundle = MutableOptionsBundle.create()
        optionsBundle.insertOption(
            StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION,
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
        )
        val imageCaptureSessionConfig =
            SessionConfig.Builder()
                .addSurface(mMockSurface1)
                .addImplementationOptions(Camera2ImplConfig(optionsBundle))
                .build()
        val meteringRepeatingSessionConfig =
            SessionConfig.Builder().addSurface(mMockSurface2).build()
        val imageCaptureConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.IMAGE_CAPTURE,
                imageFormat = ImageFormat.YUV_420_888
            )
        val meteringRepeatingConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.METERING_REPEATING,
                imageFormat = ImageFormat.PRIVATE
            )
        val sessionConfigs =
            mutableListOf(imageCaptureSessionConfig, meteringRepeatingSessionConfig)
        val useCaseConfigs = mutableListOf(imageCaptureConfig, meteringRepeatingConfig)
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
            sessionConfigs,
            useCaseConfigs,
            streamUseCaseMap
        )
        TestCase.assertTrue(
            streamUseCaseMap[mMockSurface1] ==
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong()
        )
        TestCase.assertTrue(
            streamUseCaseMap[mMockSurface2] ==
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
        )
    }

    @Test
    fun populateSurfaceToStreamUseCaseMapping_previewAndNoSurfaceVideoCapture() {
        val streamUseCaseMap: MutableMap<DeferrableSurface, Long> = mutableMapOf()
        val previewOptionsBundle = MutableOptionsBundle.create()
        previewOptionsBundle.insertOption(
            StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION,
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
        )
        val previewSessionConfig =
            SessionConfig.Builder()
                .addSurface(mMockSurface1)
                .addImplementationOptions(Camera2ImplConfig(previewOptionsBundle))
                .build()
        val videoCaptureOptionsBundle = MutableOptionsBundle.create()
        videoCaptureOptionsBundle.insertOption(
            StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION,
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
        )
        // VideoCapture doesn't contain a surface
        val videoCaptureSessionConfig =
            SessionConfig.Builder()
                .addImplementationOptions(Camera2ImplConfig(videoCaptureOptionsBundle))
                .build()
        val previewConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        val videoCaptureConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.VIDEO_CAPTURE,
                imageFormat = ImageFormat.PRIVATE
            )
        val sessionConfigs = mutableListOf(previewSessionConfig, videoCaptureSessionConfig)
        val useCaseConfigs = mutableListOf(previewConfig, videoCaptureConfig)
        StreamUseCaseUtil.populateSurfaceToStreamUseCaseMapping(
            sessionConfigs,
            useCaseConfigs,
            streamUseCaseMap
        )
        assertThat(streamUseCaseMap.size).isEqualTo(1)
        assertThat(streamUseCaseMap[mMockSurface1])
            .isEqualTo(CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong())
    }

    @Test
    fun getStreamSpecImplementationOptions() {
        val result: Camera2ImplConfig =
            StreamUseCaseUtil.getStreamSpecImplementationOptions(
                getFakeUseCaseConfigWithOptions(
                    camera2InteropOverride = true,
                    isZslDisabled = false,
                    isZslCaptureMode = false,
                    captureType = CaptureType.PREVIEW,
                    imageFormat = ImageFormat.PRIVATE
                )
            )
        TestCase.assertTrue(
            result.retrieveOption(streamUseCaseOption) == TEST_STREAM_USE_CASE_OPTION_VALUE
        )
        result.retrieveOption(UseCaseConfig.OPTION_ZSL_DISABLED)?.let { TestCase.assertFalse(it) }
        TestCase.assertTrue(
            (result.retrieveOption(ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE) ==
                TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE)
        )
        TestCase.assertTrue(
            (result.retrieveOption(UseCaseConfig.OPTION_INPUT_FORMAT) == ImageFormat.PRIVATE)
        )
    }

    @Test
    fun isStreamUseCaseSupported_streamUseCaseNotAvailable() {
        TestCase.assertFalse(StreamUseCaseUtil.isStreamUseCaseSupported(getCameraMetadata(true)))
    }

    @Test
    fun shouldUseStreamUseCase_cameraModeNotSupported() {
        TestCase.assertFalse(
            StreamUseCaseUtil.shouldUseStreamUseCase(
                SupportedSurfaceCombination.FeatureSettings(
                    CameraMode.CONCURRENT_CAMERA,
                    DynamicRange.BIT_DEPTH_8_BIT
                )
            )
        )
    }

    @Test
    fun shouldUseStreamUseCase_bitDepthNotSupported() {
        TestCase.assertFalse(
            StreamUseCaseUtil.shouldUseStreamUseCase(
                SupportedSurfaceCombination.FeatureSettings(
                    CameraMode.DEFAULT,
                    DynamicRange.BIT_DEPTH_10_BIT
                )
            )
        )
    }

    @Test
    fun containsZslUseCase_isZslUseCase() {
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = true,
                captureType = CaptureType.IMAGE_CAPTURE,
                imageFormat = ImageFormat.JPEG
            )
        val useCaseConfigList = mutableListOf(useCaseConfig)
        TestCase.assertTrue(StreamUseCaseUtil.containsZslUseCase(listOf(), useCaseConfigList))
    }

    @Test
    fun containsZslUseCase_isZslUseCase_ZslDisabled() {
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = true,
                isZslCaptureMode = true,
                captureType = CaptureType.IMAGE_CAPTURE,
                imageFormat = ImageFormat.JPEG
            )
        val useCaseConfigList = mutableListOf(useCaseConfig)
        TestCase.assertFalse(StreamUseCaseUtil.containsZslUseCase(listOf(), useCaseConfigList))
    }

    @Test
    fun containsZslUseCase_isZslSurface() {
        val attachedSurfaces =
            mutableListOf(
                getFakeAttachedSurfaceInfo(
                    camera2InteropOverride = true,
                    isZslDisabled = false,
                    isZslCaptureMode = true,
                    captureType = CaptureType.IMAGE_CAPTURE,
                    imageFormat = ImageFormat.JPEG
                )
            )
        TestCase.assertTrue(StreamUseCaseUtil.containsZslUseCase(attachedSurfaces, listOf()))
    }

    @Test
    fun containsZslUseCase_isZslSurface_ZslDisabled() {
        val attachedSurfaces =
            mutableListOf(
                getFakeAttachedSurfaceInfo(
                    camera2InteropOverride = true,
                    isZslDisabled = true,
                    isZslCaptureMode = true,
                    captureType = CaptureType.IMAGE_CAPTURE,
                    imageFormat = ImageFormat.JPEG
                )
            )
        TestCase.assertFalse(StreamUseCaseUtil.containsZslUseCase(attachedSurfaces, listOf()))
    }

    @Test
    fun populateStreamUseCaseStreamSpecOption_camera2InteropOverride_singleNewUseCase() {
        val suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec> = mutableMapOf()
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        suggestedStreamSpecMap[useCaseConfig] =
            getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig)
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
            getCameraMetadata(false),
            ArrayList<AttachedSurfaceInfo>(),
            suggestedStreamSpecMap,
            mutableMapOf()
        )
        TestCase.assertTrue(
            suggestedStreamSpecMap[useCaseConfig]!!
                .implementationOptions!!
                .retrieveOption<Long>(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                TEST_STREAM_USE_CASE_OPTION_VALUE
        )
    }

    @Test
    fun populateStreamUseCaseStreamSpecOption_camera2InteropOverride_singleSurface() {
        val attachedSurfaces: MutableList<AttachedSurfaceInfo> =
            mutableListOf(
                getFakeAttachedSurfaceInfo(
                    camera2InteropOverride = true,
                    isZslDisabled = false,
                    isZslCaptureMode = false,
                    captureType = CaptureType.PREVIEW,
                    imageFormat = ImageFormat.PRIVATE
                )
            )
        val attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec> = HashMap()
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
            getCameraMetadata(false),
            attachedSurfaces,
            mutableMapOf(),
            attachedSurfaceStreamSpecMap
        )
        TestCase.assertTrue(
            attachedSurfaceStreamSpecMap[attachedSurfaces[0]]!!
                .implementationOptions!!
                .retrieveOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                TEST_STREAM_USE_CASE_OPTION_VALUE
        )
    }

    @Test
    fun populateStreamUseCaseStreamSpecOption_camera2InteropOverride_useCaseAndSurface() {
        val suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec> = mutableMapOf()
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = true,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        suggestedStreamSpecMap[useCaseConfig] =
            getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig)
        val attachedSurfaces: MutableList<AttachedSurfaceInfo> =
            mutableListOf(
                getFakeAttachedSurfaceInfo(
                    camera2InteropOverride = true,
                    isZslDisabled = false,
                    isZslCaptureMode = false,
                    captureType = CaptureType.PREVIEW,
                    imageFormat = ImageFormat.PRIVATE
                )
            )
        val attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec> =
            mutableMapOf()
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
            getCameraMetadata(false),
            attachedSurfaces,
            suggestedStreamSpecMap,
            attachedSurfaceStreamSpecMap
        )
        TestCase.assertTrue(
            suggestedStreamSpecMap[useCaseConfig]!!
                .implementationOptions!!
                .retrieveOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                TEST_STREAM_USE_CASE_OPTION_VALUE
        )
        TestCase.assertTrue(
            attachedSurfaceStreamSpecMap[attachedSurfaces[0]]!!
                .implementationOptions!!
                .retrieveOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                TEST_STREAM_USE_CASE_OPTION_VALUE
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun populateStreamUseCaseStreamSpecOption_camera2InteropOverride_missingOverride() {
        val suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec> = mutableMapOf()
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        suggestedStreamSpecMap[useCaseConfig] =
            getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig)
        val attachedSurfaces: MutableList<AttachedSurfaceInfo> =
            mutableListOf(
                getFakeAttachedSurfaceInfo(
                    camera2InteropOverride = true,
                    isZslDisabled = false,
                    isZslCaptureMode = false,
                    captureType = CaptureType.PREVIEW,
                    imageFormat = ImageFormat.PRIVATE
                )
            )
        val attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec> = HashMap()
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithInteropOverride(
            getCameraMetadata(false),
            attachedSurfaces,
            suggestedStreamSpecMap,
            attachedSurfaceStreamSpecMap
        )
    }

    @Test
    fun areStreamUseCasesAvailableForSurfaceConfigs_success() {
        val surfaceConfigList: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                )
            )
        TestCase.assertTrue(
            StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                getCameraMetadata(false),
                surfaceConfigList
            )
        )
    }

    @Test
    fun areStreamUseCasesAvailableForSurfaceConfigs_fail() {
        val surfaceConfigList: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                )
            )
        TestCase.assertFalse(
            StreamUseCaseUtil.areStreamUseCasesAvailableForSurfaceConfigs(
                getCameraMetadata(true),
                surfaceConfigList
            )
        )
    }

    @Test
    fun areCaptureTypesEligible_success() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                ),
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo> = HashMap()
        surfaceConfigAttachedSurfaceInfoMap[0] =
            getFakeAttachedSurfaceInfo(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        val surfaceConfigUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = HashMap()
        surfaceConfigUseCaseConfigMap[1] =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.VIDEO_CAPTURE,
                imageFormat = ImageFormat.PRIVATE
            )
        TestCase.assertTrue(
            StreamUseCaseUtil.areCaptureTypesEligible(
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap,
                surfaceConfigsWithStreamUseCase
            )
        )
    }

    @Test
    fun areCaptureTypesEligible_fail() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                ),
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo> = HashMap()
        surfaceConfigAttachedSurfaceInfoMap[0] =
            getFakeAttachedSurfaceInfo(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        val surfaceConfigUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = HashMap()
        surfaceConfigUseCaseConfigMap[1] =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        TestCase.assertFalse(
            StreamUseCaseUtil.areCaptureTypesEligible(
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap,
                surfaceConfigsWithStreamUseCase
            )
        )
    }

    @Test(expected = AssertionError::class)
    fun areCaptureTypesEligible_mappingError() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                ),
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: Map<Int, AttachedSurfaceInfo> = HashMap()
        val surfaceConfigUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = HashMap()
        surfaceConfigUseCaseConfigMap[1] =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.VIDEO_CAPTURE,
                imageFormat = ImageFormat.PRIVATE
            )
        StreamUseCaseUtil.areCaptureTypesEligible(
            surfaceConfigAttachedSurfaceInfoMap,
            surfaceConfigUseCaseConfigMap,
            surfaceConfigsWithStreamUseCase
        )
    }

    @Test
    fun areCaptureTypesEligible_streamSharing_previewVideoStill_success() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong()
                )
            )
        val useCaseConfigFactory: UseCaseConfigFactory = FakeUseCaseConfigFactory()
        val children: MutableSet<UseCase> =
            mutableSetOf(
                FakeUseCase(FakeUseCaseConfig.Builder().useCaseConfig, CaptureType.PREVIEW),
                FakeUseCase(FakeUseCaseConfig.Builder().useCaseConfig, CaptureType.IMAGE_CAPTURE),
                FakeUseCase(FakeUseCaseConfig.Builder().useCaseConfig, CaptureType.VIDEO_CAPTURE)
            )
        val streamSharing =
            StreamSharing(
                FakeCamera(),
                null,
                CompositionSettings.DEFAULT,
                CompositionSettings.DEFAULT,
                children,
                useCaseConfigFactory
            )
        val surfaceConfigAttachedSurfaceInfoMap: Map<Int, AttachedSurfaceInfo> = mutableMapOf()
        val surfaceConfigUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = mutableMapOf()
        surfaceConfigUseCaseConfigMap[0] =
            streamSharing.getDefaultConfig(true, useCaseConfigFactory)!!
        TestCase.assertTrue(
            StreamUseCaseUtil.areCaptureTypesEligible(
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap,
                surfaceConfigsWithStreamUseCase
            )
        )
    }

    @Test
    fun areCaptureTypesEligible_streamSharing_videoRecord_success() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo> =
            mutableMapOf()
        val surfaceConfigUseCaseConfigMap: Map<Int, UseCaseConfig<*>> = mutableMapOf()
        val captureTypes: MutableList<CaptureType> = ArrayList()
        captureTypes.add(CaptureType.PREVIEW)
        captureTypes.add(CaptureType.VIDEO_CAPTURE)
        surfaceConfigAttachedSurfaceInfoMap[0] =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.PRIVATE,
                SizeUtil.RESOLUTION_720P,
                DynamicRange.SDR,
                captureTypes,
                /*implementationOptions=*/ null,
                /*targetFrameRate=*/ null
            )
        TestCase.assertTrue(
            StreamUseCaseUtil.areCaptureTypesEligible(
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap,
                surfaceConfigsWithStreamUseCase
            )
        )
    }

    @Test
    fun areCaptureTypesEligible_streamSharing_fail() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo> =
            mutableMapOf()
        val surfaceConfigUseCaseConfigMap: Map<Int, UseCaseConfig<*>> = mutableMapOf()
        val captureTypes: MutableList<CaptureType> =
            mutableListOf(CaptureType.PREVIEW, CaptureType.IMAGE_CAPTURE, CaptureType.VIDEO_CAPTURE)
        surfaceConfigAttachedSurfaceInfoMap[0] =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.PRIVATE,
                SizeUtil.RESOLUTION_720P,
                DynamicRange.SDR,
                captureTypes,
                /*implementationOptions=*/ null,
                /*targetFrameRate=*/ null
            )
        TestCase.assertFalse(
            StreamUseCaseUtil.areCaptureTypesEligible(
                surfaceConfigAttachedSurfaceInfoMap,
                surfaceConfigUseCaseConfigMap,
                surfaceConfigsWithStreamUseCase
            )
        )
    }

    @Test
    fun populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs_success() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                ),
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: MutableMap<Int, AttachedSurfaceInfo> =
            mutableMapOf()
        val attachedSurfaceInfo =
            getFakeAttachedSurfaceInfo(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.PREVIEW,
                imageFormat = ImageFormat.PRIVATE
            )
        surfaceConfigAttachedSurfaceInfoMap[0] = attachedSurfaceInfo
        val surfaceConfigUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = mutableMapOf()
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.VIDEO_CAPTURE,
                imageFormat = ImageFormat.PRIVATE
            )
        surfaceConfigUseCaseConfigMap[1] = useCaseConfig
        val attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec> =
            mutableMapOf()
        val suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec> = mutableMapOf()
        suggestedStreamSpecMap[useCaseConfig] =
            getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig)
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
            suggestedStreamSpecMap,
            attachedSurfaceStreamSpecMap,
            surfaceConfigAttachedSurfaceInfoMap,
            surfaceConfigUseCaseConfigMap,
            surfaceConfigsWithStreamUseCase
        )
        TestCase.assertTrue(
            (attachedSurfaceStreamSpecMap[attachedSurfaceInfo]!!
                .implementationOptions!!
                .retrieveOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong())
        )
        TestCase.assertTrue(
            (suggestedStreamSpecMap[useCaseConfig]!!
                .implementationOptions!!
                .retrieveOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION) ==
                CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong())
        )
    }

    @Test(expected = AssertionError::class)
    fun populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs_mappingError() {
        val surfaceConfigsWithStreamUseCase: MutableList<SurfaceConfig> =
            mutableListOf(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                ),
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD,
                    CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            )
        val surfaceConfigAttachedSurfaceInfoMap: Map<Int, AttachedSurfaceInfo> = HashMap()
        val surfaceConfigUseCaseConfigMap: MutableMap<Int, UseCaseConfig<*>> = HashMap()
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride = false,
                isZslDisabled = false,
                isZslCaptureMode = false,
                captureType = CaptureType.VIDEO_CAPTURE,
                imageFormat = ImageFormat.PRIVATE
            )
        surfaceConfigUseCaseConfigMap[1] = useCaseConfig
        val attachedSurfaceStreamSpecMap: MutableMap<AttachedSurfaceInfo, StreamSpec> = HashMap()
        val suggestedStreamSpecMap: MutableMap<UseCaseConfig<*>, StreamSpec> = HashMap()
        suggestedStreamSpecMap[useCaseConfig] =
            getFakeStreamSpecFromFakeUseCaseConfig(useCaseConfig)
        StreamUseCaseUtil.populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs(
            suggestedStreamSpecMap,
            attachedSurfaceStreamSpecMap,
            surfaceConfigAttachedSurfaceInfoMap,
            surfaceConfigUseCaseConfigMap,
            surfaceConfigsWithStreamUseCase
        )
    }

    private fun getFakeUseCaseConfigWithOptions(
        camera2InteropOverride: Boolean,
        isZslDisabled: Boolean,
        isZslCaptureMode: Boolean,
        captureType: CaptureType,
        imageFormat: Int
    ): UseCaseConfig<*> {
        val fakeUseCaseConfigBuilder = FakeUseCaseConfig.Builder(captureType)
        val fakeConfig = fakeUseCaseConfigBuilder.mutableConfig
        if (camera2InteropOverride) {
            fakeConfig.insertOption(streamUseCaseOption, TEST_STREAM_USE_CASE_OPTION_VALUE)
        }
        fakeConfig.insertOption(UseCaseConfig.OPTION_ZSL_DISABLED, isZslDisabled)
        fakeConfig.insertOption(
            ImageCaptureConfig.OPTION_IMAGE_CAPTURE_MODE,
            if (isZslCaptureMode) ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
            else TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE
        )
        fakeConfig.insertOption(ImageCaptureConfig.OPTION_INPUT_FORMAT, imageFormat)
        return fakeUseCaseConfigBuilder.useCaseConfig
    }

    private fun getFakeAttachedSurfaceInfo(
        camera2InteropOverride: Boolean,
        isZslDisabled: Boolean,
        isZslCaptureMode: Boolean,
        captureType: CaptureType,
        imageFormat: Int
    ): AttachedSurfaceInfo {
        val useCaseConfig =
            getFakeUseCaseConfigWithOptions(
                camera2InteropOverride,
                isZslDisabled,
                isZslCaptureMode,
                captureType,
                imageFormat
            )
        val captureTypes: MutableList<CaptureType> = ArrayList()
        captureTypes.add(useCaseConfig.captureType)
        return AttachedSurfaceInfo.create(
            SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.PREVIEW),
            useCaseConfig.inputFormat,
            SizeUtil.RESOLUTION_720P,
            DynamicRange.SDR,
            captureTypes,
            StreamUseCaseUtil.getStreamSpecImplementationOptions(useCaseConfig),
            null
        /* targetFrameRate= */ )
    }

    private fun getFakeStreamSpecFromFakeUseCaseConfig(
        fakeUseCaseConfig: UseCaseConfig<*>
    ): StreamSpec {
        return StreamSpec.builder(SizeUtil.RESOLUTION_720P)
            .setDynamicRange(DynamicRange.UNSPECIFIED)
            .setImplementationOptions(
                StreamUseCaseUtil.getStreamSpecImplementationOptions(fakeUseCaseConfig)
            )
            .build()
    }

    private fun getCameraMetadata(
        noAvailableStreamUseCase: Boolean
    ): androidx.camera.camera2.pipe.CameraMetadata {
        val characteristicsMap: MutableMap<CameraCharacteristics.Key<*>, Any?> = mutableMapOf()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !noAvailableStreamUseCase) {
            val uc =
                longArrayOf(
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                    SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            characteristicsMap[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES] = uc
        }
        return FakeCameraMetadata(
            cameraId = CameraId.fromCamera2Id(CAMERA_ID_0),
            characteristics = characteristicsMap
        )
    }

    companion object {
        private const val CAMERA_ID_0 = "0"
        private const val TEST_STREAM_USE_CASE_OPTION_VALUE =
            CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()

        @CaptureMode
        private val TEST_OPTION_IMAGE_CAPTURE_MODE_VALUE =
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    }
}
