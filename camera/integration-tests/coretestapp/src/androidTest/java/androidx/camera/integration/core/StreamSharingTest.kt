/*
 * Copyright 2024 The Android Open Source Project
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
import android.graphics.SurfaceTexture
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.lifecycle.LifecycleCamera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.SurfaceTextureProvider.SurfaceTextureCallback
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class StreamSharingTest(private val implName: String, private val cameraConfig: CameraXConfig) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L
        private const val TAG = "StreamSharingTest"
        private val DEFAULT_CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private var surfaceFutureSemaphore: Semaphore = Semaphore(0)

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(DEFAULT_CAMERA_SELECTOR.lensFacing!!))
        skipVideoRecordingTestIfNotSupportedByEmulator()
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun previewAndRecordingCanProceedAfterSessionError() {
        val recorder = Recorder.Builder().build()
        val videoCapture = VideoCapture.withOutput(recorder)
        val preview = Preview.Builder().build()
        val imageAnalysis = ImageAnalysis.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        val useCases = arrayOf(videoCapture, preview, imageAnalysis, imageCapture)

        instrumentation.runOnMainSync {
            // Sets surface provider to preview
            preview.surfaceProvider = getSurfaceProvider()
        }

        // Checks whether the back camera can support four UseCases combination
        val cameraUseCaseAdapter =
            checkStreamSharingSupportAndRetrieveCameraUseCaseAdapter(
                DEFAULT_CAMERA_SELECTOR,
                useCases
            )
        assumeTrue(cameraUseCaseAdapter != null)

        // Checks whether stream sharing is actually enabled
        var streamSharing = retrieveStreamSharing(cameraUseCaseAdapter!!.cameraUseCases)
        assumeTrue(streamSharing != null)

        // Verifies preview and recording before triggering onError event
        verifyPreviewImageReceivedAndRecording(videoCapture)

        // Retrieves the initial session config
        var sessionConfig = streamSharing!!.sessionConfig

        // Verifies preview and recording after triggering onError event
        triggerErrorAndVerifyPreviewImageReceivedAndRecording(sessionConfig, videoCapture)

        val frontCameraUseCaseAdapter =
            if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                checkStreamSharingSupportAndRetrieveCameraUseCaseAdapter(
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    useCases
                )
            } else {
                null
            }

        // Rebinds to different camera
        frontCameraUseCaseAdapter?.let {
            // Checks whether stream sharing is actually enabled
            streamSharing = retrieveStreamSharing(it.cameraUseCases)
            assumeTrue(streamSharing != null)

            // Verifies preview and recording after triggering onError event to the closed error
            // listener
            triggerErrorAndVerifyPreviewImageReceivedAndRecording(sessionConfig, videoCapture)
        }

        // Update the session config
        sessionConfig = streamSharing!!.sessionConfig

        // Verifies preview and recording after triggering onError event to the new active error
        // listener
        triggerErrorAndVerifyPreviewImageReceivedAndRecording(sessionConfig, videoCapture)
    }

    private fun checkStreamSharingSupportAndRetrieveCameraUseCaseAdapter(
        cameraSelector: CameraSelector,
        useCases: Array<UseCase>
    ): CameraUseCaseAdapter? {
        lateinit var lifecycleOwner: FakeLifecycleOwner
        lateinit var camera: Camera

        instrumentation.runOnMainSync {
            lifecycleOwner = FakeLifecycleOwner().apply { startAndResume() }
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
        }

        // Checks whether the camera can support the UseCases combination only when stream sharing
        // is enabled
        if (
            camera.isUseCasesCombinationSupported(false, *useCases) ||
                !camera.isUseCasesCombinationSupported(true, *useCases)
        ) {
            return null
        }

        instrumentation.runOnMainSync {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases)
        }

        return (camera as LifecycleCamera).cameraUseCaseAdapter
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // Note that the onSurfaceTextureReady will only be received once since
                    // surfaceTexture.updateTexImage() isn't invoked here. Therefore,
                    // surfaceFutureSemaphore will also only be released once. This is to simplify
                    // the preview related test. Otherwise, the test need to make sure that the
                    // previous preview stream has been stopped and then renew the semaphore to
                    // monitor the new preview stream is started successfully.
                    surfaceTexture.setOnFrameAvailableListener { surfaceFutureSemaphore.release() }
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                }
            }
        )
    }

    private fun retrieveStreamSharing(useCases: Iterable<UseCase>): StreamSharing? =
        useCases.filterIsInstance<StreamSharing>().firstOrNull()

    private fun verifyPreviewImageReceivedAndRecording(videoCapture: VideoCapture<Recorder>) {
        // Verify. preview image can be received
        verifyPreviewStreaming()
        // Verify. recording can work successfully
        startAndVerifyVideoRecording(videoCapture)
    }

    private fun triggerErrorAndVerifyPreviewImageReceivedAndRecording(
        sessionConfig: SessionConfig,
        videoCapture: VideoCapture<Recorder>
    ) {
        // This should be reset before onError is invoked to make sure that the semaphore can be
        // successfully released by the onSurfaceTextureReady + onFrameAvailable events
        surfaceFutureSemaphore = Semaphore(0)

        instrumentation.runOnMainSync {
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN
            )
        }

        // Verify. preview image can be received
        verifyPreviewStreaming()
        // Verify. recording can work successfully
        startAndVerifyVideoRecording(videoCapture)
    }

    private fun verifyPreviewStreaming() {
        assertThat(surfaceFutureSemaphore.tryAcquire(10, TimeUnit.SECONDS)).isTrue()
    }

    private fun startAndVerifyVideoRecording(videoCapture: VideoCapture<Recorder>) {
        val latchForVideoSaved = CountDownLatch(1)
        val latchForVideoRecording = CountDownLatch(5)
        val videoRecordEventListener =
            Consumer<VideoRecordEvent> {
                when (it) {
                    is VideoRecordEvent.Finalize -> {
                        latchForVideoSaved.countDown()
                    }
                    is VideoRecordEvent.Status -> {
                        latchForVideoRecording.countDown()
                    }
                }
            }
        val fileOutputOptions = FileOutputOptions.Builder(temporaryFolder.newFile()).build()
        val recording =
            videoCapture.output
                .prepareRecording(context, fileOutputOptions)
                .start(mainThreadExecutor(), videoRecordEventListener)

        try {
            // Wait for status event to proceed recording for a while.
            assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        } catch (ex: Exception) {
            fail("Recording has started, but timed out for Status update.")
        } finally {
            recording.stop()
        }

        try {
            // Wait for finalize event to saved file.
            assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        } catch (ex: Exception) {
            fail("Recording has stopped, but timed out for Finalize event.")
        }
    }
}
