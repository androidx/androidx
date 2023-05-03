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

import androidx.camera.testing.mocks.helpers.ArgumentCaptor as ArgumentCaptorCameraX
import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.AppOpsManager.OnOpNotedCallback
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.location.Location
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Observable.Observer
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.AudioUtil
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.GarbageCollectionUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.asFlow
import androidx.camera.testing.mocks.MockConsumer
import androidx.camera.testing.mocks.helpers.CallTimes
import androidx.camera.testing.mocks.helpers.CallTimesAtLeast
import androidx.camera.video.VideoOutput.SourceState.ACTIVE_NON_STREAMING
import androidx.camera.video.VideoOutput.SourceState.ACTIVE_STREAMING
import androidx.camera.video.VideoOutput.SourceState.INACTIVE
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_RECORDING_GARBAGE_COLLECTED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.camera.video.VideoRecordEvent.Pause
import androidx.camera.video.VideoRecordEvent.Resume
import androidx.camera.video.VideoRecordEvent.Start
import androidx.camera.video.VideoRecordEvent.Status
import androidx.camera.video.internal.compat.quirk.DeactivateEncoderSurfaceBeforeStopEncoderQuirk
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.ExtraSupportedResolutionQuirk
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import androidx.camera.video.internal.encoder.EncoderFactory
import androidx.camera.video.internal.encoder.InvalidConfigException
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.assertThrows
import androidx.testutils.fail
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout

private const val DEFAULT_STATUS_COUNT = 5
private const val GENERAL_TIMEOUT = 5000L
private const val STATUS_TIMEOUT = 15000L
private const val TEST_ATTRIBUTION_TAG = "testAttribution"

