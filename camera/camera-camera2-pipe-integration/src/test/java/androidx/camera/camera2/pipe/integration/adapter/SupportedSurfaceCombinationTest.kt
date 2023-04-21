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
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
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
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getConcurrentSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getFullSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLegacySupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getRAWSupportedCombinationList
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.UseCase
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.AttachedSurfaceInfo
import androidx.camera.core.impl.CameraMode
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_1440P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.EncoderProfilesUtil
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraCoordinator
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.fakes.FakeUseCaseConfig
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
    private val sensorOrientation90 = 90
    private val landscapePixelArraySize = Size(4032, 3024)
    private val displaySize = Size(720, 1280)
    private val vgaSize = Size(640, 480)
    private val previewSize = Size(1280, 720)
    private val recordSize = Size(3840, 2160)
    private val maximumSize = Size(4032, 3024)
    private val legacyVideoMaximumVideoSize = Size(1920, 1080)
    private val mod16Size = Size(960, 544)
    private val profileUhd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(
        recordSize.width, recordSize.height
    )
    private val profileFhd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(
        1920, 1080
    )
    private val profileHd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(
        previewSize.width, previewSize.height
    )
    private val profileSd = EncoderProfilesUtil.createFakeEncoderProfilesProxy(
        vgaSize.width, vgaSize.height
    )
    private val supportedSizes = arrayOf(
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
    private val highResolutionSupportedSizes = arrayOf(
        Size(6000, 4500), // 4:3
        Size(6000, 3375), // 16:9
    )
    private val ultraHighMaximumSize = Size(8000, 6000)
    private val maximumResolutionSupportedSizes = arrayOf(
        Size(7200, 5400), // 4:3
        Size(7200, 4050), // 16:9
    )
    private val maximumResolutionHighResolutionSupportedSizes = arrayOf(
        Size(8000, 6000)
    )
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
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(
            displaySize
                .height
        )
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLegacySurfaceCombinationSubListSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLegacySupportedCombinationList()
        val isSupported = isAllSubConfigListSupported(
            CameraMode.DEFAULT, supportedSurfaceCombination, combinationList
        )
        assertThat(isSupported).isTrue()
    }

    @Test
    fun checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSubListSupportedInLimited3Device() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLimitedSupportedCombinationList()
        val isSupported = isAllSubConfigListSupported(
            CameraMode.DEFAULT, supportedSurfaceCombination, combinationList
        )
        assertThat(isSupported).isTrue()
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSubListSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getFullSupportedCombinationList()
        val isSupported = isAllSubConfigListSupported(
            CameraMode.DEFAULT, supportedSurfaceCombination, combinationList
        )
        assertThat(isSupported).isTrue()
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getRAWSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationSupportedInLevel3Device() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLevel3SupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.DEFAULT, combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationSubListSupportedInLevel3Device() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLevel3SupportedCombinationList()
        val isSupported = isAllSubConfigListSupported(
            CameraMode.DEFAULT, supportedSurfaceCombination, combinationList
        )
        assertThat(isSupported).isTrue()
    }

    @Test
    fun checkConcurrentSurfaceCombinationSupportedInConcurrentCameraMode() {
        Shadows.shadowOf(context.packageManager).setSystemFeature(
            PackageManager.FEATURE_CAMERA_CONCURRENT, true
        )
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getConcurrentSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(
                    CameraMode.CONCURRENT_CAMERA, combination.surfaceConfigList
                )
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkConcurrentSurfaceCombinationSubListSupportedInConcurrentCameraMode() {
        Shadows.shadowOf(context.packageManager).setSystemFeature(
            PackageManager.FEATURE_CAMERA_CONCURRENT, true
        )
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getConcurrentSupportedCombinationList()
        val isSupported = isAllSubConfigListSupported(
            CameraMode.CONCURRENT_CAMERA, supportedSurfaceCombination, combinationList
        )
        assertThat(isSupported).isTrue()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun checkUltraHighResolutionSurfaceCombinationSupportedInUltraHighCameraMode() {
        setupCamera(
            maximumResolutionSupportedSizes = maximumResolutionSupportedSizes,
            maximumResolutionHighResolutionSupportedSizes =
            maximumResolutionHighResolutionSupportedSizes,
            capabilities = intArrayOf(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList().forEach {
            assertThat(
                supportedSurfaceCombination.checkSupported(
                    CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA, it.surfaceConfigList
                )
            ).isTrue()
        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun checkUltraHighResolutionSurfaceCombinationSubListSupportedInUltraHighCameraMode() {
        setupCamera(
            maximumResolutionSupportedSizes = maximumResolutionSupportedSizes,
            maximumResolutionHighResolutionSupportedSizes =
            maximumResolutionHighResolutionSupportedSizes,
            capabilities = intArrayOf(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        GuaranteedConfigurationsUtil.getUltraHighResolutionSupportedCombinationList().also {
            assertThat(
                isAllSubConfigListSupported(
                    CameraMode.ULTRA_HIGH_RESOLUTION_CAMERA, supportedSurfaceCombination, it
                )
            ).isTrue()
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.YUV_420_888, vgaSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.YUV_420_888, previewSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.YUV_420_888, recordSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.YUV_420_888, maximumSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.YUV, SurfaceConfig.ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGAnalysisSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.JPEG, vgaSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGPreviewSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.JPEG, previewSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGRecordSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.JPEG, recordSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGMaximumSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.DEFAULT,
            ImageFormat.JPEG, maximumSize
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.CONCURRENT_CAMERA,
            ImageFormat.PRIVATE, SizeUtil.RESOLUTION_720P
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.CONCURRENT_CAMERA,
            ImageFormat.YUV_420_888, SizeUtil.RESOLUTION_720P
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.CONCURRENT_CAMERA,
            ImageFormat.JPEG, SizeUtil.RESOLUTION_720P
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.CONCURRENT_CAMERA,
            ImageFormat.PRIVATE, RESOLUTION_1440P
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.CONCURRENT_CAMERA,
            ImageFormat.YUV_420_888, RESOLUTION_1440P
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            CameraMode.CONCURRENT_CAMERA,
            ImageFormat.JPEG, RESOLUTION_1440P
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
            capabilities = intArrayOf(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        assertThat(
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.PRIVATE, ultraHighMaximumSize
            )
        ).isEqualTo(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.ULTRA_MAXIMUM
            )
        )
        assertThat(
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.YUV_420_888, ultraHighMaximumSize
            )
        ).isEqualTo(
            SurfaceConfig.create(
                SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.ULTRA_MAXIMUM
            )
        )
        assertThat(
            supportedSurfaceCombination.transformSurfaceConfig(
                CameraMode.DEFAULT,
                ImageFormat.JPEG, ultraHighMaximumSize
            )
        ).isEqualTo(
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

    /**
     * PRIV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSize_singlePrivStream_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE)
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * JPEG/MAXIMUM
     */
    @Test
    fun canSelectCorrectSize_singleJpegStream_inLegacyDevice() {
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(jpegUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSize_singleYuvStream_inLegacyDevice() {
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS)
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * PRIV/PREVIEW + JPEG/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusJpeg_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW)
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            put(jpegUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * YUV/PREVIEW + JPEG/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusJpeg_inLegacyDevice() {
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            put(jpegUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * PRIV/PREVIEW + PRIV/PREVIEW
     */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inLegacyDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase1, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            put(privUseCase2, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * PRIV/PREVIEW + YUV/PREVIEW
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            put(yuvUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusJpeg_inLegacyDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            put(yuvUseCase, if (Build.VERSION.SDK_INT == 21) RESOLUTION_VGA else previewSize)
            put(jpegUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(useCaseExpectedResultMap)
    }

    /**
     * Unsupported PRIV + JPEG + PRIV for legacy level devices
     */
    @Test
    fun throwsException_unsupportedConfiguration_inLegacyDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val privUseCas2 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + PRIV/RECORD
     */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inLimitedDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCas2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase1, recordSize)
            put(privUseCas2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /**
     * PRIV/PREVIEW + YUV/RECORD
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLimitedDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, previewSize)
            put(yuvUseCase, recordSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /**
     * YUV/PREVIEW + YUV/RECORD
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase1, recordSize)
            put(yuvUseCase2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /**
     * PRIV/PREVIEW + PRIV/RECORD + JPEG/RECORD
     */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpeg_inLimitedDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase1, recordSize)
            put(privUseCase2, previewSize)
            put(jpegUseCase, recordSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /**
     * PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusJpeg_inLimitedDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, previewSize)
            put(yuvUseCase, recordSize)
            put(jpegUseCase, recordSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /**
     * YUV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusJpeg_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase1, previewSize)
            put(yuvUseCase2, previewSize)
            put(jpegUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
    }

    /**
     * Unsupported YUV + PRIV + YUV for limited level devices
     */
    @Test
    fun throwsException_unsupportedConfiguration_inLimitedDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + PRIV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inFullDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase1, maximumSize)
            put(privUseCase2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /**
     * PRIV/PREVIEW + YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inFullDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, previewSize)
            put(yuvUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /**
     * YUV/PREVIEW + YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inFullDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase1, maximumSize)
            put(yuvUseCase2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /**
     * PRIV/PREVIEW + PRIV/PREVIEW + JPEG/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpeg_inFullDevice() {
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(jpegUseCase, maximumSize)
            put(privUseCase1, previewSize)
            put(privUseCase2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /**
     * YUV/VGA + PRIV/PREVIEW + YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusPrivPlusYuv_inFullDevice() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, previewSize)
            put(yuvUseCase1, maximumSize)
            put(yuvUseCase2, RESOLUTION_VGA)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /**
     * YUV/VGA + YUV/PREVIEW + YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusYuv_inFullDevice() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase3 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase1, maximumSize)
            put(yuvUseCase2, previewSize)
            put(yuvUseCase3, RESOLUTION_VGA)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
    }

    /**
     * Unsupported PRIV + PRIV + YUV + RAW for full level devices
     */
    @Test
    fun throwsException_unsupportedConfiguration_inFullDevice() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + PRIV/VGA + YUV/MAXIMUM + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusYuvPlusRaw_inLevel3Device() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + PRIV/VGA + JPEG/MAXIMUM + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusJpegPlusRaw_inLevel3Device() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * Unsupported PRIV + YUV + YUV + RAW for level-3 devices
     */
    @Test
    fun throwsException_unsupportedConfiguration_inLevel3Device() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + PRIV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusPriv_inLimitedDevice_withBurstCapability() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase1, maximumSize)
            put(privUseCase2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            )
        )
    }

    /**
     * PRIV/PREVIEW + YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuv_inLimitedDevice_withBurstCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, previewSize)
            put(yuvUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            )
        )
    }

    /**
     * YUV/PREVIEW + YUV/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuv_inLimitedDevice_withBurstCapability() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(yuvUseCase1, maximumSize)
            put(yuvUseCase2, previewSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            )
        )
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Resolution selection tests for RAW-capability guaranteed configurations
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * RAW/MAX
     */
    @Test
    fun canSelectCorrectSizes_singleRawStream_inLimitedDevice_withRawCapability() {
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(rawUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /**
     * PRIV/PREVIEW + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
            put(privUseCase, previewSize)
            put(rawUseCase, maximumSize)
        }
        getSuggestedSpecsAndVerify(
            useCaseExpectedResultMap,
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
    }

    /**
     * PRIV/PREVIEW + PRIV/PREVIEW + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusPrivPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.VIDEO_CAPTURE) // PRIV
        val privUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + YUV/PREVIEW + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusYuvPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * YUV/PREVIEW + YUV/PREVIEW + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusYuvPlusRAW_inLimitedDevice_withRawCapability() {
        val yuvUseCase1 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val yuvUseCase2 = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * PRIV/PREVIEW + JPEG/MAXIMUM + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_privPlusJpegPlusRAW_inLimitedDevice_withRawCapability() {
        val privUseCase = createUseCase(UseCaseConfigFactory.CaptureType.PREVIEW) // PRIV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    /**
     * YUV/PREVIEW + JPEG/MAXIMUM + RAW/MAXIMUM
     */
    @Test
    fun canSelectCorrectSizes_yuvPlusJpegPlusRAW_inLimitedDevice_withRawCapability() {
        val yuvUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS) // YUV
        val jpegUseCase = createUseCase(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE) // JPEG
        val rawUseCase = createRawUseCase() // RAW
        val useCaseExpectedResultMap = mutableMapOf<UseCase, Size>().apply {
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

    private fun getSuggestedSpecsAndVerify(
        useCasesExpectedResultMap: Map<UseCase, Size>,
        attachedSurfaceInfoList: List<AttachedSurfaceInfo> = emptyList(),
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        capabilities: IntArray? = null,
        compareWithAtMost: Boolean = false
    ) {
        setupCamera(hardwareLevel = hardwareLevel, capabilities = capabilities)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        val useCaseConfigMap = getUseCaseToConfigMap(useCasesExpectedResultMap.keys.toList())
        val useCaseConfigToOutputSizesMap =
            getUseCaseConfigToOutputSizesMap(useCaseConfigMap.values.toList())
        val suggestedStreamSpecs = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            CameraMode.DEFAULT,
            attachedSurfaceInfoList,
            useCaseConfigToOutputSizesMap
        )

        useCasesExpectedResultMap.keys.forEach {
            val resultSize = suggestedStreamSpecs[useCaseConfigMap[it]]!!.resolution
            val expectedSize = useCasesExpectedResultMap[it]!!
            if (!compareWithAtMost) {
                assertThat(resultSize).isEqualTo(expectedSize)
            } else {
                assertThat(sizeIsAtMost(resultSize, expectedSize)).isTrue()
            }
        }
    }

    private fun getUseCaseToConfigMap(useCases: List<UseCase>): Map<UseCase, UseCaseConfig<*>> {
        val useCaseConfigMap = mutableMapOf<UseCase, UseCaseConfig<*>>().apply {
            useCases.forEach {
                put(it, it.currentConfig)
            }
        }
        return useCaseConfigMap
    }

    private fun getUseCaseConfigToOutputSizesMap(
        useCaseConfigs: List<UseCaseConfig<*>>
    ): Map<UseCaseConfig<*>, List<Size>> {
        val resultMap = mutableMapOf<UseCaseConfig<*>, List<Size>>().apply {
            useCaseConfigs.forEach {
                put(it, supportedSizes.toList())
            }
        }

        return resultMap
    }

    /**
     * Helper function that returns whether size is <= maxSize
     *
     */
    private fun sizeIsAtMost(size: Size, maxSize: Size): Boolean {
        return (size.height * size.width) <= (maxSize.height * maxSize.width)
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    //
    // Other tests
    //
    // //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    fun generateCorrectSurfaceDefinition() {
        Shadows.shadowOf(context.packageManager).setSystemFeature(
            PackageManager.FEATURE_CAMERA_CONCURRENT, true
        )
        setupCamera()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
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
        Shadows.shadowOf(context.packageManager).setSystemFeature(
            PackageManager.FEATURE_CAMERA_CONCURRENT, true
        )
        setupCamera(supportedSizes = arrayOf(RESOLUTION_VGA))
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.s720pSizeMap[imageFormat])
            .isEqualTo(RESOLUTION_VGA)
    }

    @Test
    fun correctS1440pSize_withSmallerOutputSizes() {
        Shadows.shadowOf(context.packageManager).setSystemFeature(
            PackageManager.FEATURE_CAMERA_CONCURRENT, true
        )
        setupCamera(supportedSizes = arrayOf(RESOLUTION_VGA))
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.s1440pSizeMap[imageFormat]).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    @Config(minSdk = 23)
    fun correctMaximumSize_withHighResolutionOutputSizes() {
        setupCamera(highResolutionSupportedSizes = highResolutionSupportedSizes)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.maximumSizeMap[imageFormat]).isEqualTo(
            highResolutionMaximumSize
        )
    }

    @Test
    @Config(minSdk = 32)
    fun correctUltraMaximumSize_withMaximumResolutionMap() {
        setupCamera(
            maximumResolutionSupportedSizes = maximumResolutionSupportedSizes,
            maximumResolutionHighResolutionSupportedSizes =
            maximumResolutionHighResolutionSupportedSizes,
            capabilities = intArrayOf(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageFormat = ImageFormat.JPEG
        val surfaceSizeDefinition =
            supportedSurfaceCombination.getUpdatedSurfaceSizeDefinitionByFormat(imageFormat)
        assertThat(surfaceSizeDefinition.ultraMaximumSizeMap[imageFormat]).isEqualTo(
            ultraHighMaximumSize
        )
    }

    @Test
    fun determineRecordSizeFromStreamConfigurationMap() {
        // Setup camera with non-integer camera Id
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            cameraId = CameraId("externalCameraId")
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        // Checks the determined RECORD size
        assertThat(
            supportedSurfaceCombination.surfaceSizeDefinition.recordSize
        ).isEqualTo(
            legacyVideoMaximumVideoSize
        )
    }

    @Test
    fun applyLegacyApi21QuirkCorrectly() {
        setupCamera()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val sortedSizeList = listOf(
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
        val expectedResultList = if (Build.VERSION.SDK_INT == 21) {
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
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val resultList =
            supportedSurfaceCombination.applyResolutionSelectionOrderRelatedWorkarounds(
                supportedSizes.toList(),
                ImageFormat.YUV_420_888
            )
        val expectedResultList = if (Build.VERSION.SDK_INT in 21..26) {
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

    private fun setupCamera(
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        sensorOrientation: Int = sensorOrientation90,
        pixelArraySize: Size = landscapePixelArraySize,
        supportedSizes: Array<Size> = this.supportedSizes,
        highResolutionSupportedSizes: Array<Size>? = null,
        maximumResolutionSupportedSizes: Array<Size>? = null,
        maximumResolutionHighResolutionSupportedSizes: Array<Size>? = null,
        capabilities: IntArray? = null,
        cameraId: CameraId = CameraId.fromCamera1Id(0)
    ) {
        cameraFactory = FakeCameraFactory()
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK
        )
        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, hardwareLevel
        )
        shadowCharacteristics.set(CameraCharacteristics.SENSOR_ORIENTATION, sensorOrientation)
        shadowCharacteristics.set(
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE,
            pixelArraySize
        )
        if (capabilities != null) {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities
            )
        }

        val mockMap: StreamConfigurationMap = mock()
        val mockMaximumResolutionMap: StreamConfigurationMap? =
            if (maximumResolutionSupportedSizes != null ||
                maximumResolutionHighResolutionSupportedSizes != null
            ) {
                mock()
            } else {
                null
            }

        val characteristicsMap = mutableMapOf(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
            CameraCharacteristics.SENSOR_ORIENTATION to sensorOrientation,
            CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to pixelArraySize,
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to capabilities,
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap,
        ).also { characteristicsMap ->
            mockMaximumResolutionMap?.let {
                characteristicsMap[
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION] =
                    mockMaximumResolutionMap
            }
        }

        // set up FakeCafakeCameraMetadatameraMetadata
        fakeCameraMetadata = FakeCameraMetadata(
            cameraId = cameraId,
            characteristics = characteristicsMap
        )

        val cameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        (Shadow.extract<Any>(cameraManager) as ShadowCameraManager)
            .addCamera(fakeCameraMetadata.camera.value, characteristics)

        whenever(mockMap.getOutputSizes(ArgumentMatchers.anyInt())).thenReturn(supportedSizes)
        // ImageFormat.PRIVATE was supported since API level 23. Before that, the supported
        // output sizes need to be retrieved via SurfaceTexture.class.
        whenever(
            mockMap.getOutputSizes(
                SurfaceTexture::class.java
            )
        ).thenReturn(supportedSizes)
        // This is setup for the test to determine RECORD size from StreamConfigurationMap
        whenever(
            mockMap.getOutputSizes(
                MediaRecorder::class.java
            )
        ).thenReturn(supportedSizes)
        // This is setup for high resolution output sizes
        highResolutionSupportedSizes?.let {
            whenever(mockMap.getHighResolutionOutputSizes(ArgumentMatchers.anyInt())).thenReturn(it)
        }
        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)
        mockMaximumResolutionMap?.let {
            whenever(mockMaximumResolutionMap.getOutputSizes(ArgumentMatchers.anyInt()))
                .thenReturn(maximumResolutionSupportedSizes)
            whenever(mockMaximumResolutionMap.getOutputSizes(SurfaceTexture::class.java))
                .thenReturn(maximumResolutionSupportedSizes)
            whenever(
                mockMaximumResolutionMap.getHighResolutionOutputSizes(
                    ArgumentMatchers.anyInt()
                )
            ).thenReturn(
                maximumResolutionHighResolutionSupportedSizes
            )
            shadowCharacteristics.set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION,
                mockMaximumResolutionMap
            )
        }
        @LensFacing val lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
            CameraCharacteristics.LENS_FACING_BACK
        )
        val cameraInfo = FakeCameraInfoInternal(fakeCameraMetadata.camera.value)
        cameraInfo.encoderProfilesProvider = FakeEncoderProfilesProvider.Builder()
            .add(QUALITY_2160P, profileUhd)
            .add(QUALITY_1080P, profileFhd)
            .add(QUALITY_720P, profileHd)
            .add(QUALITY_480P, profileSd)
            .build()
        cameraFactory!!.insertCamera(
            lensFacingEnum, fakeCameraMetadata.camera.value
        ) { FakeCamera(fakeCameraMetadata.camera.value, null, cameraInfo) }

        initCameraX(fakeCameraMetadata.camera.value)
    }

    private fun initCameraX(cameraId: String) {
        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = FakeCameraBackend.FAKE_CAMERA_BACKEND_ID,
                cameraMetadataMap =
                mapOf(FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to listOf(fakeCameraMetadata))
            )
        whenever(mockCameraAppComponent.getCameraDevices())
            .thenReturn(fakeCameraDevices)
        cameraFactory!!.cameraManager = mockCameraAppComponent
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            CameraPipeConfig.defaultConfig()
        )
            .setDeviceSurfaceManagerProvider { context: Context?, _: Any?, _: Set<String?>? ->
                CameraSurfaceAdapter(
                    context!!,
                    mockCameraAppComponent, setOf(cameraId)
                )
            }
            .setCameraFactoryProvider { _: Context?,
                _: CameraThreadConfig?,
                _: CameraSelector?
                ->
                cameraFactory!!
            }
            .build()
        val cameraX: CameraX = try {
            CameraXUtil.getOrCreateInstance(context) { cameraXConfig }.get()
        } catch (e: ExecutionException) {
            throw IllegalStateException("Unable to initialize CameraX for test.")
        } catch (e: InterruptedException) {
            throw IllegalStateException("Unable to initialize CameraX for test.")
        }
        useCaseConfigFactory = cameraX.defaultConfigFactory
    }

    private fun isAllSubConfigListSupported(
        cameraMode: Int,
        supportedSurfaceCombination: SupportedSurfaceCombination,
        combinationList: List<SurfaceCombination>
    ): Boolean {
        for (combination in combinationList) {
            val configList = combination.surfaceConfigList
            val length = configList.size
            if (length <= 1) {
                continue
            }
            for (index in 0 until length) {
                val subConfigurationList: MutableList<SurfaceConfig> = ArrayList(configList)
                subConfigurationList.removeAt(index)
                val isSupported = supportedSurfaceCombination.checkSupported(
                    cameraMode, subConfigurationList
                )
                if (!isSupported) {
                    return false
                }
            }
        }
        return true
    }

    private fun createUseCase(
        captureType: UseCaseConfigFactory.CaptureType,
        targetFrameRate: Range<Int>? = null
    ): UseCase {
        val builder = FakeUseCaseConfig.Builder(
            captureType, when (captureType) {
                UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE -> ImageFormat.JPEG
                UseCaseConfigFactory.CaptureType.IMAGE_ANALYSIS -> ImageFormat.YUV_420_888
                else -> ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
            }
        )
        targetFrameRate?.let {
            builder.mutableConfig.insertOption(UseCaseConfig.OPTION_TARGET_FRAME_RATE, it)
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