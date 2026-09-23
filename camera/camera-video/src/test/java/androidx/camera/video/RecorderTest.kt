/*
 * Copyright 2026 The Android Open Source Project
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
import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaCodec
import android.media.MediaRecorder
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.util.Range
import android.util.Size
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.Observable.Observer
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.GarbageCollectionUtil
import androidx.camera.testing.impl.fakes.FakeByteBufferInput
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeEncoder
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import androidx.camera.testing.impl.fakes.NoOpMuxer
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.video.Recording
import androidx.camera.testing.impl.video.RecordingSession
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM
import androidx.camera.video.Recorder.AudioState
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
import androidx.camera.video.internal.BufferProvider
import androidx.camera.video.internal.OutputStorage
import androidx.camera.video.internal.audio.AudioStreamFactory
import androidx.camera.video.internal.audio.FakeAudioStream
import androidx.camera.video.internal.encoder.EncodeException
import androidx.camera.video.internal.encoder.EncoderFactory
import androidx.camera.video.internal.encoder.InvalidConfigException
import androidx.camera.video.internal.muxer.MuxerException
import androidx.camera.video.internal.muxer.MuxerFactory
import androidx.camera.video.internal.utils.StorageUtil.NO_SPACE_LEFT_MESSAGE
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class RecorderTest {

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fakeCamera = FakeCamera()
    private val surfaceRequestsToClose = mutableListOf<SurfaceRequest>()
    private val recordersToRelease = mutableListOf<Recorder>()
    private lateinit var recordingSession: RecordingSession
    private var currentTimestampUs = 100_000L
    private var latestVideoEncoder: FakeEncoder? = null
    private var latestAudioEncoder: FakeEncoder? = null
    private var latestSurfaceRequest: SurfaceRequest? = null

    private val storageFullException by lazy { IOException(NO_SPACE_LEFT_MESSAGE) }

    @Before
    fun setUp() {
        // PendingRecording.withAudioEnabled() checks RECORD_AUDIO permission via
        // PermissionChecker.checkSelfPermission() and throws SecurityException if not granted,
        // before Recorder/AudioSource or FakeAudioStream is ever invoked.
        shadowOf(context as Application).grantPermissions(Manifest.permission.RECORD_AUDIO)
        currentTimestampUs = 100_000L
        recordingSession =
            RecordingSession(
                RecordingSession.Defaults(
                    context = context,
                    outputOptionsProvider = { createFileOutputOptions() },
                    withAudio = true,
                    callbackExecutor = directExecutor(),
                    verifyStatusCount = 0,
                    verifyTimeoutMs = 100L,
                    verifyStatusTimeoutMs = 100L,
                    verifyNoFinalizeTimeoutMs = 0L,
                    verifyOutputFile = false,
                    onAction = { recording -> idle(recording.recorder) },
                )
            )
    }

    @After
    fun tearDown() {
        if (this::recordingSession.isInitialized) {
            recordingSession.release(500)
        }
        idleMainLooper()

        recordersToRelease.forEach {
            it.onSourceStateChanged(INACTIVE)
        }
        surfaceRequestsToClose.forEach { it.deferrableSurface.close() }
        idleMainLooper()

        val latch = CountDownLatch(2)
        Recorder.AUDIO_EXECUTOR.execute { latch.countDown() }
        CameraXExecutors.audioExecutor().execute { latch.countDown() }
        latch.await(1, TimeUnit.SECONDS)
    }

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun defaultRecorder_hasEmptyAudioProcessors() {
        val recorder = Recorder.Builder().build()
        assertThat(recorder.audioProcessors).isEmpty()
    }

    @Test
    fun setAudioProcessors_setsProcessorsOnRecorder() {
        val processor1 = NoOpAudioProcessor()
        val processor2 = NoOpAudioProcessor()
        val processors = listOf(processor1, processor2)
        val recorder = Recorder.Builder().setAudioProcessors(processors).build()

        assertThat(recorder.audioProcessors).containsExactly(processor1, processor2).inOrder()
    }

    @Test
    fun setAudioProcessors_defensiveCopy() {
        val processor1 = NoOpAudioProcessor()
        val processor2 = NoOpAudioProcessor()
        val mutableList = mutableListOf<AudioProcessor>(processor1)
        val recorder = Recorder.Builder().setAudioProcessors(mutableList).build()

        mutableList.add(processor2)
        assertThat(recorder.audioProcessors).containsExactly(processor1)
    }

    @Test
    fun setAudioProcessors_nullElementThrowsNullPointerException() {
        assertThrows(NullPointerException::class.java) {
            val listWithNull = listOf(NoOpAudioProcessor(), null)
            @Suppress("UNCHECKED_CAST")
            Recorder.Builder().setAudioProcessors(listWithNull as List<AudioProcessor>)
        }
    }

    @Test
    fun canSetTargetVideoEncodingBitrate() {
        val recorder = Recorder.Builder().setTargetVideoEncodingBitRate(6_000_000).build()

        assertThat(recorder.targetVideoEncodingBitRate).isEqualTo(6_000_000)
    }

    @Test
    fun recordingWithNegativeBitRate() {
        assertThrows(IllegalArgumentException::class.java) {
            Recorder.Builder().setTargetVideoEncodingBitRate(-5)
        }
    }

    @Test
    fun canSetTargetAudioEncodingBitrate() {
        val recorder = Recorder.Builder().setTargetAudioEncodingBitRate(128_000).build()

        assertThat(recorder.targetAudioEncodingBitRate).isEqualTo(128_000)
    }

    @Test
    fun recordingWithNegativeAudioBitRate() {
        assertThrows(IllegalArgumentException::class.java) {
            Recorder.Builder().setTargetAudioEncodingBitRate(-5)
        }
    }

    @Test
    fun canSetTargetAudioChannelCount() {
        val recorder = Recorder.Builder().setTargetAudioChannelCount(2).build()

        assertThat(recorder.targetAudioChannelCount).isEqualTo(2)
    }

    @Test
    fun recordingWithNegativeAudioChannelCount() {
        assertThrows(IllegalArgumentException::class.java) {
            Recorder.Builder().setTargetAudioChannelCount(-5)
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
        createRecording(recorder = recorder, withAudio = true).start()

        // Assert.
        assertThat(recorder.mAudioSource!!.mAudioSource)
            .isEqualTo(MediaRecorder.AudioSource.VOICE_RECOGNITION)
    }

    @Test
    @Config(maxSdk = 25)
    @SuppressLint("NewApi") // Intentionally testing behavior of calling from invalid API level
    fun prepareRecordingWithFileDescriptor_throwsExceptionBeforeApi26() {
        val recorder = Recorder.Builder().build()
        val file = createTempFile()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
            assertThrows(UnsupportedOperationException::class.java) {
                recorder.prepareRecording(context, FileDescriptorOutputOptions.Builder(pfd).build())
            }
        }
    }

    @Test
    fun optionsOverridesDefaults() {
        val qualitySelector = QualitySelector.from(Quality.HIGHEST)
        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()

        assertThat(recorder.qualitySelector).isEqualTo(qualitySelector)
    }

    @Test
    fun canRetrieveProvidedExecutorFromRecorder() {
        val myExecutor = Executor { command -> command?.run() }
        val recorder = Recorder.Builder().setExecutor(myExecutor).build()

        assertThat(recorder.executor).isSameInstanceAs(myExecutor)
    }

    @Test
    fun cannotRetrieveExecutorWhenExecutorNotProvided() {
        val recorder = Recorder.Builder().build()

        assertThat(recorder.executor).isNull()
    }

    @Test
    fun cannotStartMultiplePendingRecordingsWhileInitializing() {
        val recorder = Recorder.Builder().build()
        val file1 = createTempFile()
        val file2 = createTempFile()
        val recording =
            recorder.prepareRecording(context, FileOutputOptions.Builder(file1).build()).start(
                directExecutor()
            ) {}

        recording.use {
            assertThrows(IllegalStateException::class.java) {
                recorder.prepareRecording(context, FileOutputOptions.Builder(file2).build()).start(
                    directExecutor()
                ) {}
            }
        }
    }

    @Test
    fun defaultVideoCapabilitiesSource() {
        val recorder = Recorder.Builder().build()

        assertThat(recorder.videoCapabilitiesSource)
            .isEqualTo(VIDEO_CAPABILITIES_SOURCE_CAMCORDER_PROFILE)
    }

    @Test
    fun canSetVideoCapabilitiesSource() {
        val recorder =
            Recorder.Builder()
                .setVideoCapabilitiesSource(VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES)
                .build()

        assertThat(recorder.videoCapabilitiesSource)
            .isEqualTo(VIDEO_CAPABILITIES_SOURCE_CODEC_CAPABILITIES)
    }

    @Test
    fun canSetVideoEncodingFrameRate() {
        val recorder = Recorder.Builder().build().apply { setVideoEncodingFrameRate(60) }

        assertThat(recorder.videoEncodingFrameRate).isEqualTo(60)
    }

    @Test
    fun setNonSupportedVideoCapabilitiesSource_throwException() {
        assertThrows(IllegalArgumentException::class.java) {
            Recorder.Builder().setVideoCapabilitiesSource(Integer.MAX_VALUE)
        }
    }

    @Test
    fun staticSupportedMimeTypesShouldBeLowerCase() {
        for (mimeType in
            Recorder.SUPPORTED_VIDEO_MIME_TYPES + Recorder.SUPPORTED_AUDIO_MIME_TYPES) {
            assertWithMessage("MIME type $mimeType is not lower case")
                .that(mimeType)
                .isEqualTo(mimeType.lowercase())
        }
    }

    @Test
    fun canSetOutputFormat() {
        val recorder = Recorder.Builder().setOutputFormat(OUTPUT_FORMAT_WEBM).build()

        assertThat(recorder.outputFormat).isEqualTo(OUTPUT_FORMAT_WEBM)
    }

    @Test
    fun canSetVideoMimeType() {
        val mimeTypes = Recorder.SUPPORTED_VIDEO_MIME_TYPES

        mimeTypes.forEach { mimeType ->
            val recorder = Recorder.Builder().setVideoMimeType(mimeType).build()

            assertThat(recorder.videoMimeType).isEqualTo(mimeType)
        }
    }

    @Test
    fun setUnsupportedVideoMimeType_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Recorder.Builder().setVideoMimeType("unknown")
        }
    }

    @Test
    fun canSetAudioMimeType() {
        val mimeTypes = Recorder.SUPPORTED_AUDIO_MIME_TYPES

        mimeTypes.forEach { mimeType ->
            val recorder = Recorder.Builder().setAudioMimeType(mimeType).build()

            assertThat(recorder.audioMimeType).isEqualTo(mimeType)
        }
    }

    @Test
    fun setUnsupportedAudioMimeType_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            Recorder.Builder().setAudioMimeType("unknown")
        }
    }

    @Test
    fun getHighSpeedVideoCapabilities_whenCameraDoesNotSupportHighSpeed_returnNull() {
        val cameraInfo = FakeCameraInfoInternal().apply { isHighSpeedSupported = false }

        val videoCapabilities = Recorder.getHighSpeedVideoCapabilities(cameraInfo)

        assertThat(videoCapabilities).isNull()
    }

    @Test
    fun getVideoCapabilities_withUnsupportedMimeType_returnNull() {
        val cameraInfo = FakeCameraInfoInternal()

        val capabilities = Recorder.getVideoCapabilities(cameraInfo, "video/unknown")

        assertThat(capabilities).isNull()
    }

    @Test
    fun muxerFactory_failedToSetOutput_receiveError() {
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun setOutput(path: String, format: Int) {
                    throw IOException("Failure on purpose")
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)
        val recording = createRecording(recorder = recorder)

        recording.start().sendFrames(1).verifyFinalize(error = ERROR_INVALID_OUTPUT_OPTIONS)
    }

    @Test
    fun canPauseResume() {
        createRecording()
            .startAndVerify()
            .sendFrames()
            .pauseAndVerify()
            .resumeAndVerify()
            .sendFrames()
            .stopAndVerify()
    }

    @Test
    fun canStartRecordingPaused_whenRecorderInitializing() {
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = createRecording(recorder = recorder)

        recording.start()
        recording.pause()
        recorder.sendSurfaceRequest()

        recording.verifyStart()
        recording.verifyPause()
    }

    @Test
    fun canReceiveRecordingStats() {
        val recording = createRecording()

        recording
            .startAndVerify()
            .sendFrames(5)
            .also { it.verifyStatus(statusCount = 5) }
            .pauseAndVerify()
            .resumeAndVerify()
            .sendFrames(5)
            .also { it.verifyStatus(statusCount = 5) }
            .stopAndVerify()

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

        List(events.size - 1) { index ->
                Pair(events[index].recordingStats, events[index + 1].recordingStats)
            }
            .forEach { (former: RecordingStats, latter: RecordingStats) ->
                assertThat(former.numBytesRecorded).isAtMost(latter.numBytesRecorded)
                assertThat(former.recordedDurationNanos).isAtMost(latter.recordedDurationNanos)
            }

        events.last().recordingStats.also {
            assertThat(it.numBytesRecorded).isGreaterThan(0L)
            assertThat(it.recordedDurationNanos).isGreaterThan(0L)
        }
    }

    @Test
    fun setFileSizeLimit() {
        val fileSizeLimit = 500L * 1024L // 500 KB
        val outputOptions = createFileOutputOptions(fileSizeLimit = fileSizeLimit)
        val recording = createRecording(outputOptions = outputOptions)

        recording.startAndVerify().sendFrames(1)
        // Send enough bytes to exceed 95% of 500 KB (~486 KB)
        recording.sendFrames(count = 10, size = 60 * 1024)

        val result = recording.verifyFinalize(error = ERROR_FILE_SIZE_LIMIT_REACHED)
        assertThat(result.finalize.recordingStats.numBytesRecorded).isLessThan(fileSizeLimit)
    }

    @Test
    fun setFileSizeLimitLowerThanInitialDataSize() {
        val fileSizeLimit = 1L // 1 byte
        val outputOptions = createFileOutputOptions(fileSizeLimit = fileSizeLimit)
        val recording = createRecording(outputOptions = outputOptions)

        recording.start().sendFrames(count = 1, size = 1024)
        recording.verifyFinalize(error = ERROR_FILE_SIZE_LIMIT_REACHED)
    }

    @Test
    fun stop_withErrorWhenDurationLimitReached() {
        val durationLimitMs = 3000L
        val outputOptions = createFileOutputOptions(durationLimitMillis = durationLimitMs)
        val recording = createRecording(outputOptions = outputOptions)

        recording.startAndVerify().sendFrames(1)
        // Advance presentation timestamps past 3000ms (each frame in sendFrames with stepUs =
        // 1_600_000L)
        recording.sendFrames(count = 3, stepUs = 1_600_000L)

        val result = recording.verifyFinalize(error = ERROR_DURATION_LIMIT_REACHED)
        assertThat(result.finalize.recordingStats.recordedDurationNanos)
            .isAtMost(TimeUnit.MILLISECONDS.toNanos(durationLimitMs))
    }

    @Test
    fun checkStreamState() {
        val recorder = createRecorder()

        @Suppress("UNCHECKED_CAST")
        val streamInfoObserver = mock(Observer::class.java) as Observer<StreamInfo>
        val inOrder = inOrder(streamInfoObserver)
        recorder.streamInfo.addObserver(directExecutor(), streamInfoObserver)
        idleMainLooper()

        inOrder
            .verify(streamInfoObserver)
            .onNewData(argThat { it!!.streamState == StreamInfo.StreamState.INACTIVE })
        val recording = createRecording(recorder = recorder)

        recording.start()

        inOrder
            .verify(streamInfoObserver)
            .onNewData(argThat { it!!.streamState == StreamInfo.StreamState.ACTIVE })

        recording.stop()

        inOrder
            .verify(streamInfoObserver)
            .onNewData(argThat { it!!.streamState == StreamInfo.StreamState.INACTIVE })
    }

    @Test
    fun start_throwsExceptionWhenActive() {
        val recorder = createRecorder()
        val recording = createRecording(recorder = recorder)

        recording.start()

        assertThrows(IllegalStateException::class.java) {
            val recording2 = createRecording(recorder = recorder)
            recording2.start()
        }
    }

    @Test
    fun start_whenSourceActiveNonStreaming() {
        val recorder = createRecorder(initSourceState = ACTIVE_NON_STREAMING)
        val recording = createRecording(recorder = recorder)

        recording.start()
        recorder.onSourceStateChanged(ACTIVE_STREAMING)
        idleMainLooper()
        recording.verifyStart()
        recording.sendFrames(5)
        recording.verifyStatus()
        recording.stopAndVerify()
    }

    @Test
    fun start_finalizeImmediatelyWhenSourceInactive() {
        val recorder = createRecorder(initSourceState = INACTIVE)
        val recording = createRecording(recorder = recorder)

        recording.start()

        recording.verifyFinalize(error = ERROR_SOURCE_INACTIVE)
    }

    @Test
    fun pause_whenSourceActiveNonStreaming() {
        val recorder =
            createRecorder(sendSurfaceRequest = false, initSourceState = ACTIVE_NON_STREAMING)
        val recording = createRecording(recorder = recorder)

        recording.start()
        recording.pause()
        recorder.sendSurfaceRequest()

        recording.verifyStart()
        recording.verifyPause()
        recording.stopAndVerify(error = ERROR_NO_VALID_DATA)
    }

    @Test
    fun pause_noOpWhenAlreadyPaused() {
        val recording = createRecording()

        recording.startAndVerify().pauseAndVerify().pause()

        val events = recording.getAllEvents()
        val pauseEvents = events.filterIsInstance<Pause>()
        assertThat(pauseEvents.size).isAtMost(1)
    }

    @Test
    fun pause_throwsExceptionWhenStopping() {
        val recording = createRecording()

        recording.startAndVerify().sendFrames().stopAndVerify()

        assertThrows(IllegalStateException::class.java) { recording.pause() }
    }

    @Test
    fun resume_noOpWhenNotPaused() {
        val recording = createRecording()

        recording.startAndVerify().resume()

        val resumeEvents = recording.getAllEvents().filterIsInstance<Resume>()
        assertThat(resumeEvents).isEmpty()
    }

    @Test
    fun resume_throwsExceptionWhenStopping() {
        val recording = createRecording()

        recording.startAndVerify().stop()

        assertThrows(IllegalStateException::class.java) { recording.resume() }
    }

    @Test
    fun stop_beforeSurfaceRequested() {
        val recorder = createRecorder(sendSurfaceRequest = false)
        val recording = createRecording(recorder = recorder)

        recording.start().stop()
        recorder.sendSurfaceRequest()

        recording.verifyFinalize(error = ERROR_NO_VALID_DATA)
    }

    @SuppressLint("CheckResult") // Intended to be garbage collected
    @Test
    fun stop_whenRecordingIsGarbageCollected() {
        val recorder = createRecorder()
        val events = mutableListOf<VideoRecordEvent>()

        // start unreferenced recording
        recorder.prepareRecording(context, createFileOutputOptions()).start(directExecutor()) {
            events.add(it)
        }
        idleMainLooper()
        repeat(5) {
            if (events.filterIsInstance<Finalize>().isNotEmpty()) return@repeat
            GarbageCollectionUtil.runFinalization()
            idleMainLooper()
        }

        assertThat(events.filterIsInstance<Finalize>()).isNotEmpty()
    }

    @Test
    fun mute_receiveCorrectAudioStats() {
        val recording = createRecording(withAudio = true)

        recording.startAndVerify().muteAndVerify(true).muteAndVerify(false)

        recording.sendFrames().stopAndVerify()
    }

    @Test
    fun mute_withInitialMuted() {
        val recording = createRecording(withAudio = true, initialAudioMuted = true)

        recording.startAndVerify()

        recording.verifyMute(true)
        recording.sendFrames().stopAndVerify()
    }

    @Test
    fun mute_defaultToNotMuted() {
        val recorder = createRecorder()
        createRecording(recorder = recorder, withAudio = true)
            .startAndVerify()
            .mute(true)
            .sendFrames()
            .stopAndVerify()

        val recording2 =
            createRecording(recorder = recorder, withAudio = true).startAndVerify().sendFrames()
        val status = recording2.getStatusEvents().first()

        assertThat(status.recordingStats.audioStats.audioState)
            .isEqualTo(AudioStats.AUDIO_STATE_ACTIVE)

        recording2.stopAndVerify()
    }

    @Test
    fun mute_noOpIfAudioDisabled() {
        val recording = createRecording(withAudio = false)

        recording.startAndVerify()

        recording.mute(true).mute(false).sendFrames().stopAndVerify()
    }

    @Test
    fun canRecordWithoutAudio() {
        createRecording(withAudio = false).startAndVerify().sendFrames().stopAndVerify()
    }

    @Test
    fun canRecordMultipleFilesWithThenWithoutAudio() {
        val recorder = createRecorder()
        createRecording(recorder = recorder, withAudio = true)
            .startAndVerify()
            .sendFrames()
            .stopAndVerify()
        createRecording(recorder = recorder, withAudio = false)
            .startAndVerify()
            .sendFrames()
            .stopAndVerify()
    }

    @Test
    fun canRecordMultipleFilesWithoutThenWithAudio() {
        val recorder = createRecorder()
        createRecording(recorder = recorder, withAudio = false)
            .startAndVerify()
            .sendFrames()
            .stopAndVerify()
        createRecording(recorder = recorder, withAudio = true)
            .startAndVerify()
            .sendFrames()
            .stopAndVerify()
    }

    @Test
    fun canStartNextRecordingPausedAfterFirstRecordingFinalized() {
        val recorder = createRecorder()
        createRecording(recorder = recorder).startAndVerify().sendFrames().stopAndVerify()

        createRecording(recorder = recorder)
            .start()
            .also { it.verifyStart() }
            .pauseAndVerify()
            .stopAndVerify(error = ERROR_NO_VALID_DATA)
    }

    @Test
    fun canSwitchAudioOnOff() {
        val recorder = createRecorder()

        createRecording(recorder = recorder, withAudio = true)
            .startAndVerify()
            .sendFrames()
            .stopAndVerify()

        val recording =
            createRecording(recorder = recorder, withAudio = false).startAndVerify().sendFrames()
        val status = recording.getStatusEvents().first()
        assertThat(status.recordingStats.audioStats.audioState)
            .isEqualTo(AudioStats.AUDIO_STATE_DISABLED)
        recording.stopAndVerify()

        createRecording(recorder = recorder, withAudio = true)
            .startAndVerify()
            .sendFrames()
            .stopAndVerify()
    }

    @Test
    fun audioAmplitudeIsNoneWhenAudioIsDisabled() {
        val recorder = createRecorder()
        recorder.mAudioAmplitude = 0.5
        val recording =
            createRecording(recorder = recorder, withAudio = false).startAndVerify().sendFrames()

        val status = recording.getStatusEvents().first()
        val amplitude = status.recordingStats.audioStats.audioAmplitude
        assertThat(amplitude).isEqualTo(AudioStats.AUDIO_AMPLITUDE_NONE)

        val result = recording.stopAndVerify()
        assertThat(result.finalize.recordingStats.audioStats.audioAmplitude)
            .isEqualTo(AudioStats.AUDIO_AMPLITUDE_NONE)
    }

    @Test
    fun canGetAudioStatsAmplitude() {
        val recorder = createRecorder()
        recorder.mAudioAmplitude = 0.5
        val recording =
            createRecording(recorder = recorder, withAudio = true).startAndVerify().sendFrames()

        val status = recording.getStatusEvents().first()
        val amplitude = status.recordingStats.audioStats.audioAmplitude
        assertThat(amplitude).isAtLeast(AudioStats.AUDIO_AMPLITUDE_NONE)

        val result = recording.stopAndVerify()
        assertThat(result.finalize.recordingStats.audioStats.audioAmplitude)
            .isAtLeast(AudioStats.AUDIO_AMPLITUDE_NONE)
    }

    @Test
    fun canRecoverFromErrorState() {
        val recorder =
            createRecorder(
                videoEncoderFactory = createFailingVideoEncoderFactory(),
                retrySetupVideoMaxCount = 0,
            )
        assertThat(recorder.streamInfo.fetchData().get()!!.id).isEqualTo(StreamInfo.STREAM_ID_ERROR)

        createRecording(recorder = recorder).start().verifyFinalize(error = ERROR_RECORDER_ERROR)

        createRecording(recorder = recorder).startAndVerify().sendFrames().stopAndVerify()
    }

    @Test
    fun canRetrySetupVideo() {
        val recorder =
            createRecorder(
                videoEncoderFactory = createFailingVideoEncoderFactory(),
                retrySetupVideoMaxCount = 3,
                retrySetupVideoDelayMs = 10,
            )
        shadowOf(Looper.getMainLooper()).idleFor(50, TimeUnit.MILLISECONDS)

        createRecording(recorder = recorder).startAndVerify().sendFrames().stopAndVerify()
    }

    @Test
    fun insufficientStorageWhenRecording_shouldFailWithInsufficientStorageError() {
        val storageAvailableBytes = 100L * 1024L * 1024L // 100MB
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
        val recording = createRecording(recorder = recorder)

        val result = recording.start().verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
        assertThat(result.uri).isEqualTo(Uri.EMPTY)
    }

    @Test
    fun insufficientStorageDuringRecording_shouldFailWithInsufficientStorageError() {
        var storageAvailableBytes = 100L * 1024L * 1024L // 100MB
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
        val recording = createRecording(recorder = recorder)

        recording.startAndVerify().sendFrames(3).also { it.verifyStatus(statusCount = 3) }
        storageAvailableBytes = requiredFreeStorageBytes - 10L
        recording.sendFrames(1)

        val result = recording.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
        assertThat(result.finalize.error).isEqualTo(ERROR_INSUFFICIENT_STORAGE)
    }

    @Test
    fun insufficientStorageOnNextRecordingStarts_shouldFailWithInsufficientStorageError() {
        var storageAvailableBytes = 100L * 1024L * 1024L // 100MB
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
        val recording = createRecording(recorder = recorder)

        recording.startAndVerify().sendFrames().stopAndVerify()

        storageAvailableBytes = requiredFreeStorageBytes - 10L

        val recording2 = createRecording(recorder = recorder).start()

        val result = recording2.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
        assertThat(result.uri).isEqualTo(Uri.EMPTY)
    }

    @Test
    fun throwStorageFullExceptionOnMuxerStart_receiveInsufficientStorageError() {
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun start() {
                    throw MuxerException(storageFullException)
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)

        val recording = createRecording(recorder = recorder).start().sendFrames(1)

        recording.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
    }

    @Test
    fun throwStorageFullExceptionOnMuxerWriteSampleData_receiveInsufficientStorageError() {
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun writeSampleData(
                    trackIndex: Int,
                    byteBuffer: ByteBuffer,
                    bufferInfo: MediaCodec.BufferInfo,
                ) {
                    throw MuxerException(storageFullException)
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)

        val recording = createRecording(recorder = recorder).start().sendFrames(1)

        recording.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
    }

    @Test
    fun throwStorageFullExceptionOnMuxerStop_receiveInsufficientStorageError() {
        val muxerFactory = MuxerFactory {
            object : NoOpMuxer() {
                override fun stop() {
                    throw MuxerException(storageFullException)
                }
            }
        }
        val recorder = createRecorder(muxerFactory = muxerFactory)

        val recording = createRecording(recorder = recorder).startAndVerify().sendFrames()
        recording.stop()

        recording.verifyFinalize(error = ERROR_INSUFFICIENT_STORAGE)
    }

    @Test
    fun recorder_retainsRecording_duringSourceReconfiguration() {
        val outputOptions = createFileOutputOptions()
        val recorder = createRecorder()
        val recording = createRecording(recorder = recorder, outputOptions = outputOptions)

        recording.startAndVerify().sendFrames()

        recorder.onSourceStateChanged(VideoOutput.SourceState.CONFIGURING)
        idleMainLooper()

        recorder.sendSurfaceRequest()

        recording.verifyNoFinalize()

        recording.clearEvents()

        recording.sendFrames(3)
        recording.verifyStatus(statusCount = 3)

        recording.stopAndVerify()
    }

    @Test
    fun getVideoCapabilitiesStabilizationSupportIsCorrect_whenNotSupportedInExtensions() {
        val cameraInfo = FakeCameraInfoInternal().apply { isVideoStabilizationSupported = true }
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
        val cameraConfig = FakeCameraConfig(sessionProcessor = sessionProcessor)
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, cameraConfig)
        val capabilities = Recorder.getVideoCapabilities(adapterCameraInfo)

        assertThat(capabilities.isStabilizationSupported).isFalse()
    }

    @Test
    fun getVideoCapabilitiesStabilizationSupportIsCorrect_whenSupportedInExtensions() {
        val cameraInfo = FakeCameraInfoInternal().apply { isVideoStabilizationSupported = false }
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
        val cameraConfig = FakeCameraConfig(sessionProcessor = sessionProcessor)
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, cameraConfig)
        val capabilities = Recorder.getVideoCapabilities(adapterCameraInfo)

        assertThat(capabilities.isStabilizationSupported).isTrue()
    }

    @Test
    fun stopAndSourceInactive_releasesVideoAndAudioEncoders() {
        val recorder = createRecorder()
        val recording = createRecording(recorder, withAudio = true).startAndVerify().sendFrames()

        val videoEncoder = checkNotNull(latestVideoEncoder)
        val audioEncoder = checkNotNull(latestAudioEncoder)
        assertThat(videoEncoder.isReleaseCalled).isFalse()
        assertThat(audioEncoder.isReleaseCalled).isFalse()

        recording.stopAndVerify()
        recorder.onSourceStateChanged(INACTIVE)
        latestSurfaceRequest?.deferrableSurface?.close()
        idleMainLooper()

        assertThat(videoEncoder.isReleaseCalled).isTrue()
        assertThat(audioEncoder.isReleaseCalled).isTrue()
    }

    @Test
    fun audioEncoderError_stopsAudioSourceWhenRecordingFinalized() {
        // Arrange.
        val audioStream = createFakeAudioStream()
        val recorder = createRecorder(audioStreamFactory = { _, _ -> audioStream })
        val recording = createRecording(recorder, withAudio = true).startAndVerify()
        audioStream.verifyStartCall(CallTimes(1), VERIFY_AUDIO_STREAM_TIMEOUT_MS)

        // Act: the audio encoder fails, so the recording continues without audio.
        checkNotNull(latestAudioEncoder)
            .triggerEncodeError(EncodeException.ERROR_UNKNOWN, "Audio encoder fail on purpose.")
        idle(recorder)
        assertThat(recorder.mAudioState).isEqualTo(AudioState.ERROR_ENCODER)

        recording.sendFrames().stopAndVerify()

        // Assert: the audio source is stopped rather than left streaming.
        audioStream.verifyStopCall(CallTimes(1), VERIFY_AUDIO_STREAM_TIMEOUT_MS)
    }

    @Test
    fun audioSourceError_stopsAudioSourceWhenRecordingFinalized() {
        // Arrange: a failing audio processor makes the audio source report an
        // AudioSourceAccessException while the recording is in progress.
        val audioStream = createFakeAudioStream()
        val recorder =
            createRecorder(
                audioStreamFactory = { _, _ -> audioStream },
                audioProcessors = listOf(FailingAudioProcessor()),
            )
        val recording = createRecording(recorder, withAudio = true).startAndVerify()
        audioStream.verifyStartCall(CallTimes(1), VERIFY_AUDIO_STREAM_TIMEOUT_MS)

        // Act: wait until the audio source error has been propagated to the recorder.
        val deadlineMs = SystemClock.uptimeMillis() + VERIFY_AUDIO_STREAM_TIMEOUT_MS
        while (
            recorder.mAudioState != AudioState.ERROR_SOURCE &&
                SystemClock.uptimeMillis() < deadlineMs
        ) {
            idle(recorder)
        }
        assertThat(recorder.mAudioState).isEqualTo(AudioState.ERROR_SOURCE)

        recording.sendFrames().stopAndVerify()

        // Assert: the audio source is stopped rather than left streaming.
        audioStream.verifyStopCall(CallTimes(1), VERIFY_AUDIO_STREAM_TIMEOUT_MS)
    }

    @Test
    fun recordingWithSetTargetVideoEncodingBitRate() {
        testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(6_000_000)
    }

    @Test
    fun recordingWithSetTargetVideoEncodingBitRateOutOfRange() {
        testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(1000_000_000)
    }

    private fun testRecorderIsConfiguredBasedOnTargetVideoEncodingBitrate(targetBitrate: Int) {
        val recorder = createRecorder(targetBitrate = targetBitrate)
        val recording = createRecording(recorder = recorder, withAudio = false)

        recording.startAndVerify().sendFrames().stopAndVerify()

        val videoEncoderBitrateRange = recorder.videoEncoderBitrateRange.fetchData().get()!!
        assertThat(recorder.mFirstRecordingVideoBitrate)
            .isIn(
                com.google.common.collect.Range.closed(
                    videoEncoderBitrateRange.lower,
                    videoEncoderBitrateRange.upper,
                )
            )
    }

    private fun createTempFile(): File = temporaryFolder.newFile()

    private fun createFileOutputOptions(
        file: File = createTempFile(),
        fileSizeLimit: Long? = null,
        durationLimitMillis: Long? = null,
    ): FileOutputOptions =
        FileOutputOptions.Builder(file)
            .apply {
                fileSizeLimit?.let { setFileSizeLimit(it) }
                durationLimitMillis?.let { setDurationLimitMillis(it) }
            }
            .build()

    private fun createFailingVideoEncoderFactory(failCreationTimes: Int = 2): EncoderFactory {
        var createEncoderRequestCount = 0
        return EncoderFactory { _, _, _ ->
            if (createEncoderRequestCount < failCreationTimes) {
                createEncoderRequestCount++
                throw InvalidConfigException("Create video encoder fail on purpose.")
            } else {
                FakeEncoder().also { latestVideoEncoder = it }
            }
        }
    }

    private fun Recorder.sendSurfaceRequest(): SurfaceRequest {
        latestSurfaceRequest?.deferrableSurface?.close()
        idleMainLooper()
        val request = SurfaceRequest(Size(640, 480), fakeCamera) {}
        latestSurfaceRequest = request
        surfaceRequestsToClose.add(request)
        onSurfaceRequested(request)
        idleMainLooper()
        return request
    }

    private fun createRecorder(
        sendSurfaceRequest: Boolean = true,
        initSourceState: VideoOutput.SourceState = ACTIVE_STREAMING,
        videoEncoderFactory: EncoderFactory? = null,
        audioEncoderFactory: EncoderFactory? = null,
        muxerFactory: MuxerFactory? = null,
        outputStorageFactory: OutputStorage.Factory? = null,
        audioStreamFactory: AudioStreamFactory? = null,
        targetBitrate: Int? = null,
        retrySetupVideoMaxCount: Int? = null,
        retrySetupVideoDelayMs: Long? = null,
        audioSource: Int? = null,
        requiredFreeStorageBytes: Long? = null,
        audioProcessors: List<AudioProcessor>? = null,
    ): Recorder {
        val defaultVideoEncoderFactory = EncoderFactory { _, config, _ ->
            FakeEncoder(
                    encoderInfo =
                        FakeVideoEncoderInfo(supportedBitrateRange = Range(1, 100_000_000)),
                    encoderConfig = config,
                )
                .also { latestVideoEncoder = it }
        }
        val defaultAudioEncoderFactory = EncoderFactory { _, _, _ ->
            val byteBufferInput = FakeByteBufferInput()
            FakeEncoder(
                    encoderInput = byteBufferInput,
                    onStateChanged = { isActive ->
                        byteBufferInput.setState(
                            if (isActive) BufferProvider.State.ACTIVE
                            else BufferProvider.State.INACTIVE
                        )
                    },
                )
                .also { latestAudioEncoder = it }
        }
        val defaultOutputStorageFactory =
            object : OutputStorage.Factory {
                override fun create(outputOptions: OutputOptions): OutputStorage =
                    object : OutputStorage {
                        override fun getOutputOptions(): OutputOptions = outputOptions

                        override fun getAvailableBytes(): Long = Long.MAX_VALUE
                    }
            }
        val defaultAudioStreamFactory = AudioStreamFactory { _, _ -> createFakeAudioStream() }
        val recorder =
            Recorder.Builder()
                .setExecutor(mainThreadExecutor())
                .setVideoEncoderFactory(videoEncoderFactory ?: defaultVideoEncoderFactory)
                .setAudioEncoderFactory(audioEncoderFactory ?: defaultAudioEncoderFactory)
                .setMuxerFactory(muxerFactory ?: MuxerFactory { NoOpMuxer() })
                .setOutputStorageFactory(outputStorageFactory ?: defaultOutputStorageFactory)
                .setAudioStreamFactory(audioStreamFactory ?: defaultAudioStreamFactory)
                .apply {
                    targetBitrate?.let { setTargetVideoEncodingBitRate(it) }
                    audioSource?.let { setAudioSource(it) }
                    requiredFreeStorageBytes?.let { setRequiredFreeStorageBytes(it) }
                    audioProcessors?.let { setAudioProcessors(it) }
                }
                .build()
                .apply {
                    retrySetupVideoMaxCount?.let { sRetrySetupVideoMaxCount = it }
                    retrySetupVideoDelayMs?.let { sRetrySetupVideoDelayMs = it }
                }
        recordersToRelease.add(recorder)
        if (sendSurfaceRequest) {
            recorder.sendSurfaceRequest()
        }
        recorder.onSourceStateChanged(initSourceState)
        idleMainLooper()
        return recorder
    }

    private fun createRecording(
        recorder: Recorder = createRecorder(),
        outputOptions: OutputOptions = createFileOutputOptions(),
        withAudio: Boolean = false,
        initialAudioMuted: Boolean = false,
    ): Recording =
        recordingSession.createRecording(
            recorder = recorder,
            outputOptions = outputOptions,
            withAudio = withAudio,
            initialAudioMuted = initialAudioMuted,
        )

    private fun idle(recorder: Recorder) {
        idleMainLooper()
        recorder.mAudioSource?.let { audioSource ->
            repeat(2) {
                val latch = CountDownLatch(1)
                audioSource.executor.execute { latch.countDown() }
                latch.await(1, TimeUnit.SECONDS)
                idleMainLooper()
            }
        }
    }

    private fun Recording.sendFrames(
        count: Int = 1,
        size: Int = 1024,
        stepUs: Long = 100_000L,
    ): Recording {
        repeat(count) {
            if (latestVideoEncoder?.isStarted != true) return@repeat
            currentTimestampUs += stepUs
            latestVideoEncoder?.sendEncodedData(
                size = size,
                presentationTimeUs = currentTimestampUs,
                isKeyFrame = true,
            )
            if (withAudio && latestAudioEncoder?.isStarted == true) {
                latestAudioEncoder?.sendEncodedData(
                    size = 512,
                    presentationTimeUs = currentTimestampUs,
                    isKeyFrame = false,
                )
            }
            idle(recorder)
        }
        return this
    }

    private fun createFakeAudioStream(): FakeAudioStream =
        FakeAudioStream(
            audioDataProvider = { index ->
                val byteBuffer = ByteBuffer.allocate(1024).put(0, 10.toByte())
                FakeAudioStream.AudioData(byteBuffer, index * 10_000_000L)
            },
            readDelayMs = 1,
        )

    private class NoOpAudioProcessor : PassthroughAudioProcessor() {
        override fun onAudioBuffer(audioBuffer: ByteBuffer) {}
    }

    /** An [AudioProcessor] that always fails, which makes the audio source report an error. */
    private class FailingAudioProcessor : PassthroughAudioProcessor() {
        override fun onAudioBuffer(audioBuffer: ByteBuffer) {
            throw RuntimeException("Audio processing fail on purpose.")
        }
    }

    private companion object {
        private const val VERIFY_AUDIO_STREAM_TIMEOUT_MS = 10_000L
    }
}
