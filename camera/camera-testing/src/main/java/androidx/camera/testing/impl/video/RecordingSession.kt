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

import android.content.Context
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.video.OutputOptions
import androidx.camera.video.Recorder
import java.util.concurrent.Executor
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

public class RecordingSession(private val defaults: Defaults) {
    public companion object {
        public const val DEFAULT_VERIFY_STATUS_COUNT: Int = 5
        public const val DEFAULT_VERIFY_TIMEOUT_MS: Long = 10000
        public const val DEFAULT_VERIFY_STATUS_TIMEOUT_MS: Long = 15000
    }

    public data class Defaults(
        val context: Context,
        val recorder: Recorder? = null,
        val outputOptionsProvider: () -> OutputOptions,
        val withAudio: Boolean = true,
        val callbackExecutor: Executor = mainThreadExecutor(),
        val verifyStatusCount: Int = DEFAULT_VERIFY_STATUS_COUNT,
        val verifyTimeoutMs: Long = DEFAULT_VERIFY_TIMEOUT_MS,
        val verifyStatusTimeoutMs: Long = DEFAULT_VERIFY_STATUS_TIMEOUT_MS,
    )

    private val recordingsToStop = mutableListOf<Recording>()

    public fun createRecording(
        context: Context = defaults.context,
        recorder: Recorder = defaults.recorder!!,
        outputOptions: OutputOptions = defaults.outputOptionsProvider.invoke(),
        withAudio: Boolean = defaults.withAudio,
        initialAudioMuted: Boolean = false,
        asPersistentRecording: Boolean = false,
    ): Recording {
        return Recording(
                context = context,
                recorder = recorder,
                outputOptions = outputOptions,
                withAudio = withAudio,
                initialAudioMuted = initialAudioMuted,
                asPersistentRecording = asPersistentRecording,
                callbackExecutor = defaults.callbackExecutor,
                defaultVerifyStatusCount = defaults.verifyStatusCount,
                defaultVerifyTimeoutMs = defaults.verifyTimeoutMs,
                defaultVerifyStatusTimeoutMs = defaults.verifyStatusTimeoutMs,
            )
            .apply { recordingsToStop.add(this) }
    }

    // Intentionally made a non-suspend function, which is convenient at the end of most tests.
    public fun release(timeoutMs: Long): Unit = runBlocking {
        withTimeoutOrNull(timeoutMs) {
            recordingsToStop
                .filter { !it.stoppedDeferred.isCompleted }
                .run {
                    forEach { it.stop() }
                    map { it.stoppedDeferred }
                }
                .awaitAll()
        } // Ignore timeout.
    }
}
