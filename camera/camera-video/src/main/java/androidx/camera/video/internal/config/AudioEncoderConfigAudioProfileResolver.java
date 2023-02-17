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
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.AudioSpec;
import androidx.camera.video.internal.audio.AudioSettings;
import androidx.camera.video.internal.encoder.AudioEncoderConfig;
import androidx.core.util.Supplier;

/**
 * An {@link AudioEncoderConfig} supplier that resolves requested encoder settings from an
 * {@link AudioSpec} for the given {@link AudioSettings} using the provided
 * {@link AudioProfileProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioEncoderConfigAudioProfileResolver implements
        Supplier<AudioEncoderConfig> {

    private static final String TAG = "AudioEncAdPrflRslvr";

    private final String mMimeType;
    private final Timebase mInputTimebase;
    private final int mAudioProfile;
    private final AudioSpec mAudioSpec;
    private final AudioSettings mAudioSettings;
    private final AudioProfileProxy mAudioProfileProxy;

    /**
     * Constructor for an AudioEncoderConfigAudioProfileResolver.
     *
     * @param mimeType            The mime type for the audio encoder
     * @param audioProfile        The profile required for the audio encoder
     * @param inputTimebase       The timebase of the input frame
     * @param audioSpec           The {@link AudioSpec} which defines the settings that should be
     *                            used with the audio encoder.
     * @param audioSettings       The settings used to configure the source of audio.
     * @param audioProfileProxy   The {@link AudioProfileProxy} used to resolve automatic and
     *                            range settings.
     */
    public AudioEncoderConfigAudioProfileResolver(@NonNull String mimeType,
            int audioProfile, @NonNull Timebase inputTimebase, @NonNull AudioSpec audioSpec,
            @NonNull AudioSettings audioSettings,
            @NonNull AudioProfileProxy audioProfileProxy) {
        mMimeType = mimeType;
        mAudioProfile = audioProfile;
        mInputTimebase = inputTimebase;
        mAudioSpec = audioSpec;
        mAudioSettings = audioSettings;
        mAudioProfileProxy = audioProfileProxy;
    }

    @Override
    @NonNull
    public AudioEncoderConfig get() {
        Logger.d(TAG, "Using resolved AUDIO bitrate from AudioProfile");
        Range<Integer> audioSpecBitrateRange = mAudioSpec.getBitrate();
        int resolvedBitrate = AudioConfigUtil.scaleAndClampBitrate(
                mAudioProfileProxy.getBitrate(),
                mAudioSettings.getChannelCount(), mAudioProfileProxy.getChannels(),
                mAudioSettings.getSampleRate(), mAudioProfileProxy.getSampleRate(),
                audioSpecBitrateRange);

        return AudioEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setProfile(mAudioProfile)
                .setInputTimebase(mInputTimebase)
                .setChannelCount(mAudioSettings.getChannelCount())
                .setSampleRate(mAudioSettings.getSampleRate())
                .setBitrate(resolvedBitrate)
                .build();
    }
}
