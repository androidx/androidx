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

@file:RequiresApi(21)

package androidx.camera.camera2.internal

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Pair
import android.util.Rational
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.internal.Camera2CameraControlImpl.CaptureResultListener
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.quirk.AfRegionFlipHorizontallyQuirk
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal.ControlUpdateCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLooper

private const val CAMERA0_ID = "0" // 640x480 sensor size
private const val CAMERA1_ID = "1" // 1920x1080 sensor size
private const val CAMERA2_ID = "2" // 640x480 sensor size, not support AF_AUTO.
private const val CAMERA3_ID = "3" // camera that does not support 3A regions.

private const val SENSOR_WIDTH = 640
private const val SENSOR_HEIGHT = 480
private const val SENSOR_WIDTH2 = 1920
private const val SENSOR_HEIGHT2 = 1080

private val AREA_WIDTH = (MeteringPointFactory.getDefaultPointSize() * SENSOR_WIDTH).toInt()
private val AREA_HEIGHT = (MeteringPointFactory.getDefaultPointSize() * SENSOR_HEIGHT).toInt()
private val AREA_WIDTH2 = (MeteringPointFactory.getDefaultPointSize() * SENSOR_WIDTH2).toInt()
private val AREA_HEIGHT2 = (MeteringPointFactory.getDefaultPointSize() * SENSOR_HEIGHT2).toInt()

private val M_RECT_1 = Rect(0, 0, AREA_WIDTH / 2, AREA_HEIGHT / 2)
private val M_RECT_2 = Rect(0, SENSOR_HEIGHT - AREA_HEIGHT / 2, AREA_WIDTH / 2, SENSOR_HEIGHT)
private val M_RECT_3 = Rect(
    SENSOR_WIDTH - AREA_WIDTH / 2,
    SENSOR_HEIGHT - AREA_HEIGHT / 2,
    SENSOR_WIDTH,
    SENSOR_HEIGHT
)

