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
import androidx.camera.core.Logger
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.audio.AudioStreamImpl
import androidx.camera.video.internal.config.AudioSettingsDefaultResolver
import kotlinx.coroutines.runBlocking

public class AudioChecker {

    public companion object {
        private const val TAG = "AudioChecker"

        public fun canAudioStreamBeStarted(): Boolean {
            return try {
                checkAudioStreamCanBeStarted()
                Logger.i(TAG, "Audio stream can be started.")
                true
            } catch (t: Throwable) {
                Logger.i(TAG, "Audio stream failed to start.", t)
                false
            }
        }

        @SuppressLint("MissingPermission")
        private fun checkAudioStreamCanBeStarted() = runBlocking {
            val audioSpec = AudioSpec.builder().build()
            // Get a config using the default audio spec.
            val audioSettings = AudioSettingsDefaultResolver(audioSpec, null).get()
            with(AudioStreamImpl(audioSettings, null)) {
                try {
                    start()
                } finally {
                    release()
                }
            }
        }
    }
}
