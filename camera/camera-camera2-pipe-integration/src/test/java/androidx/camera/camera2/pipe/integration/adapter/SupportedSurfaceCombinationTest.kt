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
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.CamcorderProfile.QUALITY_1080P
import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_480P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.MediaRecorder
import android.os.Build
import android.util.Pair
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getFullSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLegacySupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList
import androidx.camera.camera2.pipe.integration.adapter.GuaranteedConfigurationsUtil.getRAWSupportedCombinationList
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.Configs
import androidx.camera.testing.EncoderProfilesUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeEncoderProfilesProvider
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaSpec
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoOutput
import androidx.camera.video.VideoOutput.SourceState
import androidx.camera.video.VideoSpec
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.junit.After
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

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedSurfaceCombinationTest {
    private val sensorOrientation0 = 0
    private val sensorOrientation90 = 90
    private val aspectRatio43 = Rational(4, 3)
    private val aspectRatio169 = Rational(16, 9)
    private val landscapePixelArraySize = Size(4032, 3024)
    private val portraitPixelArraySize = Size(3024, 4032)
    private val displaySize = Size(720, 1280)
    private val vgaSize = Size(640, 480)
    private val vgaSizeStreamSpec = StreamSpec.builder(vgaSize).build()
    private val previewSize = Size(1280, 720)
    private val previewSizeStreamSpec = StreamSpec.builder(previewSize).build()
    private val recordSize = Size(3840, 2160)
    private val recordSizeStreamSpec = StreamSpec.builder(recordSize).build()
    private val maximumSize = Size(4032, 3024)
    private val maximumSizeStreamSpec = StreamSpec.builder(maximumSize).build()
    private val legacyVideoMaximumVideoSize = Size(1920, 1080)
    private val mod16Size = Size(960, 544)
    private val mod16SizeStreamSpec = StreamSpec.builder(mod16Size).build()
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
        Size(320, 240), // 4:3
        Size(320, 180), // 16:9
        Size(256, 144) // 16:9 For checkSmallSizesAreFilteredOut test.
    )
    private val context = InstrumentationRegistry.getInstrumentation().context
    private var cameraFactory: FakeCameraFactory? = null
    private var useCaseConfigFactory: UseCaseConfigFactory = mock()
    private lateinit var fakeCameraMetadata: FakeCameraMetadata

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
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
        val isSupported = isAllSubConfigListSupported(supportedSurfaceCombination, combinationList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
        val isSupported = isAllSubConfigListSupported(supportedSurfaceCombination, combinationList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
        val isSupported = isAllSubConfigListSupported(supportedSurfaceCombination, combinationList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
            assertThat(isSupported).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLimitedSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkLegacySurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getLegacySupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getFullSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
            assertThat(isSupported).isTrue()
        }
    }

    @Test
    fun checkRawSurfaceCombinationSupportedInRawDevice() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val combinationList = getRAWSupportedCombinationList()
        for (combination in combinationList) {
            val isSupported =
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
                supportedSurfaceCombination.checkSupported(combination.surfaceConfigList)
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
        val isSupported = isAllSubConfigListSupported(supportedSurfaceCombination, combinationList)
        assertThat(isSupported).isTrue()
    }

    @Test
    fun checkTargetAspectRatio() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val fakeUseCase = FakeUseCaseConfig.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(fakeUseCase)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            ArrayList(useCaseToConfigMap.values)
        )
        val selectedStreamSpec = suggestedStreamSpecMap[useCaseToConfigMap[fakeUseCase]]!!
        val resultAspectRatio = Rational(
            selectedStreamSpec.resolution.width,
            selectedStreamSpec.resolution.height
        )
        assertThat(resultAspectRatio).isEqualTo(aspectRatio169)
    }

    @Test
    fun checkResolutionForMixedUseCase_AfterBindToLifecycle() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)

        // The test case make sure the selected result is expected after the regular flow.
        val targetAspectRatio = aspectRatio169
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(mock())
        )
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val cameraUseCaseAdapter = CameraUtil
            .createCameraUseCaseAdapter(
                context,
                CameraSelector.DEFAULT_BACK_CAMERA
            )
        cameraUseCaseAdapter.addUseCases(listOf(preview, imageCapture, imageAnalysis))
        val previewResolution = preview.attachedSurfaceResolution!!
        val previewRatio = Rational(
            previewResolution.width,
            previewResolution.height
        )
        val imageCaptureResolution = preview.attachedSurfaceResolution
        val imageCaptureRatio = Rational(
            imageCaptureResolution!!.width,
            imageCaptureResolution.height
        )
        val imageAnalysisResolution = preview.attachedSurfaceResolution
        val imageAnalysisRatio = Rational(
            imageAnalysisResolution!!.width,
            imageAnalysisResolution.height
        )

        // Checks no correction is needed.
        assertThat(previewRatio).isEqualTo(targetAspectRatio)
        assertThat(imageCaptureRatio).isEqualTo(targetAspectRatio)
        assertThat(imageAnalysisRatio).isEqualTo(targetAspectRatio)
    }

    @Test
    fun checkDefaultAspectRatioAndResolutionForMixedUseCase() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(mock())
        )
        val imageCapture = ImageCapture.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()

        // Preview/ImageCapture/ImageAnalysis' default config settings that will be applied after
        // bound to lifecycle. Calling bindToLifecycle here to make sure sizes matching to
        // default aspect ratio will be selected.
        val cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(
            context,
            CameraSelector.DEFAULT_BACK_CAMERA
        )
        cameraUseCaseAdapter.addUseCases(
            listOf(
                preview,
                imageCapture, imageAnalysis
            )
        )
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(preview)
        useCases.add(imageCapture)
        useCases.add(imageAnalysis)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            ArrayList(useCaseToConfigMap.values)
        )
        val previewSize = suggestedStreamSpecMap[useCaseToConfigMap[preview]]!!.resolution
        val imageCaptureSize = suggestedStreamSpecMap[useCaseToConfigMap[imageCapture]]!!.resolution
        val imageAnalysisSize =
            suggestedStreamSpecMap[useCaseToConfigMap[imageAnalysis]]!!.resolution

        val previewAspectRatio = Rational(
            previewSize.width,
            previewSize.height
        )

        val imageCaptureAspectRatio = Rational(
            imageCaptureSize.width,
            imageCaptureSize.height
        )

        val imageAnalysisAspectRatio = Rational(
            imageAnalysisSize.width,
            imageAnalysisSize.height
        )

        // Checks the default aspect ratio.
        assertThat(previewAspectRatio).isEqualTo(aspectRatio43)
        assertThat(imageCaptureAspectRatio).isEqualTo(aspectRatio43)
        assertThat(imageAnalysisAspectRatio).isEqualTo(aspectRatio43)

        // Checks the default resolution.
        assertThat(imageAnalysisSize).isEqualTo(vgaSize)
    }

    @Test
    fun checkSmallSizesAreFilteredOutByDefaultSize480p() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        /* This test case is for b/139018208 that get small resolution 144x256 with below
        conditions:
        1. The target aspect ratio is set to the screen size 1080 x 2220 (9:18.5).
        2. The camera doesn't provide any 9:18.5 resolution and the size 144x256(9:16)
         is considered the 9:18.5 mod16 version.
        3. There is no other bigger resolution matched the target aspect ratio.
        */
        val displayWidth = 1080
        val displayHeight = 2220
        val preview = Preview.Builder()
            .setTargetResolution(Size(displayHeight, displayWidth))
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            ArrayList(useCaseToConfigMap.values)
        )

        // Checks the preconditions.
        val preconditionSize = Size(256, 144)
        val targetRatio = Rational(displayHeight, displayWidth)
        val sizeList = ArrayList(listOf(*supportedSizes))
        assertThat(sizeList).contains(preconditionSize)
        for (s in supportedSizes) {
            val supportedRational = Rational(s.width, s.height)
            assertThat(supportedRational).isNotEqualTo(targetRatio)
        }

        // Checks the mechanism has filtered out the sizes which are smaller than default size
        // 480p.
        val previewSize = suggestedStreamSpecMap[useCaseToConfigMap[preview]]
        assertThat(previewSize).isNotEqualTo(preconditionSize)
    }

    @Test
    fun checkAspectRatioMatchedSizeCanBeSelected() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        // Sets each of mSupportedSizes as target resolution and also sets target rotation as
        // Surface.ROTATION to make it aligns the sensor direction and then exactly the same size
        // will be selected as the result. This test can also verify that size smaller than
        // 640x480 can be selected after set as target resolution.
        for (targetResolution in supportedSizes) {
            val imageCapture = ImageCapture.Builder().setTargetResolution(
                targetResolution
            ).setTargetRotation(Surface.ROTATION_90).build()
            val suggestedStreamSpecMap =
                supportedSurfaceCombination.getSuggestedStreamSpecifications(
                    emptyList(),
                    listOf(imageCapture.currentConfig)
                )
            assertThat(targetResolution).isEqualTo(
                suggestedStreamSpecMap[imageCapture.currentConfig]?.resolution
            )
        }
    }

    @Test
    fun checkCorrectAspectRatioNotMatchedSizeCanBeSelected() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        // Sets target resolution as 1280x640, all supported resolutions will be put into aspect
        // ratio not matched list. Then, 1280x720 will be the nearest matched one. Finally,
        // checks whether 1280x720 is selected or not.
        val targetResolution = Size(1280, 640)
        val imageCapture = ImageCapture.Builder().setTargetResolution(
            targetResolution
        ).setTargetRotation(Surface.ROTATION_90).build()
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            listOf(imageCapture.currentConfig)
        )
        assertThat(Size(1280, 720)).isEqualTo(
            suggestedStreamSpecMap[imageCapture.currentConfig]?.resolution
        )
    }

    @Test
    fun suggestedStreamSpecsForMixedUseCaseNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val videoCapture = createVideoCapture()
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(imageCapture)
        useCases.add(videoCapture)
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        assertThrows(IllegalArgumentException::class.java) {
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )
        }
    }

    @Test
    fun suggestedStreamSpecsForCustomizeResolutionsNotSupportedInLegacyDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        // Legacy camera only support (PRIV, PREVIEW) + (PRIV, PREVIEW)
        val quality = Quality.UHD
        val previewResolutionsPairs = listOf(
            Pair.create(ImageFormat.PRIVATE, arrayOf(previewSize))
        )
        val videoCapture = createVideoCapture(quality)
        val preview = Preview.Builder()
            .setSupportedResolutions(previewResolutionsPairs)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(videoCapture)
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        assertThrows(IllegalArgumentException::class.java) {
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )
        }
    }

    // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
    @Test
    fun suggestedStreamSpecsForMixedUseCaseInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val videoCapture = createVideoCapture(Quality.HIGHEST)
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(imageCapture)
        useCases.add(videoCapture)
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec> =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )

        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageCapture],
            recordSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[videoCapture],
            recordSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[preview],
            previewSizeStreamSpec
        )
    }

    @Test
    fun suggestedStreamSpecsInFullDevice_videoHasHigherPriorityThanImage() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val videoCapture = createVideoCapture(
            QualitySelector.from(
                Quality.UHD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.UHD)
            )
        )
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(imageCapture)
        useCases.add(videoCapture)
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec> =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )

        // There are two possible combinations in Full level device
        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD) => should be applied
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageCapture],
            recordSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[videoCapture],
            recordSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[preview],
            previewSizeStreamSpec
        )
    }

    @Test
    fun suggestedResInFullDevice_videoRecordSizeLowPriority_imageCanGetMaxSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3) // mMaximumSize(4032x3024) is 4:3
            .build()
        val videoCapture = createVideoCapture(
            QualitySelector.fromOrderedList(
                listOf(Quality.HD, Quality.FHD, Quality.UHD)
            )
        )
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(imageCapture)
        useCases.add(videoCapture)
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec> =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )

        // There are two possible combinations in Full level device
        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM) => should be applied
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageCapture],
            maximumSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[videoCapture],
            previewSizeStreamSpec
        ) // Quality.HD
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[preview],
            previewSizeStreamSpec
        )
    }

    @Test
    fun suggestedStreamSpecsWithSameSupportedListForDifferentUseCases() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        /* This test case is for b/132603284 that divide by zero issue crash happened in below
    conditions:
    1. There are duplicated two 1280x720 supported sizes for ImageCapture and Preview.
    2. supportedOutputSizes for ImageCapture and Preview in
    SupportedSurfaceCombination#getAllPossibleSizeArrangements are the same.
    */
        val imageCapture = ImageCapture.Builder()
            .setTargetResolution(displaySize)
            .build()
        val preview = Preview.Builder()
            .setTargetResolution(displaySize)
            .build()
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(displaySize)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(imageCapture)
        useCases.add(preview)
        useCases.add(imageAnalysis)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec> =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageCapture],
            previewSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[preview],
            previewSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageAnalysis],
            previewSizeStreamSpec
        )
    }

    @Test
    fun throwsWhenSetBothTargetResolutionAndAspectRatioForDifferentUseCases() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        var previewExceptionHappened = false
        val previewBuilder = Preview.Builder()
            .setTargetResolution(displaySize)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        try {
            previewBuilder.build()
        } catch (e: IllegalArgumentException) {
            previewExceptionHappened = true
        }
        assertThat(previewExceptionHappened).isTrue()
        var imageCaptureExceptionHappened = false
        val imageCaptureConfigBuilder = ImageCapture.Builder()
            .setTargetResolution(displaySize)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        try {
            imageCaptureConfigBuilder.build()
        } catch (e: IllegalArgumentException) {
            imageCaptureExceptionHappened = true
        }
        assertThat(imageCaptureExceptionHappened).isTrue()
        var imageAnalysisExceptionHappened = false
        val imageAnalysisConfigBuilder = ImageAnalysis.Builder()
            .setTargetResolution(displaySize)
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        try {
            imageAnalysisConfigBuilder.build()
        } catch (e: IllegalArgumentException) {
            imageAnalysisExceptionHappened = true
        }
        assertThat(imageAnalysisExceptionHappened).isTrue()
    }

    @Test
    fun suggestedStreamSpecsForCustomizedSupportedResolutions() {

        // Checks all suggested stream specs will have their resolutions become 640x480.
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val formatResolutionsPairList: MutableList<Pair<Int, Array<Size>>> = ArrayList()
        formatResolutionsPairList.add(Pair.create(ImageFormat.JPEG, arrayOf(vgaSize)))
        formatResolutionsPairList.add(
            Pair.create(ImageFormat.YUV_420_888, arrayOf(vgaSize))
        )
        formatResolutionsPairList.add(Pair.create(ImageFormat.PRIVATE, arrayOf(vgaSize)))

        // Sets use cases customized supported resolutions to 640x480 only.
        val imageCapture = ImageCapture.Builder()
            .setSupportedResolutions(formatResolutionsPairList)
            .build()
        val videoCapture = createVideoCapture(Quality.SD)
        val preview = Preview.Builder()
            .setSupportedResolutions(formatResolutionsPairList)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(imageCapture)
        useCases.add(videoCapture)
        useCases.add(preview)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec> =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )

        // Checks all suggested stream specs will have their resolutions become 640x480.
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageCapture],
            vgaSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[videoCapture],
            vgaSizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[preview],
            vgaSizeStreamSpec
        )
    }

    @Test
    fun transformSurfaceConfigWithYUVAnalysisSize() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
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
            ImageFormat.JPEG, maximumSize
        )
        val expectedSurfaceConfig =
            SurfaceConfig.create(SurfaceConfig.ConfigType.JPEG, SurfaceConfig.ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun maximumSizeForImageFormat() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val maximumYUVSize =
            supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.YUV_420_888)
        assertThat(maximumYUVSize).isEqualTo(maximumSize)
        val maximumJPEGSize =
            supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG)
        assertThat(maximumJPEGSize).isEqualTo(maximumSize)
    }

    @Test
    fun isAspectRatioMatchWithSupportedMod16Resolution() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setDefaultResolution(mod16Size)
            .build()
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setDefaultResolution(mod16Size)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(preview)
        useCases.add(imageCapture)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap: Map<UseCaseConfig<*>, StreamSpec> =
            supportedSurfaceCombination.getSuggestedStreamSpecifications(
                emptyList(),
                ArrayList(useCaseToConfigMap.values)
            )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[preview],
            mod16SizeStreamSpec
        )
        assertThat(suggestedStreamSpecMap).containsEntry(
            useCaseToConfigMap[imageCapture],
            mod16SizeStreamSpec
        )
    }

    @Test
    fun sortByCompareSizesByArea_canSortSizesCorrectly() {
        val sizes = arrayOfNulls<Size>(supportedSizes.size)

        // Generates a unsorted array from mSupportedSizes.
        val centerIndex = supportedSizes.size / 2
        // Puts 2nd half sizes in the front
        if (supportedSizes.size - centerIndex >= 0) {
            System.arraycopy(
                supportedSizes,
                centerIndex, sizes, 0,
                supportedSizes.size - centerIndex
            )
        }
        // Puts 1st half sizes inversely in the tail
        for (j in centerIndex - 1 downTo 0) {
            sizes[supportedSizes.size - j - 1] = supportedSizes[j]
        }

        // The testing sizes array will be equal to mSupportedSizes after sorting.
        Arrays.sort(sizes, CompareSizesByArea(true))
        assertThat(listOf(*sizes)).isEqualTo(listOf(*supportedSizes))
    }

    @Test
    fun supportedOutputSizes_noConfigSettings() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. No any aspect ratio related setting. The returned sizes list will be sorted in
        // descending order.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(4032, 3024),
            Size(3840, 2160),
            Size(1920, 1440),
            Size(1920, 1080),
            Size(1280, 960),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_aspectRatio4x3() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 4/3 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(
                640,
                480
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_aspectRatio16x9() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetAspectRatio(
            AspectRatio.RATIO_16_9
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(
                800,
                450
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_targetResolution1080x1920InRotation0() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            Size(1080, 1920)
        ).build()

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will be calibrated by default target rotation 0 degree. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Therefore, sizes of aspect ratio
        // 16/9 will be in front of the returned sizes list and the list is sorted in descending
        // order. Other items will be put in the following that are sorted by aspect ratio delta
        // and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(
                800,
                450
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_targetResolutionLargerThan640x480() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetRotation(
            Surface.ROTATION_90
        ).setTargetResolution(Size(1280, 960)).build()

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Target resolution larger than 640x480 won't overwrite
        // minimum size setting. Sizes smaller than 640x480 will be removed. The auto-resolution
        // mechanism will try to select the sizes which aspect ratio is nearest to the aspect
        // ratio of target resolution in priority. Therefore, sizes of aspect ratio 4/3 will be
        // in front of the returned sizes list and the list is sorted in descending order. Other
        // items will be put in the following that are sorted by aspect ratio delta and then area
        // size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1280, 960),
            Size(
                640,
                480
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_targetResolutionSmallerThan640x480() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetRotation(
            Surface.ROTATION_90
        ).setTargetResolution(Size(320, 240)).build()

        // Unnecessary big enough sizes will be removed from the result list. Minimum size will
        // be overwritten as 320x240. Sizes smaller than 320x240 will also be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Therefore, sizes of aspect ratio
        // 4/3 will be in front of the returned sizes list and the list is sorted in descending
        // order. Other items will be put in the following that are sorted by aspect ratio delta
        // and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(
                320,
                240
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_targetResolution1800x1440NearTo4x3() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetRotation(
            Surface.ROTATION_90
        ).setTargetResolution(Size(1800, 1440)).build()

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Size 1800x1440 is near to 4/3
        // therefore, sizes of aspect ratio 4/3 will be in front of the returned sizes list and
        // the list is sorted in descending order.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Sizes of 4/3 are near to aspect ratio of 1800/1440
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480), // Sizes of 16/9 are far to aspect ratio of 1800/1440
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_targetResolution1280x600NearTo16x9() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            Size(1280, 600)
        ).setTargetRotation(Surface.ROTATION_90).build()

        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Size 1280x600 is near to 16/9,
        // therefore, sizes of aspect ratio 16/9 will be in front of the returned sizes list and
        // the list is sorted in descending order.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Sizes of 16/9 are near to aspect ratio of 1280/600
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450), // Sizes of 4/3 are far to aspect ratio of 1280/600
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_maxResolution1280x720() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(Size(1280, 720)).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 or
        // larger than 1280x720 will be removed. The returned sizes list will be sorted in
        // descending order.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_setCustomOrderedResolutions() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val customOrderedResolutions = listOf(
            Size(640, 480),
            Size(1280, 720),
            Size(1920, 1080),
            Size(3840, 2160),
        )
        val useCase = FakeUseCaseConfig.Builder()
            .setCustomOrderedResolutions(customOrderedResolutions)
            .setTargetResolution(Size(1280, 720))
            .setMaxResolution(Size(1920, 1440))
            .setDefaultResolution(Size(1280, 720))
            .setSupportedResolutions(
                listOf(
                    Pair.create(
                        ImageFormat.PRIVATE, arrayOf(
                            Size(800, 450),
                            Size(640, 480),
                            Size(320, 240),
                        )
                    )
                )
            ).build()

        // Custom ordered resolutions is fully respected, meaning it will not be sorted or filtered
        // by other configurations such as max/default/target/supported resolutions.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        assertThat(resultList).containsExactlyElementsIn(customOrderedResolutions).inOrder()
    }

    @Test
    fun supportedOutputSizes_defaultResolution1280x720_noTargetResolution() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setDefaultResolution(
            Size(
                1280,
                720
            )
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. If there is no target resolution setting, it will be overwritten by default
        // resolution as 1280x720. Unnecessary big enough sizes will also be removed. The
        // returned sizes list will be sorted in descending order.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_defaultResolution1280x720_targetResolution1920x1080() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setDefaultResolution(
            Size(1280, 720)
        ).setTargetRotation(Surface.ROTATION_90).setTargetResolution(
            Size(1920, 1080)
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. There is target resolution 1920x1080, it won't be overwritten by default
        // resolution 1280x720. Unnecessary big enough sizes will also be removed. Sizes of
        // aspect ratio 16/9 will be in front of the returned sizes list and the list is sorted
        // in descending order.  Other items will be put in the following that are sorted by
        // aspect ratio delta and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(
                800,
                450
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_fallbackToGuaranteedResolution_whenNotFulfillConditions() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            Size(1920, 1080)
        ).setTargetRotation(Surface.ROTATION_90).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. There is target resolution 1920x1080 (16:9). Even 640x480 does not match 16:9
        // requirement, it will still be returned to use.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(Size(640, 480))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenMaxSizeSmallerThanDefaultMiniSize() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(
            Size(320, 240)
        ).build()

        // There is default minimum size 640x480 setting. Originally, sizes smaller than 640x480
        // will be removed. Due to maximal size bound is smaller than the default minimum size
        // bound and it is also smaller than 640x480, the default minimum size bound will be
        // ignored. Then, sizes equal to or smaller than 320x240 will be kept in the result list.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(320, 240),
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenMaxSizeSmallerThanSmallTargetResolution() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(
            Size(320, 180)
        ).setTargetResolution(Size(320, 240)).setTargetRotation(
            Surface.ROTATION_90
        ).build()

        // The default minimum size 640x480 will be overwritten by the target resolution 320x240.
        // Originally, sizes smaller than 320x240 will be removed. Due to maximal size bound is
        // smaller than the minimum size bound and it is also smaller than 640x480, the minimum
        // size bound will be ignored. Then, sizes equal to or smaller than 320x180 will be kept
        // in the result list.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenBothMaxAndTargetResolutionsSmallerThan640x480() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(
            Size(320, 240)
        ).setTargetResolution(Size(320, 180)).setTargetRotation(
            Surface.ROTATION_90
        ).build()

        // The default minimum size 640x480 will be overwritten by the target resolution 320x180.
        // Originally, sizes smaller than 320x180 will be removed. Due to maximal size bound is
        // smaller than the minimum size bound and it is also smaller than 640x480, the minimum
        // size bound will be ignored. Then, all sizes equal to or smaller than 320x320 will be
        // kept in the result list.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(320, 180),
            Size(256, 144),
            Size(320, 240)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenMaxSizeSmallerThanBigTargetResolution() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(
            Size(1920, 1080)
        ).setTargetResolution(Size(3840, 2160)).setTargetRotation(
            Surface.ROTATION_90
        ).build()

        // Because the target size 3840x2160 is larger than 640x480, it won't overwrite the
        // default minimum size 640x480. Sizes smaller than 640x480 will be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Therefore, sizes of aspect ratio
        // 16/9 will be in front of the returned sizes list and the list is sorted in descending
        // order. Other items will be put in the following that are sorted by aspect ratio delta
        // and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(
                800,
                450
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenNoSizeBetweenMaxSizeAndTargetResolution() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(
            Size(320, 200)
        ).setTargetResolution(Size(320, 190)).setTargetRotation(
            Surface.ROTATION_90
        ).build()

        // The default minimum size 640x480 will be overwritten by the target resolution 320x190.
        // Originally, sizes smaller than 320x190 will be removed. Due to there is no available
        // size between the maximal size and the minimum size bound and the maximal size is
        // smaller than 640x480, the default minimum size bound will be ignored. Then, sizes
        // equal to or smaller than 320x200 will be kept in the result list.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenTargetResolutionSmallerThanAnySize() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            Size(192, 144)
        ).setTargetRotation(Surface.ROTATION_90).build()

        // The default minimum size 640x480 will be overwritten by the target resolution 192x144.
        // Because 192x144 is smaller than any size in the supported list, no one will be
        // filtered out by it. The result list will only keep one big enough size of aspect ratio
        // 4:3 and 16:9.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(320, 240),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenMaxResolutionSmallerThanAnySize() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(
            Size(192, 144)
        ).build()

        // All sizes will be filtered out by the max resolution 192x144 setting and an
        // IllegalArgumentException will be thrown.
        assertThrows(IllegalArgumentException::class.java) {
            supportedSurfaceCombination.getSupportedOutputSizes(useCase.currentConfig)
        }
    }

    @Test
    fun supportedOutputSizes_whenMod16IsIgnoredForSmallSizes() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(296, 144),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            Size(185, 90)
        ).setTargetRotation(Surface.ROTATION_90).build()

        // The default minimum size 640x480 will be overwritten by the target resolution 185x90
        // (18.5:9). If mod 16 calculation is not ignored for the sizes smaller than 640x480, the
        // size 256x144 will be considered to match 18.5:9 and then become the first item in the
        // result list. After ignoring mod 16 calculation for small sizes, 256x144 will still be
        // kept as a 16:9 resolution as the result.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(296, 144),
            Size(256, 144),
            Size(320, 240)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizes_whenOneMod16SizeClosestToTargetResolution() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, arrayOf(
                Size(1920, 1080),
                Size(1440, 1080),
                Size(1280, 960),
                Size(1280, 720),
                Size(864, 480), // This is a 16:9 mod16 size that is closest to 2016x1080
                Size(768, 432),
                Size(640, 480),
                Size(640, 360),
                Size(480, 360),
                Size(384, 288)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            Size(1080, 2016)
        ).build()
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf(
            Size(1920, 1080),
            Size(1280, 720),
            Size(864, 480),
            Size(768, 432),
            Size(1440, 1080),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizesWithPortraitPixelArraySize_aspectRatio16x9() {
        val supportedSizes = arrayOf(
            Size(1080, 1920),
            Size(1080, 1440),
            Size(960, 1280),
            Size(720, 1280),
            Size(1280, 720),
            Size(480, 640),
            Size(640, 480),
            Size(360, 480)
        )

        // Sets the sensor orientation as 0 and pixel array size as a portrait size to simulate a
        // phone device which majorly supports portrait output sizes.
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation0, portraitPixelArraySize, supportedSizes, null
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetAspectRatio(
            AspectRatio.RATIO_16_9
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Due to the pixel array size is portrait, sizes of aspect ratio 9/16 will be in
        // front of the returned sizes list and the list is sorted in descending order. Other
        // items will be put in the following that are sorted by aspect ratio delta and then area
        // size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1080, 1920),
            Size(
                720,
                1280
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1080, 1440),
            Size(960, 1280),
            Size(480, 640),
            Size(640, 480),
            Size(1280, 720)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizesOnTabletWithPortraitPixelArraySize_aspectRatio16x9() {
        val supportedSizes = arrayOf(
            Size(1080, 1920),
            Size(1080, 1440),
            Size(960, 1280),
            Size(720, 1280),
            Size(1280, 720),
            Size(480, 640),
            Size(640, 480),
            Size(360, 480)
        )

        // Sets the sensor orientation as 90 and pixel array size as a portrait size to simulate a
        // tablet device which majorly supports portrait output sizes.
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation90, portraitPixelArraySize, supportedSizes, null
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetAspectRatio(
            AspectRatio.RATIO_16_9
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Due to the pixel array size is portrait, sizes of aspect ratio 9/16 will be in
        // front of the returned sizes list and the list is sorted in descending order. Other
        // items will be put in the following that are sorted by aspect ratio delta and then area
        // size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1080, 1920),
            Size(
                720,
                1280
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1080, 1440),
            Size(960, 1280),
            Size(480, 640),
            Size(640, 480),
            Size(1280, 720)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizesOnTablet_aspectRatio16x9() {
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation0, landscapePixelArraySize, supportedSizes, null
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetAspectRatio(
            AspectRatio.RATIO_16_9
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(
                800,
                450
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun supportedOutputSizesOnTabletWithPortraitSizes_aspectRatio16x9() {
        val supportedSizes = arrayOf(
            Size(1920, 1080),
            Size(1440, 1080),
            Size(1280, 960),
            Size(1280, 720),
            Size(720, 1280),
            Size(640, 480),
            Size(480, 640),
            Size(480, 360)
        )
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation0, landscapePixelArraySize, supportedSizes, null
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val useCase = FakeUseCaseConfig.Builder().setTargetAspectRatio(
            AspectRatio.RATIO_16_9
        ).build()

        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList: List<Size?> = supportedSurfaceCombination.getSupportedOutputSizes(
            useCase.currentConfig
        )
        val expectedList = listOf( // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(
                1280,
                720
            ), // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1440, 1080),
            Size(1280, 960),
            Size(640, 480),
            Size(480, 640),
            Size(720, 1280)
        )
        assertThat(resultList).isEqualTo(expectedList)
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
    fun canGet640x480_whenAnotherGroupMatchedInMod16Exists() {
        val supportedSizes = arrayOf(
            Size(4000, 3000),
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1024, 738), // This will create a 512/269 aspect ratio group that
            // 640x480 will be considered to match in mod16 condition.
            Size(800, 600),
            Size(640, 480),
            Size(320, 240)
        )
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation90, landscapePixelArraySize, supportedSizes, null
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        // Sets the target resolution as 640x480 with target rotation as ROTATION_90 because the
        // sensor orientation is 90.
        val useCase = FakeUseCaseConfig.Builder().setTargetResolution(
            vgaSize
        ).setTargetRotation(Surface.ROTATION_90).build()
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            listOf(useCase.currentConfig)
        )

        // Checks 640x480 is final selected for the use case.
        assertThat(suggestedStreamSpecMap[useCase.currentConfig]?.resolution).isEqualTo(vgaSize)
    }

    @Test
    fun canGetSupportedSizeSmallerThan640x480_whenLargerMaxResolutionIsSet() {
        val supportedSizes = arrayOf(
            Size(480, 480)
        )
        setupCamera(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation90, landscapePixelArraySize, supportedSizes, null
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )

        // Sets the max resolution as 720x1280
        val useCase = FakeUseCaseConfig.Builder().setMaxResolution(displaySize).build()
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            listOf(useCase.currentConfig)
        )

        // Checks 480x480 is final selected for the use case.
        assertThat(suggestedStreamSpecMap[useCase.currentConfig]?.resolution).isEqualTo(
            Size(480, 480)
        )
    }

    @Test
    fun previewSizeIsSelectedForImageAnalysis_imageCaptureHasNoSetSizeInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(
                mock()
            )
        )

        // ImageCapture has no explicit target resolution setting
        val imageCapture = ImageCapture.Builder().build()

        // A LEGACY-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM
        //
        // A LIMITED-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD
        //
        // Even there is a RECORD size target resolution setting for ImageAnalysis, ImageCapture
        // will still have higher priority to have a MAXIMUM size resolution if the app doesn't
        // explicitly specify a RECORD size target resolution to ImageCapture.
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(Surface.ROTATION_90)
            .setTargetResolution(recordSize)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(preview)
        useCases.add(imageCapture)
        useCases.add(imageAnalysis)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            ArrayList(useCaseToConfigMap.values)
        )
        assertThat(suggestedStreamSpecMap[useCaseToConfigMap[imageAnalysis]]?.resolution).isEqualTo(
            previewSize
        )
    }

    @Test
    fun recordSizeIsSelectedForImageAnalysis_imageCaptureHasExplicitSizeInLimitedDevice() {
        setupCamera(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, fakeCameraMetadata,
            mockEncoderProfilesAdapter
        )
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(
                mock()
            )
        )

        // ImageCapture has no explicit RECORD size target resolution setting
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_90)
            .setTargetResolution(recordSize)
            .build()

        // A LEGACY-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM
        //
        // A LIMITED-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD
        //
        // A RECORD can be selected for ImageAnalysis if the ImageCapture has a explicit RECORD
        // size target resolution setting. It means that the application know the trade-off and
        // the ImageAnalysis has higher priority to get a larger resolution than ImageCapture.
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(Surface.ROTATION_90)
            .setTargetResolution(recordSize)
            .build()
        val useCases: MutableList<UseCase> = ArrayList()
        useCases.add(preview)
        useCases.add(imageCapture)
        useCases.add(imageAnalysis)
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(fakeCameraMetadata.camera.value).cameraInfoInternal,
            useCases,
            useCaseConfigFactory
        )
        val suggestedStreamSpecMap = supportedSurfaceCombination.getSuggestedStreamSpecifications(
            emptyList(),
            ArrayList(useCaseToConfigMap.values)
        )
        assertThat(suggestedStreamSpecMap[useCaseToConfigMap[imageAnalysis]]?.resolution).isEqualTo(
            recordSize
        )
    }

    private fun setupCamera(hardwareLevel: Int, capabilities: IntArray) {
        setupCamera(
            hardwareLevel, sensorOrientation90, landscapePixelArraySize,
            supportedSizes, capabilities
        )
    }

    private fun setupCamera(hardwareLevel: Int, supportedSizes: Array<Size>) {
        setupCamera(
            hardwareLevel, sensorOrientation90, landscapePixelArraySize,
            supportedSizes, null
        )
    }

    private fun setupCamera(
        hardwareLevel: Int,
        sensorOrientation: Int = sensorOrientation90,
        pixelArraySize: Size = landscapePixelArraySize,
        supportedSizes: Array<Size> =
            this.supportedSizes,
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

        // set up FakeCafakeCameraMetadatameraMetadata
        fakeCameraMetadata = FakeCameraMetadata(
            cameraId = cameraId,
            characteristics = mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                CameraCharacteristics.SENSOR_ORIENTATION to sensorOrientation,
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE to pixelArraySize,
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to capabilities,
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap
            )
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
        shadowCharacteristics.set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)
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
                val isSupported = supportedSurfaceCombination.checkSupported(subConfigurationList)
                if (!isSupported) {
                    return false
                }
            }
        }
        return true
    }

    /** Creates a VideoCapture with one ore more specific Quality  */
    private fun createVideoCapture(vararg quality: Quality): VideoCapture<TestVideoOutput> {
        return createVideoCapture(QualitySelector.fromOrderedList(listOf(*quality)))
    }
    /** Creates a VideoCapture with a customized QualitySelector  */
    /** Creates a VideoCapture with a default QualitySelector  */
    @JvmOverloads
    fun createVideoCapture(
        qualitySelector: QualitySelector = VideoSpec.QUALITY_SELECTOR_AUTO
    ): VideoCapture<TestVideoOutput> {
        val mediaSpecBuilder = MediaSpec.builder()
        mediaSpecBuilder.configureVideo { builder: VideoSpec.Builder ->
            builder.setQualitySelector(
                qualitySelector
            )
        }
        val videoOutput = TestVideoOutput()
        videoOutput.mediaSpecObservable.setState(mediaSpecBuilder.build())
        return VideoCapture.withOutput(videoOutput)
    }

    /** A fake implementation of VideoOutput  */
    class TestVideoOutput : VideoOutput {
        var mediaSpecObservable =
            MutableStateObservable.withInitialState(MediaSpec.builder().build())
        private var surfaceRequest: SurfaceRequest? = null
        private var sourceState: SourceState? = null
        override fun onSurfaceRequested(request: SurfaceRequest) {
            surfaceRequest = request
        }

        override fun getMediaSpec(): Observable<MediaSpec> {
            return mediaSpecObservable
        }

        override fun onSourceStateChanged(sourceState: SourceState) {
            this.sourceState = sourceState
        }
    }
}