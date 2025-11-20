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

package androidx.camera.view

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.MainThread
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.CoreAppTestUtil.ForegroundOccupiedError
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.LabTestRule.Companion.isInLabTest
import androidx.camera.testing.impl.fakes.FakeActivity
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.testrule.PreTestRule
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import androidx.camera.video.internal.compat.quirk.StopCodecAfterSurfaceRemovalCrashMediaServerQuirk
import androidx.camera.view.CameraController.IMAGE_ANALYSIS
import androidx.camera.view.CameraController.VIDEO_CAPTURE
import androidx.camera.view.video.AudioConfig
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class VideoCaptureDeviceTest(
    private val initialQuality: TargetQuality,
    private val nextQuality: TargetQuality,
    private val cameraSelector: CameraSelector,
    private val lensFacing: Int,
) {

    /**
     * The helper class to workaround the issue that "null" cannot be accepted as a parameter value
     * in Parameterized tests, ref: b/37086576
     */
    enum class TargetQuality {
        NOT_SPECIFIED,
        FHD,
        HD,
        HIGHEST,
        LOWEST,
        SD,
        UHD;

        fun getSelector(): QualitySelector {
            return when (this) {
                NOT_SPECIFIED -> toQualitySelector(null)
                FHD -> toQualitySelector(Quality.FHD)
                HD -> toQualitySelector(Quality.HD)
                HIGHEST -> toQualitySelector(Quality.HIGHEST)
                LOWEST -> toQualitySelector(Quality.LOWEST)
                SD -> toQualitySelector(Quality.SD)
                UHD -> toQualitySelector(Quality.UHD)
            }
        }

        private fun toQualitySelector(quality: Quality?): QualitySelector {
            return if (quality == null) {
                Recorder.DEFAULT_QUALITY_SELECTOR
            } else {
                QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality))
            }
        }
    }

    companion object {
        private const val VIDEO_TIMEOUT_SEC = 10L
        private const val VIDEO_RECORDING_COUNT_DOWN = 5
        private const val VIDEO_STARTED_COUNT_DOWN = 1
        private const val VIDEO_SAVED_COUNT_DOWN = 1
        private const val TAG = "VideoCaptureDeviceTest"

        @JvmStatic
        @BeforeClass
        @Throws(ForegroundOccupiedError::class)
        fun classSetUp() {
            CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "initialQuality={0}, nextQuality={1}, lensFacing={3}")
        fun data() =
            if (isInLabTest()) {
                mutableListOf<Array<Any?>>().apply {
                    CameraUtil.getAvailableCameraSelectors().forEach { selector ->
                        val lens = selector.lensFacing
                        add(arrayOf(TargetQuality.NOT_SPECIFIED, TargetQuality.FHD, selector, lens))
                        add(arrayOf(TargetQuality.FHD, TargetQuality.HD, selector, lens))
                        add(arrayOf(TargetQuality.HD, TargetQuality.HIGHEST, selector, lens))
                        add(arrayOf(TargetQuality.HIGHEST, TargetQuality.LOWEST, selector, lens))
                        add(arrayOf(TargetQuality.LOWEST, TargetQuality.SD, selector, lens))
                        add(arrayOf(TargetQuality.SD, TargetQuality.UHD, selector, lens))
                        add(arrayOf(TargetQuality.UHD, TargetQuality.NOT_SPECIFIED, selector, lens))
                    }
                }
            } else {
                // Return empty list since prepareDeviceUI will skip the test if not in the CameraX
                // lab environment.
                emptyList()
            }
    }

    @get:Rule(order = 0)
    val skipRule: TestRule =
        RuleChain.outerRule(IgnoreVideoRecordingProblematicDeviceRule())
            .around(PreTestRule { skipTestWithSurfaceProcessingOnCuttlefishApi30() })

    @get:Rule(order = 1)
    val cameraRule: TestRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule(order = 3)
    val activityRule: ActivityScenarioRule<FakeActivity> =
        ActivityScenarioRule(FakeActivity::class.java)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audioEnabled = AudioConfig.create(true)
    private val audioDisabled = AudioConfig.AUDIO_DISABLED
    private lateinit var previewView: PreviewView
    private lateinit var lifecycleOwner: FakeLifecycleOwner
    private lateinit var cameraController: LifecycleCameraController
    private lateinit var activeRecording: Recording
    private lateinit var latchForVideoStarted: CountDownLatch
    private lateinit var latchForVideoPaused: CountDownLatch
    private lateinit var latchForVideoResumed: CountDownLatch
    private lateinit var latchForVideoSaved: CountDownLatch
    private lateinit var latchForVideoRecording: CountDownLatch
    private lateinit var finalize: VideoRecordEvent.Finalize

    private val videoRecordEventListener =
        Consumer<VideoRecordEvent> {
            when (it) {
                is VideoRecordEvent.Start -> {
                    Log.d(TAG, "Recording start")
                    latchForVideoStarted.countDown()
                }
                is VideoRecordEvent.Finalize -> {
                    Log.d(TAG, "Recording finalize")
                    finalize = it
                    latchForVideoSaved.countDown()
                }
                is VideoRecordEvent.Status -> {
                    // Make sure the recording proceed for a while.
                    Log.d(TAG, "Recording Status")
                    latchForVideoRecording.countDown()
                }
                is VideoRecordEvent.Pause -> {
                    Log.d(TAG, "Recording Pause")
                    latchForVideoPaused.countDown()
                }
                is VideoRecordEvent.Resume -> {
                    Log.d(TAG, "Recording Resume")
                    latchForVideoResumed.countDown()
                }
                else -> {
                    throw IllegalStateException()
                }
            }
        }

    @Before
    fun setUp() {
        initialLifecycleOwner()
        initialPreviewView()
        initialController()
    }

    @After
    fun tearDown() {
        if (this::cameraController.isInitialized) {
            instrumentation.runOnMainSync { cameraController.shutDownForTests() }
        }
    }

    @Test
    fun canRecordToMediaStore() {
        if (Build.VERSION.SDK_INT == 28) return // b/264902324
        assumeTrue(
            "Ignore the test since the MediaStore.Video has compatibility issues.",
            DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null,
        )

        // Arrange.
        val resolver: ContentResolver = context.contentResolver
        val outputOptions = createMediaStoreOutputOptions(resolver)

        // Act.
        recordVideoCompletely(outputOptions, audioEnabled)

        // Verify.
        val uri = finalize.outputResults.outputUri
        assertThat(uri).isNotEqualTo(Uri.EMPTY)
        checkFileHasAudioAndVideo(uri)

        // Cleanup.
        resolver.delete(uri, null, null)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun canRecordToFileDescriptor() {
        // Arrange.
        val file = createTempFile()
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        val outputOptions = FileDescriptorOutputOptions.Builder(fileDescriptor).build()

        // Act.
        recordVideoCompletely(outputOptions, audioEnabled)

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)

        // Cleanup.
        fileDescriptor.close()
        file.delete()
    }

    @Test
    fun canRecordToFile() {
        // Arrange.
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoCompletely(outputOptions, audioEnabled)

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    fun canRecordToFile_withoutAudio_whenAudioDisabled() {
        if (Build.VERSION.SDK_INT == 28) return // b/264902324
        // Arrange.
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoCompletely(outputOptions, audioDisabled)

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileOnlyHasVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    fun canRecordToFile_whenLifecycleStops() {
        if (Build.VERSION.SDK_INT == 28) return // b/264902324
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoWithInterruptAction(outputOptions, audioEnabled) {
            instrumentation.runOnMainSync { lifecycleOwner.pauseAndStop() }
        }

        // Verify.
        assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    fun canRecordToFile_whenTargetQualityChanged() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoWithInterruptAction(outputOptions, audioEnabled) {
            instrumentation.runOnMainSync {
                cameraController.videoCaptureQualitySelector = nextQuality.getSelector()
            }
        }

        // Verify.
        assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    fun canRecordToFile_whenEnabledUseCasesChanged() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoWithInterruptAction(outputOptions, audioEnabled) {
            instrumentation.runOnMainSync { cameraController.setEnabledUseCases(IMAGE_ANALYSIS) }
        }

        // Verify.
        assertThat(finalize.hasError()).isFalse()
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    fun canRecordToFile_rightAfterPreviousRecordingStopped() {
        if (Build.VERSION.SDK_INT == 30) return // b/264902324
        // Arrange.
        val file1 = createTempFile()
        val file2 = createTempFile()
        val outputOptions1 = FileOutputOptions.Builder(file1).build()
        val outputOptions2 = FileOutputOptions.Builder(file2).build()

        // Pre Act.
        latchForVideoSaved = CountDownLatch(VIDEO_SAVED_COUNT_DOWN)
        recordVideo(outputOptions1, audioEnabled)
        instrumentation.runOnMainSync {
            activeRecording.stop()
            assertThat(cameraController.isRecording).isFalse()
        }
        latchForVideoStarted = CountDownLatch(VIDEO_STARTED_COUNT_DOWN)

        // Act.
        instrumentation.runOnMainSync {
            startRecording(outputOptions2, audioEnabled)
            assertThat(cameraController.isRecording).isTrue()
        }

        // Wait for the Finalize event of the previous recording.
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Reset latches and wait for Start and Status events
        latchForVideoRecording = CountDownLatch(VIDEO_RECORDING_COUNT_DOWN)
        latchForVideoSaved = CountDownLatch(VIDEO_SAVED_COUNT_DOWN)
        assertThat(latchForVideoStarted.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
        assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Stop the second recording and wait for the Finalize event
        instrumentation.runOnMainSync {
            activeRecording.stop()
            assertThat(cameraController.isRecording).isFalse()
        }
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Verify.
        assertThat(finalize.hasError()).isFalse()
        val uri1 = Uri.fromFile(file1)
        checkFileHasAudioAndVideo(uri1)
        val uri2 = Uri.fromFile(file2)
        checkFileHasAudioAndVideo(uri2)

        // Cleanup.
        file1.delete()
        file2.delete()
    }

    @Test
    fun canRecordToFile_whenPauseAndStop() {
        val pauseTimes = 1

        // Arrange.
        latchForVideoPaused = CountDownLatch(pauseTimes)
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoWithInterruptAction(outputOptions, audioEnabled) {
            instrumentation.runOnMainSync { activeRecording.pause() }
            assertThat(latchForVideoPaused.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

            instrumentation.runOnMainSync { activeRecording.stop() }
        }

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    fun canRecordToFile_whenPauseAndResumeInTheMiddle() {
        if (Build.VERSION.SDK_INT == 33 && Build.VERSION.CODENAME != "REL") {
            return // b/262909049: Do not run this test on pre-release Android U.
        }

        val pauseTimes = 1
        val resumeTimes = 1

        // Arrange.
        latchForVideoPaused = CountDownLatch(pauseTimes)
        latchForVideoResumed = CountDownLatch(resumeTimes)
        val file = createTempFile()
        val outputOptions = FileOutputOptions.Builder(file).build()

        // Act.
        recordVideoWithInterruptAction(outputOptions, audioEnabled) {
            instrumentation.runOnMainSync { activeRecording.pause() }
            assertThat(latchForVideoPaused.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

            instrumentation.runOnMainSync { activeRecording.resume() }
            assertThat(latchForVideoResumed.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

            instrumentation.runOnMainSync { activeRecording.stop() }
        }

        // Verify.
        val uri = Uri.fromFile(file)
        checkFileHasAudioAndVideo(uri)
        assertThat(finalize.outputResults.outputUri).isEqualTo(uri)

        // Cleanup.
        file.delete()
    }

    @Test
    fun startRecording_throwsExceptionWhenAlreadyInRecording() {
        // Arrange.
        val file1 = createTempFile()
        val file2 = createTempFile()
        val outputOptions1 = FileOutputOptions.Builder(file1).build()
        val outputOptions2 = FileOutputOptions.Builder(file2).build()

        // Act.
        recordVideoWithInterruptAction(outputOptions1, audioEnabled) {
            instrumentation.runOnMainSync {
                assertThrows(java.lang.IllegalStateException::class.java) {
                    activeRecording =
                        cameraController.startRecording(
                            outputOptions2,
                            audioEnabled,
                            CameraXExecutors.directExecutor(),
                        ) {}
                }
                activeRecording.stop()
            }
        }

        // Cleanup.
        file1.delete()
        file2.delete()
    }

    private fun initialLifecycleOwner() {
        instrumentation.runOnMainSync {
            lifecycleOwner = FakeLifecycleOwner()
            lifecycleOwner.startAndResume()
        }
    }

    private fun initialPreviewView() {
        activityRule.scenario.onActivity { activity ->
            previewView = PreviewView(context)
            previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            activity.setContentView(previewView)
        }
    }

    private fun initialController() {
        cameraController = LifecycleCameraController(context)
        cameraController.initializationFuture.get()
        instrumentation.runOnMainSync {
            if (initialQuality != TargetQuality.NOT_SPECIFIED) {
                cameraController.videoCaptureQualitySelector = initialQuality.getSelector()
            }
            cameraController.cameraSelector = cameraSelector

            //  If the PreviewView is not attached, the enabled use cases will not be applied.
            previewView.controller = cameraController

            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.setEnabledUseCases(VIDEO_CAPTURE)
        }
    }

    private fun createTempFile(): File {
        return File.createTempFile("CameraX", ".tmp").apply { deleteOnExit() }
    }

    private fun createMediaStoreOutputOptions(resolver: ContentResolver): MediaStoreOutputOptions {
        val videoFileName = "video_" + System.currentTimeMillis()
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.Video.Media.TITLE, videoFileName)
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
        return MediaStoreOutputOptions.Builder(
                resolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            )
            .setContentValues(contentValues)
            .build()
    }

    private fun recordVideoCompletely(outputOptions: OutputOptions, audioConfig: AudioConfig) {
        // Act.
        recordVideoWithInterruptAction(outputOptions, audioConfig) {
            instrumentation.runOnMainSync { activeRecording.stop() }
        }

        // Verify.
        assertThat(finalize.hasError()).isFalse()
    }

    private fun recordVideoWithInterruptAction(
        outputOptions: OutputOptions,
        audioConfig: AudioConfig,
        runInterruptAction: () -> Unit,
    ) {
        // Arrange.
        latchForVideoSaved = CountDownLatch(VIDEO_SAVED_COUNT_DOWN)

        // Act.
        recordVideo(outputOptions, audioConfig)
        runInterruptAction()

        // Verify.
        // Wait for finalize event to saved file.
        assertThat(latchForVideoSaved.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        instrumentation.runOnMainSync { assertThat(cameraController.isRecording).isFalse() }
    }

    private fun recordVideo(outputOptions: OutputOptions, audioConfig: AudioConfig) {
        // Arrange.
        latchForVideoStarted = CountDownLatch(VIDEO_STARTED_COUNT_DOWN)
        latchForVideoRecording = CountDownLatch(VIDEO_RECORDING_COUNT_DOWN)

        // Act.
        instrumentation.runOnMainSync {
            startRecording(outputOptions, audioConfig)
            assertThat(cameraController.isRecording).isTrue()
        }

        // Verify.
        assertThat(latchForVideoStarted.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()

        // Wait for status event to proceed recording for a while.
        assertThat(latchForVideoRecording.await(VIDEO_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue()
    }

    @MainThread
    private fun startRecording(outputOptions: OutputOptions, audioConfig: AudioConfig) {
        if (outputOptions is FileOutputOptions) {
            activeRecording =
                cameraController.startRecording(
                    outputOptions,
                    audioConfig,
                    CameraXExecutors.directExecutor(),
                    videoRecordEventListener,
                )
        } else if (outputOptions is FileDescriptorOutputOptions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activeRecording =
                    cameraController.startRecording(
                        outputOptions,
                        audioConfig,
                        CameraXExecutors.directExecutor(),
                        videoRecordEventListener,
                    )
            } else {
                throw UnsupportedOperationException(
                    "File descriptors are not supported on pre-Android O (API 26) devices."
                )
            }
        } else if (outputOptions is MediaStoreOutputOptions) {
            activeRecording =
                cameraController.startRecording(
                    outputOptions,
                    audioConfig,
                    CameraXExecutors.directExecutor(),
                    videoRecordEventListener,
                )
        } else {
            throw IllegalArgumentException("Unsupported OutputOptions type.")
        }
    }

    private fun checkFileOnlyHasVideo(uri: Uri) {
        checkFileHasVideo(uri)
        checkFileHasAudio(uri, false)
    }

    private fun checkFileHasAudioAndVideo(uri: Uri) {
        checkFileHasVideo(uri)
        checkFileHasAudio(uri, true)
    }

    private fun checkFileHasVideo(uri: Uri) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val hasVideo = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            assertThat(hasVideo).isEqualTo("yes")
        }
    }

    private fun checkFileHasAudio(uri: Uri, hasAudio: Boolean) {
        val mediaRetriever = MediaMetadataRetriever()
        mediaRetriever.apply {
            setDataSource(context, uri)
            val value = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)

            assertThat(value).isEqualTo(if (hasAudio) "yes" else null)
        }
    }

    private fun skipTestWithSurfaceProcessingOnCuttlefishApi30() {
        // Skip test for b/253211491
        Assume.assumeFalse(
            "Skip tests for Cuttlefish API 30 eglCreateWindowSurface issue",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 30,
        )
    }
}

fun assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk() {
    // Skip for b/293978082. For tests that will unbind the VideoCapture before stop the recording,
    // they should be skipped since media server will crash if the codec surface has been removed
    // before MediaCodec.stop() is called.
    assumeTrue(
        DeviceQuirks.get(StopCodecAfterSurfaceRemovalCrashMediaServerQuirk::class.java) == null
    )
}
