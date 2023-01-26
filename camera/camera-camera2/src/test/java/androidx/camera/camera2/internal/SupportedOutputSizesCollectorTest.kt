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
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.os.Build
import android.util.Pair
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.ResolutionSelector
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.SizeCoordinate
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.Configs
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

private const val FAKE_USE_CASE = 0
private const val PREVIEW_USE_CASE = 1
private const val IMAGE_CAPTURE_USE_CASE = 2
private const val IMAGE_ANALYSIS_USE_CASE = 3
private const val UNKNOWN_ASPECT_RATIO = -1
private const val DEFAULT_CAMERA_ID = "0"
private const val SENSOR_ORIENTATION_0 = 0
private const val SENSOR_ORIENTATION_90 = 90
private val LANDSCAPE_PIXEL_ARRAY_SIZE = Size(4032, 3024)
private val PORTRAIT_PIXEL_ARRAY_SIZE = Size(3024, 4032)
private val DISPLAY_SIZE = Size(720, 1280)
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

/** Robolectric test for [SupportedOutputSizesCollector] class */
@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SupportedOutputSizesCollectorTest(
    private val sizeCoordinate: SizeCoordinate
) {
    private val mockCamcorderProfileHelper = Mockito.mock(CamcorderProfileHelper::class.java)
    private lateinit var cameraManagerCompat: CameraManagerCompat
    private lateinit var cameraCharacteristicsCompat: CameraCharacteristicsCompat
    private lateinit var displayInfoManager: DisplayInfoManager
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var cameraFactory: FakeCameraFactory? = null
    private var useCaseConfigFactory: UseCaseConfigFactory? = null

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

        displayInfoManager = DisplayInfoManager.getInstance(context)
    }

    @After
    fun tearDown() {
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio4x3() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_4_3
        )

        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio16x9_InLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )

        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_aspectRatio16x9_inLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList: List<Size> = if (Build.VERSION.SDK_INT == 21) {
            listOf(
                // Matched maximum JPEG resolution AspectRatio items, sorted by area size.
                Size(4032, 3024),
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                Size(320, 240),
                // Mismatched maximum JPEG resolution AspectRatio items, sorted by area size.
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(320, 180),
                Size(256, 144)
            )
        } else {
            listOf(
                // Matched preferred AspectRatio items, sorted by area size.
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(320, 180),
                Size(256, 144),
                // Mismatched preferred AspectRatio items, sorted by area size.
                Size(4032, 3024),
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                Size(320, 240)
            )
        }
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_preferredResolution1920x1080_InLimitedDevice() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(1920, 1080),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        // The 4:3 default aspect ratio will make sizes of 4/3 have the 2nd highest priority just
        // after the preferred resolution.
        val expectedList =
            listOf(
                // Matched preferred resolution size will be put in first priority.
                Size(1920, 1080),
                // Matched default preferred AspectRatio items, sorted by area size.
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                Size(320, 240),
                // Mismatched default preferred AspectRatio items, sorted by area size.
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(320, 180),
                Size(256, 144),
            )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_preferredResolution1920x1080_InLegacyDevice() {
        setupCameraAndInitCameraX()
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(1920, 1080),
            sizeCoordinate = sizeCoordinate
        )

        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        // The 4:3 default aspect ratio will make sizes of 4/3 have the 2nd highest priority just
        // after the preferred resolution.
        val expectedList = if (Build.VERSION.SDK_INT == 21) {
                listOf(
                    // Matched maximum JPEG resolution AspectRatio items, sorted by area size.
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    // Mismatched maximum JPEG resolution AspectRatio items, sorted by area size.
                    Size(1920, 1080),
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144)
                )
            } else {
                // The 4:3 default aspect ratio will make sizes of 4/3 have the 2nd highest
                // priority just after the preferred resolution size.
                listOf(
                    // Matched default preferred resolution size will be put in first priority.
                    Size(1920, 1080),
                    // Matched preferred AspectRatio items, sorted by area size.
                    Size(1920, 1440),
                    Size(1280, 960),
                    Size(640, 480),
                    Size(320, 240),
                    // Mismatched preferred default AspectRatio items, sorted by area size.
                    Size(1280, 720),
                    Size(960, 544),
                    Size(800, 450),
                    Size(320, 180),
                    Size(256, 144),
                )
            }
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    @Suppress("DEPRECATION") /* defaultDisplay */
    fun getSupportedOutputSizes_smallDisplay_withMaxResolution1920x1080() {
        // Sets up small display.
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(240)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(320)
        displayInfoManager = DisplayInfoManager.getInstance(context)

        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )

        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            PREVIEW_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            maxResolution = Size(1920, 1080)
        )
        // Max resolution setting will remove sizes larger than 1920x1080. The auto-resolution
        // mechanism will try to select the sizes which aspect ratio is nearest to the aspect ratio
        // of target resolution in priority. Therefore, sizes of aspect ratio 16/9 will be in front
        // of the returned sizes list and the list is sorted in descending order. Other items will
        // be put in the following that are sorted by aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144),

            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_preferredResolution1800x1440NearTo4x3() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(1800, 1440),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList =
            listOf(
                // No matched preferred resolution size found.
                // Matched default preferred AspectRatio items, sorted by area size.
                Size(1920, 1440),
                Size(1280, 960),
                Size(640, 480),
                Size(320, 240),
                // Mismatched default preferred AspectRatio items, sorted by area size.
                Size(3840, 2160),
                Size(1920, 1080),
                Size(1280, 720),
                Size(960, 544),
                Size(800, 450),
                Size(320, 180),
                Size(256, 144)
            )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_preferredResolution1280x600NearTo16x9() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(1280, 600),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // No matched preferred resolution size found.
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_maxResolution1280x720() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            maxResolution = Size(1280, 720)
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_maxResolution720x1280() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            maxResolution = Size(720, 1280)
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_defaultResolution1280x720_noTargetResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            defaultResolution = Size(1280, 720)
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_defaultResolution1280x720_preferredResolution1920x1080() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            defaultResolution = Size(1280, 720),
            preferredResolution = Size(1920, 1080),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred resolution size will be put in first priority.
            Size(1920, 1080),
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144)
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
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(1920, 1080),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // No matched preferred resolution size found.
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(320, 180),
            Size(256, 144)
        )
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
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(320, 240),
            sizeCoordinate = sizeCoordinate,
            maxResolution = Size(320, 180)
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(Size(320, 180), Size(256, 144))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenMaxSizeSmallerThanBigPreferredResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(3840, 2160),
            sizeCoordinate = sizeCoordinate,
            maxResolution = Size(1920, 1080)
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // No matched preferred resolution size found after filtering by max resolution setting.
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenNoSizeBetweenMaxSizeAndPreferredResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(320, 190),
            sizeCoordinate = sizeCoordinate,
            maxResolution = Size(320, 200)
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(Size(320, 180), Size(256, 144))
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenPreferredResolutionSmallerThanAnySize() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            supportedSizes = arrayOf(
                Size(640, 480),
                Size(320, 240),
                Size(320, 180),
                Size(256, 144)
            )
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(192, 144),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
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
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            maxResolution = Size(192, 144)
        )
        // All sizes will be filtered out by the max resolution 192x144 setting and an
        // IllegalArgumentException will be thrown.
        Assert.assertThrows(IllegalArgumentException::class.java) {
            getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
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
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(185, 90),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // No matched preferred resolution size found.
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(320, 240),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(256, 144),
            Size(296, 144)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Test
    fun getSupportedOutputSizes_whenOneMod16SizeClosestToTargetResolution() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
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
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredResolution = Size(1080, 2016),
            sizeCoordinate = sizeCoordinate
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // No matched preferred resolution size found.
            // Matched default preferred AspectRatio items, sorted by area size.
            Size(1440, 1080),
            Size(1280, 960),
            Size(640, 480),
            Size(480, 360),
            Size(384, 288),
            // Mismatched default preferred AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            Size(864, 480),
            Size(768, 432),
            Size(640, 360)
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
                Size(960, 540),
                Size(480, 640),
                Size(640, 480),
                Size(360, 480)
            )
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(1080, 1920),
            Size(720, 1280),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(1080, 1440),
            Size(960, 1280),
            Size(480, 640),
            Size(360, 480),
            Size(640, 480),
            Size(960, 540)
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
                Size(960, 540),
                Size(480, 640),
                Size(640, 480),
                Size(360, 480)
            )
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        // Due to the pixel array size is portrait, sizes of aspect ratio 9/16 will be in front of
        // the returned sizes list and the list is sorted in descending order. Other items will be
        // put in the following that are sorted by aspect ratio delta and then area size.
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(1080, 1920),
            Size(720, 1280),
            // Mismatched preferred AspectRatio items, sorted by aspect ratio delta then area size.
            Size(1080, 1440),
            Size(960, 1280),
            Size(480, 640),
            Size(360, 480),
            Size(640, 480),
            Size(960, 540)
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
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240)
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
                Size(540, 960),
                Size(640, 480),
                Size(480, 640),
                Size(480, 360)
            )
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )
        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(1920, 1080),
            Size(1280, 720),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(1440, 1080),
            Size(1280, 960),
            Size(640, 480),
            Size(480, 360),
            Size(480, 640),
            Size(540, 960)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun getSupportedOutputSizes_whenHighResolutionIsEnabled_aspectRatio16x9() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            ),
            supportedHighResolutionSizes = arrayOf(Size(8000, 6000), Size(8000, 4500))
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )

        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            highResolutionEnabled = true
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(8000, 4500),
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(8000, 6000),
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240)
        )
        assertThat(resultList).isEqualTo(expectedList)
    }

    @Config(minSdk = Build.VERSION_CODES.M)
    @Test
    fun highResolutionCanNotBeSelected_whenHighResolutionForceDisabled() {
        setupCameraAndInitCameraX(
            hardwareLevel = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            capabilities = intArrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE
            ),
            supportedHighResolutionSizes = arrayOf(Size(8000, 6000), Size(8000, 4500))
        )
        val supportedOutputSizesCollector = SupportedOutputSizesCollector(
            DEFAULT_CAMERA_ID,
            cameraCharacteristicsCompat,
            displayInfoManager
        )

        val useCase = createUseCaseByResolutionSelector(
            FAKE_USE_CASE,
            preferredAspectRatio = AspectRatio.RATIO_16_9,
            highResolutionEnabled = true,
            highResolutionForceDisabled = true
        )
        val resultList = getSupportedOutputSizes(supportedOutputSizesCollector, useCase)
        val expectedList = listOf(
            // Matched preferred AspectRatio items, sorted by area size.
            Size(3840, 2160),
            Size(1920, 1080),
            Size(1280, 720),
            Size(960, 544),
            Size(800, 450),
            Size(320, 180),
            Size(256, 144),
            // Mismatched preferred AspectRatio items, sorted by area size.
            Size(4032, 3024),
            Size(1920, 1440),
            Size(1280, 960),
            Size(640, 480),
            Size(320, 240)
        )
        assertThat(resultList).isEqualTo(expectedList)
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

        @CameraSelector.LensFacing val lensFacingEnum = CameraUtil.getLensFacingEnumFromInt(
            CameraCharacteristics.LENS_FACING_BACK
        )
        cameraManagerCompat = CameraManagerCompat.from(context)
        val cameraInfo = FakeCameraInfoInternal(
            cameraId,
            sensorOrientation,
            CameraCharacteristics.LENS_FACING_BACK
        )

        cameraFactory = FakeCameraFactory().apply {
            insertCamera(lensFacingEnum, cameraId) {
                FakeCamera(cameraId, null, cameraInfo)
            }
        }

        cameraCharacteristicsCompat = cameraManagerCompat.getCameraCharacteristicsCompat(cameraId)

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
                    CameraManagerCompat.from(this@SupportedOutputSizesCollectorTest.context),
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

    /**
     * Gets the supported output sizes by the converted ResolutionSelector use case config which
     * will also be converted when a use case is bound to the lifecycle.
     */
    private fun getSupportedOutputSizes(
        supportedOutputSizesCollector: SupportedOutputSizesCollector,
        useCase: UseCase,
        cameraId: String = DEFAULT_CAMERA_ID,
        sensorOrientation: Int = SENSOR_ORIENTATION_90,
        useCaseConfigFactory: UseCaseConfigFactory = this.useCaseConfigFactory!!
    ): List<Size?> {
        // Converts the use case config to new ResolutionSelector config
        val useCaseToConfigMap = Configs.useCaseConfigMapWithDefaultSettingsFromUseCaseList(
            cameraFactory!!.getCamera(cameraId).cameraInfoInternal,
            listOf(useCase),
            useCaseConfigFactory
        )

        val useCaseConfig = useCaseToConfigMap[useCase]!!
        val resolutionSelector = (useCaseConfig as ImageOutputConfig).resolutionSelector
        val imageFormat = useCaseConfig.inputFormat
        val isHighResolutionDisabled = useCaseConfig.isHigResolutionDisabled(false)
        val customizedSupportSizes = getCustomizedSupportSizesFromConfig(imageFormat, useCaseConfig)
        val miniBoundingSize = SupportedOutputSizesCollector.getTargetSizeByResolutionSelector(
            resolutionSelector,
            Surface.ROTATION_0,
            sensorOrientation,
            CameraCharacteristics.LENS_FACING_BACK
        ) ?: useCaseConfig.getDefaultResolution(null)

        return supportedOutputSizesCollector.getSupportedOutputSizes(
            resolutionSelector,
            imageFormat,
            miniBoundingSize,
            isHighResolutionDisabled,
            customizedSupportSizes
        )
    }

    companion object {

        /**
         * Sets up camera according to the specified settings.
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
        @JvmStatic
        fun setupCamera(
            cameraId: String = DEFAULT_CAMERA_ID,
            hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            sensorOrientation: Int = SENSOR_ORIENTATION_90,
            pixelArraySize: Size = LANDSCAPE_PIXEL_ARRAY_SIZE,
            supportedSizes: Array<Size> = DEFAULT_SUPPORTED_SIZES,
            supportedHighResolutionSizes: Array<Size>? = null,
            capabilities: IntArray? = null
        ) {
            val mockMap = Mockito.mock(StreamConfigurationMap::class.java).also {
                // Sets up the supported sizes
                Mockito.`when`(it.getOutputSizes(ArgumentMatchers.anyInt()))
                    .thenReturn(supportedSizes)
                // ImageFormat.PRIVATE was supported since API level 23. Before that, the supported
                // output sizes need to be retrieved via SurfaceTexture.class.
                Mockito.`when`(it.getOutputSizes(SurfaceTexture::class.java))
                    .thenReturn(supportedSizes)
                // This is setup for the test to determine RECORD size from StreamConfigurationMap
                Mockito.`when`(it.getOutputSizes(MediaRecorder::class.java))
                    .thenReturn(supportedSizes)

                // setup to return different minimum frame durations depending on resolution
                // minimum frame durations were designated only for the purpose of testing
                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(4032, 3024))))
                    .thenReturn(50000000L) // 20 fps, size maximum

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(3840, 2160))))
                    .thenReturn(40000000L) // 25, size record

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(1920, 1440))))
                    .thenReturn(30000000L) // 30

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(1920, 1080))))
                    .thenReturn(28000000L) // 35

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(1280, 960))))
                    .thenReturn(25000000L) // 40

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(1280, 720))))
                    .thenReturn(22000000L) // 45, size preview/display

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(960, 544))))
                    .thenReturn(20000000L) // 50

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(800, 450))))
                    .thenReturn(16666000L) // 60fps

                Mockito.`when`(it.getOutputMinFrameDuration(anyInt(), eq(Size(640, 480))))
                    .thenReturn(16666000L) // 60fps

                // Sets up the supported high resolution sizes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Mockito.`when`(it.getHighResolutionOutputSizes(ArgumentMatchers.anyInt()))
                        .thenReturn(supportedHighResolutionSizes)
                }
            }

            val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
            Shadow.extract<ShadowCameraCharacteristics>(characteristics).apply {
                set(CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK)
                set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, hardwareLevel)
                set(CameraCharacteristics.SENSOR_ORIENTATION, sensorOrientation)
                set(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, pixelArraySize)
                set(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP, mockMap)
                capabilities?.let {
                    set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, it)
                }
            }

            val cameraManager = ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE) as CameraManager
            (Shadow.extract<Any>(cameraManager) as ShadowCameraManager)
                .addCamera(cameraId, characteristics)
        }

        /**
         * Creates [Preview], [ImageCapture], [ImageAnalysis], [androidx.camera.core.VideoCapture] or
         * FakeUseCase by the legacy or new ResolutionSelector API according to the specified settings.
         *
         * @param useCaseType Which of [Preview], [ImageCapture], [ImageAnalysis],
         * [androidx.camera.core.VideoCapture] and FakeUseCase should be created.
         * @param preferredAspectRatio the target aspect ratio setting. Default is UNKNOWN_ASPECT_RATIO
         * and no target aspect ratio will be set to the created use case.
         * @param preferredResolution the preferred resolution setting which should be specified in the
         * camera sensor coordinate. The resolution will be transformed to set via
         * [ResolutionSelector.Builder.setPreferredResolutionByViewSize] if size coordinate is
         * [SizeCoordinate.ANDROID_VIEW]. Default is null.
         * @param maxResolution the max resolution setting. Default is null.
         * @param highResolutionEnabled the high resolution setting, Default is false.
         * @param highResolutionForceDisabled the high resolution force disabled setting, Default
         * is false. This will be set in the use case config to force disable high resolution.
         * @param defaultResolution the default resolution setting. Default is null.
         * @param supportedResolutions the customized supported resolutions. Default is null.
         */
        @JvmStatic
        fun createUseCaseByResolutionSelector(
            useCaseType: Int,
            preferredAspectRatio: Int = UNKNOWN_ASPECT_RATIO,
            preferredResolution: Size? = null,
            sizeCoordinate: SizeCoordinate = SizeCoordinate.CAMERA_SENSOR,
            maxResolution: Size? = null,
            highResolutionEnabled: Boolean = false,
            highResolutionForceDisabled: Boolean = false,
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

            val resolutionSelectorBuilder = ResolutionSelector.Builder()

            if (preferredAspectRatio != UNKNOWN_ASPECT_RATIO) {
                resolutionSelectorBuilder.setPreferredAspectRatio(preferredAspectRatio)
            }

            preferredResolution?.let {
                if (sizeCoordinate == SizeCoordinate.CAMERA_SENSOR) {
                    resolutionSelectorBuilder.setPreferredResolution(it)
                } else {
                    val flippedResolution = Size(
                        /* width= */ it.height,
                        /* height= */ it.width
                    )
                    resolutionSelectorBuilder.setPreferredResolutionByViewSize(flippedResolution)
                }
            }

            maxResolution?.let { resolutionSelectorBuilder.setMaxResolution(it) }
            resolutionSelectorBuilder.setHighResolutionEnabled(highResolutionEnabled)

            builder.setResolutionSelector(resolutionSelectorBuilder.build())
            builder.setHighResolutionDisabled(highResolutionForceDisabled)

            defaultResolution?.let { builder.setDefaultResolution(it) }
            supportedResolutions?.let { builder.setSupportedResolutions(it) }
            customOrderedResolutions?.let { builder.setCustomOrderedResolutions(it) }
            return builder.build()
        }

        @JvmStatic
        fun getCustomizedSupportSizesFromConfig(
            imageFormat: Int,
            config: ImageOutputConfig
        ): Array<Size>? {
            var outputSizes: Array<Size>? = null

            // Try to retrieve customized supported resolutions from config.
            val formatResolutionsPairList = config.getSupportedResolutions(null)
            if (formatResolutionsPairList != null) {
                for (formatResolutionPair in formatResolutionsPairList) {
                    if (formatResolutionPair.first == imageFormat) {
                        outputSizes = formatResolutionPair.second
                        break
                    }
                }
            }
            return outputSizes
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "sizeCoordinate = {0}")
        fun data() = listOf(
            SizeCoordinate.CAMERA_SENSOR,
            SizeCoordinate.ANDROID_VIEW
        )
    }
}