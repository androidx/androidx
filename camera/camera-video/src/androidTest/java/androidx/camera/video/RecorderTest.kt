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
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.AppOpsManager.OnOpNotedCallback
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.location.Location
import android.media.MediaCodec
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.ImageFormatConstants
import androidx.camera.core.impl.Observable.Observer
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.AudioUtil
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.GarbageCollectionUtil
import androidx.camera.testing.impl.IgnoreVideoRecordingProblematicDeviceRule
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.asFlow
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.fakes.NoOpMuxer
import androidx.camera.testing.impl.getDurationMs
import androidx.camera.testing.impl.getLocation
import androidx.camera.testing.impl.getMimeType
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE
import androidx.camera.video.Recorder.VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES
import androidx.camera.video.Recorder.sRetrySetupVideoDelayMs
import androidx.camera.video.Recorder.sRetrySetupVideoMaxCount
import androidx.camera.video.VideoOutput.SourceState.ACTIVE_NON_STREAMING
import androidx.camera.video.VideoOutput.SourceState.ACTIVE_STREAMING
import androidx.camera.video.VideoOutput.SourceState.INACTIVE
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.camera.video.VideoRecordEvent.Pause
import androidx.camera.video.VideoRecordEvent.Resume
import androidx.camera.video.internal.OutputStorage
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.ExtraSupportedResolutionQuirk
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite
import androidx.camera.video.internal.encoder.EncoderFactory
import androidx.camera.video.internal.encoder.InvalidConfigException
import androidx.camera.video.internal.muxer.MuxerException
import androidx.camera.video.internal.muxer.MuxerFactory
import androidx.camera.video.internal.utils.StorageUtil.NO_SPACE_LEFT_MESSAGE
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
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout

private const val GENERAL_TIMEOUT = 5000L
private const val TEST_ATTRIBUTION_TAG = "testAttribution"

// For the file size is small, the final file length possibly exceeds the file size limit
// after adding the file header. We still add the buffer for the tolerance of comparing the
// file length and file size limit.
private const val FILE_SIZE_LIMIT_BUFFER = 50 * 1024 // 50k threshold buffer

