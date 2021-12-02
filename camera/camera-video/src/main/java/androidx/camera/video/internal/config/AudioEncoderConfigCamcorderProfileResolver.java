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
import androidx.camera.video.internal.encoder.AudioEncoderConfig;
import androidx.core.util.Supplier;

/**
 * An {@link AudioEncoderConfig} supplier that resolves requested encoder settings from an
 * {@link AudioSpec} for the given {@link AudioSource.Settings} using the provided
 * {@link CamcorderProfileProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioEncoderConfigCamcorderProfileResolver implements
        Supplier<AudioEncoderConfig> {

    private static final String TAG = "AudioEncCmcrdrPrflRslvr";

    private final String mMimeType;
    private final int mAudioProfile;
    private final AudioSpec mAudioSpec;
    private final AudioSource.Settings mAudioSourceSettings;
    private final CamcorderProfileProxy mCamcorderProfile;

    /**
     * Constructor for an AudioEncoderConfigCamcorderProfileResolver.
     *
     * @param mimeType            The mime type for the audio encoder
     * @param audioProfile        The profile required for the audio encoder
     * @param audioSpec           The {@link AudioSpec} which defines the settings that should be
     *                            used with the audio encoder.
     * @param audioSourceSettings The settings used to configure the source of audio.
     * @param camcorderProfile    The {@link CamcorderProfileProxy} used to resolve automatic and
     *                            range settings.
     */
    public AudioEncoderConfigCamcorderProfileResolver(@NonNull String mimeType,
            int audioProfile, @NonNull AudioSpec audioSpec,
            @NonNull AudioSource.Settings audioSourceSettings,
            @NonNull CamcorderProfileProxy camcorderProfile) {
        mMimeType = mimeType;
        mAudioProfile = audioProfile;
        mAudioSpec = audioSpec;
        mAudioSourceSettings = audioSourceSettings;
        mCamcorderProfile = camcorderProfile;
    }

    @Override
    @NonNull
    public AudioEncoderConfig get() {
        Logger.d(TAG, "Using resolved AUDIO bitrate from CamcorderProfile");
        Range<Integer> audioSpecBitrateRange = mAudioSpec.getBitrate();
        int resolvedBitrate = AudioConfigUtil.scaleAndClampBitrate(
                mCamcorderProfile.getAudioBitRate(),
                mAudioSourceSettings.getChannelCount(), mCamcorderProfile.getAudioChannels(),
                mAudioSourceSettings.getSampleRate(), mCamcorderProfile.getAudioSampleRate(),
                audioSpecBitrateRange);

        return AudioEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setProfile(mAudioProfile)
                .setChannelCount(mAudioSourceSettings.getChannelCount())
                .setSampleRate(mAudioSourceSettings.getSampleRate())
                .setBitrate(resolvedBitrate)
                .build();
    }
}
