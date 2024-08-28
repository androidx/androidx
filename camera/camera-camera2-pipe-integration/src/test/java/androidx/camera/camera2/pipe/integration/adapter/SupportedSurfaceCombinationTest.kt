/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.ImageFormat.JPEG_R
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES
import android.hardware.camera2.CameraCharacteristics.REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.MediaRecorder
import android.os.Build
import android.util.Pair
import android.util.Range
import android.util.Size
import android.view.WindowManager
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getConcurrentSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getFullSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLegacySupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getRAWSupportedCombinationList
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_10B_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_8B_SDR_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_8B_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_8B_UNCONSTRAINED_HLG10_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.DOLBY_VISION_CONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HDR10_HDR10_PLUS_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HDR10_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_CONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_SDR_CONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.HLG10_UNCONSTRAINED
import androidx.camera.camera2.pipe.integration.internal.LATENCY_NONE
import androidx.camera.camera2.pipe.integration.internal.StreamUseCaseUtil
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.UseCase
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1440P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.util.ReflectionHelpers

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedSurfaceCombinationTest {
    private val streamUseCaseOption: androidx.camera.core.impl.Config.Option<Long> =
        androidx.camera.core.impl.Config.Option.create(
            "camera2.cameraCaptureSession.streamUseCase",
            Long::class.javaPrimitiveType!!
        )
    private val sensorOrientation90 = 90
    private val landscapePixelArraySize = Size(4032, 3024)
    private val displaySize = Size(720, 1280)
    private val vgaSize = Size(640, 480)
    private val previewSize = Size(1280, 720)
    private val recordSize = Size(3840, 2160)
    private val maximumSize = Size(4032, 3024)
    private val legacyVideoMaximumVideoSize = Size(1920, 1080)
    private val mod16Size = Size(960, 544)
    private val profileUhd =
        EncoderProfilesUtil.createFakeEncoderProfilesProxy(recordSize.width, recordSize.height)
    private val profileFhd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(1920, 1080)
    private val profileHd =
        EncoderProfilesUtil.createFakeEncoderProfilesProxy(previewSize.width, previewSize.height)
    private val profileSd =
        EncoderProfilesUtil.createFakeEncoderProfilesProxy(vgaSize.width, vgaSize.height)
    private val supportedSizes =
        arrayOf(
            Size(4032, 3024), // 4:3
            Size(3840, 2160), // 16:9
            Size(1920, 1440), // 4:3
            Size(1920, 1080), // 16:9
            Size(1280, 960), // 4:3
            Size(1280, 720), // 16:9
            Size(1280, 720), // duplicate the size since Nexus 5X emulator has the
            Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
            Size(800, 450), // 16:9
            Size(640, 480), // 4:3
        )
    private val highResolutionMaximumSize = Size(6000, 4500)
    private val highResolutionSupportedSizes =
        arrayOf(
            Size(6000, 4500), // 4:3
            Size(6000, 3375), // 16:9
        )
    private val ultraHighMaximumSize = Size(8000, 6000)
    private val maximumResolutionSupportedSizes =
        arrayOf(
            Size(7200, 5400), // 4:3
            Size(7200, 4050), // 16:9
        )
    private val maximumResolutionHighResolutionSupportedSizes = arrayOf(Size(8000, 6000))

    private val streamUseCaseOverrideValue = 3L
    private val context = InstrumentationRegistry.getInstrumentation().context
    private var cameraFactory: FakeCameraFactory? = null
    private var useCaseConfigFactory: UseCaseConfigFactory = mock()
    private lateinit var fakeCameraMetadata: FakeCameraMetadata
    private lateinit var cameraCoordinator: CameraCoordinator

    private val mockCameraAppComponent: CameraAppComponent = mock()
    private val mockEncoderProfilesAdapter: EncoderProfilesProviderAdapter = mock()
    private val mockEncoderProfilesProxy: EncoderProfilesProxy = mock()
    private val mockVideoProfileProxy: VideoProfileProxy = mock()

    @Before
    fun setUp() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(displaySize.width)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(displaySize.height)
        whenever(mockEncoderProfilesAdapter.hasProfile(ArgumentMatchers.anyInt())).thenReturn(true)
        whenever(mockVideoProfileProxy.width).thenReturn(3840)
        whenever(mockVideoProfileProxy.height).thenReturn(2160)
        whenever(mockEncoderProfilesProxy.videoProfiles).thenReturn(listOf(mockVideoProfileProxy))
        whenever(mockEncoderProfilesAdapter.getAll(ArgumentMatchers.anyInt()))
            .thenReturn(mockEncoderProfilesProxy)
        cameraCoordinator = FakeCameraCoordinator()
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Surface combination support tests for guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun checkLegacySurfaceCombinationSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLegacySurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkRawSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getRAWSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationSupportedInLevel3Device() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkConcurrentSurfaceCombinationSupportedInConcurrentCameraMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val combinationList = getConcurrentSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.CONCURRENT_CAMERA,
                        DynamicRange.BIT_DEPTH_8_BIT
                    ),
                    combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun checkUltraHighResolutionSurfaceCombinationSupportedInUltraHighCameraMode() {
        setupCamera(
            maximumResolutionSupportedSizes = maximumResolutionSupportedSizes,
            maximumResolutionHighResolutionSupportedSizes =
                maximumResolutionHighResolutionSupportedSizes,
            capabilities =
                intArrayOf(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                )
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA,
                            DynamicRange.BIT_DEPTH_8_BIT
                        ),
                        it.surfaceConfigList
                    )
                )
                .isTrue()
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Surface config transformation tests
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun transformSurfaceConfigWithYUVAnalysisSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                vgaSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                previewSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                recordSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                maximumSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGAnalysisSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.JPEG,
                vgaSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.JPEG,
                previewSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.JPEG,
                recordSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.JPEG,
                maximumSize
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithPRIVS720PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.PRIVATE,
                SizeUtil.RESOLUTION_720P
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.s720p)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVS720PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.YUV_420_888,
                SizeUtil.RESOLUTION_720P
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.s720p)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGS720PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.JPEG,
                SizeUtil.RESOLUTION_720P
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.s720p)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithPRIVS1440PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.PRIVATE,
                RESOLUTION_1440P
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV, SurfaceConfig.ConfigSize.s1440p)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVS1440PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.YUV_420_888,
                RESOLUTION_1440P
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.s1440p)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGS1440PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.JPEG,
                RESOLUTION_1440P
            )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.s1440p)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    @Config(minSdk = 31)
    fun transformSurfaceConfigWithUltraHighResolution() {
        setupCamera(
            maximumResolutionSupportedSizes = maximumResolutionSupportedSizes,
            maximumResolutionHighResolutionSupportedSizes =
                maximumResolutionHighResolutionSupportedSizes,
            capabilities =
                intArrayOf(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                )
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        assertThat(
                supportedSurfaceCombination.transformSurfaceConfig(
                    CameraMode.DEFAULT,
                    ImageFormat.PRIVATE,
                    ultraHighMaximumSize
                )
            )
            .isEqualTo(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.ULTRA_MAXIMUM
                )
            )
        assertThat(
                supportedSurfaceCombination.transformSurfaceConfig(
                    CameraMode.DEFAULT,
                    ImageFormat.YUV_420_888,
                    ultraHighMaximumSize
                )
            )
            .isEqualTo(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.YUV,
                    SurfaceConfig.ConfigSize.ULTRA_MAXIMUM
                )
            )
        assertThat(
                supportedSurfaceCombination.transformSurfaceConfig(
                    CameraMode.DEFAULT,
                    ImageFormat.JPEG,
                    ultraHighMaximumSize
                )
            )
            .isEqualTo(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.JPEG,
                    SurfaceConfig.ConfigSize.ULTRA_MAXIMUM
                )
            )
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for LEGACY-level guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** PRIV/MAXIMUM */
    @Test
    fun canSelectCorrectSize_singlePrivStream_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(privUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSize_singleJpegStream_inLegacyDevice() {
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(jpegUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSize_singleYuvStream_inLegacyDevice() {
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(yuvUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** PRIV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusJpeg_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** YUV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusJpeg_inLegacyDevice() {
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** PRIV/PREVIEW + PRIV/PREVIEW */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inLegacyDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(privUseCase2, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** PRIV/PREVIEW + YUV/PREVIEW */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(yuvUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusJpeg_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(yuvUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** Unsupported PRIV + JPEG + PRIV for legacy level devices */
    @Test
    fun throwsException_unsupportedConfiguration_inLegacyDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val privUseCas2 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, RESOLUTION_VGA)
                put(jpegUseCase, RESOLUTION_VGA)
                put(privUseCas2, RESOLUTION_VGA)
            }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for LIMITED-level guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** PRIV/PREVIEW + PRIV/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inLimitedDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCas2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, recordSize)
                put(privUseCas2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /** PRIV/PREVIEW + YUV/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLimitedDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, recordSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /** YUV/PREVIEW + YUV/RECORD */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, recordSize)
                put(yuvUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /** PRIV/PREVIEW + PRIV/RECORD + JPEG/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpeg_inLimitedDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, recordSize)
                put(privUseCase2, previewSize)
                put(jpegUseCase, recordSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /** PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusJpeg_inLimitedDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, recordSize)
                put(jpegUseCase, recordSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /** YUV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusJpeg_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, previewSize)
                put(yuvUseCase2, previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /** Unsupported YUV + PRIV + YUV for limited level devices */
    @Test
    fun throwsException_unsupportedConfiguration_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, RESOLUTION_VGA)
                put(privUseCase, RESOLUTION_VGA)
                put(yuvUseCase2, RESOLUTION_VGA)
            }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
            )
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for FULL-level guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** PRIV/PREVIEW + PRIV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inFullDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, maximumSize)
                put(privUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /** PRIV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inFullDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /** YUV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inFullDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /** PRIV/PREVIEW + PRIV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpeg_inFullDevice() {
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(jpegUseCase, maximumSize)
                put(privUseCase1, previewSize)
                put(privUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /** YUV/VGA + PRIV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusPrivPlusYuv_inFullDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, RESOLUTION_VGA)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /** YUV/VGA + YUV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusYuv_inFullDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase3 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, previewSize)
                put(yuvUseCase3, RESOLUTION_VGA)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /** Unsupported PRIV + PRIV + YUV + RAW for full level devices */
    @Test
    fun throwsException_unsupportedConfiguration_inFullDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, RESOLUTION_VGA)
                put(privUseCase2, RESOLUTION_VGA)
                put(yuvUseCase, RESOLUTION_VGA)
                put(rawUseCase, RESOLUTION_VGA)
            }
        assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
            )
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for Level-3 guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** PRIV/PREVIEW + PRIV/VGA + YUV/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusYuvPlusRaw_inLevel3Device() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, previewSize)
                put(privUseCase2, RESOLUTION_VGA)
                put(yuvUseCase, maximumSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** PRIV/PREVIEW + PRIV/VGA + JPEG/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpegPlusRaw_inLevel3Device() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, previewSize)
                put(privUseCase2, RESOLUTION_VGA)
                put(jpegUseCase, maximumSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** Unsupported PRIV + YUV + YUV + RAW for level-3 devices */
    @Test
    fun throwsException_unsupportedConfiguration_inLevel3Device() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, RESOLUTION_VGA)
                put(yuvUseCase1, RESOLUTION_VGA)
                put(yuvUseCase2, RESOLUTION_VGA)
                put(rawUseCase, RESOLUTION_VGA)
            }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            )
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for Burst-capability guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** PRIV/PREVIEW + PRIV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inLimitedDevice_withBurstCapability() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, maximumSize)
                put(privUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities =
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
        )
    }

    /** PRIV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLimitedDevice_withBurstCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities =
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
        )
    }

    /** YUV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inLimitedDevice_withBurstCapability() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities =
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
        )
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for RAW-capability guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** RAW/MAX */
    @Test
    fun canSelectCorrectSizes_singleRawStream_inLimitedDevice_withRawCapability() {
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(rawUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** PRIV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** PRIV/PREVIEW + PRIV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, previewSize)
                put(privUseCase2, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** PRIV/PREVIEW + YUV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** YUV/PREVIEW + YUV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusRAW_inLimitedDevice_withRawCapability() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, previewSize)
                put(yuvUseCase2, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** PRIV/PREVIEW + JPEG/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusJpegPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(jpegUseCase, maximumSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /** YUV/PREVIEW + JPEG/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusJpegPlusRAW_inLimitedDevice_withRawCapability() {
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase, previewSize)
                put(jpegUseCase, maximumSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    @Test
    fun hasVideoCapture_suggestedStreamSpecZslDisabled() {
        val useCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // VIDEO
        val useCase2 = createUseCase(CaptureType.PREVIEW) // PREVIEW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(useCase1, recordSize)
                put(useCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            hasVideoCapture = true
        )
    }

    @Test
    fun hasNoVideoCapture_suggestedStreamSpecZslNotDisabled() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PREVIEW
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap, hasVideoCapture = false)
    }

    private fun getSuggestedSpecsAndVerify(
        useCasesExpectedResultMap: Map<UseCase, Size>,
        attachedSurfaceInfoList: List<AttachedSurfaceInfo> = emptyList(),
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        capabilities: IntArray? = null,
        compareWithAtMost: Boolean = false,
        compareExpectedFps: Range<Int>? = null,
        cameraMode: Int = CameraMode.DEFAULT,
        useCasesExpectedDynamicRangeMap: Map<UseCase, DynamicRange> = emptyMap(),
        supportedOutputFormats: IntArray? = null,
        dynamicRangeProfiles: DynamicRangeProfiles? = null,
        default10BitProfile: Long? = null,
        isPreviewStabilizationOn: Boolean = false,
        hasVideoCapture: Boolean = false
    ): Pair<Map<UseCaseConfig<*>, StreamSpec>, Map<AttachedSurfaceInfo, StreamSpec>> {
        setupCamera(
            hardwareLevel = hardwareLevel,
            capabilities = capabilities,
            dynamicRangeProfiles = dynamicRangeProfiles,
            default10BitProfile = default10BitProfile,
            supportedFormats = supportedOutputFormats
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)

        val useCaseConfigMap = getUseCaseToConfigMap(useCasesExpectedResultMap.keys.toList())
        val useCaseConfigToOutputSizesMap =
            getUseCaseConfigToOutputSizesMap(useCaseConfigMap.values.toList())
        val resultPair =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                cameraMode,
                attachedSurfaceInfoList,
                useCaseConfigToOutputSizesMap,
                isPreviewStabilizationOn,
                hasVideoCapture
            )
        val suggestedStreamSpecsForNewUseCases = resultPair.first
        val suggestedStreamSpecsForOldSurfaces = resultPair.second

        useCasesExpectedResultMap.keys.forEach {
            val resultSize = suggestedStreamSpecsForNewUseCases[useCaseConfigMap[it]]!!.resolution
            val expectedSize = useCasesExpectedResultMap[it]!!
            if (!compareWithAtMost) {
                assertThat(resultSize).isEqualTo(expectedSize)
            } else {
                assertThat(sizeIsAtMost(resultSize, expectedSize)).isTrue()
            }

            compareExpectedFps?.let { _ ->
                assertThat(
                        suggestedStreamSpecsForNewUseCases[useCaseConfigMap[it]]!!
                            .expectedFrameRateRange
                    )
                    .isEqualTo(compareExpectedFps)
            }
            val zslDisabled = suggestedStreamSpecsForNewUseCases[useCaseConfigMap[it]]!!.zslDisabled
            assertThat(zslDisabled == hasVideoCapture)
        }

        useCasesExpectedDynamicRangeMap.keys.forEach {
            val resultDynamicRange =
                suggestedStreamSpecsForNewUseCases[useCaseConfigMap[it]]!!.dynamicRange
            val expectedDynamicRange = useCasesExpectedDynamicRangeMap[it]

            assertThat(resultDynamicRange).isEqualTo(expectedDynamicRange)
        }

        // Assert that if one stream specification has stream use case options, all other
        // stream specifications also have it.
        var hasStreamUseCaseStreamSpecOption: Boolean? = null
        suggestedStreamSpecsForNewUseCases.entries.forEach {
            // Gets the first entry to determine whether StreamUseCaseStreamSpecOption
            // should exist or not.
            if (hasStreamUseCaseStreamSpecOption == null) {
                hasStreamUseCaseStreamSpecOption =
                    it.value.implementationOptions?.containsOption(
                        StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION
                    )
            }

            // All the other entries should align with the first entry
            assertThat(
                    it.value.implementationOptions?.containsOption(
                        StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION
                    )
                )
                .isEqualTo(hasStreamUseCaseStreamSpecOption)
        }
        suggestedStreamSpecsForOldSurfaces.entries.forEach {
            // All entries should align with the first entry
            assertThat(
                    it.value.implementationOptions?.containsOption(
                        StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION
                    )
                )
                .isEqualTo(hasStreamUseCaseStreamSpecOption)
        }
        return resultPair
    }

    private fun getUseCaseToConfigMap(useCases: List<UseCase>): Map<UseCase, UseCaseConfig<*>> {
        val useCaseConfigMap =
            mutableMapOf<UseCase, UseCaseConfig<*>>().apply {
                useCases.forEach { put(it, it.currentConfig) }
            }
        return useCaseConfigMap
    }

    private fun getUseCaseConfigToOutputSizesMap(
        useCaseConfigs: List<UseCaseConfig<*>>
    ): Map<UseCaseConfig<*>, List<Size>> {
        val resultMap =
            mutableMapOf<UseCaseConfig<*>, List<Size>>().apply {
                useCaseConfigs.forEach { put(it, supportedSizes.toList()) }
            }

        return resultMap
    }

    /** Helper function that returns whether size is <= maxSize */
    private fun sizeIsAtMost(size: Size, maxSize: Size): Boolean {
        return (size.height * size.width) <= (maxSize.height * maxSize.width)
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for Ultra HDR
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun checkUltraHdrCombinationsSupported() {
        setupCamera(
            supportedFormats = intArrayOf(JPEG_R),
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)

        GuaranteedConfigurationsUtil.getUltraHdrSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.DEFAULT,
                            requiredMaxBitDepth = BIT_DEPTH_10_BIT,
                            isUltraHdrOn = true
                        ),
                        it.surfaceConfigList
                    )
                )
                .isTrue()
        }
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun checkUltraHdrCombinationsSupported_when8bit() {
        // Device might support Ultra HDR but not 10-bit.
        setupCamera(supportedFormats = intArrayOf(JPEG_R))
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)

        GuaranteedConfigurationsUtil.getUltraHdrSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.DEFAULT,
                            requiredMaxBitDepth = DynamicRange.BIT_DEPTH_8_BIT,
                            isUltraHdrOn = true
                        ),
                        it.surfaceConfigList
                    )
                )
                .isTrue()
        }
    }

    /** JPEG_R/MAXIMUM when Ultra HDR is ON. */
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun canSelectCorrectSizes_onlyJpegr_whenUltraHdrIsOn() {
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE, imageFormat = JPEG_R) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(jpegUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(
            useCasesExpectedResultMap = useCaseExpectedResultMap,
            dynamicRangeProfiles = HLG10_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            supportedOutputFormats = intArrayOf(JPEG_R),
        )
    }

    /** PRIV/PREVIEW + JPEG_R/MAXIMUM when Ultra HDR is ON. */
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun canSelectCorrectSizes_privPlusJpegr_whenUltraHdrIsOn() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val jpegUseCase =
            createUseCase(
                CaptureType.IMAGE_CAPTURE,
                imageFormat = JPEG_R,
            ) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCasesExpectedResultMap = useCaseExpectedResultMap,
            supportedOutputFormats = intArrayOf(JPEG_R),
        )
    }

    /** HLG10 PRIV/PREVIEW + JPEG_R/MAXIMUM when Ultra HDR is ON. */
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun canSelectCorrectSizes_hlg10PrivPlusJpegr_whenUltraHdrIsOn() {
        val privUseCase = createUseCase(CaptureType.PREVIEW, dynamicRange = HLG_10_BIT) // PRIV
        val jpegUseCase =
            createUseCase(
                CaptureType.IMAGE_CAPTURE,
                imageFormat = JPEG_R,
            ) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCasesExpectedResultMap = useCaseExpectedResultMap,
            dynamicRangeProfiles = HLG10_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            supportedOutputFormats = intArrayOf(JPEG_R),
        )
    }

    /** Unsupported PRIV + PRIV + JPEG when Ultra HDR is ON. */
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun throwsException_unsupportedConfiguration_whenUltraHdrIsOn() {
        val privUseCase1 = createUseCase(CaptureType.PREVIEW) // PRIV
        val privUseCase2 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val jpegUseCase =
            createUseCase(
                CaptureType.IMAGE_CAPTURE,
                imageFormat = JPEG_R,
            ) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, previewSize)
                put(privUseCase2, RESOLUTION_VGA)
                put(jpegUseCase, maximumSize)
            }
        assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCasesExpectedResultMap = useCaseExpectedResultMap,
                dynamicRangeProfiles = HLG10_CONSTRAINED,
                capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
                supportedOutputFormats = intArrayOf(JPEG_R),
            )
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // StreamSpec selection tests for DynamicRange
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun check10BitDynamicRangeCombinationsSupported() {
        setupCamera(
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)

        GuaranteedConfigurationsUtil.get10BitSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.DEFAULT,
                            DynamicRange.BIT_DEPTH_10_BIT
                        ),
                        it.surfaceConfigList
                    )
                )
                .isTrue()
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun getSupportedStreamSpecThrows_whenUsingUnsupportedDynamicRange() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        val useCaseExpectedResultMap =
            mapOf(
                useCase to Size(0, 0) // Should throw before verifying size
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                capabilities =
                    intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun getSupportedStreamSpecThrows_whenUsingConcurrentCameraAndSupported10BitRange() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        val useCaseExpectedSizeMap =
            mapOf(
                useCase to Size(0, 0) // Should throw before verifying size
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                cameraMode = CameraMode.CONCURRENT_CAMERA,
                dynamicRangeProfiles = HLG10_CONSTRAINED
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun getSupportedStreamSpecThrows_whenUsingUltraHighResolutionAndSupported10BitRange() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        val useCaseExpectedSizeMap =
            mapOf(
                useCase to Size(0, 0) // Should throw before verifying size
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                cameraMode = CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA,
                dynamicRangeProfiles = HLG10_CONSTRAINED
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsHlg_dueToMandatory10Bit() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.HLG_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_CONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsHdr10_dueToRecommended10BitDynamicRange() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.HDR10_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HDR10_UNCONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            default10BitProfile = DynamicRangeProfiles.HDR10
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_dueToSupportedDynamicRanges() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_HDR_UNSPECIFIED,
                        DynamicRange.BIT_DEPTH_8_BIT
                    )
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_fromUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_DOLBY_VISION,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED
                    )
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision10_fromUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_DOLBY_VISION,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED
                    )
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_10B_UNCONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_fromUnspecifiedHdrWithUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_HDR_UNSPECIFIED,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED
                    )
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision10_fromUnspecifiedHdrWithUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_HDR_UNSPECIFIED,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED
                    )
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_CONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            default10BitProfile = DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_withUndefinedBitDepth_andFullyDefinedHlg10() {
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HLG_10_BIT
            )
        val previewUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_DOLBY_VISION,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED
                    )
            )
        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)
        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.HLG_10_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED_HLG10_UNCONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision10_dueToDynamicRangeConstraints() {
        // VideoCapture partially defined dynamic range
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        // Preview fully defined dynamic range
        val previewUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.DOLBY_VISION_8_BIT,
            )
        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)
        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.DOLBY_VISION_10_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_CONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecifiedDynamicRange_afterPartiallySpecifiedDynamicRange() {
        // VideoCapture partially defined dynamic range
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT
            )
        // Preview unspecified dynamic range
        val previewUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)
        val useCaseExpectedDynamicRangeMap =
            mapOf(
                previewUseCase to DynamicRange.HLG_10_BIT,
                videoUseCase to DynamicRange.HLG_10_BIT
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_UNCONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecifiedDynamicRangeToSdr() {
        // Preview unspecified dynamic range
        val useCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_CONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Test
    fun dynamicRangeResolver_resolvesToSdr_when10BitNotSupported() {
        // Preview unspecified dynamic range
        val useCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Test
    fun dynamicRangeResolver_resolvesToSdr8Bit_whenSdrWithUnspecifiedBitDepthProvided() {
        // Preview unspecified dynamic range
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(DynamicRange.ENCODING_SDR, DynamicRange.BIT_DEPTH_UNSPECIFIED)
            )

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecified8Bit_usingConstraintsFrom10BitDynamicRange() {
        // VideoCapture has 10-bit HDR range with constraint for 8-bit non-SDR range
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.DOLBY_VISION_10_BIT
            )
        // Preview unspecified encoding but 8-bit bit depth
        val previewUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(DynamicRange.ENCODING_UNSPECIFIED, DynamicRange.BIT_DEPTH_8_BIT)
            )

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.DOLBY_VISION_10_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            dynamicRangeProfiles = DOLBY_VISION_CONSTRAINED
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesToSdr_forUnspecified8Bit_whenNoOtherDynamicRangesPresent() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(DynamicRange.ENCODING_UNSPECIFIED, DynamicRange.BIT_DEPTH_8_BIT)
            )

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)

        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_SDR_UNCONSTRAINED
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecified8BitToDolbyVision8Bit_whenAlreadyPresent() {
        // VideoCapture fully resolved Dolby Vision 8-bit
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.DOLBY_VISION_8_BIT
            )
        // Preview unspecified encoding / 8-bit
        val previewUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.UNSPECIFIED
            )

        // Since there are no 10-bit dynamic ranges, the 10-bit resolution table isn't used.
        // Instead, this will use the camera default LIMITED table which is limited to preview
        // size for 2 PRIV use cases.
        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to previewSize, previewUseCase to previewSize)

        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.DOLBY_VISION_8_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_SDR_UNCONSTRAINED
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun tenBitTable_isUsed_whenAttaching10BitUseCaseToAlreadyAttachedSdrUseCases() {
        // JPEG use case can't be attached with an existing PRIV + YUV in the 10-bit tables
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                dynamicRange = DynamicRange.HLG_10_BIT
            )
        val useCaseExpectedSizeMap =
            mapOf(
                // Size would be valid for LIMITED table
                useCase to recordSize
            )
        // existing surfaces (Preview + ImageAnalysis)
        val attachedPreview =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.PRIVATE,
                previewSize,
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.PREVIEW),
                useCase.currentConfig,
                /*targetFrameRate=*/ null
            )
        val attachedAnalysis =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.RECORD),
                ImageFormat.YUV_420_888,
                recordSize,
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS),
                useCase.currentConfig,
                /*targetFrameRate=*/ null
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                attachedSurfaceInfoList = listOf(attachedPreview, attachedAnalysis),
                // LIMITED allows this combination, but 10-bit table does not
                hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                dynamicRangeProfiles = HLG10_SDR_CONSTRAINED,
                capabilities =
                    intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeConstraints_causeAutoResolutionToThrow() {
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                dynamicRange = DynamicRange.HLG_10_BIT
            )
        val useCaseExpectedSizeMap =
            mapOf(
                // Size would be valid for 10-bit table within constraints
                useCase to recordSize
            )
        // existing surfaces (PRIV + PRIV)
        val attachedPriv1 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.PRIVATE,
                previewSize,
                DynamicRange.HDR10_10_BIT,
                listOf(UseCaseConfigFactory.CaptureType.PREVIEW),
                useCase.currentConfig,
                /*targetFrameRate=*/ null
            )
        val attachedPriv2 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD
                ),
                ImageFormat.YUV_420_888,
                recordSize,
                DynamicRange.HDR10_PLUS_10_BIT,
                listOf(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE),
                useCase.currentConfig,
                /*targetFrameRate=*/ null
            )

        // These constraints say HDR10 and HDR10_PLUS can be combined, but not HLG
        val constraintsTable =
            DynamicRangeProfiles(
                longArrayOf(
                    DynamicRangeProfiles.HLG10,
                    DynamicRangeProfiles.HLG10,
                    LATENCY_NONE,
                    DynamicRangeProfiles.HDR10,
                    DynamicRangeProfiles.HDR10 or DynamicRangeProfiles.HDR10_PLUS,
                    LATENCY_NONE,
                    DynamicRangeProfiles.HDR10_PLUS,
                    DynamicRangeProfiles.HDR10_PLUS or DynamicRangeProfiles.HDR10,
                    LATENCY_NONE
                )
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                attachedSurfaceInfoList = listOf(attachedPriv1, attachedPriv2),
                dynamicRangeProfiles = constraintsTable,
                capabilities =
                    intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canAttachHlgDynamicRange_toExistingSdrStreams() {
        // JPEG use case can be attached with an existing PRIV + PRIV in the 10-bit tables
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                dynamicRange = DynamicRange.HLG_10_BIT
            )
        val useCaseExpectedSizeMap =
            mapOf(
                // Size is valid for 10-bit table within constraints
                useCase to recordSize
            )
        // existing surfaces (PRIV + PRIV)
        val attachedPriv1 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.PRIVATE,
                previewSize,
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.PREVIEW),
                useCase.currentConfig,
                /*targetFrameRate=*/ null
            )
        val attachedPriv2 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.PRIV,
                    SurfaceConfig.ConfigSize.RECORD
                ),
                ImageFormat.YUV_420_888,
                recordSize,
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS),
                useCase.currentConfig,
                /*targetFrameRate=*/ null
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            attachedSurfaceInfoList = listOf(attachedPriv1, attachedPriv2),
            dynamicRangeProfiles = HLG10_SDR_CONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT)
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun requiredSdrDynamicRangeThrows_whenCombinedWithConstrainedHlg() {
        // VideoCapture HLG dynamic range
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HLG_10_BIT
            )
        // Preview SDR dynamic range
        val previewUseCase =
            createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW, dynamicRange = DynamicRange.SDR)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        // Fails because HLG10 is constrained to only HLG10
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                dynamicRangeProfiles = HLG10_CONSTRAINED,
                capabilities =
                    intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun requiredSdrDynamicRange_canBeCombinedWithUnconstrainedHlg() {
        // VideoCapture HLG dynamic range
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HLG_10_BIT
            )
        // Preview SDR dynamic range
        val previewUseCase =
            createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW, dynamicRange = DynamicRange.SDR)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        // Should succeed due to HLG10 being unconstrained
        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_UNCONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun multiple10BitUnconstrainedDynamicRanges_canBeCombined() {
        // VideoCapture HDR10 dynamic range
        val videoUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HDR10_10_BIT
            )
        // Preview HDR10_PLUS dynamic range
        val previewUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                dynamicRange = DynamicRange.HDR10_PLUS_10_BIT
            )

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        // Succeeds because both HDR10 and HDR10_PLUS are unconstrained
        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HDR10_HDR10_PLUS_UNCONSTRAINED,
            capabilities =
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
        )
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for FPS settings
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun getSupportedOutputSizes_single_valid_targetFPS() {
        // a valid target means the device is capable of that fps
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(25, 30)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase, Size(3840, 2160)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    @Test
    fun getSuggestedStreamSpec_single_invalid_targetFPS() {
        // an invalid target means the device would neve be able to reach that fps
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(65, 70)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase, Size(800, 450)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_targetFPS_first_is_larger() {
        // a valid target means the device is capable of that fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 35)
            )
        val useCase2 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(15, 25)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1445
                put(useCase1, Size(1920, 1445))
                put(useCase2, Size(1920, 1445))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_targetFPS_first_is_smaller() {
        // a valid target means the device is capable of that fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 35)
            )
        val useCase2 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(45, 50)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1440
                put(useCase1, Size(1920, 1440))
                put(useCase2, Size(1920, 1440))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_targetFPS_intersect() {
        // first and second new use cases have target fps that intersect each other
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 40)
            )
        val useCase2 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(35, 45)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // effective target fps becomes 35-40
                // both selected size should be no larger than 1920 x 1080
                put(useCase1, Size(1920, 1080))
                put(useCase2, Size(1920, 1080))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_cases_first_has_targetFPS() {
        // first new use case has a target fps, second new use case does not
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 35)
            )
        val useCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1440
                put(useCase1, Size(1920, 1440))
                put(useCase2, Size(1920, 1440))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_cases_second_has_targetFPS() {
        // second new use case does not have a target fps, first new use case does not
        val useCase1 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)
        val useCase2 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 35)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1440
                put(useCase1, Size(1920, 1440))
                put(useCase2, Size(1920, 1440))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_attached_with_targetFPS_no_new_targetFPS() {
        // existing surface with target fps + new use case without a target fps
        val useCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // size should be no larger than 1280 x 960
                put(useCase, Size(1280, 960))
            }
        // existing surface w/ target fps
        val attachedSurfaceInfo =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.JPEG,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.JPEG,
                Size(1280, 720),
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.PREVIEW),
                useCase.currentConfig,
                Range(40, 50)
            )
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            attachedSurfaceInfoList = listOf(attachedSurfaceInfo),
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_attached_with_targetFPS_and_new_targetFPS_no_intersect() {
        // existing surface with target fps + new use case with target fps that does not intersect
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 35)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // size of new surface should be no larger than 1280 x 960
                put(useCase, Size(1280, 960))
            }
        // existing surface w/ target fps
        val attachedSurfaceInfo =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.JPEG,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.JPEG,
                Size(1280, 720),
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.PREVIEW),
                useCase.currentConfig,
                Range(40, 50)
            )
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            attachedSurfaceInfoList = listOf(attachedSurfaceInfo),
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_attached_with_targetFPS_and_new_targetFPS_with_intersect() {
        // existing surface with target fps + new use case with target fps that intersect each other
        val useCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(45, 50)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // size of new surface should be no larger than 1280 x 720
                put(useCase, Size(1280, 720))
            }
        // existing surface w/ target fps
        val attachedSurfaceInfo =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(
                    SurfaceConfig.ConfigType.JPEG,
                    SurfaceConfig.ConfigSize.PREVIEW
                ),
                ImageFormat.JPEG,
                Size(1280, 720),
                DynamicRange.SDR,
                listOf(UseCaseConfigFactory.CaptureType.PREVIEW),
                useCase.currentConfig,
                Range(40, 50)
            )
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            attachedSurfaceInfoList = listOf(attachedSurfaceInfo),
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true
        )
    }

    @Test
    fun getSuggestedStreamSpec_has_device_supported_expectedFrameRateRange() {
        // use case with target fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(15, 25)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(10, 22)
        )
        // expected fps 10,22 because it has the largest intersection
    }

    @Test
    fun getSuggestedStreamSpec_has_exact_device_supported_expectedFrameRateRange() {
        // use case with target fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 40)
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30)
        )
        // expected fps 30,30 because the fps ceiling is 30
    }

    @Test
    fun getSuggestedStreamSpec_has_no_device_supported_expectedFrameRateRange() {
        // use case with target fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(65, 65)
            )

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(800, 450)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(60, 60)
        )
        // expected fps 60,60 because it is the closest range available
    }

    @Test
    fun getSuggestedStreamSpec_has_multiple_device_supported_expectedFrameRateRange() {

        // use case with target fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(36, 45)
            )

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1280, 960)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 40)
        )
        // expected size will give a maximum of 40 fps
        // expected range 30,40. another range with the same intersection size was 30,50, but 30,40
        // was selected instead because its range has a larger ratio of intersecting value vs
        // non-intersecting
    }

    @Test
    fun getSuggestedStreamSpec_has_no_device_intersection_expectedFrameRateRange() {
        // target fps is between ranges, but within device capability (for some reason lol)

        // use case with target fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(26, 27)
            )

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30)
        )
        // 30,30 was expected because it is the closest and shortest range to our target fps
    }

    @Test
    fun getSuggestedStreamSpec_has_no_device_intersection_equidistant_expectedFrameRateRange() {

        // use case with target fps
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(26, 26)
            )

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30)
        )
        // 30,30 selected because although there are other ranges that  have the same distance to
        // the target, 30,30 is the shortest range that also happens to be on the upper side of the
        // target range
    }

    @Test
    fun getSuggestedStreamSpec_has_no_expectedFrameRateRange() {
        // a valid target means the device is capable of that fps

        // use case with no target fps
        val useCase1 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
        )
        // since no target fps present, no specific device fps will be selected, and is set to
        // unspecified: (0,0)
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Other tests
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun generateCorrectSurfaceDefinition() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera()
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.s720pSizeMap[imageFormat]).isEqualTo(RESOLUTION_720P)
        assertThat(surfaceSizeDefinition.previewSize).isEqualTo(previewSize)
        assertThat(surfaceSizeDefinition.s1440pSizeMap[imageFormat]).isEqualTo(RESOLUTION_1440P)
        assertThat(surfaceSizeDefinition.recordSize).isEqualTo(recordSize)
        assertThat(surfaceSizeDefinition.maximumSizeMap[imageFormat]).isEqualTo(maximumSize)
        assertThat(surfaceSizeDefinition.ultraMaximumSizeMap).isEmpty()
    }

    @Test
    fun correctS720pSize_withSmallerOutputSizes() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(supportedSizes = arrayOf(RESOLUTION_VGA))
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.s720pSizeMap[imageFormat]).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    fun correctS1440pSize_withSmallerOutputSizes() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(supportedSizes = arrayOf(RESOLUTION_VGA))
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.s1440pSizeMap[imageFormat]).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    @Config(minSdk = 23)
    fun correctMaximumSize_withHighResolutionOutputSizes() {
        setupCamera(highResolutionSupportedSizes = highResolutionSupportedSizes)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.maximumSizeMap[imageFormat])
            .isEqualTo(highResolutionMaximumSize)
    }

    @Test
    @Config(minSdk = 32)
    fun correctUltraMaximumSize_withMaximumResolutionMap() {
        setupCamera(
            maximumResolutionSupportedSizes = maximumResolutionSupportedSizes,
            maximumResolutionHighResolutionSupportedSizes =
                maximumResolutionHighResolutionSupportedSizes,
            capabilities =
                intArrayOf(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                )
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.ultraMaximumSizeMap[imageFormat])
            .isEqualTo(ultraHighMaximumSize)
    }

    @Test
    fun determineRecordSizeFromStreamConfigurationMap() {
        // Setup camera with non-integer camera Id
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            cameraId = CameraId("externalCameraId")
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)

        // Checks the determined RECORD size
        assertThat(supportedSurfaceCombination.surfaceSizeDefinition.recordSize)
            .isEqualTo(legacyVideoMaximumVideoSize)
    }

    @Test
    fun applyLegacyApi21QuirkCorrectly() {
        setupCamera()
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val sortedSizeList =
            listOf(
                // 16:9 sizes are put in the front of the list
                Size(3840, 2160), // 16:9
                Size(1920, 1080), // 16:9
                Size(1280, 720), // 16:9
                Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
                Size(800, 450), // 16:9

                // 4:3 sizes are put in the end of the list
                Size(4032, 3024), // 4:3
                Size(1920, 1440), // 4:3
                Size(1280, 960), // 4:3
                Size(640, 480), // 4:3
            )
        val resultList =
            supportedSurfaceCombination.applyResolutionSelectionOrderRelatedWorkarounds(
                sortedSizeList,
                ImageFormat.YUV_420_888
            )
        val expectedResultList =
            if (Build.VERSION.SDK_INT == 21) {
                listOf(
                    // 4:3 sizes are pulled to the front of the list
                    Size(4032, 3024), // 4:3
                    Size(1920, 1440), // 4:3
                    Size(1280, 960), // 4:3
                    Size(640, 480), // 4:3

                    // 16:9 sizes are put in the end of the list
                    Size(3840, 2160), // 16:9
                    Size(1920, 1080), // 16:9
                    Size(1280, 720), // 16:9
                    Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
                    Size(800, 450), // 16:9
                )
            } else {
                sortedSizeList
            }
        assertThat(resultList).containsExactlyElementsIn(expectedResultList).inOrder()
    }

    @Test
    fun applyResolutionCorrectorWorkaroundCorrectly() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J710MN")
        setupCamera(hardwareLevel = CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(context, fakeCameraMetadata, mockEncoderProfilesAdapter)
        val resultList =
            supportedSurfaceCombination.applyResolutionSelectionOrderRelatedWorkarounds(
                supportedSizes.toList(),
                ImageFormat.YUV_420_888
            )
        val expectedResultList =
            if (Build.VERSION.SDK_INT in 21..26) {
                listOf(
                    // 1280x720 is pulled to the first position for YUV format.
                    Size(1280, 720),

                    // The remaining sizes keep the original order
                    Size(4032, 3024),
                    Size(3840, 2160),
                    Size(1920, 1440),
                    Size(1920, 1080),
                    Size(1280, 960),
                    Size(960, 544),
                    Size(800, 450),
                    Size(640, 480),
                )
            } else {
                supportedSizes.toList()
            }
        assertThat(resultList).containsExactlyElementsIn(expectedResultList).inOrder()
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseStreamSpecOption_jpeg() {
        val jpegUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                streamUseCaseOverride = true
            ) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(jpegUseCase, landscapePixelArraySize) }
        val resultPair = getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
        assertThat(resultPair.first.size).isEqualTo(1)
        assertThat(
                resultPair.first[jpegUseCase.currentConfig]!!
                    .implementationOptions!!
                    .retrieveOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION)
            )
            .isEqualTo(streamUseCaseOverrideValue)
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun throwException_PopulateStreamUseCaseStreamSpecOption_notFullyOverride() {
        val jpegUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                streamUseCaseOverride = true
            ) // JPEG
        val yuvUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                streamUseCaseOverride = false
            ) // PREVIEW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(jpegUseCase, landscapePixelArraySize)
                put(yuvUseCase, previewSize)
            }
        assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun skipPopulateStreamUseCaseStreamSpecOption_unsupportedCombination() {
        val useCase1 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                streamUseCaseOverride = true
            ) // PREVIEW
        val useCase2 =
            createUseCase(
                UseCaseConfigFactory.CaptureType.PREVIEW,
                streamUseCaseOverride = true
            ) // PREVIEW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(useCase1, previewSize)
                put(useCase2, previewSize)
            }
        // PRIV + PRIV is supported by the Ultra-high table but not Stream use case
        val resultPair =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                cameraMode = CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA,
            )
        assertThat(resultPair.first.size).isEqualTo(2)
        assertThat(
                resultPair.first[useCase1.currentConfig]!!
                    .implementationOptions!!
                    .containsOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION)
            )
            .isFalse()
        assertThat(
                resultPair.first[useCase2.currentConfig]!!
                    .implementationOptions!!
                    .containsOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION)
            )
            .isFalse()
    }

    @Config(minSdk = 21, maxSdk = 32)
    @Test
    fun skipPopulateStreamUseCaseStreamSpecOption_unsupportedOs() {
        val jpegUseCase =
            createUseCase(
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE,
                streamUseCaseOverride = true
            ) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(jpegUseCase, landscapePixelArraySize) }
        val resultPair =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
            )
        assertThat(resultPair.first.size).isEqualTo(1)
        assertThat(
                resultPair.first[jpegUseCase.currentConfig]!!
                    .implementationOptions!!
                    .containsOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION)
            )
            .isFalse()
    }

    private fun setupCamera(
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        sensorOrientation: Int = sensorOrientation90,
        pixelArraySize: Size = landscapePixelArraySize,
        supportedSizes: Array<Size> = this.supportedSizes,
        supportedFormats: IntArray? = null,
        highResolutionSupportedSizes: Array<Size>? = null,
        maximumResolutionSupportedSizes: Array<Size>? = null,
        maximumResolutionHighResolutionSupportedSizes: Array<Size>? = null,
        dynamicRangeProfiles: DynamicRangeProfiles? = null,
        default10BitProfile: Long? = null,
        capabilities: IntArray? = null,
        cameraId: CameraId = CameraId.fromCamera1Id(0)
    ) {
        cameraFactory = FakeCameraFactory()
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.LENS_FACING,
            CameraCharacteristics.LENS_FACING_BACK
        )
        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            hardwareLevel
        )
        shadowCharacteristics.set(CameraCharacteristics.SENSOR_ORIENTATION, sensorOrientation)
        shadowCharacteristics.set(
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE,
            pixelArraySize
        )
        if (capabilities != null) {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                capabilities
            )
        }

        val mockMap: StreamConfigurationMap = mock()
        val mockMaximumResolutionMap: StreamConfigurationMap? =
            if (
                maximumResolutionSupportedSizes != null ||
                    maximumResolutionHighResolutionSupportedSizes != null
            ) {
                mock()
            } else {
                null
            }

        val deviceFPSRanges: Array<Range<Int>?> =
            arrayOf(
                Range(10, 22),
                Range(22, 22),
                Range(30, 30),
                Range(30, 50),
                Range(30, 40),
                Range(30, 60),
                Range(50, 60),
                Range(60, 60)
            )

        val characteristicsMap =
            mutableMapOf(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                    CameraCharacteristics.SENSOR_ORIENTATION to sensorOrientation,
                    CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to pixelArraySize,
                    CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to capabilities,
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap,
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to deviceFPSRanges
                )
                .also { characteristicsMap ->
                    mockMaximumResolutionMap?.let {
                        if (Build.VERSION.SDK_INT >= 31) {
                            characteristicsMap[
                                CameraCharacteristics
                                    .SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION] =
                                mockMaximumResolutionMap
                        }
                    }
                }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            dynamicRangeProfiles?.let {
                characteristicsMap[REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES] = it
            }
            default10BitProfile?.let {
                characteristicsMap[REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE] = it
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val uc =
                longArrayOf(
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW_VIDEO_STILL
                        .toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_STILL_CAPTURE.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_CALL.toLong(),
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                )
            characteristicsMap[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES] = uc
        }

        // set up FakeCafakeCameraMetadatameraMetadata
        fakeCameraMetadata =
            FakeCameraMetadata(cameraId = cameraId, characteristics = characteristicsMap)

        val cameraManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        (Shadow.extract<Any>(cameraManager) as ShadowCameraManager).addCamera(
            fakeCameraMetadata.camera.value,
            characteristics
        )

        whenever(mockMap.getOutputSizes(ArgumentMatchers.anyInt())).thenReturn(supportedSizes)
        // ImageFormat.PRIVATE was supported since API level 23. Before that, the supported
        // output sizes need to be retrieved via SurfaceTexture.class.
        whenever(mockMap.getOutputSizes(SurfaceTexture::class.java)).thenReturn(supportedSizes)
        // This is setup for the test to determine RECORD size from StreamConfigurationMap
        whenever(mockMap.getOutputSizes(MediaRecorder::class.java)).thenReturn(supportedSizes)
        // This is setup for the test to determine output formats from StreamConfigurationMap
        whenever(mockMap.outputFormats).thenReturn(supportedFormats)
        // This is setup for high resolution output sizes
        highResolutionSupportedSizes?.let {
            if (Build.VERSION.SDK_INT >= 23) {
                whenever(mockMap.getHighResolutionOutputSizes(ArgumentMatchers.anyInt()))
                    .thenReturn(it)
            }
        }

        // setup to return different minimum frame durations depending on resolution
        // minimum frame durations were designated only for the purpose of testing
        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(4032, 3024))
                )
            )
            .thenReturn(50000000L) // 20 fps, size maximum

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(3840, 2160))
                )
            )
            .thenReturn(40000000L) // 25, size record

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(1920, 1440))
                )
            )
            .thenReturn(33333333L) // 30

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(1920, 1080))
                )
            )
            .thenReturn(28571428L) // 35

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(1280, 960))
                )
            )
            .thenReturn(25000000L) // 40

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(1280, 720))
                )
            )
            .thenReturn(22222222L) // 45, size preview/display

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(960, 544))
                )
            )
            .thenReturn(20000000L) // 50

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(800, 450))
                )
            )
            .thenReturn(16666666L) // 60fps

        Mockito.`when`(
                mockMap.getOutputMinFrameDuration(
                    ArgumentMatchers.anyInt(),
                    ArgumentMatchers.eq(Size(640, 480))
                )
            )
            .thenReturn(16666666L) // 60fps

        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)
        mockMaximumResolutionMap?.let {
            whenever(mockMaximumResolutionMap.getOutputSizes(ArgumentMatchers.anyInt()))
                .thenReturn(maximumResolutionSupportedSizes)
            whenever(mockMaximumResolutionMap.getOutputSizes(SurfaceTexture::class.java))
                .thenReturn(maximumResolutionSupportedSizes)
            if (Build.VERSION.SDK_INT >= 23) {
                whenever(
                        mockMaximumResolutionMap.getHighResolutionOutputSizes(
                            ArgumentMatchers.anyInt()
                        )
                    )
                    .thenReturn(maximumResolutionHighResolutionSupportedSizes)
            }
            if (Build.VERSION.SDK_INT >= 31) {
                shadowCharacteristics.set(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION,
                    mockMaximumResolutionMap
                )
            }
        }
        @LensFacing
        val lensFacingEnum =
            CameraUtil.getLensFacingEnumFromInt(CameraCharacteristics.LENS_FACING_BACK)
        val cameraInfo = FakeCameraInfoInternal(fakeCameraMetadata.camera.value)
        cameraInfo.encoderProfilesProvider =
            FakeEncoderProfilesProvider.Builder()
                .add(QUALITY_2160P, profileUhd)
                .add(QUALITY_1080P, profileFhd)
                .add(QUALITY_720P, profileHd)
                .add(QUALITY_480P, profileSd)
                .build()
        cameraFactory!!.insertCamera(lensFacingEnum, fakeCameraMetadata.camera.value) {
            FakeCamera(fakeCameraMetadata.camera.value, null, cameraInfo)
        }

        initCameraX(fakeCameraMetadata.camera.value)
    }

    private fun initCameraX(cameraId: String) {
        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = FakeCameraBackend.FAKE_CAMERA_BACKEND_ID,
                concurrentCameraBackendIds =
                    setOf(
                        setOf(CameraBackendId("0"), CameraBackendId("1")),
                        setOf(CameraBackendId("0"), CameraBackendId("2"))
                    ),
                cameraMetadataMap =
                    mapOf(FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to listOf(fakeCameraMetadata))
            )
        whenever(mockCameraAppComponent.getCameraDevices()).thenReturn(fakeCameraDevices)
        cameraFactory!!.cameraManager = mockCameraAppComponent
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(CameraPipeConfig.defaultConfig())
                .setDeviceSurfaceManagerProvider { context: Context?, _: Any?, _: Set<String?>? ->
                    CameraSurfaceAdapter(context!!, mockCameraAppComponent, setOf(cameraId))
                }
                .setCameraFactoryProvider {
                    _: Context?,
                    _: CameraThreadConfig?,
                    _: CameraSelector?,
                    _: Long ->
                    cameraFactory!!
                }
                .build()
        val cameraX: CameraX =
            try {
                CameraXUtil.getOrCreateInstance(context) { cameraXConfig }.get()
            } catch (e: ExecutionException) {
                throw IllegalStateException("Unable to initialize CameraX for test.")
            } catch (e: InterruptedException) {
                throw IllegalStateException("Unable to initialize CameraX for test.")
            }
        useCaseConfigFactory = cameraX.defaultConfigFactory
    }

    private fun createUseCase(
        captureType: UseCaseConfigFactory.CaptureType,
        targetFrameRate: Range<Int>? = null,
        dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED
    ): UseCase {
        return createUseCase(captureType, targetFrameRate, dynamicRange, false)
    }

    private fun createUseCase(
        captureType: UseCaseConfigFactory.CaptureType,
        targetFrameRate: Range<Int>? = null,
        dynamicRange: DynamicRange? = DynamicRange.UNSPECIFIED,
        streamUseCaseOverride: Boolean = false,
        imageFormat: Int? = null
    ): UseCase {
        val builder =
            FakeUseCaseConfig.Builder(
                captureType,
                imageFormat
                    ?: when (captureType) {
                        UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE -> ImageFormat.JPEG
                        UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS -> ImageFormat.YUV_420_888
                        else -> ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    }
            )
        targetFrameRate?.let {
            builder.mutableConfig.insertOption(UseCaseConfig.OPTION_TARGET_FRAME_RATE, it)
        }
        builder.mutableConfig.insertOption(
            ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE,
            dynamicRange
        )
        if (streamUseCaseOverride) {
            builder.mutableConfig.insertOption(streamUseCaseOption, streamUseCaseOverrideValue)
        }
        return builder.build()
    }

    private fun createRawUseCase(): UseCase {
        val builder = FakeUseCaseConfig.Builder()
        builder.mutableConfig.insertOption(
            UseCaseConfig.OPTION_INPUT_FORMAT,
            ImageFormat.RAW_SENSOR
        )
        return builder.build()
    }
}
