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
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_3_4
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3
import androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_9_16
import androidx.camera.core.impl.utils.TransformUtils.is90or270
import androidx.camera.core.impl.utils.TransformUtils.rectToSize
import androidx.camera.core.impl.utils.TransformUtils.rotateSize
import androidx.camera.core.impl.utils.TransformUtils.within360
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.AndroidUtil.skipVideoRecordingTestIfNotSupportedByEmulator
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraTaskTrackingExecutor
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.getRotatedAspectRatio
import androidx.camera.testing.impl.getRotation
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.Recording
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runners.Parameterized

abstract class VideoRecordingTestBase(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName.contains(CameraPipeConfig::class.simpleName!!),
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "back+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    Camera2Config.defaultConfig(),
                ),
                arrayOf(
                    "front+" + Camera2Config::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    Camera2Config.defaultConfig(),
                ),
                arrayOf(
                    "back+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                ),
                arrayOf(
                    "front+" + CameraPipeConfig::class.simpleName,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    CameraPipeConfig.defaultConfig(),
                ),
            )
        }
    }

    protected abstract val testTag: String
    protected abstract val enableStreamSharing: Boolean

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    // TODO(b/278168212): Only SDR is checked by now. Need to extend to HDR dynamic ranges.
    private val dynamicRange = DynamicRange.SDR
    private lateinit var cameraProvider: ProcessCameraProviderWrapper
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var preview: Preview
    private lateinit var cameraInfo: CameraInfo
    private lateinit var videoCapabilities: VideoCapabilities
    private lateinit var camera: Camera
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var recordingSession: RecordingSession
    private lateinit var cameraExecutor: CameraTaskTrackingExecutor

    private val oppositeCameraSelector: CameraSelector by lazy {
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else CameraSelector.DEFAULT_BACK_CAMERA
    }

    private val oppositeCamera: Camera by lazy {
        lateinit var camera: Camera
        instrumentation.runOnMainSync {
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, oppositeCameraSelector)
        }
        camera
    }

    private val audioStreamAvailable by lazy {
        AudioChecker.canAudioStreamBeStarted(videoCapabilities, Recorder.DEFAULT_QUALITY_SELECTOR)
    }

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))
        skipVideoRecordingTestIfNotSupportedByEmulator()

        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        cameraExecutor = CameraTaskTrackingExecutor()
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(cameraConfig).setCameraExecutor(cameraExecutor).build()

        ProcessCameraProvider.configureInstance(cameraXConfig)

        cameraProvider =
            ProcessCameraProviderWrapper(
                ProcessCameraProvider.getInstance(context).get(),
                enableStreamSharing
            )
        lifecycleOwner = FakeLifecycleOwner()
        lifecycleOwner.startAndResume()

        // Add extra Preview to provide an additional surface for b/168187087.
        preview = Preview.Builder().build()
        videoCapture = VideoCapture.withOutput(Recorder.Builder().build())

        instrumentation.runOnMainSync {
            // Sets surface provider to preview
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()

            // Retrieves the target testing camera and camera info
            camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            cameraInfo = camera.cameraInfo
            videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        }

        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
                    recorder = videoCapture.output,
                    outputOptionsProvider = {
                        FileOutputOptions.Builder(temporaryFolder.newFile()).build()
                    },
                    withAudio = audioStreamAvailable,
                )
            )
    }

    @After
    fun tearDown() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(timeoutMs = 5000)
        }
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

        checkAndBindUseCases(preview, videoCapture)

        // Act.
        val result1 = recordingSession.createRecording().recordAndVerify()

        // Verify.
        val (videoContentRotation, metadataRotation) = getExpectedRotation(videoCapture, cameraInfo)
        verifyMetadataRotation(metadataRotation, result1.file)

        // Arrange: Prepare for 2nd recording
        // Act: Update targetRotation.
        videoCapture.targetRotation = targetRotation2
        val result2 = recordingSession.createRecording().recordAndVerify()

        // Verify.
        val metadataRotation2 =
            cameraInfo.getSensorRotationDegrees(targetRotation2).let {
                if (isSurfaceProcessingEnabled(videoCapture)) {
                    // If effect is enabled, the rotation should eliminate the video content
                    // rotation.
                    within360(it - videoContentRotation)
                } else it
            }
        verifyMetadataRotation(metadataRotation2, result2.file)
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
        val recorder = Recorder.Builder().setAspectRatio(aspectRatio).build()
        val videoCapture = VideoCapture.withOutput(recorder)

        checkAndBindUseCases(preview, videoCapture)

        // Act.
        val result =
            recordingSession.createRecording(recorder = videoCapture.output).recordAndVerify()

        // Verify.
        verifyVideoAspectRatio(
            getRotatedAspectRatio(aspectRatio, getRotationNeeded(videoCapture, cameraInfo)),
            result.file
        )
    }

    @Test
    fun getCorrectResolution_when_setCropRect() {
        // In stream sharing (VirtualCameraAdapter), children's ViewPortCropRect is ignored and
        // override to the parent size, the cropRect is also rotated. Skip the test.
        assumeFalse(enableStreamSharing)

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

        checkAndBindUseCases(preview, videoCapture)
        val calculatedCropRect = videoCapture.cropRect!!

        // Act.
        val result = recordingSession.createRecording(recorder = recorder).recordAndVerify()

        // Verify.
        val resolution = rectToSize(calculatedCropRect)
        verifyVideoResolution(
            context,
            result.file,
            rotateSize(resolution, getRotationNeeded(videoCapture, cameraInfo))
        )
    }

    @Test
    fun getResolutionInfo_shouldMatchRecordedVideoResolution() {
        // Arrange.
        checkAndBindUseCases(preview, videoCapture)
        val resolutionInfo = videoCapture.resolutionInfo!!

        // Act.
        val result = recordingSession.createRecording().recordAndVerify()

        // Assert: the resolution of the video file should match the resolution calculated by
        // rotating the cropRect specified in the ResolutionInfo.
        val expectedResolution =
            rotateSize(rectToSize(resolutionInfo.cropRect), resolutionInfo.rotationDegrees)
        verifyVideoResolution(context, result.file, expectedResolution)
    }

    @Test
    fun stopRecording_when_useCaseUnbind() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        checkAndBindUseCases(preview, videoCapture)

        // Act.
        val recording = recordingSession.createRecording().startAndVerify()
        instrumentation.runOnMainSync { cameraProvider.unbind(videoCapture) }

        // Verify.
        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)
    }

    @Test
    fun stopRecording_when_lifecycleStops() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        checkAndBindUseCases(preview, videoCapture)

        // Act.
        val recording = recordingSession.createRecording().startAndVerify()
        instrumentation.runOnMainSync { lifecycleOwner.pauseAndStop() }

        // Verify.
        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)
    }

    @Test
    fun start_finalizeImmediatelyWhenSourceInactive() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        checkAndBindUseCases(preview, videoCapture)

        // Act: Ensure the Recorder is initialized before start test.
        recordingSession.createRecording().startAndVerify().stop()

        lateinit var recording: Recording
        instrumentation.runOnMainSync {
            lifecycleOwner.pauseAndStop()

            // TODO(b/353578694): call start() in main thread to workaround the race condition.
            recording = recordingSession.createRecording().start()
        }

        // Verify.
        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)
    }

    @Test
    fun recordingWithPreview_boundSeparately() {
        assumeTrue(camera.isUseCasesCombinationSupported(preview, videoCapture))

        // Act: Intentionally bind the preview and videoCapture separately.
        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, videoCapture)
        }

        // Verify.
        recordingSession.createRecording().recordAndVerify()
    }

    @Test
    fun recordingWhenSessionErrorListenerReceivesError() {
        checkAndBindUseCases(preview, videoCapture)

        // Verifies recording before triggering onError event
        recordingSession.createRecording().recordAndVerify()

        // Retrieves the initial session config
        var sessionConfig = videoCapture.sessionConfig

        // Checks that video can be recorded successfully when onError is received.
        triggerOnErrorAndWaitForReady(
            sessionConfig,
            videoCapture.output.mVideoEncoderSession.readyToReleaseFuture
        )
        // Verifies recording after triggering onError event
        recordingSession.createRecording().recordAndVerify()

        // Rebinds to different camera
        if (CameraUtil.hasCameraWithLensFacing(oppositeCameraSelector.lensFacing!!)) {
            instrumentation.runOnMainSync { cameraProvider.unbindAll() }
            checkAndBindUseCases(preview, videoCapture, useOppositeCamera = true)

            // Verifies recording after binding to different camera
            recordingSession.createRecording().recordAndVerify()

            // Checks that video can be recorded successfully when onError is received by the
            // old error listener.
            triggerOnErrorAndWaitForReady(sessionConfig)

            // Verifies recording after triggering onError event to the closed error listener
            recordingSession.createRecording().recordAndVerify()
        }

        // Update the session config
        sessionConfig = videoCapture.sessionConfig

        // Checks that image can be received successfully when onError is received by the new
        // error listener.
        triggerOnErrorAndWaitForReady(
            sessionConfig,
            videoCapture.output.mVideoEncoderSession.readyToReleaseFuture
        )
        // Verifies recording after triggering onError event to the new active error listener
        recordingSession.createRecording().recordAndVerify()
    }

    @Test
    fun canRecordMultipleFilesInARow() {
        checkAndBindUseCases(preview, videoCapture)
        recordingSession.createRecording().recordAndVerify()
        recordingSession.createRecording().recordAndVerify()
        recordingSession.createRecording().recordAndVerify()
    }

    @Test
    fun canRecordMultipleFilesWithThenWithoutAudio() {
        // This test requires that audio is available
        assumeTrue("Audio stream is not available", audioStreamAvailable)
        checkAndBindUseCases(preview, videoCapture)

        recordingSession.createRecording(withAudio = true).recordAndVerify()
        recordingSession.createRecording(withAudio = false).recordAndVerify()
    }

    @Test
    fun canRecordMultipleFilesWithoutThenWithAudio() {
        // This test requires that audio is available
        assumeTrue(audioStreamAvailable)
        checkAndBindUseCases(preview, videoCapture)

        recordingSession.createRecording(withAudio = false).recordAndVerify()
        recordingSession.createRecording(withAudio = true).recordAndVerify()
    }

    @Test
    fun canStartNextRecordingPausedAfterFirstRecordingFinalized() {
        checkAndBindUseCases(preview, videoCapture)

        // Start and stop a recording to ensure recorder is idling
        recordingSession.createRecording().recordAndVerify()

        // First recording is now finalized. Try starting second recording paused.
        recordingSession
            .createRecording()
            .start()
            .pauseAndVerify()
            // Immediate pause may cause no frame received, ignore the result code.
            .stopAndVerify(error = null)
    }

    @Test
    fun canSwitchAudioOnOff() {
        assumeTrue("Audio stream is not available", audioStreamAvailable)
        checkAndBindUseCases(preview, videoCapture)

        // Record the first video with audio enabled.
        recordingSession.createRecording(withAudio = true).recordAndVerify()

        // Record the second video with audio disabled.
        val recording = recordingSession.createRecording(withAudio = false).startAndVerify()
        val status = recording.getStatusEvents().first()
        assertThat(status.recordingStats.audioStats.audioState)
            .isEqualTo(AudioStats.AUDIO_STATE_DISABLED)
        recording.stopAndVerify()

        // Record the third video with audio enabled.
        recordingSession.createRecording(withAudio = true).recordAndVerify()
    }

    @Test
    fun canReuseRecorder_explicitlyStop() {
        val recorder = Recorder.Builder().build()
        val videoCapture1 = VideoCapture.withOutput(recorder)
        val videoCapture2 = VideoCapture.withOutput(recorder)

        checkAndBindUseCases(preview, videoCapture1)

        recordingSession.createRecording(recorder = recorder).recordAndVerify()

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        checkAndBindUseCases(preview, videoCapture2)

        recordingSession.createRecording(recorder = recorder).recordAndVerify()
    }

    @Test
    fun canReuseRecorder_sourceInactive() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        val recorder = Recorder.Builder().build()
        val videoCapture1 = VideoCapture.withOutput(recorder)

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture1)
        }

        val recording = recordingSession.createRecording(recorder = recorder)
        recording.startAndVerify()

        // Unbind use case should stop the in-progress recording.
        instrumentation.runOnMainSync { cameraProvider.unbindAll() }

        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)

        val videoCapture2 = VideoCapture.withOutput(recorder)

        checkAndBindUseCases(preview, videoCapture2)

        recordingSession.createRecording(recorder = recorder).recordAndVerify()
    }

    @Test
    fun mute_defaultToNotMuted() {
        assumeTrue("Audio stream is not available", audioStreamAvailable)

        // Arrange.
        checkAndBindUseCases(preview, videoCapture)

        recordingSession
            .createRecording(withAudio = true)
            .startAndVerify()
            // Keep the first recording muted.
            .mute(true)
            .stopAndVerify()

        val recording = recordingSession.createRecording(withAudio = true).startAndVerify()
        val status = recording.getStatusEvents().first()

        // Assert: The second recording should not be muted.
        assertThat(status.recordingStats.audioStats.audioState)
            .isEqualTo(AudioStats.AUDIO_STATE_ACTIVE)

        recording.stopAndVerify()
    }

    @Test
    fun canRecordWithCorrectTransformation() {
        // Act.
        checkAndBindUseCases(preview, videoCapture)
        val result1 = recordingSession.createRecording().recordAndVerify()

        // Assert.
        verifyMetadataRotation(
            getExpectedRotation(videoCapture, camera.cameraInfo).metadataRotation,
            result1.file
        )

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        checkAndBindUseCases(preview, videoCapture, useOppositeCamera = true)

        val result2 = recordingSession.createRecording().recordAndVerify()

        // Assert.
        verifyMetadataRotation(
            getExpectedRotation(videoCapture, oppositeCamera.cameraInfo).metadataRotation,
            result2.file
        )
    }

    @Test
    fun updateVideoUsage_whenRecordingStartedPausedResumedStopped(): Unit = runBlocking {
        checkAndBindUseCases(videoCapture, preview)

        // Act 1 - isRecording is true after start.
        val recording = recordingSession.createRecording().startAndVerify()
        camera.cameraControl.verifyIfInVideoUsage(
            true,
            "Video started but camera still not in video usage"
        )

        // Act 2 - isRecording is false after pause.
        recording.pauseAndVerify()
        camera.cameraControl.verifyIfInVideoUsage(
            false,
            "Video paused but camera still in video usage"
        )

        // Act 3 - isRecording is true after resume.
        recording.resumeAndVerify()
        camera.cameraControl.verifyIfInVideoUsage(
            true,
            "Video resumed but camera still not in video usage"
        )

        // Act 4 - isRecording is false after stop.
        recording.stopAndVerify()
        camera.cameraControl.verifyIfInVideoUsage(
            false,
            "Video stopped but camera still in video usage"
        )
    }

    @Test
    fun updateVideoUsage_whenUnboundBeforeCompletingAndStartNewAfterRebind(): Unit = runBlocking {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        checkAndBindUseCases(preview, videoCapture)
        val recording1 = recordingSession.createRecording().startAndVerify()

        // Act 1 - unbind before recording completes and check if isRecording is false.
        instrumentation.runOnMainSync { cameraProvider.unbind(videoCapture) }

        camera.cameraControl.verifyIfInVideoUsage(
            false,
            "Video stopped but camera still in video usage"
        )

        // Cleanup.
        recording1.verifyFinalize(error = ERROR_SOURCE_INACTIVE)

        // Act 2 - rebind and start new recording, check if isRecording is true now.
        checkAndBindUseCases(preview, videoCapture)

        recordingSession.createRecording().startAndVerify()
        camera.cameraControl.verifyIfInVideoUsage(
            true,
            "Video started but camera still not in video usage"
        )
    }

    @Test
    fun updateVideoUsage_whenLifecycleStoppedBeforeCompletingRecording(): Unit = runBlocking {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        checkAndBindUseCases(preview, videoCapture)
        recordingSession.createRecording().startAndVerify()

        // Act.
        instrumentation.runOnMainSync { lifecycleOwner.pauseAndStop() }

        camera.cameraControl.verifyIfInVideoUsage(
            false,
            "Lifecycle stopped but camera still in video usage"
        )
    }

    // TODO: b/341691683 - Add tests for multiple VideoCapture bound and recording concurrently

    private fun getCameraSelector(useOppositeCamera: Boolean): CameraSelector =
        if (!useOppositeCamera) cameraSelector else oppositeCameraSelector

    private fun getCamera(useOppositeCamera: Boolean): Camera =
        if (!useOppositeCamera) camera else oppositeCamera

    private fun isUseCasesCombinationSupported(
        vararg useCases: UseCase,
        withStreamSharing: Boolean,
        useOppositeCamera: Boolean = false
    ) = getCamera(useOppositeCamera).isUseCasesCombinationSupported(withStreamSharing, *useCases)

    /** Checks use case combination with considering StreamSharing and then binds to lifecycle. */
    private fun checkAndBindUseCases(
        vararg useCases: UseCase,
        withStreamSharing: Boolean = enableStreamSharing,
        useOppositeCamera: Boolean = false,
    ) {
        assumeTrue(
            isUseCasesCombinationSupported(
                *useCases,
                withStreamSharing = withStreamSharing,
                useOppositeCamera = useOppositeCamera
            )
        )

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                getCameraSelector(useOppositeCamera),
                *useCases
            )
        }
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
                    "Rotation test failure: " +
                        "videoRotation: $videoRotation" +
                        ", expectedRotation: $expectedRotation"
                )
                .that(videoRotation)
                .isEqualTo(expectedRotation)
        }
    }

    private fun verifyVideoAspectRatio(expectedAspectRatio: Rational, file: File) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, Uri.fromFile(file))
            val aspectRatio = it.getRotatedAspectRatio()

            assertWithMessage(
                    "VerifyVideoAspectRatio failure:" +
                        ", videoAspectRatio: $aspectRatio" +
                        ", expectedAspectRatio: $expectedAspectRatio"
                )
                .that(aspectRatio.toDouble())
                .isWithin(0.1)
                .of(expectedAspectRatio.toDouble())
        }
    }

    private fun assumeExtraCroppingQuirk() {
        assumeExtraCroppingQuirk(implName)
    }

    private suspend fun CameraControl.verifyIfInVideoUsage(
        expected: Boolean,
        message: String = ""
    ) {
        instrumentation.waitForIdleSync() // VideoCapture observes Recorder in main thread
        // VideoUsage is updated in camera thread. So, we should ensure all tasks already submitted
        // to camera thread are completed before checking isInVideoUsage
        cameraExecutor.awaitIdle()
        assertWithMessage(message).that((this as CameraControlInternal).isInVideoUsage).apply {
            if (expected) {
                isTrue()
            } else {
                isFalse()
            }
        }
    }

    /**
     * Triggers the onError to the error listener in the session config
     *
     * If the test starts recording immediately after `onError` is called. There could be a timing
     * issue which causes the recording to be stopped. In that case, input the VideoEncoderSession's
     * readyToReleaseFuture. This function will wait for the ready-to-release future to be
     * completed. This can make sure that the following recording operation won't be interrupted
     * when the previous DeferrableSurface is closed.
     */
    private fun triggerOnErrorAndWaitForReady(
        sessionConfig: SessionConfig,
        readyFuture: ListenableFuture<*>? = null
    ) {
        instrumentation.runOnMainSync {
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN
            )
        }

        // If the test starts recording immediately after `onError` is called. There could be a
        // timing issue which causes the recording to be stopped.
        // On the main thread: trigger OnError
        //    -> resetPipeline
        //    -> DeferrableSurface is closed
        //    -> SurfaceRequest is complete
        // On the test thread: start recording
        // On the Recorder mSequentialExecutor:
        //    The listener of readyToReleaseFuture executes due to SurfaceRequest is complete
        //    -> Recorder.requestReset()
        //    -> recording is stopped unexpectedly.
        readyFuture?.get(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)
    }
}
