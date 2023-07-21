/*
 * Copyright 2021 The Android Open Source Project
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

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.CaptureResult.CONTROL_AF_STATE
import android.hardware.camera2.CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
import android.hardware.camera2.CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.util.Rational
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.ZoomCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.AeFpsRange
import androidx.camera.camera2.pipe.integration.compat.workaround.MeteringRegionCorrection
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpAutoFlashAEModeDisabler
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpMeteringRegionCorrection
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.impl.State3AControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.testing.FakeZoomCompat
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.UseCase
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeUseCase
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

private const val CAMERA_ID_0 = "0" // 640x480 sensor size
private const val CAMERA_ID_1 = "1" // 1920x1080 sensor size
private const val CAMERA_ID_2 = "2" // 640x480 sensor size, not support AF_AUTO.
private const val CAMERA_ID_3 = "3" // camera that does not support 3A regions.
private const val CAMERA_ID_4 = "4" // camera 0 with LENS_FACING_FRONT
private const val CAMERA_ID_5 = "5" // camera 0 supporting AF regions only

private const val SENSOR_WIDTH = 640
private const val SENSOR_HEIGHT = 480
private const val SENSOR_WIDTH2 = 1920
private const val SENSOR_HEIGHT2 = 1080

private val AREA_WIDTH = (MeteringPointFactory.getDefaultPointSize() * SENSOR_WIDTH).toInt()
private val AREA_HEIGHT = (MeteringPointFactory.getDefaultPointSize() * SENSOR_HEIGHT).toInt()
private val AREA_WIDTH_2 = (MeteringPointFactory.getDefaultPointSize() * SENSOR_WIDTH2).toInt()
private val AREA_HEIGHT_2 = (MeteringPointFactory.getDefaultPointSize() * SENSOR_HEIGHT2).toInt()

private val M_RECT_1 = Rect(0, 0, AREA_WIDTH / 2, AREA_HEIGHT / 2)
private val M_RECT_2 = Rect(0, SENSOR_HEIGHT - AREA_HEIGHT / 2, AREA_WIDTH / 2, SENSOR_HEIGHT)
private val M_RECT_3 = Rect(
    SENSOR_WIDTH - AREA_WIDTH / 2,
    SENSOR_HEIGHT - AREA_HEIGHT / 2,
    SENSOR_WIDTH,
    SENSOR_HEIGHT
)

// the following rectangles are for metering point (0, 0)
private val M_RECT_PVIEW_RATIO_16x9_SENSOR_640x480 = Rect(
    0, 60 - AREA_HEIGHT / 2,
    AREA_WIDTH / 2, 60 + AREA_HEIGHT / 2
)
private val M_RECT_PVIEW_RATIO_4x3_SENSOR_1920x1080 = Rect(
    240 - AREA_WIDTH_2 / 2, 0,
    240 + AREA_WIDTH_2 / 2, AREA_HEIGHT_2 / 2
)

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class FocusMeteringControlTest {
    private val pointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
    private lateinit var focusMeteringControl: FocusMeteringControl

    private val point1 = pointFactory.createPoint(0f, 0f)
    private val point2 = pointFactory.createPoint(0.0f, 1.0f)
    private val point3 = pointFactory.createPoint(1.0f, 1.0f)

    private val cameraPropertiesMap = mutableMapOf<String, CameraProperties>()

    private val fakeRequestControl = FakeUseCaseCameraRequestControl()
    private val fakeUseCaseThreads by lazy {
        val executor = Executors.newSingleThreadExecutor()
        val dispatcher = executor.asCoroutineDispatcher()
        val cameraScope = CoroutineScope(Job() + dispatcher)

        UseCaseThreads(
            cameraScope,
            executor,
            dispatcher,
        )
    }

    @Before
    fun setUp() {
        loadCameraProperties()
        fakeRequestControl.focusMeteringResult =
            CompletableDeferred(Result3A(status = Result3A.Status.OK))
        focusMeteringControl = initFocusMeteringControl(CAMERA_ID_0)
    }

    @After
    fun tearDown() {
        // CoroutineScope#cancel can throw exception if the scope has no job left
        try {
            // fakeUseCaseThreads may still be using Main dispatcher which sometimes
            // causes Dispatchers.resetMain() to throw an exception:
            // "IllegalStateException: Dispatchers.Main is used concurrently with setting it"
            fakeUseCaseThreads.scope.cancel()
            fakeUseCaseThreads.sequentialScope.cancel()
        } catch (_: Exception) {
        }
    }

    @Test
    fun meteringRegionsFromMeteringPoint_fovAspectRatioEqualToCropAspectRatio() {
        val meteringPoint = FakeMeteringPointFactory().createPoint(0.0f, 0.0f)
        val meteringRectangles = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint),
            1,
            Rect(0, 0, 800, 600),
            Rational(4, 3),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles.size).isEqualTo(1)
        // Aspect ratio of crop region is same as default aspect ratio. So no padding is needed
        // along width or height. However only the bottom right quadrant of the metering rectangle
        // will fit inside the crop region.
        val expectedMeteringRectangle = MeteringRectangle(
            0, 0, 60, 45, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles[0]).isEqualTo(expectedMeteringRectangle)

        val meteringPoint1 = FakeMeteringPointFactory().createPoint(0.5f, 0.5f)
        val meteringRectangles1 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint1),
            1,
            Rect(0, 0, 800, 600),
            Rational(4, 3),
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                or FocusMeteringAction.FLAG_AWB,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles1.size).isEqualTo(1)
        // Aspect ratio of crop region is same as default aspect ratio. So no padding is needed
        // along width or height. The metering region will completely fit inside the crop region.
        val expectedMeteringRectangle1 = MeteringRectangle(
            340, 255, 120, 90, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles1[0]).isEqualTo(expectedMeteringRectangle1)

        val meteringPoint2 = FakeMeteringPointFactory().createPoint(1f, 1f)
        val meteringRectangles2 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint2),
            1,
            Rect(0, 0, 800, 600),
            Rational(4, 3),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles2.size).isEqualTo(1)
        // Aspect ratio of crop region is same as default aspect ratio. So no padding is needed
        // along width or height. However only the top left quadrant of the metering rectangle
        // will fit inside the crop region.
        val expectedMeteringRectangle2 = MeteringRectangle(
            740, 555, 60, 45, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles2[0]).isEqualTo(expectedMeteringRectangle2)
    }

    @Test
    fun meteringRegionsFromMeteringPoint_fovAspectRatioGreaterThanCropAspectRatio() {
        val meteringPoint = FakeMeteringPointFactory().createPoint(0.0f, 0.0f)
        val meteringRectangles = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint),
            1,
            Rect(0, 0, 400, 400),
            Rational(4, 3),
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                or FocusMeteringAction.FLAG_AWB,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles.size).isEqualTo(1)
        // Default aspect ratio is greater than the aspect ratio of the crop region. So we need
        // to add some padding at the top.
        val expectedMeteringRectangle = MeteringRectangle(
            0, 20, 30, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles[0]).isEqualTo(expectedMeteringRectangle)

        val meteringPoint1 = FakeMeteringPointFactory().createPoint(0.5f, 0.5f)
        val meteringRectangles1 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint1),
            1,
            Rect(0, 0, 400, 400),
            Rational(4, 3),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles1.size).isEqualTo(1)
        val expectedMeteringRectangle1 = MeteringRectangle(
            170, 170, 60, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles1[0]).isEqualTo(expectedMeteringRectangle1)

        val meteringPoint2 = FakeMeteringPointFactory().createPoint(1f, 1f)
        val meteringRectangles2 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint2),
            1,
            Rect(0, 0, 400, 400),
            Rational(4, 3),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles2.size).isEqualTo(1)
        // Default aspect ratio is greater than the aspect ratio of the crop region. So we need
        // to add some padding at the bottom.
        val expectedMeteringRectangle2 = MeteringRectangle(
            370, 320, 30, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles2[0]).isEqualTo(expectedMeteringRectangle2)
    }

    @Test
    fun meteringRegionsFromMeteringPoint_fovAspectRatioLessThanCropAspectRatio() {
        val meteringPoint = FakeMeteringPointFactory().createPoint(0.0f, 0.0f)
        val meteringRectangles = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint),
            1,
            Rect(0, 0, 400, 400),
            Rational(3, 4),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles.size).isEqualTo(1)
        val expectedMeteringRectangle = MeteringRectangle(
            20, 0, 60, 30, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles[0]).isEqualTo(expectedMeteringRectangle)

        val meteringPoint1 = FakeMeteringPointFactory().createPoint(0.5f, 0.5f)
        val meteringRectangles1 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint1),
            1,
            Rect(0, 0, 400, 400),
            Rational(3, 4),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles1.size).isEqualTo(1)
        val expectedMeteringRectangle1 = MeteringRectangle(
            170, 170, 60, 60, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles1[0]).isEqualTo(expectedMeteringRectangle1)

        val meteringPoint2 = FakeMeteringPointFactory().createPoint(1f, 1f)
        val meteringRectangles2 = FocusMeteringControl.meteringRegionsFromMeteringPoints(
            listOf(meteringPoint2),
            1,
            Rect(0, 0, 400, 400),
            Rational(3, 4),
            FocusMeteringAction.FLAG_AF,
            NoOpMeteringRegionCorrection,
        )
        assertThat(meteringRectangles2.size).isEqualTo(1)
        val expectedMeteringRectangle2 = MeteringRectangle(
            320, 370, 60, 30, FocusMeteringControl.METERING_WEIGHT_DEFAULT
        )
        assertThat(meteringRectangles2[0]).isEqualTo(expectedMeteringRectangle2)
    }

    @Test
    fun startFocusAndMetering_invalidPoint() = runBlocking {
        val invalidPoint = pointFactory.createPoint(1f, 1.1f)

        val future = focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(invalidPoint).build()
        )

        assertFutureFailedWithIllegalArgumentException(future)
    }

    @Test
    fun startFocusAndMetering_defaultPoint_3ARectsAreCorrect() = runBlocking {
        startFocusMeteringAndAwait(FocusMeteringAction.Builder(point1).build())

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AE regions").that(aeRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(0)?.rect).isEqualTo(M_RECT_1)

            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region").that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_1)

            assertWithMessage("Wrong number of AWB regions").that(awbRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AWB region").that(awbRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
        }
    }

    @Test
    fun startFocusAndMetering_defaultPoint_3ALocksAreCorrect() = runBlocking {
        startFocusMeteringAndAwait(FocusMeteringAction.Builder(point1).build())

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong lock behavior for AE")
                .that(aeLockBehavior).isEqualTo(Lock3ABehavior.AFTER_NEW_SCAN)
            assertWithMessage("Wrong lock behavior for AF")
                .that(afLockBehavior).isEqualTo(Lock3ABehavior.AFTER_NEW_SCAN)
            assertWithMessage("Wrong lock behavior for AWB")
                .that(awbLockBehavior).isEqualTo(Lock3ABehavior.AFTER_NEW_SCAN)
        }
    }

    @Test
    fun startFocusAndMetering_multiplePoints_3ARectsAreCorrect() = runBlocking {
        // Camera 0 i.e. Max AF count = 3, Max AE count = 3, Max AWB count = 1
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1)
                .addPoint(point2)
                .addPoint(point3)
                .build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AE regions").that(aeRegions?.size).isEqualTo(3)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(1)?.rect).isEqualTo(M_RECT_2)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(2)?.rect).isEqualTo(M_RECT_3)

            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(3)
            assertWithMessage("Wrong AF region").that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
            assertWithMessage("Wrong AF region").that(afRegions?.get(1)?.rect).isEqualTo(M_RECT_2)
            assertWithMessage("Wrong AF region").that(afRegions?.get(2)?.rect).isEqualTo(M_RECT_3)

            assertWithMessage("Wrong number of AWB regions").that(awbRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AWB region").that(awbRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
        }
    }

    @Test
    @Config(maxSdk = 32)
    fun startFocusAndMetering_AfRegionCorrectedByQuirk() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Samsung")

        focusMeteringControl = initFocusMeteringControl(cameraId = CAMERA_ID_4)

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1)
                .addPoint(point2)
                .addPoint(point3)
                .build()
        )

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

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AE regions").that(aeRegions?.size).isEqualTo(3)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(1)?.rect).isEqualTo(M_RECT_2)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(2)?.rect).isEqualTo(M_RECT_3)

            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(3)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(0)?.rect).isEqualTo(flippedRect1)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(1)?.rect).isEqualTo(flippedRect2)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(2)?.rect).isEqualTo(flippedRect3)

            assertWithMessage("Wrong number of AWB regions")
                .that(awbRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AWB region").that(awbRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
        }
    }

    @Test
    fun startFocusAndMetering_multiplePointsVariousModes() = runBlocking {
        // Camera 0 i.e. Max AF count = 3, Max AE count = 3, Max AWB count = 1
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AWB)
                .addPoint(point2, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .addPoint(
                    point3,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                        FocusMeteringAction.FLAG_AWB
                )
                .build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AE regions").that(aeRegions?.size).isEqualTo(2)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(0)?.rect).isEqualTo(M_RECT_2)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(1)?.rect).isEqualTo(M_RECT_3)

            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(2)
            assertWithMessage("Wrong AF region").that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_2)
            assertWithMessage("Wrong AF region").that(afRegions?.get(1)?.rect).isEqualTo(M_RECT_3)

            assertWithMessage("Wrong number of AWB regions").that(awbRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AWB region").that(awbRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
        }
    }

    @Test
    fun startFocusAndMetering_multiplePointsDistinctModes() {
        // Camera 0 i.e. Max AF count = 3, Max AE count = 3, Max AWB count = 1
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF)
                .addPoint(point2, FocusMeteringAction.FLAG_AWB)
                .addPoint(point3, FocusMeteringAction.FLAG_AE)
                .build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AE regions").that(aeRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AE region").that(aeRegions?.get(0)?.rect).isEqualTo(M_RECT_3)

            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region").that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_1)

            assertWithMessage("Wrong number of AWB regions").that(awbRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AWB region").that(awbRegions?.get(0)?.rect).isEqualTo(M_RECT_2)
        }
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

        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            zoomCompat = FakeZoomCompat(croppedSensorArea = cropRect),
        )

        val centerPt = pointFactory.createPoint(0.5f, 0.5f)
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(centerPt).build()
        )

        val areaWidth = (MeteringPointFactory.getDefaultPointSize() * cropRect.width()).toInt()
        val areaHeight = (MeteringPointFactory.getDefaultPointSize() * cropRect.height()).toInt()
        val adjustedRect = Rect(
            cropRect.centerX() - areaWidth / 2,
            cropRect.centerY() - areaHeight / 2,
            cropRect.centerX() + areaWidth / 2,
            cropRect.centerY() + areaHeight / 2
        )
        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(0)?.rect).isEqualTo(adjustedRect)
        }
    }

    @Test
    fun cropRegionIsSetTwice_resultAlwaysBasedOnCurrentCropRegion() {
        val cropWidth = 480
        val cropHeight = 360
        val cropRect = Rect(
            SENSOR_WIDTH / 2 - cropWidth / 2,
            SENSOR_HEIGHT / 2 - cropHeight / 2,
            SENSOR_WIDTH / 2 + cropWidth / 2, SENSOR_HEIGHT / 2 + cropHeight / 2
        )

        val zoomCompat = FakeZoomCompat(croppedSensorArea = Rect(0, 0, 640, 480))
        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            zoomCompat = zoomCompat,
        )

        val centerPt = pointFactory.createPoint(0.5f, 0.5f)
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(centerPt).build()
        )

        zoomCompat.croppedSensorArea = cropRect
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(centerPt).build()
        )

        val areaWidth = (MeteringPointFactory.getDefaultPointSize() * cropRect.width()).toInt()
        val areaHeight = (MeteringPointFactory.getDefaultPointSize() * cropRect.height()).toInt()
        val adjustedRect = Rect(
            cropRect.centerX() - areaWidth / 2,
            cropRect.centerY() - areaHeight / 2,
            cropRect.centerX() + areaWidth / 2,
            cropRect.centerY() + areaHeight / 2
        )
        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(0)?.rect).isEqualTo(adjustedRect)
        }
    }

    @Test
    fun previewFovAdjusted_16by9_to_4by3() {
        // use 16:9 preview aspect ratio with sensor region of 4:3 (camera 0)
        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            useCases = setOf(createPreview(Size(1920, 1080))),
        )

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_PVIEW_RATIO_16x9_SENSOR_640x480)
        }
    }

    @Test
    fun previewFovAdjusted_4by3_to_16by9() {
        // use 4:3 preview aspect ratio with sensor region of 16:9 (camera 1)
        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_1,
            useCases = setOf(createPreview(Size(640, 480))),
            zoomCompat = FakeZoomCompat(croppedSensorArea = Rect(0, 0, 1920, 1080))
        )

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_PVIEW_RATIO_4x3_SENSOR_1920x1080)
        }
    }

    @Test
    fun customFovAdjusted() {
        // 16:9 to 4:3
        val useCase = FakeUseCase()
        useCase.updateSuggestedStreamSpec(StreamSpec.builder(Size(1920, 1080)).build())

        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f, useCase)
        val point = factory.createPoint(0f, 0f)

        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            useCases = setOf(createPreview(Size(640, 480))),
        )

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region")
                .that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_PVIEW_RATIO_16x9_SENSOR_640x480)
        }
    }

    @Test
    fun previewRatioNotUsed_whenPreviewUseCaseIsRemoved() {
        // add 16:9 aspect ratio Preview with sensor region of 4:3 (camera 0), then remove Preview
        focusMeteringControl = initFocusMeteringControl(
            CAMERA_ID_0,
            useCases = setOf(createPreview(Size(1920, 1080))),
        )
        fakeUseCaseCamera.runningUseCases = emptySet()
        focusMeteringControl.onRunningUseCasesChanged()

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1).build()
        )

        // point1 = (0, 0) is considered as center point of metering rectangle.
        // Since previewAspectRatio is not set, it will be same as cropRegionAspectRatio
        // which is the size of SENSOR_1 in this test. So the point is not adjusted,
        // and simply M_RECT_1 (metering rectangle of point1 with SENSOR_1) should be used.
        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong AF region").that(afRegions?.get(0)?.rect).isEqualTo(M_RECT_1)
        }
    }

    @Test
    fun meteringPointsWithSize_convertedCorrectly() {
        val point1 = pointFactory.createPoint(0.5f, 0.5f, 1.0f)
        val point2 = pointFactory.createPoint(0.5f, 0.5f, 0.5f)
        val point3 = pointFactory.createPoint(0.5f, 0.5f, 0.1f)

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(point1)
                .addPoint(point2)
                .addPoint(point3).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(3)

            assertWithMessage("Wrong AF region width")
                .that(afRegions?.get(0)?.rect?.width()).isEqualTo((SENSOR_WIDTH * 1.0f).toInt())
            assertWithMessage("Wrong AF region height")
                .that(afRegions?.get(0)?.rect?.height()).isEqualTo((SENSOR_HEIGHT * 1.0f).toInt())

            assertWithMessage("Wrong AF region width")
                .that(afRegions?.get(1)?.rect?.width()).isEqualTo((SENSOR_WIDTH * 0.5f).toInt())
            assertWithMessage("Wrong AF region height")
                .that(afRegions?.get(1)?.rect?.height()).isEqualTo((SENSOR_HEIGHT * 0.5f).toInt())

            assertWithMessage("Wrong AF region width")
                .that(afRegions?.get(2)?.rect?.width()).isEqualTo((SENSOR_WIDTH * 0.1f).toInt())
            assertWithMessage("Wrong AF region height")
                .that(afRegions?.get(2)?.rect?.height()).isEqualTo((SENSOR_HEIGHT * 0.1f).toInt())
        }
    }

    @Test
    fun startFocusMetering_AfLocked_completesWithFocusTrue() {
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.OK,
                frameMetadata = FakeFrameMetadata(
                    extraMetadata = mapOf(
                        CONTROL_AF_STATE to CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
                )
            )
        )
        val action = FocusMeteringAction.Builder(point1).build()

        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFocusCompleted(future, true)
    }

    @Test
    fun startFocusMetering_AfNotLocked_completesWithFocusFalse() {
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.OK,
                frameMetadata = FakeFrameMetadata(
                    extraMetadata = mapOf(
                        CONTROL_AF_STATE to CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                    )
                )
            )
        )
        val action = FocusMeteringAction.Builder(point1).build()

        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_AfStateIsNull_completesWithFocusTrue() {
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.OK,
                frameMetadata = FakeFrameMetadata(
                    extraMetadata = mapOf(
                        CONTROL_AF_STATE to null
                    )
                )
            )
        )
        val action = FocusMeteringAction.Builder(point1)
            .build()

        val result = focusMeteringControl.startFocusAndMetering(
            action
        )

        assertFutureFocusCompleted(result, true)
    }

    @Test
    fun startFocusMeteringAfRequested_CameraNotSupportAfAuto_CompletesWithTrue() {
        // Use camera which does not support AF_AUTO
        focusMeteringControl = initFocusMeteringControl(CAMERA_ID_2)
        val action = FocusMeteringAction.Builder(point1)
            .build()

        val result = focusMeteringControl.startFocusAndMetering(
            action
        )

        assertFutureFocusCompleted(result, true)
    }

    @MediumTest
    @Test
    fun startFocusMetering_cancelledBeforeCompletion_failsWithOperationCanceledOperation() =
        runBlocking {
            // Arrange. Set a delay CompletableDeferred
            fakeRequestControl.focusMeteringResult = CompletableDeferred<Result3A>().apply {
                async(Dispatchers.Default) {
                    delay(500)
                    complete(
                        Result3A(
                            status = Result3A.Status.OK,
                            frameMetadata = FakeFrameMetadata(
                                extraMetadata = mapOf(
                                    CONTROL_AF_STATE to CONTROL_AF_STATE_FOCUSED_LOCKED
                                )
                            )
                        )
                    )
                }
            }
            val action = FocusMeteringAction.Builder(point1).build()
            val future = focusMeteringControl.startFocusAndMetering(action)

            // Act.
            focusMeteringControl.cancelFocusAndMeteringAsync()

            // Assert.
            assertFutureFailedWithOperationCancellation(future)
        }

    @Test
    fun startThenCancelThenStart_previous2FuturesFailsWithOperationCanceled() {
        // Arrange. Set a never complete CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred()
        fakeRequestControl.cancelFocusMeteringResult = CompletableDeferred()
        val action = FocusMeteringAction.Builder(point1).build()

        // Act.
        val result1 = focusMeteringControl.startFocusAndMetering(action)
        val result2 = focusMeteringControl.cancelFocusAndMeteringAsync().asListenableFuture()
        focusMeteringControl.startFocusAndMetering(action)

        // Assert.
        assertFutureFailedWithOperationCancellation(result1)
        assertFutureFailedWithOperationCancellation(result2)
    }

    @MediumTest
    @Test
    fun startMultipleActions_allExceptLatestAreCancelled() = runBlocking {
        // Arrange. Set a delay CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred<Result3A>().apply {
            async(Dispatchers.Default) {
                delay(500)
                complete(
                    Result3A(
                        status = Result3A.Status.OK,
                        frameMetadata = FakeFrameMetadata(
                            extraMetadata = mapOf(
                                CONTROL_AF_STATE to CONTROL_AF_STATE_FOCUSED_LOCKED
                            )
                        )
                    )
                )
            }
        }
        val action = FocusMeteringAction.Builder(point1).build()
        val result1 = focusMeteringControl.startFocusAndMetering(action)
        val result2 = focusMeteringControl.startFocusAndMetering(action)
        val result3 = focusMeteringControl.startFocusAndMetering(action)
        assertFutureFailedWithOperationCancellation(result1)
        assertFutureFailedWithOperationCancellation(result2)
        assertFutureFocusCompleted(result3, true)
    }

    @Test
    fun startFocusMetering_focusedThenCancel_futureStillCompletes() = runBlocking {
        // Arrange.
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.OK,
                frameMetadata = FakeFrameMetadata(
                    extraMetadata = mapOf(
                        CONTROL_AF_STATE to CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
                )
            )
        )
        val action = FocusMeteringAction.Builder(point1).build()

        val result = focusMeteringControl.startFocusAndMetering(action).apply {
            get(3, TimeUnit.SECONDS)
        }

        // Act. Cancel it and then ensure the returned ListenableFuture still completes.
        focusMeteringControl.cancelFocusAndMeteringAsync().join()

        // Assert.
        assertFutureFocusCompleted(result, true)
    }

    @Test
    fun startFocusMeteringAFAEAWB_noPointsAreSupported_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_3)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFailedWithIllegalArgumentException(future)
    }

    @Test
    fun startFocusMeteringAEAWB_noPointsAreSupported_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_3)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFailedWithIllegalArgumentException(future)
    }

    @Test
    fun startFocusMeteringAFAWB_noPointsAreSupported_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_3)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFailedWithIllegalArgumentException(future)
    }

    @Test
    fun startFocusMetering_morePointsThanSupported_futureCompletes() {
        // Camera 0 supports only 3 AF, 3 AE, 1 AWB regions, here we try to have 1 AE region, 2 AWB
        // regions. It should still complete the future, even though focus is not locked.
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.OK,
                frameMetadata = FakeFrameMetadata(
                    extraMetadata = mapOf(
                        CONTROL_AF_STATE to CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
                    )
                )
            )
        )

        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).addPoint(point2, FocusMeteringAction.FLAG_AWB)
            .build()

        val future = focusMeteringControl.startFocusAndMetering(action)

        // isFocused should be false since AF shouldn't trigger for lack of AF region.
        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_noPointsAreValid_failFuture() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_0)

        // These will generate MeteringRectangles (width == 0 or height ==0)
        val invalidPt1 = pointFactory.createPoint(2.0f, 2.0f)
        val invalidPt2 = pointFactory.createPoint(2.0f, 0.5f)
        val invalidPt3 = pointFactory.createPoint(-1.0f, -1.0f)
        val action = FocusMeteringAction.Builder(invalidPt1, FocusMeteringAction.FLAG_AF)
            .addPoint(invalidPt2, FocusMeteringAction.FLAG_AE)
            .addPoint(invalidPt3, FocusMeteringAction.FLAG_AWB).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFailedWithIllegalArgumentException(future)
    }

    @Test
    fun isFocusMeteringSupported_allSupportedPoints_shouldReturnTrue() {
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                FocusMeteringAction.FLAG_AWB
        ).addPoint(point2, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .addPoint(point2, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .build()
        assertThat(focusMeteringControl.isFocusMeteringSupported(action)).isTrue()
    }

    @Test
    fun isFocusMeteringSupported_morePointsThanSupported_shouldReturnTrue() {
        // Camera 0 supports 3 AF, 3 AE, 1 AWB regions, here we try to have 1 AE region, 2 AWB
        // regions. But it should still be supported.
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
        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_3)
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

    @Test
    fun cancelFocusMetering_actionIsCanceledAndFutureCompletes() {
        // Arrange. Set a never complete CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred()
        val action = FocusMeteringAction.Builder(point1).build()

        // Act.
        val actionResult = focusMeteringControl.startFocusAndMetering(action)
        val cancelResult = focusMeteringControl.cancelFocusAndMeteringAsync().asListenableFuture()

        // Assert.
        assertFutureFailedWithOperationCancellation(actionResult)
        assertThat(cancelResult[3, TimeUnit.SECONDS]?.status).isEqualTo(Result3A.Status.OK)
    }

    @MediumTest
    @Test
    fun cancelFocusAndMetering_autoCancelIsDisabled(): Unit = runBlocking {
        // Arrange. Set a never complete CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred()
        val autoCancelDuration: Long = 500
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
            .build()
        val autoFocusTimeoutDuration: Long = 1000
        focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutDuration)

        // Act. Call cancel before the auto cancel occur.
        focusMeteringControl.cancelFocusAndMeteringAsync().await()
        assertThat(fakeRequestControl.cancelFocusMeteringCallCount).isEqualTo(1)

        // Assert. cancelFocusMetering only be invoked once.
        delay(autoFocusTimeoutDuration)
        assertThat(fakeRequestControl.cancelFocusMeteringCallCount).isEqualTo(1)
    }

    @MediumTest
    @Test
    fun autoCancelDuration_completeWithIsFocusSuccessfulFalse() {
        // Arrange. Set a never complete CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred()
        val autoCancelTimeOutDuration: Long = 500
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelTimeOutDuration, TimeUnit.MILLISECONDS)
            .build()

        // Act.
        val future = focusMeteringControl.startFocusAndMetering(
            action,
            autoCancelTimeOutDuration
        )

        // Assert.
        assertFutureFocusCompleted(future, false)
    }

    @MediumTest
    @Test
    fun shorterAutoCancelDuration_cancelIsCalled_completeActionFutureIsNotCalled(): Unit =
        runBlocking {
            // Arrange. Set a never complete CompletableDeferred
            fakeRequestControl.focusMeteringResult = CompletableDeferred()
            val autoCancelDuration: Long = 500
            val action = FocusMeteringAction.Builder(point1)
                .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
                .build()
            val autoFocusTimeoutDuration: Long = 1000
            focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutDuration)

            // Act.
            val future = focusMeteringControl.startFocusAndMetering(
                action,
                autoFocusTimeoutDuration
            )

            // Assert.
            assertFutureFailedWithOperationCancellation(future)
        }

    @MediumTest
    @Test
    fun longerAutoCancelDuration_completeWithIsFocusSuccessfulFalse() {
        // Arrange. Set a never complete CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred()
        val autoCancelDuration: Long = 1000
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
            .build()
        val autoFocusTimeoutDuration: Long = 500

        // Act.
        val future = focusMeteringControl.startFocusAndMetering(action, autoFocusTimeoutDuration)

        // Assert.
        assertFutureFocusCompleted(future, false)
    }

    @MediumTest
    @Test
    fun autoCancelDurationDisabled_completeAfterAutoFocusTimeoutDuration(): Unit = runBlocking {
        // Arrange. Set a never complete CompletableDeferred
        fakeRequestControl.focusMeteringResult = CompletableDeferred()
        val autoCancelDuration: Long = 500
        val action = FocusMeteringAction.Builder(point1)
            .setAutoCancelDuration(autoCancelDuration, TimeUnit.MILLISECONDS)
            .disableAutoCancel()
            .build()
        val autoFocusTimeoutTestDuration: Long = 1000

        // Act.
        val future = focusMeteringControl.startFocusAndMetering(
            action, autoFocusTimeoutTestDuration
        )

        // Assert.
        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_afAutoModeIsSet() {
        // Arrange.
        val action = FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF).build()
        val state3AControl = createState3AControl(CAMERA_ID_0)
        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            useCases = setOf(createPreview(Size(1920, 1080))),
            useCaseThreads = fakeUseCaseThreads,
            state3AControl = state3AControl,
        )

        // Act.
        focusMeteringControl.startFocusAndMetering(
            action
        )[5, TimeUnit.SECONDS]

        // Assert.
        assertThat(
            state3AControl.preferredFocusMode
        ).isEqualTo(CaptureRequest.CONTROL_AF_MODE_AUTO)
    }

    @Test
    fun startFocusMetering_AfNotInvolved_afAutoModeNotSet() {
        // Arrange.
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        val state3AControl = createState3AControl(CAMERA_ID_0)
        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            useCases = setOf(createPreview(Size(1920, 1080))),
            useCaseThreads = fakeUseCaseThreads,
            state3AControl = state3AControl,
        )

        // Act.
        focusMeteringControl.startFocusAndMetering(
            action
        )[5, TimeUnit.SECONDS]

        // Assert.
        assertThat(
            state3AControl.preferredFocusMode
        ).isEqualTo(null)
    }

    @Test
    fun startAndThenCancel_afAutoModeNotSet(): Unit = runBlocking {
        // Arrange.
        val action = FocusMeteringAction.Builder(point1, FocusMeteringAction.FLAG_AF).build()
        val state3AControl = createState3AControl(CAMERA_ID_0)
        focusMeteringControl = initFocusMeteringControl(
            cameraId = CAMERA_ID_0,
            useCases = setOf(createPreview(Size(1920, 1080))),
            useCaseThreads = fakeUseCaseThreads,
            state3AControl = state3AControl,
        )

        // Act.
        focusMeteringControl.startFocusAndMetering(
            action
        )[5, TimeUnit.SECONDS]
        focusMeteringControl.cancelFocusAndMeteringAsync().join()

        // Assert.
        assertThat(
            state3AControl.preferredFocusMode
        ).isEqualTo(null)
    }

    @Test
    fun startFocusMetering_submitFailed_failsWithOperationCanceledOperation() = runBlocking {
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.SUBMIT_FAILED,
                frameMetadata = null,
            )
        )

        val result = focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(point1).build()
        )

        assertFutureFailedWithOperationCancellation(result)
    }

    @Test
    fun startFocusMetering_noAfPoint_futureCompletesWithFocusUnsuccessful() {
        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_1)
        val action = FocusMeteringAction.Builder(
            point1,
            FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        ).build()
        val future = focusMeteringControl.startFocusAndMetering(action)

        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_frameMetadataNullWithOkStatus_futureCompletesWithFocusSuccessful() {
        /**
         * According to [androidx.camera.camera2.pipe.graph.Controller3A.lock3A] method
         * documentation, if the operation is not supported by the camera device, then this method
         * returns early with Result3A made of 'OK' status and 'null' metadata.
         */
        fakeRequestControl.focusMeteringResult = CompletableDeferred(
            Result3A(
                status = Result3A.Status.OK,
                frameMetadata = null,
            )
        )

        val focusMeteringControl = initFocusMeteringControl(CAMERA_ID_0)
        val future = focusMeteringControl.startFocusAndMetering(
            FocusMeteringAction.Builder(
                point1
            ).build()
        )

        assertFutureFocusCompleted(future, false)
    }

    @Test
    fun startFocusMetering_noAePoint_aeRegionsSetToDefault() {
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(
                point1, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AWB
            ).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong AE regions").that(aeRegions)
                .isEqualTo(CameraGraph.Constants3A.METERING_REGIONS_DEFAULT.toList())
        }
    }

    @Test
    fun startFocusMetering_noAfPoint_afRegionsSetToDefault() {
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(
                point1, FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
            ).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong AF regions").that(afRegions)
                .isEqualTo(CameraGraph.Constants3A.METERING_REGIONS_DEFAULT.toList())
        }
    }

    @Test
    fun startFocusMetering_noAwbPoint_awbRegionsSetToDefault() {
        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(
                point1, FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AF
            ).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong AWB regions").that(awbRegions)
                .isEqualTo(CameraGraph.Constants3A.METERING_REGIONS_DEFAULT.toList())
        }
    }

    @Test
    fun startFocusMetering_onlyAfSupported_unsupportedRegionsNotConfigured() {
        // camera 5 supports 1 AF and 0 AE/AWB regions
        focusMeteringControl = initFocusMeteringControl(cameraId = CAMERA_ID_5)

        startFocusMeteringAndAwait(
            FocusMeteringAction.Builder(
                point1,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or
                    FocusMeteringAction.FLAG_AWB
            ).build()
        )

        with(fakeRequestControl.focusMeteringCalls.last()) {
            assertWithMessage("Wrong number of AE regions").that(aeRegions).isNull()
            assertWithMessage("Wrong lock behavior for AE").that(aeLockBehavior).isNull()

            assertWithMessage("Wrong number of AF regions").that(afRegions?.size).isEqualTo(1)
            assertWithMessage("Wrong lock behavior for AE")
                .that(afLockBehavior).isEqualTo(Lock3ABehavior.AFTER_NEW_SCAN)

            assertWithMessage("Wrong number of AWB regions").that(awbRegions).isNull()
            assertWithMessage("Wrong lock behavior for AWB").that(awbLockBehavior).isNull()
        }
    }

    // TODO: Port the following tests once their corresponding logics have been implemented.
    //  - [b/255679866] triggerAfWithTemplate, triggerAePrecaptureWithTemplate,
    //          cancelAfAeTriggerWithTemplate

    private fun assertFutureFocusCompleted(
        future: ListenableFuture<FocusMeteringResult>,
        isFocused: Boolean
    ) {
        val focusMeteringResult = future[3, TimeUnit.SECONDS]
        assertThat(focusMeteringResult.isFocusSuccessful).isEqualTo(isFocused)
    }

    private fun <T> assertFutureFailedWithIllegalArgumentException(future: ListenableFuture<T>) {
        assertThrows(ExecutionException::class.java) {
            future[3, TimeUnit.SECONDS]
        }.apply {
            assertThat(cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    private fun <T> assertFutureFailedWithOperationCancellation(future: ListenableFuture<T>) {
        assertThrows(ExecutionException::class.java) {
            future[3, TimeUnit.SECONDS]
        }.apply {
            assertThat(cause).isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
    }

    private val focusMeteringResultCallback = object : FutureCallback<FocusMeteringResult?> {
        private var latch = CountDownLatch(1)

        @Volatile
        var successResult: FocusMeteringResult? = null

        @Volatile
        var failureThrowable: Throwable? = null

        override fun onSuccess(result: FocusMeteringResult?) {
            successResult = result
            latch.countDown()
        }

        override fun onFailure(t: Throwable) {
            failureThrowable = t
            latch.countDown()
        }

        fun reset() {
            latch = CountDownLatch(1)
        }

        suspend fun await(timeoutMs: Long = 10000) {
            withContext(Dispatchers.IO) {
                latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun startFocusMetering(action: FocusMeteringAction) {
        focusMeteringResultCallback.reset()

        val result = focusMeteringControl.startFocusAndMetering(action)
        Futures.addCallback<FocusMeteringResult>(
            result,
            focusMeteringResultCallback,
            Executors.newSingleThreadExecutor()
        )
    }

    private fun startFocusMeteringAndAwait(action: FocusMeteringAction) = runBlocking {
        startFocusMetering(action)
        focusMeteringResultCallback.await()
    }

    private val fakeUseCaseCamera = object : UseCaseCamera {
        override var runningUseCases = setOf<UseCase>()

        override val requestControl: UseCaseCameraRequestControl
            get() = fakeRequestControl

        override fun <T> setParameterAsync(
            key: CaptureRequest.Key<T>,
            value: T,
            priority: androidx.camera.core.impl.Config.OptionPriority
        ): Deferred<Unit> {
            TODO("Not yet implemented")
        }

        override fun setParametersAsync(
            values: Map<CaptureRequest.Key<*>, Any>,
            priority: androidx.camera.core.impl.Config.OptionPriority
        ): Deferred<Unit> {
            TODO("Not yet implemented")
        }

        override fun close(): Job {
            TODO("Not yet implemented")
        }
    }

    private fun initFocusMeteringControl(
        cameraId: String,
        useCases: Set<UseCase> = emptySet(),
        useCaseThreads: UseCaseThreads = fakeUseCaseThreads,
        state3AControl: State3AControl = createState3AControl(cameraId),
        zoomCompat: ZoomCompat = FakeZoomCompat()
    ) = FocusMeteringControl(
        cameraPropertiesMap[cameraId]!!,
        MeteringRegionCorrection.Bindings.provideMeteringRegionCorrection(
            CameraQuirks(
                cameraPropertiesMap[cameraId]!!.metadata,
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        cameraPropertiesMap[cameraId]!!.metadata,
                        StreamConfigurationMapBuilder.newBuilder().build()
                    ),
                )
            )
        ),
        state3AControl,
        useCaseThreads,
        zoomCompat
    ).apply {
        fakeUseCaseCamera.runningUseCases = useCases
        useCaseCamera = fakeUseCaseCamera
        onRunningUseCasesChanged()
    }

    private fun initCameraProperties(
        cameraIdStr: String,
        characteristics: Map<CameraCharacteristics.Key<*>, Any?>
    ): FakeCameraProperties {
        val cameraId = CameraId(cameraIdStr)
        return FakeCameraProperties(
            FakeCameraMetadata(
                cameraId = cameraId,
                characteristics = characteristics
            ),
            cameraId
        )
    }

    private fun loadCameraProperties() {
        // **** Camera 0 characteristics (640X480 sensor size)****//
        val characteristics0 = mapOf(
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to
                Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT),
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(
                    CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO,
                    CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                    CaptureResult.CONTROL_AF_MODE_AUTO,
                    CaptureResult.CONTROL_AF_MODE_OFF
                ),
            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES to
                intArrayOf(
                    CaptureResult.CONTROL_AE_MODE_ON,
                    CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
                    CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH,
                    CaptureResult.CONTROL_AE_MODE_OFF
                ),
            CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES to
                intArrayOf(
                    CaptureResult.CONTROL_AWB_MODE_AUTO,
                    CaptureResult.CONTROL_AWB_MODE_OFF
                ),
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AF to 3,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AE to 3,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AWB to 1
        )

        cameraPropertiesMap[CAMERA_ID_0] = initCameraProperties(
            CAMERA_ID_0,
            characteristics0
        )

        // **** Camera 1 characteristics (1920x1080 sensor size) ****//
        val characteristics1 = mapOf(
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to
                Rect(0, 0, SENSOR_WIDTH2, SENSOR_HEIGHT2),
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AF to 1,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AE to 1,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AWB to 1
        )

        cameraPropertiesMap[CAMERA_ID_1] = initCameraProperties(
            CAMERA_ID_1,
            characteristics1
        )

        // **** Camera 2 characteristics (640x480 sensor size, does not support AF_AUTO ****//
        val characteristics2 = mapOf(
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to
                Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT),
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES to
                intArrayOf(
                    CaptureResult.CONTROL_AF_MODE_OFF
                ),
            CameraCharacteristics.CONTROL_MAX_REGIONS_AF to 1,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AE to 1,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AWB to 1
        )

        cameraPropertiesMap[CAMERA_ID_2] = initCameraProperties(
            CAMERA_ID_2,
            characteristics2
        )

        // ** Camera 3 characteristics (640x480 sensor size, does not support any 3A regions //
        val characteristics3 = mapOf(
            CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE to
                Rect(0, 0, SENSOR_WIDTH, SENSOR_HEIGHT),
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AF to 0,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AE to 0,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AWB to 0
        )

        cameraPropertiesMap[CAMERA_ID_3] = initCameraProperties(
            CAMERA_ID_3,
            characteristics3
        )

        // **** Camera 4 characteristics (same as Camera 0, but includes LENS_FACING_FRONT) **** //
        val characteristics4 = characteristics0 + mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT
        )

        cameraPropertiesMap[CAMERA_ID_4] = initCameraProperties(
            CAMERA_ID_4,
            characteristics4
        )

        // **** Camera 5 characteristics (same as Camera 0, but supports AF regions only) **** //
        val characteristics5 = characteristics0 + mapOf(
            CameraCharacteristics.CONTROL_MAX_REGIONS_AF to 3,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AE to 0,
            CameraCharacteristics.CONTROL_MAX_REGIONS_AWB to 0
        )

        cameraPropertiesMap[CAMERA_ID_5] = initCameraProperties(
            CAMERA_ID_5,
            characteristics5
        )
    }

    private fun createPreview(suggestedStreamSpecResolution: Size) =
        Preview.Builder()
            .setCaptureOptionUnpacker { _, _ -> }
            .setSessionOptionUnpacker() { _, _, _ -> }
            .build().apply {
                setSurfaceProvider(
                    CameraXExecutors.mainThreadExecutor(),
                    SurfaceTextureProvider.createSurfaceTextureProvider()
                )
            }.also {
                it.bindToCamera(FakeCamera("0"), null, null)
                it.updateSuggestedStreamSpec(
                    StreamSpec.builder(suggestedStreamSpecResolution).build()
                )
            }

    private fun createState3AControl(
        cameraId: String = CAMERA_ID_0,
        properties: CameraProperties = cameraPropertiesMap[cameraId]!!,
        useCaseCamera: UseCaseCamera = fakeUseCaseCamera,
    ) = State3AControl(
        properties,
        NoOpAutoFlashAEModeDisabler,
        AeFpsRange(
            CameraQuirks(
                FakeCameraMetadata(),
                StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        FakeCameraMetadata(),
                        StreamConfigurationMapBuilder.newBuilder().build()
                    )
                )
            )
        )
    ).apply {
        this.useCaseCamera = useCaseCamera
    }
}
