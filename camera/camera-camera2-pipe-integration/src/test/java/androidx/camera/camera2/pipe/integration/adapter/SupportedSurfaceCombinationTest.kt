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
import android.graphics.ImageFormat.JPEG
import android.graphics.ImageFormat.JPEG_R
import android.graphics.ImageFormat.PRIVATE
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES
import android.hardware.camera2.CameraCharacteristics.REQUEST_RECOMMENDED_TEN_BIT_DYNAMIC_RANGE_PROFILE
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT
import android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.MediaRecorder
import android.os.Build
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
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery
import androidx.camera.core.featuregroup.impl.FeatureCombinationQuery.Companion.NO_OP_FEATURE_COMBINATION_QUERY
import androidx.camera.core.featuregroup.impl.UseCaseType
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_HIGH_SPEED
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.StreamSpec.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.StreamUseCase
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.Companion.DEFAULT_STREAM_USE_CASE
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigSize.S1440P_16_9
import androidx.camera.core.impl.SurfaceConfig.ConfigSize.S1440P_4_3
import androidx.camera.core.impl.SurfaceConfig.ConfigSize.S720P_16_9
import androidx.camera.core.impl.SurfaceConfig.ConfigType
import androidx.camera.core.impl.SurfaceStreamSpecQueryResult
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.StreamSpecsCalculator
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1080P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1440P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1440P_16_9
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_UHD
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.impl.fakes.FakeFeatureCombinationQuery
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
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
            Long::class.javaPrimitiveType!!,
        )
    private val sensorOrientation90 = 90
    private val landscapePixelArraySize = Size(4032, 3024)
    private val displaySize = Size(720, 1280)
    private val vgaSize = Size(640, 480)
    private val previewSize = Size(1280, 720)
    private val recordSize = Size(3840, 2160)
    private val maximumSize = Size(4032, 3024)
    private val legacyVideoMaximumSize = Size(1920, 1080)
    private val profileUhd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(recordSize)
    private val profileFhd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(Size(1920, 1080))
    private val profileHd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(previewSize)
    private val profileSd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(vgaSize)
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
    private val commonHighSpeedSupportedSizeFpsMap =
        mapOf(
            RESOLUTION_1080P to
                listOf(
                    Range.create(30, 120),
                    Range.create(120, 120),
                    Range.create(30, 240),
                    Range.create(240, 240),
                ),
            RESOLUTION_720P to
                listOf(
                    Range.create(30, 120),
                    Range.create(120, 120),
                    Range.create(30, 240),
                    Range.create(240, 240),
                    Range.create(30, 480),
                    Range.create(480, 480),
                ),
        )

    private val defaultFpsRanges: Array<Range<Int>> =
        arrayOf(
            Range(10, 22),
            Range(22, 22),
            Range(30, 30),
            Range(30, 50),
            Range(30, 40),
            Range(30, 60),
            Range(50, 60),
            Range(60, 60),
        )

    private val NO_STREAM_USE_CASE: StreamUseCase? = null

    private val context = InstrumentationRegistry.getInstrumentation().context
    private var cameraFactory: FakeCameraFactory? = null
    private var useCaseConfigFactory: UseCaseConfigFactory = mock()
    private lateinit var fakeCameraMetadata: FakeCameraMetadata
    private lateinit var cameraCoordinator: CameraCoordinator

    private val mockCameraAppComponent: CameraAppComponent = mock()
    private val mockEncoderProfilesAdapter: EncoderProfilesProviderAdapter = mock()
    private val mockEncoderProfilesProxy: EncoderProfilesProxy = mock()
    private val mockVideoProfileProxy: VideoProfileProxy = mock()
    private val mockEmptyEncoderProfilesAdapter: EncoderProfilesProviderAdapter = mock()
    private val fakeFeatureCombinationQuery = FakeFeatureCombinationQuery()

    @Before
    fun setUp() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(displaySize.width)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(displaySize.height)
        whenever(mockEncoderProfilesAdapter.hasProfile(anyInt())).thenReturn(true)
        whenever(mockVideoProfileProxy.width).thenReturn(3840)
        whenever(mockVideoProfileProxy.height).thenReturn(2160)
        whenever(mockVideoProfileProxy.resolution).thenReturn(Size(3840, 2160))
        whenever(mockEncoderProfilesProxy.videoProfiles).thenReturn(listOf(mockVideoProfileProxy))
        whenever(mockEncoderProfilesAdapter.getAll(anyInt())).thenReturn(mockEncoderProfilesProxy)
        whenever(mockEmptyEncoderProfilesAdapter.hasProfile(anyInt())).thenReturn(false)
        whenever(mockEmptyEncoderProfilesAdapter.getAll(anyInt())).thenReturn(null)
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInLimitedDevice() {
        setupCamera(INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLegacySurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkRawSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getRAWSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationSupportedInLevel3Device() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.DEFAULT,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val combinationList = getConcurrentSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    SupportedSurfaceCombination.FeatureSettings(
                        CameraMode.CONCURRENT_CAMERA,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
                    combination.surfaceConfigList,
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
                ),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            ULTRA_HIGH_RESOLUTION_CAMERA,
                            DynamicRange.BIT_DEPTH_8_BIT,
                        ),
                        it.surfaceConfigList,
                    )
                )
                .isTrue()
        }
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun checkSurfaceCombinationSupportForHighSpeed() {
        setupCamera(
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO),
            supportedHighSpeedSizeAndFpsMap = commonHighSpeedSupportedSizeFpsMap,
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val featureSettings =
            SupportedSurfaceCombination.FeatureSettings(
                CameraMode.DEFAULT,
                DynamicRange.BIT_DEPTH_8_BIT,
                isHighSpeedOn = true,
            )

        // The expected SurfaceConfig is PRIV + RECORD because the max high speed size 1920x1080 is
        // between PREVIEW and RECORD size.
        val shouldSupportCombinations =
            listOf(
                SurfaceCombination().apply {
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD))
                },
                SurfaceCombination().apply {
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                },
            )
        shouldSupportCombinations.forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        featureSettings,
                        it.surfaceConfigList,
                    )
                )
                .isTrue()
        }

        val shouldNotSupportCombinations =
            listOf(
                SurfaceCombination().apply {
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.MAXIMUM))
                },
                SurfaceCombination().apply {
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM))
                },
                SurfaceCombination().apply {
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW))
                    addSurfaceConfig(SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW))
                },
            )
        shouldNotSupportCombinations.forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        featureSettings,
                        it.surfaceConfigList,
                    )
                )
                .isFalse()
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                vgaSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                previewSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                recordSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888,
                maximumSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGAnalysisSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                JPEG,
                vgaSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                JPEG,
                previewSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                JPEG,
                recordSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                JPEG,
                maximumSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithPRIVS720PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                PRIVATE,
                RESOLUTION_720P,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.PRIV, S720P_16_9)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVS720PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.YUV_420_888,
                RESOLUTION_720P,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, S720P_16_9)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGS720PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                JPEG,
                RESOLUTION_720P,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, S720P_16_9)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithPRIVS1440PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                PRIVATE,
                RESOLUTION_1440P,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.PRIV, S1440P_4_3)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVS1440PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                ImageFormat.YUV_420_888,
                RESOLUTION_1440P,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, S1440P_4_3)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGS1440PSizeInConcurrentMode() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.CONCURRENT_CAMERA,
                JPEG,
                RESOLUTION_1440P,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, S1440P_4_3)
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
                ),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        assertThat(
                supportedSurfaceCombination.transformSurfaceConfig(
                    CameraMode.DEFAULT,
                    PRIVATE,
                    ultraHighMaximumSize,
                    DEFAULT_STREAM_USE_CASE,
                )
            )
            .isEqualTo(SurfaceConfig.create(ConfigType.PRIV, ConfigSize.ULTRA_MAXIMUM))
        assertThat(
                supportedSurfaceCombination.transformSurfaceConfig(
                    CameraMode.DEFAULT,
                    ImageFormat.YUV_420_888,
                    ultraHighMaximumSize,
                    DEFAULT_STREAM_USE_CASE,
                )
            )
            .isEqualTo(SurfaceConfig.create(ConfigType.YUV, ConfigSize.ULTRA_MAXIMUM))
        assertThat(
                supportedSurfaceCombination.transformSurfaceConfig(
                    CameraMode.DEFAULT,
                    JPEG,
                    ultraHighMaximumSize,
                    DEFAULT_STREAM_USE_CASE,
                )
            )
            .isEqualTo(SurfaceConfig.create(ConfigType.JPEG, ConfigSize.ULTRA_MAXIMUM))
    }

    @Test
    fun transformSurfaceConfigWithUnsupportedFormatRecordSize() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            supportedFormats = intArrayOf(ImageFormat.YUV_420_888, JPEG, PRIVATE),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                JPEG_R,
                recordSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithUnsupportedFormatMaximumSize() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            supportedFormats = intArrayOf(ImageFormat.YUV_420_888, JPEG, PRIVATE),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val surfaceConfig =
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                JPEG_R,
                maximumSize,
                DEFAULT_STREAM_USE_CASE,
            )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for LEGACY-level guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /** PRIV/MAXIMUM */
    @Test
    fun canSelectCorrectSize_singlePrivStream_inLegacyDevice() {
        val privUseCase = createUseCase(CaptureType.VIDEO_CAPTURE)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(privUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSize_singleJpegStream_inLegacyDevice() {
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(jpegUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSize_singleYuvStream_inLegacyDevice() {
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(yuvUseCase, maximumSize) }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /** PRIV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusJpeg_inLegacyDevice() {
        val privUseCase = createUseCase(CaptureType.PREVIEW)
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE)
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
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
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
        val privUseCase1 = createUseCase(CaptureType.PREVIEW) // PRIV
        val privUseCase2 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
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
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
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
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
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
        val privUseCase1 = createUseCase(CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val privUseCas2 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
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
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCas2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, recordSize)
                put(privUseCas2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    /** PRIV/PREVIEW + YUV/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLimitedDevice() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, recordSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    /** YUV/PREVIEW + YUV/RECORD */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, recordSize)
                put(yuvUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    /** PRIV/PREVIEW + PRIV/RECORD + JPEG/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpeg_inLimitedDevice() {
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, recordSize)
                put(privUseCase2, previewSize)
                put(jpegUseCase, recordSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    /** PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusJpeg_inLimitedDevice() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, recordSize)
                put(jpegUseCase, recordSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    /** YUV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusJpeg_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, previewSize)
                put(yuvUseCase2, previewSize)
                put(jpegUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    /** Unsupported YUV + PRIV + YUV for limited level devices */
    @Test
    fun throwsException_unsupportedConfiguration_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, RESOLUTION_VGA)
                put(privUseCase, RESOLUTION_VGA)
                put(yuvUseCase2, RESOLUTION_VGA)
            }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
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
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, maximumSize)
                put(privUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
    }

    /** PRIV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inFullDevice() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
    }

    /** YUV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inFullDevice() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
    }

    /** PRIV/PREVIEW + PRIV/PREVIEW + JPEG/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpeg_inFullDevice() {
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(jpegUseCase, maximumSize)
                put(privUseCase1, previewSize)
                put(privUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
    }

    /** YUV/VGA + PRIV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusPrivPlusYuv_inFullDevice() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, RESOLUTION_VGA)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
    }

    /** YUV/VGA + YUV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusYuv_inFullDevice() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase3 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, previewSize)
                put(yuvUseCase3, RESOLUTION_VGA)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
    }

    /** Unsupported PRIV + PRIV + YUV + RAW for full level devices */
    @Test
    fun throwsException_unsupportedConfiguration_inFullDevice() {
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
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
                hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
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
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
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
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** PRIV/PREVIEW + PRIV/VGA + JPEG/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpegPlusRaw_inLevel3Device() {
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
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
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** Unsupported PRIV + YUV + YUV + RAW for level-3 devices */
    @Test
    fun throwsException_unsupportedConfiguration_inLevel3Device() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
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
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
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
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, maximumSize)
                put(privUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities =
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE),
        )
    }

    /** PRIV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLimitedDevice_withBurstCapability() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities =
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE),
        )
    }

    /** YUV/PREVIEW + YUV/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inLimitedDevice_withBurstCapability() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, maximumSize)
                put(yuvUseCase2, previewSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities =
                intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE),
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
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** PRIV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** PRIV/PREVIEW + PRIV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase1 = createUseCase(CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(CaptureType.PREVIEW) // PRIV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase1, previewSize)
                put(privUseCase2, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** PRIV/PREVIEW + YUV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(yuvUseCase, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** YUV/PREVIEW + YUV/PREVIEW + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusRAW_inLimitedDevice_withRawCapability() {
        val yuvUseCase1 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase1, previewSize)
                put(yuvUseCase2, previewSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** PRIV/PREVIEW + JPEG/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_privPlusJpegPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(privUseCase, previewSize)
                put(jpegUseCase, maximumSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
        )
    }

    /** YUV/PREVIEW + JPEG/MAXIMUM + RAW/MAXIMUM */
    @Test
    fun canSelectCorrectSizes_yuvPlusJpegPlusRAW_inLimitedDevice_withRawCapability() {
        val yuvUseCase = createUseCase(CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                put(yuvUseCase, previewSize)
                put(jpegUseCase, maximumSize)
                put(rawUseCase, maximumSize)
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW),
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
            hasVideoCapture = true,
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

    private fun getSuggestedSpecsAndVerifyForHighSpeed(
        useCasesExpectedSizeMap: Map<UseCase, Size>,
        useCasesOutputSizesMap: Map<UseCase, List<Size>>? = null,
        supportedHighSpeedSizeAndFpsMap: Map<Size, List<Range<Int>>>? =
            commonHighSpeedSupportedSizeFpsMap,
        compareExpectedFps: Range<Int>? = null,
    ) {
        getSuggestedSpecsAndVerify(
            useCasesExpectedSizeMap,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO),
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            supportedHighSpeedSizeAndFpsMap = supportedHighSpeedSizeAndFpsMap,
            compareExpectedFps = compareExpectedFps,
            expectedSessionType = SESSION_TYPE_HIGH_SPEED,
        )
    }

    private fun getSuggestedSpecsAndVerify(
        useCasesExpectedResultMap: Map<UseCase, Size>,
        useCasesOutputSizesMap: Map<UseCase, List<Size>>? = null,
        attachedSurfaceInfoList: List<AttachedSurfaceInfo> = emptyList(),
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        capabilities: IntArray? = null,
        compareWithAtMost: Boolean = false,
        compareExpectedFps: Range<Int>? = null,
        cameraMode: Int = CameraMode.DEFAULT,
        useCasesExpectedDynamicRangeMap: Map<UseCase, DynamicRange> = emptyMap(),
        supportedOutputFormats: IntArray? = null,
        supportedHighSpeedSizeAndFpsMap: Map<Size, List<Range<Int>>>? = null,
        dynamicRangeProfiles: DynamicRangeProfiles? = null,
        default10BitProfile: Long? = null,
        isPreviewStabilizationOn: Boolean = false,
        hasVideoCapture: Boolean = false,
        findMaxSupportedFrameRate: Boolean = false,
        expectedSessionType: Int = SESSION_TYPE_REGULAR,
        maxFpsBySizeMap: Map<Size, Int> = emptyMap(),
        isFeatureComboInvocation: Boolean = false,
        featureCombinationQuery: FeatureCombinationQuery = NO_OP_FEATURE_COMBINATION_QUERY,
        deviceFPSRanges: Array<Range<Int>> = defaultFpsRanges,
        expectedStreamUseCaseMap: Map<UseCase, StreamUseCase?>? = null,
    ): SurfaceStreamSpecQueryResult {
        setupCamera(
            hardwareLevel = hardwareLevel,
            capabilities = capabilities,
            dynamicRangeProfiles = dynamicRangeProfiles,
            default10BitProfile = default10BitProfile,
            supportedFormats = supportedOutputFormats,
            supportedHighSpeedSizeAndFpsMap = supportedHighSpeedSizeAndFpsMap,
            maxFpsBySizeMap = maxFpsBySizeMap,
            deviceFPSRanges = deviceFPSRanges,
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                featureCombinationQuery,
            )

        val useCaseConfigMap = getUseCaseToConfigMap(useCasesExpectedResultMap.keys.toList())
        val useCaseConfigToOutputSizesMap =
            useCaseConfigMap.entries.associate { (useCase, config) ->
                config to (useCasesOutputSizesMap?.get(useCase) ?: supportedSizes.toList())
            }
        val result =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                cameraMode,
                attachedSurfaceInfoList,
                useCaseConfigToOutputSizesMap,
                isPreviewStabilizationOn,
                hasVideoCapture,
                isFeatureComboInvocation,
                findMaxSupportedFrameRate,
            )
        val suggestedStreamSpecsForNewUseCases = result.useCaseStreamSpecs
        val suggestedStreamSpecsForOldSurfaces = result.attachedSurfaceStreamSpecs

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
            assertThat(zslDisabled).isEqualTo(hasVideoCapture)

            val sessionType = suggestedStreamSpecsForNewUseCases[useCaseConfigMap[it]]!!.sessionType
            assertThat(sessionType).isEqualTo(expectedSessionType)
        }

        useCasesExpectedDynamicRangeMap.keys.forEach {
            val resultDynamicRange =
                suggestedStreamSpecsForNewUseCases[useCaseConfigMap[it]]!!.dynamicRange
            val expectedDynamicRange = useCasesExpectedDynamicRangeMap[it]

            assertThat(resultDynamicRange).isEqualTo(expectedDynamicRange)
        }

        // Assert that if one stream specification has stream use case options, all other
        // stream specifications also have it.
        val allStreamSpecs =
            suggestedStreamSpecsForNewUseCases.values + suggestedStreamSpecsForOldSurfaces.values
        val hasAnyStreamUseCase = allStreamSpecs.any { it.hasStreamUseCase() }
        assertThat(allStreamSpecs.all { it.hasStreamUseCase() == hasAnyStreamUseCase }).isTrue()

        expectedStreamUseCaseMap?.let { map ->
            for ((useCase, streamUseCase) in map) {
                assertThat(
                        suggestedStreamSpecsForNewUseCases[useCase.currentConfig]!!
                            .getStreamUseCase()
                    )
                    .isEqualTo(streamUseCase?.value)
            }
        }
        return result
    }

    private fun getUseCaseToConfigMap(useCases: List<UseCase>): Map<UseCase, UseCaseConfig<*>> {
        val useCaseConfigMap =
            mutableMapOf<UseCase, UseCaseConfig<*>>().apply {
                useCases.forEach { put(it, it.currentConfig) }
            }
        return useCaseConfigMap
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
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )

        GuaranteedConfigurationsUtil.getUltraHdrSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.DEFAULT,
                            requiredMaxBitDepth = BIT_DEPTH_10_BIT,
                            isUltraHdrOn = true,
                        ),
                        it.surfaceConfigList,
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )

        GuaranteedConfigurationsUtil.getUltraHdrSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.DEFAULT,
                            requiredMaxBitDepth = DynamicRange.BIT_DEPTH_8_BIT,
                            isUltraHdrOn = true,
                        ),
                        it.surfaceConfigList,
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
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE, imageFormat = JPEG_R) // JPEG
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
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE, imageFormat = JPEG_R) // JPEG
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
        val jpegUseCase = createUseCase(CaptureType.IMAGE_CAPTURE, imageFormat = JPEG_R) // JPEG
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
        setupCamera(capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT))
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )

        GuaranteedConfigurationsUtil.get10BitSupportedCombinationList().forEach {
            assertThat(
                    supportedSurfaceCombination.checkSupported(
                        SupportedSurfaceCombination.FeatureSettings(
                            CameraMode.DEFAULT,
                            BIT_DEPTH_10_BIT,
                        ),
                        it.surfaceConfigList,
                    )
                )
                .isTrue()
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun getSupportedStreamSpecThrows_whenUsingUnsupportedDynamicRange() {
        val useCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT)
        val useCaseExpectedResultMap =
            mapOf(
                useCase to Size(0, 0) // Should throw before verifying size
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun getSupportedStreamSpecThrows_whenUsingConcurrentCameraAndSupported10BitRange() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        val useCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT)
        val useCaseExpectedSizeMap =
            mapOf(
                useCase to Size(0, 0) // Should throw before verifying size
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                cameraMode = CameraMode.CONCURRENT_CAMERA,
                dynamicRangeProfiles = HLG10_CONSTRAINED,
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun getSupportedStreamSpecThrows_whenUsingUltraHighResolutionAndSupported10BitRange() {
        Shadows.shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT, true)
        val useCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT)
        val useCaseExpectedSizeMap =
            mapOf(
                useCase to Size(0, 0) // Should throw before verifying size
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                cameraMode = ULTRA_HIGH_RESOLUTION_CAMERA,
                dynamicRangeProfiles = HLG10_CONSTRAINED,
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsHlg_dueToMandatory10Bit() {
        val useCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT)
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to HLG_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsHdr10_dueToRecommended10BitDynamicRange() {
        val useCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT)
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.HDR10_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HDR10_UNCONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            default10BitProfile = DynamicRangeProfiles.HDR10,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_dueToSupportedDynamicRanges() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_HDR_UNSPECIFIED,
                        DynamicRange.BIT_DEPTH_8_BIT,
                    ),
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_fromUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_DOLBY_VISION,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED,
                    ),
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision10_fromUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_DOLBY_VISION,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED,
                    ),
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_10B_UNCONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_fromUnspecifiedHdrWithUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_HDR_UNSPECIFIED,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED,
                    ),
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision10_fromUnspecifiedHdrWithUnspecifiedBitDepth() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_HDR_UNSPECIFIED,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED,
                    ),
            )
        val useCaseExpectedSizeMap = mapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to DynamicRange.DOLBY_VISION_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            default10BitProfile = DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision8_withUndefinedBitDepth_andFullyDefinedHlg10() {
        val videoUseCase = createUseCase(CaptureType.VIDEO_CAPTURE, dynamicRange = HLG_10_BIT)
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(
                        DynamicRange.ENCODING_DOLBY_VISION,
                        DynamicRange.BIT_DEPTH_UNSPECIFIED,
                    ),
            )
        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)
        val useCaseExpectedDynamicRangeMap =
            mapOf(videoUseCase to HLG_10_BIT, previewUseCase to DynamicRange.DOLBY_VISION_8_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_UNCONSTRAINED_HLG10_UNCONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_returnsDolbyVision10_dueToDynamicRangeConstraints() {
        // VideoCapture partially defined dynamic range
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT,
            )
        // Preview fully defined dynamic range
        val previewUseCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.DOLBY_VISION_8_BIT)
        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)
        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.DOLBY_VISION_10_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT,
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = DOLBY_VISION_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecifiedDynamicRange_afterPartiallySpecifiedDynamicRange() {
        // VideoCapture partially defined dynamic range
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.HDR_UNSPECIFIED_10_BIT,
            )
        // Preview unspecified dynamic range
        val previewUseCase = createUseCase(CaptureType.PREVIEW)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)
        val useCaseExpectedDynamicRangeMap =
            mapOf(previewUseCase to HLG_10_BIT, videoUseCase to HLG_10_BIT)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_UNCONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecifiedDynamicRangeToSdr() {
        // Preview unspecified dynamic range
        val useCase = createUseCase(CaptureType.PREVIEW)

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Test
    fun dynamicRangeResolver_resolvesToSdr_when10BitNotSupported() {
        // Preview unspecified dynamic range
        val useCase = createUseCase(CaptureType.PREVIEW)

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Test
    fun dynamicRangeResolver_resolvesToSdr8Bit_whenSdrWithUnspecifiedBitDepthProvided() {
        // Preview unspecified dynamic range
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(DynamicRange.ENCODING_SDR, DynamicRange.BIT_DEPTH_UNSPECIFIED),
            )

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)
        val useCaseExpectedDynamicRangeMap = mapOf(useCase to SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecified8Bit_usingConstraintsFrom10BitDynamicRange() {
        // VideoCapture has 10-bit HDR range with constraint for 8-bit non-SDR range
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                dynamicRange = DynamicRange.DOLBY_VISION_10_BIT,
            )
        // Preview unspecified encoding but 8-bit bit depth
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(DynamicRange.ENCODING_UNSPECIFIED, DynamicRange.BIT_DEPTH_8_BIT),
            )

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.DOLBY_VISION_10_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT,
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            dynamicRangeProfiles = DOLBY_VISION_CONSTRAINED,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesToSdr_forUnspecified8Bit_whenNoOtherDynamicRangesPresent() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    DynamicRange(DynamicRange.ENCODING_UNSPECIFIED, DynamicRange.BIT_DEPTH_8_BIT),
            )

        val useCaseExpectedSizeMap = mutableMapOf(useCase to maximumSize)

        val useCaseExpectedDynamicRangeMap = mapOf(useCase to SDR)

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_SDR_UNCONSTRAINED,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeResolver_resolvesUnspecified8BitToDolbyVision8Bit_whenAlreadyPresent() {
        // VideoCapture fully resolved Dolby Vision 8-bit
        val videoUseCase =
            createUseCase(CaptureType.VIDEO_CAPTURE, dynamicRange = DynamicRange.DOLBY_VISION_8_BIT)
        // Preview unspecified encoding / 8-bit
        val previewUseCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.UNSPECIFIED)

        // Since there are no 10-bit dynamic ranges, the 10-bit resolution table isn't used.
        // Instead, this will use the camera default LIMITED table which is limited to preview
        // size for 2 PRIV use cases.
        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to previewSize, previewUseCase to previewSize)

        val useCaseExpectedDynamicRangeMap =
            mapOf(
                videoUseCase to DynamicRange.DOLBY_VISION_8_BIT,
                previewUseCase to DynamicRange.DOLBY_VISION_8_BIT,
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            useCasesExpectedDynamicRangeMap = useCaseExpectedDynamicRangeMap,
            dynamicRangeProfiles = DOLBY_VISION_8B_SDR_UNCONSTRAINED,
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun tenBitTable_isUsed_whenAttaching10BitUseCaseToAlreadyAttachedSdrUseCases() {
        // JPEG use case can't be attached with an existing PRIV + YUV in the 10-bit tables
        val useCase = createUseCase(CaptureType.IMAGE_CAPTURE, dynamicRange = HLG_10_BIT)
        val useCaseExpectedSizeMap =
            mapOf(
                // Size would be valid for LIMITED table
                useCase to recordSize
            )
        // existing surfaces (Preview + ImageAnalysis)
        val attachedPreview =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW),
                PRIVATE,
                previewSize,
                SDR,
                listOf(CaptureType.PREVIEW),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                FRAME_RATE_RANGE_UNSPECIFIED,
                /*isStrictFrameRateRequired=*/ false,
            )
        val attachedAnalysis =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD),
                ImageFormat.YUV_420_888,
                recordSize,
                SDR,
                listOf(CaptureType.IMAGE_ANALYSIS),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                FRAME_RATE_RANGE_UNSPECIFIED,
                /*isStrictFrameRateRequired=*/ false,
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                attachedSurfaceInfoList = listOf(attachedPreview, attachedAnalysis),
                // LIMITED allows this combination, but 10-bit table does not
                hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                dynamicRangeProfiles = HLG10_SDR_CONSTRAINED,
                capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun dynamicRangeConstraints_causeAutoResolutionToThrow() {
        val useCase = createUseCase(CaptureType.IMAGE_CAPTURE, dynamicRange = HLG_10_BIT)
        val useCaseExpectedSizeMap =
            mapOf(
                // Size would be valid for 10-bit table within constraints
                useCase to recordSize
            )
        // existing surfaces (PRIV + PRIV)
        val attachedPriv1 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW),
                PRIVATE,
                previewSize,
                DynamicRange.HDR10_10_BIT,
                listOf(CaptureType.PREVIEW),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                FRAME_RATE_RANGE_UNSPECIFIED,
                /*isStrictFrameRateRequired=*/ false,
            )
        val attachedPriv2 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD),
                ImageFormat.YUV_420_888,
                recordSize,
                DynamicRange.HDR10_PLUS_10_BIT,
                listOf(CaptureType.VIDEO_CAPTURE),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                FRAME_RATE_RANGE_UNSPECIFIED,
                /*isStrictFrameRateRequired=*/ false,
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
                    LATENCY_NONE,
                )
            )

        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                attachedSurfaceInfoList = listOf(attachedPriv1, attachedPriv2),
                dynamicRangeProfiles = constraintsTable,
                capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canAttachHlgDynamicRange_toExistingSdrStreams() {
        // JPEG use case can be attached with an existing PRIV + PRIV in the 10-bit tables
        val useCase = createUseCase(CaptureType.IMAGE_CAPTURE, dynamicRange = HLG_10_BIT)
        val useCaseExpectedSizeMap =
            mapOf(
                // Size is valid for 10-bit table within constraints
                useCase to recordSize
            )
        // existing surfaces (PRIV + PRIV)
        val attachedPriv1 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW),
                PRIVATE,
                previewSize,
                SDR,
                listOf(CaptureType.PREVIEW),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                FRAME_RATE_RANGE_UNSPECIFIED,
                /*isStrictFrameRateRequired=*/ false,
            )
        val attachedPriv2 =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD),
                ImageFormat.YUV_420_888,
                recordSize,
                SDR,
                listOf(CaptureType.IMAGE_ANALYSIS),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                FRAME_RATE_RANGE_UNSPECIFIED,
                /*isStrictFrameRateRequired=*/ false,
            )

        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            attachedSurfaceInfoList = listOf(attachedPriv1, attachedPriv2),
            dynamicRangeProfiles = HLG10_SDR_CONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun requiredSdrDynamicRangeThrows_whenCombinedWithConstrainedHlg() {
        // VideoCapture HLG dynamic range
        val videoUseCase = createUseCase(CaptureType.VIDEO_CAPTURE, dynamicRange = HLG_10_BIT)
        // Preview SDR dynamic range
        val previewUseCase = createUseCase(CaptureType.PREVIEW, dynamicRange = SDR)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        // Fails because HLG10 is constrained to only HLG10
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedSizeMap,
                dynamicRangeProfiles = HLG10_CONSTRAINED,
                capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun requiredSdrDynamicRange_canBeCombinedWithUnconstrainedHlg() {
        // VideoCapture HLG dynamic range
        val videoUseCase = createUseCase(CaptureType.VIDEO_CAPTURE, dynamicRange = HLG_10_BIT)
        // Preview SDR dynamic range
        val previewUseCase = createUseCase(CaptureType.PREVIEW, dynamicRange = SDR)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        // Should succeed due to HLG10 being unconstrained
        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HLG10_UNCONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun multiple10BitUnconstrainedDynamicRanges_canBeCombined() {
        // VideoCapture HDR10 dynamic range
        val videoUseCase =
            createUseCase(CaptureType.VIDEO_CAPTURE, dynamicRange = DynamicRange.HDR10_10_BIT)
        // Preview HDR10_PLUS dynamic range
        val previewUseCase =
            createUseCase(CaptureType.PREVIEW, dynamicRange = DynamicRange.HDR10_PLUS_10_BIT)

        val useCaseExpectedSizeMap =
            mutableMapOf(videoUseCase to recordSize, previewUseCase to previewSize)

        // Succeeds because both HDR10 and HDR10_PLUS are unconstrained
        getSuggestedSpecsAndVerify(
            useCaseExpectedSizeMap,
            dynamicRangeProfiles = HDR10_HDR10_PLUS_UNCONSTRAINED,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
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
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(25, 30))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    @Test
    fun getSuggestedStreamSpec_single_invalid_targetFPS() {
        // an invalid target means the device would neve be able to reach that fps
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(65, 70))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase, Size(800, 450)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_targetFPS_first_is_larger() {
        // a valid target means the device is capable of that fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 35))
        val useCase2 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(15, 25))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1445
                put(useCase1, Size(1920, 1445))
                put(useCase2, Size(1920, 1445))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_targetFPS_first_is_smaller() {
        // a valid target means the device is capable of that fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 35))
        val useCase2 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(45, 50))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1440
                put(useCase1, Size(1920, 1440))
                put(useCase2, Size(1920, 1440))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_targetFPS_intersect() {
        // first and second new use cases have target fps that intersect each other
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 40))
        val useCase2 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(35, 45))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // effective target fps becomes 35-40
                // both selected size should be no larger than 1920 x 1080
                put(useCase1, Size(1920, 1080))
                put(useCase2, Size(1920, 1080))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_cases_first_has_targetFPS() {
        // first new use case has a target fps, second new use case does not
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 35))
        val useCase2 = createUseCase(CaptureType.PREVIEW)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1440
                put(useCase1, Size(1920, 1440))
                put(useCase2, Size(1920, 1440))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_multiple_cases_second_has_targetFPS() {
        // second new use case does not have a target fps, first new use case does not
        val useCase1 = createUseCase(CaptureType.PREVIEW)
        val useCase2 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 35))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // both selected size should be no larger than 1920 x 1440
                put(useCase1, Size(1920, 1440))
                put(useCase2, Size(1920, 1440))
            }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_attached_with_targetFPS_no_new_targetFPS() {
        // existing surface with target fps + new use case without a target fps
        val useCase = createUseCase(CaptureType.PREVIEW)
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // size should be no larger than 1280 x 960
                put(useCase, Size(1280, 960))
            }
        // existing surface w/ target fps
        val attachedSurfaceInfo =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW),
                JPEG,
                Size(1280, 720),
                SDR,
                listOf(CaptureType.PREVIEW),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                Range(40, 50),
                /*isStrictFrameRateRequired=*/ false,
            )
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            attachedSurfaceInfoList = listOf(attachedSurfaceInfo),
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_attached_with_targetFPS_and_new_targetFPS_no_intersect() {
        // existing surface with target fps + new use case with target fps that does not intersect
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 35))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // size of new surface should be no larger than 1280 x 960
                put(useCase, Size(1280, 960))
            }
        // existing surface w/ target fps
        val attachedSurfaceInfo =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW),
                JPEG,
                Size(1280, 720),
                SDR,
                listOf(CaptureType.PREVIEW),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                Range(40, 50),
                /*isStrictFrameRateRequired=*/ false,
            )
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            attachedSurfaceInfoList = listOf(attachedSurfaceInfo),
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_attached_with_targetFPS_and_new_targetFPS_with_intersect() {
        // existing surface with target fps + new use case with target fps that intersect each other
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(45, 50))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply {
                // size of new surface should be no larger than 1280 x 720
                put(useCase, Size(1280, 720))
            }
        // existing surface w/ target fps
        val attachedSurfaceInfo =
            AttachedSurfaceInfo.create(
                SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW),
                JPEG,
                Size(1280, 720),
                SDR,
                listOf(CaptureType.PREVIEW),
                useCase.currentConfig,
                SESSION_TYPE_REGULAR,
                Range(40, 50),
                /*isStrictFrameRateRequired=*/ false,
            )
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            attachedSurfaceInfoList = listOf(attachedSurfaceInfo),
            hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            compareWithAtMost = true,
        )
    }

    @Test
    fun getSuggestedStreamSpec_has_device_supported_expectedFrameRateRange() {
        // use case with target fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(15, 25))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(10, 22),
        )
        // expected fps 10,22 because it has the largest intersection
    }

    @Test
    fun getSuggestedStreamSpec_isStrictFpsRequiredButFpsNotSupported_throwException() {
        val useCase1 =
            createUseCase(
                CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(15, 25),
                isStrictFpsRequired = true,
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
        }
    }

    @Test
    fun getSuggestedStreamSpec_differentIsStrictFpsRequired_throwException() {
        val useCase1 =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                targetFrameRate = Range(22, 22),
                isStrictFpsRequired = true,
            )
        val useCase2 =
            createUseCase(
                CaptureType.PREVIEW,
                targetFrameRate = Range(22, 22),
                isStrictFpsRequired = false,
            )
        val useCaseExpectedResultMap =
            mapOf(useCase1 to Size(3840, 2160), useCase2 to Size(1280, 720))
        Assert.assertThrows(IllegalStateException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
        }
    }

    @Test
    fun getSuggestedStreamSpec_isStrictFpsRequiredAndDifferentFrameRates_throwException() {
        val useCase1 =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                targetFrameRate = Range(22, 22),
                isStrictFpsRequired = true,
            )
        val useCase2 =
            createUseCase(
                CaptureType.PREVIEW,
                targetFrameRate = Range(10, 22),
                isStrictFpsRequired = true,
            )
        val useCaseExpectedResultMap =
            mapOf(useCase1 to Size(3840, 2160), useCase2 to Size(1280, 720))
        Assert.assertThrows(IllegalStateException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
        }
    }

    @Test
    fun getSuggestedStreamSpec_has_exact_device_supported_expectedFrameRateRange() {
        // use case with target fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 30))
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30),
        )
        // expected fps 30,30 because it is an exact intersection
    }

    @Test
    fun getSuggestedStreamSpec_isStrictFpsRequiredAndFpsSupported_frameRateMatches() {
        // use case with target fps
        val useCase1 =
            createUseCase(
                CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 30),
                isStrictFpsRequired = true,
            )
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30),
        )
    }

    @Test
    fun getSuggestedStreamSpec_isStrictFpsRequiredButOverMaxFps_throwException() {
        // use case with target fps
        val useCase1 =
            createUseCase(
                CaptureType.PREVIEW,
                targetFrameRate = Range<Int>(30, 30),
                isStrictFpsRequired = true,
            )
        val useCasesOutputSizesMap = mapOf(useCase1 to listOf(Size(3840, 2160))) // MaxFps = 25
        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(3840, 2160)) }
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
        }
    }

    @Test
    fun getSuggestedStreamSpec_has_no_device_supported_expectedFrameRateRange() {
        // use case with target fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(65, 65))

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(800, 450)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(60, 60),
        )
        // expected fps 60,60 because it is the closest range available
    }

    @Test
    fun getSuggestedStreamSpec_has_multiple_device_supported_expectedFrameRateRange() {

        // use case with target fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(36, 45))

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1280, 960)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 50),
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
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(26, 27))

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30),
        )
        // 30,30 was expected because it is the closest and shortest range to our target fps
    }

    @Test
    fun getSuggestedStreamSpec_has_no_device_intersection_equidistant_expectedFrameRateRange() {

        // use case with target fps
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(26, 26))

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(1920, 1440)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareWithAtMost = true,
            compareExpectedFps = Range(30, 30),
        )
        // 30,30 selected because although there are other ranges that  have the same distance to
        // the target, 30,30 is the shortest range that also happens to be on the upper side of the
        // target range
    }

    @Test
    fun getSuggestedStreamSpec_has_no_expectedFrameRateRange() {
        // a valid target means the device is capable of that fps

        // use case with no target fps
        val useCase1 = createUseCase(CaptureType.PREVIEW)

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = FRAME_RATE_RANGE_UNSPECIFIED,
        )
        // since no target fps present, no specific device fps will be selected, and is set to
        // unspecified: (0,0)
    }

    @Test
    fun getSuggestedStreamSpec_whenTargetFrameRateRangeIsUnspecified_unspecifiedRangeSuggested() {
        // a valid target means the device is capable of that fps

        // use case with no target fps
        val useCase1 =
            createUseCase(CaptureType.PREVIEW, targetFrameRate = FRAME_RATE_RANGE_UNSPECIFIED)

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = FRAME_RATE_RANGE_UNSPECIFIED,
        )
        // since target fps is unspecified, no specific device fps will be selected, and is set to
        // unspecified: (0,0)
    }

    @Test
    fun getSuggestedStreamSpec_isStrictFpsRequiredAndFpsUnspecified_noExpectedFrameRate() {
        val useCase1 = createUseCase(CaptureType.PREVIEW, isStrictFpsRequired = true)

        val useCaseExpectedResultMap =
            mutableMapOf<UseCase, Size>().apply { put(useCase1, Size(4032, 3024)) }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = FRAME_RATE_RANGE_UNSPECIFIED,
        )
    }

    @Test
    fun getSuggestedStreamSpec_singleUseCase_returnMaxSupportedFrameRate() {
        // Arrange.
        val useCase = createUseCase(CaptureType.PREVIEW)

        val useCasesOutputSizesMap =
            mapOf(
                useCase to
                    listOf(
                        Size(3840, 2160), // MaxFps = 25
                        Size(1920, 1080), // MaxFps = 35
                    )
            )

        // 3840x2160 is the first acceptable combination.
        val useCaseExpectedResultMap = mapOf(useCase to Size(3840, 2160))

        // Act.
        val result =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
                findMaxSupportedFrameRate = true,
            )

        // Verify.
        assertThat(result.maxSupportedFrameRate).isEqualTo(35)
    }

    @Test
    fun getSuggestedStreamSpec_singleUseCaseWithTargetFpsSet_returnMaxSupportedFrameRate() {
        // Arrange.
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range(30, 30))

        val useCasesOutputSizesMap =
            mapOf(
                useCase to
                    listOf(
                        Size(3840, 2160), // MaxFps = 25
                        Size(1920, 1080), // MaxFps = 35
                    )
            )

        // targetFps will be ignored, so the first combination will be adopted.
        val useCaseExpectedResultMap = mapOf(useCase to Size(3840, 2160))

        // Act.
        val result =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
                findMaxSupportedFrameRate = true,
            )

        // Verify.
        assertThat(result.maxSupportedFrameRate).isEqualTo(35)
    }

    @Test
    fun getSuggestedStreamSpec_singleUseCaseWithStrictFpsSet_returnMaxSupportedFrameRate() {
        // Arrange.
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                targetFrameRate = Range(30, 30),
                isStrictFpsRequired = true,
            )

        val useCasesOutputSizesMap =
            mapOf(
                useCase to
                    listOf(
                        Size(3840, 2160), // MaxFps = 25
                        Size(1920, 1080), // MaxFps = 35
                    )
            )

        // targetFps will be ignored, so the first combination will be adopted.
        val useCaseExpectedResultMap = mapOf(useCase to Size(3840, 2160))

        // Act.
        val result =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
                findMaxSupportedFrameRate = true,
            )

        // Verify.
        assertThat(result.maxSupportedFrameRate).isEqualTo(35)
    }

    @Test
    fun getSuggestedStreamSpec_multipleUseCases_returnMaxSupportedFrameRate() {
        // Arrange.
        val useCase1 = createUseCase(CaptureType.PREVIEW)
        val useCase2 = createUseCase(CaptureType.VIDEO_CAPTURE)

        // Output size combinations:
        // * 3840x2160 + 1280x720 : MaxFps = 25
        // * 1920x1080 + 1280x720 : MaxFps = 35, the MaxFps of all combination.
        val useCasesOutputSizesMap =
            mapOf(
                useCase1 to
                    listOf(
                        Size(3840, 2160), // MaxFps = 25
                        Size(1920, 1080), // MaxFps = 35
                    ),
                useCase2 to
                    listOf(
                        Size(1280, 720) // MaxFps = 45
                    ),
            )

        // 3840x2160 + 1280x720 is the first acceptable combination.
        val useCaseExpectedResultMap =
            mapOf(useCase1 to Size(3840, 2160), useCase2 to Size(1280, 720))

        // Act.
        val result =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
                findMaxSupportedFrameRate = true,
            )

        // Verify.
        assertThat(result.maxSupportedFrameRate).isEqualTo(35)
    }

    @Test
    fun getSuggestedStreamSpec_multipleUseCasesWithTargetFpsSet_returnMaxSupportedFrameRate() {
        // Arrange.
        val useCase1 = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range(30, 30))
        val useCase2 = createUseCase(CaptureType.VIDEO_CAPTURE, targetFrameRate = Range(30, 30))

        // Output size combinations:
        // * 3840x2160 + 1280x720 : MaxFps = 25
        // * 1920x1080 + 1280x720 : MaxFps = 35, the MaxFps of all combination.
        val useCasesOutputSizesMap =
            mapOf(
                useCase1 to
                    listOf(
                        Size(3840, 2160), // MaxFps = 25
                        Size(1920, 1080), // MaxFps = 35
                    ),
                useCase2 to
                    listOf(
                        Size(1280, 720) // MaxFps = 45
                    ),
            )

        // targetFps will be ignored, so the first combination will be adopted.
        val useCaseExpectedResultMap =
            mapOf(useCase1 to Size(3840, 2160), useCase2 to Size(1280, 720))

        // Act.
        val result =
            getSuggestedSpecsAndVerify(
                useCaseExpectedResultMap,
                hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
                findMaxSupportedFrameRate = true,
            )

        // Verify.
        assertThat(result.maxSupportedFrameRate).isEqualTo(35)
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val imageFormat = JPEG
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val imageFormat = JPEG
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
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val imageFormat = JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.s1440pSizeMap[imageFormat]).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    @Config(minSdk = 23)
    fun correctMaximumSize_withHighResolutionOutputSizes() {
        setupCamera(highResolutionSupportedSizes = highResolutionSupportedSizes)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val imageFormat = JPEG
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
                ),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val imageFormat = JPEG
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
            cameraId = CameraId("externalCameraId"),
        )
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )

        // Checks the determined RECORD size
        assertThat(supportedSurfaceCombination.surfaceSizeDefinition.recordSize)
            .isEqualTo(legacyVideoMaximumSize)
    }

    @Test
    fun determineRecordSizeFromStreamConfigurationMap_intExternalCameraId() {
        // Setup camera with external integer camera Id
        setupCamera(cameraId = CameraId("101"))
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEmptyEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )

        // Checks the determined RECORD size
        assertThat(supportedSurfaceCombination.surfaceSizeDefinition.recordSize)
            .isEqualTo(legacyVideoMaximumSize)
    }

    @Test
    fun applyLegacyApi21QuirkCorrectly() {
        setupCamera()
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
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
                ImageFormat.YUV_420_888,
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
        setupCamera(hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                NO_OP_FEATURE_COMBINATION_QUERY,
            )
        val resultList =
            supportedSurfaceCombination.applyResolutionSelectionOrderRelatedWorkarounds(
                supportedSizes.toList(),
                ImageFormat.YUV_420_888,
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

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // STREAM_USE_CASE tests
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseAsStillCaptureType_withSingleImageCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(maximumSize),
            expectedStreamUseCases = listOf(StreamUseCase.STILL_CAPTURE),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseAsMultiPurposeType_withSinglePreview() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW),
            expectedSizes = listOf(previewSize),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW_VIDEO_STILL),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseAsMultiPurposeType_withSingleImageAnalysis() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.IMAGE_ANALYSIS),
            expectedSizes = listOf(RESOLUTION_VGA),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW_VIDEO_STILL),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseTypes_withPreviewAndImageCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW, CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(previewSize, maximumSize),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW, StreamUseCase.STILL_CAPTURE),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseTypes_withPreviewAndVideoCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW, CaptureType.VIDEO_CAPTURE),
            expectedSizes = listOf(previewSize, recordSize),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW, StreamUseCase.VIDEO_RECORD),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseTypes_withPreviewAndImageAnalysis() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW, CaptureType.IMAGE_ANALYSIS),
            expectedSizes = listOf(previewSize, RESOLUTION_VGA),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW, StreamUseCase.PREVIEW),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseTypes_withPreviewVideoCaptureAndImageCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes =
                listOf(CaptureType.PREVIEW, CaptureType.VIDEO_CAPTURE, CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(previewSize, recordSize, recordSize),
            expectedStreamUseCases =
                listOf(
                    StreamUseCase.PREVIEW,
                    StreamUseCase.VIDEO_RECORD,
                    StreamUseCase.STILL_CAPTURE,
                ),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseTypes_withPreviewImageAnalysisAndImageCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes =
                listOf(CaptureType.PREVIEW, CaptureType.IMAGE_ANALYSIS, CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(previewSize, RESOLUTION_VGA, maximumSize),
            expectedStreamUseCases =
                listOf(StreamUseCase.PREVIEW, StreamUseCase.PREVIEW, StreamUseCase.STILL_CAPTURE),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseStreamSpecOption_overrideImageCaptureAsVideoRecordType() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(maximumSize),
            expectedStreamUseCases = listOf(StreamUseCase.VIDEO_RECORD),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.STILL_CAPTURE),
            streamUseCasesOverride = listOf(StreamUseCase.VIDEO_RECORD),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun throwException_PopulateStreamUseCaseStreamSpecOption_notFullyOverride() {
        assertThrows(IllegalArgumentException::class.java) {
            populateStreamUseCaseTypesForUseCases(
                captureTypes = listOf(CaptureType.IMAGE_CAPTURE, CaptureType.PREVIEW),
                expectedSizes = listOf(maximumSize, previewSize),
                expectedStreamUseCases = emptyList(), // unnecessary for the test
                useCaseConfigStreamUseCases =
                    listOf(StreamUseCase.STILL_CAPTURE, StreamUseCase.PREVIEW),
                streamUseCasesOverride = listOf(StreamUseCase.VIDEO_RECORD, NO_STREAM_USE_CASE),
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun skipPopulateStreamUseCaseStreamSpecOption_unsupportedPreviewMaxSizeCombination() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW),
            expectedSizes = listOf(maximumSize),
            expectedStreamUseCases = listOf(NO_STREAM_USE_CASE),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.PREVIEW),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun skipPopulateStreamUseCaseStreamSpecOption_unsupportedPreviewAndPreviewCombination() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW, CaptureType.PREVIEW),
            expectedSizes = listOf(previewSize, previewSize),
            expectedStreamUseCases = listOf(NO_STREAM_USE_CASE, NO_STREAM_USE_CASE),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.PREVIEW, StreamUseCase.PREVIEW),
            streamUseCasesOverride = listOf(StreamUseCase.VIDEO_RECORD, StreamUseCase.VIDEO_RECORD),
        )
    }

    @Config(minSdk = 21, maxSdk = 32)
    @Test
    fun skipPopulateStreamUseCaseStreamSpecOption_unsupportedOs() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(maximumSize),
            expectedStreamUseCases = listOf(NO_STREAM_USE_CASE),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.STILL_CAPTURE),
        )
    }

    @Config(minSdk = 21, maxSdk = 32)
    @Test
    fun skipPopulateStreamUseCaseStreamSpecOption_unsupportedOsWithOverrideStreamUseCase() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.IMAGE_CAPTURE),
            expectedSizes = listOf(maximumSize),
            expectedStreamUseCases = listOf(NO_STREAM_USE_CASE),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.STILL_CAPTURE),
            streamUseCasesOverride = listOf(StreamUseCase.VIDEO_RECORD),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs_wrongImageFormat() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW, CaptureType.IMAGE_ANALYSIS),
            expectedSizes = listOf(previewSize, RESOLUTION_VGA),
            useCaseConfigImageFormats = listOf(JPEG, null),
            expectedStreamUseCases = listOf(NO_STREAM_USE_CASE, NO_STREAM_USE_CASE),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.PREVIEW, StreamUseCase.PREVIEW),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun populateStreamUseCaseStreamSpecOptionWithSupportedSurfaceConfigs_wrongCaptureType() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.PREVIEW, CaptureType.PREVIEW),
            expectedSizes = listOf(previewSize, recordSize),
            expectedStreamUseCases = listOf(NO_STREAM_USE_CASE, NO_STREAM_USE_CASE),
            useCaseConfigStreamUseCases = listOf(StreamUseCase.PREVIEW, StreamUseCase.PREVIEW),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseStreamSpecOption_meteringRepeatingForImageCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.METERING_REPEATING),
            expectedSizes = listOf(RESOLUTION_VGA),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW),
            attachedSurfaceInfoList = listOf(createImageCaptureAttachedSurfaceInfo()),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun canPopulateStreamUseCaseStreamSpecOption_meteringRepeatingForVideoCapture() {
        populateStreamUseCaseTypesForUseCases(
            captureTypes = listOf(CaptureType.METERING_REPEATING),
            expectedSizes = listOf(RESOLUTION_VGA),
            expectedStreamUseCases = listOf(StreamUseCase.PREVIEW),
            attachedSurfaceInfoList = listOf(createVideoCaptureAttachedSurfaceInfo()),
        )
    }

    /**
     * Creates the UseCases to populate the stream use case and verify the result.
     *
     * @param captureTypes capture types info for the use cases.
     * @param expectedSizes expected sizes for the use cases.
     * @param expectedStreamUseCases expected stream use cases for the use cases.
     * @param useCaseConfigImageFormats image format setting in the use cases' configs.
     * @param useCaseConfigStreamUseCases default stream use cases setting in the use cases'
     *   configs.
     * @param streamUseCasesOverride the stream use cases override in the use cases' configs.
     * @param attachedSurfaceInfoList the attached surface info list.
     */
    private fun populateStreamUseCaseTypesForUseCases(
        captureTypes: List<CaptureType>,
        expectedSizes: List<Size>,
        expectedStreamUseCases: List<StreamUseCase?>,
        useCaseConfigImageFormats: List<Int?>? = null,
        useCaseConfigStreamUseCases: List<StreamUseCase?>? = null,
        streamUseCasesOverride: List<StreamUseCase?>? = null,
        attachedSurfaceInfoList: List<AttachedSurfaceInfo> = emptyList(),
    ) {
        val useCasesOutputSizesMap = mutableMapOf<UseCase, List<Size>>()
        val useCaseExpectedSizeResultMap = mutableMapOf<UseCase, Size>()
        val useCaseExpectedStreamUseCaseResultMap = mutableMapOf<UseCase, StreamUseCase?>()

        captureTypes.onEachIndexed { index, captureType ->
            val useCase =
                createUseCase(
                    captureType = captureType,
                    imageFormat = useCaseConfigImageFormats?.get(index),
                    streamUseCaseOverride = streamUseCasesOverride?.get(index),
                    streamUseCase = useCaseConfigStreamUseCases?.get(index),
                )
            useCasesOutputSizesMap[useCase] = listOf(expectedSizes[index])
            useCaseExpectedSizeResultMap[useCase] = expectedSizes[index]
            useCaseExpectedStreamUseCaseResultMap[useCase] = expectedStreamUseCases.getOrNull(index)
        }

        val result =
            getSuggestedSpecsAndVerify(
                useCasesExpectedResultMap = useCaseExpectedSizeResultMap,
                attachedSurfaceInfoList = attachedSurfaceInfoList,
                hardwareLevel = INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
            )
        assertThat(result.useCaseStreamSpecs.size).isEqualTo(captureTypes.size)

        useCaseExpectedStreamUseCaseResultMap.keys.forEach { useCase ->
            assertThat(result.useCaseStreamSpecs[useCase.currentConfig]!!.getStreamUseCase())
                .isEqualTo(useCaseExpectedStreamUseCaseResultMap[useCase]?.value)
        }
    }

    private fun createImageCaptureAttachedSurfaceInfo() =
        AttachedSurfaceInfo.create(
            SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM, StreamUseCase.STILL_CAPTURE),
            JPEG,
            maximumSize,
            SDR,
            listOf(CaptureType.IMAGE_CAPTURE),
            MutableOptionsBundle.emptyBundle(),
            SESSION_TYPE_REGULAR,
            FRAME_RATE_RANGE_UNSPECIFIED,
            false,
        )

    private fun createVideoCaptureAttachedSurfaceInfo() =
        AttachedSurfaceInfo.create(
            SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD, StreamUseCase.VIDEO_RECORD),
            PRIVATE,
            recordSize,
            SDR,
            listOf(CaptureType.VIDEO_CAPTURE),
            MutableOptionsBundle.emptyBundle(),
            SESSION_TYPE_REGULAR,
            FRAME_RATE_RANGE_UNSPECIFIED,
            false,
        )

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun getSuggestedStreamSpec_highSpeed_returnsCorrectSizeAndFpsRange() {
        val targetFrameRate = Range.create(240, 240)
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                surfaceOccupancyPriority = 2,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                surfaceOccupancyPriority = 5,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val useCasesOutputSizesMap =
            mapOf(
                previewUseCase to listOf(RESOLUTION_VGA, RESOLUTION_1080P, RESOLUTION_720P),
                videoUseCase to listOf(RESOLUTION_1440P, RESOLUTION_720P, RESOLUTION_1080P),
            )
        // videoUseCase has higher surface priority so the expected size should be the first
        // common size of videoUseCase. i.e. RESOLUTION_720P.
        val useCaseExpectedResultMap =
            mapOf(previewUseCase to RESOLUTION_720P, videoUseCase to RESOLUTION_720P)
        getSuggestedSpecsAndVerifyForHighSpeed(
            useCaseExpectedResultMap,
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            compareExpectedFps = Range.create(240, 240),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun getSuggestedStreamSpec_highSpeed_singleSurface_returnsCorrectSizeAndClosestFps() {
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = Range.create(30, 480),
            )
        val useCasesOutputSizesMap = mapOf(previewUseCase to listOf(RESOLUTION_1080P))
        val useCaseExpectedResultMap = mapOf(previewUseCase to RESOLUTION_1080P)
        getSuggestedSpecsAndVerifyForHighSpeed(
            useCaseExpectedResultMap,
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            compareExpectedFps = Range.create(30, 240), // Find the closest supported fps.
        )
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun getSuggestedStreamSpec_highSpeed_multipleSurfaces_returnsCorrectSizeAndClosetMaxFps() {
        val targetFrameRate = Range.create(30, 480)
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val useCasesOutputSizesMap =
            mapOf(
                previewUseCase to listOf(RESOLUTION_1080P),
                videoUseCase to listOf(RESOLUTION_1080P),
            )
        val useCaseExpectedResultMap =
            mapOf(previewUseCase to RESOLUTION_1080P, videoUseCase to RESOLUTION_1080P)
        getSuggestedSpecsAndVerifyForHighSpeed(
            useCaseExpectedResultMap,
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            compareExpectedFps = Range.create(240, 240), // Find the closest max supported fps.
        )
    }

    @Config(minSdk = 21, maxSdk = 22)
    @Test
    fun getSuggestedStreamSpec_highSpeed_unsupportedSdkVersion_throwException() {
        val useCase =
            createUseCase(
                CaptureType.PREVIEW,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = Range.create(240, 240),
            )
        val useCaseExpectedResultMap = mapOf(useCase to RESOLUTION_1080P)
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerifyForHighSpeed(useCaseExpectedResultMap)
        }
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun getSuggestedStreamSpec_highSpeed_noCommonSize_throwException() {
        val targetFrameRate = Range.create(240, 240)
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val useCasesOutputSizesMap =
            mapOf(
                previewUseCase to listOf(RESOLUTION_VGA, RESOLUTION_720P),
                videoUseCase to listOf(RESOLUTION_1440P, RESOLUTION_1080P),
            )
        val useCaseExpectedResultMap =
            mapOf(previewUseCase to RESOLUTION_VGA, videoUseCase to RESOLUTION_1440P)
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerifyForHighSpeed(
                useCaseExpectedResultMap,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
            )
        }
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun getSuggestedStreamSpec_highSpeed_tooManyUseCases_throwException() {
        val targetFrameRate = Range.create(240, 240)
        val previewUseCase1 =
            createUseCase(
                CaptureType.PREVIEW,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val previewUseCase2 =
            createUseCase(
                CaptureType.PREVIEW,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val videoUseCase =
            createUseCase(
                CaptureType.VIDEO_CAPTURE,
                sessionType = SESSION_TYPE_HIGH_SPEED,
                targetFrameRate = targetFrameRate,
            )
        val useCasesOutputSizesMap =
            mapOf(
                previewUseCase1 to listOf(RESOLUTION_1080P),
                previewUseCase2 to listOf(RESOLUTION_1080P),
                videoUseCase to listOf(RESOLUTION_1080P),
            )
        val useCaseExpectedResultMap =
            mapOf(
                previewUseCase1 to RESOLUTION_1080P,
                previewUseCase2 to RESOLUTION_1080P,
                videoUseCase to RESOLUTION_1080P,
            )
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSuggestedSpecsAndVerifyForHighSpeed(
                useCaseExpectedResultMap,
                useCasesOutputSizesMap = useCasesOutputSizesMap,
            )
        }
    }

    @Test
    fun filterSupportedSizes_notFeatureComboInvocation_withoutTargetFpsRange_filtersCorrectly() {
        // Arrange
        val useCaseConfig = createUseCase(CaptureType.IMAGE_CAPTURE).currentConfig
        val supportedSurfaceCombination = createSupportedSurfaceCombinationWithSetup()
        val useCaseConfigToSizesMap =
            mapOf(
                useCaseConfig to
                    listOf(
                        maximumSize, // maps to MAX size with max FPS of 20
                        recordSize, // maps to RECORD size with max FPS of 25
                        S1440P_16_9.relatedFixedSize!!, // maps to RECORD size with max FPS of 30
                        S1440P_4_3.relatedFixedSize!!, // maps to RECORD size with max FPS of 30
                        previewSize, // maps to PREVIEW size with max FPS of 45
                        S720P_16_9.relatedFixedSize!!, // maps to PREVIEW size with max FPS of 45
                    )
            )

        // Act
        val filteredSizes =
            supportedSurfaceCombination.filterSupportedSizes(
                useCaseConfigToSizesMap,
                createFeatureSettings(),
            )

        // Assert: Sizes mapping to same ConfigSize pairs are filtered out
        assertThat(filteredSizes.getValue(useCaseConfig))
            .containsExactly(maximumSize, recordSize, previewSize)
            .inOrder()
    }

    @Test
    fun filterSupportedSizes_notFeatureComboInvocation_withTargetFpsRange_filtersCorrectly() {
        // Arrange
        val useCaseConfig = createUseCase(CaptureType.IMAGE_CAPTURE).currentConfig
        val supportedSurfaceCombination = createSupportedSurfaceCombinationWithSetup()
        val useCaseConfigToSizesMap =
            mapOf(
                useCaseConfig to
                    listOf(
                        maximumSize, // maps to MAX size with max FPS of 20
                        recordSize, // maps to RECORD size with max FPS of 25
                        S1440P_16_9.relatedFixedSize!!, // maps to RECORD size with max FPS of 30
                        S1440P_4_3.relatedFixedSize!!, // maps to RECORD size with max FPS of 30
                        previewSize, // maps to PREVIEW size with max FPS of 45
                        S720P_16_9.relatedFixedSize!!, // maps to PREVIEW size with max FPS of 45
                    )
            )

        // Act
        val filteredSizes =
            supportedSurfaceCombination.filterSupportedSizes(
                useCaseConfigToSizesMap,
                createFeatureSettings(targetFpsRange = Range(30, 30)),
            )

        // Assert: Sizes mapping to same (ConfigSize, FPS) pairs are filtered out
        assertThat(filteredSizes.getValue(useCaseConfig))
            .containsExactly(maximumSize, recordSize, S1440P_16_9.relatedFixedSize, previewSize)
            .inOrder()
    }

    @Test
    fun filterSupportedSizes_featureComboInvocationButFcqNotRequired_filtersCorrectly() {
        // Arrange
        val useCaseConfig = createUseCase(CaptureType.IMAGE_CAPTURE).currentConfig
        val supportedSurfaceCombination = createSupportedSurfaceCombinationWithSetup()
        val useCaseConfigToSizesMap =
            mapOf(
                useCaseConfig to
                    listOf(
                        maximumSize, // maps to MAX size with max FPS of 20
                        recordSize, // maps to UHD size with max FPS of 25
                        S1440P_16_9.relatedFixedSize!!, // maps to 1440P_16_9 size with 30 max FPS
                        S1440P_4_3.relatedFixedSize!!, // ConfigSize.NOT_SUPPORT, not in src table
                        previewSize, // maps to 720P_16_9 size with max FPS of 45
                        S720P_16_9.relatedFixedSize!!, // maps to 720P_16_9 size with 45 max FPS
                    )
            )

        // Act
        val filteredSizes =
            supportedSurfaceCombination.filterSupportedSizes(
                useCaseConfigToSizesMap,
                createFeatureSettings(
                    isFeatureComboInvocation = true,
                    requiresFeatureComboQuery = false,
                ),
            )

        // Assert: Unsupported sizes are filtered out. Since the capture session tables are used in
        // this test, not FCQ table, S1440P_16_9 is transformed to ConfigSize#RECORD and thus
        // filtered out.
        assertThat(filteredSizes.getValue(useCaseConfig))
            .containsExactly(maximumSize, recordSize, previewSize)
            .inOrder()
    }

    @Test
    fun filterSupportedSizes_featureComboInvocationAndFcqRequired_filtersCorrectly() {
        // Arrange
        val useCaseConfig = createUseCase(CaptureType.IMAGE_CAPTURE).currentConfig
        val supportedSurfaceCombination = createSupportedSurfaceCombinationWithSetup()
        val useCaseConfigToSizesMap =
            mapOf(
                useCaseConfig to
                    listOf(
                        maximumSize, // maps to MAX size with max FPS of 20
                        recordSize, // maps to UHD size with max FPS of 25
                        S1440P_16_9.relatedFixedSize!!, // maps to 1440P_16_9 size with 30 max FPS
                        S1440P_4_3.relatedFixedSize!!, // ConfigSize.NOT_SUPPORT, not in src table
                        previewSize, // maps to 720P_16_9 size with max FPS of 45
                        S720P_16_9.relatedFixedSize!!, // maps to 720P_16_9 size with 45 max FPS
                    )
            )

        // Act
        val filteredSizes =
            supportedSurfaceCombination.filterSupportedSizes(
                useCaseConfigToSizesMap,
                createFeatureSettings(
                    isFeatureComboInvocation = true,
                    requiresFeatureComboQuery = true,
                ),
            )

        // Assert: Unsupported sizes are filtered out. Since the FCQ table is used in this test, not
        // the capture session tables, S1440P_16_9 is transformed to a distinct ConfigSize and thus
        // not filtered out.
        assertThat(filteredSizes.getValue(useCaseConfig))
            .containsExactly(maximumSize, recordSize, S1440P_16_9.relatedFixedSize, previewSize)
            .inOrder()
    }

    @Test
    fun filterSupportedSizes_featureComboQueryRequired_withTargetFpsRange_filtersCorrectly() {
        // Arrange
        val useCaseConfig = createUseCase(CaptureType.IMAGE_CAPTURE).currentConfig
        val supportedSurfaceCombination = createSupportedSurfaceCombinationWithSetup()
        val useCaseConfigToSizesMap =
            mapOf(
                useCaseConfig to
                    listOf(
                        maximumSize, // maps to MAX size with max FPS of 20
                        recordSize, // maps to UHD size with max FPS of 25
                        S1440P_16_9.relatedFixedSize!!, // maps to 1440P_16_9 size with 30 max FPS
                        S1440P_4_3.relatedFixedSize!!, // ConfigSize.NOT_SUPPORT, not in src table
                        previewSize, // maps to 720P_16_9 size with max FPS of 45
                        S720P_16_9.relatedFixedSize!!, // maps to 720P_16_9 size with 45 max FPS
                    )
            )

        // Act
        val filteredSizes =
            supportedSurfaceCombination.filterSupportedSizes(
                useCaseConfigToSizesMap,
                createFeatureSettings(
                    isFeatureComboInvocation = true,
                    requiresFeatureComboQuery = true,
                    targetFpsRange = Range(22, 30),
                ),
            )

        // Assert: Unsupported sizes are filtered out, e.g. recordSize is filtered out since it
        // doesn't have a high enough max FPS.
        assertThat(filteredSizes.getValue(useCaseConfig))
            .containsExactly(S1440P_16_9.relatedFixedSize, S720P_16_9.relatedFixedSize)
            .inOrder()
    }

    @Test
    fun checkSupported_featureComboQueryNotRequiredInSettings_featureCombinationQueryNotInvoked() {
        // Arrange: Setup resources with a FeatureCombinationQuery impl. tracking isSupported calls
        setupCamera()
        val latch = CountDownLatch(1)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                object : FeatureCombinationQuery {
                    override fun isSupported(sessionConfig: SessionConfig): Boolean {
                        latch.countDown()
                        return false
                    }
                },
            )
        val surfaceConfigList =
            GuaranteedConfigurationsUtil.QUERYABLE_FCQ_COMBINATIONS.first().surfaceConfigList

        // Act: Check for a FCQ SurfaceConfig combination
        supportedSurfaceCombination.checkSupported(
            createFeatureSettings(requiresFeatureComboQuery = false),
            surfaceConfigList,
        )

        // Assert: Waits a small time for latch update in isSupported call just in case any code
        // flow happens asynchronously in future
        assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun checkSupported_featureComboQueryReportsUnsupported_fcqSurfaceCombinationUnsupported() {
        // Arrange: Setup resources with a FeatureCombinationQuery impl. that always returns false
        setupCamera()
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                fakeFeatureCombinationQuery.apply { isSupported = false },
            )
        val surfaceConfigList =
            GuaranteedConfigurationsUtil.QUERYABLE_FCQ_COMBINATIONS.first().surfaceConfigList

        // Act & assert
        assertThat(
                supportedSurfaceCombination.checkSupported(
                    createFeatureSettings(requiresFeatureComboQuery = true),
                    surfaceConfigList,
                    surfaceConfigList.associateWith { DynamicRange.UNSPECIFIED },
                    surfaceConfigList.toUseCaseConfigs(),
                    (0 until surfaceConfigList.size).toList(),
                )
            )
            .isFalse()
    }

    @Test
    fun checkSupported_featureComboQueryReportsSupported_fcqSurfaceCombinationSupported() {
        // Arrange: Setup resources with a FeatureCombinationQuery impl. that always returns true
        setupCamera()
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                fakeFeatureCombinationQuery.apply { isSupported = true },
            )
        val surfaceConfigList =
            GuaranteedConfigurationsUtil.QUERYABLE_FCQ_COMBINATIONS.first().surfaceConfigList

        // Act & assert
        assertThat(
                supportedSurfaceCombination.checkSupported(
                    createFeatureSettings(requiresFeatureComboQuery = true),
                    surfaceConfigList,
                    surfaceConfigList.associateWith { DynamicRange.UNSPECIFIED },
                    surfaceConfigList.toUseCaseConfigs(),
                    (0 until surfaceConfigList.size).toList(),
                )
            )
            .isTrue()
    }

    @Test
    fun checkSupported_nonQueryableSurfaceConfig_featureCombinationQueryNotInvoked() {
        // Arrange: Setup resources with a FeatureCombinationQuery impl. tracking isSupported calls
        setupCamera()
        val latch = CountDownLatch(1)
        val supportedSurfaceCombination =
            SupportedSurfaceCombination(
                context,
                fakeCameraMetadata,
                mockEncoderProfilesAdapter,
                object : FeatureCombinationQuery {
                    override fun isSupported(sessionConfig: SessionConfig): Boolean {
                        latch.countDown()
                        return false
                    }
                },
            )

        val surfaceConfigList =
            listOf(
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.UHD),
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.UHD),
                SurfaceConfig.create(ConfigType.YUV, ConfigSize.UHD),
            )

        // Act: Check for a FCQ SurfaceConfig combination
        supportedSurfaceCombination.checkSupported(
            createFeatureSettings(requiresFeatureComboQuery = true),
            surfaceConfigList,
            surfaceConfigList.associateWith { DynamicRange.UNSPECIFIED },
            surfaceConfigList.toUseCaseConfigs(),
            (0 until surfaceConfigList.size).toList(),
        )

        // Assert: Waits a small time for latch update in isSupported call just in case any code
        // flow happens asynchronously in future
        assertThat(latch.await(100, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun getSuggestedStreamSpecs_allFeaturesSupported_fcqInvokedWithCorrectParameters() {
        // Arrange: Preview + ImageCapture use cases with all FCQ features - HLG10, 60 FPS, Preview
        // Stabilization, and Ultra HDR
        val previewUseCase =
            createUseCase(
                CaptureType.PREVIEW,
                dynamicRange =
                    if (Build.VERSION.SDK_INT >= 33) {
                        HLG_10_BIT
                    } else {
                        DynamicRange.UNSPECIFIED
                    },
                targetFrameRate = Range(60, 60),
            )
        val imageCaptureUseCase = createUseCase(CaptureType.IMAGE_CAPTURE, imageFormat = JPEG_R)

        val useCasesOutputSizesMap =
            mapOf(
                previewUseCase to listOf(RESOLUTION_1080P),
                imageCaptureUseCase to listOf(RESOLUTION_1440P_16_9),
            )
        val useCaseExpectedResultMap =
            mapOf(previewUseCase to RESOLUTION_1080P, imageCaptureUseCase to RESOLUTION_1440P_16_9)

        // Act & assert that all features are supported
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            dynamicRangeProfiles = if (Build.VERSION.SDK_INT >= 33) HLG10_CONSTRAINED else null,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_DYNAMIC_RANGE_TEN_BIT),
            isPreviewStabilizationOn = Build.VERSION.SDK_INT >= 33,
            isFeatureComboInvocation = true,
            featureCombinationQuery = fakeFeatureCombinationQuery.apply { isSupported = true },
            maxFpsBySizeMap =
                mapOf(RESOLUTION_1080P to 60, RESOLUTION_1440P_16_9 to 60, RESOLUTION_UHD to 60),
        )

        // Assert: Correct params were passed every time FeatureCombinationQuery API was invoked

        // Same dynamic range should be resolved to all use cases, HLG_10 is not supported before
        // API 33
        val expectedDynamicRange = if (Build.VERSION.SDK_INT >= 33) HLG_10_BIT else SDR

        val expectedPreviewStabilization =
            if (Build.VERSION.SDK_INT >= 33) StabilizationMode.ON else StabilizationMode.UNSPECIFIED

        fakeFeatureCombinationQuery.queriedConfigs.forEach { sessionConfig ->
            // Verify surface parameters of each output config, each config dynamic range should be
            // resolved to the same HLG10
            assertThat(
                    sessionConfig.outputConfigs.map {
                        listOf(
                            it.surface.prescribedStreamFormat,
                            it.surface.prescribedSize,
                            it.surface.containerClass,
                            it.dynamicRange,
                        )
                    }
                )
                .containsExactly(
                    listOf( // Preview
                        PRIVATE,
                        RESOLUTION_1080P,
                        UseCaseType.PREVIEW.surfaceClass,
                        expectedDynamicRange,
                    ),
                    listOf( // ImageCapture
                        JPEG_R, // Verify Ultra HDR
                        RESOLUTION_1440P_16_9,
                        UseCaseType.IMAGE_CAPTURE.surfaceClass,
                        expectedDynamicRange,
                    ),
                )

            // Verify 60 FPS
            assertThat(sessionConfig.expectedFrameRateRange).isEqualTo(Range(60, 60))

            // Verify Preview Stabilization
            assertThat(sessionConfig.repeatingCaptureConfig.previewStabilizationMode)
                .isEqualTo(expectedPreviewStabilization)
        }
    }

    @Test
    fun getSuggestedStreamSpecs_hasManualSensor_skipsZeroMinFrameDurationStream() {
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 30))
        val useCasesOutputSizesMap =
            mapOf(
                useCase to
                    listOf(
                        Size(1000, 1000), // MaxFps = 0
                        Size(1920, 1080), // MaxFps = 35
                    )
            )
        val useCaseExpectedResultMap = mutableMapOf(useCase to Size(1920, 1080))

        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            capabilities = intArrayOf(REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR),
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = Range(30, 30),
        )
    }

    @Test
    fun getSuggestedStreamSpecs_noManualSensor_zeroMinFrameDurationIndicatesUnlimitedFps() {
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(30, 30))
        val useCasesOutputSizesMap =
            mapOf(
                useCase to
                    listOf(
                        Size(1000, 1000), // MaxFps = no limit
                        Size(1920, 1080), // MaxFps = 35
                    )
            )
        val useCaseExpectedResultMap = mutableMapOf(useCase to Size(1000, 1000))

        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            useCasesOutputSizesMap = useCasesOutputSizesMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = Range(30, 30),
        )
    }

    @Test
    fun getSuggestedStreamSpecs_canSelectMaxUpperBoundFps() {
        val useCase = createUseCase(CaptureType.PREVIEW, targetFrameRate = Range<Int>(15, 60))
        val useCaseExpectedResultMap = mutableMapOf(useCase to Size(800, 450))

        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            compareExpectedFps = Range(15, 60),
            deviceFPSRanges =
                mutableListOf<Range<Int>>()
                    .apply {
                        addAll(defaultFpsRanges)
                        add(Range.create(15, 30))
                        add(Range.create(15, 60))
                    }
                    .toTypedArray(),
        )
    }

    private fun createSupportedSurfaceCombinationWithSetup(
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    ): SupportedSurfaceCombination {
        setupCamera(hardwareLevel)
        return SupportedSurfaceCombination(
            context,
            fakeCameraMetadata,
            mockEncoderProfilesAdapter,
            NO_OP_FEATURE_COMBINATION_QUERY,
        )
    }

    private fun createFeatureSettings(
        isFeatureComboInvocation: Boolean = false,
        requiresFeatureComboQuery: Boolean = false,
        targetFpsRange: Range<Int> = FRAME_RATE_RANGE_UNSPECIFIED,
        isStrictFpsRequired: Boolean = false,
    ) =
        SupportedSurfaceCombination.FeatureSettings(
            CameraMode.DEFAULT,
            DynamicRange.BIT_DEPTH_8_BIT,
            isFeatureComboInvocation = isFeatureComboInvocation,
            requiresFeatureComboQuery = requiresFeatureComboQuery,
            targetFpsRange = targetFpsRange,
            isStrictFpsRequired = isStrictFpsRequired,
        )

    private fun setupCamera(
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        sensorOrientation: Int = sensorOrientation90,
        pixelArraySize: Size = landscapePixelArraySize,
        supportedSizes: Array<Size> = this.supportedSizes,
        supportedFormats: IntArray? = null,
        highResolutionSupportedSizes: Array<Size>? = null,
        supportedHighSpeedSizeAndFpsMap: Map<Size, List<Range<Int>>>? = null,
        maximumResolutionSupportedSizes: Array<Size>? = null,
        maximumResolutionHighResolutionSupportedSizes: Array<Size>? = null,
        dynamicRangeProfiles: DynamicRangeProfiles? = null,
        default10BitProfile: Long? = null,
        capabilities: IntArray? = null,
        cameraId: CameraId = CameraId.fromCamera1Id(0),
        maxFpsBySizeMap: Map<Size, Int> = emptyMap(),
        deviceFPSRanges: Array<Range<Int>> = defaultFpsRanges,
    ) {
        cameraFactory = FakeCameraFactory()
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.LENS_FACING,
            CameraCharacteristics.LENS_FACING_BACK,
        )
        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            hardwareLevel,
        )
        shadowCharacteristics.set(CameraCharacteristics.SENSOR_ORIENTATION, sensorOrientation)
        shadowCharacteristics.set(
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE,
            pixelArraySize,
        )
        if (capabilities != null) {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                capabilities,
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

        val characteristicsMap =
            mutableMapOf(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                    CameraCharacteristics.SENSOR_ORIENTATION to sensorOrientation,
                    CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to pixelArraySize,
                    CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to capabilities,
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap,
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES to deviceFPSRanges,
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
                    CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong(),
                )
            characteristicsMap[CameraCharacteristics.SCALER_AVAILABLE_STREAM_USE_CASES] = uc
        }

        val vs: IntArray =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intArrayOf(
                    CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                    CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                    CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION,
                )
            } else {
                intArrayOf(
                    CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                    CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON,
                )
            }
        characteristicsMap[CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES] = vs

        // set up FakeCafakeCameraMetadatameraMetadata
        fakeCameraMetadata =
            FakeCameraMetadata(cameraId = cameraId, characteristics = characteristicsMap)

        val cameraManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        (Shadow.extract<Any>(cameraManager) as ShadowCameraManager).addCamera(
            fakeCameraMetadata.camera.value,
            characteristics,
        )

        whenever(
                mockMap.getOutputSizes(
                    ArgumentMatchers.intThat { format ->
                        supportedFormats?.contains(format) != false
                    }
                )
            )
            .thenReturn(supportedSizes)
        // PRIVATE was supported since API level 23. Before that, the supported
        // output sizes need to be retrieved via SurfaceTexture.class.
        whenever(mockMap.getOutputSizes(SurfaceTexture::class.java)).thenReturn(supportedSizes)
        // This is setup for the test to determine RECORD size from StreamConfigurationMap
        whenever(mockMap.getOutputSizes(MediaRecorder::class.java)).thenReturn(supportedSizes)
        // This is setup for the test to determine output formats from StreamConfigurationMap
        whenever(mockMap.outputFormats).thenReturn(supportedFormats)
        // This is setup for high resolution output sizes
        highResolutionSupportedSizes?.let {
            if (Build.VERSION.SDK_INT >= 23) {
                whenever(mockMap.getHighResolutionOutputSizes(anyInt())).thenReturn(it)
            }
        }

        if (
            supportedHighSpeedSizeAndFpsMap != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        ) {
            // Mock highSpeedVideoSizes
            whenever(mockMap.highSpeedVideoSizes)
                .thenReturn(supportedHighSpeedSizeAndFpsMap.keys.toTypedArray())

            // Mock highSpeedVideoFpsRanges
            val allFpsRanges = supportedHighSpeedSizeAndFpsMap.values.flatten().distinct()
            whenever(mockMap.highSpeedVideoFpsRanges).thenReturn(allFpsRanges.toTypedArray())

            // Mock getHighSpeedVideoSizesFor
            allFpsRanges.forEach { fpsRange ->
                val sizesForRange =
                    supportedHighSpeedSizeAndFpsMap.entries
                        .filter { (_, fpsRanges) -> fpsRanges.contains(fpsRange) }
                        .map { it.key }
                        .sortedWith(CompareSizesByArea(false)) // Descending order
                        .toTypedArray()
                whenever(mockMap.getHighSpeedVideoSizesFor(fpsRange)).thenReturn(sizesForRange)
            }

            // Mock getHighSpeedVideoFpsRangesFor
            supportedHighSpeedSizeAndFpsMap.forEach { (size, fpsRanges) ->
                whenever(mockMap.getHighSpeedVideoFpsRangesFor(size))
                    .thenReturn(fpsRanges.toTypedArray())
            }
        }

        // setup to return different minimum frame durations depending on resolution
        // minimum frame durations were designated only for the purpose of testing
        // 20fps, size maximum
        mockMap.mockOutputMinFrameDuration(Size(4032, 3024), 50000000L)

        // 25fps, size record
        mockMap.mockOutputMinFrameDuration(Size(3840, 2160), 40000000L)

        // 30fps
        mockMap.mockOutputMinFrameDuration(Size(1920, 1440), 33333333L)

        // 30fps
        mockMap.mockOutputMinFrameDuration(Size(2560, 1440), 33333333L)

        // 35fps
        mockMap.mockOutputMinFrameDuration(Size(1920, 1080), 28571428L)

        // 40fps
        mockMap.mockOutputMinFrameDuration(Size(1280, 960), 25000000L)

        // 45fps, size preview/display
        mockMap.mockOutputMinFrameDuration(Size(1280, 720), 22222222L)

        // 50fps
        mockMap.mockOutputMinFrameDuration(Size(960, 544), 20000000L)

        // 60fps
        mockMap.mockOutputMinFrameDuration(Size(800, 450), 16666666L)

        // 60fps
        mockMap.mockOutputMinFrameDuration(Size(640, 480), 16666666L)

        maxFpsBySizeMap.forEach { (size, maxFps) ->
            // x FPS means 1 second for x frames, so min frame duration is (1e9 / x) ns
            mockMap.mockOutputMinFrameDuration(size, floor(1e9 / maxFps.toDouble()).toLong())
        }

        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)
        mockMaximumResolutionMap?.let {
            whenever(mockMaximumResolutionMap.getOutputSizes(anyInt()))
                .thenReturn(maximumResolutionSupportedSizes)
            whenever(mockMaximumResolutionMap.getOutputSizes(SurfaceTexture::class.java))
                .thenReturn(maximumResolutionSupportedSizes)
            if (Build.VERSION.SDK_INT >= 23) {
                whenever(mockMaximumResolutionMap.getHighResolutionOutputSizes(anyInt()))
                    .thenReturn(maximumResolutionHighResolutionSupportedSizes)
            }
            if (Build.VERSION.SDK_INT >= 31) {
                shadowCharacteristics.set(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION,
                    mockMaximumResolutionMap,
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
                        setOf(CameraBackendId("0"), CameraBackendId("2")),
                    ),
                cameraMetadataMap =
                    mapOf(FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to listOf(fakeCameraMetadata)),
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
                    _: Long,
                    _: StreamSpecsCalculator ->
                    cameraFactory!!
                }
                .build()
        val cameraX: CameraX =
            try {
                CameraXUtil.getOrCreateInstance(context) { cameraXConfig }.get()
            } catch (_: ExecutionException) {
                throw IllegalStateException("Unable to initialize CameraX for test.")
            } catch (_: InterruptedException) {
                throw IllegalStateException("Unable to initialize CameraX for test.")
            }
        useCaseConfigFactory = cameraX.defaultConfigFactory
    }

    private fun createUseCase(
        captureType: CaptureType,
        sessionType: Int? = null,
        targetFrameRate: Range<Int>? = null,
        isStrictFpsRequired: Boolean = false,
        dynamicRange: DynamicRange = DynamicRange.UNSPECIFIED,
        surfaceOccupancyPriority: Int? = null,
        streamUseCase: StreamUseCase? = null,
    ): UseCase {
        return createUseCase(
            captureType,
            sessionType,
            targetFrameRate,
            isStrictFpsRequired,
            dynamicRange,
            surfaceOccupancyPriority = surfaceOccupancyPriority,
            streamUseCase = streamUseCase,
            streamUseCaseOverride = null,
        )
    }

    private fun StreamConfigurationMap.mockOutputMinFrameDuration(size: Size, duration: Long) {
        Mockito.`when`(getOutputMinFrameDuration(anyInt(), ArgumentMatchers.eq(size)))
            .thenReturn(duration)
        Mockito.`when`(getOutputMinFrameDuration(any<Class<*>>(), ArgumentMatchers.eq(size)))
            .thenReturn(duration)
    }

    private fun createUseCase(
        captureType: CaptureType,
        sessionType: Int? = null,
        targetFrameRate: Range<Int>? = null,
        isStrictFpsRequired: Boolean? = null,
        dynamicRange: DynamicRange? = DynamicRange.UNSPECIFIED,
        imageFormat: Int? = null,
        surfaceOccupancyPriority: Int? = null,
        streamUseCase: StreamUseCase? = null,
        streamUseCaseOverride: StreamUseCase? = null,
    ): UseCase {
        val builder =
            FakeUseCaseConfig.Builder(
                FakeUseCaseConfigFactory()
                    .getConfig(captureType, ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY),
                captureType,
                imageFormat
                    ?: when (captureType) {
                        CaptureType.IMAGE_CAPTURE -> JPEG
                        CaptureType.IMAGE_ANALYSIS -> ImageFormat.YUV_420_888
                        else -> ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
                    },
            )
        sessionType?.let {
            builder.mutableConfig.insertOption(UseCaseConfig.OPTION_SESSION_TYPE, it)
        }
        targetFrameRate?.let {
            builder.mutableConfig.insertOption(UseCaseConfig.OPTION_TARGET_FRAME_RATE, it)
        }
        isStrictFpsRequired?.let {
            builder.mutableConfig.insertOption(
                UseCaseConfig.OPTION_IS_STRICT_FRAME_RATE_REQUIRED,
                it,
            )
        }
        streamUseCase?.let { builder.setStreamUseCase(it) }
        builder.mutableConfig.insertOption(
            ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE,
            dynamicRange,
        )
        streamUseCaseOverride?.let {
            builder.mutableConfig.insertOption(streamUseCaseOption, it.value)
        }

        surfaceOccupancyPriority?.let { builder.setSurfaceOccupancyPriority(it) }

        return builder.build()
    }

    private fun createRawUseCase(): UseCase {
        val builder = FakeUseCaseConfig.Builder()
        builder.mutableConfig.insertOption(
            UseCaseConfig.OPTION_INPUT_FORMAT,
            ImageFormat.RAW_SENSOR,
        )
        return builder.build()
    }

    private fun List<SurfaceConfig>.toUseCaseConfigs(): List<UseCaseConfig<*>> {
        return map {
            createUseCase(
                    when (it.configType) {
                        ConfigType.PRIV -> CaptureType.PREVIEW
                        ConfigType.YUV -> CaptureType.IMAGE_ANALYSIS
                        ConfigType.JPEG -> CaptureType.IMAGE_CAPTURE
                        ConfigType.JPEG_R -> CaptureType.IMAGE_CAPTURE
                        ConfigType.RAW -> CaptureType.IMAGE_CAPTURE
                    }
                )
                .currentConfig
        }
    }

    private fun StreamSpec.hasStreamUseCase(): Boolean =
        implementationOptions?.containsOption(StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION)
            ?: false

    private fun StreamSpec.getStreamUseCase(): Long? =
        implementationOptions?.retrieveOption(
            StreamUseCaseUtil.STREAM_USE_CASE_STREAM_SPEC_OPTION,
            null,
        )
}
