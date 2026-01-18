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
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.video.AudioSpec
import androidx.camera.video.internal.audio.AudioSettings
import androidx.core.util.Supplier

/**
 * An [AudioSettings] supplier that resolves requested audio settings from an [AudioSpec] using an
 * [AudioProfileProxy].
 */
public class AudioSettingsAudioProfileResolver
/**
 * Constructor for an AudioSettingsAudioProfileResolver.
 *
 * @param audioProfile The [AudioProfileProxy] used to resolve automatic and range settings.
 * @param audioSpec The [AudioSpec] which defines the settings that should be used with the audio
 *   source.
 * @param captureToEncodeRatio The capture to encode sample rate ratio.
 */
public constructor(
    private val audioSpec: AudioSpec,
    private val audioProfile: AudioProfileProxy,
    private val captureToEncodeRatio: Rational?,
) : Supplier<AudioSettings> {

    public companion object {
        private const val TAG = "AudioSrcAdPrflRslvr"
    }

    override fun get(): AudioSettings {
        // Resolve audio source
        val resolvedAudioSource = AudioConfigUtil.resolveAudioSource(audioSpec)

        // Resolve source format
        val resolvedSourceFormat = AudioConfigUtil.resolveAudioSourceFormat(audioSpec)

        val audioSpecChannelCount = audioSpec.channelCount
        val resolvedChannelCount: Int
        val audioProfileChannelCount = audioProfile.getChannels()
        if (audioSpecChannelCount == AudioSpec.CHANNEL_COUNT_UNSPECIFIED) {
            resolvedChannelCount = audioProfileChannelCount
            Logger.d(TAG, "Resolved AUDIO channel count from AudioProfile: $resolvedChannelCount")
        } else {
            resolvedChannelCount = audioSpecChannelCount
            Logger.d(
                TAG,
                "Media spec AUDIO channel count overrides AudioProfile " +
                    "[AudioProfile channel count: $audioProfileChannelCount" +
                    ", Resolved Channel Count: $resolvedChannelCount]",
            )
        }

        // Resolve sample rate
        val audioSpecSampleRate = audioSpec.sampleRate
        val audioProfileSampleRate = audioProfile.getSampleRate()
        val targetSampleRate: Int =
            if (audioSpecSampleRate != AudioSpec.SAMPLE_RATE_UNSPECIFIED) {
                audioSpecSampleRate
            } else {
                audioProfileSampleRate
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
            "Using resolved AUDIO sample rate or nearest supported from " +
                "AudioProfile: Capture sample rate: ${resolvedSampleRates.captureRate}Hz. " +
                "Encode sample rate: ${resolvedSampleRates.encodeRate}Hz. " +
                "[AudioProfile sample rate: ${audioProfileSampleRate}Hz]",
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
