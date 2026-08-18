/*
 * Copyright 2026 The Android Open Source Project
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
import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.hardware.DataSpace
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.applyCamera2Interop
import androidx.camera.camera2.interop.applyCamera2InteropAsync
import androidx.camera.camera2.interop.camera2Interop
import androidx.camera.camera2.interop.cameraCharacteristics
import androidx.camera.camera2.interop.cameraId
import androidx.camera.camera2.interop.toCameraSelector
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.imageAnalysis
import androidx.camera.core.imageCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.preview
import androidx.camera.core.sessionConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * Integration tests for Camera2Interop Kotlin DSL extensions (`camera2Interop` and
 * `applyCamera2InteropAsync`).
 */
@OptIn(ExperimentalCamera2Interop::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class Camera2InteropV2DslIntegrationTest {

    @get:Rule
    val useCamera: TestRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var testExecutor: ExecutorService

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()

        cameraProvider = ProcessCameraProvider.awaitInstance(context)
        assumeTrue(cameraProvider.hasCamera(cameraSelector))
        testExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "test-executor-thread") }
    }

    @After
    fun tearDown() = runBlocking {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync().await()
        }
        if (::testExecutor.isInitialized) {
            testExecutor.shutdown()
        }
    }

    // =========================================================================================
    // Section 1: UseCase.camera2Interop Tests (via DSL)
    // =========================================================================================

    @Test
    fun canConfigureUseCaseInteropOutputOptions() = runBlocking {
        // Arrange
        val preview = preview {
            camera2Interop {
                if (Build.VERSION.SDK_INT >= 24) {
                    surfaceGroupId = 1
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    physicalCameraId = "0"
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    streamUseCase = 0L // STREAM_USE_CASE_DEFAULT
                    timestampBase = 0 // TIMESTAMP_BASE_DEFAULT
                    mirrorMode = 0 // MIRROR_MODE_AUTO
                    dynamicRangeProfile = 1L // DYNAMIC_RANGE_PROFILE_STANDARD
                }
            }
        }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act & Assert
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            assertThat(camera).isNotNull()
        }
    }

    @Test
    fun canConfigureImageAnalysisWithUseCaseInterop() = runBlocking {
        // Arrange
        val imageAnalysis = imageAnalysis {
            camera2Interop {
                if (Build.VERSION.SDK_INT >= 24) {
                    surfaceGroupId = 1
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    streamUseCase = 0L
                }
            }
        }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act & Assert
        withContext(Dispatchers.Main) {
            imageAnalysis.setAnalyzer(CameraXExecutors.highPriorityExecutor()) { it.close() }
            val camera =
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
            assertThat(camera).isNotNull()
        }
    }

    // =========================================================================================
    // Section 2: ImageCapture.camera2Interop Tests (via DSL)
    // =========================================================================================

    @Test
    fun canConfigureImageCaptureOutputOptions() = runBlocking {
        // Arrange
        val imageCapture = imageCapture {
            camera2Interop {
                if (Build.VERSION.SDK_INT >= 24) {
                    surfaceGroupId = 1
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    physicalCameraId = "0"
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    streamUseCase = 0L
                    timestampBase = 0
                    mirrorMode = 0
                    dynamicRangeProfile = 1L
                }
            }
        }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act & Assert
        withContext(Dispatchers.Main) {
            val camera =
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)
            assertThat(camera).isNotNull()
        }
    }

    @Test
    fun canConfigureUseCaseInteropStillOptions() = runBlocking {
        // Arrange
        val stillCaptureLatch = CountDownLatch(1)
        val callbackThreadName = AtomicReference<String>()
        val requestHolder = AtomicReference<CaptureRequest>()

        val preview = preview {}

        val stillCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    callbackThreadName.set(Thread.currentThread().name)
                    requestHolder.set(request)
                    stillCaptureLatch.countDown()
                }
            }

        val imageCapture = imageCapture {
            camera2Interop {
                setStillCaptureCallback(testExecutor, stillCallback)
                stillCaptureRequest[CaptureRequest.CONTROL_AE_MODE] =
                    CaptureRequest.CONTROL_AE_MODE_OFF
                stillCaptureRequestTemplateType = CameraDevice.TEMPLATE_PREVIEW
            }
        }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        }

        // Act
        val imageReceivedLatch = CountDownLatch(1)
        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    imageReceivedLatch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {}
            },
        )

        // Assert
        assertThat(imageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(stillCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(callbackThreadName.get()).isEqualTo("test-executor-thread")
        assertThat(requestHolder.get().get(CaptureRequest.CONTROL_CAPTURE_INTENT))
            .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW)
        assertThat(requestHolder.get().get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    fun canConfigureImageCaptureStillCallbackDirectExecutor() = runBlocking {
        // Arrange
        val stillCaptureLatch = CountDownLatch(1)
        val preview = preview {}

        val imageCapture = imageCapture {
            camera2Interop {
                stillCaptureCallback =
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            stillCaptureLatch.countDown()
                        }
                    }
            }
        }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
        }

        // Act
        val imageReceivedLatch = CountDownLatch(1)
        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    imageReceivedLatch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {}
            },
        )

        // Assert
        assertThat(imageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(stillCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun stillCaptureDoesNotTriggerPreviewInteropCallback() = runBlocking {
        // Arrange
        val stillCaptureLatch = CountDownLatch(1)
        val stillRequestHolder = AtomicReference<CaptureRequest>()

        val previewCaptureLatch = CountDownLatch(10)
        val previewRequests = mutableListOf<CaptureRequest>()

        val preview = preview {}

        val imageCapture = imageCapture {
            camera2Interop {
                stillCaptureCallback =
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            stillRequestHolder.set(request)
                            stillCaptureLatch.countDown()
                        }
                    }
            }
        }

        val sessionConfig =
            sessionConfig(listOf(preview, imageCapture)) {
                camera2Interop {
                    repeatingCaptureCallback =
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                previewRequests.add(request)
                                previewCaptureLatch.countDown()
                            }
                        }
                }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        withContext(Dispatchers.Main) {
            preview.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Act
        val imageReceivedLatch = CountDownLatch(1)
        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    imageReceivedLatch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {}
            },
        )

        // Assert
        assertThat(previewCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(imageReceivedLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(stillCaptureLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(previewRequests).doesNotContain(stillRequestHolder.get())
    }

    // =========================================================================================
    // Section 3: SessionConfig.camera2Interop Tests (via DSL)
    // =========================================================================================

    @Test
    fun canConfigureSessionConfigInterop() = runBlocking {
        // Arrange
        val deviceLatch = CountDownLatch(1)
        val sessionLatch = CountDownLatch(1)
        val captureLatch = CountDownLatch(1)
        val capturedRequest = AtomicReference<CaptureRequest>()
        val capturedTotalResult = AtomicReference<TotalCaptureResult>()

        val preview = preview {}

        val deviceStateCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    deviceLatch.countDown()
                }

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }

        val sessionConfig =
            sessionConfig(listOf(preview)) {
                camera2Interop {
                    setDeviceStateCallback(testExecutor, deviceStateCallback)
                    setSessionStateCallback(
                        testExecutor,
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                sessionLatch.countDown()
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        },
                    )
                    setRepeatingCaptureCallback(
                        testExecutor,
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                capturedRequest.set(request)
                                capturedTotalResult.set(result)
                                captureLatch.countDown()
                            }
                        },
                    )
                    captureRequest[CaptureRequest.CONTROL_AE_MODE] =
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_PREVIEW
                }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Assert
        assertThat(deviceLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(sessionLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()

        val request = capturedRequest.get()
        assertThat(request).isNotNull()
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    fun canConfigureDefaultCallback() = runBlocking {
        // Arrange
        val deviceLatch = CountDownLatch(1)
        val sessionLatch = CountDownLatch(1)
        val captureLatch = CountDownLatch(1)

        val preview = preview {}

        val sessionConfig =
            sessionConfig(listOf(preview)) {
                camera2Interop {
                    deviceStateCallback =
                        object : CameraDevice.StateCallback() {
                            override fun onOpened(camera: CameraDevice) {
                                deviceLatch.countDown()
                            }

                            override fun onDisconnected(camera: CameraDevice) {}

                            override fun onError(camera: CameraDevice, error: Int) {}
                        }
                    sessionStateCallback =
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                sessionLatch.countDown()
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }
                    repeatingCaptureCallback =
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                captureLatch.countDown()
                            }
                        }
                }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Assert
        assertThat(deviceLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(sessionLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun repeatingCaptureCallback_invokedOncePerFrame_whenFourUseCasesBound() = runBlocking {
        // Arrange
        val frameCounts = mutableMapOf<Long, Int>()
        val totalFramesLatch = CountDownLatch(15)

        val preview = preview {}
        val imageCapture = imageCapture {}
        val imageAnalysis =
            imageAnalysis {}
                .apply { setAnalyzer(CameraXExecutors.highPriorityExecutor()) { it.close() } }
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        val sessionConfig =
            sessionConfig(listOf(preview, imageCapture, videoCapture, imageAnalysis)) {
                camera2Interop {
                    repeatingCaptureCallback =
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                synchronized(frameCounts) {
                                    val current = frameCounts[result.frameNumber] ?: 0
                                    frameCounts[result.frameNumber] = current + 1
                                }
                                totalFramesLatch.countDown()
                            }
                        }
                }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Assert
        assertThat(totalFramesLatch.await(10, TimeUnit.SECONDS)).isTrue()

        // Verify that every frame number received was called exactly once (no duplicates per frame)
        val duplicateFrames = synchronized(frameCounts) { frameCounts.filterValues { it > 1 } }
        assertThat(duplicateFrames).isEmpty()
    }

    @Test
    fun repeatingCaptureCallback_inSessionConfig_isNotInvokedForOneShotRequest() = runBlocking {
        // Arrange
        val repeatingCallbackLatch = CountDownLatch(5)
        val stillCaptureCallbackLatch = CountDownLatch(1)

        val preview = preview {}
        val imageCapture = imageCapture {}

        val sessionConfig =
            sessionConfig(listOf(preview, imageCapture)) {
                camera2Interop {
                    repeatingCaptureCallback =
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                repeatingCallbackLatch.countDown()
                                val intent = request[CaptureRequest.CONTROL_CAPTURE_INTENT]
                                if (intent == CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE) {
                                    stillCaptureCallbackLatch.countDown()
                                }
                            }
                        }
                }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Verify repeating callback is invoked for preview repeating requests
        assertThat(repeatingCallbackLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // Act: trigger a one-shot request via imageCapture.takePicture
        val pictureTakenLatch = CountDownLatch(1)
        imageCapture.takePicture(
            CameraXExecutors.mainThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    pictureTakenLatch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {}
            },
        )

        // Assert: still picture succeeds AND repeatingCaptureCallback in SessionConfig is NOT
        // invoked for the one-shot request
        assertThat(pictureTakenLatch.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(stillCaptureCallbackLatch.await(3, TimeUnit.SECONDS)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun canConfigureColorSpaceAndVerifyImageColorSpace() = runBlocking {
        val characteristics =
            Camera2Interop.getCameraCharacteristics(cameraProvider.getCameraInfo(cameraSelector))
        val profiles =
            characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_COLOR_SPACE_PROFILES)
        assumeTrue(profiles != null)
        val supportedColorSpaces = profiles!!.getSupportedColorSpaces(ImageFormat.YUV_420_888)
        assumeTrue(supportedColorSpaces.contains(ColorSpace.Named.DISPLAY_P3))

        // Arrange
        val frameLatch = CountDownLatch(1)
        val capturedDataSpace = AtomicReference<Int>()

        val preview = preview {}
        val imageAnalysis = imageAnalysis {}
        imageAnalysis.setAnalyzer(testExecutor) { imageProxy ->
            if (imageProxy.image != null) {
                capturedDataSpace.set(imageProxy.image!!.dataSpace)
                frameLatch.countDown()
            }
            imageProxy.close()
        }

        val sessionConfig =
            sessionConfig(listOf(preview, imageAnalysis)) {
                camera2Interop { colorSpace = ColorSpace.Named.DISPLAY_P3 }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Assert frame received and data space matches DISPLAY_P3
        assertThat(frameLatch.await(5, TimeUnit.SECONDS)).isTrue()
        val dataSpace = capturedDataSpace.get()
        assertThat(dataSpace).isEqualTo(DataSpace.DATASPACE_DISPLAY_P3)
    }

    @Test
    fun canClearCaptureRequestOptionInSessionConfig() = runBlocking {
        // Arrange
        val captureLatch = CountDownLatch(1)
        val capturedRequest = AtomicReference<CaptureRequest>()
        val preview = preview {}

        val sessionConfig =
            sessionConfig(listOf(preview)) {
                camera2Interop {
                    captureRequest[CaptureRequest.CONTROL_AE_MODE] =
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
                    repeatingCaptureCallback =
                        object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult,
                            ) {
                                capturedRequest.set(request)
                                captureLatch.countDown()
                            }
                        }
                }
            }

        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        // Act
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            )
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, sessionConfig)
        }

        // Assert
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
        val request = capturedRequest.get()
        assertThat(request).isNotNull()
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
            .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    // =========================================================================================
    // Section 4: CameraControl.applyCamera2InteropAsync Tests (via DSL)
    // =========================================================================================

    @Test
    fun canConfigureCameraControlInterop() = runBlocking {
        // Arrange
        val captureLatch = CountDownLatch(5)
        val capturedRequest = AtomicReference<CaptureRequest>()
        val callbackThreadName = AtomicReference<String>()

        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }
        assertThat(camera).isNotNull()

        val repeatingCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    callbackThreadName.set(Thread.currentThread().name)
                    capturedRequest.set(request)
                    captureLatch.countDown()
                }
            }

        camera.cameraControl
            .applyCamera2InteropAsync {
                repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_RECORD
                setRepeatingCaptureCallback(testExecutor, repeatingCallback)
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
            }
            .await()

        // Assert
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(callbackThreadName.get()).isEqualTo("test-executor-thread")

        val request = capturedRequest.get()
        assertThat(request).isNotNull()
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
            .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD)

        // Act: Clear option
        val captureLatch2 = CountDownLatch(5)
        val capturedRequest2 = AtomicReference<CaptureRequest>()
        camera.cameraControl
            .applyCamera2InteropAsync {
                setRepeatingCaptureCallback(
                    testExecutor,
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            capturedRequest2.set(request)
                            captureLatch2.countDown()
                        }
                    },
                )
                clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
            }
            .await()

        assertThat(captureLatch2.await(5, TimeUnit.SECONDS)).isTrue()
        val request2 = capturedRequest2.get()
        assertThat(request2).isNotNull()
        assertThat(request2.get(CaptureRequest.CONTROL_AE_MODE))
            .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    fun canConfigureCameraControlInteropViaSuspendApplyCamera2Interop() = runBlocking {
        // Arrange
        val captureLatch = CountDownLatch(5)
        val capturedRequest = AtomicReference<CaptureRequest>()
        val callbackThreadName = AtomicReference<String>()

        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }
        assertThat(camera).isNotNull()

        val repeatingCallback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    callbackThreadName.set(Thread.currentThread().name)
                    capturedRequest.set(request)
                    captureLatch.countDown()
                }
            }

        // Act: Apply interop directly using suspend applyCamera2Interop extension
        camera.cameraControl.applyCamera2Interop {
            repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_RECORD
            setRepeatingCaptureCallback(testExecutor, repeatingCallback)
            captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
        }

        // Assert
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(callbackThreadName.get()).isEqualTo("test-executor-thread")

        val request = capturedRequest.get()
        assertThat(request).isNotNull()
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
            .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_VIDEO_RECORD)
    }

    @Test
    fun canConfigureCameraControlDirectExecutorCallback() = runBlocking {
        // Arrange
        val captureLatch = CountDownLatch(5)
        val capturedRequest = AtomicReference<CaptureRequest>()

        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

        camera.cameraControl
            .applyCamera2InteropAsync {
                repeatingCaptureCallback =
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            capturedRequest.set(request)
                            captureLatch.countDown()
                        }
                    }
            }
            .await()

        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(capturedRequest.get()).isNotNull()
    }

    @Test
    fun canClearAllCaptureRequestOptions() = runBlocking {
        // Arrange
        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

        // Set capture request options
        camera.cameraControl
            .applyCamera2InteropAsync {
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
                captureRequest[CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION] = 1
            }
            .await()

        // Act: Clear all capture request options
        val captureLatch = CountDownLatch(1)
        val capturedRequest = AtomicReference<CaptureRequest>()
        camera.cameraControl
            .applyCamera2InteropAsync {
                repeatingCaptureCallback =
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult,
                        ) {
                            val aeMode = request.get(CaptureRequest.CONTROL_AE_MODE)
                            val expComp =
                                request.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)
                            val isAeOff = CaptureRequest.CONTROL_AE_MODE_OFF == aeMode
                            val isExpComp1 = expComp == 1
                            if (!isAeOff && !isExpComp1) {
                                capturedRequest.set(request)
                                captureLatch.countDown()
                            }
                        }
                    }
                clearAllCaptureRequestOptions()
            }
            .await()

        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
        val request2 = capturedRequest.get()
        assertThat(request2).isNotNull()
        assertThat(request2.get(CaptureRequest.CONTROL_AE_MODE))
            .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(request2.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION)).isNotEqualTo(1)
    }

    @Test
    fun multipleCaptureRequestOptionsSetInSingleUpdateWhenAppliedViaCameraControl() = runBlocking {
        // Arrange
        val captureLatch = CountDownLatch(1)
        val capturedRequest = AtomicReference<CaptureRequest>()

        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

        val repeatingCallback =
            object : CameraCaptureSession.CaptureCallback() {
                var isFirstCapture = true

                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    val aeMode = request.get(CaptureRequest.CONTROL_AE_MODE)
                    val afMode = request.get(CaptureRequest.CONTROL_AF_MODE)
                    val isAeOff = CaptureRequest.CONTROL_AE_MODE_OFF == aeMode
                    val isAfOff = CaptureRequest.CONTROL_AF_MODE_OFF == afMode
                    if (isAeOff && isAfOff && isFirstCapture) {
                        capturedRequest.set(request)
                        captureLatch.countDown()
                    }
                    isFirstCapture = false
                }
            }

        // Act: Apply multiple capture request keys in one applyCamera2InteropAsync call
        camera.cameraControl
            .applyCamera2InteropAsync {
                setRepeatingCaptureCallback(testExecutor, repeatingCallback)
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
                captureRequest[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_OFF
            }
            .await()

        // Assert: Both keys are updated in the same repeating request call
        assertThat(captureLatch.await(5, TimeUnit.SECONDS)).isTrue()
        val request = capturedRequest.get()
        assertThat(request).isNotNull()
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(request.get(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
    }

    @Test
    fun applyInteropAsyncReturnsListenableFutureThatCompletesWhenUpdated() = runBlocking {
        // Arrange
        val captureWithKeyLatch = CountDownLatch(1)
        val futureCompletionLatch = CountDownLatch(1)
        val updatedRequestRef = AtomicReference<CaptureRequest>()
        val keyUpdatedBeforeOrAtFutureCompletion = AtomicBoolean(false)

        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

        // Act: Apply interop with key AND repeating capture callback
        val step1Callback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    val aeMode = request.get(CaptureRequest.CONTROL_AE_MODE)
                    val isAeOff = CaptureRequest.CONTROL_AE_MODE_OFF == aeMode
                    if (isAeOff) {
                        updatedRequestRef.set(request)
                        captureWithKeyLatch.countDown()
                    }
                }
            }

        val future =
            camera.cameraControl.applyCamera2InteropAsync {
                setRepeatingCaptureCallback(testExecutor, step1Callback)
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
            }

        // Attach a listener to verify the state at the exact moment the future completes
        future.addListener(
            {
                if (updatedRequestRef.get() != null) {
                    keyUpdatedBeforeOrAtFutureCompletion.set(true)
                }
                futureCompletionLatch.countDown()
            },
            testExecutor,
        )

        // Assert 1: The ListenableFuture completes successfully
        assertThat(future.await()).isNull()
        assertThat(futureCompletionLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // Assert 2: Verify repeating request callbacks confirmed AE_MODE_OFF update
        assertThat(captureWithKeyLatch.await(5, TimeUnit.SECONDS)).isTrue()
        val request = updatedRequestRef.get()
        assertThat(request).isNotNull()
        assertThat(request.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)

        // Assert 3: Verify future completed as a result of/after key was updated
        assertThat(keyUpdatedBeforeOrAtFutureCompletion.get()).isTrue()
    }

    @Test
    fun applyInteropAsyncFailsWithOperationCanceledExceptionWhenOverwritten() = runBlocking {
        // Arrange
        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

        // Act: Issue first interop call and immediately override with a second interop call
        val future1 =
            camera.cameraControl.applyCamera2InteropAsync {
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
            }

        val future2 =
            camera.cameraControl.applyCamera2InteropAsync {
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_ON
            }

        // Assert: Second future completes successfully
        assertThat(future2.await()).isNull()

        // Assert: First future failed because it was canceled by the newer request
        assertThat(future1.isDone).isTrue()
        val exception = assertThrows(ExecutionException::class.java) { future1.get() }
        assertThat(exception.cause)
            .isInstanceOf(CameraControl.OperationCanceledException::class.java)
    }

    @Test
    fun captureRequestKeysAreAdditiveWhenAppliedViaCameraControl() = runBlocking {
        // Arrange
        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }

        // Step 1: Set AE_MODE to OFF via first applyCamera2InteropAsync call
        camera.cameraControl
            .applyCamera2InteropAsync {
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
            }
            .await()

        // Step 2: Set AF_MODE to OFF via second applyCamera2InteropAsync call
        val step2Latch = CountDownLatch(1)
        val step2RequestRef = AtomicReference<CaptureRequest>()
        val step2Callback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    val aeMode = request.get(CaptureRequest.CONTROL_AE_MODE)
                    val afMode = request.get(CaptureRequest.CONTROL_AF_MODE)
                    val isAeOff = CaptureRequest.CONTROL_AE_MODE_OFF == aeMode
                    val isAfOff = CaptureRequest.CONTROL_AF_MODE_OFF == afMode
                    if (isAeOff && isAfOff) {
                        step2RequestRef.set(request)
                        step2Latch.countDown()
                    }
                }
            }

        camera.cameraControl
            .applyCamera2InteropAsync {
                setRepeatingCaptureCallback(testExecutor, step2Callback)
                captureRequest[CaptureRequest.CONTROL_AF_MODE] = CaptureRequest.CONTROL_AF_MODE_OFF
            }
            .await()

        // Assert Step 2: Both AE_MODE and AF_MODE present in request (additive)
        assertThat(step2Latch.await(5, TimeUnit.SECONDS)).isTrue()
        val step2Request = step2RequestRef.get()
        assertThat(step2Request).isNotNull()
        assertThat(step2Request.get(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(step2Request.get(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)

        // Step 3: Explicitly clear AE_MODE key
        val step3Latch = CountDownLatch(1)
        val step3RequestRef = AtomicReference<CaptureRequest>()
        val step3Callback =
            object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    val aeMode = request.get(CaptureRequest.CONTROL_AE_MODE)
                    val afMode = request.get(CaptureRequest.CONTROL_AF_MODE)
                    val isAeOff = CaptureRequest.CONTROL_AE_MODE_OFF == aeMode
                    val isAfOff = CaptureRequest.CONTROL_AF_MODE_OFF == afMode
                    if (!isAeOff && isAfOff) {
                        step3RequestRef.set(request)
                        step3Latch.countDown()
                    }
                }
            }

        camera.cameraControl
            .applyCamera2InteropAsync {
                setRepeatingCaptureCallback(testExecutor, step3Callback)
                clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
            }
            .await()

        // Assert Step 3: AF_MODE is still OFF, but AE_MODE was cleared
        assertThat(step3Latch.await(5, TimeUnit.SECONDS)).isTrue()
        val step3Request = step3RequestRef.get()
        assertThat(step3Request).isNotNull()
        assertThat(step3Request.get(CaptureRequest.CONTROL_AE_MODE))
            .isNotEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(step3Request.get(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
    }

    // =========================================================================================
    // Section 5: Camera2Interop Static Metadata Utilities & Kotlin Extensions
    // =========================================================================================

    @Test
    fun canUseMetadataApis() = runBlocking {
        // Arrange
        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }
        assertThat(camera).isNotNull()
        val cameraInfo = camera.cameraInfo

        // Assert
        // Test Camera2Interop.getCameraId(CameraInfo)
        val cameraId = Camera2Interop.getCameraId(cameraInfo)
        assertThat(cameraId).isNotEmpty()

        // Test Camera2Interop.getCameraCharacteristics(CameraInfo)
        val characteristics = Camera2Interop.getCameraCharacteristics(cameraInfo)
        assertThat(characteristics).isNotNull()

        // Test Camera2Interop.getCameraSelectorFromCameraId(String)
        val selectorFromId = Camera2Interop.getCameraSelectorFromCameraId(cameraId)
        assertThat(selectorFromId).isNotNull()

        // Test Camera2Interop.getCameraFilterFromCameraId(String)
        val filter = Camera2Interop.getCameraFilterFromCameraId(cameraId)
        assertThat(filter).isNotNull()

        // Verify we can bind with the new selector
        val preview2 = preview {}
        withContext(Dispatchers.Main) {
            preview2.surfaceProvider =
                SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
            val camera2 = cameraProvider.bindToLifecycle(lifecycleOwner, selectorFromId, preview2)
            assertThat(Camera2Interop.getCameraId(camera2.cameraInfo)).isEqualTo(cameraId)
        }
    }

    @Test
    fun canUseMetadataKotlinExtensions() = runBlocking {
        // Arrange
        val preview = preview {}
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.RESUMED)

        val camera =
            withContext(Dispatchers.Main) {
                preview.surfaceProvider =
                    SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            }
        val cameraInfo = camera.cameraInfo

        // Test Kotlin Extension Properties
        val id = cameraInfo.cameraId
        assertThat(id).isNotEmpty()

        val characteristics = cameraInfo.cameraCharacteristics
        assertThat(characteristics).isNotNull()

        val selector = id.toCameraSelector()
        assertThat(selector).isNotNull()
    }
}
