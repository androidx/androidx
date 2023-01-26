
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
package androidx.camera.camera2.internal
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.media.CamcorderProfile
import android.os.Build
import android.util.Pair
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.NonNull
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.SupportedOutputSizesCollectorTest.Companion.createUseCaseByResolutionSelector
import androidx.camera.camera2.internal.SupportedOutputSizesCollectorTest.Companion.setupCamera
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.MutableStateObservable
import androidx.camera.core.impl.SizeCoordinate
import androidx.camera.core.impl.SurfaceCombination
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigType
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3
import androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.camera.testing.CamcorderProfileUtil
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.Configs
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.fakes.FakeCamcorderProfileProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
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
import com.google.common.truth.Truth.assertThat
import java.util.Arrays
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.codehaus.plexus.util.ReflectionUtils
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val FAKE_USE_CASE = 0
private const val PREVIEW_USE_CASE = 1
private const val IMAGE_CAPTURE_USE_CASE = 2
private const val IMAGE_ANALYSIS_USE_CASE = 3
private const val UNKNOWN_ROTATION = -1
private const val UNKNOWN_ASPECT_RATIO = -1
private const val DEFAULT_CAMERA_ID = "0"
private const val EXTERNAL_CAMERA_ID = "0-external"
private const val SENSOR_ORIENTATION_0 = 0
private const val SENSOR_ORIENTATION_90 = 90
private val ASPECT_RATIO_16_9 = Rational(16, 9)
private val LANDSCAPE_PIXEL_ARRAY_SIZE = Size(4032, 3024)
private val PORTRAIT_PIXEL_ARRAY_SIZE = Size(3024, 4032)
private val DISPLAY_SIZE = Size(720, 1280)
private val PREVIEW_SIZE = Size(1280, 720)
private val RECORD_SIZE = Size(3840, 2160)
private val MAXIMUM_SIZE = Size(4032, 3024)
private val LEGACY_VIDEO_MAXIMUM_SIZE = Size(1920, 1080)
private val MOD16_SIZE = Size(960, 544)
private val DEFAULT_SUPPORTED_SIZES = arrayOf(
    Size(4032, 3024), // 4:3
    Size(3840, 2160), // 16:9
    Size(1920, 1440), // 4:3
    Size(1920, 1080), // 16:9
    Size(1280, 960), // 4:3
    Size(1280, 720), // 16:9
    Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
    Size(800, 450), // 16:9
    Size(640, 480), // 4:3
    Size(320, 240), // 4:3
    Size(320, 180), // 16:9
    Size(256, 144) // 16:9 For checkSmallSizesAreFilteredOut test.
)

