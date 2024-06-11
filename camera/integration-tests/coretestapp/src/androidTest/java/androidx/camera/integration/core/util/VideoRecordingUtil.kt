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

package androidx.camera.integration.core.util

import android.content.Context
import android.util.Log
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.core.util.Consumer
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "VideoRecordingUtil"

/**
 * Executes the given block in the scope of a recording [Recording] with a [SharedFlow] containing
 * the [VideoRecordEvent]s for that recording.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun PendingRecording.startWithRecording(
    crossinline block: suspend Recording.(SharedFlow<VideoRecordEvent>) -> Unit
) {
    val eventFlow = MutableSharedFlow<VideoRecordEvent>(replay = 1)
    val eventListener =
        Consumer<VideoRecordEvent> { event ->
            when (event) {
                is VideoRecordEvent.Pause,
                is VideoRecordEvent.Resume,
                is Finalize -> {
                    // For all of these events, we need to reset the replay cache since we want
                    // them to be the first event received by new subscribers. The same is true for
                    // Start, but since no events should exist before start, we don't need to reset
                    // in that case.
                    eventFlow.resetReplayCache()
                }
            }
            // We still try to emit every event. This should cause the replay cache to contain one
            // of Start, Pause, Resume or Finalize. Status events will always only be sent after
            // Start or Resume, so they will only be sent to subscribers that have received one of
            // those events already.
            eventFlow.tryEmit(event)
        }

    val recording = start(CameraXExecutors.directExecutor(), eventListener)
    recording.use { it.apply { block(eventFlow) } }
}

suspend inline fun <reified T : VideoRecordEvent> SharedFlow<VideoRecordEvent>.waitForEvent(
    timeoutMs: Long
) =
    withTimeout(timeoutMs) {
        transformWhile {
            emit(it)
            it !is T
        }
    }

suspend fun doTempRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    minDurationMillis: Long,
    pauseDurationMillis: Long = 1000,
    withAudio: Boolean = true
): File {
    val tmpFile = createTempFileForRecording().apply { deleteOnExit() }

    videoCapture.output
        .prepareRecording(context, FileOutputOptions.Builder(tmpFile).build())
        .apply {
            if (withAudio) {
                withAudioEnabled()
            }

            val segmentDuration =
                if (pauseDurationMillis > 0) {
                    minDurationMillis / 2
                } else {
                    minDurationMillis
                }

            startWithRecording { eventFlow ->
                // Record until the duration matches the first segment duration
                withTimeout(timeMillis = 5 * minDurationMillis) {
                    eventFlow
                        .takeWhile { event ->
                            event.recordingStats.recordedDurationNanos <=
                                TimeUnit.MILLISECONDS.toNanos(segmentDuration)
                        }
                        .collect {}
                }

                if (pauseDurationMillis > 0) {
                    // Pause in the middle of the recording
                    pause()

                    // Wait for the pause event
                    eventFlow.waitForEvent<VideoRecordEvent.Pause>(
                        timeoutMs = 5 * minDurationMillis
                    )

                    // Stay paused for pauseDurationMillis
                    delay(pauseDurationMillis)

                    // Resume the recording
                    resume()

                    // Wait for the resume event
                    eventFlow.waitForEvent<VideoRecordEvent.Resume>(
                        timeoutMs = 5 * minDurationMillis
                    )

                    // Record for the remaining half of the min duration time
                    withTimeout(timeMillis = 5 * minDurationMillis) {
                        eventFlow
                            .takeWhile { event ->
                                event.recordingStats.recordedDurationNanos <=
                                    TimeUnit.MILLISECONDS.toNanos(minDurationMillis)
                            }
                            .collect {}
                    }
                }

                // Stop the recording
                stop()

                // Wait for the recording to finalize
                val finalize =
                    eventFlow.waitForEvent<Finalize>(timeoutMs = 5 * minDurationMillis).last()
                        as Finalize
                Log.i(
                    TAG,
                    "Recording finalized " +
                        if (!finalize.hasError()) "successfully" else "with error ${finalize.error}"
                )
            }
        }

    return tmpFile
}

// See b/177458751
suspend fun createTempFileForRecording(): File =
    withContext(Dispatchers.IO) { File.createTempFile("CameraX", ".tmp") }
