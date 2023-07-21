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

package androidx.camera.camera2.pipe.integration

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_MODE
import android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_MODE_FAST
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_OFF
import android.hardware.camera2.CaptureRequest.CONTROL_AE_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_OFF
import android.hardware.camera2.CaptureRequest.CONTROL_AF_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE
import android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE_OFF
import android.hardware.camera2.CaptureRequest.CONTROL_AWB_REGIONS
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT
import android.hardware.camera2.CaptureRequest.CONTROL_CAPTURE_INTENT_MANUAL
import android.hardware.camera2.CaptureRequest.Key
import android.hardware.camera2.CaptureRequest.SCALER_CROP_REGION
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.Camera2Interop
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.testing.VerifyResultListener
import androidx.camera.camera2.pipe.testing.toCameraControlAdapter
import androidx.camera.camera2.pipe.testing.toCameraInfoAdapter
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCase
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCamera2Interop::class)
@SdkSuppress(minSdkVersion = 21)
class Camera2CameraControlDeviceTest {
    private lateinit var cameraSelector: CameraSelector
    private lateinit var context: Context
    private lateinit var camera: CameraUseCaseAdapter
    private lateinit var camera2CameraControl: Camera2CameraControl
    private lateinit var cameraControl: CameraControlAdapter
    private lateinit var comboListener: ComboRequestListener

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest()

