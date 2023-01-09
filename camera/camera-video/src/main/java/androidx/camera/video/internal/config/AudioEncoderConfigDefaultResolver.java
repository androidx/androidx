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
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.AudioSpec;
import androidx.camera.video.internal.audio.AudioSettings;
import androidx.camera.video.internal.encoder.AudioEncoderConfig;
import androidx.core.util.Supplier;

/**
 * An {@link AudioEncoderConfig} supplier that resolves requested encoder settings from a
 * {@link AudioSpec} for the given {@link AudioSettings} using pre-defined default values.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioEncoderConfigDefaultResolver implements Supplier<AudioEncoderConfig> {

    private static final String TAG = "AudioEncCfgDefaultRslvr";

    private final String mMimeType;
    private final int mAudioProfile;
    private final AudioSpec mAudioSpec;
    private final AudioSettings mAudioSettings;
    private final Timebase mInputTimeBase;

    // Base config based on generic 720p AAC(LC) quality will be scaled by actual source settings.
    // TODO: These should vary based on quality/codec and be derived from actual devices
    private static final int AUDIO_BITRATE_BASE = 156000;
    private static final int AUDIO_CHANNEL_COUNT_BASE = 2;
    private static final int AUDIO_SAMPLE_RATE_BASE = 48000;

    /**
     * Constructor for an AudioEncoderConfigDefaultResolver.
     *
     * @param mimeType            The mime type for the audio encoder
     * @param audioProfile        The profile required for the audio encoder
     * @param inputTimebase       The timebase of the input frame.
     * @param audioSpec           The {@link AudioSpec} which defines the settings that should be
     *                            used with the audio encoder.
     * @param audioSettings       The settings used to configure the source of audio.
     */
    public AudioEncoderConfigDefaultResolver(@NonNull String mimeType,
            int audioProfile, @NonNull Timebase inputTimebase, @NonNull AudioSpec audioSpec,
            @NonNull AudioSettings audioSettings) {
        mMimeType = mimeType;
        mAudioProfile = audioProfile;
        mInputTimeBase = inputTimebase;
        mAudioSpec = audioSpec;
        mAudioSettings = audioSettings;
    }

    @Override
    @NonNull
    public AudioEncoderConfig get() {
        Range<Integer> audioSpecBitrateRange = mAudioSpec.getBitrate();
        Logger.d(TAG, "Using fallback AUDIO bitrate");
        // We have no other information to go off of. Scale based on fallback defaults.
        int resolvedBitrate = AudioConfigUtil.scaleAndClampBitrate(
                AUDIO_BITRATE_BASE,
                mAudioSettings.getChannelCount(), AUDIO_CHANNEL_COUNT_BASE,
                mAudioSettings.getSampleRate(), AUDIO_SAMPLE_RATE_BASE,
                audioSpecBitrateRange);

        return AudioEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setProfile(mAudioProfile)
                .setInputTimebase(mInputTimeBase)
                .setChannelCount(mAudioSettings.getChannelCount())
                .setSampleRate(mAudioSettings.getSampleRate())
                .setBitrate(resolvedBitrate)
                .build();
    }

}
