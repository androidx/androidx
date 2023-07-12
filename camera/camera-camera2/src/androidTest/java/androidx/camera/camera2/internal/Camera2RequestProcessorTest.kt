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

package androidx.camera.camera2.internal

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.params.DynamicRangesCompat
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks
import androidx.camera.camera2.internal.util.RequestProcessorRequest
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessorSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val CAMERA_ID = "0"
const val PREVIEW_OUTPUT_CONFIG_ID = 1
const val CAPTURE_OUTPUT_CONFIG_ID = 2
const val INVALID_OUTPUT_CONFIG_ID = 99999
const val ORIENTATION_1 = 90
const val ORIENTATION_2 = 270

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class Camera2RequestProcessorTest {
    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private lateinit var cameraDeviceHolder: CameraUtil.CameraDeviceHolder
    private lateinit var captureSessionRepository: CaptureSessionRepository
    private lateinit var dynamicRangesCompat: DynamicRangesCompat
    private lateinit var captureSessionOpenerBuilder: SynchronizedCaptureSessionOpener.Builder
    private lateinit var mainThreadExecutor: Executor
    private lateinit var previewSurface: SessionProcessorSurface
    private lateinit var captureSurface: SessionProcessorSurface
    private val captureImagesRetrieved =
        listOf(CompletableDeferred<Unit>(), CompletableDeferred<Unit>())
    private var numOfCapturedImages = 0
    private val previewImageRetrieved = CompletableDeferred<Unit>()

    private fun getCameraCharacteristic(cameraId: String): CameraCharacteristicsCompat {
        val cameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager

        return CameraCharacteristicsCompat.toCameraCharacteristicsCompat(
            cameraManager.getCameraCharacteristics(cameraId),
            cameraId
        )
    }

    @Before
    fun setUp() {
        val handler = Handler(Looper.getMainLooper())
        mainThreadExecutor = CameraXExecutors.newHandlerExecutor(handler)
        captureSessionRepository = CaptureSessionRepository(mainThreadExecutor)
        val cameraCharacteristics = getCameraCharacteristic(CAMERA_ID)
        dynamicRangesCompat = DynamicRangesCompat.fromCameraCharacteristics(cameraCharacteristics)
        captureSessionOpenerBuilder = SynchronizedCaptureSessionOpener.Builder(
            mainThreadExecutor,
            mainThreadExecutor as ScheduledExecutorService,
            handler,
            captureSessionRepository,
            CameraQuirks.get(CAMERA_ID, cameraCharacteristics),
            DeviceQuirks.getAll()
        )

        cameraDeviceHolder = CameraUtil.getCameraDevice(
            CAMERA_ID,
            captureSessionRepository.cameraStateCallback
        )

        // Preview Surface
        val surfaceTexture = SurfaceTexture(0)
        surfaceTexture.setDefaultBufferSize(640, 480)
        surfaceTexture.setOnFrameAvailableListener { previewImageRetrieved.complete(Unit) }
        val surface = Surface(surfaceTexture)
        previewSurface = SessionProcessorSurface(surface, PREVIEW_OUTPUT_CONFIG_ID)
        previewSurface.terminationFuture.addListener(
            {
                surfaceTexture.release()
                surface.release()
            },
            CameraXExecutors.directExecutor()
        )

        // Capture Surface
        val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
        imageReader.setOnImageAvailableListener(
            {
                if (numOfCapturedImages <= 1) {
                    captureImagesRetrieved[numOfCapturedImages].complete(Unit)
                    numOfCapturedImages++
                }
                val image = imageReader.acquireNextImage()
                image.close()
            },
            handler
        )
        captureSurface = SessionProcessorSurface(imageReader.surface, CAPTURE_OUTPUT_CONFIG_ID)
        captureSurface.terminationFuture.addListener(
            {
                imageReader.close()
            },
            CameraXExecutors.directExecutor()
        )
    }

    @After
    fun tearDown() {
        CameraUtil.releaseCameraDevice(cameraDeviceHolder)
        previewSurface.close()
        captureSurface.close()
    }

    private fun getSessionConfig(): SessionConfig {
        return SessionConfig.Builder().apply {
            addSurface(previewSurface)
            addSurface(captureSurface)
            setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
        }.build()
    }

    @Test
    fun canSubmit(): Unit = runBlocking {
        // Arrange
        val captureSession = CaptureSession(dynamicRangesCompat)
        val cameraDevice = cameraDeviceHolder.get()!!
        captureSession.open(
            getSessionConfig(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        ).await()
        val requestProcessor = Camera2RequestProcessor(
            captureSession,
            listOf(previewSurface, captureSurface)
        )
        val request = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(CAPTURE_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
            setParameters(
                CaptureRequestOptions.Builder().apply {
                    setCaptureRequestOption(
                        CaptureRequest.JPEG_ORIENTATION,
                        ORIENTATION_1
                    )
                }.build()
            )
        }.build()
        val callbackToVerify = CallbackToVerify(numOfCaptureResultToVerify = 1)

        // Act
        val sequenceId = requestProcessor.submit(
            request, callbackToVerify
        )

        // Assert
        withTimeout(5000) {
            val (captureResult, receivedRequest) = callbackToVerify.awaitCaptureResults()[0]
            val camera2CaptureResult =
                Camera2CameraCaptureResultConverter.getCaptureResult(captureResult)!!
            assertThat(camera2CaptureResult.request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE)
            assertThat(camera2CaptureResult.request.get(CaptureRequest.JPEG_ORIENTATION))
                .isEqualTo(ORIENTATION_1)

            assertThat(receivedRequest).isSameInstanceAs(request)
            captureImagesRetrieved[0].await()
            assertThat(callbackToVerify.awaitCaptureSequenceId()).isEqualTo(sequenceId)
        }
    }

    @Test
    fun canSubmitMultipleThreads() {
        // Arrange
        val captureSession = CaptureSession(dynamicRangesCompat)
        val cameraDevice = cameraDeviceHolder.get()!!
        captureSession.open(
            getSessionConfig(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        ).get()
        val requestProcessor = Camera2RequestProcessor(
            captureSession,
            listOf(previewSurface, captureSurface)
        )

        val request1 = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(CAPTURE_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }.build()
        val request2 = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(CAPTURE_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }.build()
        val request3 = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(CAPTURE_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }.build()

        val callbackToVerify1 = CallbackToVerify(numOfCaptureResultToVerify = 1)
        val callbackToVerify2 = CallbackToVerify(numOfCaptureResultToVerify = 1)
        val callbackToVerify3 = CallbackToVerify(numOfCaptureResultToVerify = 1)

        // Act
        val latchForStart = CountDownLatch(1)
        val thread1 = Thread {
            latchForStart.await()
            requestProcessor.submit(
                request1, callbackToVerify1
            )
        }
        val thread2 = Thread {
            latchForStart.await()
            requestProcessor.submit(
                request2, callbackToVerify2
            )
        }
        mainThreadExecutor.execute {
            latchForStart.await()
            requestProcessor.submit(
                request3, callbackToVerify3
            )
        }
        thread2.start()
        thread1.start()
        latchForStart.countDown() // make sure 3 threads are invoking submit() at the same time.

        // Assert
        runBlocking<Unit> {
            withTimeout(5000) {
                callbackToVerify1.awaitCaptureResults()
                callbackToVerify2.awaitCaptureResults()
                callbackToVerify3.awaitCaptureResults()
            }
        }
    }

    @Test
    fun canSubmitList(): Unit = runBlocking {
        // Arrange
        val captureSession = CaptureSession(dynamicRangesCompat)
        val cameraDevice = cameraDeviceHolder.get()!!
        captureSession.open(
            getSessionConfig(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        ).await()
        val requestProcessor = Camera2RequestProcessor(
            captureSession,
            listOf(previewSurface, captureSurface)
        )
        val request1 = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(CAPTURE_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
            setParameters(
                CaptureRequestOptions.Builder().apply {
                    setCaptureRequestOption(
                        CaptureRequest.JPEG_ORIENTATION,
                        ORIENTATION_1
                    )
                }.build()
            )
        }.build()
        val request2 = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(CAPTURE_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_PREVIEW)
            setParameters(
                CaptureRequestOptions.Builder().apply {
                    setCaptureRequestOption(
                        CaptureRequest.JPEG_ORIENTATION,
                        ORIENTATION_2
                    )
                }.build()
            )
        }.build()
        val callbackToVerify = CallbackToVerify(numOfCaptureResultToVerify = 2)

        // Act
        val sequenceId = requestProcessor.submit(
            listOf(request1, request2),
            callbackToVerify
        )

        // Assert
        withTimeout(5000) {
            val captureResults = callbackToVerify.awaitCaptureResults()
            val (captureResult1, receivedRequest1) = captureResults[0]
            val (captureResult2, receivedRequest2) = captureResults[1]
            captureImagesRetrieved[0].await()
            captureImagesRetrieved[1].await()

            val camera2CaptureResult1 =
                Camera2CameraCaptureResultConverter.getCaptureResult(captureResult1)!!
            assertThat(camera2CaptureResult1.request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE)
            assertThat(camera2CaptureResult1.request.get(CaptureRequest.JPEG_ORIENTATION))
                .isEqualTo(ORIENTATION_1)
            assertThat(receivedRequest1).isSameInstanceAs(request1)

            val camera2CaptureResult2 =
                Camera2CameraCaptureResultConverter.getCaptureResult(captureResult2)!!
            assertThat(camera2CaptureResult2.request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW)
            assertThat(camera2CaptureResult2.request.get(CaptureRequest.JPEG_ORIENTATION))
                .isEqualTo(ORIENTATION_2)
            assertThat(receivedRequest2).isSameInstanceAs(request2)

            assertThat(callbackToVerify.awaitCaptureSequenceId()).isEqualTo(sequenceId)

            // Ensure onCaptureSequenceCompleted is called only once.
            delay(100)
            assertThat(callbackToVerify.getSequenceCompletedCount()).isEqualTo(1)
        }
    }

    @Test
    fun canSetRepeating(): Unit = runBlocking {
        // Arrange
        val captureSession = CaptureSession(dynamicRangesCompat)
        val cameraDevice = cameraDeviceHolder.get()!!
        captureSession.open(
            getSessionConfig(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        ).await()
        val requestProcessor = Camera2RequestProcessor(
            captureSession,
            listOf(previewSurface, captureSurface)
        )
        val request = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(PREVIEW_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_PREVIEW)
            setParameters(
                CaptureRequestOptions.Builder().apply {
                    setCaptureRequestOption(
                        CaptureRequest.JPEG_ORIENTATION,
                        ORIENTATION_1
                    )
                }.build()
            )
        }.build()
        // ensure capture result is received multiple times since it is a repeating request.
        val callbackToVerify = CallbackToVerify(numOfCaptureResultToVerify = 4)

        // Act
        val sequenceId = requestProcessor.setRepeating(
            request, callbackToVerify
        )

        // Assert
        withTimeout(5000) {
            val (captureResult, receivedRequest) = callbackToVerify.awaitCaptureResults()[0]
            previewImageRetrieved.await()
            val camera2CaptureResult =
                Camera2CameraCaptureResultConverter.getCaptureResult(captureResult)!!
            assertThat(camera2CaptureResult.request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))
                .isEqualTo(CaptureResult.CONTROL_CAPTURE_INTENT_PREVIEW)
            assertThat(camera2CaptureResult.request.get(CaptureRequest.JPEG_ORIENTATION))
                .isEqualTo(ORIENTATION_1)
            assertThat(receivedRequest).isSameInstanceAs(request)

            // Repeating request needs to be stopped to trigger onCaptureSequenceCompleted.
            requestProcessor.stopRepeating()
            assertThat(callbackToVerify.awaitCaptureSequenceId()).isEqualTo(sequenceId)
        }
    }

    @Test
    fun closeRequestProcessor(): Unit = runBlocking {
        // Arrange
        val captureSession = CaptureSession(dynamicRangesCompat)
        val cameraDevice = cameraDeviceHolder.get()!!
        captureSession.open(
            getSessionConfig(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        ).await()

        val requestProcessor = Camera2RequestProcessor(
            captureSession,
            listOf(previewSurface, captureSurface)
        )

        // Act
        requestProcessor.close()

        // Assert
        val request = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(PREVIEW_OUTPUT_CONFIG_ID)
            setTemplateId(CameraDevice.TEMPLATE_PREVIEW)
        }.build()

        val callbackToVerify = CallbackToVerify(numOfCaptureResultToVerify = 1)
        assertThat(
            requestProcessor.submit(
                request, callbackToVerify
            )
        ).isEqualTo(-1)
        assertThat(
            requestProcessor.submit(
                listOf(request, request), callbackToVerify
            )
        ).isEqualTo(-1)
        assertThat(
            requestProcessor.setRepeating(
                request, callbackToVerify
            )
        ).isEqualTo(-1)
    }

    @Test
    fun invalidRequest(): Unit = runBlocking {
        // Arrange
        val captureSession = CaptureSession(dynamicRangesCompat)
        val cameraDevice = cameraDeviceHolder.get()!!
        captureSession.open(
            getSessionConfig(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        ).await()

        val requestProcessor = Camera2RequestProcessor(
            captureSession,
            listOf(previewSurface, captureSurface),
        )
        val invalidRequest1 = RequestProcessorRequest.Builder().build()
        val invalidRequest2 = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(INVALID_OUTPUT_CONFIG_ID)
        }.build()
        val validRequest = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(PREVIEW_OUTPUT_CONFIG_ID)
        }.build()
        val callbackToVerify = CallbackToVerify(numOfCaptureResultToVerify = 1)
        // Act and Assert
        assertThat(requestProcessor.submit(invalidRequest1, callbackToVerify)).isEqualTo(-1)
        assertThat(requestProcessor.submit(invalidRequest2, callbackToVerify)).isEqualTo(-1)
        assertThat(
            requestProcessor.submit(
                listOf(validRequest, invalidRequest1),
                callbackToVerify
            )
        ).isEqualTo(-1)
        assertThat(
            requestProcessor.submit(
                listOf(validRequest, invalidRequest2),
                callbackToVerify
            )
        ).isEqualTo(-1)
        assertThat(requestProcessor.setRepeating(invalidRequest1, callbackToVerify)).isEqualTo(-1)
        assertThat(requestProcessor.setRepeating(invalidRequest2, callbackToVerify)).isEqualTo(-1)
    }

    class CallbackToVerify(
        private val numOfCaptureResultToVerify: Int
    ) : RequestProcessor.Callback {
        private val deferredCaptureResults =
            CompletableDeferred<List<Pair<CameraCaptureResult, RequestProcessor.Request>>>()
        private val captureResultsReceived =
            ArrayList<Pair<CameraCaptureResult, RequestProcessor.Request>>()
        private val deferredCaptureSequenceReceived = CompletableDeferred<Int>()
        private var completedCount = 0
        private var sequenceCompleteCount = 0

        suspend fun awaitCaptureResults():
            List<Pair<CameraCaptureResult, RequestProcessor.Request>> {
            return deferredCaptureResults.await()
        }

        suspend fun awaitCaptureSequenceId(): Int {
            return deferredCaptureSequenceReceived.await()
        }

        fun getSequenceCompletedCount(): Int {
            return sequenceCompleteCount
        }

        override fun onCaptureStarted(
            request: RequestProcessor.Request,
            frameNumber: Long,
            timestamp: Long
        ) {
        }

        override fun onCaptureProgressed(
            request: RequestProcessor.Request,
            captureResult: CameraCaptureResult
        ) {
        }

        override fun onCaptureCompleted(
            request: RequestProcessor.Request,
            captureResult: CameraCaptureResult
        ) {
            if (completedCount >= numOfCaptureResultToVerify) {
                return
            }
            completedCount++
            captureResultsReceived.add(captureResult to request)
            if (completedCount == numOfCaptureResultToVerify) {
                deferredCaptureResults.complete(captureResultsReceived)
            }
        }

        override fun onCaptureFailed(
            request: RequestProcessor.Request,
            captureFailure: CameraCaptureFailure
        ) {
        }

        override fun onCaptureBufferLost(
            request: RequestProcessor.Request,
            frameNumber: Long,
            outputConfigId: Int
        ) {
        }

        override fun onCaptureSequenceCompleted(sequenceId: Int, frameNumber: Long) {
            sequenceCompleteCount++
            deferredCaptureSequenceReceived.complete(sequenceId)
        }

        override fun onCaptureSequenceAborted(sequenceId: Int) {
        }
    }
}