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
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
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
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class VideoRecordingTest(
    private var cameraSelector: CameraSelector
) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L
        private const val TAG = "VideoRecordingTest"
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(CameraSelector.DEFAULT_BACK_CAMERA),
                arrayOf(CameraSelector.DEFAULT_FRONT_CAMERA),
            )
        }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var preview: Preview
    private lateinit var cameraInfo: CameraInfo
    private lateinit var camera: Camera

    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch

    private lateinit var finalize: VideoRecordEvent.Finalize

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
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        // Skip test for b/168175357
        Assume.assumeFalse(
            "Cuttlefish has MediaCodec dequeueInput/Output buffer fails issue. Unable to test.",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 29
        )

        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        // Add extra Preview to provide an additional surface for b/168187087.
        preview = Preview.Builder().build()

        instrumentation.runOnMainSync {
            // Sets surface provider to preview
            preview.setSurfaceProvider(getSurfaceProvider())

            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
        }
    }

    @After
    fun tearDown() {
        if (this::cameraProvider.isInitialized) {
            instrumentation.runOnMainSync {
                cameraProvider.unbindAll()
            }
            cameraProvider.shutdown()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun getMetadataRotation_when_setTargetRotation() {
        // Arrange.
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        // Just set one Surface.ROTATION_90 to verify the function work or not.
        val targetRotation = Surface.ROTATION_90
        videoCapture.targetRotation = targetRotation

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        // Act.
        completeVideoRecording(videoCapture, file)

        // Verify.
        verifyMetadataRotation(targetRotation, file)
        // Cleanup.
        file.delete()
    }

    // TODO: Add other metadata info check, e.g. location, after Recorder add more metadata.

    @Test
    fun getCorrectResolution_when_setSupportedQuality() {
        // Pre-arrange.
        Assume.assumeTrue(QualitySelector.getSupportedQualities(cameraInfo).isNotEmpty())
        val qualityList = QualitySelector.getSupportedQualities(cameraInfo)
        Log.d(TAG, "CameraSelector: ${cameraSelector.lensFacing}, QualityList: $qualityList ")

        qualityList.forEach loop@{ quality ->
            // Arrange.
            val targetResolution = QualitySelector.getResolution(cameraInfo, quality)
            if (targetResolution == null) {
                // If targetResolution is null, try next one
                Log.e(TAG, "Unable to get resolution for the quality: $quality")
                return@loop
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(quality)).build()

            val videoCapture = VideoCapture.withOutput(recorder)

            if (!camera.isUseCasesCombinationSupported(preview, videoCapture)) {
                Log.e(TAG, "The UseCase combination is not supported for quality setting: $quality")
                return@loop
            }

            instrumentation.runOnMainSync {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            }

            val file = File.createTempFile("video_$targetResolution", ".tmp")
                .apply { deleteOnExit() }

            latchForVideoSaved = CountDownLatch(1)
            latchForVideoRecording = CountDownLatch(5)

            // Act.
            completeVideoRecording(videoCapture, file)

            // Verify.
            verifyVideoResolution(targetResolution, file)

            // Cleanup.
            file.delete()
        }
    }

    @Test
    fun stopRecording_when_useCaseUnbind() {
        // Arrange.
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
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
        // Arrange.
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
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
        // Arrange.
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        @Suppress("UNCHECKED_CAST")
        val mockListener = mock(Consumer::class.java) as Consumer<VideoRecordEvent>
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
            lifecycleOwner.pauseAndStop()
        }

        // Act.
        videoCapture.output
            .prepareRecording(context, FileOutputOptions.Builder(file).build())
            .start(CameraXExecutors.directExecutor(), mockListener).use {

                // Verify.
                verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Finalize::class.java))
                verifyNoMoreInteractions(mockListener)
                val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
                verify(mockListener, atLeastOnce()).accept(captor.capture())
                val finalize = captor.value as VideoRecordEvent.Finalize
                assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)

                // Cleanup.
                file.delete()
            }
    }

    @Test
    fun recordingWithPreviewAndImageAnalysis() {
        // Pre-check and arrange
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val analysis = ImageAnalysis.Builder().build()
        Assume.assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture, analysis))

        val file = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        latchForVideoSaved = CountDownLatch(1)
        latchForVideoRecording = CountDownLatch(5)
        val latchForImageAnalysis = CountDownLatch(5)
        analysis.setAnalyzer(CameraXExecutors.directExecutor()) { it: ImageProxy ->
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
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        val imageCapture = ImageCapture.Builder().build()
        Assume.assumeTrue(
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
        @Suppress("UNCHECKED_CAST")
        val mockListener = mock(Consumer::class.java) as Consumer<VideoRecordEvent>
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(mockListener)
        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file1).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockListener).use { activeRecording ->

                inOrder.verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Start::class.java))
                inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                    .accept(any(VideoRecordEvent.Status::class.java))

                activeRecording.stop()
            }

        inOrder.verify(mockListener, timeout(5000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file2).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockListener).use { activeRecording ->

                inOrder.verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Start::class.java))
                inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                    .accept(any(VideoRecordEvent.Status::class.java))

                activeRecording.stop()
            }

        inOrder.verify(mockListener, timeout(5000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        val file3 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file3).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockListener).use { activeRecording ->

                inOrder.verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Start::class.java))
                inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                    .accept(any(VideoRecordEvent.Status::class.java))

                activeRecording.stop()
            }

        inOrder.verify(mockListener, timeout(5000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        verifyRecordingResult(file1, true)
        verifyRecordingResult(file2, true)
        verifyRecordingResult(file3, true)

        file1.delete()
        file2.delete()
        file3.delete()
    }

    @Test
    fun canStartNextRecordingPausedAfterFirstRecordingFinalized() {
        @Suppress("UNCHECKED_CAST")
        val mockListener = mock(Consumer::class.java) as Consumer<VideoRecordEvent>
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }

        // Start and stop a recording to ensure recorder is idling
        val inOrder = inOrder(mockListener)

        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file1).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockListener).use { activeRecording1 ->

                inOrder.verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Start::class.java))

                inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                    .accept(any(VideoRecordEvent.Status::class.java))

                activeRecording1.stop()
            }

        inOrder.verify(mockListener, timeout(5000L))
            .accept(any(VideoRecordEvent.Finalize::class.java))

        // First recording is now finalized. Try starting second recording paused.
        videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file2).build())
            .withAudioEnabled()
            .start(CameraXExecutors.directExecutor(), mockListener).use { activeRecording2 ->

                activeRecording2.pause()

                inOrder.verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Start::class.java))

                inOrder.verify(mockListener, timeout(1000L))
                    .accept(any(VideoRecordEvent.Pause::class.java))

                activeRecording2.stop()
            }

        file1.delete()
        file2.delete()
    }

    @Test
    fun nextRecordingCanBeStartedAfterLastRecordingStopped() {
        @Suppress("UNCHECKED_CAST")
        val mockListener = mock(Consumer::class.java) as Consumer<VideoRecordEvent>
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }
        val file1 = File.createTempFile("CameraX1", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX2", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(mockListener)
        try {
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file1).build())
                .start(CameraXExecutors.directExecutor(), mockListener).use {
                    inOrder.verify(mockListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))
                }

            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file2).build())
                .start(CameraXExecutors.directExecutor(), mockListener).use {
                    inOrder.verify(mockListener, timeout(5000L))
                        .accept(any(VideoRecordEvent.Finalize::class.java))
                    inOrder.verify(mockListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))
                }

            inOrder.verify(mockListener, timeout(5000L))
                .accept(any(VideoRecordEvent.Finalize::class.java))

            verifyRecordingResult(file1)
            verifyRecordingResult(file2)
        } finally {
            file1.delete()
            file2.delete()
        }
    }

    @Test
    fun canSwitchAudioOnOff() {
        @Suppress("UNCHECKED_CAST")
        val mockListener = mock(Consumer::class.java) as Consumer<VideoRecordEvent>
        val videoCapture = VideoCapture.withOutput(Recorder.Builder().build())
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
        }

        val file1 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val file2 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
        val file3 = File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }

        val inOrder = inOrder(mockListener)
        try {
            // Record the first video with audio enabled.
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file1).build())
                .withAudioEnabled()
                .start(CameraXExecutors.directExecutor(), mockListener).use {
                    inOrder.verify(mockListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))
                }

            // Record the second video with audio disabled.
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file2).build())
                .start(CameraXExecutors.directExecutor(), mockListener).use {
                    inOrder.verify(mockListener, timeout(5000L))
                        .accept(any(VideoRecordEvent.Finalize::class.java))
                    inOrder.verify(mockListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))

                    // Check the audio information reports state as disabled.
                    val captor = ArgumentCaptor.forClass(VideoRecordEvent::class.java)
                    verify(mockListener, atLeastOnce()).accept(captor.capture())
                    assertThat(captor.value).isInstanceOf(VideoRecordEvent.Status::class.java)
                    val status = captor.value as VideoRecordEvent.Status
                    assertThat(status.recordingStats.audioStats.audioState)
                        .isEqualTo(AudioStats.AUDIO_STATE_DISABLED)
                }

            // Record the third video with audio enabled.
            videoCapture.output.prepareRecording(context, FileOutputOptions.Builder(file3).build())
                .withAudioEnabled()
                .start(CameraXExecutors.directExecutor(), mockListener).use {
                    inOrder.verify(mockListener, timeout(5000L))
                        .accept(any(VideoRecordEvent.Finalize::class.java))
                    inOrder.verify(mockListener, timeout(1000L))
                        .accept(any(VideoRecordEvent.Start::class.java))
                    inOrder.verify(mockListener, timeout(15000L).atLeast(5))
                        .accept(any(VideoRecordEvent.Status::class.java))
                }

            inOrder.verify(mockListener, timeout(5000L))
                .accept(any(VideoRecordEvent.Finalize::class.java))

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

    private fun verifyMetadataRotation(targetRotation: Int, file: File) {
        // Whether the camera lens and display are facing opposite directions.
        val isOpposite = cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK
        val relativeRotation = CameraOrientationUtil.getRelativeImageRotation(
            CameraOrientationUtil.surfaceRotationToDegrees(targetRotation),
            CameraUtil.getSensorOrientation(cameraSelector.lensFacing!!)!!,
            isOpposite
        )
        val videoRotation = getRotationInMetadata(Uri.fromFile(file))

        // Checks the rotation from video file's metadata is matched with the relative rotation.
        assertWithMessage(
            TAG + ", $targetRotation rotation test failure:" +
                ", videoRotation: $videoRotation" +
                ", relativeRotation: $relativeRotation"
        ).that(videoRotation).isEqualTo(relativeRotation)
    }

    private fun verifyVideoResolution(targetResolution: Size, file: File) {
        val mediaRetriever = MediaMetadataRetriever()
        lateinit var resolution: Size
        mediaRetriever.apply {
            setDataSource(context, Uri.fromFile(file))
            val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)!!
                .toInt()
            val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)!!
                .toInt()
            resolution = Size(width, height)
        }

        // Compare with the resolution of video and the targetResolution in QualitySelector
        assertWithMessage(
            TAG + ", verifyVideoResolution failure:" +
                ", videoResolution: $resolution" +
                ", targetResolution: $targetResolution"
        ).that(resolution).isEqualTo(targetResolution)
    }

    private fun verifyRecordingResult(file: File, hasAudio: Boolean = false) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, Uri.fromFile(file))
            val video = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val audio = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)

            assertThat(video).isEqualTo("yes")
            assertThat(audio).isEqualTo(if (hasAudio) "yes" else null)
        }
    }

    private fun getRotationInMetadata(uri: Uri): Int {
        val mediaRetriever = MediaMetadataRetriever()
        return mediaRetriever.let {
            it.setDataSource(context, uri)
            it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt()!!
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
}