private val PREVIEW_ASPECT_RATIO_4_X_3 = Rational(4, 3)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FocusMeteringControlTest(private val template: Int) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "template={0}")
        fun data() = listOf(
            CameraDevice.TEMPLATE_PREVIEW,
            CameraDevice.TEMPLATE_RECORD
        )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val pointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
    private lateinit var focusMeteringControl: FocusMeteringControl

    private val point1 = pointFactory.createPoint(0f, 0f)
    private val point2 = pointFactory.createPoint(0.0f, 1.0f)
    private val point3 = pointFactory.createPoint(1.0f, 1.0f)

    private lateinit var camera2CameraControlImpl: Camera2CameraControlImpl

    @Before
    fun setUp() {
        initCameras()
        focusMeteringControl = spy(initFocusMeteringControl(CAMERA0_ID))
        focusMeteringControl.setActive(true)
        focusMeteringControl.setTemplate(template)
    }

    private fun initFocusMeteringControl(cameraID: String): FocusMeteringControl {
        return initFocusMeteringControl(cameraID, Quirks(ArrayList()))
    }

    private fun initFocusMeteringControl(
        cameraId: String,
        cameraQuirks: Quirks
    ): FocusMeteringControl {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraCharacteristics = CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            cameraManager.getCameraCharacteristics(cameraId),
            cameraId
        )
        val updateCallback = Mockito.mock(ControlUpdateCallback::class.java)

        camera2CameraControlImpl = spy(
            Camera2CameraControlImpl(
                cameraCharacteristics,
                CameraXExecutors.mainThreadExecutor(),
                CameraXExecutors.directExecutor(),
                updateCallback
            )
        )

        return FocusMeteringControl(
            camera2CameraControlImpl,
            CameraXExecutors.mainThreadExecutor(), CameraXExecutors.directExecutor(),
            cameraQuirks
        ).apply {
            setActive(true)
            setPreviewAspectRatio(PREVIEW_ASPECT_RATIO_4_X_3)
        }
    }

    private fun initCameras() {
        // **** Camera 0 characteristics (640X480 sensor size)****//
        val characteristics0 = ShadowCameraCharacteristics.newCameraCharacteristics()
        Shadow.extract<ShadowCameraCharacteristics>(characteristics0).apply {
            set(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT)
            )
            set(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, intArrayOf(
                    CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
                    CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    CaptureResult.CONTROL_AF_MODE_AUTO,
                    CaptureResult.CONTROL_AF_MODE_OFF
                )
            )
            set(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, intArrayOf(
                    CaptureResult.CONTROL_AE_MODE_ON,
                    CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                    CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH,
                    CaptureResult.CONTROL_AE_MODE_OFF
                )
            )
            set(
                CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, intArrayOf(
                    CaptureResult.CONTROL_AWB_MODE_AUTO,
                    CaptureResult.CONTROL_AWB_MODE_OFF
                )
            )
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
            )
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 3)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 3)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 1)
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            context.getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera(CAMERA0_ID, characteristics0)

        // **** Camera 1 characteristics (1920x1080 sensor size) ****//
        val characteristics1 = ShadowCameraCharacteristics.newCameraCharacteristics()
        Shadow.extract<ShadowCameraCharacteristics>(characteristics1).apply {
            set(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                Rect(0, 0, SENSOR_WIDTH2, SENSOR_HEIGHT2)
            )
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            )
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 1)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 1)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 1)
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            context.getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera(CAMERA1_ID, characteristics1)

        // **** Camera 2 characteristics (640x480 sensor size, does not support AF_AUTO ****//
        val characteristics2 = ShadowCameraCharacteristics.newCameraCharacteristics()
        Shadow.extract<ShadowCameraCharacteristics>(characteristics2).apply {
            set(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT)
            )
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            )
            set(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, intArrayOf(
                    CaptureResult.CONTROL_AF_MODE_OFF
                )
            )
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 1)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 1)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 1)
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            context.getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera(CAMERA2_ID, characteristics2)

        // ** Camera 3 characteristics (640x480 sensor size, does not support any 3A regions //
        val characteristics3 = ShadowCameraCharacteristics.newCameraCharacteristics()
        Shadow.extract<ShadowCameraCharacteristics>(characteristics3).apply {
            set(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT)
            )
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3
            )
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 0)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 0)
            set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 0)
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            context.getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera(CAMERA3_ID, characteristics3)
    }

    private fun getAfRects(control: FocusMeteringControl) = Camera2ImplConfig.Builder().apply {
        control.addFocusMeteringOptions(this)
    }.build().getCaptureRequestOption(CaptureRequest.CONTROL_AF_REGIONS, arrayOf())!!

    private fun getAeRects(control: FocusMeteringControl) = Camera2ImplConfig.Builder().apply {
        control.addFocusMeteringOptions(this)
    }.build().getCaptureRequestOption(CaptureRequest.CONTROL_AE_REGIONS, arrayOf())!!

    private fun getAwbRects(control: FocusMeteringControl) = Camera2ImplConfig.Builder().apply {
        control.addFocusMeteringOptions(this)
    }.build().getCaptureRequestOption(CaptureRequest.CONTROL_AWB_REGIONS, arrayOf())!!

    @Test
    fun addFocusMeteringOptions_hasCorrectAfMode() {
        verifyAfMode(focusMeteringControl.defaultAfMode)
    }

    @Test
    fun triggerAfWithTemplate() {
        focusMeteringControl.triggerAf(null, false)
        verifyTemplate(template)
    }

    @Test
    fun triggerAePrecaptureWithTemplate() {
        focusMeteringControl.triggerAePrecapture(null)
        verifyTemplate(template)
    }

    @Test
    fun cancelAfAeTriggerWithTemplate() {
        focusMeteringControl.cancelAfAeTrigger(true, true)
        verifyTemplate(template)
    }

    @Test
    fun startFocusAndMetering_invalidPoint() {
        val invalidPoint = pointFactory.createPoint(1f, 1.1f)
        focusMeteringControl.setPreviewAspectRatio(Rational(16, 9))
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(invalidPoint).build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val aeRects = getAeRects(focusMeteringControl)
        val awbRects = getAwbRects(focusMeteringControl)
        assertThat(afRects.size).isEqualTo(0)
        assertThat(aeRects.size).isEqualTo(0)
        assertThat(awbRects.size).isEqualTo(0)
    }

    @Test
    fun startFocusAndMetering_defaultPoint_3ARectsAreCorrect() {
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1).build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val aeRects = getAeRects(focusMeteringControl)
        val awbRects = getAwbRects(focusMeteringControl)
        assertThat(afRects.size).isEqualTo(1)
        assertThat(afRects[0].rect).isEqualTo(M_RECT_1)
        assertThat(aeRects.size).isEqualTo(1)
        assertThat(aeRects[0].rect).isEqualTo(M_RECT_1)
        assertThat(awbRects.size).isEqualTo(1)
        assertThat(awbRects[0].rect).isEqualTo(M_RECT_1)
    }

    @Test
    fun startFocusAndMetering_multiplePoint_3ARectsAreCorrect() {
        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1)
                .addPoint(point2)
                .addPoint(point3)
                .build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val aeRects = getAeRects(focusMeteringControl)
        val awbRects = getAwbRects(focusMeteringControl)
        assertThat(afRects.size).isEqualTo(3)
        assertThat(afRects[0].rect).isEqualTo(M_RECT_1)
        assertThat(afRects[1].rect).isEqualTo(M_RECT_2)
        assertThat(afRects[2].rect).isEqualTo(M_RECT_3)
        assertThat(aeRects.size).isEqualTo(3)
        assertThat(aeRects[0].rect).isEqualTo(M_RECT_1)
        assertThat(aeRects[1].rect).isEqualTo(M_RECT_2)
        assertThat(aeRects[2].rect).isEqualTo(M_RECT_3)
        assertThat(awbRects.size).isEqualTo(1)
        assertThat(awbRects[0].rect).isEqualTo(M_RECT_1)
    }

    @Test
    fun startFocusAndMetering_AfRegionCorrectedByQuirk() {
        focusMeteringControl = initFocusMeteringControl(
            CAMERA0_ID,
            Quirks(listOf(AfRegionFlipHorizontallyQuirk()))
        )
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1)
                .addPoint(point2)
                .addPoint(point3)
                .build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val aeRects = getAeRects(focusMeteringControl)
        val awbRects = getAwbRects(focusMeteringControl)

        // after flipping horizontally, left / right will be swapped.
        val flippedRect1 = Rect(
            SENSOR_WIDTH - M_RECT_1.right, M_RECT_1.top,
            SENSOR_WIDTH - M_RECT_1.left, M_RECT_1.bottom
        )
        val flippedRect2 = Rect(
            SENSOR_WIDTH - M_RECT_2.right, M_RECT_2.top,
            SENSOR_WIDTH - M_RECT_2.left, M_RECT_2.bottom
        )
        val flippedRect3 = Rect(
            SENSOR_WIDTH - M_RECT_3.right, M_RECT_3.top,
            SENSOR_WIDTH - M_RECT_3.left, M_RECT_3.bottom
        )
        assertThat(afRects.size).isEqualTo(3)
        assertThat(afRects[0].rect).isEqualTo(flippedRect1)
        assertThat(afRects[1].rect).isEqualTo(flippedRect2)
        assertThat(afRects[2].rect).isEqualTo(flippedRect3)
        assertThat(aeRects.size).isEqualTo(3)
        assertThat(aeRects[0].rect).isEqualTo(M_RECT_1)
        assertThat(aeRects[1].rect).isEqualTo(M_RECT_2)
        assertThat(aeRects[2].rect).isEqualTo(M_RECT_3)
        assertThat(awbRects.size).isEqualTo(1)
        assertThat(awbRects[0].rect).isEqualTo(M_RECT_1)
    }

    @Test
    fun startFocusAndMetering_multiplePointVariousModes() {
        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AWB)
                .addPoint(point2, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .addPoint(
                    point3,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                        FocusMeteringAction.FLAG_AWB
                )
                .build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val aeRects = getAeRects(focusMeteringControl)
        val awbRects = getAwbRects(focusMeteringControl)
        assertThat(afRects.size).isEqualTo(2)
        assertThat(afRects[0].rect).isEqualTo(M_RECT_2)
        assertThat(afRects[1].rect).isEqualTo(M_RECT_3)
        assertThat(aeRects.size).isEqualTo(2)
        assertThat(aeRects[0].rect).isEqualTo(M_RECT_2)
        assertThat(aeRects[1].rect).isEqualTo(M_RECT_3)
        assertThat(awbRects.size).isEqualTo(1)
        assertThat(awbRects[0].rect).isEqualTo(M_RECT_1)
    }

    @Test
    fun startFocusAndMetering_multiplePointVariousModes2() {
        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF)
                .addPoint(point2, FocusMeteringAction.FLAG_AWB)
                .addPoint(point3, FocusMeteringAction.FLAG_AE)
                .build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val aeRects = getAeRects(focusMeteringControl)
        val awbRects = getAwbRects(focusMeteringControl)
        assertThat(afRects.size).isEqualTo(1)
        assertThat(afRects[0].rect).isEqualTo(M_RECT_1)
        assertThat(aeRects.size).isEqualTo(1)
        assertThat(aeRects[0].rect).isEqualTo(M_RECT_3)
        assertThat(awbRects.size).isEqualTo(1)
        assertThat(awbRects[0].rect).isEqualTo(M_RECT_2)
    }

    @Test
    fun cropRegionIsSet_resultBasedOnCropRegion() {
        val cropWidth = 480
        val cropHeight = 360
        val cropRect = Rect(
            SENSOR_WIDTH / 2 - cropWidth / 2,
            SENSOR_HEIGHT / 2 - cropHeight / 2,
            SENSOR_WIDTH / 2 + cropWidth / 2, SENSOR_HEIGHT / 2 + cropHeight / 2
        )
        Mockito.`when`(camera2CameraControlImpl.cropSensorRegion).thenReturn(cropRect)
        val centerPt = pointFactory.createPoint(0.5f, 0.5f)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(centerPt).build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val areaWidth = (MeteringPointFactory.getDefaultPointSize() * cropRect.width()).toInt()
        val areaHeight = (MeteringPointFactory.getDefaultPointSize() * cropRect.height()).toInt()
        val adjustedRect = Rect(
            cropRect.centerX() - areaWidth / 2,
            cropRect.centerY() - areaHeight / 2,
            cropRect.centerX() + areaWidth / 2,
            cropRect.centerY() + areaHeight / 2
        )
        assertThat(afRects[0].rect).isEqualTo(adjustedRect)
    }

    @Test
    fun previewFovAdjusted_16by9_to_4by3() {
        // use 16:9 preview aspect ratio
        val previewAspectRatio = Rational(16, 9)
        focusMeteringControl.setPreviewAspectRatio(previewAspectRatio)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1).build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val adjustedRect = Rect(0, 60 - AREA_HEIGHT / 2, AREA_WIDTH / 2, 60 + AREA_HEIGHT / 2)
        assertThat(afRects[0].rect).isEqualTo(adjustedRect)
    }

    @Test
    fun previewFovAdjusted_4by3_to_16by9() {
        // Camera1 sensor region is 16:9
        focusMeteringControl = initFocusMeteringControl(CAMERA1_ID)
        focusMeteringControl.setPreviewAspectRatio(PREVIEW_ASPECT_RATIO_4_X_3)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1).build()
        )
        val afRects = getAfRects(focusMeteringControl)
        val adjustedRect = Rect(
            240 - AREA_WIDTH2 / 2, 0, 240 + AREA_WIDTH2 / 2,
            AREA_HEIGHT2 / 2
        )
        assertThat(afRects[0].rect).isEqualTo(adjustedRect)
    }

    @Test
    fun customFovAdjusted() {
        // 16:9 to 4:3
        val useCase = Mockito.mock(UseCase::class.java)
        Mockito.`when`(useCase.attachedSurfaceResolution).thenReturn(
            Size(1920, 1080)
        )
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f, useCase)
        val point = factory.createPoint(0f, 0f)
        focusMeteringControl.setPreviewAspectRatio(PREVIEW_ASPECT_RATIO_4_X_3)
        focusMeteringControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
        val afRects = getAfRects(focusMeteringControl)
        val adjustedRect = Rect(0, 60 - AREA_HEIGHT / 2, AREA_WIDTH / 2, 60 + AREA_HEIGHT / 2)
        assertThat(afRects[0].rect).isEqualTo(adjustedRect)
    }

    @Test
    fun pointSize_ConvertedCorrect() {
        val point1 = pointFactory.createPoint(0.5f, 0.5f, 1.0f)
        val point2 = pointFactory.createPoint(0.5f, 0.5f, 0.5f)
        val point3 = pointFactory.createPoint(0.5f, 0.5f, 0.1f)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1)
                .addPoint(point2)
                .addPoint(point3).build()
        )
        val afRects = getAfRects(focusMeteringControl)
        assertThat(afRects.size).isEqualTo(3)
        assertThat(afRects[0].width).isEqualTo((SENSOR_WIDTH * 1.0f).toInt())
        assertThat(afRects[0].height).isEqualTo((SENSOR_HEIGHT * 1.0f).toInt())
        assertThat(afRects[1].width).isEqualTo((SENSOR_WIDTH * 0.5f).toInt())
        assertThat(afRects[1].height).isEqualTo((SENSOR_HEIGHT * 0.5f).toInt())
        assertThat(afRects[2].width).isEqualTo((SENSOR_WIDTH * 0.1f).toInt())
        assertThat(afRects[2].height).isEqualTo((SENSOR_HEIGHT * 0.1f).toInt())
    }

    @Test
    fun withAFPoints_AFIsTriggered() {
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(
                point1,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                    FocusMeteringAction.FLAG_AWB
            ).build()
        )
        verify(focusMeteringControl).triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF).build()
        )
        verify(focusMeteringControl)
            .triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(
                point1,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            ).build()
        )
        verify(focusMeteringControl).triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(
                point1,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AWB
            ).build()
        )
        verify(focusMeteringControl).triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
    }

    @Test
    fun withoutAFPoints_AFIsNotTriggered() {
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AE).build()
        )
        verify(focusMeteringControl, never())
            .triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AWB).build()
        )
        verify(focusMeteringControl, never())
            .triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AE).build()
        )
        verify(focusMeteringControl, never())
            .triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(
                point1,
                FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            ).build()
        )
        verify(focusMeteringControl, never())
            .triggerAf(ArgumentMatchers.any(), ArgumentMatchers.eq(true))
        reset(focusMeteringControl)
    }

    @Test
    fun updateSessionConfigIsCalled() {
        focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1).build()
        )
        verify(camera2CameraControlImpl, Mockito.times(1)).updateSessionConfigSynchronous()
    }

    @MediumTest
    @Test
    fun autoCancelDuration_completeWithIsFocusSuccessfulFalse() {
        focusMeteringControl = spy(focusMeteringControl)
        val autoCancelTimeOutDuration: Long = 500
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelTimeOutDuration, TimeUnit.MILLISECONDS)
            .build()
        focusMeteringControl.startFocusAndMetering(action, autoCancelTimeOutDuration)

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(focusMeteringControl, timeout(autoCancelTimeOutDuration))
            .completeActionFuture(false)
        verify(focusMeteringControl, timeout(autoCancelTimeOutDuration))
            .cancelFocusAndMeteringWithoutAsyncResult()
    }

    @MediumTest
    @Test
    fun shorterAutoCancelDuration_cancelIsCalled_completeActionFutureIsNotCalled(): Unit =
        runTest {
            focusMeteringControl = spy(focusMeteringControl)
            val autoCancelDuration: Long = 500
            val action = FocusMeteringAction.Builder(point1)
                .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
                .build()
            val autoFocusTimeoutDuration: Long = 1000
            focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutDuration)

            // This is necessary for running delayed task in robolectric.
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            verify(focusMeteringControl, timeout(action.autoCancelDurationInMillis))
                .cancelFocusAndMeteringWithoutAsyncResult()
            val remainingDuration = autoFocusTimeoutDuration - action.autoCancelDurationInMillis
            delay(remainingDuration)
            verify(focusMeteringControl, never()).completeActionFuture(anyBoolean())
        }

    @MediumTest
    @Test
    fun longerAutoCancelDuration_cancelIsCalled_afterCompleteWithIsFocusSuccessfulFalse() {
        focusMeteringControl = spy(focusMeteringControl)
        val autoCancelDuration: Long = 1000
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
            .build()
        val autoFocusTimeoutDuration: Long = 500
        focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutDuration)

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(focusMeteringControl, timeout(autoFocusTimeoutDuration))
            .completeActionFuture(false)
        val remainingDuration = autoCancelDuration - autoFocusTimeoutDuration
        // cancelFocusAndMeteringWithoutAsyncResult will be called finally
        verify(focusMeteringControl, timeout(remainingDuration))
            .cancelFocusAndMeteringWithoutAsyncResult()
    }

    @MediumTest
    @Test
    fun autoCancelDurationDisabled_completeAfterAutoFocusTimeoutDuration(): Unit = runTest {
        focusMeteringControl = spy(focusMeteringControl)
        val autoCancelDuration: Long = 500
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
            .disableAutoCancel()
            .build()
        val autoFocusTimeoutTestDuration: Long = 1000
        focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutTestDuration)

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        delay(autoCancelDuration)
        // cancelFocusAndMeteringWithoutAsyncResult won't be called
        verify(focusMeteringControl, never())
            .cancelFocusAndMeteringWithoutAsyncResult()
        val remainingDuration = autoFocusTimeoutTestDuration - autoCancelDuration
        // Completes with isFocusSuccessful false finally
        verify(focusMeteringControl, timeout(remainingDuration))
            .completeActionFuture(false)
    }

    private fun assertFutureFocusCompleted(
        future: ListenableFuture<FocusMeteringResult>,
        isFocused: Boolean
    ) {
        val focusMeteringResult = future[3, TimeUnit.SECONDS]
        assertThat(focusMeteringResult.isFocusSuccessful).isEqualTo(isFocused)
    }

    private fun assertFutureComplete(future: ListenableFuture<Void>) {
        try {
            future.get()
        } catch (e: Exception) {
            TestCase.fail("Future fails to complete")
        }
    }

    private fun retrieveCaptureResultListener(): CaptureResultListener {
        val argumentCaptor = ArgumentCaptor.forClass(
            CaptureResultListener::class.java
        )
        verify(camera2CameraControlImpl).addCaptureResultListener(argumentCaptor.capture())
        val listener = argumentCaptor.value
        reset(camera2CameraControlImpl)
        return listener
    }

    private fun updateCaptureResultWithSessionUpdateId(
        captureResultListener: CaptureResultListener,
        sessionUpdateId: Long
    ) {
        val result = Mockito.mock(
            TotalCaptureResult::class.java
        )
        val captureRequest = Mockito.mock(
            CaptureRequest::class.java
        )
        Mockito.`when`(result.request).thenReturn(captureRequest)
        val tagBundle = TagBundle.create(
            Pair(
                Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID,
                sessionUpdateId
            )
        )
        Mockito.`when`(captureRequest.tag).thenReturn(tagBundle)
        captureResultListener.onCaptureResult(result)
    }

    private fun updateCaptureResultWithAfState(
        captureResultListener: CaptureResultListener,
        afState: Int?
    ) {
        val result1 = Mockito.mock(
            TotalCaptureResult::class.java
        )
        Mockito.`when`(result1.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(afState)
        captureResultListener.onCaptureResult(result1)
    }

    private fun updateCaptureResultWithAfStateAndSessionUpdateId(
        captureResultListener: CaptureResultListener,
        afState: Int?,
        sessionUpdateId: Long
    ) {
        val result = Mockito.mock(
            TotalCaptureResult::class.java
        )
        val captureRequest = Mockito.mock(
            CaptureRequest::class.java
        )
        Mockito.`when`(result.get(CaptureResult.CONTROL_AF_STATE)).thenReturn(afState)
        Mockito.`when`(result.request).thenReturn(captureRequest)
        val tagBundle = TagBundle.create(
            Pair(
                Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID,
                sessionUpdateId
            )
        )
        Mockito.`when`(captureRequest.tag).thenReturn(tagBundle)
        captureResultListener.onCaptureResult(result)
    }

    private fun updateCaptureResultWithAfModeAndSessionUpdateId(
        captureResultListener: CaptureResultListener,
        afMode: Int,
        sessionUpdateId: Long
    ) {
        val result = Mockito.mock(
            TotalCaptureResult::class.java
        )
        val captureRequest = Mockito.mock(
            CaptureRequest::class.java
        )
        Mockito.`when`(result.get(CaptureResult.CONTROL_AF_MODE)).thenReturn(afMode)
        Mockito.`when`(result.request).thenReturn(captureRequest)
        val tagBundle = TagBundle.create(
            Pair(
                Camera2CameraControlImpl.TAG_SESSION_UPDATE_ID,
                sessionUpdateId
            )
        )
        Mockito.`when`(captureRequest.tag).thenReturn(tagBundle)
        captureResultListener.onCaptureResult(result)
    }

    private fun <T> assertFutureFailedWithOperationCancellation(future: ListenableFuture<T>) {
        try {
            future.get()
        } catch (e: ExecutionException) {
            assertThat(e.cause)
                .isInstanceOf(CameraControl.OperationCanceledException::class.java)
            return
        }
        TestCase.fail("Should fail with CameraControl.OperationCanceledException.")
    }

    @Test
    fun startFocusMeteringAEAWB_sessionUpdated_completesWithFocusFalse() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
            .build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithSessionUpdateId(
            captureResultListener,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMeteringAE_sessionUpdated_completesWithFocusFalse() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE
        ).build()
        val future2 = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithSessionUpdateId(
            captureResultListener,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future2, false)
    }

    @Test
    fun startFocusMeteringAWB_sessionUpdated_completesWithFocusFalse() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AWB
        )
            .build()
        val future3 = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithSessionUpdateId(
            captureResultListener,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future3, false)
    }

    @Test
    fun startFocusMetering_sessionUpdateIdIncreaseBy1_completesWithFocusFalse() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
            .build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithSessionUpdateId(
            captureResultListener,
            camera2CameraControlImpl.currentSessionUpdateId + 1
        )
        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_AFLockedWithSessionUpdated_completesWithFocusTrue() {
        val action = FocusMeteringAction.Builder(point1).build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, true)
    }

    @Test
    fun startFocusMetering_AFLockedThenSessionUpdated_completesWithFocusTrue() {
        val action = FocusMeteringAction.Builder(point1).build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
        )
        updateCaptureResultWithSessionUpdateId(
            captureResultListener,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, true)
    }

    @Test
    fun startFocusMetering_NotAFLocked_completesWithFocusFalse() {
        val action = FocusMeteringAction.Builder(point1).build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, false)
    }

    // When AfState is null, it means it does not support AF.
    @Test
    fun startFocusMetering_AfStateIsNull_completesWithFocusTrue() {
        val action = FocusMeteringAction.Builder(point1).build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(captureResultListener, null)
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            null,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, true)
    }

    @Test
    fun startFocusMeteringAFOnly_sessionUpdated_completesWithFocusTrue() {
        val action = FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF).build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, true)
    }

    @Test
    fun startFocusMeteringAfRequested_CameraNotSupportAfAuto_CompletesWithTrue() {
        // Use camera which does not support AF_AUTO
        val focusMeteringControl = initFocusMeteringControl(CAMERA2_ID)
        val action = FocusMeteringAction.Builder(point1)
            .build()
        val result = focusMeteringControl.startFocusAndMetering(
            action
        )
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithSessionUpdateId(
            captureResultListener,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(result, true)
    }

    @Test
    fun startFocusMetering_cancelBeforeCompleted_failWithOperationCanceledOperation() {
        val action = FocusMeteringAction.Builder(point1).build()
        val future = focusMeteringControl.startFocusAndMetering(
            action
        )
        focusMeteringControl.cancelFocusAndMetering()
        try {
            future.get()
            TestCase.fail("The future should fail.")
        } catch (e: ExecutionException) {
            assertThat(e.cause)
                .isInstanceOf(CameraControl.OperationCanceledException::class.java)
        } catch (e: InterruptedException) {
            assertThat(e.cause)
                .isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
    }

    @Test
    fun startThenCancelThenStart_previous2FuturesFailsWithOperationCanceled() {
        val action = FocusMeteringAction.Builder(point1)
            .build()
        val result1 = focusMeteringControl.startFocusAndMetering(action)
        val result2 = focusMeteringControl.cancelFocusAndMetering()
        reset(camera2CameraControlImpl)
        val result3 = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        assertFutureFailedWithOperationCancellation(result1)
        assertFutureFailedWithOperationCancellation(result2)
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(result3, true)
    }

    @Test
    fun startMultipleActions_cancelNonLatest() {
        val action = FocusMeteringAction.Builder(point1)
            .build()
        val result1 = focusMeteringControl.startFocusAndMetering(action)
        val result2 = focusMeteringControl.startFocusAndMetering(action)
        reset(camera2CameraControlImpl)
        val result3 = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        assertFutureFailedWithOperationCancellation(result1)
        assertFutureFailedWithOperationCancellation(result2)
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(result3, true)
    }

    @Test
    fun startFocusMetering_focusedThenCancel_futureStillCompletes() {
        val action = FocusMeteringAction.Builder(point1).build()
        val result = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )

        // cancel it and then ensure the returned ListenableFuture still completes;
        focusMeteringControl.cancelFocusAndMetering()
        assertFutureFocusCompleted(result, true)
    }

    @Test
    fun cancelFocusAndMetering_regionIsReset() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        )
            .addPoint(
                point2,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                    FocusMeteringAction.FLAG_AWB
            )
            .build()
        focusMeteringControl.startFocusAndMetering(action)
        var afRects = getAfRects(focusMeteringControl)
        var aeRects = getAeRects(focusMeteringControl)
        var awbRects = getAwbRects(focusMeteringControl)

        // Max AF count = 3, Max AE count = 3, Max AWB count = 1
        assertThat(afRects).hasLength(2)
        assertThat(aeRects).hasLength(2)
        assertThat(awbRects).hasLength(1)
        focusMeteringControl.cancelFocusAndMetering()
        afRects = getAfRects(focusMeteringControl)
        aeRects = getAeRects(focusMeteringControl)
        awbRects = getAwbRects(focusMeteringControl)
        assertThat(afRects).hasLength(0)
        assertThat(aeRects).hasLength(0)
        assertThat(awbRects).hasLength(0)
    }

    @Test
    fun cancelFocusAndMetering_updateSessionIsCalled() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        )
            .addPoint(
                point2,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                    FocusMeteringAction.FLAG_AWB
            )
            .build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(camera2CameraControlImpl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(camera2CameraControlImpl, Mockito.times(1))
            .updateSessionConfigSynchronous()
    }

    @Test
    fun cancelFocusAndMetering_triggerCancelAfProperly() {
        // If AF is enabled, cancel operation needs to call cancelAfAeTriggerInternal(true, false)
        var action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        )
            .build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, Mockito.times(1)).cancelAfAeTrigger(true, false)
        action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, Mockito.times(1)).cancelAfAeTrigger(true, false)
        action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AWB
        ).build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, Mockito.times(1)).cancelAfAeTrigger(true, false)
        action = FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF)
            .build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, Mockito.times(1)).cancelAfAeTrigger(true, false)
    }

    @Test
    fun cancelFocusAndMetering_AFNotInvolved_cancelAfNotTriggered() {
        var action = FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AE).build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, never()).cancelAfAeTrigger(true, false)
        action = FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AWB).build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, never()).cancelAfAeTrigger(true, false)
        action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        focusMeteringControl.startFocusAndMetering(action)
        reset(focusMeteringControl)
        focusMeteringControl.cancelFocusAndMetering()
        verify(focusMeteringControl, never()).cancelAfAeTrigger(true, false)
    }

    @Test
    fun cancelFocusMetering_actionIsCanceledAndFutureCompletes() {
        val action = FocusMeteringAction.Builder(point1).build()
        val actionResult = focusMeteringControl.startFocusAndMetering(action)

        // reset mock so that we can capture the listener added by cancelFocusAndMetering.
        reset(camera2CameraControlImpl)
        val cancelResult = focusMeteringControl.cancelFocusAndMetering()
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfModeAndSessionUpdateId(
            captureResultListener,
            focusMeteringControl.defaultAfMode,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFailedWithOperationCancellation(actionResult)
        assertFutureComplete(cancelResult)
    }

    @MediumTest
    @Test
    fun cancelFocusAndMetering_autoCancelIsDisabled(): Unit = runTest {
        focusMeteringControl = spy(focusMeteringControl)
        val autoCancelDuration: Long = 500
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
            .build()
        val autoFocusTimeoutDuration: Long = 1000
        focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutDuration)
        focusMeteringControl.cancelFocusAndMetering()
        reset(focusMeteringControl)

        // This is necessary for running delayed task in robolectric.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        delay(autoCancelDuration)

        // cancelFocusAndMetering won't be called in the specified auto cancel duration
        verify(focusMeteringControl, never()).cancelFocusAndMetering()

        // completeActionFuture won't be called in autoFocusTimeoutDuration after canceling
        // the focus and metering action.
        delay(autoFocusTimeoutDuration)
        verify(focusMeteringControl, never()).completeActionFuture(anyBoolean())
    }

    @Test
    fun startFocusMetering_isAfAutoModeIsTrue() {
        val action = FocusMeteringAction.Builder(point1).build()
        verifyAfMode(focusMeteringControl.defaultAfMode)
        focusMeteringControl.startFocusAndMetering(action)
        verifyAfMode(CaptureResult.CONTROL_AF_MODE_AUTO)
    }

    private fun verifyAfMode(expectAfMode: Int) {
        val builder1 = Camera2ImplConfig.Builder()
        focusMeteringControl.addFocusMeteringOptions(builder1)
        assertThat(
            builder1.build().getCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE, null
            )
        ).isEqualTo(expectAfMode)
    }

    @Suppress("UNCHECKED_CAST")
    private fun verifyTemplate(expectTemplate: Int) {
        val captor = ArgumentCaptor.forClass(
            List::class.java as Class<List<CaptureConfig>>
        )
        verify(camera2CameraControlImpl).submitCaptureRequestsInternal(captor.capture())
        val captureConfigList = captor.value
        assertThat(captureConfigList[0].templateType).isEqualTo(expectTemplate)
    }

    @Test
    fun startFocusMetering_AfNotInvolved_isAfAutoModeIsSet() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        val defaultAfMode = focusMeteringControl.defaultAfMode
        verifyAfMode(defaultAfMode)
        focusMeteringControl.startFocusAndMetering(action)
        verifyAfMode(defaultAfMode)
    }

    @Test
    fun startAndThenCancel_isAfAutoModeIsFalse() {
        val action = FocusMeteringAction.Builder(point1).build()
        focusMeteringControl.startFocusAndMetering(action)
        focusMeteringControl.cancelFocusAndMetering()
        verifyAfMode(focusMeteringControl.defaultAfMode)
    }

    @Test
    fun startFocusMeteringAFAEAWB_noPointsAreSupported_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA3_ID)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertThrows(ExecutionException::class.java) {
            future[500, TimeUnit.MILLISECONDS]
        }.also {
            assertThat(it.cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun startFocusMeteringAEAWB_noPointsAreSupported_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA3_ID)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertThrows(ExecutionException::class.java) {
            future[500, TimeUnit.MILLISECONDS]
        }.also {
            assertThat(it.cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun startFocusMeteringAFAWB_noPointsAreSupported_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA3_ID)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertThrows(ExecutionException::class.java) {
            future[500, TimeUnit.MILLISECONDS]
        }.also {
            assertThat(it.cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun startFocusMetering_morePointsThanSupported_futureCompletes() {
        // Camera0 only support 3 AF, 3 AE, 1 AWB regions, here we try to have 1 AE region, 2 AWB
        // regions.  it should still complete the future.
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
            .addPoint(point2, FocusMeteringAction.FLAG_AWB)
            .build()
        val future = focusMeteringControl.startFocusAndMetering(action)
        val captureResultListener = retrieveCaptureResultListener()
        updateCaptureResultWithAfState(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
        )
        updateCaptureResultWithAfStateAndSessionUpdateId(
            captureResultListener,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            camera2CameraControlImpl.currentSessionUpdateId
        )
        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_noPointsAreValid_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA0_ID)

        // These will generate MeteringRectangles (width == 0 or height ==0)
        val invalidPt1 = pointFactory.createPoint(2.0f, 2.0f)
        val invalidPt2 = pointFactory.createPoint(2.0f, 0.5f)
        val invalidPt3 = pointFactory.createPoint(-1.0f, -1.0f)
        val action = FocusMeteringAction.Builder(invalidPt1, FocusMeteringAction.FLAG_AF)
            .addPoint(invalidPt2, FocusMeteringAction.FLAG_AE)
            .addPoint(invalidPt3, FocusMeteringAction.FLAG_AWB).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertThrows(ExecutionException::class.java) {
            future[500, TimeUnit.MILLISECONDS]
        }.also {
            assertThat(it.cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun isFocusMeteringSupported_allSupportedPoints_shouldReturnTrue() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        )
            .addPoint(point2, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .addPoint(point2, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .build()
        assertThat(focusMeteringControl.isFocusMeteringSupported(action)).isTrue()
    }

    @Test
    fun isFocusMeteringSupported_morePointsThanSupported_shouldReturnTrue() {
        // Camera0 support 3 AF, 3 AE, 1 AWB regions, here we try to have 1 AE region, 2 AWB
        // regions. but it should still be supported.
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        )
            .addPoint(point2, FocusMeteringAction.FLAG_AWB)
            .build()
        assertThat(focusMeteringControl.isFocusMeteringSupported(action)).isTrue()
    }

    @Test
    fun isFocusMeteringSupported_noSupport3ARegion_shouldReturnFalse() {
        val action = FocusMeteringAction.Builder(point1).build()

        // No 3A regions are supported on Camera3
        val focusMeteringControl = initFocusMeteringControl(CAMERA3_ID)
        assertThat(focusMeteringControl.isFocusMeteringSupported(action)).isFalse()
    }

    @Test
    fun isFocusMeteringSupported_allInvalidPoints_shouldReturnFalse() {
        val invalidPoint1 = pointFactory.createPoint(1.1f, 0f)
        val invalidPoint2 = pointFactory.createPoint(0f, 1.1f)
        val invalidPoint3 = pointFactory.createPoint(-0.1f, 0f)
        val invalidPoint4 = pointFactory.createPoint(0f, -0.1f)
        val action = FocusMeteringAction.Builder(invalidPoint1)
            .addPoint(invalidPoint2)
            .addPoint(invalidPoint3)
            .addPoint(invalidPoint4).build()
        assertThat(focusMeteringControl.isFocusMeteringSupported(action)).isFalse()
    }
}
