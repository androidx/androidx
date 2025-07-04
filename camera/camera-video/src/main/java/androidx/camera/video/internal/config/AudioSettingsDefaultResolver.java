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

package androidx.camera.video.internal.config;

import android.util.Range;
import android.util.Rational;

import androidx.camera.core.Logger;
import androidx.camera.video.AudioSpec;
import androidx.camera.video.internal.audio.AudioSettings;
import androidx.core.util.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An {@link AudioSettings} supplier that resolves requested source settings from an
 * {@link AudioSpec} using pre-defined default values.
 */
public final class AudioSettingsDefaultResolver implements Supplier<AudioSettings> {

    private static final String TAG = "DefAudioResolver";

    private final AudioSpec mAudioSpec;
    @Nullable
    private final Rational mCaptureToEncodeRatio;

    /**
     * Constructor for an AudioSettingsDefaultResolver.
     *
     * @param audioSpec The {@link AudioSpec} which defines the settings that should be used with
     *                  the audio source.
     * @param captureToEncodeRatio The capture to encode sample rate ratio.
     */
    public AudioSettingsDefaultResolver(@NonNull AudioSpec audioSpec,
            @Nullable Rational captureToEncodeRatio) {
        mAudioSpec = audioSpec;
        mCaptureToEncodeRatio = captureToEncodeRatio;
    }

    @Override
    public @NonNull AudioSettings get() {
        // Resolve audio source
        int resolvedAudioSource = AudioConfigUtil.resolveAudioSource(mAudioSpec);

        // Resolve source format
        int resolvedSourceFormat = AudioConfigUtil.resolveAudioSourceFormat(mAudioSpec);

        // Resolve channel count
        int audioSpecChannelCount = mAudioSpec.getChannelCount();
        int resolvedChannelCount;
        if (audioSpecChannelCount == AudioSpec.CHANNEL_COUNT_AUTO) {
            resolvedChannelCount = AudioConfigUtil.AUDIO_CHANNEL_COUNT_DEFAULT;
            Logger.d(TAG, "Using fallback AUDIO channel count: " + resolvedChannelCount);
        } else {
            resolvedChannelCount = audioSpecChannelCount;
            Logger.d(TAG, "Using supplied AUDIO channel count: " + audioSpecChannelCount);
        }

        // Resolve sample rate
        Range<Integer> audioSpecSampleRateRange = mAudioSpec.getSampleRate();
        int targetSampleRate;
        if (AudioSpec.SAMPLE_RATE_RANGE_AUTO.equals(audioSpecSampleRateRange)) {
            targetSampleRate = AudioConfigUtil.AUDIO_SAMPLE_RATE_DEFAULT;
        } else {
            targetSampleRate = audioSpecSampleRateRange.getUpper();
        }
        CaptureEncodeRates resolvedSampleRates = AudioConfigUtil.resolveSampleRates(
                audioSpecSampleRateRange, targetSampleRate, resolvedChannelCount,
                resolvedAudioSource, mCaptureToEncodeRatio);
        Logger.d(TAG, "Using AUDIO sample rate resolved from AudioSpec: "
                + "Capture sample rate: " + resolvedSampleRates.getCaptureRate() + "Hz. "
                + "Encode sample rate: " + resolvedSampleRates.getEncodeRate() + "Hz.");

        return AudioSettings.builder()
                .setAudioSource(resolvedAudioSource)
                .setAudioFormat(resolvedSourceFormat)
                .setChannelCount(resolvedChannelCount)
                .setCaptureSampleRate(resolvedSampleRates.getCaptureRate())
                .setEncodeSampleRate(resolvedSampleRates.getEncodeRate())
                .build();
    }

}
