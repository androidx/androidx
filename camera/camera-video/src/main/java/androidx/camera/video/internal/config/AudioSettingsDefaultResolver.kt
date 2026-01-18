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

import android.util.Rational
import androidx.camera.core.Logger
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.audio.AudioSettings
import androidx.core.util.Supplier

/**
 * An [AudioSettings] supplier that resolves requested source settings from an [AudioSpec] using
 * pre-defined default values.
 */
public class AudioSettingsDefaultResolver
/**
 * Constructor for an AudioSettingsDefaultResolver.
 *
 * @param audioSpec The [AudioSpec] which defines the settings that should be used with the audio
 *   source.
 * @param captureToEncodeRatio The capture to encode sample rate ratio.
 */
public constructor(private val audioSpec: AudioSpec, private val captureToEncodeRatio: Rational?) :
    Supplier<AudioSettings> {

    public companion object {
        private const val TAG = "DefAudioResolver"
    }

    override fun get(): AudioSettings {
        // Resolve audio source
        val resolvedAudioSource = AudioConfigUtil.resolveAudioSource(audioSpec)

        // Resolve source format
        val resolvedSourceFormat = AudioConfigUtil.resolveAudioSourceFormat(audioSpec)

        // Resolve channel count
        val audioSpecChannelCount = audioSpec.channelCount
        val resolvedChannelCount: Int
        if (audioSpecChannelCount == AudioSpec.CHANNEL_COUNT_UNSPECIFIED) {
            resolvedChannelCount = AudioConfigUtil.AUDIO_CHANNEL_COUNT_DEFAULT
            Logger.d(TAG, "Using fallback AUDIO channel count: $resolvedChannelCount")
        } else {
            resolvedChannelCount = audioSpecChannelCount
            Logger.d(TAG, "Using supplied AUDIO channel count: $audioSpecChannelCount")
        }

        // Resolve sample rate
        val audioSpecSampleRate = audioSpec.sampleRate
        val targetSampleRate: Int =
            if (audioSpecSampleRate != AudioSpec.SAMPLE_RATE_UNSPECIFIED) {
                audioSpecSampleRate
            } else {
                AudioConfigUtil.AUDIO_SAMPLE_RATE_DEFAULT
            }
        val resolvedSampleRates =
            AudioConfigUtil.resolveSampleRates(
                targetEncodeSampleRate = targetSampleRate,
                channelCount = resolvedChannelCount,
                sourceFormat = resolvedSourceFormat,
                captureToEncodeRatio = captureToEncodeRatio,
            )
        Logger.d(
            TAG,
            "Using AUDIO sample rate resolved from AudioSpec: " +
                "Capture sample rate: ${resolvedSampleRates.captureRate}Hz. " +
                "Encode sample rate: ${resolvedSampleRates.encodeRate}Hz.",
        )

        return AudioSettings.builder()
            .setAudioSource(resolvedAudioSource)
            .setAudioFormat(resolvedSourceFormat)
            .setChannelCount(resolvedChannelCount)
            .setCaptureSampleRate(resolvedSampleRates.captureRate)
            .setEncodeSampleRate(resolvedSampleRates.encodeRate)
            .build()
    }
}