// For the file size is small, the final file length possibly exceeds the file size limit
// after adding the file header. We still add the buffer for the tolerance of comparing the
// file length and file size limit.
private const val FILE_SIZE_LIMIT_BUFFER = 50 * 1024 // 50k threshold buffer

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class RecorderTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule
    var testName: TestName = TestName()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    // TODO(b/278168212): Only SDR is checked by now. Need to extend to HDR dynamic ranges.
    private val dynamicRange = DynamicRange.SDR
    private val recordingsToStop = mutableListOf<RecordingProcess>()

    private lateinit var cameraUseCaseAdapter: CameraUseCaseAdapter
    private lateinit var preview: Preview
    private lateinit var surfaceTexturePreview: Preview

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK))
        // Skip for b/168175357, b/233661493
        assumeFalse(
            "Skip tests for Cuttlefish MediaCodec issues",
            Build.MODEL.contains("Cuttlefish") &&
                (Build.VERSION.SDK_INT == 29 || Build.VERSION.SDK_INT == 33)
        )
        // Skip for b/241876294
        assumeFalse(
            "Skip test for devices with ExtraSupportedResolutionQuirk, since the extra" +
                " resolutions cannot be used when the provided surface is an encoder surface.",
            DeviceQuirks.get(ExtraSupportedResolutionQuirk::class.java) != null
        )
        assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        CameraXUtil.initialize(
            context,
            cameraConfig
        ).get()
        cameraUseCaseAdapter = CameraUtil.createCameraUseCaseAdapter(context, cameraSelector)

        // Using Preview so that the surface provider could be set to control when to issue the
        // surface request.
        val cameraInfo = cameraUseCaseAdapter.cameraInfo
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        val candidates = mutableSetOf<Size>().apply {
            if (testName.methodName == "setFileSizeLimit") {
                videoCapabilities.getProfiles(Quality.FHD, dynamicRange)?.defaultVideoProfile
                    ?.let { add(Size(it.width, it.height)) }
                videoCapabilities.getProfiles(Quality.HD, dynamicRange)?.defaultVideoProfile
                    ?.let { add(Size(it.width, it.height)) }
                videoCapabilities.getProfiles(Quality.SD, dynamicRange)?.defaultVideoProfile
                    ?.let { add(Size(it.width, it.height)) }
            }
            videoCapabilities.getProfiles(Quality.LOWEST, dynamicRange)?.defaultVideoProfile
                ?.let { add(Size(it.width, it.height)) }
        }
        assumeTrue(candidates.isNotEmpty())

        val resolutions: List<android.util.Pair<Int, Array<Size>>> =
            listOf<android.util.Pair<Int, Array<Size>>>(
                android.util.Pair.create(
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    candidates.toTypedArray()
                )
            )
        preview = Preview.Builder().setSupportedResolutions(resolutions).build()

        // Add another Preview to provide an additional surface for b/168187087.
        surfaceTexturePreview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            surfaceTexturePreview.setSurfaceProvider(
                SurfaceTextureProvider.createSurfaceTextureProvider(
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
            )
        }

        assumeTrue(
            "This combination (preview, surfaceTexturePreview) is not supported.",
            cameraUseCaseAdapter.isUseCasesCombinationSupported(
                preview,
                surfaceTexturePreview
            )
        )

        cameraUseCaseAdapter = CameraUtil.createCameraAndAttachUseCase(
            context,
            cameraSelector,
            // Must put surfaceTexturePreview before preview while addUseCases, otherwise
            // an issue on Samsung device will occur. See b/196755459.
            surfaceTexturePreview,
            preview
        )
    }

    @After
    fun tearDown() {
        for (recording in recordingsToStop) {
            recording.stop()
        }

        if (this::cameraUseCaseAdapter.isInitialized) {
            instrumentation.runOnMainSync {
                cameraUseCaseAdapter.removeUseCases(cameraUseCaseAdapter.useCases)
            }
        }

        CameraXUtil.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun canRecordToFile() {
        // Arrange.
        val outputOptions = createFileOutputOptions()
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify { finalize ->
            // Assert.
            val uri = finalize.outputResults.outputUri
            assertThat(uri).isEqualTo(Uri.fromFile(outputOptions.file))
            checkFileHasAudioAndVideo(uri)
        }
    }

    @Test
    fun recordingWithSetTargetVideoEncodingBitRate() {
        testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(6_000_000)
    }

    @Test
    fun recordingWithSetTargetVideoEncodingBitRateOutOfRange() {
        testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(1000_000_000)
    }

    @Test
    fun recordingWithNegativeBitRate() {
        assertThrows(IllegalArgumentException::class.java) {
            createRecorder(targetBitrate = -5)
        }
    }

    @Test
    fun canRecordToMediaStore() {
        assumeTrue(
            "Ignore the test since the MediaStore.Video has compatibility issues.",
            DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null
        )

        // Arrange.
        val contentResolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify { finalize ->
            // Assert.
            val uri = finalize.outputResults.outputUri
            checkFileHasAudioAndVideo(uri)

            // Clean-up.
            contentResolver.delete(uri, null, null)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun canRecordToFileDescriptor() {
        // Arrange.
        val file = createTempFile()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        val outputOptions = FileDescriptorOutputOptions.Builder(pfd).build()
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.startAndVerify()
        // ParcelFileDescriptor should be safe to close after PendingRecording#start.
        pfd.close()
        recording.stopAndVerify()

        // Assert.
        checkFileHasAudioAndVideo(Uri.fromFile(file))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun recordToFileDescriptor_withClosedFileDescriptor_receiveError() {
        // Arrange.
        val file = createTempFile()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
        pfd.close()
        val outputOptions = FileDescriptorOutputOptions.Builder(pfd).build()
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.start()
        recording.stopAndVerify { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_INVALID_OUTPUT_OPTIONS)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 25)
    @SuppressLint("NewApi") // Intentionally testing behavior of calling from invalid API level
    fun prepareRecordingWithFileDescriptor_throwsExceptionBeforeApi26() {
        // Arrange.
        val recorder = createRecorder()
        val file = createTempFile()
        ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_WRITE
        ).use { pfd ->
            // Assert.
            assertThrows(UnsupportedOperationException::class.java) {
                // Act.
                recorder.prepareRecording(context, FileDescriptorOutputOptions.Builder(pfd).build())
            }
        }
    }

    @Test
    fun canPauseResume() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.pauseAndVerify()
        recording.resumeAndVerify()
        recording.stopAndVerify { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_NONE)
        }
    }

    @Test
    fun canStartRecordingPaused_whenRecorderInitializing() {
        // Arrange.
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = createRecordingProcess(recorder = recorder)

        // Act.
        recording.start()
        recording.pause()
        // Only invoke surface request after pause() has been called
        recorder.sendSurfaceRequest()

        // Assert.
        recording.verifyStart()
        recording.verifyPause()
    }

    @Test
    fun canReceiveRecordingStats() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.pauseAndVerify()
        recording.resumeAndVerify()
        recording.stopAndVerify()

        // Assert.
        val events = recording.getAllEvents()
        assertThat(events.size).isAtLeast(
            1 /* Start */ +
                5 /* Status */ +
                1 /* Pause */ +
                1 /* Resume */ +
                5 /* Status */ +
                1 /* Stop */
        )

        // Assert: Ensure duration and bytes are increasing.
        List(events.size - 1) { index ->
            Pair(events[index].recordingStats, events[index + 1].recordingStats)
        }.forEach { (former: RecordingStats, latter: RecordingStats) ->
            assertThat(former.numBytesRecorded).isAtMost(latter.numBytesRecorded)
            assertThat(former.recordedDurationNanos).isAtMost((latter.recordedDurationNanos))
        }

        // Assert: Ensure they are not all zero by checking the last stats.
        events.last().recordingStats.also {
            assertThat(it.numBytesRecorded).isGreaterThan(0L)
            assertThat(it.recordedDurationNanos).isGreaterThan(0L)
        }
    }

    @Test
    fun setFileSizeLimit() {
        // Arrange.
        val fileSizeLimit = 500L * 1024L // 500 KB
        val outputOptions = createFileOutputOptions(fileSizeLimit = fileSizeLimit)
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        // To avoid long timeout of finalize, verify the start event to ensure the recording is
        // started. But don't verify the status count since we don't know how many status will
        // reach the file size limit.
        recording.startAndVerify(statusCount = 0)
        recording.verifyFinalize(timeoutMs = 60_000L) { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_FILE_SIZE_LIMIT_REACHED)
            assertThat(outputOptions.file.length())
                .isLessThan(fileSizeLimit + FILE_SIZE_LIMIT_BUFFER)
        }
    }

    // Sets the file size limit to 1 byte, which will be lower than the initial data sent from
    // the encoder. This will ensure that the recording will be finalized even if it has no data
    // written to it.
    @Test
    fun setFileSizeLimitLowerThanInitialDataSize() {
        // Arrange.
        val fileSizeLimit = 1L // 1 byte
        val outputOptions = createFileOutputOptions(fileSizeLimit = fileSizeLimit)
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.start()
        recording.verifyFinalize { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_FILE_SIZE_LIMIT_REACHED)
        }
    }

    @Test
    fun setLocation() {
        runLocationTest(createLocation(25.033267462243586, 121.56454121737946))
    }

    @Test
    fun setNegativeLocation() {
        runLocationTest(createLocation(-27.14394722411734, -109.33053675296067))
    }

    @Test
    fun stop_withErrorWhenDurationLimitReached() {
        // Arrange.
        val durationLimitMs = 3000L
        val durationToleranceMs = 50L
        val outputOptions = createFileOutputOptions(durationLimitMillis = durationLimitMs)
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.start()

        // Assert.
        recording.verifyFinalize(timeoutMs = durationLimitMs + 2000L) { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_DURATION_LIMIT_REACHED)
            assertThat(finalize.recordingStats.recordedDurationNanos)
                .isAtMost(TimeUnit.MILLISECONDS.toNanos(durationLimitMs + durationToleranceMs))
            checkDurationAtMost(
                Uri.fromFile(outputOptions.file),
                durationLimitMs + durationToleranceMs
            )
        }
    }

    @Test
    fun checkStreamState() {
        // Arrange.
        val recorder = createRecorder()

        @Suppress("UNCHECKED_CAST")
        val streamInfoObserver = mock(Observer::class.java) as Observer<StreamInfo>
        val inOrder = inOrder(streamInfoObserver)
        recorder.streamInfo.addObserver(directExecutor(), streamInfoObserver)

        // Assert: Recorder should start in INACTIVE stream state before any recordings
        inOrder.verify(streamInfoObserver, timeout(GENERAL_TIMEOUT)).onNewData(
            argThat {
                it!!.streamState == StreamInfo.StreamState.INACTIVE
            }
        )
        val recording = createRecordingProcess(recorder = recorder)

        // Act.
        recording.start()

        // Assert: Starting recording should move Recorder to ACTIVE stream state
        inOrder.verify(streamInfoObserver, timeout(5000L)).onNewData(
            argThat { it!!.streamState == StreamInfo.StreamState.ACTIVE }
        )

        // Act.
        recording.stop()

        // Assert: Stopping recording should eventually move to INACTIVE stream state
        inOrder.verify(streamInfoObserver, timeout(GENERAL_TIMEOUT)).onNewData(
            argThat {
                it!!.streamState == StreamInfo.StreamState.INACTIVE
            }
        )
    }

    @Test
    fun start_throwsExceptionWhenActive() {
        // Arrange.
        val recorder = createRecorder()
        val recording = createRecordingProcess(recorder = recorder)

        // Act: 1st start.
        recording.start()

        // Assert.
        assertThrows(java.lang.IllegalStateException::class.java) {
            // Act: 2nd start.
            val recording2 = createRecordingProcess(recorder = recorder)
            recording2.start()
        }
    }

    @Test
    fun start_whenSourceActiveNonStreaming() {
        // Arrange.
        val recorder = createRecorder(initSourceState = ACTIVE_NON_STREAMING)
        val recording = createRecordingProcess(recorder = recorder)

        // Act.
        recording.start()
        recorder.onSourceStateChanged(ACTIVE_STREAMING)
        recording.verifyStart()
        recording.verifyStatus()
        recording.stopAndVerify { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_NONE)
        }
    }

    @Test
    fun start_finalizeImmediatelyWhenSourceInactive() {
        // Arrange.
        val recorder = createRecorder(initSourceState = INACTIVE)
        val recording = createRecordingProcess(recorder = recorder)

        // Act.
        recording.start()

        // Assert.
        recording.verifyFinalize { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)
        }
    }

    @Test
    fun pause_whenSourceActiveNonStreaming() {
        // Arrange.
        val recorder = createRecorder(
            sendSurfaceRequest = false,
            initSourceState = ACTIVE_NON_STREAMING
        )
        val recording = createRecordingProcess(recorder = recorder)

        // Act.
        recording.start()
        recording.pause()
        recorder.sendSurfaceRequest()

        // Assert.
        recording.verifyStart()
        recording.verifyPause()
        recording.stopAndVerify { finalize ->
            // Assert.
            assertThat(finalize.error).isEqualTo(ERROR_NO_VALID_DATA)
        }
    }

    @Test
    fun pause_noOpWhenAlreadyPaused() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.pauseAndVerify()
        recording.pause()

        // Assert: One Pause event.
        val events = recording.getAllEvents()
        val pauseEvents = events.filterIsInstance<Pause>()
        assertThat(pauseEvents.size).isAtMost(1)
    }

    @Test
    fun pause_throwsExceptionWhenStopping() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify()

        // Assert.
        assertThrows(IllegalStateException::class.java) {
            recording.pause()
        }
    }

    @Test
    fun resume_noOpWhenNotPaused() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.resume()
        recording.stopAndVerify()

        // Assert: No Resume event.
        val events = recording.getAllEvents()
        val resumeEvents = events.filterIsInstance<Resume>()
        assertThat(resumeEvents).isEmpty()
    }

    @Test
    fun resume_throwsExceptionWhenStopping() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.stop()

        // Assert.
        assertThrows(IllegalStateException::class.java) {
            recording.resumeAndVerify()
        }
    }

    @Test
    fun stop_beforeSurfaceRequested() {
        // Arrange.
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = createRecordingProcess(recorder = recorder)

        // Act.
        recording.start()
        recording.stop()
        recorder.sendSurfaceRequest()

        // Assert.
        recording.verifyFinalize { finalize ->
            assertThat(finalize.error).isEqualTo(ERROR_NO_VALID_DATA)
        }
    }

    @Test
    fun stop_WhenUseCaseDetached() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        instrumentation.runOnMainSync {
            cameraUseCaseAdapter.removeUseCases(listOf(preview))
        }

        // Assert.
        recording.verifyFinalize { finalize ->
            assertThat(finalize.error).isEqualTo(ERROR_SOURCE_INACTIVE)
        }
    }

    @Test
    fun stop_whenRecordingIsGarbageCollected() {
        // Arrange.
        var recording: RecordingProcess? = createRecordingProcess()
        val listener = recording!!.listener

        // Act.
        recording.startAndVerify()
        // Remove reference to recording and run GC. The recording should be stopped once
        // the Recording's finalizer runs.
        recordingsToStop.remove(recording)
        @Suppress("UNUSED_VALUE")
        recording = null
        GarbageCollectionUtil.runFinalization()

        // Assert: Ensure the event listener gets a finalize event. Note: the word "finalize" is
        // very overloaded here. This event means the recording has finished, but does not relate
        // to the finalizer that runs during garbage collection. However, that is what causes the
        // recording to finish.
        listener.verifyFinalize { finalize ->
            assertThat(finalize.error).isEqualTo(ERROR_RECORDING_GARBAGE_COLLECTED)
        }
    }

    @Test
    fun stop_noOpWhenStopping() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify()
        recording.stop()

        // Assert.
        recording.verifyNoMoreEvent()
    }

    @Test
    fun mute_outputWithAudioTrack() {
        // Arrange.
        val outputOptions = createFileOutputOptions()
        val recording = createRecordingProcess(outputOptions = outputOptions)

        recording.startAndVerify()
        recording.mute(true)

        recording.stopAndVerify { finalize ->
            // Assert.
            val uri = finalize.outputResults.outputUri
            assertThat(uri).isEqualTo(Uri.fromFile(outputOptions.file))
            // The output file should contain audio track even it's muted at the beginning.
            checkFileHasAudioAndVideo(uri)
        }
    }

    @Test
    fun mute_receiveCorrectAudioStats() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify()
        recording.mute(true)
        // TODO(b/274862085): Change to verify the status events consecutively having MUTED state
        //  by adding the utility to MockConsumer.
        recording.verifyStatus(5) { statusList ->
            // Assert.
            assertThat(statusList.last().recordingStats.audioStats.audioState)
                .isEqualTo(AudioStats.AUDIO_STATE_MUTED)
        }

        // Act.
        recording.mute(false)
        recording.verifyStatus(5) { statusList ->
            // Assert.
            assertThat(statusList.last().recordingStats.audioStats.audioState)
                .isEqualTo(AudioStats.AUDIO_STATE_ACTIVE)
        }
        recording.stopAndVerify()
    }

    @Test
    fun mute_noOpIfAudioDisabled() {
        // Arrange.
        val recording = createRecordingProcess(withAudio = false)

        // Act.
        recording.startAndVerify()

        // Assert: muting or unmuting a recording without audio should be no-op.
        recording.mute(true)
        recording.mute(false)
        recording.stopAndVerify()
    }

    @Test
    fun optionsOverridesDefaults() {
        val qualitySelector = QualitySelector.from(Quality.HIGHEST)
        val recorder = createRecorder(qualitySelector = qualitySelector)

        assertThat(recorder.qualitySelector).isEqualTo(qualitySelector)
    }

    @Test
    fun canRetrieveProvidedExecutorFromRecorder() {
        val myExecutor = Executor { command -> command?.run() }
        val recorder = createRecorder(executor = myExecutor)

        assertThat(recorder.executor).isSameInstanceAs(myExecutor)
    }

    @Test
    fun cannotRetrieveExecutorWhenExecutorNotProvided() {
        val recorder = createRecorder()

        assertThat(recorder.executor).isNull()
    }

    @Test
    fun canRecordWithoutAudio() {
        // Arrange.
        val recording = createRecordingProcess(withAudio = false)

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify { finalize ->
            // Assert.
            val uri = finalize.outputResults.outputUri
            checkFileHasAudioAndVideo(uri, hasAudio = false)
        }
    }

    @Test
    fun audioAmplitudeIsNoneWhenAudioIsDisabled() {
        // Arrange.
        val recording = createRecordingProcess(withAudio = false)

        // Act.
        recording.startAndVerify { onStatus ->
            val amplitude = onStatus[0].recordingStats.audioStats.audioAmplitude
            assertThat(amplitude).isEqualTo(AudioStats.AUDIO_AMPLITUDE_NONE)
        }

        recording.stopAndVerify { finalize ->
            // Assert.
            val uri = finalize.outputResults.outputUri
            checkFileHasAudioAndVideo(uri, hasAudio = false)
            assertThat(finalize.recordingStats.audioStats.audioAmplitude)
                .isEqualTo(AudioStats.AUDIO_AMPLITUDE_NONE)
        }
    }

    @Test
    fun canGetAudioStatsAmplitude() {
        // Arrange.
        val recording = createRecordingProcess()

        // Act.
        recording.startAndVerify { onStatus ->
            val amplitude = onStatus[0].recordingStats.audioStats.audioAmplitude
            assertThat(amplitude).isAtLeast(AudioStats.AUDIO_AMPLITUDE_NONE)
        }

        recording.stopAndVerify { finalize ->
            // Assert.
            val uri = finalize.outputResults.outputUri
            checkFileHasAudioAndVideo(uri, hasAudio = true)
            assertThat(finalize.recordingStats.audioStats.audioAmplitude)
                .isAtLeast(AudioStats.AUDIO_AMPLITUDE_NONE)
        }
    }

    @Test
    fun cannotStartMultiplePendingRecordingsWhileInitializing() {
        // Arrange: Prepare 1st recording and start.
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = createRecordingProcess(recorder = recorder)
        recording.start()

        // Assert.
        assertThrows<IllegalStateException> {
            // Act: Prepare 2nd recording and start.
            createRecordingProcess(recorder = recorder).start()
        }
    }

    @Test
    fun canRecoverFromErrorState(): Unit = runBlocking {
        // Arrange.
        // Create a video encoder factory that will fail on first 2 create encoder requests.
        var createEncoderRequestCount = 0
        val recorder = createRecorder(
            videoEncoderFactory = { executor, config ->
                if (createEncoderRequestCount < 2) {
                    createEncoderRequestCount++
                    throw InvalidConfigException("Create video encoder fail on purpose.")
                } else {
                    Recorder.DEFAULT_ENCODER_FACTORY.createEncoder(executor, config)
                }
            })
        // Recorder initialization should fail by 1st encoder creation fail.
        // Wait STREAM_ID_ERROR which indicates Recorder enter the error state.
        withTimeoutOrNull(3000) {
            recorder.streamInfo.asFlow().dropWhile { it!!.id != StreamInfo.STREAM_ID_ERROR }.first()
        } ?: fail("Do not observe STREAM_ID_ERROR from StreamInfo observer.")

        // Act: 1st recording request should fail by 2nd encoder creation fail.
        var recording = createRecordingProcess(recorder = recorder)
        recording.start()
        recording.verifyFinalize { finalize ->
            assertThat(finalize.error).isEqualTo(ERROR_RECORDER_ERROR)
        }

        // Act: 2nd recording request should be successful.
        recording = createRecordingProcess(recorder = recorder)
        recording.startAndVerify()
        recording.stopAndVerify()
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun audioRecordIsAttributed() = runBlocking {
        // Arrange.
        val notedTag = CompletableDeferred<String>()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appOps.setOnOpNotedCallback(Dispatchers.Main.asExecutor(), object : OnOpNotedCallback() {
            override fun onNoted(p0: SyncNotedAppOp) {
                // no-op. record_audio should be async.
            }

            override fun onSelfNoted(p0: SyncNotedAppOp) {
                // no-op. record_audio should be async.
            }

            override fun onAsyncNoted(noted: AsyncNotedAppOp) {
                if (AppOpsManager.OPSTR_RECORD_AUDIO == noted.op &&
                    TEST_ATTRIBUTION_TAG == noted.attributionTag
                ) {
                    notedTag.complete(noted.attributionTag!!)
                }
            }
        })
        val attributionContext = context.createAttributionContext(TEST_ATTRIBUTION_TAG)
        val recording = createRecordingProcess(context = attributionContext)

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
        }
    }

    private fun testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(targetBitrate: Int) {
        // Arrange.
        val recorder = createRecorder(targetBitrate = targetBitrate)
        val recording = createRecordingProcess(recorder = recorder, withAudio = false)

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify { finalize ->
            assertThat(finalize.error).isEqualTo(ERROR_NONE)
        }

        // Assert.
        assertThat(recorder.mFirstRecordingVideoBitrate).isIn(
            com.google.common.collect.Range.closed(
                recorder.mVideoEncoderBitrateRange.lower,
                recorder.mVideoEncoderBitrateRange.upper
            )
        )
    }

    private fun Recorder.sendSurfaceRequest() {
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request: SurfaceRequest ->
                onSurfaceRequested(request)
            }
        }
    }

    private fun createTempFile() = temporaryFolder.newFile()

    private fun createRecorder(
        sendSurfaceRequest: Boolean = true,
        initSourceState: VideoOutput.SourceState = ACTIVE_STREAMING,
        qualitySelector: QualitySelector? = null,
        executor: Executor? = null,
        videoEncoderFactory: EncoderFactory? = null,
        audioEncoderFactory: EncoderFactory? = null,
        targetBitrate: Int? = null,
    ): Recorder {
        val recorder = Recorder.Builder().apply {
            qualitySelector?.let { setQualitySelector(it) }
            executor?.let { setExecutor(it) }
            videoEncoderFactory?.let { setVideoEncoderFactory(it) }
            audioEncoderFactory?.let { setAudioEncoderFactory(it) }
            targetBitrate?.let { setTargetVideoEncodingBitRate(targetBitrate) }
        }.build()
        if (sendSurfaceRequest) {
            recorder.sendSurfaceRequest()
        }
        recorder.onSourceStateChanged(initSourceState)
        return recorder
    }

    private fun createFileOutputOptions(
        file: File = createTempFile(),
        fileSizeLimit: Long? = null,
        durationLimitMillis: Long? = null,
        location: Location? = null,
    ): FileOutputOptions = FileOutputOptions.Builder(file).apply {
        fileSizeLimit?.let { setFileSizeLimit(it) }
        durationLimitMillis?.let { setDurationLimitMillis(it) }
        location?.let { setLocation(it) }
    }.build()

    private fun createRecordingProcess(
        recorder: Recorder = createRecorder(),
        context: Context = ApplicationProvider.getApplicationContext(),
        outputOptions: OutputOptions = createFileOutputOptions(),
        withAudio: Boolean = true
    ) = RecordingProcess(
        recorder,
        context,
        outputOptions,
        withAudio
    )

    inner class RecordingProcess(
        private val recorder: Recorder,
        context: Context,
        outputOptions: OutputOptions,
        withAudio: Boolean
    ) {
        private val pendingRecording: PendingRecording =
            PendingRecording(context, recorder, outputOptions).apply {
                if (withAudio) {
                    withAudioEnabled()
                }
            }
        val listener = MockConsumer<VideoRecordEvent>()
        private lateinit var recording: Recording

        fun startAndVerify(
            statusCount: Int = DEFAULT_STATUS_COUNT,
            onStatus: ((List<Status>) -> Unit)? = null,
        ) = startInternal(verify = true, statusCount = statusCount, onStatus = onStatus)

        fun start() = startInternal(verify = false)

        private fun startInternal(
            verify: Boolean = false,
            statusCount: Int = DEFAULT_STATUS_COUNT,
            onStatus: ((List<Status>) -> Unit)? = null
        ) {
            recording = pendingRecording.start(mainThreadExecutor(), listener)
            recordingsToStop.add(this)
            if (verify) {
                verifyStart()
                if (statusCount > 0) {
                    verifyStatus(statusCount = statusCount, onStatus = onStatus)
                }
            }
        }

        fun verifyStart() {
            listener.verifyStart()
        }

        fun verifyStatus(
            statusCount: Int = DEFAULT_STATUS_COUNT,
            onStatus: ((List<Status>) -> Unit)? = null,
        ) {
            listener.verifyStatus(eventCount = statusCount, onEvent = onStatus)
        }

        fun stopAndVerify(onFinalize: ((Finalize) -> Unit)? = null) =
            stopInternal(verify = true, onFinalize)

        fun stop() = stopInternal(verify = false)

        private fun stopInternal(
            verify: Boolean = false,
            onFinalize: ((Finalize) -> Unit)? = null
        ) {
            recording.stopSafely(recorder)
            if (verify) {
                verifyFinalize(onFinalize = onFinalize)
            }
        }

        fun verifyFinalize(
            timeoutMs: Long = GENERAL_TIMEOUT,
            onFinalize: ((Finalize) -> Unit)? = null
        ) = listener.verifyFinalize(timeoutMs = timeoutMs, onFinalize = onFinalize)

        fun pauseAndVerify() = pauseInternal(verify = true)

        fun pause() = pauseInternal(verify = false)

        private fun pauseInternal(verify: Boolean = false) {
            recording.pause()
            if (verify) {
                verifyPause()
            }
        }

        fun verifyPause() = listener.verifyPause()

        fun resumeAndVerify() = resumeInternal(verify = true)

        fun resume() = resumeInternal(verify = false)

        private fun resumeInternal(verify: Boolean = false) {
            recording.resume()
            if (verify) {
                verifyResume()
            }
        }

        fun mute(muted: Boolean = false) = recording.mute(muted)

        private fun verifyResume() {
            listener.verifyResume()
            listener.verifyStatus()
        }

        fun getAllEvents(): List<VideoRecordEvent> {
            lateinit var events: List<VideoRecordEvent>
            listener.verifyEvent(
                VideoRecordEvent::class.java,
                CallTimesAtLeast(1),
                onEvent = {
                    events = it
                }
            )
            return events
        }

        fun verifyNoMoreEvent() = listener.verifyNoMoreAcceptCalls(/*inOrder=*/true)
    }

    private fun checkFileHasAudioAndVideo(
        uri: Uri,
        hasAudio: Boolean = true,
    ) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, uri)
            assertThat(it.hasVideo()).isEqualTo(true)
            assertThat(it.hasAudio()).isEqualTo(hasAudio)
        }
    }

    @Suppress("SameParameterValue")
    private fun checkLocation(uri: Uri, location: Location) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, uri)
            // Only test on mp4 output format, others will be ignored.
            val mime = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            assumeTrue(
                "Unsupported mime = $mime",
                "video/mp4".equals(mime, ignoreCase = true)
            )
            val value = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            assertThat(value).isNotNull()
            // ex: (90, 180) => "+90.0000+180.0000/" (ISO-6709 standard)
            val matchGroup =
                "([+-]?[0-9]+(\\.[0-9]+)?)([+-]?[0-9]+(\\.[0-9]+)?)".toRegex()
                    .find(value!!) ?: fail("Fail on checking location metadata: $value")
            val lat = matchGroup.groupValues[1].toDouble()
            val lon = matchGroup.groupValues[3].toDouble()

            // MediaMuxer.setLocation rounds the value to 4 decimal places
            val tolerance = 0.0001
            assertWithMessage("Fail on latitude. $lat($value) vs ${location.latitude}")
                .that(lat).isWithin(tolerance).of(location.latitude)
            assertWithMessage("Fail on longitude. $lon($value) vs ${location.longitude}")
                .that(lon).isWithin(tolerance).of(location.longitude)
        }
    }

    @Suppress("SameParameterValue")
    private fun checkDurationAtMost(uri: Uri, duration: Long) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, uri)
            val durationFromFile = it.getDuration()

            assertThat(durationFromFile).isNotNull()
            assertThat(durationFromFile!!).isAtMost(duration)
        }
    }

    // It fails on devices with certain chipset if the codec is stopped when the camera is still
    // producing frames to the provided surface. This method first stop the camera from
    // producing frames then stops the recording safely on the problematic devices.
    private fun Recording.stopSafely(recorder: Recorder) {
        val deactivateSurfaceBeforeStop =
            DeviceQuirks.get(DeactivateEncoderSurfaceBeforeStopEncoderQuirk::class.java) != null
        if (deactivateSurfaceBeforeStop) {
            instrumentation.runOnMainSync {
                preview.setSurfaceProvider(null)
            }
        }
        stop()
        if (deactivateSurfaceBeforeStop && Build.VERSION.SDK_INT >= 23) {
            recorder.sendSurfaceRequest()
        }
    }

    private fun runLocationTest(location: Location) {
        // Arrange.
        val outputOptions = createFileOutputOptions(location = location)
        val recording = createRecordingProcess(outputOptions = outputOptions)

        // Act.
        recording.startAndVerify()
        recording.stopAndVerify { finalize ->
            // Assert.
            checkLocation(finalize.outputResults.outputUri, location)
        }
    }

    private fun createLocation(
        latitude: Double,
        longitude: Double,
        provider: String = "FakeProvider"
    ): Location =
        Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
        }

    private fun MockConsumer<VideoRecordEvent>.verifyStart(
        inOrder: Boolean = true,
        onEvent: ((Start) -> Unit)? = null
    ) {
        verifyEvent(Start::class.java, inOrder = inOrder, onEvent = onEvent)
    }

    private fun MockConsumer<VideoRecordEvent>.verifyFinalize(
        inOrder: Boolean = true,
        timeoutMs: Long = GENERAL_TIMEOUT,
        onFinalize: ((Finalize) -> Unit)? = null
    ) {
        verifyEvent(
            Finalize::class.java,
            inOrder = inOrder,
            timeoutMs = timeoutMs,
            onEvent = onFinalize
        )
    }

    private fun MockConsumer<VideoRecordEvent>.verifyStatus(
        eventCount: Int = DEFAULT_STATUS_COUNT,
        inOrder: Boolean = true,
        onEvent: ((List<Status>) -> Unit)? = null,
    ) {
        verifyEvent(
            Status::class.java,
            CallTimesAtLeast(eventCount),
            inOrder = inOrder,
            timeoutMs = STATUS_TIMEOUT,
            onEvent = onEvent
        )
    }

    private fun MockConsumer<VideoRecordEvent>.verifyPause(
        inOrder: Boolean = true,
        onEvent: ((Pause) -> Unit)? = null
    ) {
        verifyEvent(Pause::class.java, inOrder = inOrder, onEvent = onEvent)
    }

    private fun MockConsumer<VideoRecordEvent>.verifyResume(
        inOrder: Boolean = true,
        onEvent: ((Resume) -> Unit)? = null,
    ) {
        verifyEvent(Resume::class.java, inOrder = inOrder, onEvent = onEvent)
    }

    private fun <T : VideoRecordEvent> MockConsumer<VideoRecordEvent>.verifyEvent(
        eventType: Class<T>,
        inOrder: Boolean = false,
        timeoutMs: Long = GENERAL_TIMEOUT,
        onEvent: ((T) -> Unit)? = null,
    ) {
        verifyEvent(
            eventType,
            callTimes = CallTimes(1),
            inOrder = inOrder,
            timeoutMs = timeoutMs
        ) { events ->
            onEvent?.invoke(events.last())
        }
    }

    private fun <T : VideoRecordEvent> MockConsumer<VideoRecordEvent>.verifyEvent(
        eventType: Class<T>,
        callTimes: CallTimes,
        inOrder: Boolean = false,
        timeoutMs: Long = GENERAL_TIMEOUT,
        onEvent: ((List<T>) -> Unit)? = null,
    ) {
        verifyAcceptCall(eventType, inOrder, timeoutMs, callTimes)
        if (onEvent != null) {
            val captor = ArgumentCaptorCameraX<VideoRecordEvent> { argument ->
                eventType.isInstance(argument)
            }
            verifyAcceptCall(eventType, false, callTimes, captor)
            @Suppress("UNCHECKED_CAST")
            onEvent.invoke(captor.allValues as List<T>)
        }
    }
}
