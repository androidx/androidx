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
import android.graphics.ImageFormat.JPEG
import android.graphics.ImageFormat.PRIVATE
import android.graphics.ImageFormat.YUV_420_888
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.camera2.internal.util.RequestProcessorRequest
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.OutputSurface
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessorSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.CameraDeviceHolder
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Objects
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

const val FAKE_CAPTURE_SEQUENCE_ID = 1
const val JPEG_ORIENTATION_VALUE = 90
const val JPEG_QUALITY_VALUE: Byte = 50

/**
 * Testing ProcessingCaptureSession for two parts
 * Part1: Testing if SessionProcessorCaptureSession can be served as a normal CaptureSession to
 * configure camera and execute capture request correctly.
 * Part2: Testing if the methods in SessionProcessor are invoked as expected in the right timing.
 *
 * Since ProcessingCaptureSession is not thread-safe, all tests will run on the main thread.
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 23)
class ProcessingCaptureSessionTest(
    private var lensFacing: Int,
    // The pair specifies (Origin image format to Transformed format), SessionProcessor will
    // create the surface even if transformed format is the same as origin format but if the
    // transformed format is null, it means no conversion and original surface is used directly.
    private var previewFormatConvert: Pair<Int, Int?>,
    private var captureFormatConvert: Pair<Int, Int?>,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "Lens facing:{0} preview={1} capture={2}")
        fun data() = listOf(
            arrayOf(
                CameraSelector.LENS_FACING_BACK, (PRIVATE to null), (JPEG to null)
            ),
            arrayOf(
                CameraSelector.LENS_FACING_BACK, (YUV_420_888 to YUV_420_888), (JPEG to null)
            ),
            arrayOf(
                CameraSelector.LENS_FACING_BACK, (PRIVATE to null), (YUV_420_888 to YUV_420_888)
            ),
            arrayOf(
                CameraSelector.LENS_FACING_FRONT, (PRIVATE to null), (JPEG to null)
            ),
            arrayOf(
                CameraSelector.LENS_FACING_FRONT, (YUV_420_888 to YUV_420_888), (JPEG to null)
            ),
            arrayOf(
                CameraSelector.LENS_FACING_FRONT, (PRIVATE to null), (YUV_420_888 to YUV_420_888)
            )
        )
    }

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    private lateinit var cameraDeviceHolder: CameraDeviceHolder
    private lateinit var captureSessionRepository: CaptureSessionRepository
    private lateinit var captureSessionOpenerBuilder: SynchronizedCaptureSessionOpener.Builder
    private lateinit var sessionProcessor: CustomSessionProcessor
    private lateinit var executor: Executor
    private lateinit var handler: Handler
    private lateinit var sessionConfigParameters: SessionConfigParameters
    private lateinit var camera2CameraInfo: Camera2CameraInfoImpl

    @Before
    fun setUp() {
        // Only testing on API level >=23 since SessionProcessor requires ImageWriter support.
        assumeTrue(Build.VERSION.SDK_INT >= 23)

        val cameraManagerCompat = CameraManagerCompat.from(
            ApplicationProvider
                .getApplicationContext() as Context
        )

        handler = Handler(Looper.getMainLooper())
        executor = CameraXExecutors.newHandlerExecutor(handler)
        sessionProcessor = CustomSessionProcessor()

        val cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
        camera2CameraInfo = Camera2CameraInfoImpl(cameraId, cameraManagerCompat)
        captureSessionRepository = CaptureSessionRepository(executor)
        captureSessionOpenerBuilder = SynchronizedCaptureSessionOpener.Builder(
            executor,
            executor as ScheduledExecutorService,
            handler,
            captureSessionRepository,
            camera2CameraInfo.supportedHardwareLevel
        )

        cameraDeviceHolder = CameraUtil.getCameraDevice(
            cameraId,
            captureSessionRepository.getCameraStateCallback()
        )

        sessionConfigParameters = SessionConfigParameters()
    }

    @After
    fun tearDown() {
        if (::cameraDeviceHolder.isInitialized) {
            CameraUtil.releaseCameraDevice(cameraDeviceHolder)
        }

        if (::sessionConfigParameters.isInitialized) {
            sessionConfigParameters.tearDown()
        }
    }

    private fun createProcessingCaptureSession(): ProcessingCaptureSession {
        val cameraManagerCompat = CameraManagerCompat.from(
            ApplicationProvider
                .getApplicationContext() as Context
        )

        val cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
        val camera2Info = Camera2CameraInfoImpl(cameraId, cameraManagerCompat)

        return ProcessingCaptureSession(
            sessionProcessor, camera2Info, executor, executor as ScheduledExecutorService
        )
    }

    // Part1: Testing if SessionProcessorCaptureSession can be served as a normal CaptureSession to
    // configure camera and execute capture request correctly.
    @Test
    fun canOpen(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()

        // Act
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Assert
        sessionConfigParameters.assertSessionOnConfigured()
    }

    @Test
    fun canSetSessionConfigBeforeOpen(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()

        // Act
        captureSession.sessionConfig =
            sessionConfigParameters.getActiveSessionConfigForRepeating()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Assert
        sessionConfigParameters.assertRepeatingRequestCompleted()
        sessionConfigParameters.assertPreviewImageReceived()

        val parametersConfigSet = sessionProcessor.assertSetParametersInvoked()
        assertThat(
            areParametersConfigIdentical(
                parametersConfigSet,
                captureSession.sessionConfig!!.implementationOptions
            )
        ).isTrue()
    }

    @Test
    fun canSetSessionConfigAfterOpen(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()

        // Act
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)
        captureSession.sessionConfig =
            sessionConfigParameters.getActiveSessionConfigForRepeating()

        // Assert
        sessionConfigParameters.assertRepeatingRequestCompleted()
        sessionConfigParameters.assertPreviewImageReceived()

        val parametersConfigSet = sessionProcessor.assertSetParametersInvoked()
        assertThat(
            areParametersConfigIdentical(
                parametersConfigSet,
                captureSession.sessionConfig!!.implementationOptions
            )
        ).isTrue()
    }

    private fun areParametersConfigIdentical(config1: Config, config2: Config): Boolean {
        val options1 = CaptureRequestOptions.Builder.from(config1).build()
        val options2 = CaptureRequestOptions.Builder.from(config2).build()

        if (options1.listOptions().size != options2.listOptions().size) {
            return false
        }

        for (option in options1.listOptions()) {
            val value1 = options1.retrieveOption(option)
            val value2 = options2.retrieveOption(option)
            if (value1 != value2) {
                return false
            }
        }
        return true
    }

    @Test
    fun canIssueStillCapture(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        captureSession.sessionConfig =
            sessionConfigParameters.getActiveSessionConfigForRepeating()

        // Act
        captureSession.issueCaptureRequests(
            listOf(sessionConfigParameters.getStillCaptureCaptureConfig())
        )

        // Assert
        sessionConfigParameters.assertStillCaptureCompleted()
        sessionConfigParameters.assertCaptureImageReceived()
        val parametersConfig = sessionProcessor.getLatestParameters()
        assertThat(
            parametersConfig.isParameterSet(
                CaptureRequest.JPEG_ORIENTATION, JPEG_ORIENTATION_VALUE
            )
        ).isTrue()
        assertThat(
            parametersConfig.isParameterSet(CaptureRequest.JPEG_QUALITY, JPEG_QUALITY_VALUE)
        ).isTrue()
    }

    private fun <T> Config.isParameterSet(key: CaptureRequest.Key<T>, objValue: T): Boolean {
        val options = CaptureRequestOptions.Builder.from(this).build()
        return Objects.equals(
            options.getCaptureRequestOption(key),
            objValue
        )
    }

    @Test
    fun parametersIncludeSessionAndStillCapture(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()

        // Act
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Act
        captureSession.sessionConfig =
            sessionConfigParameters.getActiveSessionConfigForRepeating()
        captureSession.issueCaptureRequests(
            listOf(sessionConfigParameters.getStillCaptureCaptureConfig())
        )

        // Assert
        sessionConfigParameters.assertStillCaptureCompleted()
        val parametersConfig = sessionProcessor.getLatestParameters()
        assertThat(
            parametersConfig.isParameterSet(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
        ).isTrue()
        assertThat(
            parametersConfig.isParameterSet(
                CaptureRequest.JPEG_ORIENTATION,
                JPEG_ORIENTATION_VALUE
            )
        ).isTrue()
        assertThat(
            parametersConfig.isParameterSet(
                CaptureRequest.JPEG_QUALITY,
                JPEG_QUALITY_VALUE
            )
        ).isTrue()
    }

    @Test
    fun canIssueStillCaptureBeforeOpen(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.sessionConfig =
            sessionConfigParameters.getActiveSessionConfigForRepeating()

        // Act
        captureSession.issueCaptureRequests(
            listOf(sessionConfigParameters.getStillCaptureCaptureConfig())
        )
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Assert
        sessionConfigParameters.assertStillCaptureCompleted()
        sessionConfigParameters.assertCaptureImageReceived()
    }

    @Test
    fun willCancelRequests_whenIssueMultipleConfigs(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        val cancelCountLatch = CountDownLatch(2)
        val captureConfig1 = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            addCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCancelled() {
                    cancelCountLatch.countDown()
                }
            })
        }.build()
        val captureConfig2 = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            addCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCancelled() {
                    cancelCountLatch.countDown()
                }
            })
        }.build()

        // Act
        captureSession.issueCaptureRequests(listOf(captureConfig1, captureConfig2))

        // Assert
        assertThat(cancelCountLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun willCancelNonStillCaptureRequests(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)
        val cancelCountLatch = CountDownLatch(1)
        val captureConfig = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_PREVIEW
            addCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCancelled() {
                    cancelCountLatch.countDown()
                }
            })
        }.build()

        // Act
        captureSession.issueCaptureRequests(listOf(captureConfig))

        // Assert
        assertThat(cancelCountLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun willCancelRequests_whenPendingRequestNotFinished(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Act
        captureSession.issueCaptureRequests(
            listOf(sessionConfigParameters.getStillCaptureCaptureConfig())
        )

        val cancelCountLatch = CountDownLatch(1)
        val captureConfig = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            addCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCancelled() {
                    cancelCountLatch.countDown()
                }
            })
        }.build()
        // send 2nd request immediately.
        captureSession.issueCaptureRequests(listOf(captureConfig))

        // Assert
        assertThat(cancelCountLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun canExecuteStillCaptureOneByOne(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Act
        // first request
        captureSession.issueCaptureRequests(
            listOf(sessionConfigParameters.getStillCaptureCaptureConfig())
        )
        sessionConfigParameters.assertStillCaptureCompleted()

        // second request after first request completed
        val deferredRequestCompleted = CompletableDeferred<Unit>()
        val captureConfig = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            addCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCompleted(cameraCaptureResult: CameraCaptureResult) {
                    deferredRequestCompleted.complete(Unit)
                }
            })
        }.build()
        captureSession.issueCaptureRequests(listOf(captureConfig))

        // Assert
        deferredRequestCompleted.awaitWithTimeout(3000)
    }

    @Test
    fun canCancelCaptureRequests(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val captureSession = createProcessingCaptureSession()

        val deferredRequestCancelled = CompletableDeferred<Unit>()
        val captureConfig = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            addCameraCaptureCallback(object : CameraCaptureCallback() {
                override fun onCaptureCancelled() {
                    deferredRequestCancelled.complete(Unit)
                }
            })
        }.build()

        // Act
        captureSession.issueCaptureRequests(listOf(captureConfig))
        captureSession.cancelIssuedCaptureRequests()

        // Assert
        deferredRequestCancelled.awaitWithTimeout(3000)
    }

    @Test
    fun openWithClosedSurface() {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        sessionConfigParameters.closeOutputSurfaces()

        // Act
        val future = captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(),
            cameraDevice,
            captureSessionOpenerBuilder.build()
        )

        // Assert
        assertThrows(ExecutionException::class.java) {
            future.get()
        }.hasCauseThat().isInstanceOf(DeferrableSurface.SurfaceClosedException::class.java)
    }

    @Test
    fun canReleaseCaptureSession(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        // Act and Assert
        captureSession.close()
        captureSession.release(false).awaitWithTimeout(3000)
    }

    @Test
    fun outputSurfaceTerminatedProperly(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        val sessionConfigForOpen = sessionConfigParameters.getSessionConfigForOpen()
        captureSession.open(
            sessionConfigForOpen, cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)
        val surfaceTerminated = CountDownLatch(sessionConfigForOpen.surfaces.size)
        // Output surfaces should be held during the session regardless it is used in
        // CaptureSession directly or not.
        for (outputSurface in sessionConfigForOpen.surfaces) {
            assertThat(outputSurface.useCount).isAtLeast(1)
            outputSurface.terminationFuture.addListener(
                { surfaceTerminated.countDown() },
                CameraXExecutors.directExecutor()
            )
        }

        // Act
        captureSession.close()
        captureSession.release(false).await()
        CameraUtil.releaseCameraDevice(cameraDeviceHolder)
        sessionConfigParameters.closeOutputSurfaces()

        // Assert
        assertThat(surfaceTerminated.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun openCaptureSessionFailed_deInitWasInvokedAndSurfacesAreReleased(): Unit = runBlocking {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()

        // Release the surface when starting to open capture session (after initSession)
        // to make it failed.
        sessionProcessor.runAfterInitSession {
            sessionConfigParameters.releaseSurfaces()
            sessionProcessor.releaseSurfaces()
        }

        // Act
        val sessionConfigForOpen = sessionConfigParameters.getSessionConfigForOpen()
        captureSession.open(
            sessionConfigForOpen, cameraDevice,
            captureSessionOpenerBuilder.build()
        )

        // Assert
        sessionProcessor.assertInitSessionInvoked()
        sessionProcessor.assertDeInitSessionInvoked()

        sessionConfigParameters.closeOutputSurfaces()
        // DeferrableSurfaces are released after failed.
        val surfaceTerminated = CountDownLatch(sessionConfigForOpen.surfaces.size)
        for (outputSurface in sessionConfigForOpen.surfaces) {
            outputSurface.terminationFuture.addListener(
                { surfaceTerminated.countDown() },
                CameraXExecutors.directExecutor()
            )
        }
        assertThat(surfaceTerminated.await(3, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun canOpen2ndSession(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        )
        captureSession.close()
        captureSession.release(false)

        // Act
        val captureSession2 = createProcessingCaptureSession()
        val sessionConfigParameters2 = SessionConfigParameters()
        captureSession2.open(
            sessionConfigParameters2.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        )

        // Assert
        sessionConfigParameters2.assertSessionOnConfigured()

        sessionConfigParameters2.tearDown()
    }

    // ============================================
    // Part2: Testing if SessionProcessor methods were called in the right timing.
    @Test
    fun sessionProcessorMethodsCalled_fullLifecycle(): Unit = runBlocking(Dispatchers.Main) {
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        ).awaitWithTimeout(3000)

        val time1 = sessionProcessor.assertInitSessionInvoked()
        val time2 = sessionProcessor.assertOnCaptureSessionStartInvoked()

        captureSession.sessionConfig = sessionConfigParameters
            .getActiveSessionConfigForRepeating()
        val time3 = sessionProcessor.assertStartRepeatingInvoked()

        captureSession.issueCaptureRequests(
            listOf(sessionConfigParameters.getStillCaptureCaptureConfig())
        )
        val time4 = sessionProcessor.assertStartCaptureInvoked()

        captureSession.close()
        val time5 = sessionProcessor.assertOnCaptureEndInvoked()
        val time6 = sessionProcessor.assertDeInitSessionInvoked()

        assertThat(time6).isAtLeast(time5)
        assertThat(time5).isAtLeast(time4)
        assertThat(time4).isAtLeast(time3)
        assertThat(time3).isAtLeast(time2)
        assertThat(time2).isAtLeast(time1)
    }

    @Test
    fun sessionProcessorMethodsCalled_closeOpeningSession(): Unit = runBlocking(Dispatchers.Main) {
        // Arrange
        val cameraDevice = cameraDeviceHolder.get()!!
        val captureSession = createProcessingCaptureSession()

        val initSessionIsCalled = CompletableDeferred<Unit>()
        // Ensures when captureSession.close() is called, it is still opening.
        sessionProcessor.runAfterInitSession {
            initSessionIsCalled.complete(Unit)
        }
        captureSession.open(
            sessionConfigParameters.getSessionConfigForOpen(), cameraDevice,
            captureSessionOpenerBuilder.build()
        )

        // Act
        initSessionIsCalled.awaitWithTimeout(3000) // wait until initSession is called.
        captureSession.close()

        // Assert
        sessionProcessor.assertInitSessionInvoked()
        sessionProcessor.assertDeInitSessionInvoked()

        if (sessionProcessor.wasOnCaptureSessionStartInvoked()) {
            sessionProcessor.assertOnCaptureEndInvoked()
        }
    }

    private suspend fun <T> ListenableFuture<T>.awaitWithTimeout(timeMillis: Long): T {
        return withTimeout(timeMillis) {
            await()
        }
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(timeMillis: Long): T {
        return withTimeout(timeMillis) {
            await()
        }
    }

    private inner class SessionConfigParameters {
        private var previewOutputDeferrableSurface: DeferrableSurface
        private var captureOutputDeferrableSurface: DeferrableSurface
        // Use SurfaceTexture for preview if PRIVATE format, use ImageReader if YUV format.
        private var previewSurfaceTexture: SurfaceTexture? = null
        private var previewImageReader: ImageReader? = null
        private var captureImageReader: ImageReader
        private val sessionConfigured = CompletableDeferred<Unit>()
        private val repeatingRequestCompleted = CompletableDeferred<Unit>()
        private val previewImageReady = CompletableDeferred<Unit>()
        private val captureImageReady = CompletableDeferred<Unit>()
        private val stillCaptureCompleted = CompletableDeferred<Unit>()

        init {
            // Preview
            lateinit var previewSurface: Surface
            var previewFormat = previewFormatConvert.first
            if (previewFormat == PRIVATE) {
                previewSurfaceTexture = SurfaceTexture(0)
                previewSurfaceTexture!!.setOnFrameAvailableListener {
                    previewImageReady.complete(Unit)
                }
                previewSurface = Surface(previewSurfaceTexture!!)
            } else {
                previewImageReader = ImageReader.newInstance(640, 480, previewFormat, 2)
                previewImageReader!!.setOnImageAvailableListener(
                    {
                        it.acquireNextImage().use {
                            previewImageReady.complete(Unit)
                        }
                    },
                    handler
                )
                previewSurface = previewImageReader!!.surface
            }
            previewOutputDeferrableSurface = ImmediateSurface(
                previewSurface, Size(640, 480), previewFormat
            )
            previewOutputDeferrableSurface.terminationFuture.addListener(
                {
                    previewSurfaceTexture?.release()
                    previewImageReader?.close()
                },
                CameraXExecutors.directExecutor()
            )
            previewOutputDeferrableSurface.setContainerClass(Preview::class.java)

            // ImageCapture
            val captureFormat = captureFormatConvert.first
            captureImageReader = ImageReader.newInstance(
                640, 480, captureFormat, 2
            )
            captureImageReader.setOnImageAvailableListener(
                {
                    it.acquireNextImage().use {
                        captureImageReady.complete(Unit)
                    }
                },
                handler
            )
            captureOutputDeferrableSurface = ImmediateSurface(
                captureImageReader.surface, Size(640, 480),
                JPEG
            )
            captureOutputDeferrableSurface.setContainerClass(ImageCapture::class.java)
            captureOutputDeferrableSurface.terminationFuture.addListener(
                { captureImageReader.close() },
                CameraXExecutors.directExecutor()
            )
        }

        fun getSessionConfigForOpen(): SessionConfig {
            val sessionBuilder = SessionConfig.Builder()
            sessionBuilder.addSurface(captureOutputDeferrableSurface)
            sessionBuilder.addSurface(previewOutputDeferrableSurface)
            sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
            sessionBuilder.addSessionStateCallback(
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        sessionConfigured.complete(Unit)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }
                }
            )
            return sessionBuilder.build()
        }

        fun getActiveSessionConfigForRepeating(): SessionConfig {
            return SessionConfig.Builder().apply {
                setImplementationOptions(
                    CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_OFF
                        ).build()
                )
                addRepeatingCameraCaptureCallback(object : CameraCaptureCallback() {
                    override fun onCaptureCompleted(cameraCaptureResult: CameraCaptureResult) {
                        repeatingRequestCompleted.complete(Unit)
                    }
                })
            }.build()
        }

        fun getStillCaptureCaptureConfig(): CaptureConfig {
            return CaptureConfig.Builder().apply {
                templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                implementationOptions = CaptureRequestOptions.Builder().apply {
                    setCaptureRequestOption(CaptureRequest.JPEG_ORIENTATION, JPEG_ORIENTATION_VALUE)
                    setCaptureRequestOption(CaptureRequest.JPEG_QUALITY, JPEG_QUALITY_VALUE)
                }.build()
                addCameraCaptureCallback(object : CameraCaptureCallback() {
                    override fun onCaptureCompleted(cameraCaptureResult: CameraCaptureResult) {
                        stillCaptureCompleted.complete(Unit)
                    }
                })
            }.build()
        }

        fun closeOutputSurfaces() {
            previewOutputDeferrableSurface.close()
            captureOutputDeferrableSurface.close()
        }

        fun releaseSurfaces() {
            captureImageReader.close()
            previewImageReader?.close()
            previewSurfaceTexture?.release()
        }

        suspend fun assertSessionOnConfigured() {
            sessionConfigured.awaitWithTimeout(3000)
        }

        suspend fun assertRepeatingRequestCompleted() {
            repeatingRequestCompleted.awaitWithTimeout(3000)
        }

        suspend fun assertPreviewImageReceived() {
            previewImageReady.awaitWithTimeout(3000)
        }

        suspend fun assertStillCaptureCompleted() {
            stillCaptureCompleted.awaitWithTimeout(3000)
        }

        suspend fun assertCaptureImageReceived() {
            captureImageReady.awaitWithTimeout(3000)
        }

        fun tearDown() {
            closeOutputSurfaces()
        }
    }

    inner class CustomSessionProcessor : SessionProcessor {
        private lateinit var previewProcessorSurface: DeferrableSurface
        private lateinit var captureProcessorSurface: DeferrableSurface
        private var intermediaPreviewImageReader: ImageReader? = null
        private var intermediaCaptureImageReader: ImageReader? = null
        private var intermediaPreviewImageWriter: ImageWriter? = null
        private var intermediaCaptureImageWriter: ImageWriter? = null

        private val previewOutputConfigId = 1
        private val captureOutputConfigId = 2

        private var requestProcessor: RequestProcessor? = null

        // Values of these Deferred are the timestamp to complete.
        private val initSessionCalled = CompletableDeferred<Long>()
        private val deInitSessionCalled = CompletableDeferred<Long>()
        private val onCaptureSessionStartCalled = CompletableDeferred<Long>()
        private val onCaptureSessionEndCalled = CompletableDeferred<Long>()
        private val startRepeatingCalled = CompletableDeferred<Long>()
        private val startCaptureCalled = CompletableDeferred<Long>()
        private val setParametersCalled = CompletableDeferred<Config>()
        private var latestParameters: Config = OptionsBundle.emptyBundle()
        private var blockRunAfterInitSession: () -> Unit = {}

        fun releaseSurfaces() {
            intermediaPreviewImageReader?.close()
            intermediaCaptureImageReader?.close()
        }

        fun runAfterInitSession(block: () -> Unit) {
            blockRunAfterInitSession = block
        }

        override fun initSession(
            cameraInfo: CameraInfo,
            previewSurfaceConfig: OutputSurface,
            imageCaptureSurfaceConfig: OutputSurface,
            imageAnalysisSurfaceConfig: OutputSurface?
        ): SessionConfig {
            initSessionCalled.complete(SystemClock.elapsedRealtimeNanos())
            val handler = Handler(Looper.getMainLooper())

            var sessionBuilder = SessionConfig.Builder()

            // Preview
            lateinit var previewTransformedSurface: Surface
            if (previewFormatConvert.second == null) { // no conversion, use origin surface.
                previewTransformedSurface = previewSurfaceConfig.surface
            } else {
                intermediaPreviewImageReader = ImageReader.newInstance(
                    640, 480,
                    previewFormatConvert.second!!, 2
                )
                previewTransformedSurface = intermediaPreviewImageReader!!.surface

                intermediaPreviewImageWriter = ImageWriter.newInstance(
                    previewSurfaceConfig.surface, 2
                )

                intermediaPreviewImageReader!!.setOnImageAvailableListener(
                    {
                        it.acquireNextImage().use {
                            val imageDequeued = intermediaPreviewImageWriter!!.dequeueInputImage()
                            intermediaPreviewImageWriter!!.queueInputImage(imageDequeued)
                        }
                    },
                    handler
                )
            }
            previewProcessorSurface =
                SessionProcessorSurface(previewTransformedSurface, previewOutputConfigId)
            previewProcessorSurface.terminationFuture.addListener(
                {
                    intermediaPreviewImageReader?.close()
                    intermediaPreviewImageWriter?.close()
                },
                CameraXExecutors.directExecutor()
            )
            sessionBuilder.addSurface(previewProcessorSurface)

            // Capture
            lateinit var captureTransformedSurface: Surface
            if (captureFormatConvert.second == null) { // no conversion, use origin surface.
                captureTransformedSurface = imageCaptureSurfaceConfig.surface
            } else {
                intermediaCaptureImageReader = ImageReader.newInstance(
                    640, 480,
                    captureFormatConvert.second!!, 2
                )
                captureTransformedSurface = intermediaCaptureImageReader!!.surface

                intermediaCaptureImageWriter = ImageWriter.newInstance(
                    imageCaptureSurfaceConfig.surface, 2
                )

                intermediaCaptureImageReader!!.setOnImageAvailableListener(
                    {
                        it.acquireNextImage().use {
                            val imageDequeued = intermediaCaptureImageWriter!!.dequeueInputImage()
                            intermediaCaptureImageWriter!!.queueInputImage(imageDequeued)
                        }
                    },
                    handler
                )
            }
            captureProcessorSurface =
                SessionProcessorSurface(captureTransformedSurface, captureOutputConfigId)

            captureProcessorSurface.terminationFuture.addListener(
                {
                    intermediaCaptureImageReader?.close()
                    intermediaCaptureImageWriter?.close()
                },
                CameraXExecutors.directExecutor()
            )
            sessionBuilder.addSurface(captureProcessorSurface)

            sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
            val sessionConfig = sessionBuilder.build()
            blockRunAfterInitSession()
            return sessionConfig
        }

        override fun deInitSession() {
            deInitSessionCalled.complete(SystemClock.elapsedRealtimeNanos())
            previewProcessorSurface.close()
            captureProcessorSurface.close()
        }

        override fun setParameters(config: Config) {
            setParametersCalled.complete(config)
            latestParameters = config
        }

        override fun onCaptureSessionStart(_requestProcessor: RequestProcessor) {
            onCaptureSessionStartCalled.complete(SystemClock.elapsedRealtimeNanos())
            requestProcessor = _requestProcessor
        }

        override fun onCaptureSessionEnd() {
            onCaptureSessionEndCalled.complete(SystemClock.elapsedRealtimeNanos())
        }

        fun getLatestParameters(): Config {
            return latestParameters
        }

        override fun startRepeating(callback: SessionProcessor.CaptureCallback): Int {
            startRepeatingCalled.complete(SystemClock.elapsedRealtimeNanos())
            val builder = RequestProcessorRequest.Builder().apply {
                addTargetOutputConfigId(previewOutputConfigId)
                setParameters(latestParameters)
                setTemplateId(CameraDevice.TEMPLATE_PREVIEW)
            }

            requestProcessor!!.setRepeating(
                builder.build(),
                object : RequestProcessor.Callback {
                    override fun onCaptureStarted(
                        request: RequestProcessor.Request,
                        frameNumber: Long,
                        timestamp: Long
                    ) {}

                    override fun onCaptureProgressed(
                        request: RequestProcessor.Request,
                        captureResult: CameraCaptureResult
                    ) {}

                    override fun onCaptureCompleted(
                        request: RequestProcessor.Request,
                        captureResult: CameraCaptureResult
                    ) {
                        callback.onCaptureSequenceCompleted(1)
                    }

                    override fun onCaptureFailed(
                        request: RequestProcessor.Request,
                        captureFailure: CameraCaptureFailure
                    ) {}

                    override fun onCaptureBufferLost(
                        request: RequestProcessor.Request,
                        frameNumber: Long,
                        outputConfigId: Int
                    ) {}

                    override fun onCaptureSequenceCompleted(
                        sequenceId: Int,
                        frameNumber: Long
                    ) {}

                    override fun onCaptureSequenceAborted(sequenceId: Int) {
                    }
                }
            )
            return FAKE_CAPTURE_SEQUENCE_ID
        }

        override fun stopRepeating() {
        }

        override fun startCapture(callback: SessionProcessor.CaptureCallback): Int {
            startCaptureCalled.complete(SystemClock.elapsedRealtimeNanos())
            val request = RequestProcessorRequest.Builder().apply {
                addTargetOutputConfigId(captureOutputConfigId)
                setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
            }.build()

            requestProcessor!!.submit(
                request,
                object : RequestProcessor.Callback {
                    override fun onCaptureCompleted(
                        request: RequestProcessor.Request,
                        captureResult: CameraCaptureResult
                    ) {
                        callback.onCaptureSequenceCompleted(1)
                    }

                    override fun onCaptureStarted(
                        request: RequestProcessor.Request,
                        frameNumber: Long,
                        timestamp: Long
                    ) {}

                    override fun onCaptureProgressed(
                        request: RequestProcessor.Request,
                        captureResult: CameraCaptureResult
                    ) {}

                    override fun onCaptureFailed(
                        request: RequestProcessor.Request,
                        captureFailure: CameraCaptureFailure
                    ) {
                        callback.onCaptureFailed(1)
                    }

                    override fun onCaptureBufferLost(
                        request: RequestProcessor.Request,
                        frameNumber: Long,
                        outputConfigId: Int
                    ) {}

                    override fun onCaptureSequenceCompleted(sequenceId: Int, frameNumber: Long) {}

                    override fun onCaptureSequenceAborted(sequenceId: Int) {}
                }
            )
            return FAKE_CAPTURE_SEQUENCE_ID
        }

        override fun abortCapture(captureSequenceId: Int) {
        }

        suspend fun assertInitSessionInvoked(): Long {
            return initSessionCalled.awaitWithTimeout(3000)
        }

        suspend fun wasInitSessionInvoked(): Boolean {
            val result = withTimeoutOrNull(3000) { initSessionCalled.await() }
            return result != null
        }

        suspend fun assertDeInitSessionInvoked(): Long {
            return deInitSessionCalled.awaitWithTimeout(3000)
        }

        suspend fun assertOnCaptureSessionStartInvoked(): Long {
            return onCaptureSessionStartCalled.awaitWithTimeout(3000)
        }

        suspend fun wasOnCaptureSessionStartInvoked(): Boolean {
            val result = withTimeoutOrNull(3000) { onCaptureSessionStartCalled.await() }
            return result != null
        }

        suspend fun assertOnCaptureEndInvoked(): Long {
            return onCaptureSessionEndCalled.awaitWithTimeout(3000)
        }

        suspend fun assertStartRepeatingInvoked(): Long {
            return startRepeatingCalled.awaitWithTimeout(3000)
        }

        suspend fun assertStartCaptureInvoked(): Long {
            return startCaptureCalled.awaitWithTimeout(3000)
        }

        suspend fun assertSetParametersInvoked(): Config {
            return setParametersCalled.awaitWithTimeout(3000)
        }
    }
}