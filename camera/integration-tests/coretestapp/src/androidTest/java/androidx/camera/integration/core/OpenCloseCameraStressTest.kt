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
import android.hardware.camera2.CameraDevice
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
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
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
class OpenCloseCameraStressTest(
    val implName: String,
    val cameraConfig: CameraXConfig,
    val cameraId: String,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(PreTestCameraIdList(cameraConfig))

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
    private val cameraDeviceStateMonitor = CameraDeviceStateMonitor()

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

        preview = createPreviewWithDeviceStateMonitor(implName, cameraDeviceStateMonitor)
        withContext(Dispatchers.Main) {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
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

    companion object {
        @ClassRule @JvmField val stressTest = StressTestRule()

        @JvmStatic
        @Parameterized.Parameters(name = "config = {0}, cameraId = {2}")
        fun data() = StressTestUtil.getAllCameraXConfigCameraIdCombinations()
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(
            preview,
            imageCapture,
            cameraDeviceStateMonitor = cameraDeviceStateMonitor,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewImageCaptureImageAnalysis(): Unit = runBlocking {
        val imageAnalysis = ImageAnalysis.Builder().build()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(
            preview,
            imageCapture,
            imageAnalysis = imageAnalysis,
            cameraDeviceStateMonitor = cameraDeviceStateMonitor,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewVideoCapture(): Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(
            preview,
            videoCapture = videoCapture,
            cameraDeviceStateMonitor = cameraDeviceStateMonitor,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewVideoCaptureImageCapture(): Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageCapture))
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(
            preview,
            videoCapture = videoCapture,
            imageCapture = imageCapture,
            cameraDeviceStateMonitor = cameraDeviceStateMonitor,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewVideoCaptureImageAnalysis(): Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageAnalysis = ImageAnalysis.Builder().build()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(
            preview,
            videoCapture = videoCapture,
            imageAnalysis = imageAnalysis,
            cameraDeviceStateMonitor = cameraDeviceStateMonitor,
        )
    }

    /**
     * Repeatedly binds use cases, unbind all to check whether the camera can be opened and closed
     * successfully by monitoring the camera state events.
     *
     * <p>This function checks the nullabilities of the input ImageCapture, VideoCapture and
     * ImageAnalysis to determine whether the use cases will be bound together to run the test.
     */
    private fun bindUseCase_unbindAll_toCheckCameraState_repeatedly(
        preview: Preview,
        imageCapture: ImageCapture? = null,
        videoCapture: VideoCapture<Recorder>? = null,
        imageAnalysis: ImageAnalysis? = null,
        cameraDeviceStateMonitor: CameraDeviceStateMonitor,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT,
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            cameraDeviceStateMonitor.reset()

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

            // Assert: checks the CameraDevice opened event can be received
            cameraDeviceStateMonitor.awaitCameraOpenedAndAssert()

            // Act: unbinds all use cases
            withContext(Dispatchers.Main) { cameraProvider.unbindAll() }

            // Assert: checks the CameraDevice closed event can be received
            cameraDeviceStateMonitor.awaitCameraClosedAndAssert()
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun createPreviewWithDeviceStateMonitor(
        implementationName: String,
        cameraDeviceStateMonitor: CameraDeviceStateMonitor,
    ): Preview {
        val builder = Preview.Builder()

        when (implementationName) {
            CameraPipeConfig::class.simpleName -> {
                androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender(builder)
                    .setDeviceStateCallback(cameraDeviceStateMonitor)
            }
            else ->
                Camera2Interop.Extender(builder).setDeviceStateCallback(cameraDeviceStateMonitor)
        }

        return builder.build()
    }

    private class CameraDeviceStateMonitor : CameraDevice.StateCallback() {
        private var openCameraLatch = CountDownLatch(1)
        private var closeCameraLatch = CountDownLatch(1)

        override fun onOpened(p0: CameraDevice) {
            openCameraLatch.countDown()
        }

        override fun onClosed(camera: CameraDevice) {
            closeCameraLatch.countDown()
        }

        override fun onDisconnected(p0: CameraDevice) {
            // No op.
        }

        override fun onError(p0: CameraDevice, p1: Int) {
            // No op.
        }

        fun reset() {
            openCameraLatch = CountDownLatch(1)
            closeCameraLatch = CountDownLatch(1)
        }

        fun awaitCameraOpenedAndAssert() {
            assertThat(openCameraLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun awaitCameraClosedAndAssert() {
            assertThat(closeCameraLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()
        }
    }
}
