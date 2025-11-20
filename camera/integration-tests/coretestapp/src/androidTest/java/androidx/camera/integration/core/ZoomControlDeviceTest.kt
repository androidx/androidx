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

package androidx.camera.integration.core

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Pair
import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.adapter.ZoomValue
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.internal.ImmutableZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.util.Camera2InteropUtil
import androidx.concurrent.futures.await
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ZoomControlDeviceTest(
    private val testName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo
    private lateinit var captureCallback: Camera2InteropUtil.CaptureCallback

    @Before
    fun setUp(): Unit = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        captureCallback = Camera2InteropUtil.CaptureCallback(_numOfCaptures = 200)

        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    cameraSelector,
                    ImageCapture.Builder()
                        .also { builder ->
                            Camera2InteropUtil.setCameraCaptureSessionCallback(
                                implName,
                                builder,
                                captureCallback,
                            )
                        }
                        .build(),
                )
        }

        cameraControl = camera.cameraControl
        cameraInfo = camera.cameraInfo
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }
    }

    @Test
    fun setZoomRatio_futuresCompletes() {
        assumeTrue(cameraInfo.zoomState.value!!.maxZoomRatio + DELTA >= 2.0f)

        // use ratio with fraction because it often causes unable-to-complete issue.
        val result = cameraControl.setZoomRatio(1.3640054f)
        assertFutureCompletes(result)
    }

    @Test
    fun rebindAndSetZoomRatio_futureCompletes() = runBlocking {
        withContext(Dispatchers.Main) {
            cameraProvider.unbindAll()
            val useCase = ImageCapture.Builder().build()
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }
        val result = cameraControl.setZoomRatio(1.0f)
        assertFutureCompletes(result)
    }

    @Test
    fun setZoomRatio_getValueIsCorrect() = runBlocking {
        val newZoomRatio = 2.0f
        assumeTrue(newZoomRatio <= cameraInfo.zoomState.value!!.maxZoomRatio + DELTA)

        cameraControl.setZoomRatio(newZoomRatio).await()

        assertThat(cameraInfo.zoomState.value?.zoomRatio).isEqualTo(newZoomRatio)
    }

    @Test
    fun setZoomRatio_largerThanMax_zoomUnmodified() = runBlocking {
        assumeTrue(2.0f <= cameraInfo.zoomState.value!!.maxZoomRatio + DELTA)
        cameraControl.setZoomRatio(2.0f)[5, TimeUnit.SECONDS]

        val maxZoomRatio = cameraInfo.zoomState.value!!.maxZoomRatio

        /**
         * The exception is caught but not handled here intentionally. Because in this test, we want
         * to focus on the value of the zoomRatio after exception is thrown. The exception itself is
         * tested with [setZoomRatio_largerThanMax_OutOfRangeException]
         */
        try {
            cameraControl.setZoomRatio(maxZoomRatio + 1.0f)[5, TimeUnit.SECONDS]
        } catch (_: ExecutionException) {}

        assertThat(cameraInfo.zoomState.value?.zoomRatio).isEqualTo(2.0f)
    }

    @Test
    fun setZoomRatio_largerThanMax_OutOfRangeException() = runBlocking {
        val maxZoomRatio = cameraInfo.zoomState.value!!.maxZoomRatio
        val result = cameraControl.setZoomRatio(maxZoomRatio + 1.0f)

        assertFutureThrowsIllegalArgumentException(result)
    }

    @Test
    fun setZoomRatio_smallerThanMin_zoomUnmodified() = runBlocking {
        assumeTrue(2.0f <= cameraInfo.zoomState.value!!.maxZoomRatio + DELTA)
        cameraControl.setZoomRatio(2.0f)[5, TimeUnit.SECONDS]

        val minZoomRatio = cameraInfo.zoomState.value!!.minZoomRatio

        /**
         * The exception is caught but not handled here intentionally. Because in this test, we want
         * to focus on the value of the zoomRatio after exception is thrown. The exception itself is
         * tested with [setZoomRatio_smallerThanMin_OutOfRangeException]
         */
        try {
            cameraControl.setZoomRatio(minZoomRatio - 1.0f)[5, TimeUnit.SECONDS]
        } catch (_: ExecutionException) {}

        assertThat(cameraInfo.zoomState.value?.zoomRatio).isEqualTo(2.0f)
    }

    @Test
    fun setZoomRatio_smallerThanMin_OutOfRangeException() = runBlocking {
        val minZoomRatio = cameraInfo.zoomState.value!!.minZoomRatio
        val result = cameraControl.setZoomRatio(minZoomRatio - 1.0f)

        assertFutureThrowsIllegalArgumentException(result)
    }

    @Test
    fun setZoomRatioBy1_0_isEqualToSensorRect() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = false)

        cameraControl.setZoomRatio(1.0f)[5, TimeUnit.SECONDS]

        captureCallback.verifyLastCaptureRequest(
            mapOf(CaptureRequest.SCALER_CROP_REGION to getSensorRect()),
            200,
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun setZoomRatioBy1_0_androidRZoomRatioIsUpdated() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = true)

        cameraControl.setZoomRatio(1.0f)[5, TimeUnit.SECONDS]

        captureCallback.verifyFor { captureRequests, _ ->
            captureRequests.last()[CaptureRequest.SCALER_CROP_REGION] == getSensorRect() &&
                areFloatsEqual(captureRequests.last()[CaptureRequest.CONTROL_ZOOM_RATIO], 1.0f)
        }
    }

    @Test
    fun setZoomRatioBy2_0_cropRegionIsSetCorrectly() = runBlocking {
        assumeTrue(getMaxDigitalZoom() != null && getMaxDigitalZoom()!! > 2.0f + DELTA)

        checkTestPreconditions(isAndroidRZoom = false)

        cameraControl.setZoomRatio(2.0f)[5, TimeUnit.SECONDS]

        val sensorRect = getSensorRect()
        val cropX = sensorRect.width() / 4
        val cropY = sensorRect.height() / 4
        val cropRect =
            Rect(cropX, cropY, cropX + sensorRect.width() / 2, cropY + sensorRect.height() / 2)

        captureCallback.verifyLastCaptureRequest(
            mapOf(CaptureRequest.SCALER_CROP_REGION to cropRect)
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun setZoomRatioBy2_0_androidRZoomRatioIsUpdated() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = true)

        cameraControl.setZoomRatio(2.0f)

        captureCallback.verifyFor { captureRequests, _ ->
            captureRequests.last()[CaptureRequest.SCALER_CROP_REGION] == getSensorRect() &&
                areFloatsEqual(captureRequests.last()[CaptureRequest.CONTROL_ZOOM_RATIO], 2.0f)
        }
    }

    @Test
    fun setLinearZoomBy0_isSameAsMinRatio() = runBlocking {
        cameraControl.setLinearZoom(0f)
        val ratioAtPercentage0 =
            cameraInfo.zoomState
                .waitForValue { value -> areFloatsEqual(value.linearZoom, 0f) }
                .zoomRatio

        val ratioAtMinZoomRatio = cameraInfo.zoomState.value?.minZoomRatio

        assertThat(ratioAtPercentage0).isEqualTo(ratioAtMinZoomRatio)
    }

    @Test
    fun setLinearZoomBy1_isSameAsMaxRatio() = runBlocking {
        cameraControl.setLinearZoom(1f)
        val ratioAtPercentage1 =
            cameraInfo.zoomState
                .waitForValue { value -> areFloatsEqual(value.linearZoom, 1f) }
                .zoomRatio

        val ratioAtMaxZoomRatio = cameraInfo.zoomState.value?.maxZoomRatio

        assertThat(ratioAtPercentage1).isEqualTo(ratioAtMaxZoomRatio)
    }

    @Test
    fun setLinearZoomBy0_5_isHalfCropWidth() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = false)

        // crop region in percentage == 0 may be null, need to use sensor rect instead.
        val cropRegionMinZoom = getSensorRect()

        val cropRegionMaxZoom = getCropRegionFromCameraCapture(linearZoom = 1f)
        val cropRegionHalfZoom = getCropRegionFromCameraCapture(linearZoom = 0.5f)

        assertThat(cropRegionHalfZoom.width().toFloat())
            .isWithin(TOLERANCE)
            .of((cropRegionMinZoom.width() + cropRegionMaxZoom.width()) / 2.0f)
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun setLinearZoomBy0_5_androidRZoomRatioUpdatedCorrectly() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = true)

        val cropWidth = 10000f

        val zoomRatioForLinearMax = getZoomRatioFromCameraCapture(1f)
        val cropWidthForLinearMax = cropWidth / zoomRatioForLinearMax

        val zoomRatioForLinearMin = getZoomRatioFromCameraCapture(0f)
        val cropWidthForLinearMin = cropWidth / zoomRatioForLinearMin

        val zoomRatioForLinearHalf = getZoomRatioFromCameraCapture(0.5f)
        val cropWidthForLinearHalf = cropWidth / zoomRatioForLinearHalf

        assertThat(cropWidthForLinearHalf)
            .isWithin(TOLERANCE)
            .of((cropWidthForLinearMin + cropWidthForLinearMax) / 2.0f)
    }

    @Test
    fun setLinearZoom_cropWidthChangedLinearly() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = false)

        // crop region in percentage == 0 may be null, need to use sensor rect instead.
        var prevCropRegion = getSensorRect()
        var prevWidthDelta = 0f

        var percentage = 0.1f
        while (percentage < 1.0f) {
            val cropRegion = getCropRegionFromCameraCapture(linearZoom = percentage)

            if (prevWidthDelta == 0f) {
                prevWidthDelta = (prevCropRegion.width() - cropRegion.width()).toFloat()
            } else {
                val widthDelta = (prevCropRegion.width() - cropRegion.width()).toFloat()

                assertThat(widthDelta).isWithin(TOLERANCE).of(prevWidthDelta)
            }
            prevCropRegion = cropRegion

            percentage += 0.1f
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun setLinearZoom_androidRZoomRatio_cropWidthChangedLinearly() = runBlocking {
        checkTestPreconditions(isAndroidRZoom = true)

        val cropWidth = 10000f

        val zoomRatioForLinearMin = getZoomRatioFromCameraCapture(linearZoom = 0f)
        var prevCropWidth = cropWidth / zoomRatioForLinearMin
        var prevWidthDelta = 0f

        var percentage = 0.1f
        while (percentage < 1.0f) {
            val zoomRatio = getZoomRatioFromCameraCapture(linearZoom = percentage)
            val cropWidthForTheRatio = cropWidth / zoomRatio

            if (prevWidthDelta == 0f) {
                prevWidthDelta = prevCropWidth - cropWidthForTheRatio
            } else {
                val widthDelta = prevCropWidth - cropWidthForTheRatio

                assertThat(widthDelta).isWithin(TOLERANCE).of(prevWidthDelta)
            }
            prevCropWidth = cropWidthForTheRatio

            percentage += 0.1f
        }
    }

    @Test
    fun setLinearZoom_largerThan1_zoomUnmodified() = runBlocking {
        cameraControl.setLinearZoom(0.5f)[5, TimeUnit.SECONDS]

        /**
         * The exception is caught but not handled here intentionally. Because in this test, we want
         * to focus on the value of the zoomRatio after exception is thrown. The exception itself is
         * tested with [setLinearZoom_largerThan1_outOfRangeException]
         */
        try {
            cameraControl.setLinearZoom(1.1f)[5, TimeUnit.SECONDS]
        } catch (_: ExecutionException) {}

        assertThat(cameraInfo.zoomState.value?.linearZoom).isEqualTo(0.5f)
    }

    @Test
    fun setLinearZoom_largerThan1_outOfRangeException() = runBlocking {
        val result = cameraControl.setLinearZoom(1.1f)

        assertFutureThrowsIllegalArgumentException(result)
    }

    @Test
    fun setLinearZoom_smallerThan0_zoomUnmodified() = runBlocking {
        cameraControl.setLinearZoom(0.5f)[5, TimeUnit.SECONDS]

        /**
         * The exception is caught but not handled here intentionally. Because in this test, we want
         * to focus on the value of the zoomRatio after exception is thrown. The exception itself is
         * tested with [setLinearZoom_smallerThan0_outOfRangeException]
         */
        try {
            cameraControl.setLinearZoom(-0.1f)[5, TimeUnit.SECONDS]
        } catch (_: ExecutionException) {}

        assertThat(cameraInfo.zoomState.value?.linearZoom).isEqualTo(0.5f)
    }

    @Test
    fun setLinearZoom_smallerThan0_outOfRangeException() = runBlocking {
        val result = cameraControl.setLinearZoom(-0.1f)

        assertFutureThrowsIllegalArgumentException(result)
    }

    @Test
    fun getterLiveData_defaultValueIsNonNull() {
        assertThat(cameraInfo.zoomState.value).isNotNull()
    }

    @Test
    fun getZoomRatioLiveData_observerIsCalledWhenZoomRatioIsSet() = runBlocking {
        assumeTrue(getMaxDigitalZoom() != null && getMaxDigitalZoom()!! > 2.0f + DELTA)

        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        val latch3 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraInfo.zoomState.observe(lifecycleOwner) { value: ZoomState ->
                if (areFloatsEqual(value.zoomRatio, 1.2f)) {
                    latch1.countDown()
                } else if (areFloatsEqual(value.zoomRatio, 1.5f)) {
                    latch2.countDown()
                } else if (areFloatsEqual(value.zoomRatio, 2.0f)) {
                    latch3.countDown()
                }
            }
            cameraControl.setZoomRatio(1.2f)
            cameraControl.setZoomRatio(1.5f)
            cameraControl.setZoomRatio(2.0f)
        }

        assertThat(latch1.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(latch2.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(latch3.await(3000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun getZoomRatioLiveData_observerIsCalledWhenZoomPercentageIsSet() = runBlocking {
        // can not test properly as zoom ratio will always be 1.0f
        assumeTrue(getMaxDigitalZoom() != null && abs(getMaxDigitalZoom()!! - 1.0f) > DELTA)

        val latch = CountDownLatch(3)
        withContext(Dispatchers.Main) {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraInfo.zoomState.observe(
                lifecycleOwner,
                Observer { value: ZoomState ->
                    if (value.zoomRatio != getMaxDigitalZoom()) {
                        latch.countDown()
                    }
                },
            )
            cameraControl.setLinearZoom(0.1f)
            cameraControl.setLinearZoom(0.2f)
            cameraControl.setLinearZoom(0.3f)
        }

        assertThat(latch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun getZoomPercentageLiveData_observerIsCalledWhenZoomPercentageIsSet() = runBlocking {
        val latch1 = CountDownLatch(1)
        val latch2 = CountDownLatch(1)
        val latch3 = CountDownLatch(1)
        withContext(Dispatchers.Main) {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraInfo.zoomState.observe(lifecycleOwner) { value: ZoomState ->
                if (areFloatsEqual(value.linearZoom, 0.1f)) {
                    latch1.countDown()
                } else if (areFloatsEqual(value.linearZoom, 0.2f)) {
                    latch2.countDown()
                } else if (areFloatsEqual(value.linearZoom, 0.3f)) {
                    latch3.countDown()
                }
            }
            cameraControl.setLinearZoom(0.1f)
            cameraControl.setLinearZoom(0.2f)
            cameraControl.setLinearZoom(0.3f)
        }

        assertThat(latch1.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(latch2.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(latch3.await(3000, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun getZoomPercentageLiveData_observerIsCalledWhenZoomRatioIsSet() = runBlocking {
        assumeTrue(getMaxDigitalZoom() != null && getMaxDigitalZoom()!! > 2.0f + DELTA)

        val latch = CountDownLatch(3)
        withContext(Dispatchers.Main) {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraInfo.zoomState.observe(
                lifecycleOwner,
                Observer { value: ZoomState ->
                    if (value.linearZoom != 0f) {
                        latch.countDown()
                    }
                },
            )
            cameraControl.setZoomRatio(1.2f)
            cameraControl.setZoomRatio(1.5f)
            cameraControl.setZoomRatio(2.0f)
        }

        assertThat(latch.await(1500, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun getZoomRatioDefaultValue() {
        assertThat(cameraInfo.zoomState.value?.zoomRatio).isEqualTo(1.0f)
    }

    @Test
    fun getZoomPercentageDefaultValue() {
        checkTestPreconditions(isAndroidRZoom = false)
        assertThat(cameraInfo.zoomState.value?.linearZoom).isEqualTo(0)
    }

    @Test
    fun getMaxZoomRatio_isMaxDigitalZoom() {
        val maxZoom = cameraInfo.zoomState.value?.maxZoomRatio
        assertThat(maxZoom).isEqualTo(getMaxDigitalZoom())
    }

    @Test
    fun getMinZoomRatio_isOne() {
        checkTestPreconditions(isAndroidRZoom = false)
        val minZoom = cameraInfo.zoomState.value?.minZoomRatio
        assertThat(minZoom).isEqualTo(1f)
    }

    private fun getMaxDigitalZoom(): Float? {
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
        assumeNotNull(cameraCharacteristics)

        if (isAndroidRZoomEnabled(cameraCharacteristics!!)) {
            return cameraCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.upper
        }

        return cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
    }

    @Test
    fun valueIsResetAfterUseCasesDetached() = runBlocking {
        cameraControl.setLinearZoom(0.2f) // this will change ratio and percentage.
        withContext(Dispatchers.Main) { cameraProvider.unbindAll() }

        cameraInfo.zoomState.waitForValue { value -> areFloatsEqual(value.zoomRatio, 1.0f) }

        return@runBlocking
    }

    @Test
    fun maxZoomShouldBeLargerThanOrEqualToMinZoom() {
        val zoomState = cameraInfo.zoomState.value
        assertThat(zoomState!!.maxZoomRatio).isAtLeast(zoomState.minZoomRatio)
    }

    private suspend fun LiveData<ZoomState>.waitForValue(
        waitCondition: (ZoomState) -> Boolean
    ): ZoomState {
        var awaitedValue: ZoomState = ZoomValue(-1f, -1f, -1f)
        val latch = CountDownLatch(1)

        withContext(Dispatchers.Main) {
            val lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            observe(
                lifecycleOwner,
                Observer { value: ZoomState ->
                    if (waitCondition(value)) {
                        awaitedValue = value
                        latch.countDown()
                    }
                },
            )
        }

        latch.await(3, TimeUnit.SECONDS)

        return awaitedValue
    }

    private fun checkTestPreconditions(isAndroidRZoom: Boolean) {
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
        assumeNotNull(cameraCharacteristics)

        if (isAndroidRZoom) {
            assumeTrue(isAndroidRZoomEnabled(cameraCharacteristics!!))
        } else {
            assumeNotNull(
                cameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            )
            assumeFalse(isAndroidRZoomEnabled(cameraCharacteristics))
        }
    }

    private suspend fun getCropRegionFromCameraCapture(linearZoom: Float): Rect {
        val cropRegionFromCameraCaptureRef = AtomicReference(Rect(0, 0, 0, 0))
        val cropRegionCallbackCountRef = AtomicReference(10)

        cameraControl.setLinearZoom(linearZoom)[5, TimeUnit.SECONDS]

        captureCallback.verifyFor { captureRequests, _ ->
            if (captureRequests.last()[CaptureRequest.SCALER_CROP_REGION] == null) {
                assert(false)
            }

            cropRegionFromCameraCaptureRef.set(
                captureRequests.last()[CaptureRequest.SCALER_CROP_REGION]!!
            )

            cropRegionCallbackCountRef.set(cropRegionCallbackCountRef.get() - 1)
            cropRegionCallbackCountRef.get() == 0
        }

        return cropRegionFromCameraCaptureRef.get()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun getZoomRatioFromCameraCapture(linearZoom: Float): Float {
        val zoomRatioFromCameraCaptureRef = AtomicReference(Float.NaN)
        val zoomRatioCallbackCountRef = AtomicReference(10)

        cameraControl.setLinearZoom(linearZoom)[5, TimeUnit.SECONDS]

        captureCallback.verifyFor { captureRequests, _ ->
            if (captureRequests.last()[CaptureRequest.CONTROL_ZOOM_RATIO] == null) {
                assert(false)
            }

            zoomRatioFromCameraCaptureRef.set(
                captureRequests.last()[CaptureRequest.CONTROL_ZOOM_RATIO]!!
            )

            zoomRatioCallbackCountRef.set(zoomRatioCallbackCountRef.get() - 1)
            zoomRatioCallbackCountRef.get() == 0
        }

        return zoomRatioFromCameraCaptureRef.get()
    }

    private fun assumeMaxZoomRatio(cameraSelector: CameraSelector, atLeastRatio: Float) {
        assumeTrue(
            cameraProvider.getCameraInfo(cameraSelector).zoomState.value!!.maxZoomRatio >=
                atLeastRatio
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun defaultZoomRatio_withSessionProcessor(): Unit =
        runBlocking(Dispatchers.Main) {
            val camera =
                bindWithSessionProcessorZoomLimitation(minZoom = MIN_ZOOM, maxZoom = MAX_ZOOM)
            val zoomState = camera.cameraInfo.zoomState.value
            assertThat(
                    zoomState?.isEquivalentTo(
                        ImmutableZoomState.create(
                            1.0f,
                            MAX_ZOOM,
                            MIN_ZOOM,
                            AdapterCameraInfo.getPercentageByRatio(1.0f, MIN_ZOOM, MAX_ZOOM),
                        )
                    )
                )
                .isTrue()
        }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun zoomRatioSet_withSessionProcessor(): Unit =
        runBlocking(Dispatchers.Main) {
            val zoomRatio = 3f
            val camera =
                bindWithSessionProcessorZoomLimitation(minZoom = MIN_ZOOM, maxZoom = MAX_ZOOM)

            withContext(Dispatchers.Main) {
                camera.cameraControl.setZoomRatio(zoomRatio)
                val zoomState = camera.cameraInfo.zoomState.value
                assertThat(
                        zoomState?.isEquivalentTo(
                            ImmutableZoomState.create(
                                zoomRatio,
                                MAX_ZOOM,
                                MIN_ZOOM,
                                AdapterCameraInfo.getPercentageByRatio(
                                    zoomRatio,
                                    MIN_ZOOM,
                                    MAX_ZOOM,
                                ),
                            )
                        )
                    )
                    .isTrue()
            }
        }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun invalidZoomRatioWillFail_withSessionProcessor(): Unit =
        runBlocking(Dispatchers.Main) {
            val camera =
                bindWithSessionProcessorZoomLimitation(minZoom = MIN_ZOOM, maxZoom = MAX_ZOOM)

            assertThrows<IllegalArgumentException> { camera.cameraControl.setZoomRatio(5f).await() }

            assertThrows<IllegalArgumentException> { camera.cameraControl.setZoomRatio(0f).await() }

            assertThrows<IllegalArgumentException> {
                camera.cameraControl.setLinearZoom(-1.0f).await()
            }

            assertThrows<IllegalArgumentException> {
                camera.cameraControl.setLinearZoom(1.1f).await()
            }
        }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun zoomObserverValueIsCorrect_withSessionProcessor(): Unit =
        runBlocking(Dispatchers.Main) {
            val zoomRatio = 2f
            val camera =
                bindWithSessionProcessorZoomLimitation(minZoom = MIN_ZOOM, maxZoom = MAX_ZOOM)

            camera.cameraControl.setZoomRatio(zoomRatio)
            camera.cameraInfo.zoomState.verifyZoomState(
                ImmutableZoomState.create(
                    zoomRatio,
                    MAX_ZOOM,
                    MIN_ZOOM,
                    AdapterCameraInfo.getPercentageByRatio(zoomRatio, MIN_ZOOM, MAX_ZOOM),
                )
            )
        }

    private fun Camera.verifyLinearZoom(linearZoom: Float, minZoom: Float, maxZoom: Float) {
        cameraControl.setLinearZoom(linearZoom)
        val zoomState = cameraInfo.zoomState.value
        assertThat(
                zoomState?.isEquivalentTo(
                    ImmutableZoomState.create(
                        AdapterCameraInfo.getZoomRatioByPercentage(linearZoom, minZoom, maxZoom),
                        maxZoom,
                        minZoom,
                        linearZoom,
                    )
                )
            )
            .isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun linearZoomSetWithSessionProcessor(): Unit =
        runBlocking(Dispatchers.Main) {
            val camera =
                bindWithSessionProcessorZoomLimitation(minZoom = MIN_ZOOM, maxZoom = MAX_ZOOM)

            camera.verifyLinearZoom(linearZoom = 0f, MIN_ZOOM, MAX_ZOOM)
            camera.verifyLinearZoom(linearZoom = 1f, MIN_ZOOM, MAX_ZOOM)
            camera.verifyLinearZoom(linearZoom = 0.5f, MIN_ZOOM, MAX_ZOOM)
        }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun linearZoomObserverValueIsCorrect_withSessionProcessor(): Unit =
        runBlocking(Dispatchers.Main) {
            val linearZoom = 0.5f
            val camera =
                bindWithSessionProcessorZoomLimitation(minZoom = MIN_ZOOM, maxZoom = MAX_ZOOM)

            camera.cameraControl.setLinearZoom(linearZoom)
            camera.cameraInfo.zoomState.verifyZoomState(
                ImmutableZoomState.create(
                    AdapterCameraInfo.getZoomRatioByPercentage(linearZoom, MIN_ZOOM, MAX_ZOOM),
                    MAX_ZOOM,
                    MIN_ZOOM,
                    linearZoom,
                )
            )
        }

    private suspend fun LiveData<ZoomState>.verifyZoomState(expectZoomState: ZoomState) {
        val latch = CountDownLatch(1)
        var zoomStates: ZoomState? = null
        withContext(Dispatchers.Main) {
            observeForever {
                // Ignore the case that initial zoom is notified first.
                if (it.zoomRatio == 1.0f && expectZoomState.zoomRatio != 1.0f) {
                    return@observeForever
                }
                zoomStates = it
                latch.countDown()
            }
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(zoomStates?.isEquivalentTo(expectZoomState)).isTrue()
    }

    @RequiresApi(30)
    private suspend fun bindWithSessionProcessorZoomLimitation(
        minZoom: Float,
        maxZoom: Float,
    ): Camera {
        assumeMaxZoomRatio(cameraSelector, atLeastRatio = maxZoom)
        return bindWithSessionProcessor(
            FakeSessionProcessor(
                supportedCameraOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_ZOOM),
                extensionSpecificChars =
                    listOf(
                        Pair(
                            CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE,
                            Range(minZoom, maxZoom),
                        )
                    ),
            )
        )
    }

    private suspend fun bindWithSessionProcessor(sessionProcessor: SessionProcessor): Camera {
        val cameraSelectorWithSessionProcessor =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                cameraSelector,
                sessionProcessor,
            )

        return withContext(Dispatchers.Main) {
            cameraProvider.unbindAll()
            val preview =
                Preview.Builder().build().also {
                    it.setSurfaceProvider {
                        SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                    }
                }
            val imageCapture = ImageCapture.Builder().build()
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelectorWithSessionProcessor,
                preview,
                imageCapture,
            )
        }
    }

    private fun getSensorRect(): Rect {
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraSelector.lensFacing!!)
        val rect = cameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        // Some device like pixel 2 will have (0, 8) as the left-top corner.
        return Rect(0, 0, rect!!.width(), rect.height())
    }

    // TODO: Use ZoomControl#isAndroidRZoomSupported for camera-camera2 config and
    //  ZoomCompat#Bindings#provideZoomRatio for camera-pipe config based on implName
    private fun isAndroidRZoomEnabled(cameraCharacteristics: CameraCharacteristics) =
        Build.VERSION.SDK_INT >= 30 && getZoomRatioRange(cameraCharacteristics) != null

    @RequiresApi(30)
    private fun getZoomRatioRange(cameraCharacteristics: CameraCharacteristics) =
        try {
            cameraCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
        } catch (e: AssertionError) {
            // Some devices may throw AssertionError when failed to get CameraCharacteristic.
            // Catch the AssertionError and return null to workaround it. b/231701345
            null
        }

    private fun <T> assertFutureCompletes(future: ListenableFuture<T>) {
        try {
            future[5, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Assert.fail("future fail:$e")
        }
    }

    private fun assertFutureThrowsIllegalArgumentException(result: ListenableFuture<Void>) {
        try {
            result[100, TimeUnit.MILLISECONDS]
        } catch (e: ExecutionException) {
            assertThat(e.cause).isInstanceOf(java.lang.IllegalArgumentException::class.java)
            return
        }
        Assert.fail()
    }

    private fun areFloatsEqual(num1: Float?, num2: Float?): Boolean {
        if (num1 == null && num2 == null) return true
        if (num1 == null || num2 == null) return false
        return abs(num1 - num2) < 2.0 * Math.ulp(abs(num1).coerceAtLeast(abs(num2)))
    }

    companion object {
        private const val DELTA = 1e-9
        private const val TOLERANCE = 5f
        private const val MIN_ZOOM = 1f
        private const val MAX_ZOOM = 4f

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                    val lens = selector.lensFacing
                    add(
                        arrayOf(
                            "config=${Camera2Config::class.simpleName} lensFacing={$lens}",
                            selector,
                            Camera2Config::class.simpleName,
                            Camera2Config.defaultConfig(),
                        )
                    )
                    add(
                        arrayOf(
                            "config=${CameraPipeConfig::class.simpleName} lensFacing={$lens}",
                            selector,
                            CameraPipeConfig::class.simpleName,
                            CameraPipeConfig.defaultConfig(),
                        )
                    )
                }
            }

        private fun ZoomState.isEquivalentTo(other: ZoomState): Boolean {
            return zoomRatio == other.zoomRatio &&
                maxZoomRatio == other.maxZoomRatio &&
                minZoomRatio == other.minZoomRatio &&
                linearZoom == other.linearZoom
        }
    }
}
