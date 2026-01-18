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
package androidx.camera.video.internal.config

import androidx.camera.core.Logger
import androidx.camera.core.impl.Timebase
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.audio.AudioSettings
import androidx.camera.video.internal.encoder.AudioEncoderConfig
import androidx.core.util.Supplier

/**
 * An [AudioEncoderConfig] supplier that resolves requested encoder settings from a [AudioSpec] for
 * the given [AudioSettings] using pre-defined default values.
 */
public class AudioEncoderConfigDefaultResolver
/**
 * Constructor for an AudioEncoderConfigDefaultResolver.
 *
 * @param mimeType The mime type for the audio encoder
 * @param audioProfile The profile required for the audio encoder
 * @param inputTimeBase The timebase of the input frame.
 * @param audioSpec The [AudioSpec] which defines the settings that should be used with the audio
 *   encoder.
 * @param audioSettings The settings used to configure the source of audio.
 */
public constructor(
    private val mimeType: String,
    private val audioProfile: Int,
    private val inputTimeBase: Timebase,
    private val audioSpec: AudioSpec,
    private val audioSettings: AudioSettings,
) : Supplier<AudioEncoderConfig> {

    public companion object {
        private const val TAG = "AudioEncCfgDefaultRslvr"

        // Base config based on generic 720p AAC(LC) quality will be scaled by actual source
        // settings.
        // TODO: These should vary based on quality/codec and be derived from actual devices
        private const val AUDIO_BITRATE_BASE = 156_000
        private const val AUDIO_CHANNEL_COUNT_BASE = 2
        private const val AUDIO_SAMPLE_RATE_BASE = 48_000
    }

    override fun get(): AudioEncoderConfig {
        val audioSpecBitrate = audioSpec.bitrate
        val resolvedBitrate: Int =
            if (audioSpecBitrate != AudioSpec.BITRATE_UNSPECIFIED) {
                audioSpecBitrate
            } else {
                Logger.d(TAG, "Using fallback AUDIO bitrate")
                // We have no other information to go off of. Scale based on fallback defaults.
                AudioConfigUtil.scaleBitrate(
                    baseBitrate = AUDIO_BITRATE_BASE,
                    actualChannelCount = audioSettings.getChannelCount(),
                    baseChannelCount = AUDIO_CHANNEL_COUNT_BASE,
                    actualSampleRate = audioSettings.getEncodeSampleRate(),
                    baseSampleRate = AUDIO_SAMPLE_RATE_BASE,
                )
            }

        return AudioEncoderConfig.builder()
            .setMimeType(mimeType)
            .setProfile(audioProfile)
            .setInputTimebase(inputTimeBase)
            .setChannelCount(audioSettings.getChannelCount())
            .setCaptureSampleRate(audioSettings.getCaptureSampleRate())
            .setEncodeSampleRate(audioSettings.getEncodeSampleRate())
            .setBitrate(resolvedBitrate)
            .build()
    }
}
