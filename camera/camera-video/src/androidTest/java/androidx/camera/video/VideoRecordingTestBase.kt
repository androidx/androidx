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
import android.app.AppOpsManager
import android.app.AppOpsManager.OnOpNotedCallback
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Rect
import android.location.Location
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Rational
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
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
import androidx.camera.testing.impl.CameraTaskTrackingExecutor
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.getLocation
import androidx.camera.testing.impl.getMimeType
import androidx.camera.testing.impl.getRotatedAspectRatio
import androidx.camera.testing.impl.getRotation
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.testing.impl.video.AudioChecker
import androidx.camera.testing.impl.video.Recording
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import androidx.camera.video.internal.muxer.MediaMuxerImpl
import androidx.camera.video.internal.muxer.MuxerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestRule
import org.junit.runners.Parameterized
import org.junit.runners.model.Statement

private const val TEST_ATTRIBUTION_TAG = "testAttribution"

/**
 * Annotates a test to run only on the first available camera (skipping secondary cameras).
 *
 * Use this annotation for camera-agnostic tests whose behavior does not depend on specific camera
 * lens characteristics (such as sensor orientation, aspect ratio, crop rect, mirror mode, or
 * per-camera capabilities), or tests that already switch between cameras internally.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FirstAvailableCameraOnly

/**
 * Annotates a test to be skipped when StreamSharing is enabled (e.g., in
 * [VideoRecordingStreamSharingTest]).
 *
 * Use this annotation for tests where StreamSharing is unsupported (e.g., custom viewport crop
 * rect) or irrelevant (e.g., pure [Recorder] output options, muxer configuration, audio recording,
 * or camera capability queries).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class IgnoreStreamSharing

abstract class VideoRecordingTestBase(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig,
) {

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
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    private val filterRule = TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                if (description.getAnnotation(IgnoreStreamSharing::class.java) != null) {
                    assumeFalse(
                        "Skipped when StreamSharing is enabled",
                        enableStreamSharing,
                    )
                }
                if (description.getAnnotation(FirstAvailableCameraOnly::class.java) != null) {
                    assumeTrue(
                        "Skipped for non-primary camera",
                        cameraSelector.lensFacing ==
                            CameraUtil.assumeFirstAvailableCameraSelector().lensFacing,
                    )
                }
                base.evaluate()
            }
        }
    }

    // Chain rule to not run WakelockEmptyActivityRule when the test is ignored.
    @get:Rule
    val skipAndWakelockRule: TestRule =
        RuleChain.outerRule(IgnoreVideoRecordingProblematicDeviceRule())
            .around(filterRule)
            .around(WakelockEmptyActivityRule())

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> =
            CameraUtil.getAvailableCameraSelectors().map { selector ->
                val lensName =
                    when (selector.lensFacing) {
                        CameraSelector.LENS_FACING_BACK -> "back"
                        CameraSelector.LENS_FACING_FRONT -> "front"
                        CameraSelector.LENS_FACING_EXTERNAL -> "external"
                        else -> "unknown"
                    }
                arrayOf(
                    "$lensName+${Camera2Config::class.simpleName}",
                    selector,
                    Camera2Config.defaultConfig(),
                )
            }
    }

    protected abstract val testTag: String
    protected abstract val enableStreamSharing: Boolean

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultDynamicRange = SDR
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

    private val audioStreamAvailable by lazy { AudioChecker.canAudioStreamBeStarted() }

    @Before
    fun setUp() {
        assumeFalse(
            "Test fails on cuttlefish b/467136521",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )

        assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        cameraExecutor = CameraTaskTrackingExecutor()
        val cameraXConfig =
            CameraXConfig.Builder.fromConfig(cameraConfig).setCameraExecutor(cameraExecutor).build()

        ProcessCameraProvider.configureInstance(cameraXConfig)

        cameraProvider =
            ProcessCameraProviderWrapper(
                ProcessCameraProvider.getInstance(context).get(),
                enableStreamSharing,
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
                    outputOptionsProvider = { createFileOutputOptions() },
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
        assumeTrue(videoCapabilities.getSupportedQualities(defaultDynamicRange).isNotEmpty())

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
            result.file,
        )
    }

    @IgnoreStreamSharing
    @Test
    fun getCorrectResolution_when_setCropRect() {
        assumeSuccessfulSurfaceProcessing()
        assumeExtraCroppingQuirk()

        // Arrange.
        assumeTrue(videoCapabilities.getSupportedQualities(defaultDynamicRange).isNotEmpty())
        val quality = Quality.LOWEST
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(quality)).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        // Arbitrary cropping
        val targetResolution = videoCapabilities.getResolution(quality, defaultDynamicRange)!!
        val cropRect = Rect(6, 6, targetResolution.width - 7, targetResolution.height - 7)
        videoCapture.setViewPortCropRect(cropRect)

        checkAndBindUseCases(preview, videoCapture)
        // On some devices stream sharing could be forcibly enabled by workaround
        // StreamSharingForceEnabler.java such as on moto-e20. Check stream sharing after binding
        // and skip it by the same reason above.
        assumeFalse(isStreamSharingEnabled(videoCapture))
        val calculatedCropRect = videoCapture.cropRect!!

        // Act.
        val result = recordingSession.createRecording(recorder = recorder).recordAndVerify()

        // Verify.
        val resolution = rectToSize(calculatedCropRect)
        verifyVideoResolution(
            context,
            result.file,
            rotateSize(resolution, getRotationNeeded(videoCapture, cameraInfo)),
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

    @FirstAvailableCameraOnly
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

    @FirstAvailableCameraOnly
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

    @FirstAvailableCameraOnly
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

    @FirstAvailableCameraOnly
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

    @FirstAvailableCameraOnly
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
            videoCapture.output.mVideoEncoderSession.readyToReleaseFuture,
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
            videoCapture.output.mVideoEncoderSession.readyToReleaseFuture,
        )
        // Verifies recording after triggering onError event to the new active error listener
        recordingSession.createRecording().recordAndVerify()
    }

    @FirstAvailableCameraOnly
    @Test
    fun canRecordMultipleFilesInARow() {
        checkAndBindUseCases(preview, videoCapture)
        recordingSession.createRecording().recordAndVerify()
        recordingSession.createRecording().recordAndVerify()
        recordingSession.createRecording().recordAndVerify()
    }

    @FirstAvailableCameraOnly
    @SdkSuppress(minSdkVersion = 33)
    @Test
    fun canRecordMultipleFilesInARow_whenHdr() {
        val recorder = Recorder.Builder().build()
        val highDynamicRanges = videoCapabilities.supportedDynamicRanges.filter { it != SDR }
        assumeTrue(highDynamicRanges.isNotEmpty())

        highDynamicRanges.forEach { dynamicRange ->
            assumeTrue(videoCapabilities.getSupportedQualities(dynamicRange).isNotEmpty())

            val videoCapture = VideoCapture.Builder(recorder).setDynamicRange(dynamicRange).build()
            checkAndBindUseCases(preview, videoCapture)
            recordingSession.createRecording(recorder = recorder).recordAndVerify()
            recordingSession.createRecording(recorder = recorder).recordAndVerify()
            recordingSession.createRecording(recorder = recorder).recordAndVerify()
        }
    }

    @FirstAvailableCameraOnly
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

    @FirstAvailableCameraOnly
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

    @FirstAvailableCameraOnly
    @Test
    fun canRecordWithCorrectTransformation() {
        assumeTrue(
            "No OppositeCamera for test.",
            CameraUtil.hasCameraWithLensFacing(oppositeCameraSelector.lensFacing!!),
        )

        // Act.
        checkAndBindUseCases(preview, videoCapture)
        val result1 = recordingSession.createRecording().recordAndVerify()

        // Assert.
        verifyMetadataRotation(
            getExpectedRotation(videoCapture, camera.cameraInfo).metadataRotation,
            result1.file,
        )

        instrumentation.runOnMainSync { cameraProvider.unbindAll() }
        checkAndBindUseCases(preview, videoCapture, useOppositeCamera = true)

        val result2 = recordingSession.createRecording().recordAndVerify()

        // Assert.
        verifyMetadataRotation(
            getExpectedRotation(videoCapture, oppositeCamera.cameraInfo).metadataRotation,
            result2.file,
        )
    }

    @Test
    fun togglingMirrorModeDuringRecordingDoesNotInterruptRecording() {
        // Set initial mode to OFF
        val preview = Preview.Builder().setMirrorMode(MirrorMode.MIRROR_MODE_OFF).build()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = SurfaceTextureProvider.createSurfaceTextureProvider()
        }
        val videoCapture =
            VideoCapture.Builder(Recorder.Builder().build())
                .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
                .build()

        checkAndBindUseCases(preview, videoCapture)

        val recording =
            recordingSession.createRecording(recorder = videoCapture.output).startAndVerify()

        instrumentation.runOnMainSync {
            // From OFF to ON with set order: videoCapture, preview
            val newMode = MirrorMode.MIRROR_MODE_ON
            videoCapture.mirrorMode = newMode
            preview.setMirrorMode(newMode)
        }

        recording.clearEvents()
        recording.verifyStatus(statusCount = 15)

        instrumentation.runOnMainSync {
            // From ON to FRONT_ONLY with reversed set order: videoCapture, preview
            val newMode = MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
            videoCapture.mirrorMode = newMode
            preview.setMirrorMode(newMode)
        }

        recording.clearEvents()
        recording.verifyStatus(statusCount = 15)
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    fun canRecordToFile() {
        // Arrange.
        checkAndBindUseCases(preview, videoCapture)
        val outputOptions = createFileOutputOptions()

        // Act & Assert.
        recordingSession.createRecording(outputOptions = outputOptions).recordAndVerify()
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    fun canRecordToNonExistFile() {
        // Arrange.
        checkAndBindUseCases(preview, videoCapture)
        val outputOptions = createFileOutputOptions(createTempFile().apply { delete() })

        // Act & Assert.
        recordingSession.createRecording(outputOptions = outputOptions).recordAndVerify()
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    fun canRecordToMediaStore() {
        assumeTrue(
            "Ignore the test since the MediaStore.Video has compatibility issues.",
            DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null,
        )
        checkAndBindUseCases(preview, videoCapture)

        // Arrange.
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues =
            ContentValues().apply { put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4") }
        val outputOptions =
            MediaStoreOutputOptions.Builder(
                    contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                )
                .setContentValues(contentValues)
                .build()

        // Act & Assert.
        val result =
            recordingSession.createRecording(outputOptions = outputOptions).recordAndVerify()

        // Clean-up.
        contentResolver.delete(result.uri, null, null)
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun canRecordToFileDescriptor() {
        // Arrange.
        checkAndBindUseCases(preview, videoCapture)
        val file = createTempFile()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        val outputOptions = FileDescriptorOutputOptions.Builder(pfd).build()
        val recording = recordingSession.createRecording(outputOptions = outputOptions)

        // Act.
        recording.startAndVerify()
        // ParcelFileDescriptor should be safe to close after PendingRecording#start.
        pfd.close()

        // Assert.
        recording.stopAndVerify()
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    fun muxerFactory_mediaMuxerImpl_recordsSuccessfully() {
        // Arrange.
        val muxerFactory = MuxerFactory { MediaMuxerImpl() }
        val recorder = Recorder.Builder().setMuxerFactory(muxerFactory).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        checkAndBindUseCases(preview, videoCapture)

        // Act & Assert.
        recordingSession.createRecording(recorder = recorder).recordAndVerify()
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun recordToFileDescriptor_withClosedFileDescriptor_receiveError() {
        // Arrange.
        checkAndBindUseCases(preview, videoCapture)
        val file = createTempFile()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        pfd.close()
        val outputOptions = FileDescriptorOutputOptions.Builder(pfd).build()
        val recording = recordingSession.createRecording(outputOptions = outputOptions)

        // Act.
        recording.start()

        // Assert.
        recording.stopAndVerify(error = ERROR_INVALID_OUTPUT_OPTIONS)
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    fun setLocation() {
        checkAndBindUseCases(preview, videoCapture)
        runLocationTest(createLocation(25.033267462243586, 121.56454121737946))

        // set negative location
        runLocationTest(createLocation(-27.14394722411734, -109.33053675296067))
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    fun mute_outputWithAudioTrack() {
        assumeTrue("Audio stream is not available", audioStreamAvailable)
        checkAndBindUseCases(preview, videoCapture)

        // Arrange.
        val outputOptions = createFileOutputOptions()
        val recording =
            recordingSession.createRecording(
                outputOptions = outputOptions,
                initialAudioMuted = true,
            )

        // The output file should contain audio track even it's muted at the beginning.
        recording.recordAndVerify()
    }

    @FirstAvailableCameraOnly
    @IgnoreStreamSharing
    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun audioRecordIsAttributed() = runBlocking {
        assumeTrue("Audio stream is not available", audioStreamAvailable)
        checkAndBindUseCases(preview, videoCapture)

        // Arrange.
        val notedTag = CompletableDeferred<String>()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appOps.setOnOpNotedCallback(
            Dispatchers.Main.asExecutor(),
            object : OnOpNotedCallback() {
                override fun onNoted(p0: SyncNotedAppOp) {
                    // no-op. record_audio should be async.
                }

                override fun onSelfNoted(p0: SyncNotedAppOp) {
                    // no-op. record_audio should be async.
                }

                override fun onAsyncNoted(noted: AsyncNotedAppOp) {
                    if (
                        AppOpsManager.OPSTR_RECORD_AUDIO == noted.op &&
                            TEST_ATTRIBUTION_TAG == noted.attributionTag
                    ) {
                        notedTag.complete(noted.attributionTag!!)
                    }
                }
            },
        )
        val attributionContext = context.createAttributionContext(TEST_ATTRIBUTION_TAG)
        val recording = recordingSession.createRecording(context = attributionContext)

        // Act.
        recording.start()
        try {
            val timeoutDuration = 5.seconds
            withTimeoutOrNull(timeoutDuration) {
                // Assert.
                assertThat(notedTag.await()).isEqualTo(TEST_ATTRIBUTION_TAG)
            } ?: fail("Timed out waiting for attribution tag. Waited $timeoutDuration.")
        } finally {
            appOps.setOnOpNotedCallback(null, null)
            recording.stop()
        }
    }

    @IgnoreStreamSharing
    @Test
    fun getVideoCapabilities_supportStandardDynamicRange() {
        assumeFalse(isDeviceWithCamcorderProfileResolutionMismatch())

        assertThat(videoCapabilities.supportedDynamicRanges).contains(SDR)
    }

    @IgnoreStreamSharing
    @Test
    fun getVideoCapabilities_supportedQualitiesOfSdrIsNotEmpty() {
        assumeFalse(isDeviceWithCamcorderProfileResolutionMismatch())

        assertThat(videoCapabilities.getSupportedQualities(SDR)).isNotEmpty()
    }

    @IgnoreStreamSharing
    @Test
    fun getVideoCapabilities_withMimeType_returnsCapabilities() {
        val capabilities = Recorder.getVideoCapabilities(camera.cameraInfo, MIMETYPE_VIDEO_AVC)

        assertThat(capabilities).isNotNull()
        // We expect at least SDR to be supported for AVC
        assertThat(capabilities!!.supportedDynamicRanges).contains(SDR)
    }

    private fun isDeviceWithCamcorderProfileResolutionMismatch(): Boolean {
        val isNokia2Point1 =
            "nokia".equals(Build.BRAND, true) && "nokia 2.1".equals(Build.MODEL, true)
        val isMotoE5Play =
            "motorola".equals(Build.BRAND, true) && "moto e5 play".equals(Build.MODEL, true)

        return isNokia2Point1 || isMotoE5Play
    }

    private fun createTempFile() = temporaryFolder.newFile()

    private fun createFileOutputOptions(
        file: File = createTempFile(),
        location: Location? = null,
    ): FileOutputOptions =
        FileOutputOptions.Builder(file).apply { location?.let { setLocation(it) } }.build()

    @Suppress("SameParameterValue")
    private fun checkLocation(uri: Uri, location: Location) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, uri)
            // Only test on mp4 output format, others will be ignored.
            val mime = it.getMimeType()
            assumeTrue("Unsupported mime = $mime", "video/mp4".equals(mime, ignoreCase = true))
            val value = it.getLocation()
            // ex: (90, 180) => "+90.0000+180.0000/" (ISO-6709 standard)
            val matchGroup =
                "([+-]?[0-9]+(\\.[0-9]+)?)([+-]?[0-9]+(\\.[0-9]+)?)".toRegex().find(value)
                    ?: fail("Fail on checking location metadata: $value")
            val lat = matchGroup.groupValues[1].toDouble()
            val lon = matchGroup.groupValues[3].toDouble()

            // MediaMuxer.setLocation rounds the value to 4 decimal places
            val tolerance = 0.0001
            assertWithMessage("Fail on latitude. $lat($value) vs ${location.latitude}")
                .that(lat)
                .isWithin(tolerance)
                .of(location.latitude)
            assertWithMessage("Fail on longitude. $lon($value) vs ${location.longitude}")
                .that(lon)
                .isWithin(tolerance)
                .of(location.longitude)
        }
    }

    private fun runLocationTest(location: Location) {
        // Arrange.
        val outputOptions = createFileOutputOptions(location = location)

        // Act.
        val result =
            recordingSession.createRecording(outputOptions = outputOptions).recordAndVerify()

        // Assert.
        checkLocation(result.uri, location)
    }

    private fun createLocation(
        latitude: Double,
        longitude: Double,
        provider: String = "FakeProvider",
    ): Location =
        Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
        }

    private fun getCameraSelector(useOppositeCamera: Boolean): CameraSelector =
        if (!useOppositeCamera) cameraSelector else oppositeCameraSelector

    private fun getCamera(useOppositeCamera: Boolean): Camera =
        if (!useOppositeCamera) camera else oppositeCamera

    private fun isUseCasesCombinationSupported(
        vararg useCases: UseCase,
        withStreamSharing: Boolean,
        useOppositeCamera: Boolean = false,
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
                useOppositeCamera = useOppositeCamera,
            )
        )

        instrumentation.runOnMainSync {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                getCameraSelector(useOppositeCamera),
                *useCases,
            )
        }
    }

    data class ExpectedRotation(val contentRotation: Int, val metadataRotation: Int)

    private fun getExpectedRotation(
        videoCapture: VideoCapture<Recorder>,
        cameraInfo: CameraInfo,
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
        readyFuture: ListenableFuture<*>? = null,
    ) {
        instrumentation.runOnMainSync {
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN,
            )
        }

        // If the test starts recording immediately after `onError` is called. There could be a
        // timing issue which causes the recording to be stopped.
        // On the main thread: trigger OnError
        //    -> updateConfigAndOutput
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