    @Before
    fun setUp() {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))

        context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(
            context,
            CameraPipeConfig.defaultConfig()
        )
        cameraSelector = CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK
        ).build()
        camera = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)
        cameraControl = camera.cameraControl.toCameraControlAdapter()
        camera2CameraControl = cameraControl.camera2cameraControl
        comboListener = camera2CameraControl.requestListener
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::camera.isInitialized) {
            withContext(Dispatchers.Main) {
                camera.removeUseCases(camera.useCases)
            }
        }

        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun canGetInteropApi() {
        Truth.assertThat(
            Camera2CameraControl.from(cameraControl)
        ).isSameInstanceAs(camera2CameraControl)
    }

    @Test
    fun canSetAndRetrieveCaptureRequestOptions() {
        // Arrange.
        bindUseCase()
        val builder: CaptureRequestOptions.Builder =
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption<Int>(
                    CONTROL_CAPTURE_INTENT,
                    CONTROL_CAPTURE_INTENT_MANUAL
                )
                .setCaptureRequestOption<Int>(
                    COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_FAST
                )
        // Act.
        camera2CameraControl.setCaptureRequestOptions(builder.build())

        // Assert.
        Truth.assertThat(
            camera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                CONTROL_CAPTURE_INTENT, null
            )
        ).isEqualTo(
            CONTROL_CAPTURE_INTENT_MANUAL
        )
        Truth.assertThat(
            camera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                COLOR_CORRECTION_MODE, null
            )
        ).isEqualTo(
            CameraMetadata.COLOR_CORRECTION_MODE_FAST
        )
    }

    @Test
    fun canSubmitCaptureRequestOptions_beforeBinding() = runBlocking {
        val future = updateCamera2Option<Int>(
            CONTROL_CAPTURE_INTENT,
            CONTROL_CAPTURE_INTENT_MANUAL
        )
        bindUseCase()
        assertFutureCompletes(future)

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_CAPTURE_INTENT] == CONTROL_CAPTURE_INTENT_MANUAL
            },
        )
    }

    @Test
    fun canSubmitCaptureRequestOptions_afterBinding() = runBlocking {
        // Arrange.
        bindUseCase()

        // Act.
        val future = updateCamera2Option(
            CONTROL_CAPTURE_INTENT,
            CONTROL_CAPTURE_INTENT_MANUAL
        )
        assertFutureCompletes(future)

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_CAPTURE_INTENT] == CONTROL_CAPTURE_INTENT_MANUAL
            },
        )
    }

    @Test
    fun canClearCaptureRequestOptions() = runBlocking {
        // Arrange.
        bindUseCase()
        val builder: CaptureRequestOptions.Builder =
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption<Int>(
                    CONTROL_CAPTURE_INTENT,
                    CONTROL_CAPTURE_INTENT_MANUAL
                )
                .setCaptureRequestOption<Int>(
                    COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_FAST
                )
        assertFutureCompletes(camera2CameraControl.setCaptureRequestOptions(builder.build()))

        // Act.
        builder.clearCaptureRequestOption<Int>(COLOR_CORRECTION_MODE)
        assertFutureCompletes(camera2CameraControl.setCaptureRequestOptions(builder.build()))

        // Assert.
        Truth.assertThat(
            camera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                CONTROL_CAPTURE_INTENT, null
            )
        ).isEqualTo(
            CONTROL_CAPTURE_INTENT_MANUAL
        )
        Truth.assertThat(
            camera2CameraControl.getCaptureRequestOptions().getCaptureRequestOption(
                COLOR_CORRECTION_MODE, null
            )
        ).isEqualTo(null)

        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_CAPTURE_INTENT] == CONTROL_CAPTURE_INTENT_MANUAL &&
                    requestMetadata.request[COLOR_CORRECTION_MODE] != COLOR_CORRECTION_MODE_FAST
            },
        )
    }

    @Test
    fun canOverrideAfMode() = runBlocking {
        updateCamera2Option(
            CONTROL_AF_MODE,
            CONTROL_AF_MODE_OFF
        )
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_AF_MODE] == CONTROL_AF_MODE_OFF
            },
        )
    }

    @Test
    fun canOverrideAeMode() = runBlocking {
        updateCamera2Option(
            CONTROL_AE_MODE,
            CONTROL_AE_MODE_OFF
        )
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_AE_MODE] == CONTROL_AE_MODE_OFF
            },
        )
    }

    @Test
    fun canOverrideAwbMode() = runBlocking {
        updateCamera2Option(
            CONTROL_AWB_MODE,
            CONTROL_AWB_MODE_OFF
        )
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_AWB_MODE] == CONTROL_AWB_MODE_OFF
            },
        )
    }

    @Test
    fun canOverrideScalarCropRegion() = runBlocking {
        // scalar crop region must be larger than the region defined
        // by SCALER_AVAILABLE_MAX_DIGITAL_ZOOM otherwise it could cause a crash on some devices.
        // Thus we cannot simply specify some random crop region.

        // Arrange.
        val cropRegion = getZoom2XCropRegion()

        // Act.
        updateCamera2Option(SCALER_CROP_REGION, cropRegion)
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[SCALER_CROP_REGION] == cropRegion
            },
        )
    }

    @Test
    fun canOverrideAfRegion() = runBlocking {
        // Arrange.
        val meteringRectangles = arrayOf(
            MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        )

        // Act.
        updateCamera2Option(CONTROL_AF_REGIONS, meteringRectangles)
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_AF_REGIONS] == meteringRectangles
            },
        )
    }

    @Test
    fun canOverrideAeRegion() = runBlocking {
        // Arrange.
        val meteringRectangles = arrayOf(
            MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        )

        // Act.
        updateCamera2Option(CONTROL_AE_REGIONS, meteringRectangles)
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_AE_REGIONS] == meteringRectangles
            },
        )
    }

    @Test
    fun canOverrideAwbRegion() = runBlocking {
        // Arrange.
        val meteringRectangles = arrayOf(
            MeteringRectangle(0, 0, 100, 100, MeteringRectangle.METERING_WEIGHT_MAX)
        )

        // Act.
        updateCamera2Option(CONTROL_AWB_REGIONS, meteringRectangles)
        bindUseCase()

        // Assert.
        registerListener().verify(
            { requestMetadata: RequestMetadata, _ ->
                requestMetadata.request[CONTROL_AWB_REGIONS] == meteringRectangles
            },
        )
    }

    @Test
    fun cancelPendingFuture_whenInactive() {
        // Arrange.
        val future = updateCamera2Option(
            CONTROL_CAPTURE_INTENT,
            CONTROL_CAPTURE_INTENT_MANUAL
        )

        // Act.
        camera.detachUseCases()

        // Assert.
        try {
            future.get()
        } catch (e: ExecutionException) {
            Truth.assertThat(e.cause)
                .isInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Suppress("DEPRECATION")
    @Test
    fun canSetPhysicalCameraId() = runBlocking {
        val physicalCameraIds = CameraUtil.getCameraCharacteristics(
            cameraSelector.lensFacing!!
        )!!.physicalCameraIds.toList()

        // Skip the test if the camera is not a logical camera.
        Assume.assumeTrue(physicalCameraIds.isNotEmpty())

        // Arrange.
        val physicalCameraId = physicalCameraIds[0]
        val useCase = ImageAnalysis.Builder().also { imageAnalysisBuilder ->
            Camera2Interop.Extender(imageAnalysisBuilder).setPhysicalCameraId(
                physicalCameraId
            )
        }.build().apply {
            // set analyzer to make it active.
            setAnalyzer(Dispatchers.Default.asExecutor()) {
                // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent the
                // closing of the CameraDevice from being stuck.
                it.close()
            }
        }

        // Act.
        bindUseCase(useCase)

        // Assert.
        registerListener().verify(
            { _, captureResult: FrameInfo ->
                captureResult.unwrapAs(TotalCaptureResult::class)!!.let { totalCaptureResult ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        totalCaptureResult.physicalCameraTotalResults.containsKey(
                            physicalCameraId
                        )
                    } else {
                        totalCaptureResult.physicalCameraResults.containsKey(physicalCameraId)
                    }
                }
            },
        )
    }

    private fun getZoom2XCropRegion(): Rect {
        val cameraManager =
            InstrumentationRegistry.getInstrumentation().context.getSystemService(
                Context.CAMERA_SERVICE
            ) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(
            camera.cameraInfo.toCameraInfoAdapter().cameraId
        )

        val maxDigitalZoom = characteristics.get(
            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM
        )

        Assume.assumeTrue(maxDigitalZoom != null && maxDigitalZoom >= 2)

        return characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            .let { sensorRect ->
                Rect(
                    sensorRect!!.centerX() - sensorRect.width() / 4,
                    sensorRect.centerY() - sensorRect.height() / 4,
                    sensorRect.centerX() + sensorRect.width() / 4,
                    sensorRect.centerY() + sensorRect.height() / 4
                )
            }
    }

    private fun registerListener(capturesCount: Int = 60): VerifyResultListener =
        VerifyResultListener(capturesCount).also {
            comboListener.addListener(it, Dispatchers.Default.asExecutor())
        }

    private fun <T> updateCamera2Option(key: Key<T>, value: T) =
        camera2CameraControl.setCaptureRequestOptions(
            CaptureRequestOptions.Builder().setCaptureRequestOption(key, value).build()
        )

    private fun <T> assertFutureCompletes(future: ListenableFuture<T?>): T? {
        var result: T? = null
        try {
            result = future[5, TimeUnit.SECONDS]
        } catch (e: Exception) {
            Assert.fail("future fail:$e")
        }
        return result
    }

    private fun bindUseCase(
        useCase: UseCase = ImageAnalysis.Builder().build().apply {
            // set analyzer to make it active.
            setAnalyzer(Dispatchers.Default.asExecutor()) {
                // Fake analyzer, do nothing. Close the ImageProxy immediately to prevent the
                // closing of the CameraDevice from being stuck.
                it.close()
            }
        }
    ) {
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            cameraSelector,
            useCase,
        )
        camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)
    }
}