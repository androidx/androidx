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
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.createCameraSelectorById
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.StressTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.lifecycle.Observer
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
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class OpenCloseCameraStressTest(
    private val cameraId: String
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
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

    @Before
    fun setUp(): Unit = runBlocking {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        cameraIdCameraSelector = createCameraSelectorById(cameraId)

        camera = withContext(Dispatchers.Main) {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraIdCameraSelector)
        }

        preview = Preview.Builder().build()
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

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}")
        val parameters: Collection<String>
            get() = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(preview, imageCapture)
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
            imageAnalysis = imageAnalysis
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = STRESS_TEST_REPEAT_COUNT)
    fun openCloseCameraStressTest_withPreviewVideoCapture(): Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        bindUseCase_unbindAll_toCheckCameraState_repeatedly(preview, videoCapture = videoCapture)
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
            imageCapture = imageCapture
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
            imageAnalysis = imageAnalysis
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
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            val openCameraLatch = CountDownLatch(1)
            val closeCameraLatch = CountDownLatch(1)
            val observer = Observer<CameraState> { state ->
                if (state.type == CameraState.Type.OPEN) {
                    openCameraLatch.countDown()
                } else if (state.type == CameraState.Type.CLOSED) {
                    closeCameraLatch.countDown()
                }
            }

            withContext(Dispatchers.Main) {
                // Arrange: sets up CameraState observer
                camera.cameraInfo.cameraState.observe(lifecycleOwner, observer)

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

            // Assert: checks the CameraState.Type.OPEN can be received
            assertThat(openCameraLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()

            // Act: unbinds all use cases
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }

            // Assert: checks the CameraState.Type.CLOSED can be received
            assertThat(closeCameraLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue()

            // Clean it up.
            withContext(Dispatchers.Main) {
                camera.cameraInfo.cameraState.removeObserver(observer)
            }
        }
    }
}
