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
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.video.AudioSpec
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoCapabilities
import androidx.camera.video.internal.audio.AudioStreamImpl
import androidx.camera.video.internal.config.AudioSettingsAudioProfileResolver
import androidx.camera.video.internal.config.AudioSettingsDefaultResolver
import kotlinx.coroutines.runBlocking

public class AudioChecker {

    public companion object {
        private const val TAG = "AudioChecker"

        public fun canAudioStreamBeStarted(
            videoCapabilities: VideoCapabilities,
            qualitySelector: QualitySelector
        ): Boolean {
            return try {
                checkAudioStreamCanBeStarted(videoCapabilities, qualitySelector)
                Logger.i(TAG, "Audio stream can be started.")
                true
            } catch (t: Throwable) {
                Logger.i(TAG, "Audio stream failed to start.", t)
                false
            }
        }

        @SuppressLint("MissingPermission")
        private fun checkAudioStreamCanBeStarted(
            videoCapabilities: VideoCapabilities,
            qualitySelector: QualitySelector
        ) = runBlocking {
            // Only standard dynamic range is checked, since video and audio should be independent.
            val sdr = DynamicRange.SDR
            val audioSpec = AudioSpec.builder().build()
            val priorityQuality = getPriorityQuality(videoCapabilities, qualitySelector)
            // Get a config using the default audio spec.
            val audioSettings =
                if (priorityQuality != null) {
                    AudioSettingsAudioProfileResolver(
                            audioSpec,
                            videoCapabilities
                                .getProfiles(priorityQuality, sdr)!!
                                .defaultAudioProfile!!
                        )
                        .get()
                } else {
                    AudioSettingsDefaultResolver(audioSpec).get()
                }
            with(AudioStreamImpl(audioSettings, null)) {
                try {
                    start()
                } finally {
                    release()
                }
            }
        }

        @SuppressLint("VisibleForTests")
        private fun getPriorityQuality(
            videoCapabilities: VideoCapabilities,
            qualitySelector: QualitySelector
        ): Quality? {
            // Only standard dynamic range is checked, since video and audio should be independent.
            val sdr = DynamicRange.SDR
            val supportedQualities = videoCapabilities.getSupportedQualities(sdr)
            return qualitySelector.getPrioritizedQualities(supportedQualities).firstOrNull()
        }
    }
}