/** Robolectric test for [SupportedSurfaceCombination] class */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedSurfaceCombinationTest {
    private val mockCamcorderProfileHelper = Mockito.mock(
        CamcorderProfileHelper::class.java
    )
    private val mockCamcorderProfile = Mockito.mock(
        CamcorderProfile::class.java
    )
    private var cameraManagerCompat: CameraManagerCompat? = null
    private val profileUhd = CamcorderProfileUtil.createCamcorderProfileProxy(
        CamcorderProfile.QUALITY_2160P, RECORD_SIZE.width, RECORD_SIZE.height
    )
    private val profileFhd = CamcorderProfileUtil.createCamcorderProfileProxy(
        CamcorderProfile.QUALITY_1080P, 1920, 1080
    )
    private val profileHd = CamcorderProfileUtil.createCamcorderProfileProxy(
        CamcorderProfile.QUALITY_720P, PREVIEW_SIZE.width, PREVIEW_SIZE.height
    )
    private val profileSd = CamcorderProfileUtil.createCamcorderProfileProxy(
        CamcorderProfile.QUALITY_480P, RESOLUTION_VGA.width,
        RESOLUTION_VGA.height
    )
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var cameraFactory: FakeCameraFactory? = null
    private var useCaseConfigFactory: UseCaseConfigFactory? = null

    private val legacyUseCaseCreator = object : UseCaseCreator {
        override fun createUseCase(
            useCaseType: Int,
            targetRotation: Int,
            preferredAspectRatio: Int,
            preferredResolution: Size?,
            maxResolution: Size?,
            highResolutionEnabled: Boolean,
            defaultResolution: Size?,
            supportedResolutions: List<Pair<Int, Array<Size>>>?,
            customOrderedResolutions: List<Size>?,
        ): UseCase {
            return createUseCaseByLegacyApi(
                useCaseType,
                targetRotation,
                preferredAspectRatio,
                preferredResolution,
                maxResolution,
                defaultResolution,
                supportedResolutions,
                customOrderedResolutions,
            )
        }
    }

    private val resolutionSelectorUseCaseCreator = object : UseCaseCreator {
        override fun createUseCase(
            useCaseType: Int,
            targetRotation: Int,
            preferredAspectRatio: Int,
            preferredResolution: Size?,
            maxResolution: Size?,
            highResolutionEnabled: Boolean,
            defaultResolution: Size?,
            supportedResolutions: List<Pair<Int, Array<Size>>>?,
            customOrderedResolutions: List<Size>?,
        ): UseCase {
            return createUseCaseByResolutionSelector(
                useCaseType,
                preferredAspectRatio,
                preferredResolution,
                sizeCoordinate = SizeCoordinate.CAMERA_SENSOR,
                maxResolution,
                highResolutionEnabled,
                highResolutionForceDisabled = false,
                defaultResolution,
                supportedResolutions,
                customOrderedResolutions,
            )
        }
    }

    private val viewSizeResolutionSelectorUseCaseCreator = object : UseCaseCreator {
        override fun createUseCase(
            useCaseType: Int,
            targetRotation: Int,
            preferredAspectRatio: Int,
            preferredResolution: Size?,
            maxResolution: Size?,
            highResolutionEnabled: Boolean,
            defaultResolution: Size?,
            supportedResolutions: List<Pair<Int, Array<Size>>>?,
            customOrderedResolutions: List<Size>?,
        ): UseCase {
            return createUseCaseByResolutionSelector(
                useCaseType,
                preferredAspectRatio,
                preferredResolution,
                sizeCoordinate = SizeCoordinate.ANDROID_VIEW,
                maxResolution,
                highResolutionEnabled,
                highResolutionForceDisabled = false,
                defaultResolution,
                supportedResolutions,
                customOrderedResolutions
            )
        }
    }

    @Suppress("DEPRECATION") // defaultDisplay
    @Before
    fun setUp() {
        DisplayInfoManager.releaseInstance()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(DISPLAY_SIZE.width)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(DISPLAY_SIZE.height)
        Mockito.`when`(
            mockCamcorderProfileHelper.hasProfile(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt()
            )
        ).thenReturn(true)
        ReflectionUtils.setVariableValueInObject(mockCamcorderProfile, "videoFrameWidth", 3840)
        ReflectionUtils.setVariableValueInObject(mockCamcorderProfile, "videoFrameHeight", 2160)
        Mockito.`when`(
            mockCamcorderProfileHelper[ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()]
        ).thenReturn(mockCamcorderProfile)
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun checkLegacySurfaceCombinationSupportedInLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLegacySupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkLegacySurfaceCombinationSubListSupportedInLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLegacySupportedCombinationList().also {
            assertThat(isAllSubConfigListSupported(supportedSurfaceCombination, it)).isTrue()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getFullSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSubListSupportedInLimited3Device() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList().also {
            assertThat(isAllSubConfigListSupported(supportedSurfaceCombination, it)).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationNotSupportedInLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getFullSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isFalse()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isFalse()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInFullDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getFullSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSubListSupportedInFullDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getFullSupportedCombinationList().also {
            assertThat(isAllSubConfigListSupported(supportedSurfaceCombination, it)).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationNotSupportedInFullDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isFalse()
        }
    }

    @Test
    fun checkLimitedSurfaceCombinationSupportedInRawDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLimitedSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkLegacySurfaceCombinationSupportedInRawDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLegacySupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkFullSurfaceCombinationSupportedInRawDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getFullSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkRawSurfaceCombinationSupportedInRawDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            capabilities = intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getRAWSupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationSupportedInLevel3Device() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList().forEach {
            assertThat(supportedSurfaceCombination.checkSupported(it.surfaceConfigList)).isTrue()
        }
    }

    @Test
    fun checkLevel3SurfaceCombinationSubListSupportedInLevel3Device() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        GuaranteedConfigurationsUtil.getLevel3SupportedCombinationList().also {
            assertThat(isAllSubConfigListSupported(supportedSurfaceCombination, it)).isTrue()
        }
    }

    @Test
    fun checkTargetAspectRatioInLegacyDevice_LegacyApi() {
        checkTargetAspectRatioInLegacyDevice(legacyUseCaseCreator)
    }

    @Test
    fun checkTargetAspectRatioInLegacyDevice_ResolutionSelector() {
        checkTargetAspectRatioInLegacyDevice(resolutionSelectorUseCaseCreator)
    }

    private fun checkTargetAspectRatioInLegacyDevice(useCaseCreator: UseCaseCreator) {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val targetAspectRatio = ASPECT_RATIO_16_9
        val useCase = useCaseCreator.createUseCase(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val maxJpegSize = supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG)
        val maxJpegAspectRatio = Rational(maxJpegSize.width, maxJpegSize.height)
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)
        val selectedSize = suggestedResolutionMap[useCase]
        val resultAspectRatio = Rational(selectedSize!!.width, selectedSize.height)
        // The targetAspectRatio value will only be set to the same aspect ratio as maximum
        // supported jpeg size in Legacy + API 21 combination. For other combinations, it should
        // keep the original targetAspectRatio set for the use case.
        if (Build.VERSION.SDK_INT == 21) {
            // Checks targetAspectRatio and maxJpegAspectRatio, which is the ratio of maximum size
            // in the mSupportedSizes, are not equal to make sure this test case is valid.
            assertThat(targetAspectRatio).isNotEqualTo(maxJpegAspectRatio)
            assertThat(resultAspectRatio).isEqualTo(maxJpegAspectRatio)
        } else {
            // Checks no correction is needed.
            assertThat(resultAspectRatio).isEqualTo(targetAspectRatio)
        }
    }

    @Test
    fun checkResolutionForMixedUseCase_AfterBindToLifecycle_InLegacyDevice_LegacyApi() {
        checkResolutionForMixedUseCase_AfterBindToLifecycle_InLegacyDevice(legacyUseCaseCreator)
    }

    @Test
    fun checkResolutionForMixedUseCase_AfterBindToLifecycle_InLegacyDevice_ResolutionSelector() {
        checkResolutionForMixedUseCase_AfterBindToLifecycle_InLegacyDevice(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun checkResolutionForMixedUseCase_AfterBindToLifecycle_InLegacyDevice(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // The test case make sure the selected result is expected after the regular flow.
        val targetAspectRatio = ASPECT_RATIO_16_9
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        ) as Preview
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(
                Mockito.mock(
                    SurfaceTextureCallback::class.java
                )
            )
        )
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val imageAnalysis = useCaseCreator.createUseCase(
            IMAGE_ANALYSIS_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val maxJpegSize = supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG)
        val maxJpegAspectRatio = Rational(maxJpegSize.width, maxJpegSize.height)
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination, preview,
            imageCapture, imageAnalysis
        )
        val previewResolution = suggestedResolutionMap[preview]
        val imageCaptureResolution = suggestedResolutionMap[imageCapture]
        val imageAnalysisResolution = suggestedResolutionMap[imageAnalysis]
        // The targetAspectRatio value will only be set to the same aspect ratio as maximum
        // supported jpeg size in Legacy + API 21 combination. For other combinations, it should
        // keep the original targetAspectRatio set for the use case.
        if (Build.VERSION.SDK_INT == 21) {
            // Checks targetAspectRatio and maxJpegAspectRatio, which is the ratio of maximum size
            // in the mSupportedSizes, are not equal to make sure this test case is valid.
            assertThat(targetAspectRatio).isNotEqualTo(maxJpegAspectRatio)
            assertThat(hasMatchingAspectRatio(previewResolution!!, maxJpegAspectRatio)).isTrue()
            assertThat(
                hasMatchingAspectRatio(
                    imageCaptureResolution!!,
                    maxJpegAspectRatio
                )
            ).isTrue()
            assertThat(
                hasMatchingAspectRatio(
                    imageAnalysisResolution!!,
                    maxJpegAspectRatio
                )
            ).isTrue()
        } else {
            // Checks no correction is needed.
            assertThat(
                hasMatchingAspectRatio(
                    previewResolution!!,
                    targetAspectRatio
                )
            ).isTrue()
            assertThat(
                hasMatchingAspectRatio(
                    imageCaptureResolution!!,
                    targetAspectRatio
                )
            ).isTrue()
            assertThat(
                hasMatchingAspectRatio(
                    imageAnalysisResolution!!,
                    targetAspectRatio
                )
            ).isTrue()
        }
    }

    @Test
    fun checkDefaultAspectRatioAndResolutionForMixedUseCase_LegacyApi() {
        checkDefaultAspectRatioAndResolutionForMixedUseCase(legacyUseCaseCreator)
    }

    @Test
    fun checkDefaultAspectRatioAndResolutionForMixedUseCase_ResolutionSelector() {
        checkDefaultAspectRatioAndResolutionForMixedUseCase(resolutionSelectorUseCaseCreator)
    }

    private fun checkDefaultAspectRatioAndResolutionForMixedUseCase(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val preview = useCaseCreator.createUseCase(PREVIEW_USE_CASE) as Preview
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(
                Mockito.mock(
                    SurfaceTextureCallback::class.java
                )
            )
        )
        val imageCapture = useCaseCreator.createUseCase(IMAGE_CAPTURE_USE_CASE)
        val imageAnalysis = useCaseCreator.createUseCase(IMAGE_ANALYSIS_USE_CASE)

        // Preview/ImageCapture/ImageAnalysis' default config settings that will be applied after
        // bound to lifecycle. Calling bindToLifecycle here to make sure sizes matching to
        // default aspect ratio will be selected.
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination, preview,
            imageCapture, imageAnalysis
        )
        val previewSize = suggestedResolutionMap[preview]!!
        val imageCaptureSize = suggestedResolutionMap[imageCapture]!!
        val imageAnalysisSize = suggestedResolutionMap[imageAnalysis]!!

        val previewAspectRatio = Rational(previewSize.width, previewSize.height)
        val imageCaptureAspectRatio = Rational(imageCaptureSize.width, imageCaptureSize.height)
        val imageAnalysisAspectRatio = Rational(imageAnalysisSize.width, imageAnalysisSize.height)

        // Checks the default aspect ratio.
        assertThat(previewAspectRatio).isEqualTo(ASPECT_RATIO_4_3)
        assertThat(imageCaptureAspectRatio).isEqualTo(ASPECT_RATIO_4_3)
        assertThat(imageAnalysisAspectRatio).isEqualTo(ASPECT_RATIO_4_3)

        // Checks the default resolution.
        assertThat(imageAnalysisSize).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    fun checkSmallSizesAreFilteredOutByDefaultSize480p() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
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
        val preview = createUseCaseByLegacyApi(
            PREVIEW_USE_CASE,
            targetResolution = Size(displayHeight, displayWidth)
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, preview)
        // Checks the preconditions.
        val preconditionSize = Size(256, 144)
        val targetRatio = Rational(displayHeight, displayWidth)
        assertThat(listOf(*DEFAULT_SUPPORTED_SIZES)).contains(preconditionSize)
        DEFAULT_SUPPORTED_SIZES.forEach {
            assertThat(Rational(it.width, it.height)).isNotEqualTo(targetRatio)
        }
        // Checks the mechanism has filtered out the sizes which are smaller than default size 480p.
        val previewSize = suggestedResolutionMap[preview]
        assertThat(previewSize).isNotEqualTo(preconditionSize)
    }

    @Test
    fun checkAllSupportedSizesCanBeSelected_LegacyApi() {
        checkAllSupportedSizesCanBeSelected(legacyUseCaseCreator)
    }

    @Test
    fun checkAllSupportedSizesCanBeSelected_ResolutionSelector_SensorSize() {
        checkAllSupportedSizesCanBeSelected(resolutionSelectorUseCaseCreator)
    }

    @Test
    fun checkAllSupportedSizesCanBeSelected_ResolutionSelector_ViewSize() {
        checkAllSupportedSizesCanBeSelected(viewSizeResolutionSelectorUseCaseCreator)
    }

    private fun checkAllSupportedSizesCanBeSelected(useCaseCreator: UseCaseCreator) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // Sets each of mSupportedSizes as target resolution and also sets target rotation as
        // Surface.ROTATION to make it aligns the sensor direction and then exactly the same size
        // will be selected as the result. This test can also verify that size smaller than
        // 640x480 can be selected after set as target resolution.
        DEFAULT_SUPPORTED_SIZES.forEach {
            val imageCapture = useCaseCreator.createUseCase(
                IMAGE_CAPTURE_USE_CASE,
                Surface.ROTATION_90,
                preferredResolution = it
            )
            val suggestedResolutionMap =
                getSuggestedResolutionMap(supportedSurfaceCombination, imageCapture)
            assertThat(it).isEqualTo(suggestedResolutionMap[imageCapture])
        }
    }

    @Test
    fun checkCorrectAspectRatioNotMatchedSizeCanBeSelected_LegacyApi() {
        // Sets target resolution as 1280x640, all supported resolutions will be put into
        // aspect ratio not matched list. Then, 1280x720 will be the nearest matched one.
        // Finally, checks whether 1280x720 is selected or not.
        checkCorrectAspectRatioNotMatchedSizeCanBeSelected(legacyUseCaseCreator, Size(1280, 720))
    }

    // 1280x640 is not included in the supported sizes list. So, the smallest size of the
    // default aspect ratio 4:3 which is 1280x960 will be finally selected.
    @Test
    fun checkCorrectAspectRatioNotMatchedSizeCanBeSelected_ResolutionSelector_SensorSize() {
        checkCorrectAspectRatioNotMatchedSizeCanBeSelected(
            resolutionSelectorUseCaseCreator,
            Size(1280, 960)
        )
    }

    @Test
    fun checkCorrectAspectRatioNotMatchedSizeCanBeSelected_ResolutionSelector_ViewSize() {
        checkCorrectAspectRatioNotMatchedSizeCanBeSelected(
            viewSizeResolutionSelectorUseCaseCreator,
            Size(1280, 960)
        )
    }

    private fun checkCorrectAspectRatioNotMatchedSizeCanBeSelected(
        useCaseCreator: UseCaseCreator,
        expectedResult: Size
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // Sets target resolution as 1280x640, all supported resolutions will be put into aspect
        // ratio not matched list. Then, 1280x720 will be the nearest matched one. Finally,
        // checks whether 1280x720 is selected or not.
        val resolution = Size(1280, 640)
        val useCase = useCaseCreator.createUseCase(
            FAKE_USE_CASE,
            Surface.ROTATION_90,
            preferredResolution = resolution
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(expectedResult)
    }

    @Test
    fun suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice_LegacyApi() {
        suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice(legacyUseCaseCreator)
    }

    @Test
    fun suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice_ResolutionSelector() {
        suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun suggestedResolutionsForMixedUseCaseNotSupportedInLegacyDevice(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val videoCapture = createVideoCapture()
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        // An IllegalArgumentException will be thrown because a LEGACY level device can't support
        // ImageCapture + VideoCapture + Preview
        assertThrows(IllegalArgumentException::class.java) {
            getSuggestedResolutionMap(
                supportedSurfaceCombination,
                imageCapture,
                videoCapture,
                preview
            )
        }
    }

    @Test
    fun suggestedResolutionsForCustomizeResolutionsNotSupportedInLegacyDevice_LegacyApi() {
        suggestedResolutionsForCustomizeResolutionsNotSupportedInLegacyDevice(legacyUseCaseCreator)
    }

    @Test
    fun suggestedResolutionsForCustomizeResolutionsNotSupportedInLegacyDevice_ResolutionSelector() {
        suggestedResolutionsForCustomizeResolutionsNotSupportedInLegacyDevice(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun suggestedResolutionsForCustomizeResolutionsNotSupportedInLegacyDevice(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // Legacy camera only support (PRIV, PREVIEW) + (PRIV, PREVIEW)
        val previewResolutionsPairs = listOf(
            Pair.create(ImageFormat.PRIVATE, arrayOf(PREVIEW_SIZE))
        )
        val videoCapture: VideoCapture<TestVideoOutput> = createVideoCapture(Quality.UHD)
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            supportedResolutions = previewResolutionsPairs
        )
        // An IllegalArgumentException will be thrown because the VideoCapture requests to only
        // support a RECORD size but the configuration can't be supported on a LEGACY level device.
        assertThrows(IllegalArgumentException::class.java) {
            getSuggestedResolutionMap(supportedSurfaceCombination, videoCapture, preview)
        }
    }

    @Test
    fun getSuggestedResolutionsForMixedUseCaseInLimitedDevice_LegacyApi() {
        getSuggestedResolutionsForMixedUseCaseInLimitedDevice(legacyUseCaseCreator)
    }

    @Test
    fun getSuggestedResolutionsForMixedUseCaseInLimitedDevice_ResolutionSelector() {
        getSuggestedResolutionsForMixedUseCaseInLimitedDevice(resolutionSelectorUseCaseCreator)
    }

    private fun getSuggestedResolutionsForMixedUseCaseInLimitedDevice(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val videoCapture = createVideoCapture(Quality.HIGHEST)
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            imageCapture,
            videoCapture,
            preview
        )
        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        assertThat(suggestedResolutionMap[imageCapture]).isEqualTo(RECORD_SIZE)
        assertThat(suggestedResolutionMap[videoCapture]).isEqualTo(RECORD_SIZE)
        assertThat(suggestedResolutionMap[preview]).isEqualTo(PREVIEW_SIZE)
    }

    // For the use case in b/230651237,
    // QualitySelector.from(Quality.UHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.UHD).
    // VideoCapture should have higher priority to choose size than ImageCapture.
    @Test
    @Throws(CameraUnavailableException::class)
    fun getSuggestedResolutionsInFullDevice_videoHasHigherPriorityThanImage_LegacyApi() {
        getSuggestedResolutionsInFullDevice_videoHasHigherPriorityThanImage(legacyUseCaseCreator)
    }

    @Test
    @Throws(CameraUnavailableException::class)
    fun getSuggestedResolutionsInFullDevice_videoHasHigherPriorityThanImage_ResolutionSelector() {
        getSuggestedResolutionsInFullDevice_videoHasHigherPriorityThanImage(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun getSuggestedResolutionsInFullDevice_videoHasHigherPriorityThanImage(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val videoCapture = createVideoCapture(QualitySelector.from(
            Quality.UHD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.UHD)
        ))
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            imageCapture,
            videoCapture,
            preview
        )
        // There are two possible combinations in Full level device
        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD) => should be applied
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM)
        assertThat(suggestedResolutionMap[imageCapture]).isEqualTo(RECORD_SIZE)
        assertThat(suggestedResolutionMap[videoCapture]).isEqualTo(RECORD_SIZE)
        assertThat(suggestedResolutionMap[preview]).isEqualTo(PREVIEW_SIZE)
    }

    @Test
    fun imageCaptureCanGetMaxSizeInFullDevice_videoRecordSizeLowPriority_LegacyApi() {
        imageCaptureCanGetMaxSizeInFullDevice_videoRecordSizeLowPriority(legacyUseCaseCreator)
    }

    @Test
    fun imageCaptureCanGetMaxSizeInFullDevice_videoRecordSizeLowPriority_ResolutionSelector() {
        imageCaptureCanGetMaxSizeInFullDevice_videoRecordSizeLowPriority(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun imageCaptureCanGetMaxSizeInFullDevice_videoRecordSizeLowPriority(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_4_3 // mMaximumSize(4032x3024) is 4:3
        )
        val videoCapture = createVideoCapture(
            QualitySelector.fromOrderedList(
                listOf<Quality>(Quality.HD, Quality.FHD, Quality.UHD)
            )
        )
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            imageCapture,
            videoCapture,
            preview
        )
        // There are two possible combinations in Full level device
        // (PRIV, PREVIEW) + (PRIV, RECORD) + (JPEG, RECORD)
        // (PRIV, PREVIEW) + (PRIV, PREVIEW) + (JPEG, MAXIMUM) => should be applied
        assertThat(suggestedResolutionMap[imageCapture]).isEqualTo(MAXIMUM_SIZE)
        assertThat(suggestedResolutionMap[videoCapture]).isEqualTo(PREVIEW_SIZE) // Quality.HD
        assertThat(suggestedResolutionMap[preview]).isEqualTo(PREVIEW_SIZE)
    }

    @Test
    fun getSuggestedResolutionsWithSameSupportedListForDifferentUseCases_LegacyApi() {
        getSuggestedResolutionsWithSameSupportedListForDifferentUseCases(
            legacyUseCaseCreator,
            DISPLAY_SIZE
        )
    }

    @Test
    fun getSuggestedResolutionsWithSameSupportedListForDifferentUseCases_RS_SensorSize() {
        getSuggestedResolutionsWithSameSupportedListForDifferentUseCases(
            resolutionSelectorUseCaseCreator,
            PREVIEW_SIZE
        )
    }

    @Test
    fun getSuggestedResolutionsWithSameSupportedListForDifferentUseCases_RS_ViewSize() {
        getSuggestedResolutionsWithSameSupportedListForDifferentUseCases(
            viewSizeResolutionSelectorUseCaseCreator,
            PREVIEW_SIZE
        )
    }

    private fun getSuggestedResolutionsWithSameSupportedListForDifferentUseCases(
        useCaseCreator: UseCaseCreator,
        preferredResolution: Size
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            supportedSizes = arrayOf(
                Size(4032, 3024), // 4:3
                Size(3840, 2160), // 16:9
                Size(1920, 1440), // 4:3
                Size(1920, 1080), // 16:9
                Size(1280, 960), // 4:3
                Size(1280, 720), // 16:9
                Size(1280, 720), // duplicate the size since Nexus 5X emulator has the case.
                Size(960, 544), // a mod16 version of resolution with 16:9 aspect ratio.
                Size(800, 450), // 16:9
                Size(640, 480), // 4:3
                Size(320, 240), // 4:3
                Size(320, 180), // 16:9
                Size(256, 144) // 16:9 For checkSmallSizesAreFilteredOut test.
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        /* This test case is for b/132603284 that divide by zero issue crash happened in below
        conditions:
        1. There are duplicated two 1280x720 supported sizes for ImageCapture and Preview.
        2. supportedOutputSizes for ImageCapture and Preview in
        SupportedSurfaceCombination#getAllPossibleSizeArrangements are the same.
        */
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredResolution = preferredResolution
        )
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredResolution = preferredResolution
        )
        val imageAnalysis = useCaseCreator.createUseCase(
            IMAGE_ANALYSIS_USE_CASE,
            preferredResolution = preferredResolution
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            imageCapture,
            imageAnalysis,
            preview
        )
        assertThat(suggestedResolutionMap[imageCapture]).isEqualTo(PREVIEW_SIZE)
        assertThat(suggestedResolutionMap[imageAnalysis]).isEqualTo(PREVIEW_SIZE)
        assertThat(suggestedResolutionMap[preview]).isEqualTo(PREVIEW_SIZE)
    }

    @Test
    fun setTargetAspectRatioForMixedUseCases_LegacyApi() {
        setTargetAspectRatioForMixedUseCases(legacyUseCaseCreator)
    }

    @Test
    fun setTargetAspectRatioForMixedUseCases_ResolutionSelector() {
        setTargetAspectRatioForMixedUseCases(resolutionSelectorUseCaseCreator)
    }

    private fun setTargetAspectRatioForMixedUseCases(useCaseCreator: UseCaseCreator) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val imageAnalysis = useCaseCreator.createUseCase(
            IMAGE_ANALYSIS_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            preview,
            imageCapture,
            imageAnalysis
        )
        assertThat(
            hasMatchingAspectRatio(
                suggestedResolutionMap[preview]!!,
                ASPECT_RATIO_16_9
            )
        ).isTrue()
        assertThat(
            hasMatchingAspectRatio(
                suggestedResolutionMap[imageCapture]!!,
                ASPECT_RATIO_16_9
            )
        ).isTrue()
        assertThat(
            hasMatchingAspectRatio(
                suggestedResolutionMap[imageAnalysis]!!,
                ASPECT_RATIO_16_9
            )
        ).isTrue()
    }

    @Test
    fun getSuggestedResolutionsForCustomizedSupportedResolutions_LegacyApi() {
        getSuggestedResolutionsForCustomizedSupportedResolutions(legacyUseCaseCreator)
    }

    @Test
    fun getSuggestedResolutionsForCustomizedSupportedResolutions_ResolutionSelector() {
        getSuggestedResolutionsForCustomizedSupportedResolutions(resolutionSelectorUseCaseCreator)
    }

    private fun getSuggestedResolutionsForCustomizedSupportedResolutions(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val formatResolutionsPairList = arrayListOf<Pair<Int, Array<Size>>>().apply {
            add(Pair.create(ImageFormat.JPEG, arrayOf(RESOLUTION_VGA)))
            add(Pair.create(ImageFormat.YUV_420_888, arrayOf(RESOLUTION_VGA)))
            add(Pair.create(ImageFormat.PRIVATE, arrayOf(RESOLUTION_VGA)))
        }
        // Sets use cases customized supported resolutions to 640x480 only.
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            supportedResolutions = formatResolutionsPairList
        )
        val videoCapture = createVideoCapture(Quality.SD)
        val preview = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            supportedResolutions = formatResolutionsPairList
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            imageCapture,
            videoCapture,
            preview
        )
        // Checks all suggested resolutions will become 640x480.
        assertThat(suggestedResolutionMap[imageCapture]).isEqualTo(RESOLUTION_VGA)
        assertThat(suggestedResolutionMap[videoCapture]).isEqualTo(RESOLUTION_VGA)
        assertThat(suggestedResolutionMap[preview]).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    fun transformSurfaceConfigWithYUVAnalysisSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.YUV_420_888, RESOLUTION_VGA
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVPreviewSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.YUV_420_888, PREVIEW_SIZE
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVRecordSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.YUV_420_888, RECORD_SIZE
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithYUVMaximumSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.YUV_420_888, MAXIMUM_SIZE
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.YUV, ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGAnalysisSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.JPEG, RESOLUTION_VGA
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.VGA)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGPreviewSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.JPEG, PREVIEW_SIZE
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.PREVIEW)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGRecordSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.JPEG, RECORD_SIZE
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.RECORD)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun transformSurfaceConfigWithJPEGMaximumSize() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val surfaceConfig = supportedSurfaceCombination.transformSurfaceConfig(
            ImageFormat.JPEG, MAXIMUM_SIZE
        )
        val expectedSurfaceConfig = SurfaceConfig.create(ConfigType.JPEG, ConfigSize.MAXIMUM)
        assertThat(surfaceConfig).isEqualTo(expectedSurfaceConfig)
    }

    @Test
    fun getMaximumSizeForImageFormat() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val maximumYUVSize =
            supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.YUV_420_888)
        assertThat(maximumYUVSize).isEqualTo(MAXIMUM_SIZE)
        val maximumJPEGSize = supportedSurfaceCombination.getMaxOutputSizeByFormat(ImageFormat.JPEG)
        assertThat(maximumJPEGSize).isEqualTo(MAXIMUM_SIZE)
    }

    @Test
    fun isAspectRatioMatchWithSupportedMod16Resolution_LegacyApi() {
        isAspectRatioMatchWithSupportedMod16Resolution(legacyUseCaseCreator)
    }

    @Test
    fun isAspectRatioMatchWithSupportedMod16Resolution_ResolutionSelector() {
        isAspectRatioMatchWithSupportedMod16Resolution(resolutionSelectorUseCaseCreator)
    }

    private fun isAspectRatioMatchWithSupportedMod16Resolution(useCaseCreator: UseCaseCreator) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = useCaseCreator.createUseCase(
            FAKE_USE_CASE,
            Surface.ROTATION_90,
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            preferredResolution = MOD16_SIZE
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(MOD16_SIZE)
    }

    @Test
    fun sortByCompareSizesByArea_canSortSizesCorrectly() {
        val sizes = arrayOfNulls<Size>(DEFAULT_SUPPORTED_SIZES.size)
        // Generates a unsorted array from mSupportedSizes.
        val centerIndex = DEFAULT_SUPPORTED_SIZES.size / 2
        // Puts 2nd half sizes in the front
        for (i in centerIndex until DEFAULT_SUPPORTED_SIZES.size) {
            sizes[i - centerIndex] = DEFAULT_SUPPORTED_SIZES[i]
        }
        // Puts 1st half sizes inversely in the tail
        for (j in centerIndex - 1 downTo 0) {
            sizes[DEFAULT_SUPPORTED_SIZES.size - j - 1] = DEFAULT_SUPPORTED_SIZES[j]
        }
        // The testing sizes array will be equal to mSupportedSizes after sorting.
        Arrays.sort(sizes, CompareSizesByArea(true))
        assertThat(listOf(*sizes)).isEqualTo(listOf(*DEFAULT_SUPPORTED_SIZES))
    }

    @Test
    fun getSupportedOutputSizes_noConfigSettings() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(FAKE_USE_CASE)
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. No any aspect ratio related setting. The returned sizes list will be sorted in
        // descending order.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
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
    fun getSupportedOutputSizes_aspectRatio4x3() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_4_3
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 4/3 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio16x9_InLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_16_9
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio16x9_inLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_16_9
        )
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed.
        val expectedList: List<Size> = if (Build.VERSION.SDK_INT == 21) {
            // Sizes with the same aspect ratio as maximum JPEG resolution will be in front of
            // the returned sizes list and the list is sorted in descending order. Other items
            // will be put in the following that are sorted by aspect ratio delta and then area
            // size.
            listOf(
                // Matched the same AspectRatio as maximum JPEG items, sorted by aspect ratio
                // delta then area size.
                Size(4032, 3024),
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                // Mismatched the same AspectRatio as maximum JPEG items, sorted by area size.
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450)
            )
        } else {
            // Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
            // list is sorted in descending order. Other items will be put in the following that
            // are sorted by aspect ratio delta and then area size.
            listOf(
                // Matched AspectRatio items, sorted by area size.
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                Size(4032, 3024),
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480)
            )
        }
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_targetResolution1080x1920InRotation0_InLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetResolution = Size(1080, 1920)
        )
        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // target resolution will be calibrated by default target rotation 0 degree. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Therefore, sizes of aspect ratio
        // 16/9 will be in front of the returned sizes list and the list is sorted in descending
        // order. Other items will be put in the following that are sorted by aspect ratio delta
        // and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_targetResolution1080x1920InRotation0_InLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetResolution = Size(1080, 1920)
        )
        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList: List<Size> = if (Build.VERSION.SDK_INT == 21) {
            // Sizes with the same aspect ratio as maximum JPEG resolution will be in front of
            // the returned sizes list and the list is sorted in descending order. Other items
            // will be put in the following that are sorted by aspect ratio delta and then area
            // size.
            listOf(
                // Matched the same AspectRatio as maximum JPEG items, sorted by aspect ratio
                // delta then area size.
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                // Mismatched the same AspectRatio as maximum JPEG items, sorted by area size.
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450)
            )
        } else {
            // The target resolution will be calibrated by default target rotation 0 degree. The
            // auto-resolution mechanism will try to select the sizes which aspect ratio is
            // nearest to the aspect ratio of target resolution in priority. Therefore, sizes of
            // aspect ratio 16/9 will be in front of the returned sizes list and the list is
            // sorted in descending order. Other items will be put in the following that are
            // sorted by aspect ratio delta and then area size.
            listOf(
                // Matched AspectRatio items, sorted by area size.
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480)
            )
        }
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_targetResolutionLargerThan640x480() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(1280, 960)
        )
        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Target resolution larger than 640x480 won't overwrite
        // minimum size setting. Sizes smaller than 640x480 will be removed. The auto-resolution
        // mechanism will try to select the sizes which aspect ratio is nearest to the aspect
        // ratio of target resolution in priority. Therefore, sizes of aspect ratio 4/3 will be
        // in front of the returned sizes list and the list is sorted in descending order. Other
        // items will be put in the following that are sorted by aspect ratio delta and then area
        // size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1280, 960),
            Size(640, 480),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_targetResolutionSmallerThan640x480() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(320, 240)
        )
        // Unnecessary big enough sizes will be removed from the result list. Minimum size will
        // be overwritten as 320x240. Sizes smaller than 320x240 will also be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Therefore, sizes of aspect ratio
        // 4/3 will be in front of the returned sizes list and the list is sorted in descending
        // order. Other items will be put in the following that are sorted by aspect ratio delta
        // and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(320, 240),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_maxResolutionSmallerThan640x480() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            maxResolution = Size(320, 240)
        )
        // Minimum size bound will be removed due to small max resolution setting.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = Arrays.asList(
            *arrayOf(
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_targetResolution1800x1440NearTo4x3() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(1800, 1440)
        )
        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Size 1800x1440 is near to 4/3
        // therefore, sizes of aspect ratio 4/3 will be in front of the returned sizes list and
        // the list is sorted in descending order.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Sizes of 4/3 are near to aspect ratio of 1800/1440
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            // Sizes of 16/9 are far to aspect ratio of 1800/1440
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_targetResolution1280x600NearTo16x9() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(1280, 600)
        )
        // Unnecessary big enough sizes will be removed from the result list. There is default
        // minimum size 640x480 setting. Sizes smaller than 640x480 will also be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Size 1280x600 is near to 16/9,
        // therefore, sizes of aspect ratio 16/9 will be in front of the returned sizes list and
        // the list is sorted in descending order.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Sizes of 16/9 are near to aspect ratio of 1280/600
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            // Sizes of 4/3 are far to aspect ratio of 1280/600
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_maxResolution1280x720() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            maxResolution = Size(1280, 720)
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 or
        // larger than 1280x720 will be removed. The returned sizes list will be sorted in
        // descending order.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(1280, 720), Size(960, 544), Size(800, 450), Size(640, 480))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_setCustomOrderedResolutions() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val customOrderedResolutions = listOf(
            Size(640, 480),
            Size(1280, 720),
            Size(1920, 1080),
            Size(3840, 2160),
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            customOrderedResolutions = customOrderedResolutions,
            maxResolution = Size(1920, 1440),
            defaultResolution = Size(1280, 720),
            supportedResolutions = listOf(
                Pair.create(
                    ImageFormat.PRIVATE, arrayOf(
                        Size(800, 450),
                        Size(640, 480),
                        Size(320, 240),
                    )
                )
            )
        )
        // Custom ordered resolutions is fully respected, meaning it will not be sorted or filtered
        // by other configurations such as max/default/target/supported resolutions.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        assertThat(resultList).containsExactlyElementsIn(customOrderedResolutions).inOrder()
    }

    @Test
    fun previewCanSelectResolutionLargerThanDisplay_withMaxResolution_LegacyApi() {
        previewCanSelectResolutionLargerThanDisplay_withMaxResolution(legacyUseCaseCreator)
    }

    @Test
    fun previewCanSelectResolutionLargerThanDisplay_withMaxResolution_ResolutionSelector() {
        previewCanSelectResolutionLargerThanDisplay_withMaxResolution(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun previewCanSelectResolutionLargerThanDisplay_withMaxResolution(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // The max resolution is expressed in the sensor coordinate.
        val useCase = useCaseCreator.createUseCase(
            PREVIEW_USE_CASE,
            maxResolution = MAXIMUM_SIZE
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)
        // Checks mMaximumSize is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(MAXIMUM_SIZE)
    }

    @Test
    fun getSupportedOutputSizes_defaultResolution1280x720_noTargetResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            defaultResolution = Size(1280, 720)
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. If there is no target resolution setting, it will be overwritten by default
        // resolution as 1280x720. Unnecessary big enough sizes will also be removed. The
        // returned sizes list will be sorted in descending order.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(1280, 720), Size(960, 544), Size(800, 450), Size(640, 480))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_defaultResolution1280x720_targetResolution1920x1080() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            defaultResolution = Size(1280, 720),
            targetResolution = Size(1920, 1080)
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. There is target resolution 1920x1080, it won't be overwritten by default
        // resolution 1280x720. Unnecessary big enough sizes will also be removed. Sizes of
        // aspect ratio 16/9 will be in front of the returned sizes list and the list is sorted
        // in descending order.  Other items will be put in the following that are sorted by
        // aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_fallbackToGuaranteedResolution_whenNotFulfillConditions() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(1920, 1080)
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. There is target resolution 1920x1080 (16:9). Even 640x480 does not match 16:9
        // requirement, it will still be returned to use.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(640, 480))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenMaxSizeSmallerThanDefaultMiniSize() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            maxResolution = Size(320, 240)
        )
        // There is default minimum size 640x480 setting. Originally, sizes smaller than 640x480
        // will be removed. Due to maximal size bound is smaller than the default minimum size
        // bound and it is also smaller than 640x480, the default minimum size bound will be
        // ignored. Then, sizes equal to or smaller than 320x240 will be kept in the result list.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(320, 240), Size(320, 180), Size(256, 144))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenMaxSizeSmallerThanSmallTargetResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(320, 240),
            maxResolution = Size(320, 180)
        )
        // The default minimum size 640x480 will be overwritten by the target resolution 320x240.
        // Originally, sizes smaller than 320x240 will be removed. Due to maximal size bound is
        // smaller than the minimum size bound and it is also smaller than 640x480, the minimum
        // size bound will be ignored. Then, sizes equal to or smaller than 320x180 will be kept
        // in the result list.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(320, 180), Size(256, 144))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenBothMaxAndTargetResolutionsSmallerThan640x480() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(320, 180),
            maxResolution = Size(320, 240)
        )
        // The default minimum size 640x480 will be overwritten by the target resolution 320x180.
        // Originally, sizes smaller than 320x180 will be removed. Due to maximal size bound is
        // smaller than the minimum size bound and it is also smaller than 640x480, the minimum
        // size bound will be ignored. Then, all sizes equal to or smaller than 320x320 will be
        // kept in the result list.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(320, 180), Size(256, 144), Size(320, 240))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenMaxSizeSmallerThanBigTargetResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(3840, 2160),
            maxResolution = Size(1920, 1080)
        )
        // Because the target size 3840x2160 is larger than 640x480, it won't overwrite the
        // default minimum size 640x480. Sizes smaller than 640x480 will be removed. The
        // auto-resolution mechanism will try to select the sizes which aspect ratio is nearest
        // to the aspect ratio of target resolution in priority. Therefore, sizes of aspect ratio
        // 16/9 will be in front of the returned sizes list and the list is sorted in descending
        // order. Other items will be put in the following that are sorted by aspect ratio delta
        // and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenNoSizeBetweenMaxSizeAndTargetResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(320, 190),
            maxResolution = Size(320, 200)
        )
        // The default minimum size 640x480 will be overwritten by the target resolution 320x190.
        // Originally, sizes smaller than 320x190 will be removed. Due to there is no available
        // size between the maximal size and the minimum size bound and the maximal size is
        // smaller than 640x480, the default minimum size bound will be ignored. Then, sizes
        // equal to or smaller than 320x200 will be kept in the result list.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(320, 180), Size(256, 144))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenTargetResolutionSmallerThanAnySize() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(192, 144)
        )
        // The default minimum size 640x480 will be overwritten by the target resolution 192x144.
        // Because 192x144 is smaller than any size in the supported list, no one will be
        // filtered out by it. The result list will only keep one big enough size of aspect ratio
        // 4:3 and 16:9.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(320, 240), Size(256, 144))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenMaxResolutionSmallerThanAnySize() {
        setupCameraAndInitCameraX(
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            maxResolution = Size(192, 144)
        )
        // All sizes will be filtered out by the max resolution 192x144 setting and an
        // IllegalArgumentException will be thrown.
        assertThrows(IllegalArgumentException::class.java) {
            getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        }
    }

    @Test
    fun getSupportedOutputSizes_whenMod16IsIgnoredForSmallSizes() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(296, 144),
                Size(256, 144)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            targetResolution = Size(185, 90)
        )
        // The default minimum size 640x480 will be overwritten by the target resolution 185x90
        // (18.5:9). If mod 16 calculation is not ignored for the sizes smaller than 640x480, the
        // size 256x144 will be considered to match 18.5:9 and then become the first item in the
        // result list. After ignoring mod 16 calculation for small sizes, 256x144 will still be
        // kept as a 16:9 resolution as the result.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(Size(296, 144), Size(256, 144), Size(320, 240))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenOneMod16SizeClosestToTargetResolution() {
        setupCameraAndInitCameraX(
            supportedSizes = arrayOf(
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
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetResolution = Size(1080, 2016)
        )
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
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
    fun getSupportedOutputSizesWithPortraitPixelArraySize_aspectRatio16x9() {
        // Sets the sensor orientation as 0 and pixel array size as a portrait size to simulate a
        // phone device which majorly supports portrait output sizes.
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation = SENSOR_ORIENTATION_0,
            pixelArraySize = PORTRAIT_PIXEL_ARRAY_SIZE,
            supportedSizes = arrayOf(
                Size(1080, 1920),
                Size(1080, 1440),
                Size(960, 1280),
                Size(720, 1280),
                Size(1280, 720),
                Size(480, 640),
                Size(640, 480),
                Size(360, 480)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_16_9
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Due to the pixel array size is portrait, sizes of aspect ratio 9/16 will be in
        // front of the returned sizes list and the list is sorted in descending order. Other
        // items will be put in the following that are sorted by aspect ratio delta and then area
        // size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1080, 1920),
            Size(720, 1280),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1080, 1440),
            Size(960, 1280),
            Size(480, 640),
            Size(640, 480),
            Size(1280, 720)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizesOnTabletWithPortraitPixelArraySize_aspectRatio16x9() {
        // Sets the sensor orientation as 90 and pixel array size as a portrait size to simulate a
        // tablet device which majorly supports portrait output sizes.
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation = SENSOR_ORIENTATION_90,
            pixelArraySize = PORTRAIT_PIXEL_ARRAY_SIZE,
            supportedSizes = arrayOf(
                Size(1080, 1920),
                Size(1080, 1440),
                Size(960, 1280),
                Size(720, 1280),
                Size(1280, 720),
                Size(480, 640),
                Size(640, 480),
                Size(360, 480)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_16_9
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Due to the pixel array size is portrait, sizes of aspect ratio 9/16 will be in
        // front of the returned sizes list and the list is sorted in descending order. Other
        // items will be put in the following that are sorted by aspect ratio delta and then area
        // size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1080, 1920),
            Size(720, 1280),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1080, 1440),
            Size(960, 1280),
            Size(480, 640),
            Size(640, 480),
            Size(1280, 720)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizesOnTablet_aspectRatio16x9() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation = SENSOR_ORIENTATION_0,
            pixelArraySize = LANDSCAPE_PIXEL_ARRAY_SIZE
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_16_9
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizesOnTabletWithPortraitSizes_aspectRatio16x9() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            sensorOrientation = SENSOR_ORIENTATION_0, supportedSizes = arrayOf(
                Size(1920, 1080),
                Size(1440, 1080),
                Size(1280, 960),
                Size(1280, 720),
                Size(720, 1280),
                Size(640, 480),
                Size(480, 640),
                Size(480, 360)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val useCase = createUseCaseByLegacyApi(
            FAKE_USE_CASE,
            targetAspectRatio = AspectRatio.RATIO_16_9
        )
        // There is default minimum size 640x480 setting. Sizes smaller than 640x480 will be
        // removed. Sizes of aspect ratio 16/9 will be in front of the returned sizes list and the
        // list is sorted in descending order. Other items will be put in the following that are
        // sorted by aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedSurfaceCombination, useCase)
        val expectedList = listOf(
            // Matched AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            // Mismatched AspectRatio items, sorted by aspect ratio delta then area size.
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
        setupCameraAndInitCameraX(cameraId = EXTERNAL_CAMERA_ID)
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, EXTERNAL_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // Checks the determined RECORD size
        assertThat(
            supportedSurfaceCombination.mSurfaceSizeDefinition.recordSize
        ).isEqualTo(
            LEGACY_VIDEO_MAXIMUM_SIZE
        )
    }

    @Test
    fun canGet640x480_whenAnotherGroupMatchedInMod16Exists_LegacyApi() {
        canGet640x480_whenAnotherGroupMatchedInMod16Exists(legacyUseCaseCreator)
    }

    @Test
    fun canGet640x480_whenAnotherGroupMatchedInMod16Exists_RS_SensorSize() {
        canGet640x480_whenAnotherGroupMatchedInMod16Exists(resolutionSelectorUseCaseCreator)
    }

    @Test
    fun canGet640x480_whenAnotherGroupMatchedInMod16Exists_RS_ViewSize() {
        canGet640x480_whenAnotherGroupMatchedInMod16Exists(viewSizeResolutionSelectorUseCaseCreator)
    }

    private fun canGet640x480_whenAnotherGroupMatchedInMod16Exists(useCaseCreator: UseCaseCreator) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(4000, 3000),
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1024, 738), // This will create a 512/269 aspect ratio group that
                // 640x480 will be considered to match in mod16 condition.
                Size(800, 600),
                Size(640, 480),
                Size(320, 240)
            )
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // Sets the target resolution as 640x480 with target rotation as ROTATION_90 because the
        // sensor orientation is 90.
        val useCase = useCaseCreator.createUseCase(
            FAKE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            preferredResolution = RESOLUTION_VGA
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)
        // Checks 640x480 is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(RESOLUTION_VGA)
    }

    @Test
    fun canGetSupportedSizeSmallerThan640x480_whenLargerMaxResolutionIsSet_LegacyApi() {
        canGetSupportedSizeSmallerThan640x480_whenLargerMaxResolutionIsSet(legacyUseCaseCreator)
    }

    @Test
    fun canGetSupportedSizeSmallerThan640x480_whenLargerMaxResolutionIsSet_ResolutionSelector() {
        canGetSupportedSizeSmallerThan640x480_whenLargerMaxResolutionIsSet(
            resolutionSelectorUseCaseCreator
        )
    }

    private fun canGetSupportedSizeSmallerThan640x480_whenLargerMaxResolutionIsSet(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(Size(480, 480))
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        // Sets the max resolution as 720x1280
        val useCase = useCaseCreator.createUseCase(
            FAKE_USE_CASE,
            maxResolution = DISPLAY_SIZE
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)
        // Checks 480x480 is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(Size(480, 480))
    }

    @Test
    fun previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice_LegacyApi() {
        previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice(
            legacyUseCaseCreator, PREVIEW_SIZE
        )
    }

    // For the ResolutionSelector API, RECORD_SIZE can't be used because it exceeds
    // PREVIEW_SIZE. Therefore, the logic will fallback to select a 4:3 PREVIEW_SIZE. Then,
    // 640x480 will be selected.
    @Test
    fun previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice_RS_SensorSize() {
        previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice(
            resolutionSelectorUseCaseCreator, RESOLUTION_VGA
        )
    }

    @Test
    fun previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice_RS_ViewSize() {
        previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice(
            viewSizeResolutionSelectorUseCaseCreator, RESOLUTION_VGA
        )
    }

    private fun previewSizeIsSelectedForImageAnalysis_withImageCaptureInLimitedDevice(
        useCaseCreator: UseCaseCreator,
        expectedResult: Size
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val preview = useCaseCreator.createUseCase(PREVIEW_USE_CASE) as Preview
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(
                Mockito.mock(
                    SurfaceTextureCallback::class.java
                )
            )
        )
        // ImageCapture has no explicit target resolution setting
        val imageCapture = useCaseCreator.createUseCase(IMAGE_CAPTURE_USE_CASE)
        // A LEGACY-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM
        //
        // A LIMITED-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD
        //
        // Even there is a RECORD size target resolution setting for ImageAnalysis, ImageCapture
        // will still have higher priority to have a MAXIMUM size resolution if the app doesn't
        // explicitly specify a RECORD size target resolution to ImageCapture.
        val imageAnalysis = useCaseCreator.createUseCase(
            IMAGE_ANALYSIS_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            preferredResolution = RECORD_SIZE
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            preview,
            imageCapture,
            imageAnalysis
        )
        assertThat(suggestedResolutionMap[imageAnalysis]).isEqualTo(expectedResult)
    }

    @Test
    fun imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice_LegacyApi() {
        imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice(
            legacyUseCaseCreator
        )
    }

    @Test
    fun imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice_RS_SensorSize() {
        imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice(
            resolutionSelectorUseCaseCreator
        )
    }

    @Test
    fun imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice_RS_ViewSize() {
        imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice(
            viewSizeResolutionSelectorUseCaseCreator
        )
    }

    private fun imageAnalysisSelectRecordSize_imageCaptureHasExplicitSizeInLimitedDevice(
        useCaseCreator: UseCaseCreator
    ) {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )
        val preview = useCaseCreator.createUseCase(PREVIEW_USE_CASE) as Preview
        preview.setSurfaceProvider(
            CameraXExecutors.directExecutor(),
            SurfaceTextureProvider.createSurfaceTextureProvider(
                Mockito.mock(
                    SurfaceTextureCallback::class.java
                )
            )
        )
        // ImageCapture has no explicit RECORD size target resolution setting
        val imageCapture = useCaseCreator.createUseCase(
            IMAGE_CAPTURE_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            preferredResolution = RECORD_SIZE
        )
        // A LEGACY-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/PREVIEW + JPEG/MAXIMUM
        //
        // A LIMITED-level above device supports the following configuration.
        //     PRIV/PREVIEW + YUV/RECORD + JPEG/RECORD
        //
        // A RECORD can be selected for ImageAnalysis if the ImageCapture has a explicit RECORD
        // size target resolution setting. It means that the application know the trade-off and
        // the ImageAnalysis has higher priority to get a larger resolution than ImageCapture.
        val imageAnalysis = useCaseCreator.createUseCase(
            IMAGE_ANALYSIS_USE_CASE,
            targetRotation = Surface.ROTATION_90,
            preferredResolution = RECORD_SIZE
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(
            supportedSurfaceCombination,
            preview,
            imageCapture,
            imageAnalysis
        )
        assertThat(suggestedResolutionMap[imageAnalysis]).isEqualTo(RECORD_SIZE)
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun highResolutionIsSelected_whenHighResolutionIsEnabled() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            ),
            supportedHighResolutionSizes = arrayOf(Size(8000, 6000), Size(8000, 4500))
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )

        val useCase = createUseCaseByResolutionSelector(FAKE_USE_CASE, highResolutionEnabled = true)
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)

        // Checks 8000x6000 is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(Size(8000, 6000))
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun highResolutionIsNotSelected_whenHighResolutionIsEnabled_withoutBurstCaptureCapability() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedHighResolutionSizes = arrayOf(Size(8000, 6000), Size(8000, 4500))
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )

        val useCase = createUseCaseByResolutionSelector(FAKE_USE_CASE, highResolutionEnabled = true)
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)

        // Checks 8000x6000 is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(Size(4032, 3024))
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun highResolutionIsNotSelected_whenHighResolutionIsNotEnabled_targetResolution8000x6000() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            ),
            supportedHighResolutionSizes = arrayOf(Size(8000, 6000), Size(8000, 4500))
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )

        val useCase =
            createUseCaseByResolutionSelector(FAKE_USE_CASE, preferredResolution = Size(8000, 6000))
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)

        // Checks 8000x6000 is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(Size(4032, 3024))
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun highResolutionIsSelected_whenHighResolutionIsEnabled_aspectRatio16x9() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            ),
            supportedHighResolutionSizes = arrayOf(Size(8000, 6000), Size(8000, 4500))
        )
        val supportedSurfaceCombination = SupportedSurfaceCombination(
            context, DEFAULT_CAMERA_ID, cameraManagerCompat!!, mockCamcorderProfileHelper
        )

        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            highResolutionEnabled = true
        )
        val suggestedResolutionMap = getSuggestedResolutionMap(supportedSurfaceCombination, useCase)

        // Checks 8000x6000 is final selected for the use case.
        assertThat(suggestedResolutionMap[useCase]).isEqualTo(Size(8000, 4500))
    }

    /**
     * Sets up camera according to the specified settings and initialize [CameraX].
     *
     * @param cameraId the camera id to be set up. Default value is [DEFAULT_CAMERA_ID].
     * @param hardwareLevel the hardware level of the camera. Default value is
     * [CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY].
     * @param sensorOrientation the sensor orientation of the camera. Default value is
     * [SENSOR_ORIENTATION_90].
     * @param pixelArraySize the active pixel array size of the camera. Default value is
     * [LANDSCAPE_PIXEL_ARRAY_SIZE].
     * @param supportedSizes the supported sizes of the camera. Default value is
     * [DEFAULT_SUPPORTED_SIZES].
     * @param capabilities the capabilities of the camera. Default value is null.
     */
    private fun setupCameraAndInitCameraX(
        cameraId: String = DEFAULT_CAMERA_ID,
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
        sensorOrientation: Int = SENSOR_ORIENTATION_90,
        pixelArraySize: Size = LANDSCAPE_PIXEL_ARRAY_SIZE,
        supportedSizes: Array<Size> = DEFAULT_SUPPORTED_SIZES,
        supportedHighResolutionSizes: Array<Size>? = null,
        capabilities: IntArray? = null
    ) {
        setupCamera(
            cameraId,
            hardwareLevel,
            sensorOrientation,
            pixelArraySize,
            supportedSizes,
            supportedHighResolutionSizes,
            capabilities
        )

        @LensFacing val lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
            CameraCharacteristics.LENS_FACING_BACK
        )
        cameraManagerCompat = CameraManagerCompat.from(context)
        val cameraInfo = FakeCameraInfoInternal(
            cameraId,
            sensorOrientation,
            CameraCharacteristics.LENS_FACING_BACK
        ).apply {
            camcorderProfileProvider = FakeCamcorderProfileProvider.Builder()
                .addProfile(
                    CamcorderProfileUtil.asHighQuality(profileUhd),
                    profileUhd,
                    profileFhd,
                    profileHd,
                    profileSd,
                    CamcorderProfileUtil.asLowQuality(profileSd)
                ).build()
        }

        cameraFactory = FakeCameraFactory().apply {
            insertCamera(lensFacingEnum, cameraId) {
                FakeCamera(cameraId, null, cameraInfo)
            }
        }

        initCameraX()
    }

    /**
     * Initializes the [CameraX].
     */
    private fun initCameraX() {
        val surfaceManagerProvider =
            CameraDeviceSurfaceManager.Provider { context, _, availableCameraIds ->
                Camera2DeviceSurfaceManager(
                    context,
                    mockCamcorderProfileHelper,
                    CameraManagerCompat.from(this@SupportedSurfaceCombinationTest.context),
                    availableCameraIds
                )
            }
        val cameraXConfig = CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setDeviceSurfaceManagerProvider(surfaceManagerProvider)
            .setCameraFactoryProvider { _, _, _ -> cameraFactory!! }
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
        combinationList.forEach { combination ->
            val configList = combination.surfaceConfigList
            val length = configList.size
            if (length <= 1) {
                return@forEach
            }
            for (index in 0 until length) {
                val subConfigurationList = arrayListOf<SurfaceConfig>().apply {
                    addAll(configList)
                    removeAt(index)
                }
                if (!supportedSurfaceCombination.checkSupported(subConfigurationList)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Gets the suggested resolution map by the converted ResolutionSelector use case config which
     * will also be converted when a use case is bound to the lifecycle.
     */
    private fun getSuggestedResolutionMap(
        supportedSurfaceCombination: SupportedSurfaceCombination,
        vararg useCases: UseCase,
        cameraFactory: CameraFactory = this.cameraFactory!!,
        cameraId: String = DEFAULT_CAMERA_ID,
        useCaseConfigFactory: UseCaseConfigFactory = this.useCaseConfigFactory!!
    ): Map<UseCase, Size?> {
        // Generates the use case to new ResolutionSelector use case config map
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory.getCamera(cameraId).cameraInfoInternal,
            listOf(*useCases),
            useCaseConfigFactory
        )
        // Uses the use case config list to get suggested resolutions
        val useCaseConfigResolutionMap = supportedSurfaceCombination.getSuggestedResolutions(
            emptyList(),
            mutableListOf<UseCaseConfig<*>?>().apply { addAll(useCaseToConfigMap.values) }
        )
        val useCaseResolutionMap = mutableMapOf<UseCase, Size?>()
        // Maps the use cases to the suggestion resolutions
        for (useCase in useCases) {
            useCaseResolutionMap[useCase] = useCaseConfigResolutionMap[useCaseToConfigMap[useCase]]
        }
        return useCaseResolutionMap
    }

    /**
     * Gets the supported output sizes by the converted ResolutionSelector use case config which
     * will also be converted when a use case is bound to the lifecycle.
     */
    private fun getSupportedOutputSizes(
        supportedSurfaceCombination: SupportedSurfaceCombination,
        useCase: UseCase,
        cameraId: String = DEFAULT_CAMERA_ID,
        useCaseConfigFactory: UseCaseConfigFactory = this.useCaseConfigFactory!!
    ): List<Size?> {
        // Converts the use case config to new ResolutionSelector config
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(cameraId).cameraInfoInternal,
            listOf(useCase),
            useCaseConfigFactory
        )
        return supportedSurfaceCombination.getSupportedOutputSizes(useCaseToConfigMap[useCase]!!)
    }

    /**
     * Creates [Preview], [ImageCapture], [ImageAnalysis] or FakeUseCase according to the specified
     * settings.
     *
     * @param useCaseType Which of [Preview], [ImageCapture], [ImageAnalysis] and FakeUseCase should
     * be created.
     * @param targetRotation the target rotation setting. Default is UNKNOWN_ROTATION and no target
     * rotation will be set to the created use case.
     * @param targetAspectRatio the target aspect ratio setting. Default is UNKNOWN_ASPECT_RATIO
     * and no target aspect ratio will be set to the created use case.
     * @param targetResolution the target resolution setting which should still be specified in the
     * legacy API approach. The size should be expressed in the coordinate frame after rotating the
     * supported sizes by the target rotation. Default is null.
     * @param maxResolution the max resolution setting. Default is null.
     * @param defaultResolution the default resolution setting. Default is null.
     * @param supportedResolutions the customized supported resolutions. Default is null.
     * @param customOrderedResolutions the custom ordered resolutions. Default is null.
     */
    private fun createUseCaseByLegacyApi(
        useCaseType: Int,
        targetRotation: Int = UNKNOWN_ROTATION,
        targetAspectRatio: Int = UNKNOWN_ASPECT_RATIO,
        targetResolution: Size? = null,
        maxResolution: Size? = null,
        defaultResolution: Size? = null,
        supportedResolutions: List<Pair<Int, Array<Size>>>? = null,
        customOrderedResolutions: List<Size>? = null,
    ): UseCase {
        val builder = when (useCaseType) {
            PREVIEW_USE_CASE -> Preview.Builder()
            IMAGE_CAPTURE_USE_CASE -> ImageCapture.Builder()
            IMAGE_ANALYSIS_USE_CASE -> ImageAnalysis.Builder()
            else -> FakeUseCaseConfig.Builder(UseCaseConfigFactory.CaptureType.IMAGE_CAPTURE)
        }
        if (targetRotation != UNKNOWN_ROTATION) {
            builder.setTargetRotation(targetRotation)
        }
        if (targetAspectRatio != UNKNOWN_ASPECT_RATIO) {
            builder.setTargetAspectRatio(targetAspectRatio)
        }
        targetResolution?.let { builder.setTargetResolution(it) }
        maxResolution?.let { builder.setMaxResolution(it) }
        defaultResolution?.let { builder.setDefaultResolution(it) }
        supportedResolutions?.let { builder.setSupportedResolutions(it) }
        customOrderedResolutions?.let { builder.setCustomOrderedResolutions(it) }
        return builder.build()
    }

    /** Creates a VideoCapture with a default QualitySelector  */
    private fun createVideoCapture(): VideoCapture<TestVideoOutput> {
        return createVideoCapture(VideoSpec.QUALITY_SELECTOR_AUTO)
    }

    /** Creates a VideoCapture with one ore more specific Quality  */
    private fun createVideoCapture(vararg quality: Quality): VideoCapture<TestVideoOutput> {
        return createVideoCapture(QualitySelector.fromOrderedList(listOf(*quality)))
    }

    /** Creates a VideoCapture with a customized QualitySelector  */
    private fun createVideoCapture(qualitySelector: QualitySelector):
        VideoCapture<TestVideoOutput> {
        val mediaSpec = MediaSpec.builder().configureVideo {
            it.setQualitySelector(
                qualitySelector
            )
        }.build()
        val videoOutput = TestVideoOutput()
        videoOutput.mediaSpecObservable.setState(mediaSpec)
        return VideoCapture.withOutput(videoOutput)
    }

    /** A fake implementation of VideoOutput  */
    private class TestVideoOutput : VideoOutput {
        var mediaSpecObservable: MutableStateObservable<MediaSpec> =
            MutableStateObservable.withInitialState(MediaSpec.builder().build())
        var surfaceRequest: SurfaceRequest? = null
        var sourceState: SourceState? = null
        override fun onSurfaceRequested(@NonNull request: SurfaceRequest) {
            surfaceRequest = request
        }
        override fun getMediaSpec() = mediaSpecObservable
        override fun onSourceStateChanged(@NonNull sourceState: SourceState) {
            this.sourceState = sourceState
        }
    }

    private interface UseCaseCreator {
        fun createUseCase(
            useCaseType: Int,
            targetRotation: Int = UNKNOWN_ROTATION,
            preferredAspectRatio: Int = UNKNOWN_ASPECT_RATIO,
            preferredResolution: Size? = null,
            maxResolution: Size? = null,
            highResolutionEnabled: Boolean = false,
            defaultResolution: Size? = null,
            supportedResolutions: List<Pair<Int, Array<Size>>>? = null,
            customOrderedResolutions: List<Size>? = null,
        ): UseCase
    }
}
