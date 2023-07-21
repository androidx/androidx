/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.testing.CameraUtil
import androidx.camera.video.internal.audio.AudioStreamImpl
import androidx.camera.video.internal.config.AudioSettingsAudioProfileResolver
import kotlinx.coroutines.runBlocking

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class AudioChecker {

    companion object {
        private const val TAG = "AudioChecker"

        fun canAudioStreamBeStarted(
            context: Context,
            cameraSelector: CameraSelector,
            qualitySelector: QualitySelector
        ): Boolean {
            return try {
                checkAudioStreamCanBeStarted(context, cameraSelector, qualitySelector)
                Logger.i(TAG, "Audio stream can be started.")
                true
            } catch (t: Throwable) {
                Logger.i(TAG, "Audio stream failed to start.", t)
                false
            }
        }

        private fun checkAudioStreamCanBeStarted(
            context: Context,
            cameraSelector: CameraSelector,
            qualitySelector: QualitySelector
        ) = runBlocking {
            // Only standard dynamic range is checked, since video and audio should be independent.
            val sdr = DynamicRange.SDR

            // Get audio source settings from EncoderProfiles
            val cameraInfo =
                CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
            val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)
            val supportedQualities = videoCapabilities.getSupportedQualities(sdr)
            val quality = qualitySelector.getPrioritizedQualities(supportedQualities).first()
            // Get a config using the default audio spec.
            val audioSettings =
                AudioSettingsAudioProfileResolver(
                    AudioSpec.builder().build(),
                    videoCapabilities.getProfiles(quality, sdr)!!.defaultAudioProfile!!
                ).get()
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