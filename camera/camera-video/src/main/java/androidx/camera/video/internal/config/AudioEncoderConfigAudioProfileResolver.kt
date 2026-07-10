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
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.audio.AudioSettings
import androidx.camera.video.internal.encoder.AudioEncoderConfig
import androidx.core.util.Supplier

/**
 * An [AudioEncoderConfig] supplier that resolves requested encoder settings from an [AudioSpec] for
 * the given [AudioSettings] using the provided [AudioProfileProxy].
 */
public class AudioEncoderConfigAudioProfileResolver
/**
 * Constructor for an AudioEncoderConfigAudioProfileResolver.
 *
 * @param mimeType The mime type for the audio encoder
 * @param audioProfile The profile required for the audio encoder
 * @param inputTimebase The timebase of the input frame
 * @param audioSpec The [AudioSpec] which defines the settings that should be used with the audio
 *   encoder.
 * @param audioSettings The settings used to configure the source of audio.
 * @param audioProfileProxy The [AudioProfileProxy] used to resolve automatic and range settings.
 */
public constructor(
    private val mimeType: String,
    private val audioProfile: Int,
    private val inputTimebase: Timebase,
    private val audioSpec: AudioSpec,
    private val audioSettings: AudioSettings,
    private val audioProfileProxy: AudioProfileProxy,
) : Supplier<AudioEncoderConfig> {

    public companion object {
        private const val TAG = "AudioEncAdPrflRslvr"
    }

    override fun get(): AudioEncoderConfig {
        val audioSpecBitrate = audioSpec.bitrate
        val resolvedBitrate: Int =
            if (audioSpecBitrate != AudioSpec.BITRATE_UNSPECIFIED) {
                audioSpecBitrate
            } else {
                Logger.d(TAG, "Using resolved AUDIO bitrate from AudioProfile")
                AudioConfigUtil.scaleBitrate(
                    baseBitrate = audioProfileProxy.getBitrate(),
                    actualChannelCount = audioSettings.getChannelCount(),
                    baseChannelCount = audioProfileProxy.getChannels(),
                    actualSampleRate = audioSettings.getEncodeSampleRate(),
                    baseSampleRate = audioProfileProxy.getSampleRate(),
                )
            }

        return AudioEncoderConfig.builder()
            .setMimeType(mimeType)
            .setProfile(audioProfile)
            .setInputTimebase(inputTimebase)
            .setChannelCount(audioSettings.getChannelCount())
            .setCaptureSampleRate(audioSettings.getCaptureSampleRate())
            .setEncodeSampleRate(audioSettings.getEncodeSampleRate())
            .setBitrate(resolvedBitrate)
            .build()
    }
}
