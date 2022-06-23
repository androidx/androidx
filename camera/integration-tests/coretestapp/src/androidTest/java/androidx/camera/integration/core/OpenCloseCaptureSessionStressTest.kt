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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.CameraEventCallback
import androidx.camera.camera2.impl.CameraEventCallbacks
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.createCameraSelectorById
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class OpenCloseCaptureSessionStressTest(
    private val cameraId: String
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraIdCameraSelector: CameraSelector
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private val cameraEventMonitor = CameraEventMonitor()

    @Before
    fun setUp(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        cameraIdCameraSelector = createCameraSelectorById(cameraId)

        camera = withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraIdCameraSelector)
        }

        // Creates the Preview with the CameraEventMonitor to monitor whether the event callbacks
        // are called.
        preview = createPreviewWithCameraEventMonitor(cameraEventMonitor)
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
        }
        imageCapture = ImageCapture.Builder().build()
    }

    @After
    fun cleanUp(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(preview, imageCapture)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewImageCaptureImageAnalysis(): Unit =
        runBlocking {
            val imageAnalysis = ImageAnalysis.Builder().build()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
            bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
                preview,
                imageCapture,
                imageAnalysis = imageAnalysis
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewVideoCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
                preview,
                videoCapture = videoCapture
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewVideoCaptureImageCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageCapture))
            bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
                preview,
                videoCapture = videoCapture,
                imageCapture = imageCapture
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCaptureSessionStressTest_withPreviewVideoCaptureImageAnalysis(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            val imageAnalysis = ImageAnalysis.Builder().build()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
            bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
                preview,
                videoCapture = videoCapture,
                imageAnalysis = imageAnalysis
            )
        }

    /**
     * Repeatedly binds use cases, unbind all to check whether the capture session can be opened
     * and closed successfully by monitoring the CameraEvent callbacks.
     *
     * <p>This function checks the nullabilities of the input ImageCapture, VideoCapture and
     * ImageAnalysis to determine whether the use cases will be bound together to run the test.
     */
    private fun bindUseCase_unbindAll_toCheckCameraEvent_repeatedly(
        preview: Preview,
        imageCapture: ImageCapture? = null,
        videoCapture: VideoCapture<Recorder>? = null,
        imageAnalysis: ImageAnalysis? = null,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            // Arrange: resets the camera event monitor
            cameraEventMonitor.reset()

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
                    *listOfNotNull(
                        preview,
                        imageCapture,
                        newVideoCapture,
                        imageAnalysis
                    ).toTypedArray()
                )
            }

            // Assert: checks the CameraEvent#onEnableSession callback function is called
            cameraEventMonitor.awaitSessionEnabledAndAssert()

            // Act: unbinds all use cases
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }

            // Assert: checks the CameraEvent#onSessionDisabled callback function is called
            cameraEventMonitor.awaitSessionDisabledAndAssert()
        }
    }

    companion object {
        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}")
        val parameters: Collection<String>
            get() = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
    }

    private fun createPreviewWithCameraEventMonitor(
        cameraEventMonitor: CameraEventMonitor
    ): Preview {
        val builder = Preview.Builder()

        Camera2ImplConfig.Extender(builder)
            .setCameraEventCallback(CameraEventCallbacks(cameraEventMonitor))

        return builder.build()
    }

    /**
     * An implementation of CameraEventCallback to monitor whether the camera event callbacks are
     * called properly or not.
     */
    private class CameraEventMonitor : CameraEventCallback() {
        private var sessionEnabledLatch = CountDownLatch(1)
        private var sessionDisabledLatch = CountDownLatch(1)

        override fun onEnableSession(): CaptureConfig? {
            sessionEnabledLatch.countDown()
            return null
        }

        override fun onDisableSession(): CaptureConfig? {
            sessionDisabledLatch.countDown()
            return null
        }

        fun reset() {
            sessionEnabledLatch = CountDownLatch(1)
            sessionDisabledLatch = CountDownLatch(1)
        }

        fun awaitSessionEnabledAndAssert() {
            assertThat(sessionEnabledLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun awaitSessionDisabledAndAssert() {
            assertThat(sessionDisabledLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}
