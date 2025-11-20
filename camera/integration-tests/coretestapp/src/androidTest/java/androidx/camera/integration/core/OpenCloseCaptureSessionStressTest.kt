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

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.StateCallback
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.integration.core.util.StressTestUtil
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.createCameraSelectorById
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.StressTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.RepeatRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class OpenCloseCaptureSessionStressTest(
    val implName: String,
    val cameraConfig: CameraXConfig,
    val cameraId: String,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule val labTest: LabTestRule = LabTestRule()

    @get:Rule val repeatRule = RepeatRule()

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraIdCameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private val sessionStateMonitor = CameraCaptureSessionStateMonitor()

    @Before
    fun setUp(): Unit = runBlocking {
        // Configures the test target config
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        cameraIdCameraSelector = createCameraSelectorById(cameraId)

        camera =
            withContext(Dispatchers.Main) {
                lifecycleOwner = FakeLifecycleOwner()
                lifecycleOwner.startAndResume()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraIdCameraSelector)
            }

        // Creates the Preview with the CameraCaptureSessionStateMonitor to monitor whether the
        // session callbacks are called.
        preview = createPreviewWithSessionStateMonitor(implName, sessionStateMonitor)

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
        }
        imageCapture = ImageCapture.Builder().build()
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCase_unbindAll_toCheckCameraSession_repeatedly(preview, imageCapture)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewImageCaptureImageAnalysis(): Unit =
        runBlocking {
            val imageAnalysis = ImageAnalysis.Builder().build()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
            bindUseCase_unbindAll_toCheckCameraSession_repeatedly(
                preview,
                imageCapture,
                imageAnalysis = imageAnalysis,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewVideoCapture(): Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        bindUseCase_unbindAll_toCheckCameraSession_repeatedly(preview, videoCapture = videoCapture)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewVideoCaptureImageCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageCapture))
            bindUseCase_unbindAll_toCheckCameraSession_repeatedly(
                preview,
                videoCapture = videoCapture,
                imageCapture = imageCapture,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewVideoCaptureImageAnalysis(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            val imageAnalysis = ImageAnalysis.Builder().build()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageCapture))
            bindUseCase_unbindAll_toCheckCameraSession_repeatedly(
                preview,
                videoCapture = videoCapture,
                imageAnalysis = imageAnalysis,
            )
        }

    /**
     * Repeatedly binds use cases, unbind all to check whether the capture session can be opened and
     * closed successfully by monitoring the camera session callbacks.
     *
     * <p>This function checks the nullabilities of the input ImageCapture, VideoCapture and
     * ImageAnalysis to determine whether the use cases will be bound together to run the test.
     */
    private fun bindUseCase_unbindAll_toCheckCameraSession_repeatedly(
        preview: Preview,
        imageCapture: ImageCapture? = null,
        videoCapture: VideoCapture<Recorder>? = null,
        imageAnalysis: ImageAnalysis? = null,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT,
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            // Arrange: resets the camera monitor
            sessionStateMonitor.reset()

            withContext(Dispatchers.Main) {
                // VideoCapture needs to be recreated everytime until b/212654991 is fixed
                var newVideoCapture: VideoCapture<Recorder>? = null
                videoCapture?.let {
                    newVideoCapture = VideoCapture.withOutput(Recorder.Builder().build())
                }

                // Act: binds use cases
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraIdCameraSelector,
                    *listOfNotNull(preview, imageCapture, newVideoCapture, imageAnalysis)
                        .toTypedArray(),
                )
            }

            // Assert: checks the capture session opened callback function is called
            sessionStateMonitor.awaitSessionConfiguredAndAssert()

            // Act: unbinds all use cases
            withContext(Dispatchers.Main) { cameraProvider.unbindAll() }
        }
    }

    companion object {
        @ClassRule @JvmField val stressTest = StressTestRule()

        @JvmStatic
        @Parameterized.Parameters(name = "config = {0}, cameraId = {2}")
        fun data() = StressTestUtil.getAllCameraXConfigCameraIdCombinations()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun createPreviewWithSessionStateMonitor(
        implementationName: String,
        sessionStateMonitor: CameraCaptureSessionStateMonitor,
    ): Preview {
        val builder = Preview.Builder()

        when (implementationName) {
            CameraPipeConfig::class.simpleName -> {
                androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender(builder)
                    .setSessionStateCallback(sessionStateMonitor)
            }
            else -> Camera2Interop.Extender(builder).setSessionStateCallback(sessionStateMonitor)
        }

        return builder.build()
    }

    /**
     * An implementation of CameraCaptureSession.StateCallback to monitor whether the session
     * callbacks are called properly or not.
     */
    private class CameraCaptureSessionStateMonitor : StateCallback() {
        private var sessionConfiguredLatch = CountDownLatch(1)

        override fun onConfigured(session: CameraCaptureSession) {
            sessionConfiguredLatch.countDown()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            throw RuntimeException("Capture session configures failed!")
        }

        fun reset() {
            sessionConfiguredLatch = CountDownLatch(1)
        }

        fun awaitSessionConfiguredAndAssert() {
            assertThat(sessionConfiguredLatch.await(15000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}
