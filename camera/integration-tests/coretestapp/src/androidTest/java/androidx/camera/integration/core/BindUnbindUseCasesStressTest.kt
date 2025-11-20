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
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.core.util.StressTestUtil
import androidx.camera.integration.core.util.StressTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_IMAGE_ANALYSIS
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_VIDEO_CAPTURE
import androidx.camera.integration.core.util.StressTestUtil.createCameraSelectorById
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.GLUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.StressTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.RepeatRule
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
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

private const val VIDEO_TIMEOUT_SEC = 10L
private const val TAG = "BindUnbindUseCasesStressTest"
private const val INVALID_TEX_ID = -1
private var texId = INVALID_TEX_ID

@LargeTest
@RunWith(Parameterized::class)
class BindUnbindUseCasesStressTest(
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

    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch

    private lateinit var finalize: VideoRecordEvent.Finalize

    private val videoRecordEventListener =
        Consumer<VideoRecordEvent> {
            when (it) {
                is VideoRecordEvent.Start -> {
                    // Recording start.
                    Log.d(TAG, "Recording start")
                }
                is VideoRecordEvent.Finalize -> {
                    // Recording stop.
                    Log.d(TAG, "Recording finalize")
                    finalize = it
                    latchForVideoSaved.countDown()
                }
                is VideoRecordEvent.Status -> {
                    // Make sure the recording proceed for a while.
                    latchForVideoRecording.countDown()
                }
                is VideoRecordEvent.Pause,
                is VideoRecordEvent.Resume -> {
                    // no op for this test, skip these event now.
                }
                else -> {
                    throw IllegalStateException()
                }
            }
        }

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

        preview = Preview.Builder().build()
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
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewImageCapture(): Unit = runBlocking {
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            verificationTarget = VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkImageCaptureInEachTime_withPreviewImageCapture(): Unit =
        runBlocking {
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                imageCapture,
                verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewImageCaptureImageAnalysis(): Unit =
        runBlocking {
            val imageAnalysis = createImageAnalysis()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                imageCapture,
                imageAnalysis = imageAnalysis,
                verificationTarget = VERIFICATION_TARGET_PREVIEW,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkImageCaptureInEachTime_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkImageAnalysisInEachTime_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            imageCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_ANALYSIS,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewVideoCapture(): Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            videoCapture = videoCapture,
            verificationTarget = VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkVideoCaptureInEachTime_withPreviewVideoCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                videoCapture = videoCapture,
                verificationTarget = VERIFICATION_TARGET_VIDEO_CAPTURE,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewVideoCaptureImageCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                imageCapture,
                videoCapture,
                verificationTarget = VERIFICATION_TARGET_PREVIEW,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkVideoCaptureInEachTime_withPreviewVideoCaptureImageCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                imageCapture,
                videoCapture,
                verificationTarget = VERIFICATION_TARGET_VIDEO_CAPTURE,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkImageCaptureInEachTime_withPreviewVideoCaptureImageCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                imageCapture,
                videoCapture,
                verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkPreviewInEachTime_withPreviewVideoCaptureImageAnalysis(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            val imageAnalysis = createImageAnalysis()
            assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
            bindUseCases_checkOutput_thenUnbindAll_repeatedly(
                preview,
                videoCapture = videoCapture,
                imageAnalysis = imageAnalysis,
                verificationTarget = VERIFICATION_TARGET_PREVIEW,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkVideoCaptureInEachTime_withPreviewVideoCaptureImageAnalysis():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            videoCapture = videoCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_VIDEO_CAPTURE,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun bindUnbindUseCases_checkImageAnalysisInEachTime_withPreviewVideoCaptureImageAnalysis():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
        bindUseCases_checkOutput_thenUnbindAll_repeatedly(
            preview,
            videoCapture = videoCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_ANALYSIS,
        )
    }

    /**
     * Repeatedly binds use cases, checks the input use cases' capture functions can work well, and
     * unbind all use cases.
     *
     * <p>This function checks the nullabilities of the input ImageCapture, VideoCapture and
     * ImageAnalysis to determine whether the use cases will be bound together to run the test.
     */
    private fun bindUseCases_checkOutput_thenUnbindAll_repeatedly(
        preview: Preview,
        imageCapture: ImageCapture? = null,
        videoCapture: VideoCapture<Recorder>? = null,
        imageAnalysis: ImageAnalysis? = null,
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT,
    ): Unit = runBlocking {
        for (i in 1..repeatCount) {
            // Arrange.
            // Sets up Preview frame available monitor
            val previewFrameAvailableMonitor = PreviewFrameAvailableMonitor()

            // Sets up the necessary objects for VideoCapture part if videCapture is non-null
            var newVideoCapture: VideoCapture<Recorder>? = null
            videoCapture?.let {
                // VideoCapture needs to be recreated everytime until b/212654991 is fixed
                newVideoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            }

            // Act: binds use cases
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createSurfaceTextureProvider(
                        previewFrameAvailableMonitor.createSurfaceTextureCallback()
                    )
                )

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraIdCameraSelector,
                    *listOfNotNull(preview, imageCapture, newVideoCapture, imageAnalysis)
                        .toTypedArray(),
                )
            }

            // Assert: checks that Preview frames can be received
            if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                previewFrameAvailableMonitor.awaitSurfaceTextureReadyAndAssert()
                previewFrameAvailableMonitor.awaitAvailableFramesAndAssert()
            }

            // Assert: checks that the captured image of ImageCapture can be received
            if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                imageCapture!!.let {
                    val imageCaptureCaptureSuccessMonitor = ImageCaptureCaptureSuccessMonitor()

                    it.takePicture(
                        Executors.newSingleThreadExecutor(),
                        imageCaptureCaptureSuccessMonitor.createCaptureCallback(),
                    )

                    imageCaptureCaptureSuccessMonitor.awaitCaptureSuccessAndAssert()
                }
            }

            // Assert: checks that a video can be recorded by VideoCapture
            if (verificationTarget.and(VERIFICATION_TARGET_VIDEO_CAPTURE) != 0) {
                newVideoCapture!!.let {
                    latchForVideoSaved = CountDownLatch(1)
                    latchForVideoRecording = CountDownLatch(5)
                    val videoFile =
                        File.createTempFile("camerax-video", ".tmp").apply { deleteOnExit() }

                    completeVideoRecording(it, videoFile)
                    videoFile.delete()
                }
            }

            // Assert: checks that images can be received by the ImageAnalysis.Analyzer
            if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_ANALYSIS) != 0) {
                imageAnalysis!!.let {
                    val analyzerFrameAvailableMonitor = ImageAnalysisImageAvailableMonitor()
                    it.setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        analyzerFrameAvailableMonitor.createAnalyzer(),
                    )
                    analyzerFrameAvailableMonitor.awaitAvailableFramesAndAssert()
                }
            }

            // Clean it up.
            withContext(Dispatchers.Main) { cameraProvider.unbindAll() }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewImageCapture(): Unit =
        runBlocking {
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                imageCapture,
                verificationTarget = VERIFICATION_TARGET_PREVIEW,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkImageCapture_afterBindUnbindUseCasesRepeatedly_withPreviewImageCapture(): Unit =
        runBlocking {
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                imageCapture,
                verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkImageCapture_afterBindUnbindUseCasesRepeatedly_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkImageAnalysis_afterBindUnbindUseCasesRepeatedly_withPreviewImageCaptureImageAnalysis():
        Unit = runBlocking {
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_ANALYSIS,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                videoCapture = videoCapture,
                verificationTarget = VERIFICATION_TARGET_PREVIEW,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkVideoCapture_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                videoCapture = videoCapture,
                verificationTarget = VERIFICATION_TARGET_VIDEO_CAPTURE,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCaptureImageCapture(): Unit =
        runBlocking {
            val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
            bindUseCases_unbindAll_repeatedly_thenCheckOutput(
                preview,
                imageCapture,
                videoCapture,
                verificationTarget = VERIFICATION_TARGET_PREVIEW,
            )
        }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkVideoCapture_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCaptureImageCapture():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            videoCapture,
            verificationTarget = VERIFICATION_TARGET_VIDEO_CAPTURE,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkImageCapture_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCaptureImageCapture():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        assumeTrue(camera.isUseCasesCombinationSupported(preview, imageCapture, videoCapture))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            imageCapture,
            videoCapture,
            verificationTarget = VERIFICATION_TARGET_IMAGE_CAPTURE,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCaptureImageAnalysis():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            videoCapture = videoCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkVideoCapture_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCaptureImageAnalysis():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            videoCapture = videoCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_VIDEO_CAPTURE,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkImageAnalysis_afterBindUnbindUseCasesRepeatedly_withPreviewVideoCaptureImageAnalysis():
        Unit = runBlocking {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageAnalysis = createImageAnalysis()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, imageAnalysis))
        bindUseCases_unbindAll_repeatedly_thenCheckOutput(
            preview,
            videoCapture = videoCapture,
            imageAnalysis = imageAnalysis,
            verificationTarget = VERIFICATION_TARGET_IMAGE_ANALYSIS,
        )
    }

    private fun createImageAnalysis() =
        ImageAnalysis.Builder().build().also {
            it.setAnalyzer(CameraXExecutors.directExecutor()) { image -> image.close() }
        }

    /**
     * Repeatedly binds use cases and unbind all, then checks the input use cases' capture functions
     * can work well.
     *
     * <p>This function checks the nullabilities of the input ImageCapture, VideoCapture and
     * ImageAnalysis to determine whether the use cases will be bound together to run the test.
     */
    private fun bindUseCases_unbindAll_repeatedly_thenCheckOutput(
        preview: Preview,
        imageCapture: ImageCapture? = null,
        videoCapture: VideoCapture<Recorder>? = null,
        imageAnalysis: ImageAnalysis? = null,
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT,
    ): Unit = runBlocking {
        lateinit var previewFrameAvailableMonitor: PreviewFrameAvailableMonitor
        var newVideoCapture: VideoCapture<Recorder>? = null

        for (i in 1..repeatCount) {
            // Arrange.
            // Sets up Preview frame available monitor
            previewFrameAvailableMonitor = PreviewFrameAvailableMonitor()

            // Sets up the necessary objects for VideoCapture part if videCapture is non-null
            videoCapture?.let {
                // VideoCapture needs to be recreated everytime until b/212654991 is fixed
                newVideoCapture = VideoCapture.withOutput(Recorder.Builder().build())
            }

            // Act: binds use cases
            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(
                    SurfaceTextureProvider.createSurfaceTextureProvider(
                        previewFrameAvailableMonitor.createSurfaceTextureCallback()
                    )
                )

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraIdCameraSelector,
                    *listOfNotNull(preview, imageCapture, newVideoCapture, imageAnalysis)
                        .toTypedArray(),
                )

                // Clean it up: do not unbind at the last time
                if (i != repeatCount) {
                    cameraProvider.unbindAll()
                }
            }
        }

        // Assert: checks that Preview frames can be received
        if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
            previewFrameAvailableMonitor.awaitSurfaceTextureReadyAndAssert()
            previewFrameAvailableMonitor.awaitAvailableFramesAndAssert()
        }

        // Assert: checks that the captured image of ImageCapture can be received
        if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
            imageCapture!!.let {
                val imageCaptureCaptureSuccessMonitor = ImageCaptureCaptureSuccessMonitor()

                it.takePicture(
                    Executors.newSingleThreadExecutor(),
                    imageCaptureCaptureSuccessMonitor.createCaptureCallback(),
                )

                imageCaptureCaptureSuccessMonitor.awaitCaptureSuccessAndAssert()
            }
        }

        // Assert: checks that a video can be recorded by VideoCapture
        if (verificationTarget.and(VERIFICATION_TARGET_VIDEO_CAPTURE) != 0) {
            newVideoCapture!!.let {
                latchForVideoSaved = CountDownLatch(1)
                latchForVideoRecording = CountDownLatch(5)
                val videoFile =
                    File.createTempFile("camerax-video", ".tmp").apply { deleteOnExit() }

                completeVideoRecording(it, videoFile)
                videoFile.delete()
            }
        }

        // Assert: checks that images can be received by the ImageAnalysis.Analyzer
        if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_ANALYSIS) != 0) {
            imageAnalysis!!.let {
                val analyzerFrameAvailableMonitor = ImageAnalysisImageAvailableMonitor()
                it.setAnalyzer(
                    Executors.newSingleThreadExecutor(),
                    analyzerFrameAvailableMonitor.createAnalyzer(),
                )
                analyzerFrameAvailableMonitor.awaitAvailableFramesAndAssert()
            }
        }
    }

    private fun startVideoRecording(videoCapture: VideoCapture<Recorder>, file: File): Recording {
        val recording =
            videoCapture.output
                .prepareRecording(context, FileOutputOptions.Builder(file).build())
                .start(CameraXExecutors.directExecutor(), videoRecordEventListener)

        try {
            // Waits for status event to proceed recording for a while.
            assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        } catch (ex: Exception) {
            recording.stop()
            throw ex
        }

        return recording
    }

    private fun completeVideoRecording(videoCapture: VideoCapture<Recorder>, file: File) {
        val recording = startVideoRecording(videoCapture, file)

        recording.stop()
        // Waits for finalize event to saved file.
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Checks if any error after recording finalized
        Truth.assertWithMessage(TAG + "Finalize with error: ${finalize.error}, ${finalize.cause}.")
            .that(finalize.hasError())
            .isFalse()
    }

    private class PreviewFrameAvailableMonitor {
        private var isSurfaceTextureReleased = false
        private val isSurfaceTextureReleasedLock = Any()

        private var surfaceTextureLatch = CountDownLatch(1)
        private var previewFrameCountDownLatch: CountDownLatch? = null

        private val onFrameAvailableListener =
            object : SurfaceTexture.OnFrameAvailableListener {
                private var complete = false

                override fun onFrameAvailable(surfaceTexture: SurfaceTexture): Unit = runBlocking {
                    withContext(Dispatchers.Main) {
                        synchronized(isSurfaceTextureReleasedLock) {
                            if (!isSurfaceTextureReleased) {
                                surfaceTexture.updateTexImage()
                            }
                        }
                    }

                    previewFrameCountDownLatch?.let {
                        if (!complete) {
                            it.countDown()
                            if (it.count == 0L) {
                                complete = true
                            }
                        }
                    }
                }
            }

        private val frameAvailableHandler: Handler
        private val frameAvailableHandlerThread =
            HandlerThread("FrameAvailable").also {
                it.start()
                frameAvailableHandler = Handler(it.looper)
            }

        fun createSurfaceTextureCallback(): SurfaceTextureProvider.SurfaceTextureCallback =
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size,
                ) {
                    if (texId == INVALID_TEX_ID) {
                        texId = GLUtil.getTexIdFromGLContext()
                    }
                    surfaceTexture.attachToGLContext(texId)
                    surfaceTexture.setOnFrameAvailableListener(
                        onFrameAvailableListener,
                        frameAvailableHandler,
                    )

                    surfaceTextureLatch.countDown()
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    synchronized(isSurfaceTextureReleasedLock) {
                        isSurfaceTextureReleased = true
                        surfaceTexture.release()
                        frameAvailableHandlerThread.quitSafely()
                    }
                }
            }

        fun awaitSurfaceTextureReadyAndAssert(timeoutDurationMs: Long = 1000) {
            assertThat(surfaceTextureLatch.await(timeoutDurationMs, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun awaitAvailableFramesAndAssert(count: Int = 10, timeoutDurationMs: Long = 10000) {
            previewFrameCountDownLatch = CountDownLatch(count)
            assertThat(previewFrameCountDownLatch!!.await(timeoutDurationMs, TimeUnit.MILLISECONDS))
                .isTrue()
        }
    }

    private class ImageCaptureCaptureSuccessMonitor {
        private val captureSuccessCountDownLatch = CountDownLatch(1)

        fun createCaptureCallback() =
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    image.close()
                    captureSuccessCountDownLatch.countDown()
                }
            }

        fun awaitCaptureSuccessAndAssert(timeoutDurationMs: Long = 10000) {
            assertThat(captureSuccessCountDownLatch.await(timeoutDurationMs, TimeUnit.MILLISECONDS))
                .isTrue()
        }
    }

    private class ImageAnalysisImageAvailableMonitor {
        private var analyzerFrameCountDownLatch: CountDownLatch? = null

        fun createAnalyzer() =
            ImageAnalysis.Analyzer { image ->
                image.close()
                analyzerFrameCountDownLatch?.countDown()
            }

        fun awaitAvailableFramesAndAssert(count: Int = 10, timeoutDurationMs: Long = 10000) {
            analyzerFrameCountDownLatch = CountDownLatch(count)
            assertThat(
                    analyzerFrameCountDownLatch!!.await(timeoutDurationMs, TimeUnit.MILLISECONDS)
                )
                .isTrue()
        }
    }
}