@SdkSuppress(minSdkVersion = 23)
@LargeTest
@RunWith(Parameterized::class)
class RecorderTest(private val implName: String, private val cameraConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraConfig)
        )

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule var testName: TestName = TestName()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule val skipRule: TestRule = IgnoreVideoRecordingProblematicDeviceRule()

    @get:Rule val labTest: LabTestRule = LabTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )

        private val storageFullException = lazy { IOException(NO_SPACE_LEFT_MESSAGE) }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector

    private lateinit var preview: Preview
    private lateinit var surfaceTexturePreview: Preview
    private lateinit var recordingSession: RecordingSession

    @Before
    fun setUp() = runBlocking {
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        // Skip for b/241876294
        assumeFalse(
            "Skip test for devices with ExtraSupportedResolutionQuirk, since the extra" +
                " resolutions cannot be used when the provided surface is an encoder surface.",
            DeviceQuirks.get(ExtraSupportedResolutionQuirk::class.java) != null,
        )
        assumeTrue(AudioUtil.canStartAudioRecord(MediaRecorder.AudioSource.CAMCORDER))

        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val lifecycleOwner = FakeLifecycleOwner().also { it.startAndResume() }
        camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector)
            }

        // Using Preview so that the surface provider could be set to control when to issue the
        // surface request.
        val cameraInfo = camera.cameraInfo
        val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
        val candidates =
            mutableSetOf<Size>().apply {
                if (testName.methodName == "setFileSizeLimit") {
                    videoCapabilities
                        .getProfiles(Quality.FHD, DynamicRange.SDR)
                        ?.defaultVideoProfile
                        ?.let { add(it.resolution) }
                    videoCapabilities
                        .getProfiles(Quality.HD, DynamicRange.SDR)
                        ?.defaultVideoProfile
                        ?.let { add(it.resolution) }
                    videoCapabilities
                        .getProfiles(Quality.SD, DynamicRange.SDR)
                        ?.defaultVideoProfile
                        ?.let { add(it.resolution) }
                }
                videoCapabilities
                    .getProfiles(Quality.LOWEST, DynamicRange.SDR)
                    ?.defaultVideoProfile
                    ?.let { add(it.resolution) }
            }
        assumeTrue(candidates.isNotEmpty())

        val resolutions: List<android.util.Pair<Int, Array<Size>>> =
            listOf<android.util.Pair<Int, Array<Size>>>(
                android.util.Pair.create(
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                    candidates.toTypedArray(),
                )
            )
        preview = Preview.Builder().setSupportedResolutions(resolutions).build()

        // Add another Preview to provide an additional surface for b/168187087.
        surfaceTexturePreview = Preview.Builder().build()
        instrumentation.runOnMainSync {
            surfaceTexturePreview.surfaceProvider =
                SurfaceTextureProvider.createSurfaceTextureProvider(
                    object : SurfaceTextureProvider.SurfaceTextureCallback {
                        override fun onSurfaceTextureReady(
                            surfaceTexture: SurfaceTexture,
                            resolution: Size,
                        ) {
                            // No-op
                        }

                        override fun onSafeToRelease(surfaceTexture: SurfaceTexture) {
                            surfaceTexture.release()
                        }
                    }
                )
        }

        assumeTrue(
            "This combination (preview, surfaceTexturePreview) is not supported.",
            camera.isUseCasesCombinationSupported(preview, surfaceTexturePreview),
        )

        camera =
            withContext(Dispatchers.Main) {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    // Must put surfaceTexturePreview before preview while addUseCases, otherwise
                    // an issue on Samsung device will occur. See b/196755459.
                    surfaceTexturePreview,
                    preview,
                )
            }

        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
                    recorder = createRecorder(),
                    outputOptionsProvider = { createFileOutputOptions() },
                    withAudio = true,
                )
            )
    }

    @After
    fun tearDown() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(5000)
        }

        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun canRecordToFile() {
        // Arrange.
        val outputOptions = createFileOutputOptions()

        // Act & Assert.
        recordingSession.createRecording(outputOptions = outputOptions).recordAndVerify()
    }

    @Test
    fun canRecordToNonExistFile() {
        // Arrange.
        val outputOptions = createFileOutputOptions(createTempFile().apply { delete() })

        // Act & Assert.
        recordingSession.createRecording(outputOptions = outputOptions).recordAndVerify()
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
        assertThrows(IllegalArgumentException::class.java) { createRecorder(targetBitrate = -5) }
    }

    @Test
    fun canRecordToMediaStore() {
        assumeTrue(
            "Ignore the test since the MediaStore.Video has compatibility issues.",
            DeviceQuirks.get(MediaStoreVideoCannotWrite::class.java) == null,
        )

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

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun canRecordToFileDescriptor() {
        // Arrange.
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

    @Test
    fun muxer_failedToSetOutput_receiveError() {
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun setOutput(path: String, format: Int) {
                    throw IOException("Failure on purpose")
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act & Assert.
        recording.start().verifyFinalize(error = ERROR_INVALID_OUTPUT_OPTIONS)
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun recordToFileDescriptor_withClosedFileDescriptor_receiveError() {
        // Arrange.
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

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    @SuppressLint("NewApi") // Intentionally testing behavior of calling from invalid API level
    fun prepareRecordingWithFileDescriptor_throwsExceptionBeforeApi26() {
        // Arrange.
        val recorder = createRecorder()
        val file = createTempFile()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
            // Assert.
            assertThrows(UnsupportedOperationException::class.java) {
                // Act.
                recorder.prepareRecording(context, FileDescriptorOutputOptions.Builder(pfd).build())
            }
        }
    }

    @Test
    fun canPauseResume() {
        recordingSession
            .createRecording()
            .startAndVerify()
            .pauseAndVerify()
            .resumeAndVerify()
            .stopAndVerify()
    }

    @Test
    fun canStartRecordingPaused_whenRecorderInitializing() {
        // Arrange.
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = recordingSession.createRecording(recorder = recorder)

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
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify().pauseAndVerify().resumeAndVerify().stopAndVerify()

        // Assert.
        val events = recording.getAllEvents()
        assertThat(events.size)
            .isAtLeast(
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
            }
            .forEach { (former: RecordingStats, latter: RecordingStats) ->
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
        val recording = recordingSession.createRecording(outputOptions = outputOptions)

        // Act.
        // To avoid long timeout of finalize, verify the start event to ensure the recording is
        // started. But don't verify the status count since we don't know how many status will
        // reach the file size limit.
        recording.startAndVerify(statusCount = 0)

        // Assert.
        val result =
            recording.verifyFinalize(timeoutMs = 60_000L, error = ERROR_FILE_SIZE_LIMIT_REACHED)
        assertThat(result.file.length()).isLessThan(fileSizeLimit + FILE_SIZE_LIMIT_BUFFER)
    }

    // Sets the file size limit to 1 byte, which will be lower than the initial data sent from
    // the encoder. This will ensure that the recording will be finalized even if it has no data
    // written to it.
    @Test
    fun setFileSizeLimitLowerThanInitialDataSize() {
        // Arrange.
        val fileSizeLimit = 1L // 1 byte
        val outputOptions = createFileOutputOptions(fileSizeLimit = fileSizeLimit)
        val recording = recordingSession.createRecording(outputOptions = outputOptions)

        // Act.
        recording.start()
        recording.verifyFinalize(error = ERROR_FILE_SIZE_LIMIT_REACHED)
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
        val recording = recordingSession.createRecording(outputOptions = outputOptions)

        // Act: ensure first frame is received before waiting for duration limit.
        recording.startAndVerify(statusCount = 1)

        // Assert.
        val result =
            recording.verifyFinalize(
                timeoutMs = durationLimitMs + 2500L,
                error = ERROR_DURATION_LIMIT_REACHED,
            )
        assertThat(result.finalize.recordingStats.recordedDurationNanos)
            .isAtMost(TimeUnit.MILLISECONDS.toNanos(durationLimitMs + durationToleranceMs))
        checkDurationAtMost(Uri.fromFile(outputOptions.file), durationLimitMs + durationToleranceMs)
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
        inOrder
            .verify(streamInfoObserver, timeout(GENERAL_TIMEOUT).atLeastOnce())
            .onNewData(argThat { it!!.streamState == StreamInfo.StreamState.INACTIVE })
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act.
        recording.start()

        // Assert: Starting recording should move Recorder to ACTIVE stream state
        inOrder
            .verify(streamInfoObserver, timeout(5000L).atLeastOnce())
            .onNewData(argThat { it!!.streamState == StreamInfo.StreamState.ACTIVE })

        // Act.
        recording.stop()

        // Assert: Stopping recording should eventually move to INACTIVE stream state
        inOrder
            .verify(streamInfoObserver, timeout(GENERAL_TIMEOUT).atLeastOnce())
            .onNewData(argThat { it!!.streamState == StreamInfo.StreamState.INACTIVE })
    }

    @Test
    fun start_throwsExceptionWhenActive() {
        // Arrange.
        val recorder = createRecorder()
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act: 1st start.
        recording.start()

        // Assert.
        assertThrows(java.lang.IllegalStateException::class.java) {
            // Act: 2nd start.
            val recording2 = recordingSession.createRecording(recorder = recorder)
            recording2.start()
        }
    }

    @Test
    fun start_whenSourceActiveNonStreaming() {
        // Arrange.
        val recorder = createRecorder(initSourceState = ACTIVE_NON_STREAMING)
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act.
        recording.start()
        recorder.onSourceStateChanged(ACTIVE_STREAMING)
        recording.verifyStart()
        recording.verifyStatus()
        recording.stopAndVerify()
    }

    @Test
    fun start_finalizeImmediatelyWhenSourceInactive() {
        // Arrange.
        val recorder = createRecorder(initSourceState = INACTIVE)
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act.
        recording.start()

        // Assert.
        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)
    }

    @Test
    fun pause_whenSourceActiveNonStreaming() {
        // Arrange.
        val recorder =
            createRecorder(sendSurfaceRequest = false, initSourceState = ACTIVE_NON_STREAMING)
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act.
        recording.start()
        recording.pause()
        recorder.sendSurfaceRequest()

        // Assert.
        recording.verifyStart()
        recording.verifyPause()
        recording.stopAndVerify(error = ERROR_NO_VALID_DATA)
    }

    @Test
    fun pause_noOpWhenAlreadyPaused() {
        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify().pauseAndVerify().pause()

        // Assert: One Pause event.
        val events = recording.getAllEvents()
        val pauseEvents = events.filterIsInstance<Pause>()
        assertThat(pauseEvents.size).isAtMost(1)
    }

    @Test
    fun pause_throwsExceptionWhenStopping() {
        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify().stopAndVerify()

        // Assert.
        assertThrows(IllegalStateException::class.java) { recording.pause() }
    }

    @Test
    fun resume_noOpWhenNotPaused() {
        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify().resume().stopAndVerify()

        // Assert: No Resume event.
        val resumeEvents = recording.getAllEvents().filterIsInstance<Resume>()
        assertThat(resumeEvents).isEmpty()
    }

    @Test
    fun resume_throwsExceptionWhenStopping() {
        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify().stop()

        // Assert.
        assertThrows(IllegalStateException::class.java) { recording.resume() }
    }

    @Test
    fun stop_beforeSurfaceRequested() {
        // Arrange.
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act.
        recording.start().stop()
        recorder.sendSurfaceRequest()

        // Assert.
        recording.verifyFinalize(error = ERROR_NO_VALID_DATA)
    }

    @Test
    fun stop_WhenUseCaseDetached() {
        assumeStopCodecAfterSurfaceRemovalCrashMediaServerQuirk()

        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify()
        instrumentation.runOnMainSync { cameraProvider.unbind(preview) }

        // Assert.
        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)
    }

    @SuppressLint("CheckResult") // Intended to be garbage collected
    @Test
    fun stop_whenRecordingIsGarbageCollected() {
        // Arrange.
        val recorder = createRecorder()
        val listener = MockConsumer<VideoRecordEvent>()

        // Act.
        recorder
            .prepareRecording(context, createFileOutputOptions())
            .start(mainThreadExecutor(), listener)
        GarbageCollectionUtil.runFinalization()

        // Assert: Ensure the event listener gets a finalize event. Note: the word "finalize" is
        // very overloaded here. This event means the recording has finished, but does not relate
        // to the finalizer that runs during garbage collection. However, that is what causes the
        // recording to finish.
        listener.verifyAcceptCall(Finalize::class.java, true, GENERAL_TIMEOUT, CallTimes(1))
    }

    @Test
    fun stop_noOpWhenStopping() {
        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.recordAndVerify()
        recording.stop()

        // Assert.
        recording.verifyNoMoreEvent()
    }

    @Test
    fun mute_outputWithAudioTrack() {
        // Arrange.
        val outputOptions = createFileOutputOptions()
        val recording = recordingSession.createRecording(outputOptions = outputOptions)

        recording.startAndVerify().mute(true)

        // The output file should contain audio track even it's muted at the beginning.
        recording.stopAndVerify()
    }

    @Test
    fun mute_receiveCorrectAudioStats() {
        // Arrange.
        val recording = recordingSession.createRecording()

        // Act.
        recording.startAndVerify().muteAndVerify(true).muteAndVerify(false)

        recording.stopAndVerify()
    }

    @Test
    fun mute_withInitialMuted() {
        // Arrange.
        val recording = recordingSession.createRecording(initialAudioMuted = true)

        // Act.
        recording.startAndVerify()

        // Assert.
        recording.verifyMute(true)
        recording.stopAndVerify()
    }

    @Test
    fun mute_noOpIfAudioDisabled() {
        // Arrange.
        val recording = recordingSession.createRecording(withAudio = false)

        // Act.
        recording.startAndVerify()

        // Assert: muting or un-muting a recording without audio should be no-op.
        recording.mute(true).mute(false).stopAndVerify()
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
        recordingSession.createRecording(withAudio = false).recordAndVerify()
    }

    @Test
    fun audioAmplitudeIsNoneWhenAudioIsDisabled() {
        // Arrange.
        val recording = recordingSession.createRecording(withAudio = false).startAndVerify()

        // Act.
        val status = recording.getStatusEvents().first()
        val amplitude = status.recordingStats.audioStats.audioAmplitude
        assertThat(amplitude).isEqualTo(AudioStats.AUDIO_AMPLITUDE_NONE)

        // Assert.
        val result = recording.stopAndVerify()
        assertThat(result.finalize.recordingStats.audioStats.audioAmplitude)
            .isEqualTo(AudioStats.AUDIO_AMPLITUDE_NONE)
    }

    @Test
    fun canGetAudioStatsAmplitude() {
        // Arrange.
        val recording = recordingSession.createRecording().startAndVerify()

        // Act.
        val status = recording.getStatusEvents().first()
        val amplitude = status.recordingStats.audioStats.audioAmplitude
        assertThat(amplitude).isAtLeast(AudioStats.AUDIO_AMPLITUDE_NONE)

        // Assert.
        val result = recording.stopAndVerify()
        assertThat(result.finalize.recordingStats.audioStats.audioAmplitude)
            .isAtLeast(AudioStats.AUDIO_AMPLITUDE_NONE)
    }

    @Test
    fun cannotStartMultiplePendingRecordingsWhileInitializing() {
        // Arrange: Prepare 1st recording and start.
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = recordingSession.createRecording(recorder = recorder)
        recording.start()

        // Assert.
        assertThrows<IllegalStateException> {
            // Act: Prepare 2nd recording and start.
            recordingSession.createRecording(recorder = recorder).start()
        }
    }

    @Test
    fun canRecoverFromErrorState(): Unit = runBlocking {
        // Arrange.
        // Create a video encoder factory that will fail on first 2 create encoder requests.
        val recorder =
            createRecorder(
                videoEncoderFactory = createVideoEncoderFactory(failCreationTimes = 2),
                retrySetupVideoMaxCount = 0, // Don't retry
            )
        // Recorder initialization should fail by 1st encoder creation fail.
        // Wait STREAM_ID_ERROR which indicates Recorder enter the error state.
        withTimeoutOrNull(3000) {
            recorder.streamInfo.asFlow().dropWhile { it!!.id != StreamInfo.STREAM_ID_ERROR }.first()
        } ?: fail("Do not observe STREAM_ID_ERROR from StreamInfo observer.")

        // Act: 1st recording request should fail by 2nd encoder creation fail.
        recordingSession
            .createRecording(recorder = recorder)
            .start()
            .verifyFinalize(error = ERROR_RECORDER_ERROR)

        // Act: 2nd recording request should be successful.
        recordingSession.createRecording(recorder = recorder).recordAndVerify()
    }

    @Test
    fun canRetrySetupVideo(): Unit = runBlocking {
        // Arrange.
        // Create a video encoder factory that will fail on first 2 create encoder requests.
        val recorder =
            createRecorder(
                videoEncoderFactory = createVideoEncoderFactory(failCreationTimes = 2),
                retrySetupVideoMaxCount = 3,
                retrySetupVideoDelayMs = 10, // make test quicker
            )

        // Act and verify.
        recordingSession.createRecording(recorder = recorder).recordAndVerify()
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun audioRecordIsAttributed() = runBlocking {
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
        }
    }

    @Test
    fun defaultVideoCapabilitiesSource() {
        val recorder = createRecorder()

        assertThat(recorder.videoCapabilitiesSource)
            .isEqualTo(VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE)
    }

    @Test
    fun canSetVideoCapabilitiesSource() {
        val recorder =
            createRecorder(videoCapabilitiesSource = VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES)

        assertThat(recorder.videoCapabilitiesSource)
            .isEqualTo(VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES)
    }

    @Test
    fun canSetVideoEncodingFrameRate() {
        // Arrange.
        val recorder = createRecorder(videoEncodingFrameRate = 60)

        // Assert.
        assertThat(recorder.videoEncodingFrameRate).isEqualTo(60)
    }

    @Test
    fun setNonSupportedVideoCapabilitiesSource_throwException() {
        assertThrows(IllegalArgumentException::class.java) {
            createRecorder(videoCapabilitiesSource = Integer.MAX_VALUE)
        }
    }

    @Test
    fun canSetAudioSource() {
        // Arrange.
        val recorder = createRecorder(audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION)

        // Assert.
        assertThat(recorder.audioSource).isEqualTo(MediaRecorder.AudioSource.VOICE_RECOGNITION)

        // Act: ensure the value is correctly propagated to the internal AudioSource instance.
        // Start recording to create the AudioSource instance.
        recordingSession.createRecording(recorder = recorder).startAndVerify(statusCount = 1)

        // Assert.
        assertThat(recorder.mAudioSource.mAudioSource)
            .isEqualTo(MediaRecorder.AudioSource.VOICE_RECOGNITION)
    }

    @Test
    fun insufficientStorageWhenRecording_shouldFailWithInsufficientStorageError() {
        // Arrange.
        val storageAvailableBytes = 100L * 1024L * 1024L // 100MB
        // Required size is larger than storage size.
        val requiredFreeStorageBytes = storageAvailableBytes + 10L
        val outputStorageFactory =
            object : OutputStorage.Factory {
                override fun create(outputOptions: OutputOptions): OutputStorage =
                    object : OutputStorage {
                        override fun getOutputOptions(): OutputOptions = outputOptions

                        override fun getAvailableBytes(): Long = storageAvailableBytes
                    }
            }
        val recorder =
            createRecorder(
                outputStorageFactory = outputStorageFactory,
                requiredFreeStorageBytes = requiredFreeStorageBytes,
            )
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act and verify.
        val result = recording.start().verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
        // Video is not saved.
        assertThat(result.uri).isEqualTo(Uri.EMPTY)
    }

    @Test
    fun insufficientStorageDuringRecording_shouldFailWithInsufficientStorageError() {
        // Arrange.
        var storageAvailableBytes = 100L * 1024L * 1024L // 100MB
        // Required size is less than storage size.
        val requiredFreeStorageBytes = storageAvailableBytes - 10L
        val outputStorageFactory =
            object : OutputStorage.Factory {
                override fun create(outputOptions: OutputOptions): OutputStorage =
                    object : OutputStorage {
                        override fun getOutputOptions(): OutputOptions = outputOptions

                        override fun getAvailableBytes(): Long = storageAvailableBytes
                    }
            }
        val recorder =
            createRecorder(
                outputStorageFactory = outputStorageFactory,
                requiredFreeStorageBytes = requiredFreeStorageBytes,
            )
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act.
        recording.startAndVerify(statusCount = 3)
        // Reduce the storage size to be less than requiredFreeStorageBytes.
        storageAvailableBytes = requiredFreeStorageBytes - 10L
        // Since the recorded bytes will over the difference between "storageAvailableBytes" and
        // "requiredFreeStorageBytes" (i.e. 10 bytes), it will refresh and check the new
        // storageAvailableBytes when receiving an encoded video data.

        // Verify.
        val result = recording.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
        // Video is saved.
        assertThat(result.uri).isNotEqualTo(Uri.EMPTY)
    }

    @Test
    fun insufficientStorageOnNextRecordingStarts_shouldFailWithInsufficientStorageError() {
        // Arrange.
        var storageAvailableBytes = 100L * 1024L * 1024L // 100MB
        // Required size is less than storage size.
        val requiredFreeStorageBytes = storageAvailableBytes - 10L
        val outputStorageFactory =
            object : OutputStorage.Factory {
                override fun create(outputOptions: OutputOptions): OutputStorage =
                    object : OutputStorage {
                        override fun getOutputOptions(): OutputOptions = outputOptions

                        override fun getAvailableBytes(): Long = storageAvailableBytes
                    }
            }
        val recorder =
            createRecorder(
                outputStorageFactory = outputStorageFactory,
                requiredFreeStorageBytes = requiredFreeStorageBytes,
            )
        val recording = recordingSession.createRecording(recorder = recorder)

        // Act: first recording should succeed.
        recording.recordAndVerify()

        // Arrange: reduce the storage size to be less than requiredFreeStorageBytes.
        @Suppress("AssignedValueIsNeverRead") // it will be read by the next recording.
        storageAvailableBytes = requiredFreeStorageBytes - 10L

        // Act: start next recording
        val recording2 = recordingSession.createRecording(recorder = recorder).start()

        // Verify.
        val result = recording2.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
        // Video is not saved.
        assertThat(result.uri).isEqualTo(Uri.EMPTY)
    }

    @Test
    fun throwStorageFullExceptionOnMuxerStart_receiveInsufficientStorageError() {
        // Arrange.
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun start() {
                    throw MuxerException(storageFullException.value)
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)

        // Act.
        val recording = recordingSession.createRecording(recorder = recorder).start()

        // Assert.
        recording.verifyFinalize(
            error = ERROR_INSUFFICIENT_STORAGE,
            shouldSkipOutputFileVerification = true,
        )
    }

    @Test
    fun throwStorageFullExceptionOnMuxerWriteSampleData_receiveInsufficientStorageError() {
        // Arrange.
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun writeSampleData(
                    trackIndex: Int,
                    byteBuffer: ByteBuffer,
                    bufferInfo: MediaCodec.BufferInfo,
                ) {
                    throw MuxerException(storageFullException.value)
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)

        // Act.
        val recording = recordingSession.createRecording(recorder = recorder).start()

        // Assert.
        recording.verifyFinalize(
            error = ERROR_INSUFFICIENT_STORAGE,
            shouldSkipOutputFileVerification = true,
        )
    }

    @Test
    fun throwStorageFullExceptionOnMuxerStop_receiveInsufficientStorageError() {
        // Arrange.
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun writeSampleData(
                    trackIndex: Int,
                    byteBuffer: ByteBuffer,
                    bufferInfo: MediaCodec.BufferInfo,
                ) {
                    throw MuxerException(storageFullException.value)
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)

        // Act.
        val recording = recordingSession.createRecording(recorder = recorder).start()

        // Assert.
        recording.verifyFinalize(
            error = ERROR_INSUFFICIENT_STORAGE,
            shouldSkipOutputFileVerification = true,
        )
    }

    @Test
    fun getVideoCapabilitiesStabilizationSupportIsCorrect_whenNotSupportedInExtensions() {
        assumeTrue(
            Recorder.getVideoCapabilities(cameraProvider.getCameraInfo(cameraSelector))
                .isStabilizationSupported
        )
        val sessionProcessor =
            FakeSessionProcessor(
                extensionSpecificChars =
                    listOf(
                        android.util.Pair(
                            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
                            intArrayOf(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF),
                        )
                    )
            )
        val cameraSelector =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                cameraSelector,
                sessionProcessor,
            )
        val capabilities =
            Recorder.getVideoCapabilities(cameraProvider.getCameraInfo(cameraSelector))

        assertThat(capabilities.isStabilizationSupported).isFalse()
    }

    @Test
    fun getVideoCapabilitiesStabilizationSupportIsCorrect_whenSupportedInExtensions() {
        assumeFalse(
            Recorder.getVideoCapabilities(cameraProvider.getCameraInfo(cameraSelector))
                .isStabilizationSupported
        )
        val sessionProcessor =
            FakeSessionProcessor(
                extensionSpecificChars =
                    listOf(
                        android.util.Pair(
                            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES,
                            intArrayOf(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON),
                        )
                    )
            )
        val cameraSelector =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                cameraSelector,
                sessionProcessor,
            )
        val capabilities =
            Recorder.getVideoCapabilities(cameraProvider.getCameraInfo(cameraSelector))

        assertThat(capabilities.isStabilizationSupported).isTrue()
    }

    @Test
    fun getVideoCapabilities_returnsCachedInstanceForSameCameraInfo() {
        // Arrange
        val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo)

        // Assert
        assertThat(capabilities1).isSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_returnsDifferentInstanceForDifferentCameraInfos() {
        // Arrange
        val cameraInfos = cameraProvider.availableCameraInfos
        assumeTrue("The device must have at least 2 cameras.", cameraInfos.size >= 2)
        val cameraInfo1 = cameraInfos[0]
        val cameraInfo2 = cameraInfos[1]

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo1)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo2)

        // Assert
        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_returnsCachedInstanceForDifferentCameraInfoWithSameIdAndConfig() {
        // Arrange
        val cameraConfig = FakeCameraConfig()
        val cameraInfo1 = AdapterCameraInfo(FakeCameraInfoInternal("0"), cameraConfig)
        val cameraInfo2 = AdapterCameraInfo(FakeCameraInfoInternal("0"), cameraConfig)

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo1)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo2)

        // Assert
        assertThat(capabilities1).isSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_returnsCachedInstanceForCameraInfoOfNewBinding() = runBlocking {
        // Arrange & act
        val capabilities1 = Recorder.getVideoCapabilities(camera.cameraInfo)
        val capabilities2 =
            Recorder.getVideoCapabilities(
                withContext(Dispatchers.Main) {
                    cameraProvider
                        .bindToLifecycle(
                            FakeLifecycleOwner().also { it.startAndResume() },
                            cameraSelector,
                            Preview.Builder().build(),
                        )
                        .cameraInfo
                }
            )

        // Assert
        assertThat(capabilities1).isSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_doesNotCacheForExternalCamera() {
        // Arrange
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_EXTERNAL),
                FakeCameraConfig(),
            )

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo)

        // Assert
        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_doesNotCacheForUnknownLensFacingCamera() {
        // Arrange
        val cameraInfo =
            AdapterCameraInfo(
                FakeCameraInfoInternal("0", CameraSelector.LENS_FACING_UNKNOWN),
                FakeCameraConfig(),
            )

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo)

        // Assert
        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_returnsDifferentInstancesForDifferentCameraConfigs() {
        // Arrange
        val cameraInfo1 = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())
        val cameraInfo2 = AdapterCameraInfo(FakeCameraInfoInternal("0"), FakeCameraConfig())

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo1)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo2)

        // Assert
        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    @Test
    fun getVideoCapabilities_returnsDifferentInstancesForExtensionCameraSelector() {
        // Arrange
        val cameraInfo1 = cameraProvider.getCameraInfo(cameraSelector)

        val sessionProcessor = FakeSessionProcessor()
        val cameraSelector2 =
            ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                cameraProvider,
                cameraSelector,
                sessionProcessor,
            )
        val cameraInfo2 = cameraProvider.getCameraInfo(cameraSelector2)

        // Act
        val capabilities1 = Recorder.getVideoCapabilities(cameraInfo1)
        val capabilities2 = Recorder.getVideoCapabilities(cameraInfo2)

        // Assert
        assertThat(capabilities1).isNotSameInstanceAs(capabilities2)
    }

    private fun testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(targetBitrate: Int) {
        // Arrange.
        val recorder = createRecorder(targetBitrate = targetBitrate)
        val recording = recordingSession.createRecording(recorder = recorder, withAudio = false)

        // Act.
        recording.recordAndVerify()

        // Assert.
        val videoEncoderBitrateRange =
            recorder.videoEncoderBitrateRange.fetchData().get(3, TimeUnit.SECONDS)
        assertThat(recorder.mFirstRecordingVideoBitrate)
            .isIn(
                com.google.common.collect.Range.closed(
                    videoEncoderBitrateRange.lower,
                    videoEncoderBitrateRange.upper,
                )
            )
    }

    private fun Recorder.sendSurfaceRequest() {
        instrumentation.runOnMainSync {
            preview.setSurfaceProvider { request: SurfaceRequest -> onSurfaceRequested(request) }
        }
    }

    private fun createTempFile() = temporaryFolder.newFile()

    private fun createRecorder(
        sendSurfaceRequest: Boolean = true,
        initSourceState: VideoOutput.SourceState = ACTIVE_STREAMING,
        qualitySelector: QualitySelector? = null,
        videoCapabilitiesSource: Int? = null,
        executor: Executor? = null,
        videoEncoderFactory: EncoderFactory? = null,
        audioEncoderFactory: EncoderFactory? = null,
        muxerFactory: MuxerFactory? = null,
        outputStorageFactory: OutputStorage.Factory? = null,
        targetBitrate: Int? = null,
        retrySetupVideoMaxCount: Int? = null,
        retrySetupVideoDelayMs: Long? = null,
        audioSource: Int? = null,
        requiredFreeStorageBytes: Long? = null,
        videoEncodingFrameRate: Int? = null,
    ): Recorder {
        val recorder =
            Recorder.Builder()
                .apply {
                    qualitySelector?.let { setQualitySelector(it) }
                    videoCapabilitiesSource?.let { setVideoCapabilitiesSource(it) }
                    executor?.let { setExecutor(it) }
                    videoEncoderFactory?.let { setVideoEncoderFactory(it) }
                    audioEncoderFactory?.let { setAudioEncoderFactory(it) }
                    muxerFactory?.let { setMuxerFactory(it) }
                    outputStorageFactory?.let { setOutputStorageFactory(it) }
                    targetBitrate?.let { setTargetVideoEncodingBitRate(it) }
                    audioSource?.let { setAudioSource(it) }
                    requiredFreeStorageBytes?.let { setRequiredFreeStorageBytes(it) }
                }
                .build()
                .apply {
                    videoEncodingFrameRate?.let { setVideoEncodingFrameRate(it) }
                    retrySetupVideoMaxCount?.let { sRetrySetupVideoMaxCount = it }
                    retrySetupVideoDelayMs?.let { sRetrySetupVideoDelayMs = it }
                }
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
    ): FileOutputOptions =
        FileOutputOptions.Builder(file)
            .apply {
                fileSizeLimit?.let { setFileSizeLimit(it) }
                durationLimitMillis?.let { setDurationLimitMillis(it) }
                location?.let { setLocation(it) }
            }
            .build()

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

    @Suppress("SameParameterValue")
    private fun checkDurationAtMost(uri: Uri, duration: Long) {
        MediaMetadataRetriever().useAndRelease {
            it.setDataSource(context, uri)
            val durationFromFile = it.getDurationMs()

            assertThat(durationFromFile).isAtMost(duration)
        }
    }

    @Suppress("SameParameterValue")
    private fun createVideoEncoderFactory(failCreationTimes: Int = 0): EncoderFactory {
        var createEncoderRequestCount = 0
        return EncoderFactory { executor, config, sessionType ->
            if (createEncoderRequestCount < failCreationTimes) {
                createEncoderRequestCount++
                throw InvalidConfigException("Create video encoder fail on purpose.")
            } else {
                Recorder.DEFAULT_ENCODER_FACTORY.createEncoder(executor, config, sessionType)
            }
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
}
