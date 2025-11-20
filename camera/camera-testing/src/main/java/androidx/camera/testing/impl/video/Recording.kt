/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.testing.impl.video

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.camera.testing.impl.hasAudio
import androidx.camera.testing.impl.hasVideo
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor
import androidx.camera.testing.impl.mocks.helpers.ArgumentMatcher
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast
import androidx.camera.testing.impl.useAndRelease
import androidx.camera.video.AudioStats
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.OutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.camera.video.VideoRecordEvent.Pause
import androidx.camera.video.VideoRecordEvent.Resume
import androidx.camera.video.VideoRecordEvent.Start
import androidx.camera.video.VideoRecordEvent.Status
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.CompletableDeferred

public class Recording
internal constructor(
    private val context: Context,
    recorder: Recorder,
    private val outputOptions: OutputOptions,
    private val withAudio: Boolean,
    private val initialAudioMuted: Boolean,
    private val asPersistentRecording: Boolean,
    private val callbackExecutor: Executor,
    private val defaultVerifyStatusCount: Int,
    private val defaultVerifyTimeoutMs: Long,
    private val defaultVerifyStatusTimeoutMs: Long,
) {
    @SuppressLint("MissingPermission", "UnsafeOptInUsageError")
    private val pendingRecording: PendingRecording =
        when (outputOptions) {
            is FileOutputOptions -> recorder.prepareRecording(context, outputOptions)
            is MediaStoreOutputOptions -> recorder.prepareRecording(context, outputOptions)
            is FileDescriptorOutputOptions ->
                if (Build.VERSION.SDK_INT >= 26) recorder.prepareRecording(context, outputOptions)
                else throw AssertionError()
            else -> throw AssertionError()
        }.apply {
            if (withAudio) {
                withAudioEnabled(initialAudioMuted)
            }
            if (asPersistentRecording) {
                asPersistentRecording()
            }
        }
    private lateinit var recording: androidx.camera.video.Recording
    private val listener = MockConsumer<VideoRecordEvent>()
    public val stoppedDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    public fun start(): Recording {
        recording =
            pendingRecording.start(callbackExecutor) {
                if (it is Finalize) {
                    stoppedDeferred.complete(Unit)
                }
                listener.accept(it)
            }
        return this
    }

    public fun startAndVerify(statusCount: Int = defaultVerifyStatusCount): Recording {
        start()
        verifyStart()
        verifyStatus(statusCount)
        return this
    }

    public fun verifyStart() {
        try {
            listener.verifyEvent(Start::class.java).single()
        } catch (t: Throwable) {
            throw AssertionError("Failed on #verifyStart", t)
        }
    }

    public fun verifyStatus(statusCount: Int = defaultVerifyStatusCount): List<Status> {
        try {
            return if (statusCount > 0) {
                listener.verifyStatus(eventCount = statusCount).also {
                    if (withAudio) {
                        // Ensure audio is recorded.
                        listener.verifyAcceptCall(
                            Status::class.java,
                            /*inOrder=*/ false,
                            defaultVerifyStatusTimeoutMs,
                            CallTimesAtLeast(1),
                        ) {
                            it.recordingStats.audioStats.audioBytesRecorded > 0L
                        }
                    }
                }
            } else emptyList()
        } catch (t: Throwable) {
            throw AssertionError("Failed on #verifyStatus", t)
        }
    }

    public fun stop() {
        if (this::recording.isInitialized) {
            recording.stop()
        } else {
            stoppedDeferred.complete(Unit)
        }
    }

    public fun stopAndVerify(error: Int? = ERROR_NONE): RecordingResult {
        stop()
        return verifyFinalize(error = error)
    }

    public fun recordAndVerify(error: Int? = ERROR_NONE): RecordingResult {
        startAndVerify()
        return stopAndVerify(error = error)
    }

    public fun verifyFinalize(
        timeoutMs: Long = defaultVerifyTimeoutMs,
        error: Int? = ERROR_NONE,
        shouldSkipOutputFileVerification: Boolean = false,
    ): RecordingResult {
        try {
            val finalize =
                listener.verifyEvent(Finalize::class.java, timeoutMs = timeoutMs).single()
            if (error != null) {
                assertWithMessage("Finalize error code is not expected.")
                    .that(finalize.error)
                    .isEqualTo(error)
            }
            if (
                !shouldSkipOutputFileVerification && finalize.outputResults.outputUri != Uri.EMPTY
            ) {
                when (outputOptions) {
                    is FileOutputOptions,
                    is MediaStoreOutputOptions ->
                        MediaMetadataRetriever().useAndRelease {
                            it.setDataSource(context, finalize.outputResults.outputUri)
                            assertThat(it.hasVideo()).isTrue()
                            assertThat(it.hasAudio()).isEqualTo(withAudio)
                        }
                }
            }
            return RecordingResult(finalize)
        } catch (t: Throwable) {
            throw AssertionError("Failed on #verifyFinalize", t)
        }
    }

    public fun pause(): Recording {
        recording.pause()
        return this
    }

    public fun pauseAndVerify(): Recording {
        pause()
        verifyPause()
        return this
    }

    public fun verifyPause() {
        try {
            listener.verifyEvent(Pause::class.java).single()
        } catch (t: Throwable) {
            throw AssertionError("Failed on #verifyPaused", t)
        }
    }

    public fun resume(): Recording {
        recording.resume()
        return this
    }

    public fun resumeAndVerify(statusCount: Int = defaultVerifyStatusCount): Recording {
        resume()
        verifyResume()
        verifyStatus(statusCount)
        return this
    }

    // Expose if needed
    private fun verifyResume() {
        try {
            listener.verifyEvent(Resume::class.java).single()
        } catch (t: Throwable) {
            throw AssertionError("Failed on #verifyResume", t)
        }
    }

    public fun mute(muted: Boolean): Recording {
        recording.mute(muted)
        return this
    }

    public fun muteAndVerify(muted: Boolean): Recording {
        mute(muted)
        verifyMute(muted)
        return this
    }

    public fun verifyMute(muted: Boolean) {
        // TODO(b/274862085): Change to verify the status events consecutively having MUTED state
        //  by adding the utility to MockConsumer.
        try {
            val expectedAudioState =
                if (muted) AudioStats.AUDIO_STATE_MUTED else AudioStats.AUDIO_STATE_ACTIVE
            val matcher =
                ArgumentMatcher<VideoRecordEvent> {
                    it.recordingStats.audioStats.audioState == expectedAudioState
                }
            listener.verifyAcceptCall(
                Status::class.java,
                /*inOrder=*/ true,
                defaultVerifyStatusTimeoutMs,
                CallTimesAtLeast(1),
                matcher,
            )
        } catch (t: Throwable) {
            throw AssertionError("Failed on #verifyMute", t)
        }
    }

    public fun clearEvents(): Unit = listener.clearAcceptCalls()

    public fun getAllEvents(): List<VideoRecordEvent> =
        listener.getAllEvents(VideoRecordEvent::class.java)

    public fun getStatusEvents(): List<Status> = listener.getAllEvents(Status::class.java)

    public fun verifyNoMoreEvent(): Unit = listener.verifyNoMoreAcceptCalls(/* inOrder= */ true)

    private fun MockConsumer<VideoRecordEvent>.verifyStatus(
        eventCount: Int = defaultVerifyStatusCount
    ): List<Status> =
        verifyEvent(
            Status::class.java,
            CallTimesAtLeast(eventCount),
            timeoutMs = defaultVerifyStatusTimeoutMs,
        )

    private fun <T : VideoRecordEvent> MockConsumer<VideoRecordEvent>.verifyEvent(
        eventType: Class<in T>,
        callTimes: CallTimes = CallTimes(1),
        inOrder: Boolean = true,
        timeoutMs: Long = defaultVerifyTimeoutMs,
    ): List<T> {
        val captor = ArgumentCaptor<VideoRecordEvent> { argument -> eventType.isInstance(argument) }
        verifyAcceptCall(eventType, inOrder, timeoutMs, callTimes, captor)
        @Suppress("UNCHECKED_CAST")
        return captor.allValues as List<T>
    }

    private fun <T : VideoRecordEvent> MockConsumer<VideoRecordEvent>.getAllEvents(
        eventType: Class<in T>
    ): List<T> = verifyEvent(eventType, CallTimesAtLeast(1), inOrder = false)

    public class RecordingResult(public val finalize: Finalize) {

        public val uri: Uri
            get() = finalize.outputResults.outputUri

        public val file: File by lazy {
            when (finalize.outputOptions) {
                is FileOutputOptions -> File(uri.path!!)
                else -> throw AssertionError()
            }
        }
    }
}
