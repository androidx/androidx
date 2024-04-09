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

package androidx.camera.video

import android.Manifest
import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_3_4
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_9_16
import androidx.camera.core.impl.utils.TransformUtils.is90or270
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.TransformUtils.within360
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor as ArgumentCaptorCameraX
import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val GENERAL_TIMEOUT = 5000L
private const val STATUS_TIMEOUT = 15000L

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class VideoRecordingTest(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName.contains(CameraPipeConfig::class.simpleName!!),
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule
    val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L
        private const val TAG = "VideoRecordingTest"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "back+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig()
                ),
                arrayOf(
                    "front+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig()
                ),
                arrayOf(
                    "back+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig()
                ),
                arrayOf(
                    "front+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig()
                ),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    // TODO(b/278168212): Only SDR is checked by now. Need to extend to HDR dynamic ranges.
    private val dynamicRange = DynamicRange.SDR
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var preview: Preview
    private lateinit var cameraInfo: CameraInfo
    private lateinit var videoCapabilities: VideoCapabilities
    private lateinit var camera: Camera

    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch

    private lateinit var finalize: VideoRecordEvent.Finalize
    private lateinit var mockVideoRecordEventConsumer: MockConsumer<VideoRecordEvent>
    private lateinit var videoCapture: VideoCapture<Recorder>

    private val audioStreamAvailable by lazy {
        AudioChecker.canAudioStreamBeStarted(
            context, cameraSelector, Recorder.DEFAULT_QUALITY_SELECTOR
        )
    }

    private val videoRecordEventListener = Consumer<VideoRecordEvent> {
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
            is VideoRecordEvent.Pause, is VideoRecordEvent.Resume -> {
                // no op for this test, skip these event now.
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        skipVideoRecordingTestIfNotSupportedByEmulator()

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        // Add extra Preview to provide an additional surface for b/168187087.
        preview = Preview.Builder().build()
        videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        instrumentation.runOnMainSync {
            // Sets surface provider to preview
            preview.setSurfaceProvider(getSurfaceProvider())

            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
            videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        }

        mockVideoRecordEventConsumer = MockConsumer<VideoRecordEvent>()
    }

    @After
    fun tearDown() {
        if (this::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun getMetadataRotation_when_setTargetRotation() {
        // Arrange.
        // Set Surface.ROTATION_90 for the 1st recording and update to Surface.ROTATION_180
        // for the 2nd recording.
        val targetRotation1 = Surface.ROTATION_90
        val targetRotation2 = Surface.ROTATION_180
        videoCapture.targetRotation = targetRotation1

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        // Act.
        completeVideoRecording(videoCapture, file)

        // Verify.
        val (videoContentRotation, metadataRotation) = getExpectedRotation(videoCapture, cameraInfo)
        verifyMetadataRotation(metadataRotation, file)

        // Cleanup.
        file.delete()

        // Arrange: Prepare for 2nd recording
        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        // Act: Update targetRotation.
        videoCapture.targetRotation = targetRotation2
        completeVideoRecording(videoCapture, file2)

        // Verify.
        val metadataRotation2 = cameraInfo.getSensorRotationDegrees(targetRotation2).let {
            if (isSurfaceProcessingEnabled(videoCapture)) {
                // If effect is enabled, the rotation should eliminate the video content rotation.
                within360(it - videoContentRotation)
            } else it
        }
        verifyMetadataRotation(metadataRotation2, file2)

        // Cleanup.
        file2.delete()
    }

    @Test
    fun getCorrectResolution_when_setAspectRatio4by3() {
        testGetCorrectResolution_when_setAspectRatio(RATIO_4_3)
    }

    @Test
    fun getCorrectResolution_when_setAspectRatio16by9() {
        testGetCorrectResolution_when_setAspectRatio(RATIO_16_9)
    }

    private fun testGetCorrectResolution_when_setAspectRatio(aspectRatio: Int) {
        // Pre-arrange.
        assumeExtraCroppingQuirk()
        assumeTrue(videoCapabilities.getSupportedQualities(dynamicRange).isNotEmpty())

        // Arrange.
        val recorder = Recorder.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)

        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture))

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        }

        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        // Act.
        val file = temporaryFolder.newFile()
        completeVideoRecording(videoCapture, file)

        // Verify.
        verifyVideoAspectRatio(
            getRotatedAspectRatio(aspectRatio, getRotationNeeded(videoCapture, cameraInfo)),
            file
        )
    }

    @Test
    fun getCorrectResolution_when_setCropRect() {
        assumeSuccessfulSurfaceProcessing()
        assumeExtraCroppingQuirk()

        // Arrange.
        assumeTrue(videoCapabilities.getSupportedQualities(dynamicRange).isNotEmpty())
        val quality = Quality.LOWEST
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        // Arbitrary cropping
        val profile = videoCapabilities.getProfiles(quality, dynamicRange)!!.defaultVideoProfile
        val targetResolution = Size(profile.width, profile.height)
        val cropRect = Rect(6, 6, targetResolution.width - 7, targetResolution.height - 7)
        videoCapture.setViewPortCropRect(cropRect)

        assumeTrue(
            "The UseCase combination is not supported for quality setting: $quality",
            camera.isUseCasesCombinationSupported(preview, videoCapture)
        )

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        }

        // TODO(b/264936115): In stream sharing (VirtualCameraAdapter), children's ViewPortCropRect
        //  is ignored and override to the parent size, the cropRect is also rotated. Skip the test
        //  for now.
        assumeTrue(!isStreamSharingEnabled(videoCapture))

        val file = File.createTempFile("video_", ".tmp").apply { deleteOnExit() }

        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        // Act.
        completeVideoRecording(videoCapture, file)

        // Verify.
        val resolution = rectToSize(videoCapture.cropRect!!)
        verifyVideoResolution(
            context,
            file,
            rotateSize(resolution, getRotationNeeded(videoCapture, cameraInfo))
        )

        // Cleanup.
        file.delete()
    }

    @Test
    fun stopRecording_when_useCaseUnbind() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        // Act.
        startVideoRecording(videoCapture, file).use {
            instrumentation.runOnMainSync {
                cameraProvider.unbind(videoCapture)
            }

            // Verify.
            // Wait for finalize event to saved file.
            assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

            assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)

            // Cleanup.
            file.delete()
        }
    }

    @Test
    fun stopRecordingWhenLifecycleStops() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        // Act.
        startVideoRecording(videoCapture, file).use {
            instrumentation.runOnMainSync {
                lifecycleOwner.pauseAndStop()
            }

            // Verify.
            // Wait for finalize event to saved file.
            assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

            assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)

            // Cleanup.
            file.delete()
        }
    }

    @Test
    fun start_finalizeImmediatelyWhenSourceInactive() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val videoCaptureMonitor = VideoCaptureMonitor()
        videoCapture.startVideoRecording(temporaryFolder.newFile(), videoCaptureMonitor).use {
            // Ensure the Recorder is initialized before start test.
            videoCaptureMonitor.waitForVideoCaptureStatus()
        }
        instrumentation.runOnMainSync {
            lifecycleOwner.pauseAndStop()
        }

        videoCapture.output
            .prepareRecording(context, FileOutputOptions.Builder(file).build())
            .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {
                mockVideoRecordEventConsumer.verifyAcceptCall(
                    VideoRecordEvent.Finalize::class.java,
                    false,
                    GENERAL_TIMEOUT
                )

                mockVideoRecordEventConsumer.verifyNoMoreAcceptCalls(false)

                val captor = ArgumentCaptorCameraX<VideoRecordEvent> { argument ->
                    VideoRecordEvent::class.java.isInstance(
                        argument
                    )
                }
                mockVideoRecordEventConsumer.verifyAcceptCall(
                    VideoRecordEvent::class.java,
                    false,
                    CallTimesAtLeast(1),
                    captor
                )
                val finalize = captor.value as VideoRecordEvent.Finalize
                assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)
            }

        file.delete()
    }

    @Test
    fun recordingWithPreviewAndImageAnalysis() {
        // Pre-check and arrange
        val analysis = ImageAnalysis.Builder().build()
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, analysis))

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)
        val latchForImageAnalysis = CountDownLatch(5)
        analysis.setAnalyzer(CameraXExecutors.directExecutor()) {
            latchForImageAnalysis.countDown()
            it.close()
        }

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysis,
                videoCapture
            )
        }

        // Act.
        completeVideoRecording(videoCapture, file)

        // Verify.
        verifyRecordingResult(file)
        assertThat(latchForImageAnalysis.await(10, TimeUnit.SECONDS)).isTrue()
        // Cleanup.
        file.delete()
    }

    @Test
    fun recordingWithPreviewAndImageCapture() {
        // Pre-check and arrange
        val imageCapture = ImageCapture.Builder().build()
        assumeTrue(
            camera.isUseCasesCombinationSupported(
                preview,
                videoCapture,
                imageCapture
            )
        )

        val videoFile = File.createTempFile("camerax-video", ".tmp").apply {
            deleteOnExit()
        }
        val imageFile = File.createTempFile("camerax-image-capture", ".tmp").apply {
            deleteOnExit()
        }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
        }

        // Act.
        completeVideoRecording(videoCapture, videoFile)
        completeImageCapture(imageCapture, imageFile)

        // Verify.
        verifyRecordingResult(videoFile)

        // Cleanup.
        videoFile.delete()
        imageFile.delete()
    }

    @Test
    fun canRecordMultipleFilesInARow() {
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file1, includeAudio = audioStreamAvailable)

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file2, includeAudio = audioStreamAvailable)

        val file3 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file3, includeAudio = audioStreamAvailable)

        verifyRecordingResult(file1, audioStreamAvailable)
        verifyRecordingResult(file2, audioStreamAvailable)
        verifyRecordingResult(file3, audioStreamAvailable)

        file1.delete()
        file2.delete()
        file3.delete()
    }

    @Test
    fun canRecordMultipleFilesWithThenWithoutAudio() {
        // This test requires that audio is available
        assumeTrue("Audio stream is not available", audioStreamAvailable)
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file1, includeAudio = true)

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file2, includeAudio = false)

        verifyRecordingResult(file1, true)
        verifyRecordingResult(file2, false)

        file1.delete()
        file2.delete()
    }

    @Test
    fun canRecordMultipleFilesWithoutThenWithAudio() {
        // This test requires that audio is available
        assumeTrue(audioStreamAvailable)
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file1, includeAudio = false)

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file2, includeAudio = true)

        verifyRecordingResult(file1, false)
        verifyRecordingResult(file2, true)

        file1.delete()
        file2.delete()
    }

    @Test
    fun canStartNextRecordingPausedAfterFirstRecordingFinalized() {
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        // Start and stop a recording to ensure recorder is idling
        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }

        performRecording(videoCapture, file1, audioStreamAvailable)

        // First recording is now finalized. Try starting second recording paused.
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }
        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file2).build())
            .apply {
                if (audioStreamAvailable) {
                    withAudioEnabled()
                }
            }.start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {

                it.pause()

                mockVideoRecordEventConsumer.verifyAcceptCall(
                    VideoRecordEvent.Start::class.java,
                    true,
                    GENERAL_TIMEOUT
                )

                mockVideoRecordEventConsumer.verifyAcceptCall(
                    VideoRecordEvent.Pause::class.java,
                    true,
                    GENERAL_TIMEOUT
                )
            }

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Finalize::class.java,
            true,
            GENERAL_TIMEOUT
        )

        file1.delete()
        file2.delete()
    }

    @Test
    fun nextRecordingCanBeStartedAfterLastRecordingStopped() {
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }

        try {
            performRecording(videoCapture, file1)
            performRecording(videoCapture, file2)

            verifyRecordingResult(file1)
            verifyRecordingResult(file2)
        } finally {
            file1.delete()
            file2.delete()
        }
    }

    @Test
    fun canSwitchAudioOnOff() {
        assumeTrue("Audio stream is not available", audioStreamAvailable)
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val file3 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        try {
            // Record the first video with audio enabled.
            performRecording(videoCapture, file1, true)

            // Record the second video with audio disabled.
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file2).build())
                .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {
                    mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()

                    // Check the audio information reports state as disabled.
                    val captor = ArgumentCaptorCameraX<VideoRecordEvent> { argument ->
                        VideoRecordEvent::class.java.isInstance(
                            argument
                        )
                    }
                    mockVideoRecordEventConsumer.verifyAcceptCall(
                        VideoRecordEvent::class.java,
                        false,
                        CallTimesAtLeast(1),
                        captor
                    )
                    assertThat(captor.value).isInstanceOf(VideoRecordEvent.Status::class.java)
                    val status = captor.value as VideoRecordEvent.Status
                    assertThat(status.recordingStats.audioStats.audioState)
                        .isEqualTo(AudioStats.AUDIO_STATE_DISABLED)
                }

            mockVideoRecordEventConsumer.verifyAcceptCall(
                VideoRecordEvent.Finalize::class.java,
                false,
                GENERAL_TIMEOUT
            )

            // Record the third video with audio enabled.
            performRecording(videoCapture, file3, true)

            // Check the audio in file is as expected.
            verifyRecordingResult(file1, true)
            verifyRecordingResult(file2, false)
            verifyRecordingResult(file3, true)
        } finally {
            file1.delete()
            file2.delete()
            file3.delete()
        }
    }

    @Test
    fun canReuseRecorder_explicitlyStop() {
        val recorder = Recorder.Builder().build()
        val videoCapture1 = VideoCapture.withOutput(recorder)
        val videoCapture2 = VideoCapture.withOutput(recorder)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture1)
        }

        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        performRecording(videoCapture1, file1, true)
        verifyRecordingResult(file1, true)
        file1.delete()

        instrumentation.runOnMainSync {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture2)
        }

        performRecording(videoCapture2, file2, true)
        verifyRecordingResult(file2, true)
        file2.delete()
    }

    @Test
    fun canReuseRecorder_sourceInactive() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        val recorder = Recorder.Builder().build()
        val videoCapture1 = VideoCapture.withOutput(recorder)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture1)
        }

        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        videoCapture1.output.prepareRecording(context, FileOutputOptions.Builder(file1).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {
                mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()

                // Unbind use case should stop the in-progress recording.
                instrumentation.runOnMainSync {
                    cameraProvider.unbindAll()
                }

                mockVideoRecordEventConsumer.verifyAcceptCall(
                    VideoRecordEvent.Finalize::class.java,
                    true,
                    GENERAL_TIMEOUT
                )
            }

        verifyRecordingResult(file1, true)
        file1.delete()

        val videoCapture2 = VideoCapture.withOutput(recorder)

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture2)
        }

        performRecording(videoCapture2, file2, true)
        verifyRecordingResult(file2, true)
        file2.delete()
    }

    @Test
    fun mute_defaultToNotMuted() {
        assumeTrue("Audio stream is not available", audioStreamAvailable)

        // Arrange.
        val recorder = Recorder.Builder().build()
        val videoCaptureLocal = VideoCapture.withOutput(recorder)
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCaptureLocal
            )
        }
        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        recorder.prepareRecording(context, FileOutputOptions.Builder(file1).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {
                mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()
                // Keep the first recording muted.
                it.mute(true)
            }

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Finalize::class.java,
            false,
            GENERAL_TIMEOUT
        )
        file1.delete()

        mockVideoRecordEventConsumer.clearAcceptCalls()

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        // Act.
        recorder.prepareRecording(context, FileOutputOptions.Builder(file2).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {
                mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()
                val captor = ArgumentCaptorCameraX<VideoRecordEvent> { argument ->
                    VideoRecordEvent::class.java.isInstance(
                        argument
                    )
                }
                mockVideoRecordEventConsumer.verifyAcceptCall(
                    VideoRecordEvent::class.java,
                    false,
                    CallTimesAtLeast(1),
                    captor
                )
                assertThat(captor.value).isInstanceOf(VideoRecordEvent.Status::class.java)
                val status = captor.value as VideoRecordEvent.Status
                // Assert: The second recording should not be muted.
                assertThat(status.recordingStats.audioStats.audioState)
                    .isEqualTo(AudioStats.AUDIO_STATE_ACTIVE)
            }

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Finalize::class.java,
            false,
            GENERAL_TIMEOUT
        )
        file2.delete()
    }

    @Ignore("b/285940946")
    @Test
    fun canContinueRecordingAfterRebind() {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val recording =
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withAudioEnabled()
                .asPersistentRecording()
                .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer)

        mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()

        mockVideoRecordEventConsumer.clearAcceptCalls()

        instrumentation.runOnMainSync {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Status::class.java,
            true,
            STATUS_TIMEOUT,
            CallTimesAtLeast(5)
        )

        recording.stop()

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Finalize::class.java,
            true,
            GENERAL_TIMEOUT
        )

        verifyRecordingResult(file, true)

        file.delete()
    }

    @Ignore("b/285940946")
    @Test
    fun canContinueRecordingPausedAfterRebind() {
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val recording =
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
                .withAudioEnabled()
                .asPersistentRecording()
                .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer)

        mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()

        recording.pause()

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Pause::class.java,
            true,
            GENERAL_TIMEOUT
        )

        mockVideoRecordEventConsumer.clearAcceptCalls()

        instrumentation.runOnMainSync {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        recording.resume()

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Resume::class.java,
            true,
            GENERAL_TIMEOUT
        )

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Status::class.java,
            true,
            STATUS_TIMEOUT,
            CallTimesAtLeast(5)
        )

        recording.stop()

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Finalize::class.java,
            true,
            GENERAL_TIMEOUT
        )

        verifyRecordingResult(file, true)

        file.delete()
    }

    @Test
    fun canRecordWithCorrectTransformation() {
        // Arrange.
        lateinit var backCamera: Camera
        lateinit var frontCamera: Camera

        // Act.
        instrumentation.runOnMainSync {
            backCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture
            )
        }

        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file1, includeAudio = true)

        instrumentation.runOnMainSync {
            cameraProvider.unbindAll()
            frontCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                videoCapture
            )
        }

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        performRecording(videoCapture, file2, includeAudio = true)

        // Assert.
        verifyMetadataRotation(
            getExpectedRotation(
                videoCapture,
                backCamera.cameraInfo
            ).metadataRotation, file1
        )
        verifyMetadataRotation(
            getExpectedRotation(
                videoCapture,
                frontCamera.cameraInfo
            ).metadataRotation, file2
        )

        file1.delete()
        file2.delete()
    }

    private fun performRecording(
        videoCapture: VideoCapture<Recorder>,
        file: File,
        includeAudio: Boolean = false
    ) {
        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
            .apply {
                if (includeAudio) {
                    withAudioEnabled()
                }
            }
            .start(CameraXExecutors.directExecutor(), mockVideoRecordEventConsumer).use {
                mockVideoRecordEventConsumer.verifyRecordingStartSuccessfully()
            }

        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent.Finalize::class.java,
            true,
            GENERAL_TIMEOUT
        )

        val captor = ArgumentCaptorCameraX<VideoRecordEvent> { argument ->
            VideoRecordEvent::class.java.isInstance(
                argument
            )
        }
        mockVideoRecordEventConsumer.verifyAcceptCall(
            VideoRecordEvent::class.java,
            false,
            CallTimesAtLeast(1),
            captor
        )
        val finalizeEvent = captor.allValues.last() as VideoRecordEvent.Finalize

        assertRecordingSuccessful(finalizeEvent, checkAudio = includeAudio)

        mockVideoRecordEventConsumer.clearAcceptCalls()
    }

    private fun assertRecordingSuccessful(
        finalizeEvent: VideoRecordEvent.Finalize,
        checkAudio: Boolean = false
    ) {
        assertWithMessage(
            "Recording did not finish successfully. Finished with error: ${
                VideoRecordEvent.Finalize.errorToString(
                    finalizeEvent.error
                )
            }"
        ).that(finalizeEvent.error).isEqualTo(ERROR_NONE)
        if (checkAudio) {
            val audioStats = finalizeEvent.recordingStats.audioStats
            assertWithMessage(
                "Recording with audio encountered audio error." +
                    "\n${audioStats.errorCause?.stackTraceToString()}"
            ).that(audioStats.audioState).isNotEqualTo(AudioStats.AUDIO_STATE_ENCODER_ERROR)
        }
    }

    private fun startVideoRecording(videoCapture: VideoCapture<Recorder>, file: File):
        Recording {
        val recording = videoCapture.output
            .prepareRecording(context, FileOutputOptions.Builder(file).build())
            .start(CameraXExecutors.directExecutor(), videoRecordEventListener)

        try {
            // Wait for status event to proceed recording for a while.
            assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS))
                .isTrue()
        } catch (ex: Exception) {
            recording.stop()
            throw ex
        }

        return recording
    }

    private fun completeVideoRecording(videoCapture: VideoCapture<Recorder>, file: File) {
        val recording = startVideoRecording(videoCapture, file)

        recording.stop()
        // Wait for finalize event to saved file.
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Check if any error after recording finalized
        assertWithMessage(TAG + "Finalize with error: ${finalize.error}, ${finalize.cause}.")
            .that(finalize.hasError()).isFalse()
    }

    private fun completeImageCapture(imageCapture: ImageCapture, imageFile: File) {
        val savedCallback = ImageSavedCallback()

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(imageFile).build(),
            CameraXExecutors.ioExecutor(),
            savedCallback
        )
        savedCallback.verifyCaptureResult()
    }

    data class ExpectedRotation(val contentRotation: Int, val metadataRotation: Int)

    private fun getExpectedRotation(
        videoCapture: VideoCapture<Recorder>,
        cameraInfo: CameraInfo
    ): ExpectedRotation {
        val rotationNeeded = getRotationNeeded(videoCapture, cameraInfo)
        return if (isSurfaceProcessingEnabled(videoCapture)) {
            ExpectedRotation(rotationNeeded, 0)
        } else {
            ExpectedRotation(0, rotationNeeded)
        }
    }

    private fun getRotatedAspectRatio(aspectRatio: Int, rotation: Int): Rational {
        val needRotate = is90or270(rotation)
        return when (aspectRatio) {
            RATIO_4_3 -> if (needRotate) ASPECT_RATIO_3_4 else ASPECT_RATIO_4_3
            RATIO_16_9 -> if (needRotate) ASPECT_RATIO_9_16 else ASPECT_RATIO_16_9
            else -> throw IllegalArgumentException("Unknown aspect ratio: $aspectRatio")
        }
    }

    private fun verifyMetadataRotation(expectedRotation: Int, file: File) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(file))
            val videoRotation = it.getRotation()

            // Checks the rotation from video file's metadata is matched with the relative rotation.
            assertWithMessage(
                TAG + ", rotation test failure: " +
                    "videoRotation: $videoRotation" +
                    ", expectedRotation: $expectedRotation"
            ).that(videoRotation).isEqualTo(expectedRotation)
        }
    }

    private fun verifyVideoAspectRatio(expectedAspectRatio: Rational, file: File) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(file))
            val aspectRatio = it.getRotatedAspectRatio()

            assertWithMessage(
                TAG + ", verifyVideoAspectRatio failure:" +
                    ", videoAspectRatio: $aspectRatio" +
                    ", expectedAspectRatio: $expectedAspectRatio"
            ).that(aspectRatio.toDouble()).isWithin(0.1).of(expectedAspectRatio.toDouble())
        }
    }

    private fun verifyRecordingResult(file: File, hasAudio: Boolean = false) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(file))

            assertThat(it.hasVideo()).isTrue()
            assertThat(it.hasAudio()).isEqualTo(hasAudio)
        }
    }

    private fun getSurfaceProvider(): Preview.SurfaceProvider {
        return SurfaceTextureProvider.createSurfaceTextureProvider(
            object : SurfaceTextureProvider.SurfaceTextureCallback {
                override fun onSurfaceTextureReady(
                    surfaceTexture: SurfaceTexture,
                    resolution: Size
                ) {
                    // No-op
                }

                override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                    surfaceTexture.release()
                }
            }
        )
    }

    private fun assumeExtraCroppingQuirk() {
        assumeExtraCroppingQuirk(implName)
    }

    private class ImageSavedCallback :
        ImageCapture.OnImageSavedCallback {

        private val latch = CountDownLatch(1)
        val results = mutableListOf<ImageCapture.OutputFileResults>()
        val errors = mutableListOf<ImageCaptureException>()

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            results.add(outputFileResults)
            latch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            errors.add(exception)
            Log.e(TAG, "OnImageSavedCallback.onError: ${exception.message}")
            latch.countDown()
        }

        fun verifyCaptureResult() {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue()
        }
    }

    private fun MockConsumer<VideoRecordEvent>.verifyRecordingStartSuccessfully() {
        verifyAcceptCall(
            VideoRecordEvent.Start::class.java,
            true,
            GENERAL_TIMEOUT
        )
        verifyAcceptCall(
            VideoRecordEvent.Status::class.java,
            true,
            STATUS_TIMEOUT,
            CallTimesAtLeast(5)
        )
    }

    private fun VideoCapture<Recorder>.startVideoRecording(
        file: File,
        eventListener: Consumer<VideoRecordEvent>
    ): Recording =
        output.prepareRecording(
            context, FileOutputOptions.Builder(file).build()
        ).start(
            CameraXExecutors.directExecutor(), eventListener
        )
}

@RequiresApi(21)
private class VideoCaptureMonitor : Consumer<VideoRecordEvent> {
    private var countDown: CountDownLatch? = null

    fun waitForVideoCaptureStatus(
        count: Int = 10,
        timeoutMillis: Long = TimeUnit.SECONDS.toMillis(10)
    ) {
        assertWithMessage("Video recording doesn't start").that(synchronized(this) {
            countDown = CountDownLatch(count)
            countDown
        }!!.await(timeoutMillis, TimeUnit.MILLISECONDS)).isTrue()
    }

    override fun accept(value: VideoRecordEvent) {
        when (value) {
            is VideoRecordEvent.Status -> {
                synchronized(this) {
                    countDown?.countDown()
                }
            }
            else -> {
                // Ignore other events.
            }
        }
    }
}
