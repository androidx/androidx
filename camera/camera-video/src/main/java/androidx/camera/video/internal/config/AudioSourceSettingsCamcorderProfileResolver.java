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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CamcorderProfileProxy;
import androidx.camera.video.AudioSpec;
import androidx.camera.video.internal.AudioSource;
import androidx.core.util.Supplier;

/**
 * An {@link AudioSource.Settings} supplier that resolves requested source settings from an
 * {@link AudioSpec} using a {@link CamcorderProfileProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioSourceSettingsCamcorderProfileResolver implements
        Supplier<AudioSource.Settings> {

    private static final String TAG = "AudioSrcCmcrdrPrflRslvr";

    private final AudioSpec mAudioSpec;
    private final CamcorderProfileProxy mCamcorderProfile;

    /**
     * Constructor for an AudioSourceSettingsCamcorderProfileResolver.
     *
     * @param camcorderProfile The {@link CamcorderProfileProxy} used to resolve automatic and
     *                         range settings.
     * @param audioSpec        The {@link AudioSpec} which defines the settings that should be
     *                         used with the audio source.
     */
    public AudioSourceSettingsCamcorderProfileResolver(@NonNull AudioSpec audioSpec,
            @NonNull CamcorderProfileProxy camcorderProfile) {
        mAudioSpec = audioSpec;
        mCamcorderProfile = camcorderProfile;
    }

    @Override
    @NonNull
    public AudioSource.Settings get() {
        // Resolve audio source
        int resolvedAudioSource = AudioConfigUtil.resolveAudioSource(mAudioSpec);

        // Resolve source format
        int resolvedSourceFormat = AudioConfigUtil.resolveAudioSourceFormat(
                mAudioSpec);

        int audioSpecChannelCount = mAudioSpec.getChannelCount();
        Range<Integer> audioSpecSampleRate = mAudioSpec.getSampleRate();
        int resolvedSampleRate;
        int resolvedChannelCount;
        int camcorderProfileChannelCount = mCamcorderProfile.getAudioChannels();
        if (audioSpecChannelCount == AudioSpec.CHANNEL_COUNT_AUTO) {
            resolvedChannelCount = camcorderProfileChannelCount;
            Logger.d(TAG, "Resolved AUDIO channel count from CamcorderProfile: "
                    + resolvedChannelCount);
        } else {
            resolvedChannelCount = audioSpecChannelCount;
            Logger.d(TAG, "Media spec AUDIO channel count overrides CamcorderProfile "
                    + "[CamcorderProfile channel count: " + camcorderProfileChannelCount
                    + ", Resolved Channel Count: " + resolvedChannelCount + "]");
        }

        int camcorderProfileAudioSampleRate = mCamcorderProfile.getAudioSampleRate();
        resolvedSampleRate = AudioConfigUtil.selectSampleRateOrNearestSupported(
                audioSpecSampleRate, resolvedChannelCount, resolvedSourceFormat,
                camcorderProfileAudioSampleRate);
        Logger.d(TAG, "Using resolved AUDIO sample rate or nearest supported from "
                + "CamcorderProfile: " + resolvedSampleRate + "Hz. [CamcorderProfile sample rate: "
                + camcorderProfileAudioSampleRate + "Hz]");

        return AudioSource.Settings.builder()
                .setAudioSource(resolvedAudioSource)
                .setAudioFormat(resolvedSourceFormat)
                .setChannelCount(resolvedChannelCount)
                .setSampleRate(resolvedSampleRate)
                .build();
    }
}
